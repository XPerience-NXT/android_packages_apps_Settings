/*
 * Copyright (C) 2012 The CyanogenMod project
 * Copyright (C) 2014 The XPerience project
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

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemUiSettings extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String CATEGORY_GENERAL = "cat_general";
    private static final String CATEGORY_EXTRA = "cat_extra";
    private static final String CATEGORY_NAVBAR = "navigation_bar";

    // ListView Animations Key
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";

    // Enable/disable nav bar  
    private static final String ENABLE_NAVIGATION_BAR = "enable_nav_bar";

    // ListView Animations Preference
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;

    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private CheckBoxPreference mNavigationBarLeftPref;

    // Enable/disable nav bar
    private CheckBoxPreference mEnableNavigationBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory generalCat =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_GENERAL);
        final PreferenceCategory extraCat =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_EXTRA);

        // ListView Animations
        mListViewAnimation = (ListPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_ANIMATION, 1);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);

        // Expanded desktop
        mExpandedDesktopPref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopNoNavbarPref =
                (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);

        // Navigation bar left
        mNavigationBarLeftPref = (CheckBoxPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                extraCat, KEY_SCREEN_GESTURE_SETTINGS);

        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
                Settings.System.EXPANDED_DESKTOP_STYLE, 0);

        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

            if (hasNavBar) {
                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
                updateExpandedDesktop(expandedDesktopValue);
                generalCat.removePreference(mExpandedDesktopNoNavbarPref);

                if (!Utils.isPhone(getActivity())) {
                    PreferenceCategory navCategory =
                            (PreferenceCategory) findPreference(CATEGORY_NAVBAR);
                    navCategory.removePreference(mNavigationBarLeftPref);
                }
            } else {
                // Hide no-op "Status bar visible" expanded desktop mode
                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
                generalCat.removePreference(mExpandedDesktopPref);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
        // Booleans to enable/disable nav bar
        // overriding overlays
        boolean hasNavBarByDefault = getResources().getBoolean(
                    com.android.internal.R.bool.config_showNavigationBar);
        boolean enableNavigationBar = Settings.System.getInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_SHOW, hasNavBarByDefault ? 1 : 0) == 1;
        mEnableNavigationBar = (CheckBoxPreference) findPreference(ENABLE_NAVIGATION_BAR);
        mEnableNavigationBar.setChecked(enableNavigationBar);
        mEnableNavigationBar.setOnPreferenceChangeListener(this);
    
        updateNavbarPreferences(enableNavigationBar);
    }

    // Enable/disbale nav bar
    private void updateNavbarPreferences(boolean show) {}

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getContentResolver();
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            updateExpandedDesktop(expandedDesktopValue);
            return true;
        } else if (preference == mExpandedDesktopNoNavbarPref) {
            boolean value = (Boolean) objValue;
            updateExpandedDesktop(value ? 2 : 0);
            return true;
            // Enable/disable nav bar (used in custom nav bar dimensions)
        } else if (preference == mEnableNavigationBar) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_SHOW,
                    ((Boolean) objValue) ? 1 : 0);
            updateNavbarPreferences((Boolean) objValue);
            return true;
        } else if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver, Settings.System.LISTVIEW_ANIMATION,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            return true;
        } else if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver, Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
            return true; 
}
        return false;
    }

    private void updateExpandedDesktop(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
            summary = R.string.expanded_desktop_disabled;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }
}
