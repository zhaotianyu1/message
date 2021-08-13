package com.juphoon.helper.mms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class RcsMessageForwardHelper {

    private static final String TAG = "RcsMessageForwardHelper";

    private static final String[] PROJECTION = new String[]{Rms.BODY, Rms.FILE_PATH, Rms.MESSAGE_TYPE, Rms.RMS_EXTRA};

    private static final int COLUMN_BODY = 0;
    private static final int COLUMN_FILE_PATH = 1;
    private static final int COLUMN_RMS_MESSAGE_TYPE = 2;
    private static final int COLUMN_RMS_EXTRA = 3;

    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
    }

    public static void uninit() {

    }

    /**
     * @param threadType
     * @param msgId
     * @param contacts   如果是群发的话，号码以 ";" 隔开
     * @return
     */
    public static boolean forwardMessage(int threadType, long msgId, String contacts) {
        long threadId = getThreadId(threadType, contacts);
        RcsMessage message = genRcsMessageFromCursor(msgId);
        if (message != null) {
            return new RcsMessageSender(sContext, threadId, threadType, contacts, message).sendMessage(threadId);
        }
        return false;
    }

    public static boolean forwardSmsMessage(int threadType, String sms, String contacts) {
        long threadId = getThreadId(threadType, contacts);
        RcsMessage message = new RcsMessage(Rms.RMS_MESSAGE_TYPE_TEXT, sms);
        return new RcsMessageSender(sContext, threadId, threadType, contacts, message).sendMessage(threadId);
    }

    public static boolean forwardFileMessage(int threadType, int fileType, Uri file, String contacts) {
        long threadId = getThreadId(threadType, contacts);
        RcsMessage message = new RcsMessage(fileType, file);
        return new RcsMessageSender(sContext, threadId, threadType, contacts, message).sendMessage(threadId);
    }

    private static RcsMessage genRcsMessageFromCursor(long msgId) {
        Cursor cursor = sContext.getContentResolver().query(Rms.CONTENT_URI_LOG, PROJECTION, Rms._ID + "=" + msgId, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                RcsMessage message = null;
                int type = cursor.getInt(COLUMN_RMS_MESSAGE_TYPE);
                if (type == Rms.RMS_MESSAGE_TYPE_VOICE || type == Rms.RMS_MESSAGE_TYPE_IMAGE || type == Rms.RMS_MESSAGE_TYPE_VIDEO || type == Rms.RMS_MESSAGE_TYPE_VCARD
                        || type == Rms.RMS_MESSAGE_TYPE_FILE) {
                    Uri uri = Uri.fromFile(new File(cursor.getString(COLUMN_FILE_PATH)));
                    if (uri != null) {
                        message = new RcsMessage(type, uri);
                    }
                } else if (type == Rms.RMS_MESSAGE_TYPE_GEO) {
                    String body = cursor.getString(COLUMN_RMS_EXTRA);
                    message = new RcsMessage(type, body);
                } else {
                    String body = cursor.getString(COLUMN_BODY);
                    message = new RcsMessage(type, body);
                }
                return message;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private static long getThreadId(int threadType, String contacts) {
        if (threadType == RmsDefine.COMMON_THREAD) {
            return Telephony.Threads.getOrCreateThreadId(sContext, contacts);
        } else if (threadType == RmsDefine.BROADCAST_THREAD) {
            Set<String> setContacts = new HashSet<>();
            String[] c = contacts.split(";");
            for (String contact : c) {
                setContacts.add(contact);
            }
            return Telephony.Threads.getOrCreateThreadId(sContext, setContacts);
        } else {
            return RmsDefine.Threads.getOrCreateThreadId(sContext, threadType, contacts);
        }
    }

}
