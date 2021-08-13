package com.juphoon.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.messaging.R;


public class RcsConfigHelper {

    public static final int SETTINGS_SEND_DEFAULT_MESSAGE = 0;
    public static final int SETTINGS_IMAGE_COMPRESSION = 1;
    public static final int SETTINGS_VIDEO_COMPRESSION = 2;
    public static final int SETTINGS_CHANGE_TO_SMS_WHEN_FAILED = 3;
    public static final int SETTINGS_SEND_MESSAGE_TYPE = 4;

    public static final int RCS_SWITCH_OFF = 0;
    public static final int RCS_SWITCH_ON = 1;


    public static class DefaultCountry {
        private static final String RCS_DEFAULT_COUNTRY = "rcs_default_country";

        public static boolean saveValue(Context context, String value) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().putString(RCS_DEFAULT_COUNTRY, value).commit();
        }

        public static String getValue(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String countryNumber = prefs.getString(RCS_DEFAULT_COUNTRY, "");
            return !TextUtils.isEmpty(countryNumber) ? countryNumber : "86";
        }
    }

    /* **************** only use in ui*************** */
    public static class CompressVideoUI {
        public static final int ORIGINAL = 1;
        public static final int COMPRESSION = 2;

        private static final String RCS_COMPRESS_VIDEO_UI = "rcs_compress_video_ui";

        public static boolean saveValue(Context context, int value) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().putInt(RCS_COMPRESS_VIDEO_UI, value).commit();
        }

        public static int getValue(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getInt(RCS_COMPRESS_VIDEO_UI, ORIGINAL);
        }
    }

    public static class CompressImageUI {
        public static final int ORIGINAL = 1;
        public static final int HIGH = 2;
        public static final int MEDIUM = 3;
        public static final int LOW = 4;

        private static final String RCS_COMPRESS_IMAGE_UI = "rcs_compress_image_ui";

        public static boolean saveValue(Context context, int value) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().putInt(RCS_COMPRESS_IMAGE_UI, value).commit();
        }

        public static int getValue(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getInt(RCS_COMPRESS_IMAGE_UI, ORIGINAL);
        }
    }


    public static class ShowDialogTime {
        private static final String RCS_SHOW_DIALOG = "rcs_show_dialog";

        public static boolean saveValue(Context context, long value) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().putLong(RCS_SHOW_DIALOG, value).commit();
        }

        public static Long getValue(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getLong(RCS_SHOW_DIALOG, System.currentTimeMillis());
        }
    }

    public static class LastAccount {
        private static final String RCS_LAST_ACCOUNT = "rcs_last_account";

        public static boolean saveAccount(Context context, String value) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().putString(RCS_LAST_ACCOUNT, value).commit();
        }

        public static String getAccount(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getString(RCS_LAST_ACCOUNT, "");
        }
    }
}
