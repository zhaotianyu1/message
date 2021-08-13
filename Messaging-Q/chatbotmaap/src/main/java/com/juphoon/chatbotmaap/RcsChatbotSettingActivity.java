package com.juphoon.chatbotmaap;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;

import com.juphoon.helper.RcsChatbotHelper;


public class RcsChatbotSettingActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    public final static String KEY_CHATBOT_NUMER = "chatbot_number";
    public static final String KEY_COMPLAIN_SETTINGS = "pref_key_settings_complain";
    public static final String KEY_ADD_BLACK_SETTINGS = "pref_key_settings_add_to_black";
    public static final String KEY_MUTE_NOTIFY = "pref_key_settings_recv_notify";
    private Preference mComplainPreference;
    private SwitchPreference mAdd2BlackPreference, mMuteNotifyPreference;
    private RcsChatbotHelper.RcsChatbot mChatbotInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);
        mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(getIntent().getStringExtra(KEY_CHATBOT_NUMER));
        initView();
        addPreferencesFromResource(R.xml.chatbot_activity_setting);
        initPreferences();
    }

    private void initPreferences() {
        mComplainPreference = findPreference(KEY_COMPLAIN_SETTINGS);
        mAdd2BlackPreference = (SwitchPreference) findPreference(KEY_ADD_BLACK_SETTINGS);
        mComplainPreference.setOnPreferenceClickListener(this);
        mComplainPreference.setOnPreferenceChangeListener(this);
        if (mChatbotInfo.special) {
            mAdd2BlackPreference.setEnabled(false);
        }
        mAdd2BlackPreference.setOnPreferenceChangeListener(this);
        mAdd2BlackPreference.setChecked(RcsChatbotHelper.isLocalBlockChatbot(mChatbotInfo.serviceId));
        mMuteNotifyPreference = (SwitchPreference) findPreference(KEY_MUTE_NOTIFY);
        mMuteNotifyPreference.setOnPreferenceChangeListener(this);
        mMuteNotifyPreference.setChecked(mChatbotInfo.muteNotify);

    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(R.string.chatbot_setting);
    }

    public static void startActivity(Context context, String chatbotNumber) {
        Intent intent = new Intent(context, RcsChatbotSettingActivity.class);
        intent.putExtra(RcsChatbotSettingActivity.KEY_CHATBOT_NUMER, chatbotNumber);
        context.startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_COMPLAIN_SETTINGS:
                Intent intent = new Intent(RcsChatbotSettingActivity.this, RcsChatbotComplaintActivity.class);
                intent.putExtra(RcsChatbotComplaintActivity.SERVICEID, mChatbotInfo.serviceId);
                startActivity(intent);
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_ADD_BLACK_SETTINGS:
                boolean switchOn = mAdd2BlackPreference.isChecked();
                if (!switchOn) {
                    if (mChatbotInfo.special) {
                        Toast toast = Toast.makeText(RcsChatbotSettingActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(getString(R.string.chatbot_unable_add_blacklist));
                        toast.show();
                        return false;
                    }
                    mAdd2BlackPreference.setChecked(true);
                    updateChatbotToBlackList(mChatbotInfo.serviceId, true);
                } else {
                    mAdd2BlackPreference.setChecked(false);
                    updateChatbotToBlackList(mChatbotInfo.serviceId, false);
                }
                break;
            case KEY_MUTE_NOTIFY:
                boolean muteNotify = mMuteNotifyPreference.isChecked();
                if (!muteNotify) {
                    mMuteNotifyPreference.setChecked(true);
                    updateChatbotMuteNotify(mChatbotInfo.serviceId, true);
                } else {
                    mMuteNotifyPreference.setChecked(false);
                    updateChatbotMuteNotify(mChatbotInfo.serviceId, false);
                }
                break;
            default:
                break;
        }
        return false;
    }

    private void updateChatbotToBlackList(String number, final boolean black) {
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                return RcsChatbotHelper.updateChatbotToBlock(strings[0], Boolean.parseBoolean(strings[1]));
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    if (black) {
                        Toast toast = Toast.makeText(RcsChatbotSettingActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_add_blacklist));
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(RcsChatbotSettingActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_remove_blacklist));
                        toast.show();
                    }
                }
            }
        }.execute(number, String.valueOf(black));
    }

    private void updateChatbotMuteNotify(String number, final boolean muteNotify) {
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                return RcsChatbotHelper.updateChatbotMuteNotify(strings[0], Boolean.parseBoolean(strings[1]));
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    if (muteNotify) {
                        Toast toast = Toast.makeText(RcsChatbotSettingActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_set_mute_notify));
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(RcsChatbotSettingActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_cancel_mute_notify));
                        toast.show();
                    }
                }
            }
        }.execute(number, String.valueOf(muteNotify));
    }
}
