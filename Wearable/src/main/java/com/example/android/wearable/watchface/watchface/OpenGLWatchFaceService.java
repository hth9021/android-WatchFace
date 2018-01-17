/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.watchface.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import com.example.android.wearable.watchface.util.Gles2ColoredTriangleList;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Sample watch face using OpenGL. The watch face is rendered using {@link Gles2ColoredTriangleList}s.
 * OpenGL을 사용하는 시계 모드 샘플. 시계의 표면은, {@link Gles2ColoredTriangleList}를 사용해 렌더링됩니다.
 * The camera moves around in interactive mode and stops moving when the watch enters ambient mode.
 * 카메라는 대화 형 모드로 이동하고 시계가 주변 모드로 들어가면 움직이지 않습니다.
 */
public class OpenGLWatchFaceService extends Gles2WatchFaceService {

    private static final String TAG = "OpenGLWatchFaceService";

    /** Expected frame rate in interactive mode. */
    /** 대화식 모드에서 예상 프레임 속도. */
    private static final long FPS = 60;

    /** Z distance from the camera to the watchface. */
    /** Z 카메라에서 시계면까지의 거리. */
    private static final float EYE_Z = -2.3f;

    /** How long each frame is displayed at expected frame rate. */
    /** 각 프레임이 예상 프레임 속도로 표시되는 기간. */
    private static final long FRAME_PERIOD_MS = TimeUnit.SECONDS.toMillis(1) / FPS;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends Gles2WatchFaceService.Engine {
        /** Cycle time before the camera motion repeats. */
        /** 카메라 동작이 반복되기 전의 사이클 시간. */
        private static final long CYCLE_PERIOD_SECONDS = 5;

        /** Number of camera angles to precompute. */
        /** 사전 계산할 카메라 각도 수입니다. */
        private final int mNumCameraAngles = (int) (CYCLE_PERIOD_SECONDS * FPS);

        /** Projection transformation matrix. Converts from 3D to 2D. */
        /** 투영 변환 행렬. 3D에서 2D로 변환합니다. */
        private final float[] mProjectionMatrix = new float[16];

        /**
         * View transformation matrices to use in interactive mode.
         * 대화 형 모드에서 사용할 변환 매트릭스를 봅니다.
         * Converts from world to camera-relative coordinates. One matrix per camera position.
         * 세계에서 카메라 상대 좌표로 변환합니다. 카메라 위치마다 하나의 행렬.
         */
        private final float[][] mViewMatrices = new float[mNumCameraAngles][16];

        /** The view transformation matrix to use in ambient mode */
        /** 주변 모드에서 사용할보기 변환 행렬 */
        private final float[] mAmbientViewMatrix = new float[16];

        /**
         * Model transformation matrices.
         * 모델 변환 행렬.
         * Converts from model-relative coordinates to world coordinates.
         * 모델 기준 좌표에서 세계 좌표로 변환합니다. 
         * One matrix per degree of rotation.
         * 한 회전 당 하나의 매트릭스
         */
        private final float[][] mModelMatrices = new float[360][16];

        /**
         * Products of {@link #mViewMatrices} and {@link #mProjectionMatrix}. 
         * {@link #mViewMatrices} 및 {@link #mProjectionMatrix}의 제품.
         * One matrix per camera position.
         * 카메라 위치마다 하나의 행렬.
         */
        private final float[][] mVpMatrices = new float[mNumCameraAngles][16];

        /** The product of {@link #mAmbientViewMatrix} and {@link #mProjectionMatrix} */
        /** {#link #mAmbientViewMatrix} 및 {@link #mProjectionMatrix}의 제품 */
        private final float[] mAmbientVpMatrix = new float[16];

        /**
         * Product of {@link #mModelMatrices}, {@link #mViewMatrices}, and {@link #mProjectionMatrix}.
         * {@link #mModelMatrices}, {@link #mViewMatrices} 및 {@link #mProjectionMatrix}의 제품
         */
        private final float[] mMvpMatrix = new float[16];

        /** Triangles for the 4 major ticks. These are grouped together to speed up rendering. */
        /** 4 가지 주요 틱에 대한 삼각형. 이들은 렌더링을 가속화하기 위해 함께 그룹화됩니다. */
        private Gles2ColoredTriangleList mMajorTickTriangles;

        /** Triangles for the 8 minor ticks. These are grouped together to speed up rendering. */
        /** 8 작은 틱에 대한 삼각형. 이들은 렌더링을 가속화하기 위해 함께 그룹화됩니다.
        private Gles2ColoredTriangleList mMinorTickTriangles;

        /** Triangle for the second hand. */
        /** 초침 삼각형. */
        private Gles2ColoredTriangleList mSecondHandTriangle;

        /** Triangle for the minute hand. */
        /** 분침을위한 삼각형. */
        private Gles2ColoredTriangleList mMinuteHandTriangle;

        /** Triangle for the hour hand. */
        /** 시간당 삼각형. */
        private Gles2ColoredTriangleList mHourHandTriangle;

        private Calendar mCalendar = Calendar.getInstance();

        /** Whether we've registered {@link #mTimeZoneReceiver}. */
        /** {@link #mTimeZoneReceiver} 등록 여부. */
        private boolean mRegisteredTimeZoneReceiver;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(surfaceHolder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(OpenGLWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onGlContextCreated() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated");
            }
            super.onGlContextCreated();

            // Create program for drawing triangles.
            // 삼각형을 그리기위한 프로그램을 만듭니다.
            Gles2ColoredTriangleList.Program triangleProgram =
                    new Gles2ColoredTriangleList.Program();

            // We only draw triangles which all use the same program so we don't need to switch programs mid-frame.
            // 모든 프로그램이 동일한 프로그램을 사용하는 삼각형을 그리기 때문에 프로그램을 중간 프레임으로 전환 할 필요가 없습니다.
            // This means we can tell OpenGL to use this program only once rather than having to do so for each frame.
            // 이것은 우리가 OpenGL에게 각 프레임마다 그렇게하지 않고 한 번만이 프로그램을 사용하도록 말할 수 있음을 의미합니다.
            // This makes OpenGL draw faster.
            // 이렇게하면 OpenGL이 더 빨리 그려집니다.
            triangleProgram.use();

            // Create triangles for the ticks.
            // 틱에 대한 삼각형을 만듭니다.
            mMajorTickTriangles = createMajorTicks(triangleProgram);
            mMajorTickTriangles = createMinorTicks(triangleProgram);

            // Create triangles for the hands.
            // 손으로 삼각형을 만듭니다.
            mSecondHandTriangle = createHand(
                    triangleProgram,
                    0.02f /* width */,
                    1.0f /* height */,
                    new float[]{
                            1.0f /* red */,
                            0.0f /* green */,
                            0.0f /* blue */,
                            1.0f /* alpha */
                    }
            );
            mMinuteHandTriangle = createHand(
                    triangleProgram,
                    0.06f /* width */,
                    1f /* height */,
                    new float[]{
                            0.7f /* red */,
                            0.7f /* green */,
                            0.7f /* blue */,
                            1.0f /* alpha */
                    }
            );
            mHourHandTriangle = createHand(
                    triangleProgram,
                    0.1f /* width */,
                    0.6f /* height */,
                    new float[]{
                            0.9f /* red */,
                            0.9f /* green */,
                            0.9f /* blue */,
                            1.0f /* alpha */
                    }
            );

            // Precompute the clock angles.
            // 시계 각을 사전 계산하십시오.
            for (int i = 0; i < mModelMatrices.length; ++i) {
                Matrix.setRotateM(mModelMatrices[i], 0, i, 0, 0, 1);
            }

            // Precompute the camera angles.
            // 카메라 앵글을 미리 계산하십시오.
            for (int i = 0; i < mNumCameraAngles; ++i) {
                // Set the camera position (View matrix). When active, move the eye around to show
                // off that this is 3D.
                // 카메라 위치를 설정합니다 (매트릭스보기). 활성화되면 눈을 움직여 이것이 3D임을 과시합니다.
                final float cameraAngle = (float) (((float) i) / mNumCameraAngles * 2 * Math.PI);
                final float eyeX = (float) Math.cos(cameraAngle);
                final float eyeY = (float) Math.sin(cameraAngle);
                Matrix.setLookAtM(mViewMatrices[i],
                        0, // dest index //목적지 색인
                        eyeX, eyeY, EYE_Z, // eye //눈
                        0, 0, 0, // center // 중앙
                        0, 1, 0); // up vector // 위 벡터
            }

            Matrix.setLookAtM(mAmbientViewMatrix,
                    0, // dest index //목적지 색인
                    0, 0, EYE_Z, // eye //눈
                    0, 0, 0, // center // 중앙
                    0, 1, 0); // up vector // 위 벡터
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlSurfaceCreated: " + width + " x " + height);
            }
            super.onGlSurfaceCreated(width, height);

            // Update the projection matrix based on the new aspect ratio.
            // 새 종횡비에 따라 투영 행렬을 업데이트하십시오.
            final float aspectRatio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix,
                    0 /* offset */,
                    -aspectRatio /* left */, //왼쪽
                    aspectRatio /* right */, // 오른쪽
                    -1 /* bottom */, //밑
                    1 /* top */, //위
                    2 /* near */, //근처
                    7 /* far */); // 멀리

