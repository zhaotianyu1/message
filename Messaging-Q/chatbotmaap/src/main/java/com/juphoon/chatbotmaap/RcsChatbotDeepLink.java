package com.juphoon.chatbotmaap;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RcsChatbotDeepLink {
    private boolean mValid;
    private boolean mChatbotUri;
    private String mServiceId;
    private String mSuggestions;
    private String mBody;
    private String mSms;


    private static final String SCHEME_SMS = "sms";
    private static final String SCHEME_SMSTO = "smsto";
    private static final String SCHEME_MMS = "mms";
    private static final String SCHEME_MMSTO = "smsto";
    public static final HashSet<String> SMS_MMS_SCHEMES = new HashSet<String>(
            Arrays.asList(SCHEME_SMS, SCHEME_MMS, SCHEME_SMSTO, SCHEME_MMSTO));

    public RcsChatbotDeepLink(final Uri deepLinkingData) {
        mValid = false;
        mChatbotUri = false;
        if (!isSmsMmsUri(deepLinkingData)) {
            return;
        }
        String schemeSpecificPart = deepLinkingData.getSchemeSpecificPart();
        if (TextUtils.isEmpty(schemeSpecificPart)) {
            return;
        }

        mValid = true;

        int index = schemeSpecificPart.indexOf("?");
        // //sms:123456 or sms:+123456

        if (index < 0) {
            if (schemeSpecificPart.contains("+")) {
                mSms = schemeSpecificPart.substring(schemeSpecificPart.indexOf("+") + 1);
                mChatbotUri = true;
            } else {
                mSms = schemeSpecificPart;
            }
        } else {
            //sms:123456?bodyxxx or sms:+123456?bodyxxx
            String maybeSms = schemeSpecificPart.substring(0, index);
            if (isNumeric(maybeSms) && !schemeSpecificPart.contains("service_id=")) {
                if (schemeSpecificPart.contains("+")) {
                    mChatbotUri = true;
                    mSms = schemeSpecificPart.substring(schemeSpecificPart.indexOf("+") + 1, index);
                } else {
                    mSms = schemeSpecificPart.substring(0, index);
                }
            } else {
                mChatbotUri = true;
            }
        }

        String paramsStr = schemeSpecificPart.substring(index + 1);
        final String[] params = paramsStr.split("&");//可能有service_id、body、suggestions


        for (final String p : params) {
            //如果 uri 包含 "service_id=" ，解析 service_id
            if (p.contains("service_id=")) {
                mServiceId = p.substring(p.indexOf("service_id=") + "service_id=".length());
            } else if (p.contains("suggestions=")) {
                try {
                    mSuggestions = URLDecoder.decode(new String(Base64.decode(p.substring(p.indexOf("suggestions=") + "suggestions=".length()).getBytes(), Base64.DEFAULT)), "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (p.contains("body=")) {
                try {
                    mBody = URLDecoder.decode(p.substring(p.indexOf("body=") + "body=".length()), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        if (TextUtils.isEmpty(mServiceId) && schemeSpecificPart.indexOf("@") > 0) {
            //sms:bot@chat?body=11 , sms:bot@chat
            if (index > 0) {
                mServiceId = schemeSpecificPart.substring(0, index).trim();
            } else {
                mServiceId = schemeSpecificPart.trim();
            }
        }
    }


    public static boolean isSmsMmsUri(final Uri uri) {
        return uri != null && SMS_MMS_SCHEMES.contains(uri.getScheme());
    }

    public String getServiceId() {
        return mServiceId;
    }

    public String getSuggestions() {
        return mSuggestions;
    }

    public String getBody() {
        return mBody;
    }

    public boolean isValid() {
        return mValid;
    }

    public String getSms() {
        return mSms;
    }

    public boolean isChatbotUri() {
        return mChatbotUri;
    }

    private boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("(\\+)?[0-9]*$");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }
}
