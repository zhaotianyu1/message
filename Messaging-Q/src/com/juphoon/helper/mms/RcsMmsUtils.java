package com.juphoon.helper.mms;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.android.messaging.BuildConfig;
import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.mmslib.SqliteWrapper;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.Assert;
import com.android.messaging.util.Assert.DoesNotRunOnMainThread;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.UriUtil;
import com.juphoon.cmcc.lemon.MtcImFthttp;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.helper.mms.RcsDatabaseMessages.RmsMessage;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsCheckUtils;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;
import com.juphoon.ui.RcsRegisterActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RcsMmsUtils {

    private static final String FIRST_USE_RCS = "first_use_rcs";
    private static final String FIRST_TRANSF_SMS = "first_transf_sms";
    private static final String FIRST_RESTORE = "first_restore";
    private static final String LAST_LOGIN_MSISDN = "last_login_msisdn";
    private static final String IS_RCS_MODE = "is_rcs_mode";
    private static final String LAST_LOGOUT_TIME = "last_logout_time";
    private static final String TAG = LogUtil.BUGLE_TAG;

    public static final int RMS_REQUEST_SENDING_FILE = 300;
    public static final String RMS_SYSTEM_MSG = "system_msg";

    public static final String[] THREADS_PROJECTION = {
            Threads._ID,
            RmsDefine.Threads.RMS_THREAD_TYPE,
            RmsDefine.Threads.PRIORITY
        };
    
    /** juphoon **/
    public static final int COLUMN_RMS_ADDRESS         = 25;
    public static final int COLUMN_RMS_DATE            = 26;
    public static final int COLUMN_RMS_DATE_SENT       = 27;
    public static final int COLUMN_RMS_READ            = 28;
    public static final int COLUMN_RMS_SEEN            = 29;
    public static final int COLUMN_RMS_STATUS          = 30;
    public static final int COLUMN_RMS_LOCKED          = 31;
    public static final int COLUMN_RMS_TYPE            = 32;
    public static final int COLUMN_RMS_BODY            = 33;
    public static final int COLUMN_RMS_MESSAGE_TYPE    = 34;
    public static final int COLUMN_RMS_ERROR_CODE      = 35;
    public static final int COLUMN_RMS_FILE_NAME       = 36;
    public static final int COLUMN_RMS_FILE_TYPE       = 37;
    public static final int COLUMN_RMS_FILE_PATH       = 38;
    public static final int COLUMN_RMS_FILE_EXPIRE     = 39;
    public static final int COLUMN_RMS_THUMB_PATH      = 40;
    public static final int COLUMN_RMS_TRANS_ID        = 41;
    public static final int COLUMN_RMS_FILE_SIZE       = 42;
    public static final int COLUMN_RMS_TRANS_SIZE      = 43;
    public static final int COLUMN_RMS_IMDN_STRING     = 44;
    public static final int COLUMN_RMS_IMDN_TYPE       = 45;
    public static final int COLUMN_RMS_TIMESTAMP       = 46;
    public static final int COLUMN_RMS_GROUP_CHAT_ID   = 47;
    public static final int COLUMN_RMS_IMDN_STATUS     = 48;
    public static final int COLUMN_RMS_PA_UUID         = 49;
    public static final int COLUMN_RMS_MSG_UUID        = 50;
    public static final int COLUMN_RMS_SMS_DIGEST      = 51;
    public static final int COLUMN_RMS_ACTIVE_STATUS   = 52;
    public static final int COLUMN_RMS_FORWARDABLE     = 53;
    public static final int COLUMN_RMS_MEDIA_UUID      = 54;
    public static final int COLUMN_RMS_THUMB_LINK      = 55;
    public static final int COLUMN_RMS_ORIGINAL_LINK   = 56;
    public static final int COLUMN_RMS_TITLE           = 57;
    public static final int COLUMN_RMS_IS_BURN_MSG     = 58;
    public static final int COLUMN_RMS_FILE_DURATION   = 59;
    public static final int COLUMN_RMS_EXTRA           = 60;
    public static final int COLUMN_RMS_MIX_TYPE        = 61;

    public static final Uri sAllThreadsUri = Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();

    /**
     * 处理超时的消息
     */
    public static void dealTimeoutMessages() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(Rms.TYPE, Rms.MESSAGE_TYPE_FAILED);
                values.put(Rms.STATUS, Rms.STATUS_FAIL);
                String whereString = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_QUEUED + " AND " + System.currentTimeMillis() + "-" + Rms.DATE
                        + ">30000";
                RcsMmsInitHelper.getContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values, whereString, null);
            }
        }).start();
    }

    /**
     * 标示发送中消息为失败
     */
    public static void markQueueAndOutboxMsgFail() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                String selection = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_QUEUED + " or " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX;
                Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms._ID },
                        selection, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(0);
                        updateMessageFailed(id, true);
                    }
                    cursor.close();
                }
            }
        }).start();
    }

    /**
     * 标示接收中消息为失败
     */
    public static void markInboxMsgFail() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                String selection = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX + " and " + Rms.STATUS + "=" + Rms.STATUS_PENDING;
                Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms._ID },
                        selection, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(0);
                        updateMessageFailed(id, false);
                    }
                    cursor.close();
                }
            }
        }).start();
    }

    /**
     * 获取是否第一次使用Rcs
     */
    public static boolean getFirstUseRcs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        return prefs.getBoolean(FIRST_USE_RCS, true);
    }

    /**
     * 设置是否第一次使用Rcs
     */
    public static void setFirstUseRcs(boolean first) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        prefs.edit().putBoolean(FIRST_USE_RCS, first).commit();
    }

    /**
     * 获取是否第一次转短信
     */
    public static boolean getFirstTransMessage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        return prefs.getBoolean(FIRST_TRANSF_SMS, true);
    }

    /**
     * 设置是否第一次转短信
     */
    public static void setFirstTransMessage(boolean first) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        prefs.edit().putBoolean(FIRST_TRANSF_SMS, first).commit();
    }

    /**
     * 获取是否第一次提示备份恢复
     */
    public static boolean getFirstRestore() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        return prefs.getBoolean(FIRST_RESTORE, true);
    }

    /**
     * 设置是否第一次提示备份恢复
     */
    public static void setFirstRestore(boolean first) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        prefs.edit().putBoolean(FIRST_RESTORE, first).commit();
    }

    /**
     * 获取上次登录的号码
     */
    public static String getLastLoginMsisdn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        return prefs.getString(LAST_LOGIN_MSISDN, "");
    }

    /**
     * 设置上次登录的号码
     */
    public static void setLastLoginMsisdn(String msisdn) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        prefs.edit().putString(LAST_LOGIN_MSISDN, msisdn).commit();
    }

    /**
     * 设置上次登出时间
     * @param time
     */
    public static void setLastLogoutTime(long time) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        prefs.edit().putLong(LAST_LOGOUT_TIME, time).commit();
    }

    /**
     * 获取上次登出时间
     * @return
     */
    public static long getLastLogoutTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
        return prefs.getLong(LAST_LOGOUT_TIME, 0);
    }

    /**
     * 获得百分比
     * @param transSize
     * @param totalSize
     * @return
     */
    public static int getPercent(int transSize, int totalSize) {
        if (totalSize <= 0) {
            return 0;
        } else {
            return (int) (Long.valueOf(transSize) * 100 / Long.valueOf(totalSize));
        }
    }

    /**
     *
     * @param activity
     * @param requestCode
     * @param durationLimit 秒
     */
    public static void recordVideo(Activity activity, int requestCode, int durationLimit) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra("android.intent.extra.durationLimit", durationLimit);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(RcsFileUtils.getNotExistFileByTime(RmsDefine.RMS_FILE_PATH, "3gpp"))));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 判断是否要显示转发菜单项
     * @param isMe
     * @param rmsMessageType
     * @param status
     * @param mixType
     * @param burn
     * @return
     */
    public static boolean isRmsMessageItemShowForwardMenu(boolean isMe, int rmsMessageType, int status, int mixType, boolean burn) {
        if (rmsMessageType == Rms.RMS_MESSAGE_TYPE_SYSTEM || rmsMessageType == Rms.RMS_MESSAGE_TYPE_CARD || burn) {
            return false;
        }
        if (isMe && (mixType & Rms.MIX_TYPE_CC) == 0) {
            return true;
        }
        if (status >= Rms.STATUS_SUCC) {
            return true;
        }
        return false;
    }

    /**
     * 检查是否可以将消息以短信方式重发
     * @param context
     * @param imdn
     * @return
     */
    public static boolean checkMsgCanTransSms(final Context context, final String imdn) {
        Cursor cursor = context.getContentResolver().query(Rms.CONTENT_URI_LOG,
                new String[] { Rms.BODY }, Rms.IMDN_STRING + "=?", new String[] { imdn }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String messageText = cursor.getString(0);
                    if (!RcsCheckUtils.checkTextOverMaxUtf8(messageText, RcsServiceManager.getMaxPageLimit())) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * dialog 设置 跳转到RcsApplication的RcsSettingActivity
     */
    public static void startRcsSettingActivity() {
        Intent intent = new Intent("com.cmcc.ccs.rcs.setting");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        RcsMmsInitHelper.getContext().startActivity(intent);
    }


    public static void startRcsTestSettingActivity() {
        Intent intent = new Intent("com.juphoon.rcs.testsetting");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        RcsMmsInitHelper.getContext().startActivity(intent);
    }



    /**
     * 跳转到注册界面
     */
    public static void startRcsRegisterActivity(Context context) {
        Intent intent = new Intent(context, RcsRegisterActivity.class);
        context.startActivity(intent);
    }


    /**
     * 判断字符是字母或数字
     * @param str
     * @return
     */
    public static boolean isNumberOrLetter(String str) {
        Pattern p = Pattern.compile("[0-9]*");
        Matcher m = p.matcher(str);
        if (m.matches()) {
            return true;
        }
        p = Pattern.compile("[a-zA-Z]");
        m = p.matcher(str);
        if (m.matches()) {
            return true;
        }
        return false;
    }

    /**
     * 转换文件大小
     *
     * @param size
     * @return
     */
    public static String formatFileSize(long size) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (size <= 0) {
            return wrongSize;
        }
        if (size < (1 << 10)) {
            fileSizeString = df.format((double) size) + "B";
        } else if (size < (1 << 20)) {
            fileSizeString = df.format((double) size / (1 << 10)) + "KB";
        } else if (size < (1 << 30)) {
            fileSizeString = df.format((double) size / (1 << 20)) + "MB";
        } else {
            fileSizeString = df.format((double) size / (1 << 30)) + "GB";
        }
        return fileSizeString;
    }
    public static void updatePriority(final Context context, final long threadId, final long priority) {
        ContentValues values = new ContentValues();
        values.put(RmsDefine.Threads.PRIORITY, priority);
        context.getContentResolver().update(Uri.withAppendedPath(RmsDefine.Threads.URI_THREAD_PRIORITY,
                String.valueOf(threadId)), values, null, null);
    }

    /**
     * 拍摄照片保存位置
     * @param activity
     * @param requestCode
     */
    public static void captureRmsPicture(Activity activity, Uri uri, int requestCode) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void stopAllProgressingFileWhenDelete(Context context, Collection<Long> threadIds) {
        Cursor cursor = context.getContentResolver().query(Rms.CONTENT_URI_LOG,
                new String[] { Rms.THREAD_ID, Rms.TRANS_ID, Rms.TYPE },
                Rms.STATUS + "=" + Rms.STATUS_PENDING + " And " + Rms.TRANS_ID + " NOT NULL", null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(0);
                    String transId = cursor.getString(1);
                    int type = cursor.getInt(2);
                    if (threadIds.contains(Long.valueOf(threadId))) {
                        RcsCallWrapper.rcsPauselFile(transId, type != Rms.MESSAGE_TYPE_INBOX);
                    }
                }
            } finally {
                 cursor.close();
            }
        }
    }

    /**
     * 获取群列表对应的threadId
     * @param context
     * @return
     */
    public static ArrayList<Long> getThreadIdWithGroups(Context context) {
        ArrayList<Long> threadIdList = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(sAllThreadsUri, THREADS_PROJECTION,
                RmsDefine.Threads.RMS_THREAD_TYPE + "=" + RmsDefine.RMS_GROUP_THREAD, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    threadIdList.add(cursor.getLong(cursor.getColumnIndex(RmsDefine.Threads._ID)));
                }
            } finally {
                cursor.close();
            }
        }
        return threadIdList;
    }

    /**
     * 判断文件大小是否超过限制
     * @return
     */
    public static boolean checkFileSize(String filePath, int limit) {
        if (new File(filePath).length() / 1024 > limit) {
            return false;
        }
        return true;
    }

    /**
     * 计算文件md5
     * @param file
     * @return
     */
    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    /**
     * 更新同步表的Rcs消息状态
     */
    public static String updateBugleRcsMessageStatus(int id, int status) {
        String conversationId = null;
        DatabaseWrapper db = DataModel.get().getDatabase();
        db.beginTransaction();
        try {
            Uri rmsUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(id));
            MessageData message = BugleDatabaseOperations.readMessageData(db, rmsUri);
            ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
            message.updateMessageStatus(status);
            BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
            db.setTransactionSuccessful();
            conversationId = message.getConversationId();
        } finally {
            db.endTransaction();
            return conversationId;
        }
    }

    /**
     * 更改同步表的群名称
     */
    public static void updateBugleGroupName(final Context context, final String groupChatId) {
        if (!TextUtils.isEmpty(groupChatId)) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    RcsGroupInfo groupInfo = RcsGroupHelper.loadGroupInfo(groupChatId);
                    // 这里可能会得不到群信息，比如创建群失败情况下
                    if (groupInfo == null) {
                        return;
                    }
                    long threadId = RmsDefine.Threads.getOrCreateThreadId(context, RmsDefine.RMS_GROUP_THREAD, groupInfo.mGroupChatId);
                    DatabaseWrapper db = DataModel.get().getDatabase();
                    db.beginTransaction();
                    try {
                        String convId = BugleDatabaseOperations.getOrCreateConversationFromThreadId(db, threadId, false, -1);
                        BugleDatabaseOperations.updateConversationNameInTransaction(db, convId, groupInfo.mSubject);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }).start();
        }
    }
    
    /**
     * 根据threadId从原始表获取thread_type 7.0 add
     * @param threadId
     * @return
     */
    public static int getThreadType(long threadId) {
        List<String> recipients = MmsUtils.getRecipientsByThread(threadId);
        if (recipients == null) {
            return RmsDefine.COMMON_THREAD;
        }
        if (recipients.size() == 0) {
            return RmsDefine.COMMON_THREAD;
        } else if (recipients.size() > 1) {
            return RmsDefine.BROADCAST_THREAD;
        } else {
            if (RcsGroupHelper.loadGroupInfo(recipients.get(0)) == null) {
                return RmsDefine.COMMON_THREAD;
            } else {
                return RmsDefine.RMS_GROUP_THREAD;
            }
        }
    }

    /**
     * 根据threadId得到threadType和priority
     * @param threadId
     * @return
     */
    public static HashMap<String, Object> getThreadTypeAndPriorityByThreadId(long threadId) {
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(sAllThreadsUri, THREADS_PROJECTION,
                RmsDefine.Threads._ID + "=" + threadId, null, null);
        HashMap<String, Object> threadMap = new HashMap<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    threadMap.put(RmsDefine.Threads.RMS_THREAD_TYPE,
                            cursor.getInt(cursor.getColumnIndex(RmsDefine.Threads.RMS_THREAD_TYPE)));
                    threadMap.put(RmsDefine.Threads.PRIORITY, cursor.getLong(
                            cursor.getColumnIndex(RmsDefine.Threads.PRIORITY)));
                } 
            } finally {
                cursor.close(); 
            }
        }
        return threadMap;
    }

   /**
    * 语音显示时长，视频大小时长，图片，文件显示大小
    * @param uri
    * @return
    */
   public static String getFileSizeAndDuration(Uri uri) {
       StringBuffer sb = new StringBuffer();
       Cursor c = RcsMmsInitHelper.getContext().getContentResolver().query(uri, new String[] {Rms.FILE_SIZE, Rms.FILE_DURATION,
               Rms.MESSAGE_TYPE}, null, null, null);
       if (c != null) {
           try {
               if (c.moveToFirst()) {
                   sb = new StringBuffer();
                   int messageType = c.getInt(2);
                   if (messageType == Rms.RMS_MESSAGE_TYPE_VOICE) {
                       sb.append(formatDuration((int) (c.getLong(1) / 1000)));
                   } else {
                       sb.append(formatFileSize(c.getLong(0)));
                       if (messageType == Rms.RMS_MESSAGE_TYPE_VIDEO) {
                           sb.append(" ");
                           sb.append(formatDuration((int) (c.getLong(1) / 1000)));
                       }
                   }
               }
           } finally {
               c.close();
           }
       }
       return sb.toString();
   }

   /**
    * 时长格式化 有小时显示hh:mm:ss,否则显示mm:ss
    * @param duration 秒
    */
   public static String formatDuration(int duration) {
       int hour = 0;
       int minute = 0;
       int second = 0;
       hour = duration / 3600;
       minute = duration / 60 - hour * 60;
       second = duration % 60;
       if (hour > 0) {
           return String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second);
       }
       return String.format(Locale.getDefault(), "%02d:%02d", minute, second);
   }

   /**
    * 获取contentType类型
    * @param fileType
    * @return
    */
   public static String getContentType(int fileType) {
       String type = null;
       switch (fileType) {
           case RmsDefine.Rms.RMS_MESSAGE_TYPE_IMAGE:
               type = ContentType.IMAGE_UNSPECIFIED;
               break;
           case RmsDefine.Rms.RMS_MESSAGE_TYPE_VIDEO:
               type = ContentType.VIDEO_UNSPECIFIED;
               break;
           case RmsDefine.Rms.RMS_MESSAGE_TYPE_VOICE:
               type = ContentType.AUDIO_UNSPECIFIED;
               break;
           default:
               type = ContentType.MULTIPART_MIXED;
               break;
       }
       return type;
   }

    /**
     * 通过图片路径剪裁图片并生成bitmap
     * @param srcPath 文件路径
     * @return 裁剪压缩后的Bitmap
     */
    public static Bitmap getCompressImage(String srcPath) {
        Bitmap bitmap = null;
        if (!TextUtils.isEmpty(srcPath)) {
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
            newOpts.inJustDecodeBounds = false;
            int width = newOpts.outWidth;
            int height = newOpts.outHeight;
            float toHeight = 800f;
            float toWidth = 480f;
            int scale = 1;
            if (width > height && width > toWidth) {
                scale = (int) (newOpts.outWidth / toWidth);
            } else if (width < height && height > toHeight) {
                scale = (int) (newOpts.outHeight / toHeight);
            }
            if (scale <= 0) {
                scale = 1;
            }
            newOpts.inSampleSize = scale;
            bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        }
        return bitmap;
    }

    /**

   /**
    * 压缩图片
    * @param srcPath
    * @param limitSize kb
    * @return genrate file
    */
   public static String compressImage(String srcPath, int limitSize) {
       BitmapFactory.Options newOpts = new BitmapFactory.Options();
       newOpts.inJustDecodeBounds = true;
       Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
       newOpts.inJustDecodeBounds = false;
       int width = newOpts.outWidth;
       int height = newOpts.outHeight;
       float toHeight = 800f;
       float toWidth = 480f;
       int scale = 1;
       if (width > height && width > toWidth) {
           scale = (int) (newOpts.outWidth / toWidth);
       } else if (width < height && height > toHeight) {
           scale = (int) (newOpts.outHeight / toHeight);
       }
       if (scale <= 0) {
           scale = 1;
       }
       newOpts.inSampleSize = scale;
       bitmap = BitmapFactory.decodeFile(srcPath, newOpts);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
       int options = 100;
       while (baos.toByteArray().length > limitSize) {
           baos.reset();
           bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
           options -= 10;
       }
       String destPath = RcsFileUtils.getNotExistFileByTime(RmsDefine.RMS_FILE_PATH, RcsFileUtils.getFileSuffix(srcPath));
       RcsFileUtils.saveFile(destPath, baos.toByteArray());

       return destPath;
   }

   public static Bitmap genPreviewImage(String srcPath) {
       if (!TextUtils.isEmpty(srcPath) && new File(srcPath).exists()) {
           BitmapFactory.Options newOpts = new BitmapFactory.Options();
           newOpts.inJustDecodeBounds = true;
           Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
           newOpts.inJustDecodeBounds = false;
           int width = newOpts.outWidth;
           int height = newOpts.outHeight;
           float toHeight = 800;
           float toWidth = 600;
           int scale = 1;
           if (width > height && width > toWidth) {
               scale = (int) (newOpts.outWidth / toWidth);
           } else if (width < height && height > toHeight) {
               scale = (int) (newOpts.outHeight / toHeight);
           }
           if (scale <= 0) {
               scale = 1;
           }
           newOpts.inSampleSize = scale;
           bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
           if (bitmap == null) {
               return null;
           }
           ByteArrayOutputStream baos = new ByteArrayOutputStream();
           bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
           int options = 100;
           while (baos.toByteArray().length > 500*1024 && options > 0) {
               baos.reset();
               bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
               options -= 10;
           }
           bitmap.recycle();
           return BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length);
       }
       return null;
   }

   /**
    * Add an RMS to the given URI with thread_id specified.
    *
    * @param resolver the content resolver to use
    * @param uri the URI to add the message to
    * @param subId subId for the receiving sim
    * @param address the address of the sender
    * @param body the body of the message
    * @param subject the psuedo-subject of the message
    * @param date the timestamp for the message
    * @param read true if the message has been read, false if not
    * @param threadId the thread_id of the message
    * @return the URI for the new message
    */
   private static Uri addMessageToUri(final Context context,
                                      final Uri uri, final int subId, final String address, final String body,
                                      final String subject, final Long date, final Long timestamp, final boolean read, final boolean seen,
                                      final int status, final int type, final long threadId, MessagePartData part) {
       final ContentValues values = new ContentValues();
       if (OsUtil.isAtLeastL_MR1()) {
           values.put(Rms.SUB_ID, subId);
       }
       values.put(Rms.ADDRESS, address);
       values.put(Rms.DATE, date);
       values.put(Rms.THREAD_ID, threadId);
       values.put(Rms.TYPE, type);
       values.put(Rms.TIMESTAMP, timestamp);
       values.put(Rms.READ, read ? 1 : 0);
       values.put(Rms.SEEN, seen ? 1 : 0);
       values.put(Rms.STATUS, status);
       String partArr[] = part.toString().split(" ");
       if (partArr.length > 1) {
           Uri fileUri = Uri.parse(partArr[1].substring(1, partArr[1].length() - 1));
           File file;
           String filePath = RcsFileUtils.getFilePathByUri(context, fileUri);
           if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
               filePath = RmsDefine.RMS_FILE_PATH + "/" + UUID.randomUUID().toString() + "." + getFileSuffix(fileUri);
               file = new File(filePath);
               try {
                   copyToFile(context.getContentResolver().openInputStream(fileUri), file);
               } catch (FileNotFoundException e) {
                   Log.d(TAG, "copyToFile failed ");
               }
           } else {
               file = new File(filePath);
           }
           String fileType = partArr[0];
           values.put(Rms.FILE_NAME, file.getName());
           values.put(Rms.FILE_PATH, filePath);
           values.put(Rms.FILE_TYPE, fileType);
           values.put(Rms.FILE_SIZE, file.length());
           String thumbPath = RcsFileUtils.getNotExistThumbFilePath(RmsDefine.RMS_FILE_PATH, file.getName(), "jpg");
           Bitmap bitmap = null;
           int duration = 0;
           if (fileType.startsWith("image/")) {
               values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_IMAGE);
               bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
               if (bitmap != null)
                   RcsFileUtils.genThumbBitmap(thumbPath, bitmap);
           } else if (fileType.startsWith("video/")) {
               values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_VIDEO);
               bitmap = RcsFileUtils.getVideoFirstFrameBitmap(file.getAbsolutePath());
               if (bitmap != null)
                   RcsFileUtils.genThumbBitmap(thumbPath, bitmap);
               duration = RcsFileUtils.getMediaDuring(file.getAbsolutePath()) * 1000;
           } else if (fileType.startsWith("audio/")) {
               values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_VOICE);
               duration = RcsFileUtils.getMediaDuring(file.getAbsolutePath()) * 1000;
           } else if (fileType.startsWith(ContentType.APP_GEO_MESSAGE)) {
               values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_GEO);
           } else if (fileType.startsWith(ContentType.TEXT_VCARD)) {
               values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_VCARD);
           } else if (fileType.startsWith("application/")) {
               values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_FILE);
           }

           if (new File(thumbPath).exists())
               values.put(Rms.THUMB_PATH, thumbPath);
           values.put(Rms.FILE_DURATION, duration);

       } else {
           values.put(Rms.BODY, body);
       }

       return context.getContentResolver().insert(uri, values);
   }

   public static MessageData createRmsMessage(final RmsMessage rms,
                                              final String conversationId, final String participantId, final String selfId,
                                              final int bugleStatus) {
       Assert.notNull(rms);

       final MessageData message = MessageData.createRmsMessage(rms.getUri(),
               participantId, selfId, rms.mTransId, conversationId, bugleStatus,
               rms.mSeen, rms.mRead, rms.mTimestampSentInMillis, rms.mTimestampInMillis,
               rms.mBody, rms.mContentType);
       final MessagePartData messagePart = createRmsMessagePart(rms);
       // Import media and text parts (skip SMIL and others)
       if (messagePart != null) {
           message.addPart(messagePart);
       }

       if (!message.getParts().iterator().hasNext()) {
           message.addPart(MessagePartData.createEmptyMessagePart());
       }

       return message;
   }

   public static MessagePartData createRmsMessagePart(final RmsMessage part) {
       MessagePartData messagePart = null;
       Uri dataUri = null;
       String fileType = part.mContentType;
       int height = MessagePartData.UNSPECIFIED_SIZE;
       int width = MessagePartData.UNSPECIFIED_SIZE;
       boolean isPaMsg = !TextUtils.isEmpty(part.mPaUuId);
       boolean isOutgoing = part.mType != Rms.MESSAGE_TYPE_INBOX;
       if (ContentType.isMediaType(fileType)) {
           if (ContentType.isVCardType(fileType)
                   || ContentType.isGeoType(fileType) || ContentType.isFileType(fileType)) {
               if (part.mStatus >= Rms.STATUS_SUCC || isOutgoing) {
                   dataUri = part.getDataUri();
               } else {
                   dataUri = Uri.parse("file://");
               }
               if (ContentType.isFileType(fileType)) {
                   part.mBody = part.mFileName;
               }
           } else if (ContentType.isImageType(fileType) || ContentType.isVideoType(fileType)) {
               if (part.mStatus >= Rms.STATUS_SUCC || isOutgoing) {
                   dataUri = part.getDataUri();
               } else if (!TextUtils.isEmpty(part.mThumbPath) && new File(part.mThumbPath).exists()) {// 缩略图下载完成
                   dataUri = part.getThumbUri();
               } else {
                   dataUri = Uri.parse("file://");
               }
               height = part.mHeight > 0 ? part.mHeight : MessagePartData.UNSPECIFIED_SIZE;
               width = part.mWidth> 0 ? part.mWidth : MessagePartData.UNSPECIFIED_SIZE;
           } else if (ContentType.isAudioType(fileType)) {
               if (part.mStatus >= Rms.STATUS_SUCC || isOutgoing) {
                   dataUri = part.getDataUri();
               } else  {
                   dataUri = Uri.parse("file://");
               }
           } else if (ContentType.isChatbotCard(fileType)) {
               dataUri = Uri.parse("file://" + part.mBody);
               JSONObject jsonObject = new JSONObject();
               try {
                   jsonObject.put(Rms.CHATBOT_SERVICE_ID, part.mRmsChatbotServiceId);
                   jsonObject.put(Rms.BODY, part.mBody);
                   jsonObject.put(Rms.RMS_EXTRA, part.mRmsExtra);
                   jsonObject.put(Rms.RMS_EXTRA2, part.mRmsExtra2);
//                   jsonObject.put(Rms.CONVERSATION_ID, part.mConversationId);
                   jsonObject.put(Rms.TRAFFIC_TYPE, part.mTrafficType);
                   jsonObject.put(Rms.CONTRIBUTION_ID, part.mContributionId);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
               part.mBody = jsonObject.toString();
           } else {
               dataUri = part.getDataUri();
           }
           messagePart = MessagePartData.createMediaMessagePartWithText(part.mBody, fileType,
                   dataUri, width, height);
       }
       return messagePart;
   }

   public static MessagePartData createRmsMessagePartDownLoaded(final RmsMessage part) {
       MessagePartData messagePart = null;
       Uri dataUri = part.getDataUri();
       messagePart = MessagePartData.createMediaMessagePart(part.mContentType, dataUri,
                   MessagePartData.UNSPECIFIED_SIZE, MessagePartData.UNSPECIFIED_SIZE);
       return messagePart;
   }

   /**
    * Load RMS from telephony
    *
    * @param rmsUri The RMS Uri
    * @return A memory copy of the RMS including parts (but not addresses)
    */
   public static RmsMessage loadRms(final Uri rmsUri) {
       final Context context = Factory.get().getApplicationContext();
       final ContentResolver resolver = context.getContentResolver();
       RmsMessage rms = null;
       Cursor cursor = null;
       try {
           cursor = SqliteWrapper.query(context, resolver,
                   rmsUri,
                   RmsMessage.getProjection(),
                   null/*selection*/, null/*selectionArgs*/, null/*sortOrder*/);
           if (cursor != null && cursor.moveToFirst()) {
               rms = RmsMessage.get(cursor);
           }
       } catch (final SQLiteException e) {
           LogUtil.e(TAG, "RmsLoader: query pdu failure: " + e, e);
       } finally {
           if (cursor != null) {
               cursor.close();
           }
       }
       if (rms == null) {
           return null;
       }
       return rms;
   }

   private static String getFileSuffix(Uri fileUri) {
       String fileSuffix = "unknown";
       String uri = fileUri.toString();
       String u[] = uri.split("=");
       if (u.length > 1) {
           fileSuffix = u[1];
       }
       return fileSuffix;
   }

   public static boolean copyToFile(InputStream inputStream, File destFile) {
       try {
           if (destFile.exists()) {
               destFile.delete();
           }
           OutputStream out = new FileOutputStream(destFile);
           try {
               byte[] buffer = new byte[4096];
               int bytesRead;
               while ((bytesRead = inputStream.read(buffer)) >= 0) {
                   out.write(buffer, 0, bytesRead);
               }
           } finally {
               out.close();
           }
           return true;
       } catch (IOException e) {
           return false;
       }
   }

   public static Uri getRmsUriWithTransId(String transId) {
       Uri rmsUri = null;
       Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
               new String[]{Rms._ID}, Rms.TRANS_ID + "=?", new String[]{transId}, null);
       if (cursor != null) {
           try {
               if (cursor.moveToFirst()) {
                   String messageId = cursor.getString(cursor.getColumnIndex(Rms._ID));
                   rmsUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(messageId));
               }
           } finally {
               cursor.close();
           }
       }
       return rmsUri;
   }

   public static Uri getRmsUriWithImdnId(String imdnId) {
       Uri rmsUri = null;
       Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
               new String[]{Rms._ID}, Rms.IMDN_STRING + "=?", new String[]{imdnId}, Rms._ID + " asc");
       if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String messageId = cursor.getString(cursor.getColumnIndex(Rms._ID));
                    rmsUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(messageId));
                }
            } finally {
                cursor.close();
            }
       }
       return rmsUri;
   }

   public static String geoFileToString(String filePath) {
       String content = "";
       if (!TextUtils.isEmpty(filePath)) {
           content = readGeo(filePath);
           if (content != null) {
               //防止sdk没有start,xml改成由上层解析
               String geo = loadGeoXml(content);
               if (!TextUtils.isEmpty(geo)) {
                   String[] params = geo.split(";", -1);
                   if (params.length >= 4) {
                       JSONObject obj = new JSONObject();
                       try {
                           obj.put(RcsJsonParamConstants.RCS_JSON_GS_LATITUDE, params[0]);
                           obj.put(RcsJsonParamConstants.RCS_JSON_GS_LONGTUDE, params[1]);
                           obj.put(RcsJsonParamConstants.RCS_JSON_GS_RADIUS, params[2]);
                           obj.put(RcsJsonParamConstants.RCS_JSON_GS_LOCATION_NAME, params[3]);
                       } catch (JSONException e) {
                           e.printStackTrace();
                       }
                       content = obj.toString();
                   }
               }
           } else {
               Log.d(TAG, "geo restore no content");
           }
       }
       return content;
   }

   private static String readGeo(String filePath) {
       try {
           FileInputStream fis = new FileInputStream(new File(filePath));
           ByteArrayOutputStream stream = new ByteArrayOutputStream();
           byte[] buf = new byte[1024];
           int len = 0;
           while ((len = fis.read(buf)) != -1) {
               stream.write(buf, 0, len);
           }
           fis.close();
           stream.close();
           return stream.toString();
       } catch (FileNotFoundException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return null;
   }

   private static final String[] MEDIA_CONTENT_PROJECTION = new String[]{
           MediaStore.MediaColumns.MIME_TYPE
   };

   @DoesNotRunOnMainThread
   public static String getContentType(final ContentResolver cr, final Uri uri) {
       // Figure out the content type of media.
       String contentType = null;
       Cursor cursor = null;
       if (UriUtil.isMediaStoreUri(uri)) {
           try {
               cursor = cr.query(uri, MEDIA_CONTENT_PROJECTION, null, null, null);

               if (cursor != null && cursor.moveToFirst()) {
                   contentType = cursor.getString(0);
               }
           } finally {
               if (cursor != null) {
                   cursor.close();
               }
           }
       }
       if (uri.toString().endsWith(".vcf")) {
           contentType = ContentType.TEXT_VCARD;
       }
       if (contentType == null) {
           // Last ditch effort to get the content type. Look at the file extension.
           contentType = ContentType.getContentTypeFromExtension(uri.toString(),
                   RcsImServiceConstants.MTC_IM_FILE_CONT_APP_OSTRM);
       }
       return contentType;
   }

   public static int getMixType(boolean cc, boolean silence, boolean direct, boolean at) {
       int mixType = Rms.MIX_TYPE_COMMON;
       if (cc) mixType += Rms.MIX_TYPE_CC;
       if (silence) mixType += Rms.MIX_TYPE_SILENCE;
       if (direct) mixType += Rms.MIX_TYPE_DIRECT;
       if (at) mixType += Rms.MIX_TYPE_AT;
       return mixType;
   }

    /**
     * Gets the corresponding path to a file from the given content:// URI
     *
     * @param contentUri The content:// URI to find the file path from
     * @param context    Context
     * @return the file path as a string
     */

    public static String getFileNameFromContentUri(Uri contentUri, Context context) {
        if (contentUri == null) {
            return null;
        }
        String fileName = "";
        String[] filePathColumn = {MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, filePathColumn, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            fileName = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            cursor.close();
            return fileName;
        }
        return fileName;
    }

    /**
    * 将文件路径转换成content类型uri
    * 注:sdk>24,调用系统播放需要音视频传入的uri类型为content
    * @param context
    * @return content Uri
    */
   public static Uri getVideoContentUri(Context context, String filePath) {
       Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
               new String[] { MediaStore.Video.Media._ID }, MediaStore.Video.Media.DATA + "=? ",
               new String[] { filePath }, null);
       if (cursor != null) {
           try {
               if (cursor.moveToFirst()) {
                   int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                   Uri baseUri = Uri.parse("content://media/external/video/media");
                   return Uri.withAppendedPath(baseUri, String.valueOf(id));
               }
           } finally {
               cursor.close();
           }
       }
       if (new File(filePath).exists()) {
           ContentValues values = new ContentValues();
           values.put(MediaStore.Video.Media.DATA, filePath);
           values.put(MediaStore.Video.Media.MIME_TYPE, "video/*");
           return context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
       } else {
           return null;
       }
   }

   /**
    * 获取是否是彩信模式
    */
   public static boolean getMmsModel(String conversationId) {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
       return prefs.getBoolean(conversationId, false);
   }

   /**
    * 设置会话中是否是彩信模式
    */
   public static void setMmsModel(String conversationId, boolean isMms) {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
       prefs.edit().putBoolean(conversationId, isMms).commit();
   }

   /**
    * 删除对应的彩信标识
    */
   public static void removeMmsModel(String conversationId) {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
       prefs.edit().remove(conversationId).commit();
   }

   /**
    * 复制到剪贴板
    * @param context
    * @param text
    */
   public static void copyToClipboard(Context context, String text) {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
           android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) context
                   .getSystemService(Context.CLIPBOARD_SERVICE);
           clipboardManager.setPrimaryClip(ClipData.newPlainText("text", text));
       } else {
           android.text.ClipboardManager cbm = (android.text.ClipboardManager) context
                   .getSystemService(Context.CLIPBOARD_SERVICE);
           cbm.setText(text);
       }
   }

   /**
    * 判断剪贴板数据
    * @param context
    * @return
    */
   public static CharSequence getClipboardText(Context context) {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
           android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) context
                   .getSystemService(Context.CLIPBOARD_SERVICE);
           if (clipboardManager.hasPrimaryClip()) {
               return clipboardManager.getPrimaryClip().getItemAt(0).getText();
           }
           return null;
       } else {
           android.text.ClipboardManager cbm = (android.text.ClipboardManager) context
                   .getSystemService(Context.CLIPBOARD_SERVICE);
           return cbm.getText();
       }
   }

   /**
    * 设置是否是Rcs模式
    * @param isRcs
    */
   public static void setIsRcsMode(boolean isRcs) {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
       prefs.edit().putBoolean(IS_RCS_MODE, isRcs).commit();
   }

   /**
    * 获取是否是彩信模式
    */
   public static boolean getIsRcsMode() {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RcsMmsInitHelper.getContext());
       return prefs.getBoolean(IS_RCS_MODE, false);
   }
   
   /**
    * 更新原始表及同步表传输中的消息状态为失败
    * @param id 原始表消息id
    */
   public static void updateMessageFailed(final int id, boolean send) {
       if (id == -1) {
           return;
       }
       int status = -1;
       ContentValues values = new ContentValues();
       if (send) {
           status = MessageData.BUGLE_STATUS_OUTGOING_FAILED;
           values.put(Rms.TYPE, Rms.MESSAGE_TYPE_FAILED);
           values.put(Rms.STATUS, Rms.STATUS_FAIL);
       } else {
           status = MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED;
           values.put(Rms.STATUS, Rms.STATUS_FAIL);
       }
       RcsMmsInitHelper.getContext().getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(id)),
               values, null, null);
       String conversationId = RcsMmsUtils.updateBugleRcsMessageStatus(id, status);
       if (!TextUtils.isEmpty(conversationId)) {
           MessagingContentProvider.notifyMessagesChanged(conversationId);
           MessagingContentProvider.notifyPartsChanged();
       }
   }

   /**
    * 更新传输中的群消息为失败
    * @param groupChatId
    */
   public static void updateGroupMessageFailed(final String groupChatId, final boolean send) {
       if (TextUtils.isEmpty(groupChatId)) {
           return;
       }
       new Thread(new Runnable() {

           @Override
           public void run() {
               String selection = null;
               if (send) {
                   selection = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_QUEUED + " or " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX;
               } else {
                   selection = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX + " and " + Rms.STATUS + "=" + Rms.STATUS_PENDING;
               }
               Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[] { Rms._ID },
                       Rms.GROUP_CHAT_ID + "=? And (" + selection + ")",
                       new String[] { groupChatId }, null);
               if (cursor != null) {
                   while (cursor.moveToNext()) {
                       int id = cursor.getInt(0);
                       updateMessageFailed(id, send);
                   }
                   cursor.close();
               }
           }
       }).start();
   }

   private static Object getSubscriptionManagerObj(Context context) {
       try {
           return ReflecterHelper.invokeStaticMethod("android.telephony.SubscriptionManager", "from",
                   new Object[] { context }, new Class[] { Context.class });
       } catch (ClassNotFoundException e) {
           e.printStackTrace();
       } catch (NoSuchMethodException e) {
           e.printStackTrace();
       } catch (IllegalArgumentException e) {
           e.printStackTrace();
       } catch (InvocationTargetException e) {
           e.printStackTrace();
       } catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       return null;
   }

   public static int getActiveSubCount(Context context) {
       Object obj = getSubscriptionManagerObj(context);
       if (obj == null) {
           return 0;
       }
       try {
           int[] ret = (int[])ReflecterHelper.invokeMethod(obj, "getActiveSubscriptionIdList", null, null);
           return ret.length;
       } catch (NoSuchMethodException e) {
           e.printStackTrace();
       } catch (IllegalArgumentException e) {
           e.printStackTrace();
       } catch (InvocationTargetException e) {
           e.printStackTrace();
       } catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       return 0;
   }

   /**
    *  获得RcsApplication 版本号
    * @param context
    * @return
    */
   public static String getRcsAppVersionName(Context context) {
       PackageManager pm = context.getPackageManager();
       String versionName = "";
       try {
           PackageInfo pi = pm.getPackageInfo("com.juphoon.service", 0);
           versionName = pi.versionName;
       } catch (NameNotFoundException e) {
           e.printStackTrace();
       }
       return versionName;
   }

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    /**
     * 解析地理位置xml
     * @param xmlMsg
     * @return
     */
    public static String loadGeoXml(String xmlMsg) {
        try {
            String label = null;
            String pos = null;
            String radius = null;
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xmlMsg));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("rcspushlocation")) {
                        label = parser.getAttributeValue(null, "label");
                    } else if (name.equals("pos")) {
                        pos = parser.nextText();
                    } else if (name.equals("radius")){
                        radius = parser.nextText();
                    }
                }
                eventType = parser.next();
            }
            if (!TextUtils.isEmpty(pos) && pos.split(" ").length == 2) {
                String[] position = pos.split(" ");
                StringBuilder builder = new StringBuilder();
                builder.append(position[0])
                        .append(";").append(position[1])
                        .append(";").append(TextUtils.isEmpty(radius) ? "0.0" : radius)
                        .append(";").append(TextUtils.isEmpty(label) ? "" : label);
                return builder.toString();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Long> getRmsAttachSmsIds() {
        List<Long> ids = new ArrayList<>();
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[]{Rms.SMS_ID}, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(0));
                }
            } finally {
                cursor.close();
            }
        }
        return ids;
    }

    public static long getRmsAttachSmsId(Uri rmsUri) {
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(rmsUri, new String[]{Rms.SMS_ID}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }
        return -1;
    }

    public static String getMessageText(String text, int rmsMsgType) {
        if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_VOICE || rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_AUDIO) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.voice_message) + "]";
        } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_IMAGE || rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_IMAGE) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.image_message) + "]";
        } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_VIDEO || rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VIDEO) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.video_message) + "]";
        } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_VCARD || rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VCARD) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.vcard_message) + "]";
        } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_FILE || rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_FILE) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.file_message) + "]";
        } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_GEO || rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_GEO) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.geo_message) + "]";
        } else if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_CHATBOT_CARD) {
            return "[" + RcsMmsInitHelper.getContext().getString(R.string.card_message) + "]";
        } else {
            return text;
        }
    }

    public static boolean isRmsInMessagingMode() {
        return TextUtils.equals(BuildConfig.FLAVOR, "RmsInMessaging");
    }

    public static String getMessageDisposition(int rmsMsgType) {
        if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_VOICE) {
            return MtcImFthttp.MTC_IM_FTHTTP_DISPOSITION_RENDER;
        }
        return MtcImFthttp.MTC_IM_FTHTTP_DISPOSITION_ATTACHMENT;
    }

    /**
     * 将音频文件的contentType转化为展示文件需要的类型
     */

    public static String transContentType(int rmsMsgType, String contentType) {
        if (rmsMsgType == Rms.RMS_MESSAGE_TYPE_FILE) {
            return ContentType.APP_FILE_MESSAGE;
        }
        return contentType;
    }

    public static void deleteRms(List<Long> threadIds) {
        for (Long threadId : threadIds) {
            RcsMmsInitHelper.getContext().getContentResolver().delete(Rms.CONTENT_URI_LOG, Rms.THREAD_ID + " = " + threadId, null);
        }
    }

    public static void sendDeepLinkBroadCast(String recipients, String suggestions) {
        JSONObject jsonObj = new JSONObject();
        try {
            if (!TextUtils.isEmpty(recipients)) {
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_ADDRESS, RcsChatbotHelper.formatServiceIdWithNoSip(recipients));
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID, recipients);
            } else {
                return;
            }
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT, true);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_BODY, "点击按钮快速回复");//暂时写死
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_ACTION, RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_RECV_MSG);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT_SUGGESTION, suggestions);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_TIMESTAMP, System.currentTimeMillis());
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_IMDN_ID, UUID.randomUUID().toString());
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_IMDN_TYPE,  0);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_CONT_ID, UUID.randomUUID().toString());
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_CONV_ID, UUID.randomUUID().toString());
            jsonObj.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
            jsonObj.put(Rms.STATUS, Rms.STATUS_SUCC);
            jsonObj.put(Rms.MIX_TYPE, Rms.MIX_TYPE_COMMON);
            jsonObj.put(RcsJsonParamConstants.RCS_JSON_SUB_ID, RcsServiceManager.getSubId());

            Intent intent = new Intent(RcsJsonParamConstants.RCS_ACTION_IM_NOTIFY);
            intent.putExtra(RcsJsonParamConstants.RCS_JSON_KEY, jsonObj.toString());
            intent.setComponent(new ComponentName("com.android.messaging", "com.juphoon.helper.mms.RcsWakeupReceiver"));
            RcsMmsInitHelper.getContext().sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getImdnFromSmsUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return "";
        }
        RcsDatabaseMessages.RmsMessage rms = RcsMmsUtils.loadRms(Uri.parse(uri));
        return rms.mImdn;
    }

    public static String getChatbotNumberFromSmsUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return "";
        }
        RcsDatabaseMessages.RmsMessage rms = RcsMmsUtils.loadRms(Uri.parse(uri));
        return RcsChatbotHelper.parseChatbotServierIdToNumber(rms.mRmsChatbotServiceId);
    }

    public static void copyFile(File srcFile, File destFile) {
        try (InputStream in = new FileInputStream(srcFile)) {
            copyToFile(in, destFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getRmsConversationIdFromThreadID(Long threadId) {
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[]{Rms.CONVERSATION_ID},
                Rms.THREAD_ID + "=?", new String[]{String.valueOf(threadId)}, Rms._ID + " desc");
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return "";
    }

    public static String getRmsConversationIdFromChatbotId(String chatbotId) {
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[]{Rms.CONVERSATION_ID},
                Rms.CHATBOT_SERVICE_ID + "=?", new String[]{String.valueOf(chatbotId)}, Rms._ID + " desc");
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return "";
    }
}
