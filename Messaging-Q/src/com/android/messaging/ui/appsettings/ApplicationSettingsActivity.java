/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.messaging.ui.appsettings;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import androidx.core.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.LicenseActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.rcs.JApplication;
import com.juphoon.ui.RcsConfigHelper;
import com.juphoon.ui.RcsRegisterActivity;

public class ApplicationSettingsActivity extends BugleActionBarActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final boolean topLevel = getIntent().getBooleanExtra(
                UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS, false);
        if (topLevel) {
            getSupportActionBar().setTitle(getString(R.string.settings_activity_title));
        }
        Log.i("zty","onCreate-------ApplicationSettingsActivity");
        receiveRepeatLogin(getIntent());

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, new ApplicationSettingsFragment());
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        Log.i("zty","onCreateOptionsMenu-------");
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveRepeatLogin(intent);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Log.i("zty","home-------");
            NavUtils.navigateUpFromSameTask(this);
            return true;
        case R.id.action_license:
            Log.i("zty","action_license-------");
            final Intent intent = new Intent(this, LicenseActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ApplicationSettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private String mNotificationsEnabledPreferenceKey;
        private TwoStatePreference mNotificationsEnabledPreference;
        private String mRingtonePreferenceKey;
        private RingtonePreference mRingtonePreference;
        private Preference mVibratePreference;
        private String mSmsDisabledPrefKey;
        private Preference mSmsDisabledPreference;
        private String mSmsEnabledPrefKey;
        private Preference mSmsEnabledPreference;
        private boolean mIsSmsPreferenceClicked;
        private String mRcsloginEnablePreferenceKey;
        private SwitchPreference mRcsloginEnablePreference;

        public ApplicationSettingsFragment() {
            // Required empty constructor
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(BuglePrefs.SHARED_PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.preferences_application);
            mRcsloginEnablePreferenceKey = getString(R.string.rcs_login_pref_key);
            mRcsloginEnablePreference = (SwitchPreference) findPreference(mRcsloginEnablePreferenceKey);
            mNotificationsEnabledPreferenceKey =
                    getString(R.string.notifications_enabled_pref_key);
            mNotificationsEnabledPreference = (TwoStatePreference) findPreference(
                    mNotificationsEnabledPreferenceKey);
            mRingtonePreferenceKey = getString(R.string.notification_sound_pref_key);
            mRingtonePreference = (RingtonePreference) findPreference(mRingtonePreferenceKey);
            mVibratePreference = findPreference(
                    getString(R.string.notification_vibration_pref_key));
           // mSmsDisabledPrefKey = getString(R.string.sms_disabled_pref_key);
           // mSmsDisabledPreference = findPreference(mSmsDisabledPrefKey);
          //  mSmsEnabledPrefKey = getString(R.string.sms_enabled_pref_key);
           // mSmsEnabledPreference = findPreference(mSmsEnabledPrefKey);
            mIsSmsPreferenceClicked = false;
            RcsServiceManager.addCallBack(mRcsServiceCallBack);
            final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            updateSoundSummary(prefs);

            if (!DebugUtils.isDebugEnabled()) {
                final Preference debugCategory = findPreference(getString(
                        R.string.debug_pref_key));
                getPreferenceScreen().removePreference(debugCategory);
            }
            if (RcsCallWrapper.rcsIsLogined() && !TextUtils.isEmpty(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext))) {
                mRcsloginEnablePreference.setSummary(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext));
            } else {
                mRcsloginEnablePreference.setSummary(getString(R.string.chatbot_not_login));
            }
            final PreferenceScreen advancedScreen = (PreferenceScreen) findPreference(
                    getString(R.string.advanced_pref_key));
            final boolean topLevel = getActivity().getIntent().getBooleanExtra(
                    UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS, false);
            if (topLevel) {
                advancedScreen.setIntent(UIIntents.get()
                        .getAdvancedSettingsIntent(getPreferenceScreen().getContext()));
            } else {
                // Hide the Advanced settings screen if this is not top-level; these are shown at
                // the parent SettingsActivity.
                getPreferenceScreen().removePreference(advancedScreen);
            }
            // juphoon 设置版本号
            final PreferenceScreen rcsVersionScreen = (PreferenceScreen) findPreference(getString(
                    R.string.rcs_pref_key));
            rcsVersionScreen.setTitle(getString(R.string.pref_rcs_title) + RcsMmsUtils.getRcsAppVersionName(getContext()));
            final PreferenceScreen rcsTestVersionScreen = (PreferenceScreen) findPreference(getString(
                    R.string.rcs_test_pref_key));

        }

        @Override
        public boolean onPreferenceTreeClick (PreferenceScreen preferenceScreen,
                Preference preference) {
           // if (preference.getKey() == mSmsDisabledPrefKey ||
           //         preference.getKey() == mSmsEnabledPrefKey) {
            mIsSmsPreferenceClicked = true;
            if (preference.getKey() == getString(R.string.rcs_pref_key)) {
            //juphoon
                RcsMmsUtils.startRcsSettingActivity();
            } else if (preference.getKey() == getString(R.string.rcs_test_pref_key)) {
                RcsMmsUtils.startRcsTestSettingActivity();
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        private void updateSoundSummary(final SharedPreferences sharedPreferences) {
            // The silent ringtone just returns an empty string
            String ringtoneName = mRingtonePreference.getContext().getString(
                    R.string.silent_ringtone);

            String ringtoneString = sharedPreferences.getString(mRingtonePreferenceKey, null);

            // Bootstrap the default setting in the preferences so that we have a valid selection
            // in the dialog the first time that the user opens it.
            if (ringtoneString == null) {
                ringtoneString = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(mRingtonePreferenceKey, ringtoneString);
                editor.apply();
            }

            if (!TextUtils.isEmpty(ringtoneString)) {
                final Uri ringtoneUri = Uri.parse(ringtoneString);
                final Ringtone tone = RingtoneManager.getRingtone(mRingtonePreference.getContext(),
                        ringtoneUri);

                if (tone != null) {
                    ringtoneName = tone.getTitle(mRingtonePreference.getContext());
                }
            }

            mRingtonePreference.setSummary(ringtoneName);
        }

        private void updateSmsEnabledPreferences() {
            if (!OsUtil.isAtLeastKLP()) {
              //  getPreferenceScreen().removePreference(mSmsDisabledPreference);
              //  getPreferenceScreen().removePreference(mSmsEnabledPreference);
            } else {
                final String defaultSmsAppLabel = getString(R.string.default_sms_app,
                        PhoneUtils.getDefault().getDefaultSmsAppLabel());
                boolean isSmsEnabledBeforeState;
                boolean isSmsEnabledCurrentState;
                if (PhoneUtils.getDefault().isDefaultSmsApp()) {
//                    if (getPreferenceScreen().findPreference(mSmsEnabledPrefKey) == null) {
//                    //    getPreferenceScreen().addPreference(mSmsEnabledPreference);
//                        isSmsEnabledBeforeState = false;
//                    } else {
//                        isSmsEnabledBeforeState = true;
//                    }
//                    isSmsEnabledCurrentState = true;
                 //   getPreferenceScreen().removePreference(mSmsDisabledPreference);
                   // mSmsEnabledPreference.setSummary(defaultSmsAppLabel);
                } else {
//                    if (getPreferenceScreen().findPreference(mSmsDisabledPrefKey) == null) {
//                        getPreferenceScreen().addPreference(mSmsDisabledPreference);
//                        isSmsEnabledBeforeState = true;
//                    } else {
//                        isSmsEnabledBeforeState = false;
//                    }
                    isSmsEnabledCurrentState = false;
               //     getPreferenceScreen().removePreference(mSmsEnabledPreference);
               //     mSmsDisabledPreference.setSummary(defaultSmsAppLabel);
                }
                updateNotificationsPreferences();
            }
            mIsSmsPreferenceClicked = false;
        }

        private void updateNotificationsPreferences() {
            final boolean canNotify = !OsUtil.isAtLeastKLP()
                    || PhoneUtils.getDefault().isDefaultSmsApp();
            mNotificationsEnabledPreference.setEnabled(canNotify);
        }

        @Override
        public void onStart() {
            super.onStart();
            // We do this on start rather than on resume because the sound picker is in a
            // separate activity.
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            boolean useRcs = getPreferenceScreen().getSharedPreferences().getBoolean(mRcsloginEnablePreferenceKey, false);
            mRcsloginEnablePreference.setChecked(useRcs);
            updateSmsEnabledPreferences();
            updateNotificationsPreferences();
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                final String key) {
            if (key.equals(mNotificationsEnabledPreferenceKey)) {
                Log.i("zty","通知----");
                updateNotificationsPreferences();
            } else if (key.equals(mRingtonePreferenceKey)) {
                Log.i("zty","mRingtonePreferenceKey");
                updateSoundSummary(sharedPreferences);
            } else if (key.equals(mRcsloginEnablePreferenceKey)) {
                Log.i("zty","mRcsloginEnablePreferenceKey");
                updateRcsLoginPreferencens(sharedPreferences);
            }
        }

        private void updateRcsLoginPreferencens(SharedPreferences sharedPreferences) {
            boolean useRcs = sharedPreferences.getBoolean(mRcsloginEnablePreferenceKey, false);
            if (useRcs) {
                Intent intent = new Intent(getContext(), RcsRegisterActivity.class);
                startActivityForResult(intent, 1000);
            } else {
                RcsConfigHelper.LastAccount.saveAccount(getContext(), "");
                RcsCallWrapper.rcsLogout();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 1000) {
                if (resultCode == 1) {
                    Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "失败", Toast.LENGTH_SHORT).show();
                    mRcsloginEnablePreference.setChecked(false);
                }
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            RcsServiceManager.removeCallBack(mRcsServiceCallBack);
        }

        private final RcsServiceManager.IServiceManagerCallback mRcsServiceCallBack = new RcsServiceManager.IServiceManagerCallback() {
            @Override
            public void onLoginStateChange(final boolean logined) {
                if (logined && !TextUtils.isEmpty(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext))) {
                    mRcsloginEnablePreference.setSummary(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext));
                } else {
                    mRcsloginEnablePreference.setSummary(getString(R.string.chatbot_not_login));
                }
            }
        };
    }
    
    private void receiveRepeatLogin(Intent intent) {
        if (intent != null && TextUtils.equals(intent.getAction(), RcsJsonParamConstants.RCS_JSON_ACTION_REPEAT_LOGIN)) {
            SharedPreferences sharedPreferences = getSharedPreferences(BuglePrefs.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.rcs_login_pref_key), false);
            editor.commit();
            RcsCallWrapper.rcsLogout();
            RcsConfigHelper.LastAccount.saveAccount(RcsMmsInitHelper.getContext(), "");
            Toast.makeText(RcsMmsInitHelper.getContext(), R.string.user_kicked_offline, Toast.LENGTH_LONG).show();
        }
    }
}