            // Precompute the products of Projection and View matrices for each camera angle.
            // 각 카메라 각도에 대한 투영 및 뷰 매트릭스의 곱을 사전 계산합니다.
            for (int i = 0; i < mNumCameraAngles; ++i) {
                Matrix.multiplyMM(mVpMatrices[i], 0, mProjectionMatrix, 0, mViewMatrices[i], 0);
            }

            Matrix.multiplyMM(mAmbientVpMatrix, 0, mProjectionMatrix, 0, mAmbientViewMatrix, 0);
        }

        /**
         * Creates a triangle for a hand on the watch face.
         * 시계 모드에서 손 모양의 삼각형을 만듭니다.
         * @param program program for drawing triangles
         * 삼각형 그리기를위한 프로그램 프로그램 
         * @param width width of base of triangle
         * @param 삼각형의 기본 폭
         * @param length length of triangle
         * @param 길이 삼각형의 길이
         * @param color color in RGBA order, each in the range [0, 1]
         * @param [0, 1] 범위의 RGBA 순서로 된 색상 색상
         */
        private Gles2ColoredTriangleList createHand(Gles2ColoredTriangleList.Program program,
                float width, float length, float[] color) {
            // Create the data for the VBO.
            // VBO에 대한 데이터를 만듭니다.
            float[] triangleCoords = new float[]{
                    // in counterclockwise order:
                    // 시계 반대 방향으로 :
                    0, length, 0,   // top //위
                    -width / 2, 0, 0,   // bottom left // 아래 왼쪽
                    width / 2, 0, 0    // bottom right // 아래 오른쪽
            };
            return new Gles2ColoredTriangleList(program, triangleCoords, color);
        }

        /**
         * Creates a triangle list for the major ticks on the watch face.
         * 시계 모드에서 주 눈금에 대한 삼각형 목록을 만듭니다.
         * @param program program for drawing triangles
         * 삼각형 그리기를위한 @param 프로그램
         */
        private Gles2ColoredTriangleList createMajorTicks(
                Gles2ColoredTriangleList.Program program) {
            // Create the data for the VBO.
            // VBO에 대한 데이터를 만듭니다.
            float[] trianglesCoords = new float[9 * 4];
            for (int i = 0; i < 4; i++) {
                float[] triangleCoords = getMajorTickTriangleCoords(i);
                System.arraycopy(triangleCoords, 0, trianglesCoords, i * 9, triangleCoords.length);
            }

            return new Gles2ColoredTriangleList(program, trianglesCoords,
                    new float[]{
                            1.0f /* red */, //빨간색
                            1.0f /* green */, // 초록색
                            1.0f /* blue */, // 파란색
                            1.0f /* alpha */ 
                    }
            );
        }

        /**
         * Creates a triangle list for the minor ticks on the watch face.
         * 시계 모드에서 작은 눈금에 대한 삼각형 목록을 만듭니다.
         * @param program program for drawing triangles
         * 삼각형 그리기를위한 @param 프로그램
         */
        private Gles2ColoredTriangleList createMinorTicks(
                Gles2ColoredTriangleList.Program program) {
            // Create the data for the VBO.
            // VBO에 대한 데이터를 만듭니다.
            float[] trianglesCoords = new float[9 * (12 - 4)];
            int index = 0;
            for (int i = 0; i < 12; i++) {
                if (i % 3 == 0) {
                    // This is where a major tick goes, so skip it.
                    // 이것은 주요한 tick 가가는 곳이기 때문에 건너 뛰십시오.
                    continue;
                }
                float[] triangleCoords = getMinorTickTriangleCoords(i);
                System.arraycopy(triangleCoords, 0, trianglesCoords, index, triangleCoords.length);
                index += 9;
            }

            return new Gles2ColoredTriangleList(program, trianglesCoords,
                    new float[]{
                            0.5f /* red */, //빨간색
                            0.5f /* green */, //초록색
                            0.5f /* blue */, // 파란색
                            1.0f /* alpha */
                    }
            );
        }

        private float[] getMajorTickTriangleCoords(int index) {
            return getTickTriangleCoords(0.03f /* width */, 0.09f /* length */,
                    index * 360 / 4 /* angleDegrees */);
        }

        private float[] getMinorTickTriangleCoords(int index) {
            return getTickTriangleCoords(0.02f /* width */, 0.06f /* length */,
                    index * 360 / 12 /* angleDegrees */);
        }

        private float[] getTickTriangleCoords(float width, float length, int angleDegrees) {
            // Create the data for the VBO.
            // VBO에 대한 데이터를 만듭니다.
            float[] coords = new float[]{
                    // in counterclockwise order:
                    // 시계 반대 방향으로 :
                    0, 1, 0,   // top
                    width / 2, length + 1, 0,   // bottom left //아래 왼쪽
                    -width / 2, length + 1, 0    // bottom right //아래 오른쪽
            };

            rotateCoords(coords, angleDegrees);
            return coords;
        }

        /**
         * Destructively rotates the given coordinates in the XY plane about the origin by the given angle.
         * 지정된 각도만큼 원점을 기준으로 XY 평면의 주어진 좌표를 파괴적으로 회전시킵니다.
         * @param coords flattened 3D coordinates
         * @param 좌표 평행화 된 3D 좌표
         * @param angleDegrees angle in degrees clockwise when viewed from negative infinity on the Z axis
         * @param angle Z 축의 음의 무한대에서 보았을 때 시계 방향으로 각도를 지정합니다.                    
         */
        private void rotateCoords(float[] coords, int angleDegrees) {
            double angleRadians = Math.toRadians(angleDegrees);
            double cos = Math.cos(angleRadians);
            double sin = Math.sin(angleRadians);
            for (int i = 0; i < coords.length; i += 3) {
                float x = coords[i];
                float y = coords[i + 1];
                coords[i] = (float) (cos * x - sin * y);
                coords[i + 1] = (float) (sin * x + cos * y);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we were detached.
                // 우리가 분리 된 동안 변경된 경우 시간대를 업데이트하십시오.
                mCalendar.setTimeZone(TimeZone.getDefault());

                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            OpenGLWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            OpenGLWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onDraw() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }
            super.onDraw();
            final float[] vpMatrix;

            // Draw background color and select the appropriate view projection matrix.
            // 배경색을 그리고 적절한 뷰 투영 행렬을 선택하십시오.
            // The background should always be black in ambient mode. 
            // 배경은 주변 모드에서 항상 검은 색이어야합니다.
            // The view projection matrix used is overhead in ambient.
            // 사용 된 뷰 투영 행렬은 주변 환경의 오버 헤드입니다.
            // In interactive mode, it's tilted depending on the current time.
            // 대화 형 모드에서는 현재 시간에 따라 기울어집니다.
            if (isInAmbientMode()) {
                GLES20.glClearColor(0, 0, 0, 1);
                vpMatrix = mAmbientVpMatrix;
            } else {
                GLES20.glClearColor(0.5f, 0.2f, 0.2f, 1);
                final int cameraIndex =
                        (int) ((System.currentTimeMillis() / FRAME_PERIOD_MS) % mNumCameraAngles);
                vpMatrix = mVpMatrices[cameraIndex];
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Compute angle indices for the three hands.
            // 세 손에 대한 각도 인덱스를 계산합니다.
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            float seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            final int secIndex = (int) (seconds / 60f * 360f);
            final int minIndex = (int) (minutes / 60f * 360f);
            final int hoursIndex = (int) (hours / 12f * 360f);

            // Draw triangles from back to front. Don't draw the second hand in ambient mode.
            // 삼각형을 뒤에서 앞으로 그립니다. 초침을 주변 모드로 그려서는 안됩니다.
            // Combine the model matrix with the projection and camera view.
            // 모델 매트릭스를 투영 및 카메라 뷰와 결합하십시오.
            Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[hoursIndex], 0);

            // Draw the triangle.
            // 삼각형을 그립니다.
            mHourHandTriangle.draw(mMvpMatrix);

            // Combine the model matrix with the projection and camera view.
            // 모델 매트릭스를 투영 및 카메라 뷰와 결합하십시오.
            Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[minIndex], 0);

            // Draw the triangle.
            // 삼각형을 그립니다.
            mMinuteHandTriangle.draw(mMvpMatrix);
            if (!isInAmbientMode()) {
                // Combine the model matrix with the projection and camera view.
                // 모델 매트릭스를 투영 및 카메라 뷰와 결합하십시오.
                Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[secIndex], 0);

                // Draw the triangle.
                // 삼각형을 그립니다.
                mSecondHandTriangle.draw(mMvpMatrix);
            }

            // Draw the major and minor ticks.
            // 크고 작은 틱을 그립니다.
            mMajorTickTriangles.draw(vpMatrix);
            mMajorTickTriangles.draw(vpMatrix);

            // Draw every frame as long as we're visible and in interactive mode.
            // 우리가 볼 수 있고 대화식 모드 인 한 모든 프레임을 그립니다.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }
    }
}
