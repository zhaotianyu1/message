package com.juphoon.rcs.app.tool;

import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.android.messaging.Factory;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.helper.mms.RcsImReceiverServiceEx;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RcsChatbotInfoGetTools {

    private final static String TAG = "RcsChatbotInfoGetTool";

    private static List<String> sListNeedGetChatboInfo = new ArrayList<>();
    private static String sNowGetChatbot = null;
    private static String sNowCookie = "";
    private static long sStartTime = 0;
    private static Map<String, List<JSONObject>> sMapMessages = new HashMap<>();
    private static long MAX_TIME = 10 * 1000;

    /**
     * 检查是否需要获取chatbot详情
     *
     * @param chatbotId
     */
    public static boolean checkNeedGetChatbotInfo(String chatbotId) {
        if (TextUtils.isEmpty(chatbotId)) {
            return false;
        }
        // 请求超过10秒置为失败
        if (!TextUtils.isEmpty(sNowCookie) && System.currentTimeMillis() > sStartTime + MAX_TIME) {
            dealGetInfoResult(sNowCookie, false);
        }
        synchronized (sListNeedGetChatboInfo) {
            if (sListNeedGetChatboInfo.contains(chatbotId) || TextUtils.equals(sNowGetChatbot, chatbotId)) {
                return true;
            }
            if (checkNeedGetInfo(chatbotId)) {
                sListNeedGetChatboInfo.add(chatbotId);
                doGetInfo();
                return true;
            }
        }
        return false;
    }

    /**
     * 添加待处理的 Chatbot 消息
     *
     * @param chatbotId
     * @param jsonObject
     */
    public static void addPendingMessage(String chatbotId, JSONObject jsonObject) {
        synchronized (sMapMessages) {
            List<JSONObject> messages;
            if (!sMapMessages.containsKey(chatbotId)) {
                messages = new ArrayList<>();
                sMapMessages.put(chatbotId, messages);
            } else {
                messages = sMapMessages.get(chatbotId);
            }
            messages.add(jsonObject);
        }
    }

    /**
     * 查询结果处理
     *
     * @param cookie
     */
    public static void dealGetInfoResult(String cookie, boolean succ) {
        if (TextUtils.equals(cookie, sNowCookie)) {
            List<JSONObject> l = null;
            synchronized (sMapMessages) {
                if (succ) {
                    if (sMapMessages.containsKey(sNowGetChatbot)) {
                        l = sMapMessages.get(sNowGetChatbot);
                    }
                }
                sMapMessages.remove(sNowGetChatbot);
            }
            sNowCookie = null;
            sNowGetChatbot = null;
            // 必须 sNowGetChatbot 为 null 后才能处理
            if (l != null) {
                Log.d(TAG, "deal pending messages");
                for (JSONObject jsonObject : l) {
                    try {
                        jsonObject.put(RcsJsonParamConstants.RCS_JSON_HAS_GET_DETAIL, true);
                        Intent intent = new Intent(RcsJsonParamConstants.RCS_ACTION_IM_NOTIFY);
                        intent.putExtra(RcsJsonParamConstants.RCS_JSON_KEY, jsonObject.toString());
                        intent.setClass(RcsMmsInitHelper.getContext(), RcsImReceiverServiceEx.class);
                        RcsImReceiverServiceEx.enqueueWork(RcsMmsInitHelper.getContext(), intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            doGetInfo();
        }
    }

    private static void doGetInfo() {
        synchronized (sListNeedGetChatboInfo) {
            if (TextUtils.isEmpty(sNowGetChatbot) && sListNeedGetChatboInfo.size() > 0) {
                sNowCookie = UUID.randomUUID().toString();
                sNowGetChatbot = sListNeedGetChatboInfo.get(0);
                sListNeedGetChatboInfo.remove(0);

                RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
                    @Override
                    public void run(boolean succ,String resultCode, String token) {
                        if (succ) {
                            int ret = RcsCallWrapper.rcsGetChatbotInfo(sNowCookie, token, sNowGetChatbot, null);
                            if (ret == -1) {
                                sNowCookie = null;
                                sNowGetChatbot = null;
                                new Handler(Factory.get().getApplicationContext().getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        doGetInfo();
                                    }
                                }, 2000);
                            } else {
                                sStartTime = System.currentTimeMillis();
                            }
                        } else {
                            sNowCookie = null;
                            sNowGetChatbot = null;
                            doGetInfo();
                        }
                    }
                });
            }
        }
    }

    /**
     * 检查是否有 Chatbot 信息
     *
     * @param chatbotId
     * @return
     */
    private static boolean checkNeedGetInfo(String chatbotId) {
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, null,
                RmsDefine.ChatbotInfo.SERVICEID + "=?", new String[]{ RcsChatbotHelper.formatServiceIdWithSip(chatbotId) }, null);
        if (cursor != null) {
            try {
                return cursor.getCount() == 0;
            } finally {
                cursor.close();
            }
        }
        return true;
    }

}
