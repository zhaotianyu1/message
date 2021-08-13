package com.juphoon.helper.mms;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.helper.mms.RcsMessageSendHelper.WaitToSendMessage;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsGroupChatManager;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine.Rms;

public class RcsMessageSendService extends JobIntentService {

    private final static String TAG = RcsMessageSendService.class.getSimpleName();

    private static final String[] SEND_PROJECTION = new String[]{
            Rms._ID,        // 0
            Rms.THREAD_ID,  // 1
            Rms.MESSAGE_TYPE, //2
            Rms.ADDRESS,    // 3
            Rms.BODY,       // 4
            Rms.STATUS,     // 5
            Rms.DATE,       // 6
            Rms.FILE_PATH,  // 7
            Rms.FILE_TYPE,  // 8
            Rms.THUMB_PATH,  // 9
            Rms.GROUP_CHAT_ID, //10
            Rms.PA_UUID, // 11
            Rms.FILE_DURATION, // 12
            Rms.TYPE, //13
            Rms.TRANS_ID, //14
            Rms.IMDN_STRING, //15
            Rms.TRANS_SIZE, //16
            Rms.FILE_SIZE, //17
            Rms.RMS_EXTRA, //18
            Rms.CONVERSATION_ID, //19
            Rms.TRAFFIC_TYPE, //20
            Rms.CONTRIBUTION_ID, // 21
    };

    // 待发送消息
    private static final String SEND_QUEUE_SELECTION = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_QUEUED;
    private static final String ORDER_DATE_ASC = "date ASC";

    private static final int SEND_COLUMN_ID = 0;
    private static final int SEND_COLUMN_THREAD_ID = 1;
    private static final int SEND_COLUMN_MESSAGE_TYPE = 2;
    private static final int SEND_COLUMN_ADDRESS = 3;
    private static final int SEND_COLUMN_BODY = 4;
    private static final int SEND_COLUMN_STATUS = 5;
    private static final int SEND_COLUMN_DATE = 6;
    private static final int SEND_COLUMN_FILE_PATH = 7;
    private static final int SEND_COLUMN_FILE_TYPE = 8;
    private static final int SEND_COLUMN_THUMB_PATH = 9;
    private static final int SEND_COLUMN_GROUP_CHAT_ID = 10;
    private static final int SEND_COLUMN_PA_UUID = 11;
    private static final int SEND_COLUMN_DURATION = 12;
    private static final int SEND_COLUMN_TYPE = 13;
    private static final int SEND_COLUMN_TRANS_ID = 14;
    private static final int SEND_COLUMN_IMDN_STRING = 15;
    private static final int SEND_COLUMN_TRANS_SIZE = 16;
    private static final int SEND_COLUMN_FILE_SIZE = 17;
    private static final int SEND_COLUMN_RMS_EXTRA = 18;
    private static final int SEND_COLUMN_CONVERSATION_ID = 19;
    private static final int SEND_COLUMN_TRAFFIC_TYPE = 20;
    private static final int SEND_COLUMN_CONTRIBUTION_ID = 21;

    // 发送rcs消息的action
    public final static String ACTION_SEND_RCS_MESSAGE = "action_send_rcs_message";
    // rcs消息直接发送
    public final static String RCS_ID_SEND_DIRECTLY = "rcs_id_send_directly";
    // 发送rcs消息,如果失败需要去更新同步表,bundle值存消息的相关信息(用于7.0)
    public final static String RCS_SEND_MESSAGE_FAILED_BUNDLE = "rcs_send_message_failed_bundle";

    private Bundle mBundle;

