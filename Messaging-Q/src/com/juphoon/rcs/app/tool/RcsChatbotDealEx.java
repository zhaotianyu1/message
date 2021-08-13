package com.juphoon.rcs.app.tool;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.juphoon.chatbotmaap.PinyinHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by mofei on 2018/8/15.
 */

public class RcsChatbotDealEx {

    private final static String TAG = "RcsChatbotDeal";

    /**
     * 处理聊天机器人列表更新
     *
     * @param jsonObj
     */
    public static void dealChatbotList(JSONObject jsonObj) {
        String chatbotList = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY);
        boolean isSpecific = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CHATBOT_IS_SPECIFIC_LIST, false);
        boolean isRecommend = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CHATBOT_IS_RECOMMEND_LIST, false);
        List<RcsChatbotHelper.RcsChatbot> chatbots = null;
        List<RcsChatbotHelper.RcsChatbot> recommendChatbots = null;
        if (isSpecific) {
            // 特殊列表的话先将所有 black 和 special 置成0，根据新下来的数据重新设置状态
            ContentValues values = new ContentValues();
            values.put(RmsDefine.ChatbotInfo.BLACK, 0);
            values.put(RmsDefine.ChatbotInfo.SPECIAL, 0);
            RcsMmsInitHelper.getContext().getContentResolver().update(RmsDefine.ChatbotInfo.CONTENT_URI, values, null, null);
            chatbots = RcsChatbotHelper.parseChatbotSpecificList(chatbotList);
        } else if (isRecommend) {
            recommendChatbots = RcsChatbotHelper.parseChatbotRecommendListJson(chatbotList);
        } else {
            chatbots = RcsChatbotHelper.parseChatbotListJson(chatbotList);
            recommendChatbots = RcsChatbotHelper.parseChatbotRecommendListJson(chatbotList);
        }
        if (chatbots != null) {
            for (RcsChatbotHelper.RcsChatbot chatbot : chatbots) {
                insertOrUpdateChatbotInfo(chatbot.serviceId, chatbot, isSpecific);
            }
        }
        if (recommendChatbots != null) {
            for (RcsChatbotHelper.RcsChatbot chatbot : recommendChatbots) {
                insertOrUpdateChatbotInfo(chatbot.serviceId, chatbot, isSpecific);
            }
        }
    }

    /**
     * 处理聊天机器人详情更新
     *
     * @param jsonObject
     * @return serviceId
     */
    public static String dealChatbotInfo(JSONObject jsonObject) {
        String body = jsonObject.optString(RcsJsonParamConstants.RCS_JSON_BODY);
        String etag = jsonObject.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_ETAG);
        int cacheSecond = jsonObject.optInt(RcsJsonParamConstants.RCS_JSON_CACHE_SECONDS);
        RcsChatbotHelper.RcsChatbot rcsChatbot = RcsChatbotHelper.praseChatbotInfoJson(body);
        if (rcsChatbot == null) {
            return null;
        }
        ContentValues value = new ContentValues();
        value.put(RmsDefine.ChatbotInfo.SERVICEID, rcsChatbot.serviceId);
        value.put(RmsDefine.ChatbotInfo.NAME, rcsChatbot.name);
        value.put(RmsDefine.ChatbotInfo.EMAIL, rcsChatbot.email);
        value.put(RmsDefine.ChatbotInfo.SMS, rcsChatbot.sms);
        value.put(RmsDefine.ChatbotInfo.CALLBACK, rcsChatbot.callback);
        value.put(RmsDefine.ChatbotInfo.WEBSITE, rcsChatbot.website);
        value.put(RmsDefine.ChatbotInfo.TCPAGE, rcsChatbot.tcpage);
        value.put(RmsDefine.ChatbotInfo.ICON, rcsChatbot.icon);
        value.put(RmsDefine.ChatbotInfo.COLOUR, rcsChatbot.colour);
        value.put(RmsDefine.ChatbotInfo.SERVICEDESCRIPTION, rcsChatbot.serviceDescription);
        value.put(RmsDefine.ChatbotInfo.BOTVERSION, rcsChatbot.botVersion);
        value.put(RmsDefine.ChatbotInfo.NUMBER, RcsChatbotHelper.parseChatbotServierIdToNumber(rcsChatbot.serviceId));
        value.put(RmsDefine.ChatbotInfo.JSON, body);
        value.put(RmsDefine.ChatbotInfo.VERIFIED, rcsChatbot.verified);
        value.put(RmsDefine.ChatbotInfo.BACKGROUNDIMAGE, rcsChatbot.backgroundImage);
        value.put(RmsDefine.ChatbotInfo.ADDRESS, rcsChatbot.address);
        value.put(RmsDefine.ChatbotInfo.CATEGORY, rcsChatbot.category);
        value.put(RmsDefine.ChatbotInfo.ETAG, etag);
        value.put(RmsDefine.ChatbotInfo.SEARCHKEYWORD, PinyinHelper.PinYin4j.getSearchKeyword(rcsChatbot.name, rcsChatbot.serviceDescription));
        value.put(RmsDefine.ChatbotInfo.DEADLINE, cacheSecond + System.currentTimeMillis() / 1000);
        value.put(RmsDefine.ChatbotInfo.PERSISTENTMENU, rcsChatbot.persistentMenu);
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, new String[]{RmsDefine.ChatbotInfo.SERVICEID},
                RmsDefine.ChatbotInfo.SERVICEID + "=?", new String[]{rcsChatbot.serviceId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    RcsMmsInitHelper.getContext().getContentResolver().update(RmsDefine.ChatbotInfo.CONTENT_URI, value, RmsDefine.ChatbotInfo.SERVICEID + "=?", new String[]{rcsChatbot.serviceId});
                } else {
                    RcsMmsInitHelper.getContext().getContentResolver().insert(RmsDefine.ChatbotInfo.CONTENT_URI, value);
                }
            } finally {
                cursor.close();
            }
        }
        return rcsChatbot.serviceId;
    }

    private static void insertOrUpdateChatbotInfo(String serviceId, RcsChatbotHelper.RcsChatbot chatBot, boolean isSpecific) {
        ContentValues value = new ContentValues();
        value.put(RmsDefine.ChatbotInfo.SERVICEID, serviceId);
        value.put(RmsDefine.ChatbotInfo.NUMBER, RcsChatbotHelper.parseChatbotServierIdToNumber(serviceId));
        if (!TextUtils.isEmpty(chatBot.name)) {
            value.put(RmsDefine.ChatbotInfo.NAME, chatBot.name);
        }
        if (!TextUtils.isEmpty(chatBot.icon)) {
            value.put(RmsDefine.ChatbotInfo.ICON, chatBot.icon);
        }
        if (!TextUtils.isEmpty(chatBot.sms)) {
            value.put(RmsDefine.ChatbotInfo.SMS, chatBot.sms);
        }
        if (!TextUtils.isEmpty(chatBot.serviceDescription)) {
            //服务器暂时不会下 serviceDescription 这个字段
            value.put(RmsDefine.ChatbotInfo.SERVICEDESCRIPTION, chatBot.serviceDescription);
        }
        if (!TextUtils.isEmpty(chatBot.serviceDescription)) {
            //服务器暂时不会下 serviceDescription 这个字段
            value.put(RmsDefine.ChatbotInfo.SERVICEDESCRIPTION, chatBot.serviceDescription);
        }
        if (isSpecific) {
            value.put(RmsDefine.ChatbotInfo.SPECIAL, chatBot.special ? 1 : 0);
            value.put(RmsDefine.ChatbotInfo.BLACK, chatBot.black ? 1 : 0);
        }
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, new String[]{RmsDefine.ChatbotInfo.SERVICEID},
                RmsDefine.ChatbotInfo.SERVICEID + "=?", new String[]{serviceId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    RcsMmsInitHelper.getContext().getContentResolver().update(RmsDefine.ChatbotInfo.CONTENT_URI, value, RmsDefine.ChatbotInfo.SERVICEID + "=?", new String[]{serviceId});
                    return;
                } else {
                    RcsMmsInitHelper.getContext().getContentResolver().insert(RmsDefine.ChatbotInfo.CONTENT_URI, value);
                }
            } finally {
                cursor.close();
            }
        }
    }

}
