package com.juphoon.helper.mms;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.data.MessageData;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RmsDefine.Rms;

import java.util.HashMap;

public class RcsTransProgressManager {

    private static HashMap<String, TransProgress> sTransProgressMap = new HashMap<>();

    /**
     * 重启后查询所有失败的文件传输的进度
     */
    public static void init() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
                String selection =
                        "(" +
                        Rms.MESSAGE_TYPE + "=" + Rms.RMS_MESSAGE_TYPE_IMAGE +
                                " or " + Rms.MESSAGE_TYPE + "=" + Rms.RMS_MESSAGE_TYPE_VIDEO
                                + " or "  + Rms.MESSAGE_TYPE + "=" + Rms.RMS_MESSAGE_TYPE_FILE
                        + ") and " + Rms.STATUS + "=" + Rms.STATUS_FAIL;
                Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms._ID, Rms.FILE_SIZE, Rms.TRANS_SIZE, Rms.TRANS_ID },
                        selection, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(0);
                        long fileSize = cursor.getLong(1);
                        long transSize = cursor.getLong(2);
                        String transId = cursor.getString(3);
                        Uri rmsUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, id);
                        MessageData message = BugleDatabaseOperations.readMessageData(dbWrapper, rmsUri);
                        if (message != null) {
                            add(message.getMessageId(), fileSize, transSize, transId);
                        }
                    }
                    cursor.close();
                }
            }
        }).start();
    }

    /**
     * 如果该消息是正在传输的文件，则停止传输
     *
     * @param messageId
     */
    public static void deleteAndPauseMessage(String messageId) {
        synchronized (sTransProgressMap) {
            RcsTransProgressManager.TransProgress progress = getTransProgress(messageId);
            if (progress != null) {
                RcsCallWrapper.rcsPauselFile(progress.getTransId(), true);
                RcsCallWrapper.rcsPauselFile(progress.getTransId(), false);
                delete(messageId);
            }
        }
    }

    public static class TransProgress {

        public long getMaxSize() {
            return maxSize;
        }

        public long getTransSize() {
            return transSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public void setTransSize(long transSize) {
            this.transSize = transSize;
        }

        public TransProgress(long maxSize, long transSize) {
            this.maxSize = maxSize;
            this.transSize = transSize;
        }

        public String getTransId() {
            return mTransId;
        }

        public void setTransId(String transId) {
            this.mTransId = transId;
        }

        private long maxSize;
        private long transSize;
        private String mTransId;

    }

    public static synchronized void delete(String msgId) {
        if (!TextUtils.isEmpty(msgId)) {
            synchronized (sTransProgressMap) {
                sTransProgressMap.remove(msgId);
            }
        }
    }

    public static TransProgress add(String msgId, long maxSize, long transSize, String transId) {
        TransProgress mProgress = null;
        if (!TextUtils.isEmpty(msgId)) {
            synchronized (sTransProgressMap) {
                mProgress = sTransProgressMap.get(msgId);
                if (mProgress != null) {
                    mProgress.setMaxSize(maxSize);
                    mProgress.setTransSize(transSize);
                    mProgress.setTransId(transId);
                } else {
                    mProgress = new TransProgress(maxSize, transSize);
                }
                sTransProgressMap.put(msgId, mProgress);
            }
        }
        return mProgress;
    }

    public static TransProgress getTransProgress(String msgId) {
        TransProgress mProgress = null;
        if (!TextUtils.isEmpty(msgId)) {
            synchronized (sTransProgressMap) {
                mProgress = sTransProgressMap.get(msgId);
            }
        }
        return mProgress;
    }
}
