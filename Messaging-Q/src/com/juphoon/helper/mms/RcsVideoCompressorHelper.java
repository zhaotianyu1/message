package com.juphoon.helper.mms;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
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
import com.juphoon.helper.mms.RcsMessageSendHelper.WaitToSendMessage;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class RcsVideoCompressorHelper {
    private static ArrayList<RcsWaitingCompressMessage> sCompressVideoMessage = new ArrayList<>();// 保存需要压缩的消息队列
    private static Context sContext;
    private static RcsWaitingCompressMessage sWorkingCompressMessage;
    private static long sStartTime;
    private static long sTimeout = 600000L;

    static class RcsWaitingCompressMessage {
        WaitToSendMessage mMessage;
        String mTempPath, mCompressFilePath;
        int mWidth, mHeight, mBit;

        public RcsWaitingCompressMessage(WaitToSendMessage message, int width, int height, int bit) {
            super();
            this.mMessage = message;
            this.mWidth = width;
            this.mHeight = height;
            this.mBit = bit;
        }

        public void updateMd5(String md5) {
            mTempPath = RmsDefine.RMS_TEMP_FILE_PATH + "/" + md5 + ".mp4";
            mCompressFilePath = RmsDefine.RMS_FILE_PATH + "/" + md5 + ".mp4";
        }

    }

    public static void init(Context context) {
        sContext = context;
    }

    private static void RunCompress() {
        synchronized (sCompressVideoMessage) {
            if (sWorkingCompressMessage != null) {
                return;
            }
            if (sCompressVideoMessage.size() == 0) {
                return;
            }
            sWorkingCompressMessage = sCompressVideoMessage.get(0);
            sCompressVideoMessage.remove(0);
            CompressVideoStepMd5();
        }
    }

    public static boolean checkVideoAllowSend(WaitToSendMessage message) {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(message.mFilePath);
        try {
            int width = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int bit = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            int rotation = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            if (rotation == 90 || rotation == 270) {
                int temp = width;
                width = height;
                height = temp;
            }
            double ratio = (double) width / (double) height;
            if (Math.max(width, height) > 640) {
                if (bit > 800000) {
                    if (width > height) {
                        addCompressList(message, 640, (int) (640 / ratio), 400000);
                    } else {
                        addCompressList(message, (int) (640 * ratio), 640, 400000);
                    }
                } else {
                    if (width > height) {
                        addCompressList(message, 640, (int) (640 / ratio), Math.min(400000, bit));
                    } else {
                        addCompressList(message, (int) (640 * ratio), 640, Math.min(400000, bit));
                    }
                }
                return false;
            } else {
                if (bit > 800000) {
                    addCompressList(message, width, height, 400000);
                    return false;
                } else {
                    return true;
                }
            }
        } catch (Exception ClassCastException) {
            return true;
        }
    }

    private static void addCompressList(WaitToSendMessage message, int width, int height, int bit) {
        synchronized (sCompressVideoMessage) {
            if (sWorkingCompressMessage != null && sWorkingCompressMessage.mMessage.mId == message.mId) {
                return;
            }
            for (RcsWaitingCompressMessage m : sCompressVideoMessage) {
                if (m.mMessage.mId == message.mId) {
                    return;
                }
            }
            sCompressVideoMessage.add(new RcsWaitingCompressMessage(message, width, height, bit));
        }
        RunCompress();
    }

    private static void CompressVideoStepMd5() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... none) {
                return RcsMmsUtils.getFileMD5(new File(sWorkingCompressMessage.mMessage.mFilePath));
            }

            @Override
            protected void onPostExecute(String result) {
                if (TextUtils.isEmpty(result)) {
                    onResult(false);
                } else {
                    sWorkingCompressMessage.updateMd5(result);
                    if (new File(sWorkingCompressMessage.mCompressFilePath).exists()) {
                        onResult(true);
                    } else {
                        CompressVideoStepCompress();
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void CompressVideoStepCompress() {
        final String cmd = "-y -i " + sWorkingCompressMessage.mMessage.mFilePath + " -s " + sWorkingCompressMessage.mWidth + "x" + sWorkingCompressMessage.mHeight
                + " -r 20 -c:v libx264 -preset ultrafast -c:a copy -me_method zero -tune fastdecode -threads auto -tune zerolatency -strict -2 -b:v " + sWorkingCompressMessage.mBit + " -minrate "
                + sWorkingCompressMessage.mBit + " -maxrate " + sWorkingCompressMessage.mBit + " -pix_fmt yuv420p " + sWorkingCompressMessage.mTempPath;
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                sStartTime = System.currentTimeMillis();
            }
            @Override
            protected Boolean doInBackground(Void... none) {
                return execCommand("ffmpeg " + cmd);
            }
            @Override
            protected void onPostExecute(Boolean result) {
                onResult(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void onResult(boolean succ) {
        if (succ) {
            File file = new File(sWorkingCompressMessage.mCompressFilePath);
            if (!file.exists()) {
                RcsMmsUtils.copyFile(new File(sWorkingCompressMessage.mTempPath), file);
                new File(sWorkingCompressMessage.mTempPath).delete();
            }
            // 更新原始表
            ContentValues values = new ContentValues();
            values.put(Rms.FILE_PATH, sWorkingCompressMessage.mCompressFilePath);
            values.put(Rms.FILE_SIZE, file.length());
            sContext.getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(sWorkingCompressMessage.mMessage.mId)), values, null, null);
            // 更新同步表
            DatabaseWrapper db = DataModel.get().getDatabase();
            db.beginTransaction();
            try {
                MessageData message = BugleDatabaseOperations.readMessage(db, String.valueOf(sWorkingCompressMessage.mMessage.mId));
                if (message != null) {
                    ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
                    if (parts.size() > 0) {
                        parts.get(0).setContentUri(Uri.parse("file://" + sWorkingCompressMessage.mCompressFilePath));
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
        Intent intent = new Intent(RcsMessageSendService.ACTION_SEND_RCS_MESSAGE, null, sContext, RcsMessageSendService.class);
        intent.putExtra(RcsMessageSendService.RCS_ID_SEND_DIRECTLY, sWorkingCompressMessage.mMessage.mId);
        RcsMessageSendService.enqueueWork(sContext, new Intent(RcsMessageSendService.ACTION_SEND_RCS_MESSAGE, null,
                sContext, RcsMessageSendService.class));
        synchronized (sCompressVideoMessage) {
            sWorkingCompressMessage = null;
        }
        RunCompress();
    }

    private static boolean execCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command); // 这句话就是shell与高级语言间的调用
            if (process == null) {
                return false;
            }
            while (!isProcessCompleted(process)) {
                if (System.currentTimeMillis() > sStartTime + sTimeout) {
                    throw new TimeoutException("FFmpeg timed out");
                }
            }
            Integer value = process.exitValue();
            if (value != null && value == 0) {
                return true;
            } else {
                return false;
            }
        } catch (TimeoutException var8) {
            return false;
        } catch (IOException var8) {
            return false;
        } catch (Exception var9) {
            return false;
        } finally {
            if (process != null)
                process.destroy();
        }
    }

    private static boolean isProcessCompleted(Process process) {
        try {
            if (process == null)
                return true;
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
        }
        return false;
    }

}
