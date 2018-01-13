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
package com.example.android.wearable.watchface.model;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.RecyclerView;

import com.example.android.wearable.watchface.R;
import com.example.android.wearable.watchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import com.example.android.wearable.watchface.config.ColorSelectionActivity;
import com.example.android.wearable.watchface.config.AnalogComplicationConfigActivity;
import com.example.android.wearable.watchface.watchface.AnalogComplicationWatchFaceService;

import java.util.ArrayList;

/**
 * Data represents different views for configuring the
 * {@link AnalogComplicationWatchFaceService} watch face's appearance and complications
 * via {@link AnalogComplicationConfigActivity}.
 * 데이터는 {@link AnalogComplicationConfigActivity}활동을 통해
 * {@link AnalogComplicationWatchFaceService} 시계 외관 및 complication 을 구성하는
 * 다양한 보기를 나타냅니다.
 */
public class AnalogComplicationConfigData {


    /**
     * Interface all ConfigItems must implement so the {@link RecyclerView}'s Adapter associated
     * with the configuration activity knows what type of ViewHolder to inflate.
     * 모든 Configltem 인터페이스가 구현되어 있어야 하므로 {@link RecyclerView} 의 어댑터가 연결 됩니다.
     * 구성활동과 함께 어떤 종류의 ViewHolder를 부풀릴 것인지를 압니다.
     */
    public interface ConfigItemType {
        int getConfigType();
    }

    /**
     * Returns Watch Face Service class associated with configuration Activity.
     * 구성 활동과 관련된 Watch Face Service 클래스를 반환합니다.
     */
    public static Class getWatchFaceServiceClass() {
        return AnalogComplicationWatchFaceService.class;
    }

    /**
     * Returns Material Design color options.
     * 디자인 색 옵션들 반환
     */
    public static ArrayList<Integer> getColorOptionsDataSet() {
        ArrayList<Integer> colorOptionsDataSet = new ArrayList<>();
        colorOptionsDataSet.add(Color.parseColor("#FFFFFF")); // White 흰색

        colorOptionsDataSet.add(Color.parseColor("#FFEB3B")); // Yellow 노란색
        colorOptionsDataSet.add(Color.parseColor("#FFC107")); // Amber 호박색
        colorOptionsDataSet.add(Color.parseColor("#FF9800")); // Orange 오랜지색
        colorOptionsDataSet.add(Color.parseColor("#FF5722")); // Deep Orange 진한 오랜지색

        colorOptionsDataSet.add(Color.parseColor("#F44336")); // Red 빨간색
        colorOptionsDataSet.add(Color.parseColor("#E91E63")); // Pink 핑크색

        colorOptionsDataSet.add(Color.parseColor("#9C27B0")); // Purple 보라색
        colorOptionsDataSet.add(Color.parseColor("#673AB7")); // Deep Purple 진한 보라색
        colorOptionsDataSet.add(Color.parseColor("#3F51B5")); // Indigo 남색
        colorOptionsDataSet.add(Color.parseColor("#2196F3")); // Blue 파란색
        colorOptionsDataSet.add(Color.parseColor("#03A9F4")); // Light Blue 하늘색

        colorOptionsDataSet.add(Color.parseColor("#00BCD4")); // Cyan 옥색
        colorOptionsDataSet.add(Color.parseColor("#009688")); // Teal 틸색
        colorOptionsDataSet.add(Color.parseColor("#4CAF50")); // Green 녹색
        colorOptionsDataSet.add(Color.parseColor("#8BC34A")); // Lime Green 라임녹색
        colorOptionsDataSet.add(Color.parseColor("#CDDC39")); // Lime 라임색

        colorOptionsDataSet.add(Color.parseColor("#607D8B")); // Blue Grey 블루 그레이색
        colorOptionsDataSet.add(Color.parseColor("#9E9E9E")); // Grey 회색
        colorOptionsDataSet.add(Color.parseColor("#795548")); // Brown 갈색
        colorOptionsDataSet.add(Color.parseColor("#000000")); // Black 검정색

        return colorOptionsDataSet;
    }

    /**
     * Includes all data to populate each of the 5 different custom
     * 5가지 사용자 정의 각각을 채우기 위한 모든 데이터를 포함합니다.
     * {@link ViewHolder} types in {@link AnalogComplicationConfigRecyclerViewAdapter}.
     * {@link AnalogComplicationConfigRecyclerViewAdapter}의 {@link ViewHolder}타입.
     */
    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face preview and complications UX in settings Activity.
        // 시계 동작 미리보기 및 설정 관련 작업의 UX에 대한 데이터
        ConfigItemType complicationConfigItem =
                new PreviewAndComplicationsConfigItem(R.drawable.add_complication);
        settingsConfigData.add(complicationConfigItem);

        // Data for "more options" UX in settings Activity.
        // 설정 활동의 "추가 옵션"UX에 대한 데이터
        ConfigItemType moreOptionsConfigItem =
                new MoreOptionsConfigItem(R.drawable.ic_expand_more_white_18dp);
        settingsConfigData.add(moreOptionsConfigItem);

