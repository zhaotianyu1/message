package com.juphoon.rcs.app.tool;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.sms.MmsUtils;
import com.juphoon.cmcc.lemon.MtcImFileConstants;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.mms.RcsImReceiverServiceEx;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsContactHelp;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsGeoUtils;
import com.juphoon.rcs.tool.RcsNumberUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.GroupChatMembers;
import com.juphoon.service.RmsDefine.Rms;
import com.juphoon.service.RmsDefine.RmsGroup;
import com.juphoon.service.RmsDefine.RmsGroupNotification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class RcsGroupDealEx {

    private final static String TAG = "RcsGroupDeal";

    public static void dealGroupPartpUpdate(JSONObject jsonObj) {
        boolean fullUpdate = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_GROUP_PARTPUPDATE_FULL, true);
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        String newChairMan = RcsNumberUtils.formatPhone86(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAIRMAN));
        String oldChairMan = "";
        String newGroupName = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME);
        String oldGroupName = "";
        ContentValues values = new ContentValues();
        
        // 读取群主和群名称
        Cursor c = Factory.get().getApplicationContext().getContentResolver().query(RmsGroup.CONTENT_URI, new String[] { RmsGroup.CHAIRMAN, RmsGroup.NAME }, RmsGroup.GROUP_CHAT_ID+"=?",
                new String[]{ groupChatId }, null);
        if (c != null) {
            if (c.moveToFirst()) {
                oldChairMan = c.getString(0);
                oldGroupName = c.getString(1);
            }
            c.close();
        }
        
        HashMap<String, RcsGroupMembers> oldMembers = new HashMap<String, RcsGroupMembers>(); // 记录本地群成员(活跃状态)
        HashMap<String, RcsGroupMembers> newMembers = new HashMap<String, RcsGroupMembers>(); // 记录在群内的成员
        HashMap<String, RcsGroupMembers> delMembers = new HashMap<String, RcsGroupMembers>(); // 记录删除成员
        
        // 加载原有成员
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(GroupChatMembers.CONTENT_URI,
                new String[] { GroupChatMembers.NUMBER, GroupChatMembers.NAME, GroupChatMembers.STATUS, GroupChatMembers.ETYPE },
                GroupChatMembers.GROUP_CHAT_ID + "=?",
                new String[] { groupChatId }, null);

        if (cursor != null) {
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    RcsGroupMembers member = new RcsGroupMembers();
                    member.mPhone = cursor.getString(0);
                    member.mDisplayName = cursor.getString(1);
                    member.mStatus = cursor.getInt(2);
                    member.mEtype = cursor.getInt(3);
                    if (member.mStatus == RcsImServiceConstants.EN_MTC_PARTP_STAT_ACTIVE
                            || member.mStatus == RcsImServiceConstants.EN_MTC_PARTP_STAT_INACITVE) {
                        oldMembers.put(member.mPhone, member);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        JSONObject objStatus = jsonObj.optJSONObject(RcsJsonParamConstants.RCS_JSON_GROUP_MEMBER_STATUS);
        JSONObject objDisplayName = jsonObj.optJSONObject(RcsJsonParamConstants.RCS_JSON_GROUP_MEMBER_DISPLAY_NAME);
        JSONObject objEType = jsonObj.optJSONObject(RcsJsonParamConstants.RCS_JSON_GROUP_MEMBER_ETYPE);
        String me = RcsServiceManager.getUserName();
        if (!TextUtils.isEmpty(me)) {
            RcsGroupMembers member = new RcsGroupMembers();
            member.mPhone = me;
            member.mDisplayName = objDisplayName.optString(me, "");
            member.mStatus = RcsImServiceConstants.EN_MTC_PARTP_STAT_ACTIVE;
            member.mEtype = RcsImServiceConstants.EN_MTC_PARTP_ETYPE_GPMANAGE;
            RcsGroupDealEx.insertOrUpdateMember(groupChatId, member);
            updateMyNickNameInGroupsTable(groupChatId, member.mDisplayName);
        }
        Iterator<?> it = objStatus.keys();
        while (it.hasNext()) {
            RcsGroupMembers member = new RcsGroupMembers();
            String key = it.next().toString();
            member.mPhone = key;
            member.mDisplayName = objDisplayName.optString(key, "");
            member.mStatus = objStatus.optInt(key, -1);
            member.mEtype = objEType.optInt(key, 0);
            if (member.mStatus == RcsImServiceConstants.EN_MTC_PARTP_STAT_ACTIVE
                    || member.mStatus == RcsImServiceConstants.EN_MTC_PARTP_STAT_INACITVE) {
                if (!oldMembers.containsKey(key)) {
                    newMembers.put(key, member);
                } else {
                    if (!TextUtils.equals(member.mDisplayName, oldMembers.get(key).mDisplayName)) {
                        RcsGroupDealEx.insertOrUpdateMember(groupChatId, member);
                    }
                }
            } else {
                if (oldMembers.containsKey(key)) {
                    delMembers.put(key, member);
                }
            }
        }

        ArrayList<String> arrayJoinMembers = new ArrayList<String>(); // 用于生成加入信息
        for (String key : newMembers.keySet()) {
            if (key.equals(me)) continue;
            if (arrayJoinMembers.size() < 6) {
                arrayJoinMembers.add(RcsGroupDealEx.getMemberDisplayName(newMembers.get(key)));
            }
            RcsGroupDealEx.insertOrUpdateMember(groupChatId, newMembers.get(key));
        }

        if (arrayJoinMembers.size() > 0) {
            values.clear();
            values = new ContentValues();
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
            values.put(Rms.GROUP_CHAT_ID, groupChatId);
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
            values.put(Rms.BODY, TextUtils.join(" ", arrayJoinMembers.toArray())
                    + (arrayJoinMembers.size() > 1 ? "..." : " ")
                    + Factory.get().getApplicationContext().getString(R.string.group_chat_join));
            values.put(Rms.DATE, System.currentTimeMillis());
            values.put(Rms.TIMESTAMP, System.currentTimeMillis()/1000);
            values.put(Rms.STATUS, Rms.STATUS_SUCC);
            dealSystemMsg(values);
        }

        for (String key : delMembers.keySet()) {
            if (key.equals(me)) continue;
            int status = objStatus.optInt(key, -1);
            values.clear();
            values = new ContentValues();
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
            values.put(Rms.GROUP_CHAT_ID, groupChatId);
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
            if (status == RcsImServiceConstants.EN_MTC_PARTP_STAT_DEPARTED) {
                values.put(Rms.BODY, RcsGroupDealEx.getMemberDisplayName(delMembers.get(key))+" "+
                        Factory.get().getApplicationContext().getString(R.string.group_chat_left));
            } else if (status == RcsImServiceConstants.EN_MTC_PARTP_STAT_BOOTED) {
                values.put(Rms.BODY, RcsGroupDealEx.getMemberDisplayName(delMembers.get(key))+" "+
                        Factory.get().getApplicationContext().getString(R.string.group_chat_kickout));
            } else {
                values.put(Rms.BODY, RcsGroupDealEx.getMemberDisplayName(delMembers.get(key))+" "+
                        Factory.get().getApplicationContext().getString(R.string.group_chat_left));
            }
            values.put(Rms.DATE, System.currentTimeMillis());
            values.put(Rms.TIMESTAMP, System.currentTimeMillis()/1000);
            values.put(Rms.STATUS, Rms.STATUS_SUCC);
            RcsGroupDealEx.deleteMember(groupChatId, delMembers.get(key));
            dealSystemMsg(values);
        }

        // 提示群主改变
        if (!TextUtils.isEmpty(newChairMan) && !TextUtils.equals(newChairMan, oldChairMan)) {
            values.clear();
            values.put(RmsGroup.CHAIRMAN, newChairMan);
            Factory.get().getApplicationContext().getContentResolver().update(RmsGroup.CONTENT_URI, values, RmsGroup.GROUP_CHAT_ID + "=?", new String[] { groupChatId });

            values.clear();
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
            values.put(Rms.GROUP_CHAT_ID, groupChatId);
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
            values.put(Rms.DATE, System.currentTimeMillis());
            values.put(Rms.TIMESTAMP, System.currentTimeMillis() / 1000);
            StringBuilder builder = new StringBuilder();
            builder.append(RcsGroupDealEx.getMemberDisplayName(
                    oldMembers.containsKey(newChairMan) ? oldMembers.get(newChairMan) : newMembers.get(newChairMan)));
            builder.append(" ");
            builder.append(Factory.get().getApplicationContext().getString(R.string.new_group_manager));
            values.put(Rms.BODY, builder.toString());
            values.put(Rms.STATUS, Rms.STATUS_SUCC);
            dealSystemMsg(values);
        }

        // 更新群名称
        if (!TextUtils.equals(newGroupName, oldGroupName)) {
            dealGroupSubjectChanged(jsonObj);
        }
    }

    public static Uri dealGroupRecvMsg(JSONObject jsonObj) {
        String imdn = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG, null, Rms.IMDN_STRING + "=?",
                new String[] { imdn }, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    return null;
                }
            } finally {
                cursor.close();
            }
        }
        boolean isCC = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_CC, false);
        boolean isAt = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_AT, false);
        boolean isSilence = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_SILENCE, false);
        boolean isCloudFile = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CLOUD_FILE, false);
        boolean isCard = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CARD, false);
        boolean isRedbag = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_REDBAG, false);
        boolean ishttp = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_IS_HTTP,false);
        ContentValues values = new ContentValues();
        values.put(Rms.ADDRESS, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ADDRESS));
        values.put(Rms.DATE, System.currentTimeMillis());
        values.put(Rms.TIMESTAMP, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_TIMESTAMP));
        values.put(Rms.TYPE, isCC ? Rms.MESSAGE_TYPE_SENT : Rms.MESSAGE_TYPE_INBOX);
        values.put(Rms.GROUP_CHAT_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID));
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        values.put(Rms.SUB_ID, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_SUB_ID, -1));
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        values.put(Rms.MIX_TYPE, RcsUtilsEx.getMixType(isCC, isSilence, false, isAt));
        if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_EMOTICON, false)) {
            // 不支持的消息
            return null;
        } else if (isCloudFile) {
            // 不支持的消息
            return null;
        } else if (isCard) {
            // 不支持的消息
            return null;
        } else if (isRedbag) {
            // 不支持的消息
            return null;
        } else if (ishttp) {
            String ftXml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_MSG_XML);
            if (!TextUtils.isEmpty(ftXml) && RcsCallWrapper.rcsGetIsHttpOpen()) {
                values.put(Rms.STATUS, Rms.STATUS_INIT);
                values.put(Rms.RMS_EXTRA, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_MSG_XML));
                values.put(Rms.BODY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY));
                values.put(Rms.TRANS_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID));
                int messageType = -1;
                List<RcsChatbotHelper.RcsChatbotHttpFileInfo> fileInfolist = RcsChatbotHelper.parseRcsChatbotFileInfo(ftXml);
                for (RcsChatbotHelper.RcsChatbotHttpFileInfo fileInfo : fileInfolist) {
                    if (TextUtils.equals(fileInfo.type, "file")) {
                        messageType = RcsFileUtils.getRmsType(fileInfo.contentType, fileInfo.disposition);
                        values.put(Rms.BODY, RcsMmsUtils.getMessageText("", messageType));
                        values.put(Rms.FILE_DURATION, fileInfo.playLength);
                        values.put(Rms.FILE_SIZE, fileInfo.size);
                        values.put(Rms.FILE_TYPE, fileInfo.contentType);
                        values.put(Rms.FILE_NAME, fileInfo.name);
                        RcsFileDownloadHelper.FileInfo dlfileInfo = new RcsFileDownloadHelper.FileInfo(fileInfo.url, fileInfo.size).setConTenType(fileInfo.contentType).setName(fileInfo.name);
                        String path = RcsFileDownloadHelper.getPathFromFileInfo(dlfileInfo, RmsDefine.RMS_FILE_PATH);
                        if (!TextUtils.isEmpty(path)) {
                            values.put(Rms.FILE_PATH, path);
                            values.put(Rms.STATUS, Rms.STATUS_SUCC);
                        } else {
                            if (messageType == Rms.RMS_MESSAGE_TYPE_VCARD) {
                                RcsFileDownloadHelper.downloadFile(null, dlfileInfo, null, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID), RmsDefine.RMS_FILE_PATH);
                            } else if (messageType == Rms.RMS_MESSAGE_TYPE_GEO) {
                                RcsFileDownloadHelper.downloadFile(null, dlfileInfo, null, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID), RmsDefine.RMS_FILE_PATH);
                            }
                        }
                    } else if (TextUtils.equals(fileInfo.type, "thumbnail")) {
                        RcsFileDownloadHelper.FileInfo thumbInfo = new RcsFileDownloadHelper.FileInfo(fileInfo.url, fileInfo.size).setConTenType(fileInfo.contentType).setConTenType(fileInfo.name);
                        String path = RcsFileDownloadHelper.getPathFromFileInfo(thumbInfo, RmsDefine.RMS_THUMB_PATH);
                        if (!TextUtils.isEmpty(path)) {
                            values.put(Rms.THUMB_PATH, path);
                        } else {
                            if (fileInfo != null) {
                                // 有缩略图尝试去下载
                                RcsFileDownloadHelper.downloadFile("PreHttpThumb-" + jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID), thumbInfo, new RcsFileDownloadHelper.Callback() {
                                    @Override
                                    public void onDownloadResult(final String cookie, final boolean succ, final String path) {
                                        Log.d(TAG, "PreHttpThumb cookie=" + cookie + " succ=" + succ);
                                        if (cookie.startsWith("PreHttpThumb-")) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String transId = cookie.substring("PreHttpThumb-".length());
                                                    if (succ) {
                                                        ContentValues contentValues = new ContentValues(1);
                                                        contentValues.put(Rms.THUMB_PATH, path);
                                                        RcsMmsInitHelper.getContext().getContentResolver().update(Rms.CONTENT_URI_LOG, contentValues, Rms.TRANS_ID + " = ?", new String[]{transId});
                                                    }
                                                    // 通知上层，针对 Messaging
                                                    try {
                                                        Intent intent = new Intent(RcsJsonParamConstants.RCS_ACTION_IM_NOTIFY);
                                                        JSONObject jsonObj = new JSONObject();
                                                        jsonObj.put(RcsJsonParamConstants.RCS_JSON_ACTION, RcsJsonParamConstants.RCS_JSON_ACTION_HTTP_THUMB_DOWNLOAD_RESULT);
                                                        jsonObj.put(RcsJsonParamConstants.RCS_JSON_RESULT, succ);
                                                        jsonObj.put(RcsJsonParamConstants.RCS_JSON_TRANS_ID, transId);
                                                        jsonObj.put(RcsJsonParamConstants.RCS_JSON_THUMB_PATH, path);
                                                        intent.putExtra(RcsJsonParamConstants.RCS_JSON_KEY, jsonObj.toString());
                                                        intent.setClass(RcsMmsInitHelper.getContext(), RcsImReceiverServiceEx.class);
                                                        RcsImReceiverServiceEx.enqueueWork(RcsMmsInitHelper.getContext(), intent);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }).start();
                                        }
                                    }
                                }, null, RmsDefine.RMS_THUMB_PATH);
                            }
                        }
                    }
                }
                if (messageType == -1) {
                    return null;
                }
                values.put(Rms.MESSAGE_TYPE, messageType);
            } else {
                return null;
            }
        } else {
            values.put(Rms.BODY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY));
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_TEXT);
            String body = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY);
            if (!TextUtils.isEmpty(body)) {
                // 尝试解析地理位置字符串，返回null则表示不是地理位置
                JSONObject object = RcsGeoUtils.parseGeoStringToJson(body);
                if (object != null) {
                    values.put(Rms.FILE_PATH, RcsFileUtils.saveGeoToFile("", object.toString()));
                    values.put(Rms.FILE_TYPE, MtcImFileConstants.MTC_IM_FILE_COUT_GS_GINFO);
                    values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_GEO);
                }
            }
        }
        // 插入 sms 表和 rms 用 id 做关联
        Uri smsUri = MmsUtils.insertSmsMessage(Factory.get().getApplicationContext(),
                Telephony.Sms.CONTENT_URI,
                RcsServiceManager.getSubId(),
                jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID),
                RcsMmsUtils.getMessageText(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY), Rms.RMS_MESSAGE_TYPE_TEXT),
                values.getAsLong(Rms.DATE),
                Telephony.Sms.STATUS_COMPLETE,
                isCC ? Telephony.Sms.MESSAGE_TYPE_SENT : Telephony.Sms.MESSAGE_TYPE_INBOX,
                -1);
        if (smsUri != null) {
            values.put(Rms.SMS_ID, ContentUris.parseId(smsUri));
            return Factory.get().getApplicationContext().getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
        }
        return null;
    }

    public static void dealGroupNotificationRecvInvite(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        if (haveGroupInDb(groupChatId) && getGroupState(groupChatId) == RmsGroup.STATE_STARTED) {
            return;
        }
        if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_CREATE) == 0) {
            return;
        }
        String organzierPhone = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_ORGANZIER_PHONE);
        ContentValues values = new ContentValues();
        values.put(RmsGroupNotification.NAME, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME));
        values.put(RmsGroupNotification.DATE, System.currentTimeMillis());
        values.put(RmsGroupNotification.ORGANIZER_PHONE, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_ORGANZIER_PHONE));
        values.put(RmsGroupNotification.GROUP_CHAT_ID, groupChatId);
        values.put(RmsGroupNotification.SESSION_IDENTITY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_SESS_IDENTIFY));
        values.put(RmsGroupNotification.INFO, RmsGroupNotification.INFO_INVITED);
        int update = Factory.get().getApplicationContext().getContentResolver().update(RmsGroupNotification.CONTENT_URI, values,
                RmsGroupNotification.GROUP_CHAT_ID+"=? AND "+ RmsGroupNotification.INFO+"="+ RmsGroupNotification.INFO_INVITED+" AND "+
                RmsGroupNotification.ORGANIZER_PHONE+"=?",
                new String[] { groupChatId, organzierPhone });
        if (update == 0)
            Factory.get().getApplicationContext().getContentResolver().insert(RmsGroupNotification.CONTENT_URI, values);
    }

    public static void dealGroupNotificationAccepted(JSONObject jsonObj) {
        if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_OFFLINE) == 1) {
            return;
        }
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        ContentValues values = new ContentValues();
        values.put(RmsGroupNotification.INFO, RmsGroupNotification.INFO_ACCEPTED);
        Factory.get().getApplicationContext().getContentResolver().update(RmsGroupNotification.CONTENT_URI, values,
                RmsGroupNotification.GROUP_CHAT_ID+"=? AND "+ RmsGroupNotification.INFO+"="+ RmsGroupNotification.INFO_INVITED,
                new String[] { groupChatId });
    }

    public static void dealGroupNotificationRejected(JSONObject jsonObj) {
        dealGroupNotificationReleased(jsonObj);
    }

    public static void dealGroupNotificationCanceled(JSONObject jsonObj) {
        dealGroupNotificationReleased(jsonObj);
    }

    public static void dealGroupNotificationReleased(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        int errorCode = jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE);
        int infoType = -1;
        if (errorCode == RcsImServiceConstants.MTC_IM_ERR_GONE
                || errorCode == RcsImServiceConstants.MTC_IM_ERR_NOT_FOUND
                || errorCode == RcsImServiceConstants.MTC_IM_ERR_DISMISSED
                || errorCode == RcsImServiceConstants.MTC_IM_ERR_LEAVED) {
            infoType = RmsGroupNotification.INFO_DISSOLVE;
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_FORBIDDEN
                    || errorCode == RcsImServiceConstants.MTC_IM_ERR_EXPELLED
                    || errorCode == RcsImServiceConstants.MTC_IM_ERR_ORIGINATOR_NOTIN_GROUP) {
            infoType = RmsGroupNotification.INFO_KICKOUT;
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_EXCEED_MAX_PARTP) {
            infoType = RmsGroupNotification.INFO_MEMBER_FULL;
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_JOINED_GRP_FULL) {
            infoType = RmsGroupNotification.INFO_GROUPS_FULL;
        }
        if (infoType != -1) {
            ContentValues values = new ContentValues();
            if (haveGroupInDb(groupChatId) && getGroupState(groupChatId) == RmsGroup.STATE_STARTED) {
                values.put(RmsGroupNotification.INFO, infoType);
                values.put(RmsGroupNotification.NAME, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME));
                values.put(RmsGroupNotification.DATE, System.currentTimeMillis());
                values.put(RmsGroupNotification.ORGANIZER_PHONE, "");
                values.put(RmsGroupNotification.GROUP_CHAT_ID, groupChatId);
                values.put(RmsGroupNotification.SESSION_IDENTITY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_SESS_IDENTIFY));
                Factory.get().getApplicationContext().getContentResolver().insert(RmsGroupNotification.CONTENT_URI, values);
            }

            values.clear();
            values.put(RmsGroupNotification.INFO, infoType);
            values.put(RmsGroupNotification.DATE, System.currentTimeMillis());
            Factory.get().getApplicationContext().getContentResolver().update(RmsGroupNotification.CONTENT_URI, values,
                    RmsGroupNotification.GROUP_CHAT_ID+"=? AND "+ RmsGroupNotification.INFO+"="+ RmsGroupNotification.INFO_INVITED,
                    new String[] { groupChatId });
        }
    }

    public static void dealGroupRecvInvite(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        String displayName = "";
        String me = RcsServiceManager.getUserName();
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(GroupChatMembers.CONTENT_URI,
                new String[] { GroupChatMembers.NAME },
                GroupChatMembers.GROUP_CHAT_ID+"=? AND "+ GroupChatMembers.NUMBER+"=?",
                new String[] { groupChatId, me }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0);
                    if (TextUtils.isEmpty(displayName))
                        displayName = "";
                }
            } finally {
                cursor.close();
            }
        }
        if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_CREATE) == 0) {
            RcsCallWrapper.rcsGroupChatAccept(groupChatId, displayName);
        }
        if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_CREATE) == 1) {
            try {
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_GROUP_CHAIRMAN, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_ORGANZIER_PHONE));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (haveGroupInDb(groupChatId)) {
            if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_CREATE) == 1) {
                updateGroupState(groupChatId, RmsGroup.STATE_INVITED);
            } else {
                updateGroupState(groupChatId, RmsGroup.STATE_STARTED);
            }
        } else {
            if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_CREATE) == 1) {
                insertGroup(jsonObj, RmsGroup.STATE_INVITED);
            } else {
                insertGroup(jsonObj, RmsGroup.STATE_STARTED);
            }
        }
    }

    public static void dealGroupAccepted(JSONObject jsonObj) {
        if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_OFFLINE) == 1) {
            return;
        }
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        ContentValues values = new ContentValues();
        int status = getGroupState(groupChatId);
        if (status == -1 || status == RmsGroup.STATE_INVITED) {
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
            values.put(Rms.GROUP_CHAT_ID, groupChatId);
            values.put(Rms.DATE, System.currentTimeMillis());
            values.put(Rms.TIMESTAMP, System.currentTimeMillis() / 1000);
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
            values.put(Rms.STATUS, Rms.STATUS_SUCC);
            if (status == -1) {
                insertGroup(jsonObj, RmsGroup.STATE_STARTED);
                values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.group_chat_established));
            } else if (status == RmsGroup.STATE_INVITED) {
                updateGroupState(groupChatId, RmsGroup.STATE_STARTED);
                values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.YOU)
                        + Factory.get().getApplicationContext().getString(R.string.group_chat_join));
            }
            dealSystemMsg(values);
        } else {
            updateGroupState(groupChatId, RmsGroup.STATE_STARTED);
        }
    }

    public static void dealGroupRejected(JSONObject jsonObj) {
        dealGroupReleased(jsonObj);
    }

    public static void dealGroupCanceled(JSONObject jsonObj) {
        dealGroupReleased(jsonObj);
    }

    public static void dealGroupReleased(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        int errorCode = jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE);
        if (!haveGroupInDb(groupChatId))
            return;
        ContentValues values = new ContentValues();
        if (errorCode == RcsImServiceConstants.MTC_IM_ERR_GONE
                || errorCode == RcsImServiceConstants.MTC_IM_ERR_DISMISSED) {
            updateGroupState(groupChatId, RmsGroup.STATE_TERMINATED);
            values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.dissolve_group));
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_FORBIDDEN
                    || errorCode == RcsImServiceConstants.MTC_IM_ERR_EXPELLED
                    || errorCode == RcsImServiceConstants.MTC_IM_ERR_ORIGINATOR_NOTIN_GROUP) {
            updateGroupState(groupChatId, RmsGroup.STATE_ABORTED);
            values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.kicked_out_group));
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_EXCEED_MAX_PARTP) {
            updateGroupState(groupChatId, RmsGroup.STATE_FAILED);
            values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.member_full_group));
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_JOINED_GRP_FULL) {
            updateGroupState(groupChatId, RmsGroup.STATE_FAILED);
            values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.full_group));
        } else if (errorCode == RcsImServiceConstants.MTC_IM_ERR_LEAVED) {
            updateGroupState(groupChatId, RmsGroup.STATE_CLOSED_BY_USER);
            values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.dissolve_group));
        }
        if (values.size() > 0) {
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
            values.put(Rms.GROUP_CHAT_ID, groupChatId);
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
            values.put(Rms.DATE, System.currentTimeMillis());
            values.put(Rms.TIMESTAMP, System.currentTimeMillis()/1000);
            values.put(Rms.STATUS, Rms.STATUS_SUCC);
            dealSystemMsg(values);
        }
    }

    public static void dealGroupSendOk(JSONObject jsonObj) {
        String rmsId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);
        ContentValues values = new ContentValues();
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_SENT);
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.IMDN_TYPE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_IMDN_TYPE));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        try {
            Factory.get().getApplicationContext().getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Integer.valueOf(rmsId)),
                    values, null, null);
        } catch (NumberFormatException e) {

        }
    }

    public static void dealGroupSendFailed(JSONObject jsonObj) {
        String rmsId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);;
        ContentValues values = new ContentValues();
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_FAILED);
        values.put(Rms.STATUS, Rms.STATUS_FAIL);
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.IMDN_TYPE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_IMDN_TYPE));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        try {
            Factory.get().getApplicationContext().getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Integer.valueOf(rmsId)),
                    values, null, null);
        } catch (NumberFormatException e) {

        }
    }

    public static void dealGroupSubjectChanged(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        ContentValues values = new ContentValues();
        values.put(RmsGroup.NAME, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME));
        Factory.get().getApplicationContext().getContentResolver().update(RmsGroup.CONTENT_URI, values, RmsGroup.GROUP_CHAT_ID+"=?",
                new String[]{ groupChatId });

        values.clear();
        values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
        values.put(Rms.GROUP_CHAT_ID, groupChatId);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
        values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.group_name_change,
                jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME)));
        values.put(Rms.DATE, System.currentTimeMillis());
        values.put(Rms.TIMESTAMP, System.currentTimeMillis()/1000);
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        dealSystemMsg(values);
    }

    public static void dealGroupRecvList(JSONObject jsonObj) {
        if (!jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_RESULT, true))
            return;
        JSONArray array = jsonObj.optJSONArray(RcsJsonParamConstants.RCS_JSON_GROUPS);
        if (array == null)
            return;
        for (int i=0; i<array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null)
                continue;
            String groupChatId = obj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
            if (!haveGroupInDb(groupChatId)) {
                insertGroup(obj, RmsGroup.STATE_STARTED);
            } else {
                updateGroupName(groupChatId, obj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME));
                updateGroupState(groupChatId, RmsGroup.STATE_STARTED);
            }
        }
    }

    public static void dealGroupRecvOneInfo(JSONObject jsonObj) {
        boolean reuslt = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_RESULT, false);
        String sessIdentify = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_SESS_IDENTIFY);
        RcsConfManagers.query(jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_CONF_ID));
        if (TextUtils.isEmpty(sessIdentify) || !reuslt)
            return;
        String chairMan = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAIRMAN);
        String me = RcsServiceManager.getUserName();
        ContentValues values = new ContentValues();
        values.put(RmsGroup.ORGANIZER_PHONE, "");
        values.put(RmsGroup.BEGIN_TIME, System.currentTimeMillis());
        values.put(RmsGroup.END_TIME, -1);
        values.put(RmsGroup.DURATION, -1);
