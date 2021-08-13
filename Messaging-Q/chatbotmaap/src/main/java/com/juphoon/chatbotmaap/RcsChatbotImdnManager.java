package com.juphoon.chatbotmaap;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsServiceConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import java.util.ArrayList;

public class RcsChatbotImdnManager {

    private static String TAG = "RcsChatbotImdnManager";

    private static ArrayList<Long> sThreadIds = new ArrayList<>();
    private static long sThreadId = -1;
    private static Context sContext;

    private static int INDEX_IMDN_TYPE = 0;
    private static int INDEX_IMDN_STRING = 1;
    private static int INDEX_CHATBOT_SERVICE_ID = 2;
    private static int INDEX_CONVERSATION_ID = 3;
    private static int INDEX_MESSAGE_ID = 4;

    private static final String[] sProjection = new String[]{
            Rms.IMDN_TYPE,
            Rms.IMDN_STRING,
            Rms.CHATBOT_SERVICE_ID,
            Rms.CONVERSATION_ID,
            Rms._ID
    };

    private static final String selection =
                    " AND ( " + Rms.STATUS + " = " + Rms.STATUS_SUCC + " OR " + Rms.STATUS + " = " + Rms.STATUS_RECEIVED + " OR " + Rms.STATUS + " = " + Rms.STATUS_INIT +" )" +
                    " AND " + Rms.CHATBOT_SERVICE_ID + " IS NOT NULL " +
                    " AND " + Rms.TYPE + " = " + Rms.MESSAGE_TYPE_INBOX +
                    " AND " + Rms.READ + " = 0";

    public static void init(Context context) {
        sContext = context;
    }

    /**
     * 发送已读回执入口
     *
     * @param threadId The message's thread
     */
    public static void sendDisplay(long threadId) {
        Log.d(TAG, "add threadId = " + threadId);
        if (!RcsServiceManager.isLogined()) {
            return;
        }
        if (Integer.valueOf(RcsCallWrapper.rcsGetConfig(RcsServiceConstants.CONFIG_KEY_DISPLAY_NOTIFICATION_SWITCH)) > 0) {
            synchronized (sThreadIds) {
                for (long m : sThreadIds) {
                    if (m == threadId) {
                        return;
                    }
                }
                sThreadIds.add(threadId);
            }
            runPreSendDisPlay();
        } else {
            updateRmsRead(sContext, threadId);
        }
    }

    /**
     * 发送单条消息已读回执
     *
     * @param  imdn
     * @param  serviceId
     * @param  conversationId
     */
    public static void sendMessageDisplay(String imdn, String serviceId, String convId) {
        Log.d(TAG, "sendMessageDisplay imdn = " + imdn + ";serviceId=" + serviceId + ";convId=" + convId);
        if (!RcsServiceManager.isLogined()) {
            return;
        }
        if (Integer.valueOf(RcsCallWrapper.rcsGetConfig(RcsServiceConstants.CONFIG_KEY_DISPLAY_NOTIFICATION_SWITCH)) > 0) {
            RcsCallWrapper.rcsSendImdnDisp(imdn, serviceId, null, convId);
        }
    }

    private static void runPreSendDisPlay() {
        Log.d(TAG, "runPreSendDisPlay ThreadId = " + sThreadId + ",ThreadIdS = " + sThreadIds);
        synchronized (sThreadIds) {
            if (sThreadId != -1) {
                return;
            }
            if (sThreadIds.size() == 0) {
                return;
            }
            sThreadId = sThreadIds.get(0);
            sThreadIds.remove(0);
            sendDisPlay();
        }
    }

    private static void sendDisPlay() {
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... none) {
                Cursor cursor = sContext.getContentResolver().query(
                        Rms.CONTENT_URI_LOG,
                        sProjection,
                        Rms.THREAD_ID + "=?" + selection,
                        new String[]{Long.toString(sThreadId)}, null);

                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (cursor == null) {
                    onResult(false);
                    return;
                }

                try {
                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            int imdnType = cursor.getInt(INDEX_IMDN_TYPE);
                            String imdn = cursor.getString(INDEX_IMDN_STRING);
                            String serviceId = cursor.getString(INDEX_CHATBOT_SERVICE_ID);
                            String conversationId = cursor.getString(INDEX_CONVERSATION_ID);
                            if (((imdnType & RcsImServiceConstants.MTC_IM_SESS_IMDN_DISP) == RcsImServiceConstants.MTC_IM_SESS_IMDN_DISP)
                                    && !TextUtils.isEmpty(imdn) && !TextUtils.isEmpty(serviceId)) {
                                RcsCallWrapper.rcsSendImdnDisp(imdn, serviceId, null, conversationId);
                            }
                        }
                        updateRmsRead(sContext, sThreadId);
                    }
                } finally {
                    cursor.close();
                }
                onResult(true);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void onResult(boolean succ) {
        Log.d(TAG, "onResult = " + succ);
        synchronized (sThreadIds) {
            sThreadId = -1;
        }
        runPreSendDisPlay();
    }

    private static void updateRmsRead(Context context, long threadId) {
        ContentValues values = new ContentValues();
        values.put(Rms.READ, 1);
        context.getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms.THREAD_ID + "=" + threadId, null);
    }

}

