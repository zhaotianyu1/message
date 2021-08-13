package com.juphoon.chatbotmaap;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.juphoon.chatbotmaap.chatbotDetail.RcsChatBotDetailActivity;
import com.juphoon.helper.RcsBitmapCache;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsChatbotHelper.RcsChatbot;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsGeoUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;

import org.json.JSONObject;

import java.util.UUID;

public class RcsChatbotUtils {

    /**
     * 格式化 chatbot 信息是 sms:1111 字符串
     * @param sms
     * @return
     */
    public static String parseSmsToNumber(String sms) {
        if (TextUtils.isEmpty(sms)) {
            return "";
        }
        int index = sms.indexOf("sms:");
        if (index >= 0) {
            return sms.substring(index + "sms:".length());
        }
        return sms;
    }

    /**
     * chatbot 头像设置
     *
     * @param imageView
     * @param icon
     */
    public static void getDefaultIcon(final ImageView imageView, String icon) {
        if (!TextUtils.isEmpty(icon)) {
            RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(icon);
            String iconPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_ICON_PATH);
            if (TextUtils.isEmpty(iconPath)) {
                imageView.setImageResource(R.drawable.chatbot_avatar);
                imageView.setTag(icon);
                RcsFileDownloadHelper.downloadFile(icon, fileInfo,
                        new RcsFileDownloadHelper.Callback() {
                            @Override
                            public void onDownloadResult(String cookie, boolean succ, String path) {
                                if (succ && TextUtils.equals(cookie, (String) imageView.getTag())) {
                                    RcsBitmapCache.getBitmapFromPath(imageView, path, true);
                                }
                            }
                        }, null, RmsDefine.RMS_ICON_PATH);
            } else {
                RcsBitmapCache.getBitmapFromPath(imageView, iconPath, true);
            }
        } else {
            imageView.setImageResource(R.drawable.chatbot_avatar);
        }
    }


    public static void setIcon(final ImageView imageView, String icon, final boolean isCircle) {
        if (!TextUtils.isEmpty(icon)) {
            RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(icon);
            String iconPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_ICON_PATH);
            if (TextUtils.isEmpty(iconPath)) {
                imageView.setImageResource(R.drawable.chatbot_avatar);
                imageView.setTag(icon);
                RcsFileDownloadHelper.downloadFile(icon, fileInfo, new RcsFileDownloadHelper.Callback() {
                    @Override
                    public void onDownloadResult(String cookie, boolean succ, String path) {
                        if (succ && TextUtils.equals(cookie, (String) imageView.getTag())) {
                            RcsBitmapCache.getBitmapFromPath(imageView, path, isCircle);
                        }
                    }
                }, null, RmsDefine.RMS_ICON_PATH);
            } else {
                RcsBitmapCache.getBitmapFromPath(imageView, iconPath, isCircle);
            }
        } else {
            imageView.setImageResource(R.drawable.chatbot_avatar);
        }
    }

    public static void saveChatbot(Context context, String serviceId) {
        if (TextUtils.isEmpty(serviceId)) {
            return;
        }
        Cursor cursor = context.getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, new String[]{RmsDefine.ChatbotInfo.SERVICEID},
                RmsDefine.ChatbotInfo.SERVICEID + "=?", new String[]{serviceId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() == 0) {
                    ContentValues value = new ContentValues();
                    value.put(RmsDefine.ChatbotInfo.SERVICEID, serviceId);
                    value.put(RmsDefine.ChatbotInfo.NUMBER, RcsChatbotHelper.parseChatbotServierIdToNumber(serviceId));
                    value.put(RmsDefine.ChatbotInfo.ICON, "");
                    value.put(RmsDefine.ChatbotInfo.NAME, "");
                    context.getContentResolver().insert(RmsDefine.ChatbotInfo.CONTENT_URI, value);
                }
            } finally {
                cursor.close();
            }
        }
    }

    public static boolean checkChatBotIsInConversation(Context context, String  serviceId) {
        Cursor cursor = context.getContentResolver().query(RmsDefine.Rms.CONTENT_URI_LOG, new String[]{RmsDefine.Rms.CHATBOT_SERVICE_ID},
                RmsDefine.Rms.CHATBOT_SERVICE_ID + " Like " + "'%" + serviceId + "'", null, null);
        if (cursor.getCount() > 0) {
            return true;
        }
        return false;
    }

    public static boolean checkChatBotIsSaveLocal(String serviceId) {
        RcsChatbot info = RcsChatbotHelper.getChatbotInfoByServiceId(serviceId);
        if (info != null && info.saveLocal) {
            return true;
        }
        return false;
    }

    public static void sendDeepLinkBroadCast(Context context, String recipients, String suggestions) {
        JSONObject jsonObj = new JSONObject();
        try {
            if (!TextUtils.isEmpty(recipients)) {
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_ADDRESS, RcsChatbotHelper.parseChatbotServierIdToNumber(recipients));
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID, recipients);
            } else {
                return;
            }
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT, true);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_BODY, "DeepLink");//暂时写死
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_ACTION, RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_RECV_MSG);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT_SUGGESTION, suggestions);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_TIMESTAMP, System.currentTimeMillis());
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_IMDN_ID, UUID.randomUUID().toString());
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_IMDN_TYPE,  0);
            jsonObj.put(RmsDefine.Rms.TYPE, RmsDefine.Rms.MESSAGE_TYPE_INBOX);
            jsonObj.put(RmsDefine.Rms.STATUS, RmsDefine.Rms.STATUS_SUCC);
            jsonObj.put(RmsDefine.Rms.MIX_TYPE, RmsDefine.Rms.MIX_TYPE_COMMON);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_SUB_ID, RcsServiceManager.getSubId());

            Intent intent = new Intent(RcsJsonParamConstants.RCS_ACTION_IM_NOTIFY);
            intent.setComponent(new ComponentName("com.android.Jmessaging", "com.juphoon.helper.mms.RcsWakeupReceiver"));
            intent.putExtra(RcsJsonParamConstants.RCS_JSON_KEY, jsonObj.toString());
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMessageingPkg() {
        return "com.android.Jmessaging";
    }

    public static String getRcsChatBotHelperReceiverName() {
        return "com.juphoon.helper.mms.RcsChatbotHelperRecevier";
    }

    public static boolean isGeoSms(String sms) {
        JSONObject object = RcsGeoUtils.parseGeoStringToJson(sms);
        return object == null ? false : true;
    }

    public static String getGeoFileBySms(String sms) {
        String geoFilePath = "";
        JSONObject object = RcsGeoUtils.parseGeoStringToJson(sms);
        if (object != null) {
            geoFilePath = RcsFileUtils.saveGeoToFile("", object.toString());
        }
        return geoFilePath;
    }


    public static void startChatBotConversation(Context context, String serviceId, String suggestion, String body) {
        if (!TextUtils.isEmpty(suggestion)) {
            RcsChatbotUtils.sendDeepLinkBroadCast(context, serviceId, suggestion);
        }
        if (!TextUtils.isEmpty(serviceId)) {
            Intent intent = new Intent();
            intent.putExtra(RcsChatbotDefine.KEY_ADDRESS, RcsChatbotHelper.formatServiceIdWithNoSip(serviceId));
            intent.setAction(RcsChatbotDefine.ACTION_LAUNCH_CONVERSATION);
            intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
            context.sendBroadcast(intent);
        }
    }

    public static void startChatBotDetailActivity(Context context, String serviceId) {
        Intent intent = new Intent(context, RcsChatBotDetailActivity.class);
        intent.putExtra(RcsChatBotDetailActivity.KEY_CHATBOT_NUMBER, serviceId);
        context.startActivity(intent);
    }

}