//        values.put(RmsGroup.NICK_NAME, "");
        values.put(RmsGroup.CHAIRMAN, chairMan);
        values.put(RmsGroup.DIRECTION, TextUtils.equals(chairMan, me)?1:0);// 0 incoming, 1 outgoing
        Factory.get().getApplicationContext().getContentResolver().update(RmsGroup.CONTENT_URI, values,
                RmsGroup.SESSION_IDENTITY+"=?", new String[] {sessIdentify});
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(RmsGroup.CONTENT_URI,
                new String[] { RmsGroup.GROUP_CHAT_ID }, RmsGroup.SESSION_IDENTITY+"=?", new String[] {sessIdentify}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String groupChatId = cursor.getString(0);
                    HashSet<String> oldMembers = new HashSet<String>();
                    Cursor cursorMembers = Factory.get().getApplicationContext().getContentResolver().query(GroupChatMembers.CONTENT_URI,
                            new String[] { GroupChatMembers.NUMBER, GroupChatMembers.NAME, GroupChatMembers.STATUS, GroupChatMembers.ETYPE },
                            GroupChatMembers.GROUP_CHAT_ID + "=?",
                            new String[] { groupChatId }, null);

                    if (cursorMembers != null) {
                        try {
                            for(cursorMembers.moveToFirst(); !cursorMembers.isAfterLast(); cursorMembers.moveToNext()) {
                                oldMembers.add(cursorMembers.getString(0));
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                    JSONObject objStatus = jsonObj.optJSONObject(RcsJsonParamConstants.RCS_JSON_GROUP_MEMBER_STATUS);
                    Iterator<?> it = objStatus.keys();
                    while (it.hasNext()) {
                        String phone = it.next().toString();
                        Integer status = objStatus.optInt(phone, -1);
                        if (status == RcsImServiceConstants.EN_MTC_PARTP_STAT_ACTIVE
                                || status == RcsImServiceConstants.EN_MTC_PARTP_STAT_INACITVE ) {
                            RcsGroupMembers member = new RcsGroupMembers();
                            member.mPhone = phone;
                            member.mDisplayName = TextUtils.equals(me, phone)?jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_DISPLAY_NAME):"";
                            member.mStatus = status;
                            member.mEtype = RcsImServiceConstants.EN_MTC_PARTP_ETYPE_GPMANAGE;
                            RcsGroupDealEx.insertOrUpdateMember(groupChatId, member);
                            if (TextUtils.equals(me, phone)) {
                                updateMyNickNameInGroupsTable(groupChatId, member.mDisplayName);
                            }
                            oldMembers.remove(phone);
                        }
                    }
                    for (String removePhone : oldMembers) {
                        RcsGroupMembers member = new RcsGroupMembers();
                        member.mPhone = removePhone;
                        RcsGroupDealEx.deleteMember(groupChatId, member);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    public static void dealGroupDissolveOk(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        if (!haveGroupInDb(groupChatId))
            return;
        updateGroupState(groupChatId, RmsGroup.STATE_TERMINATED);
        ContentValues values = new ContentValues();
        values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.dissolve_group));
        values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
        values.put(Rms.GROUP_CHAT_ID, groupChatId);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
        values.put(Rms.DATE, System.currentTimeMillis());
        values.put(Rms.TIMESTAMP, System.currentTimeMillis()/1000);
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        dealSystemMsg(values);
    }

    public static void dealGroupLeaveOk(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        if (!haveGroupInDb(groupChatId))
            return ;
        updateGroupState(groupChatId, RmsGroup.STATE_CLOSED_BY_USER);
        ContentValues values = new ContentValues();
        values.put(Rms.BODY, Factory.get().getApplicationContext().getString(R.string.leave_group));
        values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_SYSTEM);
        values.put(Rms.GROUP_CHAT_ID, groupChatId);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_INBOX);
        values.put(Rms.DATE, System.currentTimeMillis());
        values.put(Rms.TIMESTAMP, System.currentTimeMillis()/1000);
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        dealSystemMsg(values);
    }

    /***********************/

    public static void insertGroup(JSONObject jsonObj, int state) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        JSONArray array = jsonObj.optJSONArray(RcsJsonParamConstants.RCS_JSON_GROUP_MEMBERS);
        if (array != null) {
            for (int i=0; i<array.length(); i++) {
                try {
                    String phone = array.getString(i);
                    if (!TextUtils.isEmpty(phone)) {
                        RcsGroupMembers member = new RcsGroupMembers();
                        member.mPhone = phone;
                        member.mDisplayName = "";
                        member.mStatus = RcsImServiceConstants.EN_MTC_PARTP_STAT_ACTIVE;
                        member.mEtype = RcsImServiceConstants.EN_MTC_PARTP_ETYPE_GPMANAGE;
                        insertOrUpdateMember(groupChatId, member);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        ContentValues values = new ContentValues();
        values.put(RmsGroup.NAME, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME));
        values.put(RmsGroup.BEGIN_TIME, System.currentTimeMillis());
        values.put(RmsGroup.END_TIME, -1);
        values.put(RmsGroup.DURATION, -1);
        values.put(RmsGroup.NICK_NAME, "");
        values.put(RmsGroup.GROUP_CHAT_ID, groupChatId);
        values.put(RmsGroup.CHAIRMAN, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAIRMAN));
        values.put(RmsGroup.SESSION_IDENTITY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_SESS_IDENTIFY));
        values.put(RmsGroup.STATE, state);
        values.put(RmsGroup.DIRECTION, state == RmsGroup.STATE_INVITED ? 0 : 1);
        values.put(RmsGroup.RECV_TYPE, RmsGroup.RECV_TYPE_NORMAL);
        Factory.get().getApplicationContext().getContentResolver().insert(RmsGroup.CONTENT_URI, values);
    }

    public static int updateGroupName(String groupChatId, String name) {
        ContentValues values = new ContentValues();
        values.put(RmsGroup.NAME, name);
        return Factory.get().getApplicationContext().getContentResolver().update(RmsGroup.CONTENT_URI, values, RmsGroup.GROUP_CHAT_ID+"=?",
                new String[] { groupChatId });
    }

    public static int updateGroupState(String groupChatId, int state) {
        ContentValues values = new ContentValues();
        values.put(RmsGroup.STATE, state);
        return Factory.get().getApplicationContext().getContentResolver().update(RmsGroup.CONTENT_URI, values, RmsGroup.GROUP_CHAT_ID+"=?",
                new String[] { groupChatId });
    }

    public static void insertOrUpdateMember(String groupChatId, RcsGroupMembers member) {
        ContentValues value = new ContentValues();
        value.put(GroupChatMembers.GROUP_CHAT_ID, groupChatId);
        value.put(GroupChatMembers.NUMBER, member.mPhone);
        value.put(GroupChatMembers.NAME, member.mDisplayName);
        value.put(GroupChatMembers.STATUS, member.mStatus);
        value.put(GroupChatMembers.ETYPE, member.mEtype);

        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(GroupChatMembers.CONTENT_URI, null,
                GroupChatMembers.GROUP_CHAT_ID+"=? And "+ GroupChatMembers.NUMBER+"=?",
                new String[] { groupChatId, member.mPhone }, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    Factory.get().getApplicationContext().getContentResolver().update(GroupChatMembers.CONTENT_URI, value,
                    GroupChatMembers.GROUP_CHAT_ID+"=? And "+ GroupChatMembers.NUMBER+"=?",
                    new String[] { groupChatId, member.mPhone });
                    return;
                } else {
                    Factory.get().getApplicationContext().getContentResolver().insert(GroupChatMembers.CONTENT_URI, value);
                }
            } finally {
                cursor.close();
            }
        }
    }

    public static void deleteMember(String groupChatId, RcsGroupMembers member) {
        Factory.get().getApplicationContext().getContentResolver().delete(GroupChatMembers.CONTENT_URI,
                GroupChatMembers.GROUP_CHAT_ID+"=? and "+ GroupChatMembers.NUMBER+"=?",
                new String[] { groupChatId, member.mPhone });
    }

    public static boolean haveGroupInDb(String groupChatId) {
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(RmsGroup.CONTENT_URI, new String[] { RmsGroup._ID },
                Rms.GROUP_CHAT_ID+"=?", new String[] { groupChatId }, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    return true;
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    public static int getGroupState(String groupChatId) {
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(RmsGroup.CONTENT_URI, new String[] { RmsGroup.STATE },
                Rms.GROUP_CHAT_ID+"=?", new String[] { groupChatId }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return -1;
    }

    public static String getMemberDisplayName(RcsGroupMembers member) {
        String me = RcsServiceManager.getUserName();
        if (TextUtils.equals(me, member.mPhone))
            return Factory.get().getApplicationContext().getString(R.string.I);
        else {
            if (!TextUtils.isEmpty(member.mDisplayName)) {
                return member.mDisplayName;
            } else {
                String name = RcsContactHelp.getNickNameByPhone(Factory.get().getApplicationContext(), member.mPhone);
                if (TextUtils.equals(name, member.mPhone)) {
                    return RcsUtilsEx.formatPhoneWithStar(name);
                }
                return name;
            }
        }
    }

    public static boolean isGroupRejectRecvMsg(String groupChatId) {
        boolean reject = false;
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(RmsGroup.CONTENT_URI, new String[]{ RmsGroup.RECV_TYPE },
                RmsGroup.GROUP_CHAT_ID+"=?", new String[]{groupChatId}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int type = cursor.getInt(0);
                    if (type == RmsGroup.RECV_TYPE_REJECT)
                        reject = true;
                }
            } finally {
                cursor.close();
            }
        }
        return reject;
    }

    private static void updateMyNickNameInGroupsTable(String groupChatId, String name) {
        ContentValues values = new ContentValues();
        values.put(RmsGroup.MY_NICK_NAME, name);
        Factory.get().getApplicationContext().getContentResolver().update(RmsGroup.CONTENT_URI, values, RmsGroup.GROUP_CHAT_ID+"=?", new String[] { groupChatId });
    }

    private static void dealSystemMsg(ContentValues values) {
        Uri smsUri = MmsUtils.insertSmsMessage(Factory.get().getApplicationContext(),
                Telephony.Sms.CONTENT_URI,
                RcsServiceManager.getSubId(),
                values.getAsString(Rms.GROUP_CHAT_ID),
                values.getAsString(Rms.BODY),
                values.getAsLong(Rms.DATE),
                Telephony.Sms.STATUS_COMPLETE,
                Telephony.Sms.MESSAGE_TYPE_INBOX,
                -1);
        if (smsUri != null) {
            values.put(Rms.SMS_ID, ContentUris.parseId(smsUri));
            Uri uri = Factory.get().getApplicationContext().getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
            if (uri == null) {
                return;
            }
            Intent intent = new Intent(RcsJsonParamConstants.RCS_ACTION_IM_NOTIFY);
            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_ACTION, RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_RECV_SYS_MSG);
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_BODY, values.get(Rms.BODY));
                jsonObj.put(RcsJsonParamConstants.RCS_JSON_COOKIE, ContentUris.parseId(uri));
                intent.putExtra(RcsJsonParamConstants.RCS_JSON_KEY, jsonObj.toString());
                intent.setClass(Factory.get().getApplicationContext(), RcsImReceiverServiceEx.class);
                RcsImReceiverServiceEx.enqueueWork(RcsMmsInitHelper.getContext(), intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
