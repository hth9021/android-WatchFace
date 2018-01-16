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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import com.example.android.wearable.watchface.R;
import com.example.android.wearable.watchface.config.AnalogComplicationConfigRecyclerViewAdapter;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Demonstrates two simple complications in a watch face. */
/** 시계 모드에서 두 가지 간단한 문제를 설명합니다. */
public class AnalogComplicationWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "AnalogWatchFace";

    // Unique IDs for each complication. The settings activity that supports allowing users
    // 각 complication 에 대한 고유 ID 사용자를 허용하는 설정 활동
    // to select their complication data provider requires numbers to be >= 0.
    // complication 데이터 제공자를 선택하려면 숫자가> = 0이어야합니다.
    private static final int BACKGROUND_COMPLICATION_ID = 0;

    private static final int LEFT_COMPLICATION_ID = 100;
    private static final int RIGHT_COMPLICATION_ID = 101;

    // Background, Left and right complication IDs as array for Complication API.
    // 배경, 왼쪽 및 오른쪽 합병증 ID는 Complication API 용 배열입니다.
    private static final int[] COMPLICATION_IDS = {
        BACKGROUND_COMPLICATION_ID, LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID
    };

    // Left and right dial supported types.
    // 좌우 다이얼 지원 유형.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
        {ComplicationData.TYPE_LARGE_IMAGE},
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        }
    };

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to check if complication location
    // 복잡한 위치를 확인하기 위해서 {@link AnalogComplicationConfigRecyclerViewAdapter}에 의해 사용됩니다.
    // is supported in settings config activity.
    // 설정 구성 활동에서 지원됩니다.
    public static int getComplicationId(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        // 다른 지원되는 위치를 여기에 추가하십시오.
        switch (complicationLocation) {
            case BACKGROUND:
                return BACKGROUND_COMPLICATION_ID;
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
    // 모든 컴플 례티의 ID를 꺼내기 위해서 {@link AnalogComplicationConfigRecyclerViewAdapter}에 의해 사용됩니다.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to see which complication types
    // 복합 형을보기 위해서 {@link AnalogComplicationConfigRecyclerViewAdapter}을 사용합니다
    // are supported in the settings config activity.
    // 설정 구성 활동에서 지원됩니다.
    public static int[] getSupportedComplicationTypes(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        // 다른 지원되는 위치를 여기에 추가하십시오.
        switch (complicationLocation) {
            case BACKGROUND:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[2];
            default:
                return new int[] {};
        }
    }

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     * 우리는 초침을 앞당기 기 위해 초를 한 번 업데이트합니다.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 0;

        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;

        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;

        private float mCenterX;
        private float mCenterY;

        private float mSecondHandLength;
        private float mMinuteHandLength;
        private float mHourHandLength;

        // Colors for all hands (hour, minute, seconds, ticks) based on photo loaded.
        // 사진을로드 할 때 모든 (시간, 분, 초, 틱)의 색상.
        private int mWatchHandAndComplicationsColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;

        private int mBackgroundColor;

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondAndHighlightPaint;
        private Paint mTickAndCirclePaint;

        private Paint mBackgroundPaint;

        /* Maps active complication ids to the data for that complication. 
         * 활성 complication ID를 해당 complication의 데이터에 매핑합니다.
         * Note: Data will only be
         * 참고 : 데이터는
         * present if the user has chosen a provider via the settings activity for the watch face.
         * 사용자가 시계 모드에 대한 설정 활동을 통해 공급자를 선택한 경우 표시됩니다.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         * complication ID를 시계 모드에서 렌더링하는 해당 ComplicationDrawable에 문제 ID를 매핑합니다.
         */
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        // 배경색, 강조 표시 색상 및 읽지 않은 알림이 있음을 나타내는 시각을 사용자의 기본 설정으로 가져 오는 데 사용됩니다.
        SharedPreferences mSharedPref;

        // User's preference for if they want visual shown to indicate unread notifications.
        // 읽지 않은 알림을 나타 내기 위해 시각적으로 표시하려는 경우에 대한 사용자의 환경 설정입니다.
        private boolean mUnreadNotificationsPreference;
        private int mNumberOfUnreadNotifications = 0;

        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mCalendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };

        // Handler to update the time once a second in interactive mode.
        // 대화 형 모드에서 1 초에 한 번씩 시간을 업데이트하는 처리기입니다.
        private final Handler mUpdateTimeHandler =
                new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    INTERACTIVE_UPDATE_RATE_MS
                                            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                    }
                };

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");

            super.onCreate(holder);

            // Used throughout watch face to pull user's preferences.
            // 시계 모드에서 사용자의 취향을 결정하는 데 사용됩니다.
            Context context = getApplicationContext();
            mSharedPref =
                    context.getSharedPreferences(
                            getString(R.string.analog_complication_preference_file_key),
                            Context.MODE_PRIVATE);

            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(AnalogComplicationWatchFaceService.this)
                            .setAcceptsTapEvents(true)
                            .build());

            loadSavedPreferences();
            initializeComplicationsAndBackground();
            initializeWatchFace();
        }

        // Pulls all user's preferences for watch face appearance.
        // 시계 외관에 대한 모든 사용자의 환경 설정을 가져옵니다.
        private void loadSavedPreferences() {

            String backgroundColorResourceName =
                    getApplicationContext().getString(R.string.saved_background_color);

            mBackgroundColor = mSharedPref.getInt(backgroundColorResourceName, Color.BLACK);

            String markerColorResourceName =
                    getApplicationContext().getString(R.string.saved_marker_color);

            // Set defaults for colors
            // 색상 기본값 설정
            mWatchHandHighlightColor = mSharedPref.getInt(markerColorResourceName, Color.RED);

            if (mBackgroundColor == Color.WHITE) {
                mWatchHandAndComplicationsColor = Color.BLACK;
                mWatchHandShadowColor = Color.WHITE;
            } else {
                mWatchHandAndComplicationsColor = Color.WHITE;
                mWatchHandShadowColor = Color.BLACK;
            }

            String unreadNotificationPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_unread_notifications_pref);

            mUnreadNotificationsPreference =
                    mSharedPref.getBoolean(unreadNotificationPreferenceResourceName, true);
        }

        private void initializeComplicationsAndBackground() {
            Log.d(TAG, "initializeComplications()");

            // Initialize background color (in case background complication is inactive).
            // 배경색을 초기화하십시오 (배경 복잡성이 비활성 인 경우).
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face. In this watch face, we create one for left, right,
            // and background, but you could add many more.
            // 사용자가 시계 모드에서 complication을 렌더링 할 수있는 각 위치에 대해 ComplicationDrawable을 만듭니다. 
            // 이 시계 모드에서는 왼쪽, 오른쪽 및 배경으로 하나를 만들지 만 더 많이 추가 할 수 있습니다.
            ComplicationDrawable leftComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable backgroundComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            // 새로운 complication을 SparseArray에 추가하여 모든 complication의 스타일 및 앰비언트 속성 설정을 간소화합니다. 
            // 즉, 모든 항목을 반복합니다.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(
                    BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable);

            setComplicationsActiveAndAmbientColors(mWatchHandHighlightColor);
            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeWatchFace() {

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandAndComplicationsColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandAndComplicationsColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondAndHighlightPaint = new Paint();
            mSecondAndHighlightPaint.setColor(mWatchHandHighlightColor);
            mSecondAndHighlightPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondAndHighlightPaint.setAntiAlias(true);
            mSecondAndHighlightPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondAndHighlightPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandAndComplicationsColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }

        /* Sets active/ambient mode colors for all complications.
         * 모든 합병증에 대해 액티브 / 앰비언트 모드 색상을 설정합니다.
         *
         * Note: With the rest of the watch face, we update the paint colors based on
         * ambient/active mode callbacks, but because the ComplicationDrawable handles
         * the active/ambient colors, we only set the colors twice. 
         * 참고 : 나머지 시계 모드에서는 다음을 기반으로 페인트 색상을 업데이트합니다.
         * 앰비언트 / 액티브 모드 콜백이 있지만 ComplicationDrawable이 활성 / 주변 색상을 처리하기 때문에 색상을 두 번만 설정합니다.
         * Once at initialization and again if the user changes the highlight color via AnalogComplicationConfigActivity.
         * 일단 초기화되면 다시 AnalogComplicationConfigActivity를 통해 강조 색상을 변경합니다.
         */
        private void setComplicationsActiveAndAmbientColors(int primaryComplicationColor) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                if (complicationId == BACKGROUND_COMPLICATION_ID) {
                    // It helps for the background color to be black in case the image used for the
                    // watch face's background takes some time to load.
                    // 시계 표면의 배경에 사용 된 이미지가로드되는 데 시간이 걸리는 경우 배경색이 검은 색이됩니다.
                    complicationDrawable.setBackgroundColorActive(Color.BLACK);
                } else {
                    // Active mode colors.
                    // 활성 모드 색상.
                    complicationDrawable.setBorderColorActive(primaryComplicationColor);
                    complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);

                    // Ambient mode colors.
                    // 주변 모드 색상.
                    complicationDrawable.setBorderColorAmbient(Color.WHITE);
                    complicationDrawable.setRangedValuePrimaryColorAmbient(Color.WHITE);
                }
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            // 복잡성을 업데이트하여 화면의 기능에 따라 주변 모드로 올바르게 렌더링합니다.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);

                complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                complicationDrawable.setBurnInProtection(mBurnInProtection);
            }
        }

        /*
         * Called when there is updated data for a complication id.
         * complication ID에 대한 업데이트 된 데이터가있을 때 호출됩니다.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            // 배열에 활성 합병증 데이터를 추가 / 업데이트합니다.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            // 업데이트 된 데이터로 ComplicationDrawable을 업데이트합니다.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TAP:

                    for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                        int complicationId = COMPLICATION_IDS[i];
                        ComplicationDrawable complicationDrawable =
                                mComplicationDrawableSparseArray.get(complicationId);

                        boolean successfulTap = complicationDrawable.onTap(x, y, eventTime);

                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

            mAmbient = inAmbientMode;

            updateWatchPaintStyles();

            // Update drawable complications' ambient state.
            // drawable complications' 의 주변 상태를 업데이트하십시오.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            // 참고 : ComplicationDrawable은 활성 / 주변 색상 간의 전환을 처리하므로 주변 모드로 전환하기 위해 알려야합니다.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            // 타이머가 실행되어야하는지 여부를 확인하고 트리거하십시오 (활성 모드에서만).
            updateTimer();
        }

        private void updateWatchPaintStyles() {
            if (mAmbient) {

                mBackgroundPaint.setColor(Color.BLACK);

                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondAndHighlightPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondAndHighlightPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondAndHighlightPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();

            } else {

                mBackgroundPaint.setColor(mBackgroundColor);

                mHourPaint.setColor(mWatchHandAndComplicationsColor);
                mMinutePaint.setColor(mWatchHandAndComplicationsColor);
                mTickAndCirclePaint.setColor(mWatchHandAndComplicationsColor);

                mSecondAndHighlightPaint.setColor(mWatchHandHighlightColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondAndHighlightPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondAndHighlightPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            /* 음소거 모드에서 희미하게 표시됩니다.*/
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondAndHighlightPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             * 화면에서 중심점의 좌표를 찾고 창 인세 트를 무시하십시오. 
             * 따라서 "턱"이있는 둥근 시계에서 시계 부분은 사용 가능한 부분이 아닌 전체 화면의 중앙에 위치합니다.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             * 시계 화면 크기에 따라 다른 손의 길이를 계산합니다.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mHourHandLength = (float) (mCenterX * 0.5);

            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * 오른쪽 및 왼쪽 순환 합병증의 위치 범위를 계산합니다. 유의하시기 바랍니다,
             * we are not demonstrating a long text complication in this watch face.
             * 우리는이 시계면에서 긴 텍스트 complication을 시연하지 않습니다.
             * 
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             * 원형 (또는 제곱) 값에 대해 화면 너비의 1/4 이상을 사용하는 것이 좋습니다.
             * 합병증 및 화면 너비의 2/3을 가독성을 위해 넓은 직사각형 합병증의 경우.
             */

            // For most Wear devices, width and height are the same, so we just chose one (width).
            // 대부분의 Wear 장치의 너비와 높이가 동일하므로 하나 (너비)를 선택했습니다.
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    // 왼쪽, 위쪽, 오른쪽, 아래쪽
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    // 왼쪽, 위쪽, 오른쪽, 아래쪽
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

            Rect screenForBackgroundBound =
                    // Left, Top, Right, Bottom
                    // 왼쪽, 위쪽, 오른쪽, 아래쪽
                    new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawComplications(canvas, now);
            drawUnreadNotificationIcon(canvas);
            drawWatchFace(canvas);
        }

        private void drawUnreadNotificationIcon(Canvas canvas) {

            if (mUnreadNotificationsPreference && (mNumberOfUnreadNotifications > 0)) {

                int width = canvas.getWidth();
                int height = canvas.getHeight();

                canvas.drawCircle(width / 2, height - 40, 10, mTickAndCirclePaint);

                /*
                 * Ensure center highlight circle is only drawn in interactive mode. This ensures
                 * 중앙 강조 표시 원이 대화식 모드로 그려지는지 확인하십시오. 
                 * we don't burn the screen with a solid circle in ambient mode.
                 * 우리는 주변 모드에서 화면을 단색 원으로 태우지 않습니다.
                 */
                if (!mAmbient) {
                    canvas.drawCircle(width / 2, height - 40, 4, mSecondAndHighlightPaint);
                }
            }
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);

            } else {
                canvas.drawColor(mBackgroundColor);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        private void drawWatchFace(Canvas canvas) {
            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             * 일반적으로 사진에 직접 굽고 싶지만
             * 사용자가 자신의 사진을 선택할 수있게하려는 경우에는 사진 위에 동적으로 생성됩니다.
             */
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(
                        mCenterX + innerX,
                        mCenterY + innerY,
                        mCenterX + outerX,
                        mCenterY + outerY,
                        mTickAndCirclePaint);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             * 이러한 계산은 360 / 60 = 6 및 360 / 12 = 30과 같이 단위 시간당 각도로 회전을 반영합니다.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             * 캔버스 상태를 회전하기 전에 캔버스 상태를 저장하십시오.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * 대화 형 모드 일 때만 "초"가 그려지는지 확인하십시오.
             * Otherwise, we only update the watch face once a minute.
             * 그렇지 않은 경우 1 분에 한 번만 시계 모드를 업데이트합니다.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondAndHighlightPaint);
            }
            canvas.drawCircle(
                    mCenterX, mCenterY, CENTER_GAP_AND_CIRCLE_RADIUS, mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
            /* 캔버스의 원래 방향을 복원합니다. */
            canvas.restore();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {

                // Preferences might have changed since last time watch face was visible.
                // 마지막 시계 표면이 보였던 이후 환경 설정이 변경되었을 수 있습니다.
                loadSavedPreferences();

                // With the rest of the watch face, we update the paint colors based on
                // ambient/active mode callbacks, but because the ComplicationDrawable handles
                // the active/ambient colors, we only need to update the complications' colors when
                // the user actually makes a change to the highlight color, not when the watch goes
                // in and out of ambient mode.
                // 시계 모드의 나머지 부분에서는 앰비언트 / 액티브 모드 콜백을 기반으로 페인트 색상을 업데이트하지만 
                // ComplicationDrawable이 활성 / 주변 색상을 처리하기 때문에 사용자가 실제로 변경 한 경우 합병증의 색상 만 업데이트하면됩니다. 
                // 시계가 주변 모드로 들어가거나 나올 때가 아니라 색상을 강조 표시하십시오.
                setComplicationsActiveAndAmbientColors(mWatchHandHighlightColor);
                updateWatchPaintStyles();

                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                // 표시되지 않는 동안 변경된 경우 시간대를 업데이트하십시오.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            /* 타이머가 실행되어야하는지 여부를 확인하고 트리거하십시오 (활성 모드에서만). */
            updateTimer();
        }

        @Override
        public void onUnreadCountChanged(int count) {
            Log.d(TAG, "onUnreadCountChanged(): " + count);

            if (mUnreadNotificationsPreference) {

                if (mNumberOfUnreadNotifications != count) {
                    mNumberOfUnreadNotifications = count;
                    invalidate();
                }
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogComplicationWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            AnalogComplicationWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         * 시계 모드의 상태를 기반으로 {@link #mUpdateTimeHandler} 타이머를 시작 / 정지합니다.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running.
         * {@link #mUpdateTimeHandler} 타이머가 실행되고 있을지 어떨지를 돌려줍니다.
         * The timer should only run in active mode.
         * 타이머는 활성 모드로만 실행해야합니다.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }
    }
}
