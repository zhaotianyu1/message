package com.juphoon.helper.mms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.cmcc.lemon.MtcImImdn;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsServiceConstants;
import com.juphoon.service.RmsDefine;

import java.util.ArrayList;

/**
 * Created by mofei on 2020-03-03.
 */
public class RcsImdnManager {
    private static String TAG = "RcsImdnManager";

    private static ArrayList<Long> sThreadIds = new ArrayList<>();
    private static long sThreadId = -1;
    private static Context sContext;

    private static int INDEX_IMDN_STRING = 0;
    private static int INDEX_CONVERSATION_ID = 1;
    private static int INDEX_ADDRESS = 2;
    private static int INDEX_IMDN_TYPE = 3;


    private static final String[] sProjection = new String[]{
            RmsDefine.Rms.IMDN_STRING,
            RmsDefine.Rms.CONVERSATION_ID,
            RmsDefine.Rms.ADDRESS,
            RmsDefine.Rms.IMDN_TYPE
    };

    private static final String selection =
            " AND " + RmsDefine.Rms.TYPE + " = " + RmsDefine.Rms.MESSAGE_TYPE_INBOX +
                    " AND " + RmsDefine.Rms.GROUP_CHAT_ID + " IS NULL " +
                    " AND " + RmsDefine.Rms.CHATBOT_SERVICE_ID + " IS NULL " +
                    " AND " + RmsDefine.Rms.READ + " = 0";

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
     * @param imdn
     * @param number
     * @param convId
     */
    public static void sendMessageDisplay(String imdn, String number, String convId) {
        Log.d(TAG, "sendMessageDisplay imdn = " + imdn + ";number=" + number + ";convId=" + convId);
        if (!RcsServiceManager.isLogined()) {
            return;
        }
        if (Integer.valueOf(RcsCallWrapper.rcsGetConfig(RcsServiceConstants.CONFIG_KEY_DISPLAY_NOTIFICATION_SWITCH)) > 0) {
            RcsCallWrapper.rcsSendImdnDisp(imdn, number, null, convId);
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
                        RmsDefine.Rms.CONTENT_URI_LOG,
                        sProjection,
                        RmsDefine.Rms.THREAD_ID + "=?" + selection,
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
                    while (cursor.moveToNext()) {
                        String imdn = cursor.getString(INDEX_IMDN_STRING);
                        String conversationId = cursor.getString(INDEX_CONVERSATION_ID);
                        String address = cursor.getString(INDEX_ADDRESS);
                        int imdnType = cursor.getInt(INDEX_IMDN_TYPE);
                        if ((imdnType & MtcImConstants.MTC_PMSG_IMDN_DISP) > 0) {
                            RcsCallWrapper.rcsSendImdnDisp(imdn, address, null, conversationId);
                        }
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
        if(succ){
            updateRmsRead(sContext, sThreadId);
        }
        synchronized (sThreadIds) {
            sThreadId = -1;
        }
        runPreSendDisPlay();
    }

    private static void updateRmsRead(Context context, long threadId) {
        ContentValues values = new ContentValues();
        values.put(RmsDefine.Rms.READ, 1);
        context.getContentResolver().update(RmsDefine.Rms.CONTENT_URI_LOG, values, RmsDefine.Rms.THREAD_ID + "=" + threadId, null);
    }
}
