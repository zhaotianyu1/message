package com.juphoon.rcs.app.tool;

import android.preference.PreferenceManager;

import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.service.RmsDefine;

public class RcsUtilsEx {

    public final static String CC_DISABLE = "cc_disable";

    public static boolean getCCDisable() {
        return PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext()).getBoolean(CC_DISABLE, true);
    }

    public static void setCCDisable(boolean enable) {
        PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext()).edit().putBoolean(CC_DISABLE, enable).commit();
    }

    public static int getMixType(boolean cc, boolean silence, boolean direct, boolean at) {
        int mixType = RmsDefine.Rms.MIX_TYPE_COMMON;
        if (cc) {
            mixType += RmsDefine.Rms.MIX_TYPE_CC;
        }
        if (silence) {
            mixType += RmsDefine.Rms.MIX_TYPE_SILENCE;
        }
        if (direct) {
            mixType += RmsDefine.Rms.MIX_TYPE_DIRECT;
        }
        if (at) {
            mixType += RmsDefine.Rms.MIX_TYPE_AT;
        }
        return mixType;
    }

    public static String formatPhoneWithStar(String phone) {
        String out = phone.replace(" ", "");
        if (out.length() <= 11) {
            return out;
        }
        if (out.startsWith("+")) {
            return new StringBuffer().append(out.substring(3, 6)).append("****").append(out.substring(10)).toString();
        } else {
            return new StringBuffer().append(out.substring(0, 3)).append("****").append(out.substring(7)).toString();
        }
    }

}
