package com.juphoon.helper.mms;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;

import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.action.InsertNewMessageAction;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.util.UiUtils;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.rcs.tool.RcsCheckUtils;
import com.juphoon.rcs.tool.RcsServiceManager;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;

public class RcsMessageTransHelper {
    private static FailMessageCallback sCallback;

    public static class RcsFailMessage {
        public String mText, mFilePath, mId;
        public long mThreadId, mFileSize;
        public boolean mIsFile;
        public int mFileType;

        public RcsFailMessage(String text, long threadId, boolean isFile, String id) {
            this.mText = text;
            this.mThreadId = threadId;
            this.mIsFile = isFile;
            this.mId = id;
        }

        public RcsFailMessage(String filePath, int fileType, long fileSize, long threadId, boolean isFile, String id) {
            this.mFilePath = filePath;
            this.mThreadId = threadId;
            this.mIsFile = isFile;
            this.mFileSize = fileSize;
            this.mFileType = fileType;
            this.mId = id;
        }
    }

    public static void addRcsFailMessageCallback(FailMessageCallback callback) {
        sCallback = callback;
    }

    public static void removeRcsFailMessageCallback() {
        sCallback = null;
    }

    public static class FailMessageCallback {
        public void onReceiveFailMessage(RcsFailMessage message) {
        }

        public void onReceiveFailFile(RcsFailMessage message) {
        }
    };

    /**
     * 转短信提示
     */
    private static void notifyTransToSms(final Context context, final String imdn, String text) {
//        if (!Settings.canDrawOverlays(context)) {
//            UiUtils.showToastAtBottom(R.string.no_can_draw_overlays_permission);
//            return;
//        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.message_send_failed);
        builder.setNegativeButton(R.string.resend_rcs, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        Cursor cursor = context.getContentResolver().query(Rms.CONTENT_URI_LOG, RcsReTransferManager.PROJECTION, Rms.IMDN_STRING + "=?", new String[]{imdn},
                                null);
                        if (cursor != null) {
                            try {
                                if (cursor.moveToFirst()) {
                                    RcsReTransferManager.retransferTextMsg(RcsReTransferManager.getThreadType(cursor), cursor);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
        if (!RcsCheckUtils.checkTextCanTransSms(text)) {
            builder.setPositiveButton(R.string.rcs_translate_sms, new OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    transSmsSend(context, imdn);
                }
            });
        }
        builder.setNeutralButton(R.string.cancel_complain, null);
//        AlertDialog dialog = builder.create();
//        dialog.setCancelable(false);
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
//        dialog.show();
    }

    /**
     * 将消息转短信发送
     * 
     * @param context
     * @param imdn
     */
    private static void transSmsSend(final Context context, final String imdn) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Cursor cursor = context.getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms._ID, Rms.ADDRESS, Rms.BODY, Rms.CHATBOT_SERVICE_ID, Rms.RMS_EXTRA }, Rms.IMDN_STRING + "=?", new String[] { imdn },
                        null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            DatabaseWrapper db = DataModel.get().getDatabase();
                            int subId = RcsServiceManager.getSubId();
                            ParticipantData self = BugleDatabaseOperations.getOrCreateSelf(db, subId);
                            long rmsMsgId = cursor.getLong(0);
                            String messageText = cursor.getString(2);
                            String chatbotServiceId = cursor.getString(3);
                            String rmsExtra = cursor.getString(4);
                            if (!TextUtils.isEmpty(chatbotServiceId)) {
                                RcsChatbotHelper.RcsChatbot chatbot = RcsChatbotHelper.getChatbotInfoByServiceId(chatbotServiceId);
                                if (chatbot == null || TextUtils.isEmpty(chatbot.sms) || !TextUtils.isEmpty(rmsExtra)) {
                                    return;
                                }
                            }

                            Uri rmsUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, rmsMsgId);
                            context.getContentResolver().delete(rmsUri, null, null);
                            // 删同步表
                            MessageData msgData = BugleDatabaseOperations.readMessageData(db, rmsUri);
                            String msgId = msgData.getMessageId();
                            String conversationId = msgData.getConversationId();
                            BugleDatabaseOperations.deleteMessage(db, msgId);
                            // 转短信
                            MessageData messageData = MessageData.createDraftSmsMessage(conversationId, self.getId(), messageText);
                            InsertNewMessageAction.insertNewMessage(messageData, subId);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }).start();
    }

    /**
     * 处理失败文本消息
     */
    public static void dealFailMessage(final Context context, final String imdn) {
        new AsyncTask<Void, Void, RcsFailMessage>() {

            @Override
            protected RcsFailMessage doInBackground(Void... arg0) {
                Cursor cursor = context.getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms.BODY, Rms.THREAD_ID, Rms.GROUP_CHAT_ID }, Rms.IMDN_STRING + "=?", new String[] { imdn }, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            String text = cursor.getString(0);
                            long id = cursor.getLong(1);
                            String groupChatId = cursor.getString(2);
                            int threadType = RcsMmsUtils.getThreadType(id);
                            if (threadType != RmsDefine.COMMON_THREAD || !TextUtils.isEmpty(groupChatId) || TextUtils.isEmpty(text)) {
                                return null;
                            }
                            return new RcsFailMessage(text, id, false, imdn);
                        }
                    } finally {
                        cursor.close();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(RcsFailMessage result) {
                if (result != null) {
                    notifyTransToSms(context, imdn, result.mText);
                }
            }
        }.execute();
    }

    /**
     * 处理失败文件消息
     */
    public static void dealFailFile(final Context context, final String transId) {
        new AsyncTask<Void, Void, RcsFailMessage>() {

            @Override
            protected RcsFailMessage doInBackground(Void... arg0) {
                Cursor cursor = context.getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms.FILE_PATH, Rms.MESSAGE_TYPE, Rms.FILE_SIZE, Rms.THREAD_ID }, Rms.TRANS_ID + "=?",
                        new String[] { transId }, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            String body = cursor.getString(0);
                            int type = cursor.getInt(1);
                            long size = cursor.getLong(2);
                            long id = cursor.getLong(3);
                            int threadType = RcsMmsUtils.getThreadType(id);
                            if (threadType != RmsDefine.COMMON_THREAD) {
                                return null;
                            }
                            if (size > 300000) {
                                return null;
                            } else {
                                return new RcsFailMessage(body, type, size, id, true, transId);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(RcsFailMessage result) {
                if (result != null && sCallback != null) {
                    sCallback.onReceiveFailFile(result);
                }
            }
        }.execute();
    }

}
