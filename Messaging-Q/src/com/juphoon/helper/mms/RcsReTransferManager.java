package com.juphoon.helper.mms;

import android.database.Cursor;
import android.text.TextUtils;

import com.juphoon.cmcc.lemon.MtcGsGinfoConstants;
import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.cmcc.lemon.MtcImSessConstants;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsCheckUtils;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsGroupInfo;
import com.juphoon.rcs.tool.RcsNetUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RcsReTransferManager {

    private final static String TAG = "RcsReTransferManager";

    private static final int RETRY_COUNT = 3;

    public static final String[] PROJECTION = new String[] {
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
            Rms.MIX_TYPE, //19
            Rms.CONVERSATION_ID//20
    };

    private static final int COLUMN_ID = 0;
    private static final int COLUMN_THREAD_ID = 1;
    private static final int COLUMN_MESSAGE_TYPE = 2;
    private static final int COLUMN_ADDRESS = 3;
    private static final int COLUMN_BODY = 4;
    private static final int COLUMN_STATUS = 5;
    private static final int COLUMN_DATE = 6;
    private static final int COLUMN_FILE_PATH = 7;
    private static final int COLUMN_FILE_TYPE = 8;
    private static final int COLUMN_THUMB_PATH = 9;
    private static final int COLUMN_GROUP_CHAT_ID = 10;
    private static final int COLUMN_PA_UUID = 11;
    private static final int COLUMN_DURATION = 12;
    private static final int COLUMN_TYPE = 13;
    private static final int COLUMN_TRANS_ID = 14;
    private static final int COLUMN_IMDN_STRING = 15;
    private static final int COLUMN_TRANS_SIZE = 16;
    private static final int COLUMN_FILE_SIZE = 17;
    private static final int COLUMN_RMS_EXTRA = 18;
    private static final int COLUMN_MIX_TYPE = 19;
    private static final int COLUMN_CONVERSATION_ID = 20;

    private static Map<String, Integer> sMapSendTryTimes = new HashMap<String, Integer>();
    private static Map<String, Integer> sMapRecvTryTimes = new HashMap<String, Integer>();

    public static void removeFromRetransferMap(JSONObject jsonObj) {
        boolean send = false;
        String keyId = null;
        String jsonAction = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ACTION);
        String imdn = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        if (TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_OK)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_FAILED)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_MSG_SEND_RESULT)) {
            send = true;
            keyId = imdn;
        } else if (TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_OK)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_FAILED)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_OK)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_FAILED)) {
            send = true;
            keyId = transId;
        } else if (TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_DONE)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_FAILED)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_DONE)
                || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_FAILED)) {
            send = false;
            keyId = transId;
        } else {
            return;
        }
        if (send) {
            if (sMapSendTryTimes.containsKey(keyId)) {
                sMapSendTryTimes.remove(keyId);
            }
        } else {
            if (sMapRecvTryTimes.containsKey(keyId)) {
                sMapRecvTryTimes.remove(keyId);
            }
        }
    }

    public static boolean retransferIfNeed(JSONObject jsonObj) {
        if (!RcsNetUtils.checkNet(RcsMmsInitHelper.getContext())) {
            return false;
        }
        boolean ret = false;
        int errorCode = jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE);
        String jsonAction = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ACTION);
        // 文本消息用该id找
        String imdn = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
        // 文件传输用该id找
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        String keyId = null;
        if (needRetransferFromErrcode(errorCode)) {
            boolean send = false;
            if (TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_FAILED)
                    || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_MSG_SEND_RESULT)) {
                send = true;
                keyId = imdn;
            } else if (TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_FAILED)
                    || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_FAILED)) {
                send = true;
                keyId = transId;
            } else if (TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_FAILED)
                    || TextUtils.equals(jsonAction, RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_FAILED)) {
                send = false;
                keyId = transId;
            } else {
                return ret;
            }
            if (checkIsOverTimes(keyId, send)) {
                return false;
            }
            Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, PROJECTION,
                    "(" + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX + " or " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX
                            + ") and (" + Rms.TRANS_ID + "=? or " + Rms.IMDN_STRING + "=?)", new String[] { keyId, keyId }, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int rmsType = cursor.getInt(COLUMN_MESSAGE_TYPE);
                        int threadType = getThreadType(cursor);
                        if (rmsType == Rms.RMS_MESSAGE_TYPE_VOICE
                                || rmsType == Rms.RMS_MESSAGE_TYPE_IMAGE
                                || rmsType == Rms.RMS_MESSAGE_TYPE_VIDEO
                                || rmsType == Rms.RMS_MESSAGE_TYPE_VCARD
                                || rmsType == Rms.RMS_MESSAGE_TYPE_FILE) {
                            ret = retransferFileMsg(threadType, cursor);
                        } else if (rmsType == Rms.RMS_MESSAGE_TYPE_TEXT) {
                            ret = retransferTextMsg(threadType, cursor);
                        } else if (rmsType == Rms.RMS_MESSAGE_TYPE_GEO) {
                            ret = retransferGeoMsg(threadType, cursor);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return ret;
    }

    private static boolean checkIsOverTimes(String id, boolean send) {
        int count = 0;
        if (send) {
            if (sMapSendTryTimes.containsKey(id)) {
                count = sMapSendTryTimes.get(id);
                sMapSendTryTimes.put(id, ++count);
            } else {
                count = 1;
                sMapSendTryTimes.put(id, count);
            }
        } else {
            if (sMapRecvTryTimes.containsKey(id)) {
                count = sMapRecvTryTimes.get(id);
                sMapRecvTryTimes.put(id, ++count);
            } else {
                count = 1;
                sMapRecvTryTimes.put(id, count);
            }
        }
        return count > RETRY_COUNT;
    }

    public static int getThreadType(Cursor cursor) {
        String address = cursor.getString(cursor.getColumnIndex(Rms.ADDRESS));
        String paUuid = cursor.getString(cursor.getColumnIndex(Rms.PA_UUID));
        String groupChatId = cursor.getString(cursor.getColumnIndex(Rms.GROUP_CHAT_ID));
        if (!TextUtils.isEmpty(groupChatId)) {
            return RmsDefine.RMS_GROUP_THREAD;
        } else if (!TextUtils.isEmpty(address)) {
            return address.split(";").length > 1 ? RmsDefine.BROADCAST_THREAD : RmsDefine.COMMON_THREAD;
        }
        return RmsDefine.COMMON_THREAD;
    }

    private static boolean retransferFileMsg(int threadType, Cursor cursor) {
        int type = cursor.getInt(COLUMN_TYPE);
        String transId = cursor.getString(COLUMN_TRANS_ID);
        String thumbPath = cursor.getString(COLUMN_THUMB_PATH);
        String imdn = cursor.getString(COLUMN_IMDN_STRING);
        int transSize = cursor.getInt(COLUMN_TRANS_SIZE);
        int fileSize = cursor.getInt(COLUMN_FILE_SIZE);
        String filePath = cursor.getString(COLUMN_FILE_PATH);
        String fileType = cursor.getString(COLUMN_FILE_TYPE);
        String address = cursor.getString(COLUMN_ADDRESS);
        String groupChatId = cursor.getString(COLUMN_GROUP_CHAT_ID);
        int mixType = cursor.getInt(COLUMN_MIX_TYPE);
        boolean send = (type == Rms.MESSAGE_TYPE_OUTBOX);
        byte[] thumbData = new byte[0];
        if (!TextUtils.isEmpty(thumbPath) && new File(thumbPath).exists()) {
            thumbData = RcsFileUtils.fileToBytes(thumbPath);
        }
        int duration = cursor.getInt(COLUMN_DURATION);

        boolean ret = false;
        if (threadType == RmsDefine.COMMON_THREAD
                || threadType == RmsDefine.BROADCAST_THREAD) {
            if ((mixType & Rms.MIX_TYPE_CC) > 0) {
                send = false;
            }
            ret = RcsCallWrapper.rcsResumeFile(send, "", address, transId, imdn, filePath, fileType, duration, transSize, fileSize, thumbData);
        } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
            RcsGroupInfo info = RcsGroupInfo.loadGroupInfo(RcsMmsInitHelper.getContext(), groupChatId);
            if (info != null) {
                ret = RcsCallWrapper.rcsResumeGroupFile(send, info.mSubject, info.mSessionIdentify, info.mGroupChatId, transId, imdn,
                        filePath, fileType, duration, transSize, fileSize, thumbData);
            }
        }
        return ret;
    }

    public static boolean retransferTextMsg(int threadType, Cursor cursor) {
        boolean ret = false;
        String imdn = null;
        int id = cursor.getInt(COLUMN_ID);
        String address = cursor.getString(COLUMN_ADDRESS);
        String body = cursor.getString(COLUMN_BODY);
        String groupChatId = cursor.getString(COLUMN_GROUP_CHAT_ID);
        String oldImdn = cursor.getString(COLUMN_IMDN_STRING);
        String burnBody = cursor.getString(COLUMN_RMS_EXTRA);
        String conversationId = cursor.getString(COLUMN_CONVERSATION_ID);
        if (threadType == RmsDefine.COMMON_THREAD) {
            if (RcsCallWrapper.rcsIsSession1To1()) {
                imdn = RcsCallWrapper.rcsSendOTOSessMsg(String.valueOf(id), address, body);
            } else {
                imdn = RcsCallWrapper.rcsSendMessage1To1(String.valueOf(id), address, body, RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_DEFAULT, conversationId, null, null);
            }
        } else if (threadType == RmsDefine.BROADCAST_THREAD) {
            imdn = RcsCallWrapper.rcsSendMessage1ToM(String.valueOf(id), address, body, RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT);
        } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
            if (RcsCallWrapper.rcsGroupSessIsExist(groupChatId)) {
                imdn = RcsCallWrapper.rcsSendGroupMsg(String.valueOf(id), groupChatId, body, MtcImSessConstants.EN_MTC_IM_SESS_CONT_MSG_TXT_PLAIN, null);
            }
        }
        if (!TextUtils.isEmpty(imdn)) {
            ret = true;
            updateImdnKey(oldImdn, imdn, true);
        }
        return ret;
    }

    private static boolean retransferGeoMsg(int threadType, Cursor cursor) {
        boolean ret = false;
        String transId = null;
        int id = cursor.getInt(COLUMN_ID);
        String address = cursor.getString(COLUMN_ADDRESS);
        String body = cursor.getString(COLUMN_RMS_EXTRA);
        String groupChatId = cursor.getString(COLUMN_GROUP_CHAT_ID);
        String oldTransId = cursor.getString(COLUMN_TRANS_ID);
        int type = cursor.getInt(COLUMN_TYPE);
        boolean send = (type == Rms.MESSAGE_TYPE_OUTBOX);
        RcsLocationHelper.RcsLocationItem rcsLocationItem = RcsLocationHelper.parseLocationJson(RcsMmsUtils.geoFileToString(cursor.getString(COLUMN_FILE_PATH)));
        if (rcsLocationItem != null) {
            Double latitude = rcsLocationItem.mLatitude;
            Double longtitude = rcsLocationItem.mLongitude;
            float radius = rcsLocationItem.mRadius;
            String locationName = rcsLocationItem.mAddress;
            if (threadType == RmsDefine.COMMON_THREAD) {
                if (send) {
                    transId = RcsCallWrapper.rcsSendGeoMsg(String.valueOf(id), latitude, longtitude, radius, address, locationName);
                } else {
                    if (RcsCallWrapper.rcsFetchGeo(address, oldTransId)) {
                        transId = oldTransId;
                    }
                }
            } else if (threadType == RmsDefine.BROADCAST_THREAD) {
                if (send) {
                    transId = RcsCallWrapper.rcsSendOTMGeoMsg(String.valueOf(id), latitude, longtitude, radius, address, locationName);
                }
            } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
                RcsGroupInfo info = RcsGroupInfo.loadGroupInfo(RcsMmsInitHelper.getContext(), groupChatId);
                if (send) {
                    transId = RcsCallWrapper.rcsSendGroupGeoMsg(String.valueOf(id), info.mSubject, info.mGroupChatId, info.mSessionIdentify, latitude, longtitude, radius, locationName);
                } else {
                    if (RcsCallWrapper.rcsFetchGroupGeo(address, oldTransId, groupChatId)) {
                        transId = oldTransId;
                    }
                }
            }
            if (!TextUtils.isEmpty(transId)) {
                ret = true;
                updateTransIdKey(oldTransId, transId, true);
            }
        }
        return ret;
    }

    private static boolean needRetransferFromErrcode(int errcode) {
        return errcode == MtcImConstants.MTC_IM_ERR_TIMEOUT || errcode == MtcImConstants.MTC_IM_ERR_NO
                || errcode == MtcImConstants.MTC_IM_ERR_NETWORK || errcode == MtcGsGinfoConstants.MTC_GS_ERR_TIMEOUT
                || errcode == MtcGsGinfoConstants.MTC_GS_ERR_SEND_TIMEOUT
                || errcode == MtcGsGinfoConstants.MTC_GS_ERR_NO || errcode == MtcGsGinfoConstants.MTC_GS_ERR_INTERNAL_ERR;
    }

    private static void updateImdnKey(String oldImdn, String newImdn, boolean send) {
        if (send) {
            if (sMapSendTryTimes.containsKey(oldImdn)) {
                int count = sMapSendTryTimes.get(oldImdn);
                sMapSendTryTimes.remove(oldImdn);
                sMapSendTryTimes.put(newImdn, count);
            }
        } else {
            if (sMapRecvTryTimes.containsKey(oldImdn)) {
                int count = sMapRecvTryTimes.get(oldImdn);
                sMapRecvTryTimes.remove(oldImdn);
                sMapRecvTryTimes.put(newImdn, count);
            }
        }
    }

    private static void updateTransIdKey(String oldTransId, String newTransId, boolean send) {
        if (send) {
            if (sMapSendTryTimes.containsKey(oldTransId)) {
                int count = sMapSendTryTimes.get(oldTransId);
                sMapSendTryTimes.remove(oldTransId);
                sMapSendTryTimes.put(newTransId, count);
            }
        } else {
            if (sMapRecvTryTimes.containsKey(oldTransId)) {
                int count = sMapRecvTryTimes.get(oldTransId);
                sMapRecvTryTimes.remove(oldTransId);
                sMapRecvTryTimes.put(newTransId, count);
            }
        }
    }

    public static void retransferIfNetChange(long loginTime) {
        String typeSelection = Rms.TYPE + "=" + Rms.MESSAGE_TYPE_FAILED + " or (" + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX + " and " + Rms.STATUS + "=" + Rms.STATUS_FAIL + ")";
        String dateSelection = Rms.DATE + ">" + (loginTime - 30 * 1000);
        String errorCodeSelection = Rms.ERROR_CODE + " IN (" + MtcImConstants.MTC_IM_ERR_TIMEOUT + ", " + MtcImConstants.MTC_IM_ERR_NO + ", " + MtcImConstants.MTC_IM_ERR_NETWORK + ")";
        Cursor cursor = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG, PROJECTION,
                "(" +  typeSelection + ") and " + dateSelection + " and " + errorCodeSelection, null, "date ASC");
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int rmsType = cursor.getInt(COLUMN_MESSAGE_TYPE);
                    int threadType = getThreadType(cursor);
                    if (rmsType == Rms.RMS_MESSAGE_TYPE_VOICE
                            || rmsType == Rms.RMS_MESSAGE_TYPE_IMAGE
                            || rmsType == Rms.RMS_MESSAGE_TYPE_VIDEO
                            || rmsType == Rms.RMS_MESSAGE_TYPE_VCARD
                            || rmsType == Rms.RMS_MESSAGE_TYPE_FILE) {
                        retransferFileMsg(threadType, cursor);
                    } else if (rmsType == Rms.RMS_MESSAGE_TYPE_TEXT) {
                        retransferTextMsg(threadType, cursor);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

}