    public static final int JOB_ID = 1002;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, RcsMessageSendService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        onHandleIntent(intent);
    }

    protected void onHandleIntent(@Nullable Intent intent) {
        String action = intent.getAction();
        if (TextUtils.equals(action, ACTION_SEND_RCS_MESSAGE)) {
            mBundle = intent.getBundleExtra(RCS_SEND_MESSAGE_FAILED_BUNDLE);
            sendRcsMessage(intent.getIntExtra(RCS_ID_SEND_DIRECTLY, -1));
        }
    }

    private void sendRcsMessage(int id) {
        Cursor cursor = null;
        if (id != -1) {
            cursor = getContentResolver().query(Rms.CONTENT_URI_LOG, SEND_PROJECTION, SEND_QUEUE_SELECTION + " AND " + Rms._ID + "=" + id, null, null);
        } else {
            cursor = getContentResolver().query(Rms.CONTENT_URI_LOG, SEND_PROJECTION, SEND_QUEUE_SELECTION, null, ORDER_DATE_ASC);
        }
        if (cursor != null) {
            Log.d("zty", "------------sendRcsMessage1--------");
            try {
                while (cursor.moveToNext()) {
                    final WaitToSendMessage message = genMessageFromCursor(cursor);
                    ContentValues values = new ContentValues();
                    // 判断是否登录
                    if (!RcsServiceManager.isLogined() || dealBlackMessage(message)) {
                        RcsMmsUtils.updateMessageFailed(message.mId, true);
                        Log.d("zty", "------------sendRcsMessage2--------");
                        continue;
                    }
                    // 判断是否要进行rejoin，如果需要则处理下一条消息
                    if (dealGroupNeedRejoin(message)) {
                        Log.d("zty", "------------sendRcsMessage3--------");
                        continue;
                    }
                    // 判断message类型是视频
                    if (message.mMessageType == Rms.RMS_MESSAGE_TYPE_VIDEO) {
//                        // 需要直接发送的message
//                        if (id != -1) {
                        if (message.mFileSize > 500 * 1024 * 1024) {//视频大小大于500M,则不发送
                            RcsMmsUtils.updateMessageFailed(message.mId, true);
                            return;
                        }
//                        } else if (!RcsVideoCompressorHelper.checkVideoAllowSend(message)) {//非直接发送的message需要判断视频参数
//                            continue;
//                        }
                    }
                    String ret = null;
                    boolean isHttpFile = false;
                    Log.d("zty", "------------sendRcsMessage4--------");
                    switch (message.mMessageType) {
                        case Rms.RMS_MESSAGE_TYPE_VOICE:
                        case Rms.RMS_MESSAGE_TYPE_IMAGE:
                        case Rms.RMS_MESSAGE_TYPE_VIDEO:
                        case Rms.RMS_MESSAGE_TYPE_VCARD:
                        case Rms.RMS_MESSAGE_TYPE_FILE:
                            if(RcsCallWrapper.rcsGetIsHttpOpen()){
                                isHttpFile = true;
                                RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
                                    @Override
                                    public void run(final boolean result, String resultCode, final String token) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (result) {
                                                    message.mCmccToken = token;
                                                    String ret = RcsMessageSendHelper.sendHttpFileMessage(message);
                                                    if (TextUtils.isEmpty(ret)) {
                                                        Log.d(TAG, "token ok chatbot file send fail");
                                                        RcsMmsUtils.updateMessageFailed(message.mId, true);
                                                    }
                                                } else {
                                                    Log.d(TAG, "token fail chatbot file fail");
                                                    RcsMmsUtils.updateMessageFailed(message.mId, true);
                                                }
                                            }
                                        }).start();
                                    }
                                });
                            }else {
                                if (RcsChatbotHelper.isChatbotByServiceId(message.mAddress)) {
                                    isHttpFile = true;
                                } else {
                                    ret = RcsMessageSendHelper.sendFileMessage(message);
                                }

                            }
                            break;
                        case Rms.RMS_MESSAGE_TYPE_TEXT:
                            Log.d("zty", "------------RMS_MESSAGE_TYPE_TEXT--------");
                            ret = RcsMessageSendHelper.sendTextMessage(message);
                            break;
                        case Rms.RMS_MESSAGE_TYPE_GEO:
                            ret = RcsMessageSendHelper.sendGeoMessage(message);
                            break;
                        case Rms.RMS_MESSAGE_TYPE_CARD:
                            ret = RcsMessageSendHelper.sendCardMessage(message);
                            break;
                        default:
                            break;
                    }
                    Log.d("zty", "------------sendRcsMessage5--------");
                    //Maap类型消息发送
                    if (RcsCallWrapper.rcsGetIsHttpOpen() == false && isHttpFile) {
                        Log.d(TAG, "isChatbotFile getToken");
                        RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
                            @Override
                            public void run(final boolean result,String resultCode, final String token) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (result) {
                                            message.mCmccToken = token;
                                            String ret;
                                            ret = RcsMessageSendHelper.sendFileMessage(message);
                                            if (TextUtils.isEmpty(ret)) {
                                                Log.d(TAG, "token ok chatbot file send fail");
                                                RcsMmsUtils.updateMessageFailed(message.mId, true);
                                            }
                                        } else {
                                            Log.d(TAG, "token fail chatbot file fail");
                                            RcsMmsUtils.updateMessageFailed(message.mId, true);
                                        }
                                    }
                                }).start();
                            }
                        });
                    }
                    if (TextUtils.isEmpty(ret) && !isHttpFile) {
                        RcsMmsUtils.updateMessageFailed(message.mId, true);
                    } else {
                        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_OUTBOX);
                        values.put(Rms.STATUS, Rms.STATUS_PENDING);
                        getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, message.mId), values, null, null);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        Log.d("zty", "------------ends--------");
    }

    private WaitToSendMessage genMessageFromCursor(Cursor cursor) {
        WaitToSendMessage message = new WaitToSendMessage();
        message.mId = cursor.getInt(SEND_COLUMN_ID);
        message.mThreadId = cursor.getInt(SEND_COLUMN_THREAD_ID);
        message.mMessageType = cursor.getInt(SEND_COLUMN_MESSAGE_TYPE);
        message.mAddress = cursor.getString(SEND_COLUMN_ADDRESS);
        message.mBody = cursor.getString(SEND_COLUMN_BODY);
        message.mStatus = cursor.getInt(SEND_COLUMN_STATUS);
        message.mDate = cursor.getLong(SEND_COLUMN_DATE);
        message.mFilePath = cursor.getString(SEND_COLUMN_FILE_PATH);
        message.mFileType = cursor.getString(SEND_COLUMN_FILE_TYPE);
        message.mThumbPath = cursor.getString(SEND_COLUMN_THUMB_PATH);
        message.mGroupChatId = cursor.getString(SEND_COLUMN_GROUP_CHAT_ID);
        message.mPaUuid = cursor.getString(SEND_COLUMN_PA_UUID);
        message.mFileDuration = cursor.getInt(SEND_COLUMN_DURATION);
        message.mType = cursor.getInt(SEND_COLUMN_TYPE);
        message.mTransId = cursor.getString(SEND_COLUMN_TRANS_ID);
        message.mImdnString = cursor.getString(SEND_COLUMN_IMDN_STRING);
        message.mTransSize = cursor.getInt(SEND_COLUMN_TRANS_SIZE);
        message.mFileSize = cursor.getInt(SEND_COLUMN_FILE_SIZE);
        message.mRmsExtra = cursor.getString(SEND_COLUMN_RMS_EXTRA);
        message.mConversationId = cursor.getString(SEND_COLUMN_CONVERSATION_ID);
        message.mTrafficType = cursor.getString(SEND_COLUMN_TRAFFIC_TYPE);
        message.mContributionId = cursor.getString(SEND_COLUMN_CONTRIBUTION_ID);
        return message;
    }

    private boolean dealBlackMessage(final WaitToSendMessage message) {
        RcsChatbotHelper.RcsChatbot chatbotinfo = RcsChatbotHelper.getChatbotInfoByServiceId(message.mAddress);
        if (chatbotinfo != null && (chatbotinfo.block || chatbotinfo.black)) {
            return true;
        }
        return false;
    }

    private boolean dealGroupNeedRejoin(final WaitToSendMessage message) {
        if (!TextUtils.isEmpty(message.mGroupChatId) &&
                (RcsCallWrapper.rcsGetIsHttpOpen() ? true : isMessageTypeNeedRejoin(message))) {
            if (!RcsCallWrapper.rcsGroupSessIsExist(message.mGroupChatId)) {
                RcsGroupInfo groupInfo = RcsGroupHelper.getGroupInfo(message.mGroupChatId);
                if (groupInfo != null) {
                    RcsGroupChatManager.rejoinGroup(groupInfo.mGroupChatId, groupInfo.mSessionIdentify, groupInfo.mSubject, groupInfo.getDisplayName(RcsServiceManager.getUserName()));
                }
                return true;
            }
        }
        return false;
    }

    private boolean isMessageTypeNeedRejoin(WaitToSendMessage message){
        return message.mMessageType != Rms.RMS_MESSAGE_TYPE_IMAGE && message.mMessageType != Rms.RMS_MESSAGE_TYPE_VOICE
                && message.mMessageType != Rms.RMS_MESSAGE_TYPE_VIDEO && message.mMessageType != Rms.RMS_MESSAGE_TYPE_GEO
                && message.mMessageType != Rms.RMS_MESSAGE_TYPE_VCARD && message.mMessageType != Rms.RMS_MESSAGE_TYPE_FILE;

    }

    //备选方案分离方法
    private void sendhttpRcsMessage(int id) {
        Cursor cursor = null;
        if (id != -1) {
            cursor = getContentResolver().query(Rms.CONTENT_URI_LOG, SEND_PROJECTION, SEND_QUEUE_SELECTION + " AND " + Rms._ID + "=" + id, null, null);
        } else {
            cursor = getContentResolver().query(Rms.CONTENT_URI_LOG, SEND_PROJECTION, SEND_QUEUE_SELECTION, null, ORDER_DATE_ASC);
        }
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    final WaitToSendMessage message = genMessageFromCursor(cursor);
                    ContentValues values = new ContentValues();
                    // 判断是否登录
                    if (!RcsServiceManager.isLogined()) {
                        RcsMmsUtils.updateMessageFailed(message.mId, true);
                        continue;
                    }
                    // 判断是否要进行rejoin，如果需要则处理下一条消息
                    if (dealGroupNeedRejoin(message)) {
                        continue;
                    }
                    // 判断message类型是视频
                    if (message.mMessageType == Rms.RMS_MESSAGE_TYPE_VIDEO) {
//                        // 需要直接发送的message
//                        if (id != -1) {
                        if (message.mFileSize > 500 * 1024 * 1024) {//视频大小大于500M,则不发送
                            RcsMmsUtils.updateMessageFailed(message.mId, true);
                            return;
                        }
//                        } else if (!RcsVideoCompressorHelper.checkVideoAllowSend(message)) {//非直接发送的message需要判断视频参数
//                            continue;
//                        }
                    }
                    String ret = null;
                    boolean isFile = false;
                    switch (message.mMessageType) {
                        case Rms.RMS_MESSAGE_TYPE_VOICE:
                        case Rms.RMS_MESSAGE_TYPE_IMAGE:
                        case Rms.RMS_MESSAGE_TYPE_VIDEO:
                        case Rms.RMS_MESSAGE_TYPE_VCARD:
                        case Rms.RMS_MESSAGE_TYPE_FILE:
                            isFile = true;
                            RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
                                @Override
                                public void run(final boolean result, String resultCode, final String token) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (result) {
                                                message.mCmccToken = token;
                                                String ret = RcsMessageSendHelper.sendHttpFileMessage(message);
                                                if (TextUtils.isEmpty(ret)) {
                                                    Log.d(TAG, "token ok chatbot file send fail");
                                                    RcsMmsUtils.updateMessageFailed(message.mId, true);
                                                }
                                            } else {
                                                Log.d(TAG, "token fail chatbot file fail");
                                                RcsMmsUtils.updateMessageFailed(message.mId, true);
                                            }
                                        }
                                    }).start();
                                }
                            });
                            break;
                        case Rms.RMS_MESSAGE_TYPE_TEXT:
                            ret = RcsMessageSendHelper.sendTextMessage(message);
                            break;
                        case Rms.RMS_MESSAGE_TYPE_GEO:
                            ret = RcsMessageSendHelper.sendGeoMessage(message);
                            isFile = true;
                            break;
                        case Rms.RMS_MESSAGE_TYPE_CARD:
                            ret = RcsMessageSendHelper.sendCardMessage(message);
                            break;
                        default:
                            break;
                    }
                    if (TextUtils.isEmpty(ret) && !isFile) {
                        RcsMmsUtils.updateMessageFailed(message.mId, true);
                    } else {
                        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_OUTBOX);
                        values.put(Rms.STATUS, Rms.STATUS_PENDING);
                        getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, message.mId), values, null, null);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

}
