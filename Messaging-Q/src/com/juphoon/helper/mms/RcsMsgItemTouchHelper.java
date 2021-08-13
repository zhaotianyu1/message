package com.juphoon.helper.mms;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.HttpUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RcsMsgItemTouchHelper {

    public static class RcsMessageItem {
        public long mId;
        public int mType;
        public String mAddress;
        public int mRmsMessageType;
        public String mThumbPath;
        public String mFilePath;
        public int mRmsStatus;
        public int mTransSize;
        public int mTotalSize;
        public int mDuration;
        public String mRmsExtra;
        public String mTransId;
        public String mFileType;
        public String mGroupChatId;
        public int mMixType;
        public String mImdn;
        public String mMsgId;// 同步表message_id
        public String mRmsExtra2;
    }

    private static Context sContext;
    private final static Set<Long> sSetThumbMsgId = new HashSet<>();
    private final static ExecutorService sPool = Executors.newSingleThreadExecutor();

    public static void init(Context context) {
        sContext = context;
    }

    public static void dealFailMessageItem(final RcsMessageItem item) {
        // 文件消息
        if (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_IMAGE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_VIDEO
            || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_VOICE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_VCARD
            || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_FILE) {
            if (item.mType == Rms.MESSAGE_TYPE_INBOX || (item.mMixType & Rms.MIX_TYPE_CC) > 0) {
                if (item.mRmsStatus == Rms.STATUS_INIT) {
                    startFetchFile(item);
                } else if (item.mRmsStatus == Rms.STATUS_FAIL) {
                    resumeFile(item, false);
                } else if (item.mRmsStatus == Rms.STATUS_PENDING) {
                    pauseFile(item, false);
                }
            } else {
                if (item.mRmsStatus == Rms.STATUS_INIT || TextUtils.isEmpty(item.mTransId)) {
                    // 文件还未通过sdk发送则通过数据库驱动重发
                    moveMsgToQueueAndSend(item.mId);
                } else if (item.mRmsStatus == Rms.STATUS_FAIL) {
                    resumeFile(item, true);
                } else if (item.mRmsStatus == Rms.STATUS_PENDING) {
                    pauseFile(item, true);
                }
            }
        } else if (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_GEO) {
            if (item.mType == Rms.MESSAGE_TYPE_INBOX || (item.mMixType & Rms.MIX_TYPE_CC) > 0) {
                if (item.mRmsStatus == Rms.STATUS_FAIL) {
                    fetchGeo(item);
                }
            } else {
                if (item.mRmsStatus == Rms.STATUS_INIT || TextUtils.isEmpty(item.mTransId)) {
                    // 文件还未通过sdk发送则通过数据库驱动重发
                    moveMsgToQueueAndSend(item.mId);
                } else if (item.mRmsStatus == Rms.STATUS_FAIL) {
                    resumeFile(item, true);
                }
            }
        } else if (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_IMAGE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VIDEO
                || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_AUDIO || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_GEO
                || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VCARD) {
            List<RcsChatbotHelper.RcsChatbotHttpFileInfo> fileInfolist = RcsChatbotHelper.parseRcsChatbotFileInfo(item.mRmsExtra);
            for (final RcsChatbotHelper.RcsChatbotHttpFileInfo fileInfo : fileInfolist) {
                if (TextUtils.equals(fileInfo.type, "file")) {
                    RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
                        @Override
                        public void run(boolean succ, String resultCode, String token) {
                            // 强制调用底层接口下载，走完整的下载流程
                            if (succ) {
                                if (item.mRmsStatus == Rms.STATUS_FAIL) {
                                    RcsCallWrapper.rcsHttpResumeDownload("", token, fileInfo.url,
                                            RcsFileUtils.genFilePathByFileInfo(fileInfo.url, fileInfo.name, fileInfo.contentType, RmsDefine.RMS_CHATBO_PATH),
                                            fileInfo.contentType, item.mTransSize, item.mTotalSize, item.mTotalSize, item.mTransId);
                                } else {
                                    RcsCallWrapper.rcsHttpDownloadFile("", token, fileInfo.url,
                                            RcsFileUtils.genFilePathByFileInfo(fileInfo.url, fileInfo.name, fileInfo.contentType, RmsDefine.RMS_CHATBO_PATH),
                                            fileInfo.contentType, fileInfo.size <= 0 ? 1000 : fileInfo.size, item.mTransId, false);
                                }
                            }
                        }
                    });
                }
            }
        } else {
            // 文本消息则通过数据库驱动重发
            if (item.mType != Rms.MESSAGE_TYPE_INBOX) {
                moveMsgToQueueAndSend(item.mId);
            }
        }
    }

    public static void dealFailhttpMessageItem(final RcsMessageItem item) {
        if (item.mRmsStatus == Rms.STATUS_SUCC || item.mRmsStatus == Rms.STATUS_DISPLAYED || item.mRmsStatus == Rms.STATUS_RECEIVED) {
            return;
        }
        //1v1消息
        if (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_IMAGE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_VIDEO
                || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_VOICE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_VCARD
                || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_FILE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_GEO) {
            //收到
            if (item.mType == Rms.MESSAGE_TYPE_INBOX || (item.mMixType & Rms.MIX_TYPE_CC) > 0) {
                if (item.mRmsStatus == Rms.STATUS_PENDING) {
                    pauseHttpFile(item, false);
                } else {
                    List<RcsChatbotHelper.RcsChatbotHttpFileInfo> fileInfolist = RcsChatbotHelper.parseRcsChatbotFileInfo(item.mRmsExtra);
                    for (final RcsChatbotHelper.RcsChatbotHttpFileInfo fileInfo : fileInfolist) {
                        if (TextUtils.equals(fileInfo.type, "file")) {
                            RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
                                @Override
                                public void run(boolean succ, String resultCode, String token) {
                                    // 强制调用底层接口下载，走完整的下载流程
                                    if (succ) {
                                        boolean isGeo = (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_GEO);
                                        boolean isFile = (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_FILE);
                                        boolean noNeedContentType = isGeo || isFile;
                                        String fileName = RcsFileUtils.genFilePathByFileInfo(fileInfo.url, fileInfo.name, noNeedContentType ? "" : fileInfo.contentType, RmsDefine.RMS_FILE_PATH);
                                        if (TextUtils.isEmpty(fileName)) {
                                            return;
                                        }
                                        if (item.mRmsStatus == Rms.STATUS_FAIL) {
                                            RcsCallWrapper.rcsHttpResumeDownload("", token, fileInfo.url, fileName,
                                                    fileInfo.contentType, item.mTransSize, item.mTotalSize, item.mTotalSize, item.mTransId);
                                        } else {
                                            RcsCallWrapper.rcsHttpDownloadFile("", token, fileInfo.url, fileName,
                                                    fileInfo.contentType, fileInfo.size <= 0 ? 1000 : fileInfo.size, item.mTransId, false);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }//上传
            else {
                if (item.mRmsStatus == Rms.STATUS_INIT || TextUtils.isEmpty(item.mTransId)) {
                    // 文件还未通过sdk发送则通过数据库驱动重发
                    moveMsgToQueueAndSend(item.mId);
                } else if (item.mRmsStatus == Rms.STATUS_FAIL) {
                    resumeHttpFile(item);
                } else if (item.mRmsStatus == Rms.STATUS_PENDING ) {
                    pauseHttpFile(item,true);
                } else if(item.mRmsStatus == Rms.STATUS_HTTP_UPLOADED){

                }
            }
            //chatbot类型
        } else if (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_IMAGE || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VIDEO
                || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_AUDIO || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_GEO
                || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VCARD || item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_FILE) {
            List<RcsChatbotHelper.RcsChatbotHttpFileInfo> fileInfolist = RcsChatbotHelper.parseRcsChatbotFileInfo(item.mRmsExtra);
            for (final RcsChatbotHelper.RcsChatbotHttpFileInfo fileInfo : fileInfolist) {
                if (TextUtils.equals(fileInfo.type, "file")) {
                    RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
                        @Override
                        public void run(boolean succ, String resultCode, String token) {
                            // 强制调用底层接口下载，走完整的下载流程
                            if (succ) {
                                if (item.mRmsStatus == Rms.STATUS_FAIL) {
                                    RcsCallWrapper.rcsHttpResumeDownload("", token, fileInfo.url,
                                            RcsFileUtils.genFilePathByFileInfo(fileInfo.url, fileInfo.name, fileInfo.contentType, RmsDefine.RMS_CHATBO_PATH),
                                            fileInfo.contentType, item.mTransSize, item.mTotalSize, item.mTotalSize, item.mTransId);
                                } else {
                                    RcsCallWrapper.rcsHttpDownloadFile("", token, fileInfo.url,
                                            RcsFileUtils.genFilePathByFileInfo(fileInfo.url, fileInfo.name, fileInfo.contentType, RmsDefine.RMS_CHATBO_PATH),
                                            fileInfo.contentType, fileInfo.size <= 0 ? 1000 : fileInfo.size, item.mTransId, false);
                                }
                            }
                        }
                    });
                }
            }
        } else {
            // 文本消息则通过数据库驱动重发
            if (item.mType != Rms.MESSAGE_TYPE_INBOX) {
                moveMsgToQueueAndSend(item.mId);
            }
        }
    }

    private static void resumeHttpFile(final RcsMessageItem item) {
        byte[] thumbData = new byte[0];
        if (item.mThumbPath != null) {
            thumbData = RcsFileUtils.fileToBytes(item.mThumbPath);
        }
        final byte[] finalThumbData = thumbData;
        RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
            @Override
            public void run(boolean result, String resultCode, String token) {
                if (result) {
                    if (item.mTransSize == 0) {
                        RcsCallWrapper.rcsHttpUploadFile(String.valueOf(item.mId), token, item.mFilePath, item.mFileType, item.mDuration, finalThumbData, 0, RcsMmsUtils.getMessageDisposition(item.mRmsMessageType));
                    } else {
                        RcsCallWrapper.rcsHttpResumeUpload(String.valueOf(item.mId), token, item.mFilePath, item.mFileType, item.mTransId, item.mDuration, RcsMmsUtils.getMessageDisposition(item.mRmsMessageType));
                    }
                }
            }
        });
    }

    /** juphoon 暂停http文件传输 */
    private static void pauseHttpFile(final RcsMessageItem item, final boolean isSend){
        RcsCallWrapper.rcsPauseHttpFile(item.mTransId, isSend);
    }

    /** juphoon 暂停文件传输 */
    private static void pauseFile(final RcsMessageItem item, final boolean isSend) {
        RcsCallWrapper.rcsPauselFile(item.mTransId, isSend);
    }

    private static void resumeFile(RcsMessageItem item, boolean isSend) {
        int threadType = getThreadType(item);
        byte[] thumbData = new byte[0];
        if (item.mThumbPath != null) {
            thumbData = RcsFileUtils.fileToBytes(item.mThumbPath);
        }
        boolean ret = false;
        switch (threadType) {
            case RmsDefine.RMS_GROUP_THREAD:
                RcsGroupInfo info = RcsGroupHelper.getGroupInfo(item.mGroupChatId);
                if (info != null) {
                    ret = RcsCallWrapper.rcsResumeGroupFile(isSend, info.mSubject, info.mSessionIdentify, item.mGroupChatId, item.mTransId, item.mImdn, 
                            item.mFilePath, item.mFileType, item.mDuration, item.mTransSize, item.mTotalSize, thumbData);
                }
                break;
            case RmsDefine.BROADCAST_THREAD:
                ret = RcsCallWrapper.rcsResumeOTMFile(null, item.mAddress, item.mTransId, item.mImdn, item.mFilePath, item.mFileType, item.mDuration, item.mTransSize,
                        item.mTotalSize, thumbData);
                break;
            case RmsDefine.COMMON_THREAD:
                //juphoon 文件重发逻辑
                if (RcsChatbotHelper.isChatbotByServiceId(item.mAddress)) {
                    RcsChatbotHelper.RcsChatbot chatbotinfo = RcsChatbotHelper.getChatbotInfoByServiceId(item.mAddress);
                    if (chatbotinfo == null) {
                        return;
                    }
                    if (TextUtils.isEmpty(item.mRmsExtra2)) {
                        resumeChatbotFile(item, thumbData);
                    } else {
                        RcsCallWrapper.rcsSendMessage1To1(String.valueOf(item.mId), chatbotinfo.serviceId, item.mRmsExtra2, RcsImServiceConstants.RCS_MESSAGE_TYPE_HTTP, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, null, null, null);
                    }
                } else {
                    ret = RcsCallWrapper.rcsResumeFile(isSend, null, item.mAddress, item.mTransId, item.mImdn, item.mFilePath, item.mFileType, item.mDuration, item.mTransSize, item.mTotalSize,
                            thumbData);
                }
                break;
            default:
                break;
        }
        if (ret) {
            updateMsgPendingStatus(item.mId, isSend);
        }
    }

    private static void startFetchFile(RcsMessageItem item) {
        int threadType = getThreadType(item);
        boolean ret = false;
        switch (threadType) {
            case RmsDefine.RMS_GROUP_THREAD:
                RcsGroupInfo info = RcsGroupHelper.getGroupInfo(item.mGroupChatId);
                if (info != null) {
                    ret = RcsCallWrapper.rcsFetchGroupFile(info.mSubject, info.mSessionIdentify, info.mGroupChatId, item.mTransId, item.mFilePath, item.mFileType);
                }
                break;

            default:
                ret = RcsCallWrapper.rcsFetchFile(item.mAddress, item.mTransId, item.mFilePath, item.mFileType);
                break;
        }
        if (ret) {
            updateMsgPendingStatus(item.mId, false);
        }
    }

    private static void moveMsgToQueueAndSend(final long msgId) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(Rms.TYPE, Rms.MESSAGE_TYPE_QUEUED);
                values.put(Rms.STATUS, Rms.STATUS_INIT);
                sContext.getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms._ID + "=" + msgId, null);
                RcsMessageSendService.enqueueWork(sContext, new Intent(RcsMessageSendService.ACTION_SEND_RCS_MESSAGE,
                        null,
                        sContext,
                        RcsMessageSendService.class));
            }
        });
    }

    private static void updateMsgPendingStatus(final long msgId, final boolean isSend) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(Rms.STATUS, Rms.STATUS_PENDING);
                if (isSend) {
                    values.put(Rms.TYPE, Rms.MESSAGE_TYPE_OUTBOX);
                }
                sContext.getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms._ID + "=" + msgId, null);
            }
        });
    }

    private static void updateMsgFailStatus(final long msgId) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(Rms.STATUS, Rms.STATUS_FAIL);
                sContext.getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms._ID + "=" + msgId, null);
            }
        });
    }

    private static void updateMsgSuccStatus(final long msgId, final String path) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(Rms.STATUS, Rms.STATUS_SUCC);
                values.put(Rms.FILE_PATH, path);
                sContext.getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms._ID + "=" + msgId, null);
            }
        });
    }

    private static void updateMsgThumb(final long msgId, final String thumbPath) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(Rms.THUMB_PATH, thumbPath);
                sContext.getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms._ID + "=" + msgId, null);
            }
        });
    }

    private static int getThreadType(RcsMessageItem item) {
        if (!TextUtils.isEmpty(item.mGroupChatId)) {
            return RmsDefine.RMS_GROUP_THREAD;
        } else {
            String[] arrayAddress = item.mAddress.split(";");
            if (arrayAddress.length > 1) {
                return RmsDefine.BROADCAST_THREAD;
            }
            return RmsDefine.COMMON_THREAD;
        }
    }

    // 处理公众帐号及文件消息(图片,语音,视频)
    public static void dealFailMessgeTask(final String uri, final String msgId) {
        new AsyncTask<Void, Void, RcsMessageItem>() {

            @Override
            protected RcsMessageItem doInBackground(Void... arg0) {
                return getMessageItem(Uri.parse(uri), msgId);
            }

            @Override
            protected void onPostExecute(RcsMessageItem result) {
                if (result != null) {
                    if (RcsCallWrapper.rcsGetIsHttpOpen()) {
                        dealFailhttpMessageItem(result);
                    } else {
                        dealFailMessageItem(result);
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static RcsMessageItem getMessageItem(Uri uri, String msgId) {
        if (!uri.getAuthority().equals(Rms.CONTENT_URI.getAuthority())) {
            return null;
        }
        Cursor cursor = sContext.getContentResolver().query(uri, null, null, null, null);
        RcsMsgItemTouchHelper.RcsMessageItem messageItem = new RcsMsgItemTouchHelper.RcsMessageItem();
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                messageItem.mId = cursor.getInt(cursor.getColumnIndex(Rms._ID));
                messageItem.mType = cursor.getInt(cursor.getColumnIndex(Rms.TYPE));
                messageItem.mAddress = cursor.getString(cursor.getColumnIndex(Rms.ADDRESS));
                messageItem.mRmsMessageType = cursor.getInt(cursor.getColumnIndex(Rms.MESSAGE_TYPE));
                messageItem.mThumbPath = cursor.getString(cursor.getColumnIndex(Rms.THUMB_PATH));
                messageItem.mFilePath = cursor.getString(cursor.getColumnIndex(Rms.FILE_PATH));
                messageItem.mRmsStatus = cursor.getInt(cursor.getColumnIndex(Rms.STATUS));
                messageItem.mTransSize = cursor.getInt(cursor.getColumnIndex(Rms.TRANS_SIZE));
                messageItem.mTotalSize = cursor.getInt(cursor.getColumnIndex(Rms.FILE_SIZE));
                messageItem.mDuration = cursor.getInt(cursor.getColumnIndex(Rms.FILE_DURATION));
                messageItem.mRmsExtra = cursor.getString(cursor.getColumnIndex(Rms.RMS_EXTRA));
                messageItem.mTransId = cursor.getString(cursor.getColumnIndex(Rms.TRANS_ID));
                messageItem.mFileType = cursor.getString(cursor.getColumnIndex(Rms.FILE_TYPE));
                messageItem.mGroupChatId = cursor.getString(cursor.getColumnIndex(Rms.GROUP_CHAT_ID));
                messageItem.mMixType = cursor.getInt(cursor.getColumnIndex(Rms.MIX_TYPE));
                messageItem.mImdn = cursor.getString(cursor.getColumnIndex(Rms.IMDN_STRING));
                messageItem.mMsgId = msgId;
                messageItem.mRmsExtra2 = cursor.getString(cursor.getColumnIndex(Rms.RMS_EXTRA2));
                return messageItem;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    // 下载公众帐号图片视频消息的缩略图
    public static void downloadPublicThumbToUpdate(final long msgId, String thumbLink) {
        if (sSetThumbMsgId.contains(msgId)) {
            return;
        }
        sSetThumbMsgId.add(msgId);
        String thumePath = RcsFileUtils.getNotExistThumbFilePath(RmsDefine.RMS_FILE_PATH, RcsFileUtils.getFileNameNoSuffix(thumbLink), "jpg");
        HttpUtils.asyncHttpDownload(msgId, thumbLink, thumePath, new HttpUtils.LoaderCallback() {

            @Override
            public void onLoadFinished(Object cookie, String url, String path, boolean succ) {
                updateMsgThumb((Long) cookie, path);
                updateBuglePublicThumb(msgId, path);
            }
        });
    }

    // 更新同步表公众帐号图片视频消息的缩略图路径
    private static void updateBuglePublicThumb(final long msgId, final String path) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                DatabaseWrapper db = DataModel.get().getDatabase();
                db.beginTransaction();
                try {
                    MessageData message = BugleDatabaseOperations.readMessage(db, String.valueOf(msgId));
                    ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
                    if (parts.size() > 0) {
                        if (parts.get(0).getContentUri() == null || parts.get(0).getContentUri().toString().startsWith("http")) {
                            parts.get(0).setContentUri(Uri.parse("file://" + path));
                            BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
                            MessagingContentProvider.notifyMessagesChanged(message.getConversationId());
                            MessagingContentProvider.notifyPartsChanged();
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        });
    }

    // 更新同步表公众帐号消息contentUri和状态
    private static void updateMsgBugleStatus(final RcsMessageItem item, final String path, final int status) {
        sPool.execute(new Runnable() {

            @Override
            public void run() {
                DatabaseWrapper db = DataModel.get().getDatabase();
                db.beginTransaction();
                try {
                    MessageData message = BugleDatabaseOperations.readMessage(db, item.mMsgId);
                    message.updateMessageDownLoadStatus(status);
                    if (message != null) {
                        ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
                        if (parts.size() > 0) {
                            if (status == MessageData.BUGLE_STATUS_INCOMING_COMPLETE) {
                                parts.get(0).setContentUri(Uri.parse("file://" + path));
                            }
                            BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
                            MessagingContentProvider.notifyMessagesChanged(message.getConversationId());
                            MessagingContentProvider.notifyPartsChanged();
                        }
                        db.setTransactionSuccessful();
                    }
                } finally {
                    db.endTransaction();
                }
            }
        });
    }

    private static boolean fetchGeo (RcsMessageItem item) {
        int threadType = getThreadType(item);
        boolean ret = false;
        switch (threadType) {
            case RmsDefine.COMMON_THREAD:
                ret = RcsCallWrapper.rcsFetchGeo(item.mAddress, item.mTransId);
                break;
            case RmsDefine.RMS_GROUP_THREAD:
                ret = RcsCallWrapper.rcsFetchGroupGeo(item.mAddress, item.mTransId, item.mGroupChatId);
                break;
            default:
                break;
        }
        return ret;
    }

    private static void resumeChatbotFile(final RcsMessageItem item, final byte[] thumbData) {
        RcsTokenHelper.getFileToken(new RcsTokenHelper.ResultOperation() {
            @Override
            public void run(boolean result,String resultCode, String token) {
                if (result) {
                    if (item.mRmsMessageType == Rms.RMS_MESSAGE_TYPE_GEO) {
                        RcsLocationHelper.RcsLocationItem rcsLocationItem = RcsLocationHelper.parseLocationJson(RcsMmsUtils.geoFileToString(item.mFilePath));
                        if (rcsLocationItem != null) {
                            Double latitude = rcsLocationItem.mLatitude;
                            Double longtitude = rcsLocationItem.mLongitude;
                            float radius = rcsLocationItem.mRadius;
                            String locationName = rcsLocationItem.mAddress;
                            String geoPath = RmsDefine.RMS_TEMP_FILE_PATH + "/" + UUID.randomUUID().toString();
                            if (RcsCallWrapper.rcsSetGeoToFile("", latitude, longtitude, radius, locationName, geoPath)) {
                                RcsCallWrapper.rcsHttpUploadFile(String.valueOf(item.mId), token, geoPath, item.mFileType, item.mDuration, null, 0, RcsMmsUtils.getMessageDisposition(item.mRmsMessageType));
                            }
                        }
                    } else {
                        if (item.mTransSize == 0) {
                            RcsCallWrapper.rcsHttpUploadFile(String.valueOf(item.mId), token, item.mFilePath, item.mFileType, item.mDuration, thumbData, 0, RcsMmsUtils.getMessageDisposition(item.mRmsMessageType));
                        } else {
                            RcsCallWrapper.rcsHttpResumeUpload(String.valueOf(item.mId), token, item.mFilePath, item.mFileType, item.mTransId, item.mDuration, RcsMmsUtils.getMessageDisposition(item.mRmsMessageType));
                        }
                    }
                }
            }
        });
    }
}
