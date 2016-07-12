/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Secure;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Top level fragment for gesture settings.
 * This will create individual switch preference for each gesture and handle updates when each
 * preference is updated
 */
public class GestureSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "GestureSettings";
    private static final String PREF_KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";
    private static final String PREF_KEY_DOUBLE_TWIST = "gesture_double_twist";
    private static final String PREF_KEY_PICK_UP_AND_NUDGE = "gesture_pick_up_and_nudge";
    private static final String PREF_KEY_SWIPE_DOWN_FINGERPRINT = "gesture_swipe_down_fingerprint";
    private static final String DEBUG_DOZE_COMPONENT = "debug.doze.component";
    private static final String ARG_SCROLL_TO_PREFERENCE = "gesture_scroll_to_preference";

    private int mScrollPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gesture_settings);
        Context context = getActivity();

         // Double tap power for camera
        if (isCameraDoubleTapPowerGestureAvailable(getResources())) {
            int cameraDisabled = Secure.getInt(
                    getContentResolver(), Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_DOUBLE_TAP_POWER);
            preference.setChecked(cameraDisabled == 0);
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_DOUBLE_TAP_POWER);
        }

        // Ambient Display
        if (isDozeAvailable(context)) {
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_PICK_UP_AND_NUDGE);
            int dozeEnabled = Secure.getInt(getContentResolver(), Secure.DOZE_ENABLED, 1);
            preference.setChecked(dozeEnabled != 0);
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_PICK_UP_AND_NUDGE);
        }

        // Fingerprint slide for notifications
        if (isSystemUINavigationAvailable(context)) {
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_SWIPE_DOWN_FINGERPRINT);
            preference.setChecked(isSystemUINavigationEnabled(context));
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_SWIPE_DOWN_FINGERPRINT);
        }

        // Double twist for camera mode
        if (isDoubleTwistAvailable(context)) {
            int doubleTwistEnabled = Secure.getInt(
                    getContentResolver(), Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1);
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_DOUBLE_TWIST);
            preference.setChecked(doubleTwistEnabled != 0);
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_DOUBLE_TWIST);
        }

        if (savedInstanceState == null) {
            final Bundle args = getArguments();
            if (args != null && args.containsKey(ARG_SCROLL_TO_PREFERENCE)) {
                String prefKey = args.getString(ARG_SCROLL_TO_PREFERENCE);
                GesturePreference pref = (GesturePreference) findPreference(prefKey);
                if (pref != null) {
                    mScrollPosition = pref.getOrder();
                }
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (mScrollPosition >= 0) {
            getListView().scrollToPosition(mScrollPosition);
        }
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        String key = preference.getKey();
        if (PREF_KEY_DOUBLE_TAP_POWER.equals(key)) {
            Secure.putInt(getContentResolver(),
                    Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, enabled ? 0 : 1);
        } else if (PREF_KEY_PICK_UP_AND_NUDGE.equals(key)) {
            Secure.putInt(getContentResolver(), Secure.DOZE_ENABLED, enabled ? 1 : 0);
        } else if (PREF_KEY_SWIPE_DOWN_FINGERPRINT.equals(key)) {
            Secure.putInt(getContentResolver(),
                    Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, enabled ? 1 : 0);
        } else if (PREF_KEY_DOUBLE_TWIST.equals(key)) {
            Secure.putInt(getContentResolver(),
                    Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, enabled ? 1 : 0);
        }
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_gestures;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.SETTINGS_GESTURES;
    }

    private static boolean isCameraDoubleTapPowerGestureAvailable(Resources res) {
        return res.getBoolean(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get(DEBUG_DOZE_COMPONENT) : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isSystemUINavigationAvailable(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    private static boolean isSystemUINavigationEnabled(Context context) {
        return Secure.getInt(context.getContentResolver(), Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 1)
                == 1;
    }

    private static boolean isDoubleTwistAvailable(Context context) {
        Resources resources = context.getResources();
        String name = resources.getString(R.string.gesture_double_twist_sensor_name);
        String vendor = resources.getString(R.string.gesture_double_twist_sensor_vendor);
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(vendor)) {
            SensorManager sensorManager =
                    (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
                if (name.equals(s.getName()) && vendor.equals(s.getVendor())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                     boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                        new ArrayList<SearchIndexableResource>();

                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.gesture_settings;
                result.add(sir);

                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                ArrayList<String> result = new ArrayList<String>();
                if (!isCameraDoubleTapPowerGestureAvailable(context.getResources())) {
                    result.add(PREF_KEY_DOUBLE_TAP_POWER);
                }
                if (!isDozeAvailable(context)) {
                    result.add(PREF_KEY_PICK_UP_AND_NUDGE);
                }
                if (!isSystemUINavigationAvailable(context)) {
                    result.add(PREF_KEY_SWIPE_DOWN_FINGERPRINT);
                }
                return result;
            }
        };

}