        // Data for highlight/marker (second hand) color UX in settings Activity.
        // 설정 활동의 하이라이트 / 마커 (초침) 컬러 UX에 대한 데이터.
        ConfigItemType markerColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_marker_color_label),
                        R.drawable.icn_styles,
                        context.getString(R.string.saved_marker_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(markerColorConfigItem);

        // Data for Background color UX in settings Activity.
        // 설정 활동에서 배경색 UX에 대한 데이터.
        ConfigItemType backgroundColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_background_color_label),
                        R.drawable.icn_styles,
                        context.getString(R.string.saved_background_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(backgroundColorConfigItem);

        // Data for 'Unread Notifications' UX (toggle) in settings Activity.
        // 설정 활동의 '읽지 않은 알림'UX (토글) 데이터
        ConfigItemType unreadNotificationsConfigItem =
                new UnreadNotificationConfigItem(
                        context.getString(R.string.config_unread_notifications_label),
                        R.drawable.ic_notifications_white_24dp,
                        R.drawable.ic_notifications_off_white_24dp,
                        R.string.saved_unread_notifications_pref);
        settingsConfigData.add(unreadNotificationsConfigItem);

        // Data for background complications UX in settings Activity.
        // 백그라운드 복잡성에 대한 데이터 UX 설정 활동.
        ConfigItemType backgroundImageComplicationConfigItem =
                // TODO (jewalker): Revised in another CL to support background complication.
                new BackgroundComplicationConfigItem(
                        context.getString(R.string.config_background_image_complication_label),
                        R.drawable.ic_landscape_white);
        settingsConfigData.add(backgroundImageComplicationConfigItem);

        return settingsConfigData;
    }

    /**
     * Data for Watch Face Preview with Complications Preview item in RecyclerView.
     * 프리뷰 데이터 RecyclerView의 항목 미리보기.
     */
    public static class PreviewAndComplicationsConfigItem implements ConfigItemType {

        private int defaultComplicationResourceId;

        PreviewAndComplicationsConfigItem(int defaultComplicationResourceId) {
            this.defaultComplicationResourceId = defaultComplicationResourceId;
        }

        public int getDefaultComplicationResourceId() {
            return defaultComplicationResourceId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG;
        }
    }

    /**
     * Data for "more options" item in RecyclerView.
     * RecyclerView의 "추가 옵션"항목에 대한 데이터.
     */
    public static class MoreOptionsConfigItem implements ConfigItemType {

        private int iconResourceId;

        MoreOptionsConfigItem(int iconResourceId) {
            this.iconResourceId = iconResourceId;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_MORE_OPTIONS;
        }
    }

    /**
     * Data for color picker item in RecyclerView.
     * RecyclerView의 색상 선택기 항목에 대한 데이터입니다.
     */
    public static class ColorConfigItem  implements ConfigItemType {

        private String name;
        private int iconResourceId;
        private String sharedPrefString;
        private Class<ColorSelectionActivity> activityToChoosePreference;

        ColorConfigItem(
                String name,
                int iconResourceId,
                String sharedPrefString,
                Class<ColorSelectionActivity> activity) {
            this.name = name;
            this.iconResourceId = iconResourceId;
            this.sharedPrefString = sharedPrefString;
            this.activityToChoosePreference = activity;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        public String getSharedPrefString() {
            return sharedPrefString;
        }

        public Class<ColorSelectionActivity> getActivityToChoosePreference() {
            return activityToChoosePreference;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_COLOR_CONFIG;
        }
    }

    /**
     * Data for Unread Notification preference picker item in RecyclerView.
     * RecyclerView의 읽지 않은 상태 환경 설정 선택기 항목의 데이터입니다.
     */
    public static class UnreadNotificationConfigItem  implements ConfigItemType {

        private String name;
        private int iconEnabledResourceId;
        private int iconDisabledResourceId;
        private int sharedPrefId;

        UnreadNotificationConfigItem(
                String name,
                int iconEnabledResourceId,
                int iconDisabledResourceId,
                int sharedPrefId) {
            this.name = name;
            this.iconEnabledResourceId = iconEnabledResourceId;
            this.iconDisabledResourceId = iconDisabledResourceId;
            this.sharedPrefId = sharedPrefId;
        }

        public String getName() {
            return name;
        }

        public int getIconEnabledResourceId() {
            return iconEnabledResourceId;
        }

        public int getIconDisabledResourceId() {
            return iconDisabledResourceId;
        }

        public int getSharedPrefId() {
            return sharedPrefId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_UNREAD_NOTIFICATION_CONFIG;
        }
    }

    /**
     * Data for background image complication picker item in RecyclerView.
     * RecyclerView의 배경 이미지 복잡성 피커 항목에 대한 데이터.
     */
    public static class BackgroundComplicationConfigItem  implements ConfigItemType {

        private String name;
        private int iconResourceId;

        BackgroundComplicationConfigItem(
                String name,
                int iconResourceId) {

            this.name = name;
            this.iconResourceId = iconResourceId;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG;
        }
    }
}