package com.juphoon.helper.mms;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.android.messaging.sms.MmsUtils;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

public class RcsMessageSender {

    private static final String TAG = "RmsMessageSender";

    private final Context mContext;
    private RcsMessage mRmsMessage;
    private long mThreadId;
    private int mThreadType;
    private String mContact;  // 根据会话类型不通表示不通含义，联系人，公众账号id，群id
    private Uri mMessageUri; // 用于重发
    private long mTimestamp; // 用于重发
    private Uri mSmsUri;

    public RcsMessageSender(Context context, Uri uri, long timestamp) {
        mContext = context;
        mMessageUri = uri;
        mTimestamp = timestamp;
        mRmsMessage = null;
    }

    public RcsMessageSender(Context context, long threadId, int threadType, String contact, RcsMessage rmsMessage) {
        mContext = context;
        mThreadId = threadId;
        mThreadType = threadType;
        mContact = contact;
        mRmsMessage = rmsMessage;
    }
    
    public boolean sendMessage(long token) {
        return sendSpreadMessage() == null;
    }

    public Uri sendSpreadMessage() {
        if (mMessageUri != null) {
            ContentValues values = new ContentValues();
            values.put(Rms.DATE, mTimestamp);
            values.put(Rms.TIMESTAMP, mTimestamp/1000);
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_QUEUED);
            values.put(Rms.STATUS, Rms.STATUS_INIT);
            values.put(Rms.SUB_ID, RcsServiceManager.getSubId());
            mContext.getContentResolver().update(mMessageUri, values, null, null);
        } else {
            /**
             * 将 Rcs 消息存入短信表然后和 Rms 表做关联
             */
            mSmsUri = MmsUtils.insertSmsMessage(mContext,
                    Telephony.Sms.CONTENT_URI,
                    RcsServiceManager.getSubId(),
                    mContact,
                    RcsMmsUtils.getMessageText(mRmsMessage.getText(), mRmsMessage.getType()),
                    mRmsMessage.getDate(),
                    Telephony.Sms.STATUS_COMPLETE,
                    Telephony.Sms.MESSAGE_TYPE_SENT, mThreadId);
            if (mSmsUri == null) {
                return null;
            }
            int rmsMsgType = mRmsMessage.getType();
            if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_VOICE
                    || rmsMsgType == Rms.RMS_MESSAGE_TYPE_IMAGE
                    || rmsMsgType == Rms.RMS_MESSAGE_TYPE_VIDEO
                    || rmsMsgType == Rms.RMS_MESSAGE_TYPE_VCARD
                    || rmsMsgType == Rms.RMS_MESSAGE_TYPE_FILE) {
                mMessageUri = sendFile();
            } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_GEO) {
                mMessageUri = sendGeo();
            } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_TEXT) {
                mMessageUri = sendText();
            } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_CARD) {
                mMessageUri = sendCard();
            } else {
                Log.e(TAG, "rmsMsgType error " + rmsMsgType);
                return null;
            }
        }
        return mMessageUri;
    }

    private Uri sendFile() {
        int msgType = mRmsMessage.getType();
        File file;
        ContentValues values = genCommonValues();
        String filePath = RcsFileUtils.getFilePathByUri(mContext, mRmsMessage.getUri());
        // 无法得到路径则保存到文件里
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            filePath = RmsDefine.RMS_FILE_PATH + "/" + UUID.randomUUID().toString() + "." + RcsFileUtils.getFileSuffix(mContext, mRmsMessage.getUri());
            file = new File(filePath);
            try {
                RcsMmsUtils.copyToFile(mContext.getContentResolver().openInputStream(mRmsMessage.getUri()), file);
            } catch (FileNotFoundException e) {
                return null;
            }
        } else {
            file = new File(filePath);
        }
        String endSuffix = RcsFileUtils.getFileSuffix(filePath);
        String fileType = RcsFileUtils.getFileType(msgType, endSuffix);
        values.put(Rms.FILE_NAME, file.getName());
        values.put(Rms.FILE_PATH, filePath);
        values.put(Rms.FILE_TYPE, fileType);
        values.put(Rms.FILE_SIZE, file.length());
        if (!new File(RmsDefine.RMS_THUMB_PATH).exists()) {
            new File(RmsDefine.RMS_THUMB_PATH).mkdirs();
        }
        String thumbPath = RcsFileUtils.getNotExistThumbFilePathByTime(RmsDefine.RMS_THUMB_PATH, "thumb");
        Bitmap bitmap = null;
        int duration = 0;
        if (msgType == Rms.RMS_MESSAGE_TYPE_IMAGE) {
            RcsFileUtils.genThumbBitmap(thumbPath, file.getAbsolutePath());
        } else if (msgType == Rms.RMS_MESSAGE_TYPE_VIDEO) {
            bitmap = RcsFileUtils.getVideoFirstFrameBitmap(file.getAbsolutePath());
            if (bitmap != null) {
                RcsFileUtils.genThumbBitmap(thumbPath, bitmap);
            }
            duration = RcsFileUtils.getMediaDuring(file.getAbsolutePath()) * 1000;
        } else if (msgType == Rms.RMS_MESSAGE_TYPE_VOICE) {
            duration = RcsFileUtils.getMediaDuring(file.getAbsolutePath()) * 1000;
        }
        if (new File(thumbPath).exists()) {
            values.put(Rms.THUMB_PATH, thumbPath);
        }
        values.put(Rms.FILE_DURATION, duration);

        if (mThreadType == RmsDefine.COMMON_THREAD) {
            values.put(Rms.ADDRESS, mContact);
        } else if (mThreadType == RmsDefine.BROADCAST_THREAD) {
            values.put(Rms.ADDRESS, mContact);
        } else if (mThreadType == RmsDefine.RMS_GROUP_THREAD) {
            values.put(Rms.ADDRESS, RcsServiceManager.getUserName());
            values.put(Rms.GROUP_CHAT_ID, mContact);
        }
        return mContext.getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
    }

    private Uri sendGeo() {
        ContentValues values = genCommonValues();
        values.put(Rms.FILE_PATH, RcsFileUtils.getFilePathByUri(mContext, mRmsMessage.getUri()));
        values.put(Rms.FILE_TYPE, RcsFileUtils.getFileType(Rms.RMS_MESSAGE_TYPE_GEO, ""));
        if (mThreadType == RmsDefine.COMMON_THREAD) {
            values.put(Rms.ADDRESS, mContact);
        } else if (mThreadType == RmsDefine.BROADCAST_THREAD) {
            values.put(Rms.ADDRESS, mContact);
        } else if (mThreadType == RmsDefine.RMS_GROUP_THREAD) {
            values.put(Rms.ADDRESS, RcsServiceManager.getUserName());
            values.put(Rms.GROUP_CHAT_ID, mContact);
        }
        return mContext.getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
    }

    private Uri sendText() {
        ContentValues values = genCommonValues();
        values.put(Rms.BODY, mRmsMessage.getText());
        values.put(Rms.RMS_EXTRA, mRmsMessage.getRmsExtra());
        if (mThreadType == RmsDefine.COMMON_THREAD) {
            values.put(Rms.ADDRESS, mContact);
        } else if (mThreadType == RmsDefine.BROADCAST_THREAD) {
            values.put(Rms.ADDRESS, mContact);
        } else if (mThreadType == RmsDefine.RMS_GROUP_THREAD) {
            values.put(Rms.ADDRESS, RcsServiceManager.getUserName());
            values.put(Rms.GROUP_CHAT_ID, mContact);
            //values.put(Rms.RMS_EXTRA, mRmsMessage.getmRmsExtra());
        }
        return mContext.getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
    }

    private Uri sendCard() {
        return null;
//        ContentValues values = genCommonValues();
//        JSONArray artArray = new JSONArray();
//        try {
//            artArray.put(new JSONObject(mRmsMessage.getText()));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        values.put(Rms.RMS_EXTRA, artArray.toString()); // rms_extra 存储的是 art 的数组
//        if (mThreadType == RmsDefine.COMMON_THREAD) {
//            values.put(Rms.ADDRESS, mContact);
//        } else if (mThreadType == RmsDefine.BROADCAST_THREAD) {
//            values.put(Rms.ADDRESS, mContact);
//        } else if (mThreadType == RmsDefine.RMS_GROUP_THREAD) {
//            values.put(Rms.ADDRESS, RcsServiceManager.getUserName());
//            values.put(Rms.GROUP_CHAT_ID, mContact);
//        }
//        return mContext.getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
    }

    private ContentValues genCommonValues() {
        ContentValues values = new ContentValues();
        values.put(Rms.MESSAGE_TYPE, mRmsMessage.getType());
        values.put(Rms.DATE, mRmsMessage.getDate());
        values.put(Rms.TIMESTAMP, mRmsMessage.getDate()/1000);
        values.put(Rms.READ, true);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_QUEUED);
        values.put(Rms.STATUS, Rms.STATUS_INIT);
        values.put(Rms.THREAD_ID, mThreadId);
        values.put(Rms.SUB_ID, RcsServiceManager.getSubId());
        values.put(Rms.TRAFFIC_TYPE, mRmsMessage.getTrafficType());
        values.put(Rms.CONTRIBUTION_ID, mRmsMessage.getContributionId());
        values.put(Rms.CONVERSATION_ID, RcsMmsUtils.getRmsConversationIdFromThreadID(mThreadId));
        /**
         * Rms 表不在 TelephoyProvide 中需要更新该字段
         */
        values.put(Rms.SMS_ID, mSmsUri.getLastPathSegment());
        if (RcsChatbotHelper.isChatbotByServiceId(mContact)) {
            values.put(Rms.CHATBOT_SERVICE_ID, RcsChatbotHelper.getChatbotInfoByServiceId(mContact).serviceId);
        }
        return values;
    }

}
