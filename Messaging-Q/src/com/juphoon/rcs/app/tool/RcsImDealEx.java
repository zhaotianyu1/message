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
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.juphoon.chatbot.RcsChatbotCardBean;
import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.cmcc.lemon.MtcImFileConstants;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.helper.mms.RcsImReceiverServiceEx;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsGeoUtils;
import com.juphoon.rcs.tool.RcsNumberUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsCapServiceConstants;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;
import com.juphoon.service.RmsDefine.RmsGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class RcsImDealEx {
    private final static String TAG = RcsImDealEx.class.getSimpleName();

    public static Uri dealImRecvMsg(JSONObject jsonObj) {
        //文本消息根据imdn去重
        String imdnId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
                new String[]{Rms._ID}, Rms.IMDN_STRING + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX,
                new String[]{imdnId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    return null;
                }
            } finally {
                cursor.close();
            }
        }

        boolean isBurn = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_BURN_AFTER_READ, false);
        boolean isPublic = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_PUBLIC, false);
        boolean isEmoticon = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_EMOTICON, false);
        boolean isCC = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_CC, false);
        boolean isDirect = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_DIRECT, false);
        boolean isSilence = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_SILENCE, false);
        boolean isCloudFile = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CLOUD_FILE, false);
        boolean isBlocked = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_IS_BLOCKED, false);
        boolean isCard = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CARD, false);
        boolean isRedbag = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_REDBAG, false);
        boolean isChatbot = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_CHATBOT, false);
        boolean isA2P = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_A2P, false);
        boolean isHttpFile = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_IS_HTTP,false);
        // Chatbot Thumb Http FileInfo
        RcsChatbotHelper.RcsChatbotHttpFileInfo thumbFileInfo = null;
        String address = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ADDRESS);
        // chatbot 使用 serviceId 作为 Address
        if (isChatbot) {
            address = RcsChatbotHelper.formatServiceIdWithNoSip(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID));
        } else if (RcsChatbotHelper.isChatbotBySms(address)) {
            // TODO 如果是一对一消息，号码属于chatbot则把地址改掉
            address = RcsChatbotHelper.formatServiceIdWithNoSip(RcsChatbotHelper.getChatbotInfoBySmsOrServiceId(address).serviceId);
        }
        ContentValues values = new ContentValues();
        values.put(Rms.ADDRESS, isDirect ? RmsDefine.PC_ADDRESS : address);
        values.put(Rms.DATE, System.currentTimeMillis());
        values.put(Rms.TIMESTAMP, jsonObj.optLong(RcsJsonParamConstants.RCS_JSON_TIMESTAMP));
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.IMDN_TYPE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_IMDN_TYPE));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        values.put(Rms.FILE_PATH, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH));
        values.put(Rms.TYPE, isCC ? Rms.MESSAGE_TYPE_SENT : Rms.MESSAGE_TYPE_INBOX);
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        values.put(Rms.MIX_TYPE, RcsUtilsEx.getMixType(isCC, isSilence, isDirect, false));
        values.put(Rms.SUB_ID, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_SUB_ID, -1));
        if (isChatbot) {
            String chatbotId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID);
            // 如果需要获取 Chatbot 信息, 则将消息存起来等详情获得后再处理
            if (RcsChatbotInfoGetTools.checkNeedGetChatbotInfo(chatbotId)) {
                if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_HAS_GET_DETAIL, false)) {
                    return null;
                }
                RcsChatbotInfoGetTools.addPendingMessage(chatbotId, jsonObj);
                return null;
            }
            if (RcsChatbotHelper.isNeedReject(chatbotId)) {
                // 拦截消息
                return null;
            }

            String ftXml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_XML);
            String body = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY);
            String card = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_CARD);
            String a2pXml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_A2P);
            if (!TextUtils.isEmpty(ftXml)) {
                String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
                thumbFileInfo = dealftxml(true, ftXml, values, transId);
                if ((Integer) values.get(Rms.MESSAGE_TYPE) == -1) {
                    return null;
                }
            } else if (!TextUtils.isEmpty(card)) {
                values.put(Rms.BODY, genChatbotTip(true,false, ""));
                try {
                    RcsChatbotCardBean messageBean = new Gson().fromJson(card, RcsChatbotCardBean.class);
                    if (messageBean.message.generalPurposeCard != null) {
                        if (messageBean.message.generalPurposeCard.content != null) {
                            if (!TextUtils.isEmpty(messageBean.message.generalPurposeCard.content.title)) {
                                values.put(Rms.BODY, messageBean.message.generalPurposeCard.content.title);
                            } else if (!TextUtils.isEmpty(messageBean.message.generalPurposeCard.content.description)) {
                                values.put(Rms.BODY, messageBean.message.generalPurposeCard.content.description);
                            } else {
                                values.put(Rms.BODY, genChatbotTip(true, false, messageBean.message.generalPurposeCard.content.media.mediaContentType));
                            }
                            if (messageBean.message.generalPurposeCard.content.suggestions != null && messageBean.message.generalPurposeCard.content.suggestions.size() > 4) {
                                return null;
                            }
                        }
                    } else if (messageBean.message.generalPurposeCardCarousel != null) {
                        values.put(Rms.BODY, genChatbotTip(true, false, ""));
                        if (messageBean.message.generalPurposeCardCarousel.content != null) {
                            if (messageBean.message.generalPurposeCardCarousel.content.size() > 12) {
                                return null;
                            }
                            for (RcsChatbotCardBean.MessageBean.ContentBean content : messageBean.message.generalPurposeCardCarousel.content) {
                                if (content.suggestions != null && content.suggestions.size() > 4) {
                                    return null;
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    return null;
                }
                values.put(Rms.RMS_EXTRA, card);
                values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_CHATBOT_CARD);
            } else if (!TextUtils.isEmpty(body)) {
                // 尝试解析地理位置字符串，返回null则表示不是地理位置
                JSONObject object = RcsGeoUtils.parseGeoStringToJson(body);
                if (object != null) {
                    values.put(Rms.FILE_PATH, RcsFileUtils.saveGeoToFile("", object.toString()));
                    values.put(Rms.RMS_EXTRA, object.toString());
                    values.put(Rms.FILE_TYPE, MtcImFileConstants.MTC_IM_FILE_COUT_GS_GINFO);
                    values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_CHATBOT_GEO);
                } else {
                    values.put(Rms.BODY, body);
                    values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_CHATBOT_TEXT);
                }
            } else if (!TextUtils.isEmpty(a2pXml) && a2pXml.indexOf("<MediaType>SmartMsg</MediaType>") != -1) {
                values.put(Rms.BODY, genChatbotTip(false, false, "application/commontemplate+xml"));
                values.put(Rms.RMS_EXTRA, a2pXml);
                values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_CHATBOT_A2P);
            } else {
                return null;
            }

            String suggestion = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_SUGGESTION);
            if (!TextUtils.isEmpty(suggestion)) {
                values.put(Rms.RMS_EXTRA2, suggestion);
            }

            values.put(Rms.CHATBOT_SERVICE_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID));
            values.put(Rms.TRAFFIC_TYPE, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRAFFIC_TYPE));
        } else if (isPublic) {
            // 公众账号消息是在应用号插件中，这里的是指智能消息，相当于短信
//            if (isCC) {
//                if (isCard) {
//                    // 不支持的消息
//                    return null;
//                } else {
//                    values.put(Rms.BODY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY));
//                    values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_TEXT);
//                }
//            } else {
//                String xml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_PA_XML);
//                RcsPublicXmlMessage message = RcsPublicXmlMessage.parse(xml);
//                if (message == null || TextUtils.isEmpty(message.smscontent)) {
//                    return null;
//                }
//                switch (message.media_type) {
//                    case RcsPublicXmlMessage.TYPE_TEXT:
//                        break;
//                    case RcsPublicXmlMessage.TYPE_TEMPLATE:
//                        break;
//                    case RcsPublicXmlMessage.TYPE_VCARD:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_GEO:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_IMAGE:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_VIDEO:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_AUDIO:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_SART:
//                        break;
//                    case RcsPublicXmlMessage.TYPE_MART:
//                        break;
//                    case RcsPublicXmlMessage.TYPE_CARD:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_SART_VIDEO:
//                        break;
//                    case RcsPublicXmlMessage.TYPE_SMS:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_LIST_UPDATE:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_INFO_UPDATE:
//                        return null;
//                    case RcsPublicXmlMessage.TYPE_REDBAG:
//                        break;
//                    default:
//                        return null;
//                }
//                String messageBody = Factory.get().getApplicationContext().getString(R.string.rcs_public_message);
//                if (message.media_type == RcsPublicXmlMessage.TYPE_TEXT) {
//                    values.put(Rms.BODY, message.text);
//                } else if (message.media_type == RcsPublicXmlMessage.TYPE_SART || message.media_type == RcsPublicXmlMessage.TYPE_MART || message.media_type == RcsPublicXmlMessage.TYPE_SART_VIDEO){
//                    if (message.article.size() > 0) {
//                        RcsPublicXmlMessage.MediaArticle article = message.article.get(0);
//                        if (!TextUtils.isEmpty(article.title)) {
//                            values.put(Rms.BODY, article.title);
//                        } else {
//                            values.put(Rms.BODY, messageBody);
//                        }
//                    } else {
//                        values.put(Rms.BODY, messageBody);
//                    }
//                } else if (message.media_type == RcsPublicXmlMessage.TYPE_TEMPLATE) {
//                    values.put(Rms.BODY, message.template.title);
//                } else if (message.media_type == RcsPublicXmlMessage.TYPE_REDBAG) {
//                    values.put(Rms.BODY, message.title);
//                } else {
//                    values.put(Rms.BODY, messageBody);
//                }
//                // 将 subId 和 account 存入xml
//                String replaceString = String.format(Locale.getDefault(), "<subId>%s</subId><account>%s</account><simId>%d</simId></msg_content>",
//                        jsonObj.optString(RcsJsonParamConstants.RCS_JSON_SUB_ID), jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ACCOUNT),
//                        0);
//                xml = xml.replace("</msg_content>", replaceString);
//                values.put(Rms.RMS_EXTRA, xml);
//                values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_PA_XML);
//
//                RcsPublicAddressHelper.saveAddress(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ADDRESS));
//            }
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
        } else if (isA2P) {
            // 不支持的消息
            return null;
        } else if (isHttpFile) {
            String ftXml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_XML);
            String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
            if (!TextUtils.isEmpty(ftXml) && RcsCallWrapper.rcsGetIsHttpOpen()) {
                thumbFileInfo = dealftxml(false, ftXml, values, transId);
                if ((Integer) values.get(Rms.MESSAGE_TYPE) == -1) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            if (isEmoticon || isBurn) {
                // 不支持的消息
                return null;
            } else {//处理地理位置
                values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_TEXT);
                values.put(Rms.BODY, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY));
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
        }

        // 插入 sms 表和 rms 用 id 做关联
        Uri smsUri = MmsUtils.insertSmsMessage(Factory.get().getApplicationContext(),
                Telephony.Sms.CONTENT_URI,
                RcsServiceManager.getSubId(),
                isDirect ? RmsDefine.PC_ADDRESS : address,
                RcsMmsUtils.getMessageText(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY), Rms.RMS_MESSAGE_TYPE_TEXT),
                values.getAsLong(Rms.DATE),
                Telephony.Sms.STATUS_COMPLETE,
                isCC ? Telephony.Sms.MESSAGE_TYPE_SENT : Telephony.Sms.MESSAGE_TYPE_INBOX,
                -1);
        if (smsUri != null) {
            values.put(Rms.SMS_ID, ContentUris.parseId(smsUri));
            Uri retUri = Factory.get().getApplicationContext().getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
            if (thumbFileInfo != null) {
                // 有缩略图尝试去下载
                RcsFileDownloadHelper.downloadFile("PreHttpThumb-" + jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID),
                        new RcsFileDownloadHelper.FileInfo(thumbFileInfo.url, thumbFileInfo.size).setConTenType(thumbFileInfo.contentType).setName(thumbFileInfo.name),
                        new RcsFileDownloadHelper.Callback() {
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
            return retUri;
        }
        return null;
    }
    
    public static RcsChatbotHelper.RcsChatbotHttpFileInfo dealftxml(boolean isChatbotFile, String ftXml, ContentValues values, String transId) {
        RcsChatbotHelper.RcsChatbotHttpFileInfo thumbFileInfo = null;
        values.put(Rms.STATUS, Rms.STATUS_INIT);
        values.put(Rms.RMS_EXTRA, ftXml);
        values.put(Rms.TRANS_ID, transId);
        int messageType = -1;
        List<RcsChatbotHelper.RcsChatbotHttpFileInfo> fileInfolist = RcsChatbotHelper.parseRcsChatbotFileInfo(ftXml);
        for (RcsChatbotHelper.RcsChatbotHttpFileInfo fileInfo : fileInfolist) {
            if (TextUtils.equals(fileInfo.type, "file")) {
                messageType = isChatbotFile ? getChatbotRmsMessageType(fileInfo.contentType) : RcsFileUtils.getRmsType(fileInfo.contentType, fileInfo.disposition);
                values.put(Rms.BODY, RcsMmsUtils.getMessageText("", messageType));
                values.put(Rms.FILE_DURATION, fileInfo.playLength);
                values.put(Rms.FILE_SIZE, fileInfo.size);
                values.put(Rms.FILE_TYPE, RcsMmsUtils.transContentType(messageType, fileInfo.contentType));
                values.put(Rms.FILE_NAME, fileInfo.name);
                RcsFileDownloadHelper.FileInfo dlFileInfo = new RcsFileDownloadHelper.FileInfo(fileInfo.url, fileInfo.size).setConTenType(fileInfo.contentType).setName(fileInfo.name);
                String path = RcsFileDownloadHelper.getPathFromFileInfo(dlFileInfo, isChatbotFile ? RmsDefine.RMS_CHATBO_PATH : RmsDefine.RMS_FILE_PATH);
                if (!TextUtils.isEmpty(path)) {
                    values.put(Rms.FILE_PATH, path);
                    values.put(Rms.STATUS, Rms.STATUS_SUCC);
                } else {
                    if (messageType == Rms.RMS_MESSAGE_TYPE_VCARD || messageType == Rms.RMS_MESSAGE_TYPE_CHATBOT_VCARD || messageType == Rms.RMS_MESSAGE_TYPE_VOICE) {
                        RcsFileDownloadHelper.downloadFile(null, dlFileInfo, null, transId, isChatbotFile ? RmsDefine.RMS_CHATBO_PATH : RmsDefine.RMS_FILE_PATH);
                    }//msrp地理位置消息会转为httpXml
                    else if (messageType == Rms.RMS_MESSAGE_TYPE_GEO) {
                        RcsFileDownloadHelper.downloadFile(null, dlFileInfo, null, transId, isChatbotFile ? RmsDefine.RMS_CHATBO_PATH : RmsDefine.RMS_FILE_PATH);
                    }
                }
            } else if (TextUtils.equals(fileInfo.type, "thumbnail")) {
                String path = RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(fileInfo.url).setConTenType(fileInfo.name).setConTenType(fileInfo.contentType), RmsDefine.RMS_THUMB_PATH);
                if (!TextUtils.isEmpty(path)) {
                    values.put(Rms.THUMB_PATH, path);
                } else {
                    thumbFileInfo = fileInfo;
                }
            }
        }
        values.put(Rms.MESSAGE_TYPE, messageType);
        return thumbFileInfo;
    }

    public static void dealImSendResult(JSONObject jsonObj, boolean succ) {
        if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_REPORT, false)) {
            return;
        }
        String rmsId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);
        ContentValues values = new ContentValues();
        values.put(Rms.TYPE, succ ? Rms.MESSAGE_TYPE_SENT : Rms.MESSAGE_TYPE_FAILED);
        values.put(Rms.STATUS, succ ? Rms.STATUS_SUCC : Rms.STATUS_FAIL);
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.IMDN_TYPE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_IMDN_TYPE));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        values.put(Rms.ERROR_CODE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE));
        try {
            Factory.get().getApplicationContext().getContentResolver().update(ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Integer.valueOf(rmsId)), values,
                    Rms.IMDN_STRING + "=?", new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID)});
        } catch(NumberFormatException e) {
        }
    }

    public static Uri dealImRecvFileInvite(JSONObject jsonObj) {
        ContentValues values = new ContentValues();
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        if (!TextUtils.isEmpty(groupChatId)) {
            values.put(Rms.GROUP_CHAT_ID, groupChatId);
            if (!RcsGroupDealEx.haveGroupInDb(groupChatId)) {
                RcsGroupDealEx.insertGroup(jsonObj, RmsGroup.STATE_STARTED);
            }
        }

        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
                new String[]{Rms._ID}, Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX,
                new String[]{transId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    RcsCallWrapper.rcsRejectFile(transId, MtcImFileConstants.EN_MTC_IM_FILE_REJECT_REASON_FILE_EXISTED);
                    return null;
                }
            } finally {
                cursor.close();
            }
        }
        String address = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ADDRESS);
        String fileName = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_NAME);
        String fileType = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_TYPE);
        // 不支持的文件类型
        int messageType = RcsFileUtils.getRmsType(fileType,"");
        if (messageType < 0) {
            //RcsCallWrapper.rcsRejectFile(transId, MtcImFileConstants.EN_MTC_IM_FILE_REJECT_REASON_FILE_EXISTED);
            RcsCallWrapper.rcsRejectFile(transId, MtcImFileConstants.EN_MTC_IM_FILE_REJECT_REASON_FILE_EXISTED);
            return null;
        }
        boolean isCC = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_CC, false);
        boolean isDirect = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_DIRECT, false);
        boolean isSilence = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_SILENCE, false);
        boolean isBurn = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_BURN_AFTER_READ, false);
        boolean isPublic = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_PUBLIC, false);
        boolean isBlocked = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_IS_BLOCKED, false);

        values.put(Rms.ADDRESS, isDirect ? RmsDefine.PC_ADDRESS : address);
        values.put(Rms.DATE, System.currentTimeMillis());
        values.put(Rms.TIMESTAMP, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TIMESTAMP));
        values.put(Rms.FILE_NAME, fileName);
        values.put(Rms.FILE_TYPE, fileType);
        values.put(Rms.FILE_PATH, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH));
        values.put(Rms.TRANS_SIZE, 0);
        values.put(Rms.FILE_SIZE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_FILE_SIZE));
        values.put(Rms.TRANS_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID));
        values.put(Rms.THUMB_PATH, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_THUMB_PATH));
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        values.put(Rms.FILE_DURATION, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_FILE_DURATION));
        values.put(Rms.MIX_TYPE, RcsUtilsEx.getMixType(isCC, isSilence, isDirect, false));
        values.put(Rms.SUB_ID, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_SUB_ID, -1));

        values.put(Rms.MESSAGE_TYPE, messageType);
        if (!isBlocked && !isBurn && ((messageType == Rms.RMS_MESSAGE_TYPE_VCARD || messageType == Rms.RMS_MESSAGE_TYPE_VOICE)
                || RcsCallWrapper.rcsGetFileAutoAccept())) {
            if (!jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_OFFLINE, false)) {
                RcsCallWrapper.rcsAcceptFile(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID),
                        jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH));
            } else {
                if (!TextUtils.isEmpty(groupChatId)) {
                    RcsCallWrapper.rcsFetchGroupFile(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_NAME),
                            jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_SESS_IDENTIFY), groupChatId, transId, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH), fileType);
                } else {
                    RcsCallWrapper.rcsFetchFile(address, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID), jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH), fileType);
                }
            }
            values.put(Rms.STATUS, Rms.STATUS_PENDING);
            values.put(Rms.TYPE, isCC ? Rms.MESSAGE_TYPE_OUTBOX : Rms.MESSAGE_TYPE_INBOX);
        } else {
            values.put(Rms.STATUS, Rms.STATUS_INIT);
            values.put(Rms.TYPE, isCC ? Rms.MESSAGE_TYPE_QUEUED : Rms.MESSAGE_TYPE_INBOX);
        }
        // 插入 sms 表和 rms 用 id 做关联
        Uri smsUri = MmsUtils.insertSmsMessage(Factory.get().getApplicationContext(),
                Telephony.Sms.CONTENT_URI,
                RcsServiceManager.getSubId(),
                isDirect ? RmsDefine.PC_ADDRESS : address,
                RcsMmsUtils.getMessageText(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY), Rms.RMS_MESSAGE_TYPE_TEXT),
                values.getAsLong(Rms.DATE),
                Telephony.Sms.STATUS_COMPLETE,
                isCC ? Telephony.Sms.MESSAGE_TYPE_SENT : Telephony.Sms.MESSAGE_TYPE_INBOX,
                -1);
        if (smsUri != null) {
            values.put(Rms.SMS_ID, smsUri.getLastPathSegment());
            return Factory.get().getApplicationContext().getContentResolver().insert(Rms.CONTENT_URI_LOG, values);
        }
        return null;
    }

    public static void dealImRecvFileProgress(JSONObject jsonObj) {
        boolean isCC = true;
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[]{Rms._ID},
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX, new String[]{transId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    isCC = false;
                }
            } finally {
                cursor.close();
            }
        }
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_PENDING);
        values.put(Rms.TRANS_SIZE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_FILE_TRANS_SIZE));
        values.put(Rms.ERROR_CODE, 0);
        if (!isCC) {
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                    Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX,
                    new String[]{transId});
        } else {
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_OUTBOX);
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                    Rms.TRANS_ID + "=?", new String[]{transId});
        }
    }

    public static void dealImRecvFileDone(JSONObject jsonObj) {
        boolean isCC = true;
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[]{Rms._ID},
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX, new String[]{transId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    isCC = false;
                }
            } finally {
                cursor.close();
            }
        }
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        values.put(Rms.TRANS_SIZE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_FILE_TRANS_SIZE));
        values.put(Rms.ERROR_CODE, 0);
        values.put(Rms.FILE_PATH, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH));
        if (!isCC) {
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                    Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX,
                    new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
        } else {
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_SENT);
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                    Rms.TRANS_ID + "=?", new String[]{transId});
        }
    }

    public static void dealImRecvFileFailed(JSONObject jsonObj) {
        boolean isCC = true;
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG, new String[]{Rms._ID},
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX, new String[]{transId}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    isCC = false;
                }
            } finally {
                cursor.close();
            }
        }
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_FAIL);
        values.put(Rms.ERROR_CODE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE));
        if (!isCC) {
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                    Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX + " AND " + Rms.STATUS + "!=" + Rms.STATUS_SUCC,
                    new String[]{transId});
        } else {
            values.put(Rms.TYPE, Rms.MESSAGE_TYPE_FAILED);
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                    Rms.TRANS_ID + "=? AND " + Rms.STATUS + "!=" + Rms.STATUS_SUCC + " AND " + Rms.STATUS + "!=" + Rms.STATUS_RECEIVED,
                    new String[]{transId});
        }
    }

    public static void dealImSendFileProgress(JSONObject jsonObj) {
        ContentValues values = new ContentValues();
        values.put(Rms.TRANS_SIZE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_FILE_TRANS_SIZE));
        values.put(Rms.STATUS, Rms.STATUS_PENDING);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_OUTBOX);
        values.put(Rms.ERROR_CODE, 0);
        Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND (" + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX + " OR " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_FAILED + ")",
                new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
    }

    public static void dealImSendFileOk(JSONObject jsonObj) {
        if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_REPORT, false)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(Rms.TRANS_SIZE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_FILE_TRANS_SIZE));
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_SENT);
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        String conversationId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID);
        if (!TextUtils.isEmpty(conversationId)) {
            values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        }
        values.put(Rms.ERROR_CODE, 0);
        String xml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_MSG_XML);
        if (!TextUtils.isEmpty(xml)) {
            values.put(Rms.RMS_EXTRA, xml);
            values.put(Rms.STATUS, Rms.STATUS_HTTP_UPLOADED);
        } else {
            values.put(Rms.STATUS, Rms.STATUS_SUCC);
        }
        Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX,
                new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
    }

    public static void dealImSendFileFailed(JSONObject jsonObj) {
        if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_REPORT, false)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_FAIL);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_FAILED);
        values.put(Rms.ERROR_CODE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE));
        if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE) == MtcImConstants.MTC_IM_ERR_FTHTTP_FORBIDDEN) {
            //统一认证校验失败，清除缓存token
            RcsTokenHelper.clearCacheFileToken();
        }
        Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX,
                new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
    }

    public static void dealImFecthReject(JSONObject jsonObj) {
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_FAIL);
        Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX,
                new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
    }

    public static Uri dealImRecvGsInvite(JSONObject jsonObj) {
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        boolean isBlocked = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_IS_BLOCKED, false);
        if (isBlocked) {
            RcsCallWrapper.rcsGeoReject(transId);
            return null;
        } else {
            // 过滤已经在数据库中的邀请（网络差的情况下有可能接收到transId相同的邀请）
            Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
                    new String[]{Rms._ID}, Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_INBOX,
                    new String[]{transId}, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        return null;
                    }
                } finally {
                    cursor.close();
                }
            }
            String address = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ADDRESS);
            String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
            if (!jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_OFFLINE, false)) {
                RcsCallWrapper.rcsGeoAccept(transId);
            } else {
                if (TextUtils.isEmpty(groupChatId)) {
                    RcsCallWrapper.rcsFetchGeo(address, transId);
                } else {
                    RcsCallWrapper.rcsFetchGroupGeo(address, transId, groupChatId);
                }
            }
            ContentValues values = new ContentValues();
            boolean isDirect = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_DIRECT, false);
            boolean isCC = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_CC, false);
            boolean isSilence = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_SILENCE, false);
            values.put(Rms.ADDRESS, isDirect ? RmsDefine.PC_ADDRESS : address);
            values.put(Rms.DATE, System.currentTimeMillis());
            values.put(Rms.STATUS, Rms.STATUS_PENDING);
            values.put(Rms.TRANS_ID, transId);
            values.put(Rms.FILE_TYPE, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_TYPE));
            values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_GEO);
            values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
            values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
            values.put(Rms.SUB_ID, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_SUB_ID, -1));
            values.put(Rms.TYPE, isCC ? Rms.MESSAGE_TYPE_OUTBOX : Rms.MESSAGE_TYPE_INBOX);
            values.put(Rms.FILE_TYPE, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_TYPE));
            values.put(Rms.FILE_PATH, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH));
            values.put(Rms.RMS_EXTRA, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY));
            values.put(Rms.TIMESTAMP, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_TIMESTAMP));
            values.put(Rms.IMDN_STRING, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
            values.put(Rms.MIX_TYPE, RcsUtilsEx.getMixType(isCC, isSilence, isDirect, false));
            if (!TextUtils.isEmpty(groupChatId)) {
                values.put(Rms.GROUP_CHAT_ID, groupChatId);
                if (!RcsGroupDealEx.haveGroupInDb(groupChatId)) {
                    RcsGroupDealEx.insertGroup(jsonObj, RmsGroup.STATE_STARTED);
                }
            }
            // 插入 sms 表和 rms 用 id 做关联
            Uri smsUri = MmsUtils.insertSmsMessage(Factory.get().getApplicationContext(),
                    Telephony.Sms.CONTENT_URI,
                    RcsServiceManager.getSubId(),
                    isDirect ? RmsDefine.PC_ADDRESS : address,
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
    }

    public static int dealImRecvGsFailed(JSONObject jsonObj) {
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_FAIL);
        values.put(Rms.ERROR_CODE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE));
        return Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND " + Rms.STATUS + "!=" + Rms.STATUS_SUCC, new String[]{transId});
    }

    public static int dealImRecvGsOk(JSONObject jsonObj) {
        boolean isCC = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_CC, false);
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        ContentValues values = new ContentValues();
        values.put(Rms.TYPE, isCC ? Rms.MESSAGE_TYPE_SENT : Rms.MESSAGE_TYPE_INBOX);
        values.put(Rms.MESSAGE_TYPE, Rms.RMS_MESSAGE_TYPE_GEO);
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        values.put(Rms.FILE_PATH, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_FILE_PATH));
        values.put(Rms.RMS_EXTRA, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_BODY));
        values.put(Rms.ERROR_CODE, 0);
        return Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms.TRANS_ID + "=?", new String[]{transId});
    }

    public static void dealImShareGsOk(JSONObject jsonObj) {
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_SUCC);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_SENT);
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        values.put(Rms.ERROR_CODE, 0);
        Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX,
                new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
    }

    public static void dealImShareGsFailed(JSONObject jsonObj) {
        ContentValues values = new ContentValues();
        values.put(Rms.STATUS, Rms.STATUS_FAIL);
        values.put(Rms.TYPE, Rms.MESSAGE_TYPE_FAILED);
        values.put(Rms.CONTRIBUTION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONT_ID));
        values.put(Rms.CONVERSATION_ID, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID));
        values.put(Rms.ERROR_CODE, jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE));
        Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values,
                Rms.TRANS_ID + "=? AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_OUTBOX,
                new String[]{jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID)});
    }

    public static void dealImRecv1To1Invite(JSONObject jsonObj) {
        String address = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ADDRESS);
        boolean isBlocked = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_IS_BLOCKED, false);
        if (isBlocked) {
            RcsCallWrapper.rcs1To1SessReject(address, RcsImServiceConstants.EN_MTC_IM_SESS_REJECT_REASON_BUSY);
        } else {
            RcsCallWrapper.rcs1To1SessAccept(address);
        }
    }

    public static void dealImRecvImdnStatus(JSONObject jsonObj) {
        try {
            String imdn = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
            HashMap<String, Integer> statusNew = new HashMap<String, Integer>();
            JSONObject obj = jsonObj.optJSONObject(RcsJsonParamConstants.RCS_JSON_IMDN_STATUS);

            if (obj == null || TextUtils.isEmpty(imdn)) return;

            Iterator<?> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next().toString();
                statusNew.put(key, obj.getInt(key));
            }

            if (statusNew.size() == 0) return;

            String selection = Rms.IMDN_STRING + "=?" + " AND " + Rms.TYPE + "=" + Rms.MESSAGE_TYPE_SENT;
            Cursor c = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
                    new String[]{Rms.IMDN_STATUS, Rms.ADDRESS}, selection, new String[]{imdn}, null);
            String oldImdnStatus = "";

            if (c == null) return;

            String address = null;
            if (c.moveToFirst()) {
                oldImdnStatus = c.getString(0);
                address = c.getString(1);
                if (oldImdnStatus == null) oldImdnStatus = "";
            }
            c.close();

            HashMap<String, Integer> statusMap = new HashMap<String, Integer>();
            String[] statusArray = oldImdnStatus.split(";");
            for (String status : statusArray) {
                String[] keyValue = status.split(":");
                if (keyValue.length == 2)
                    statusMap.put(keyValue[0], Integer.valueOf(keyValue[1]));
            }

            for (String key : statusNew.keySet()) {
                Integer value = statusNew.get(key);
                //防止已送达回执在已读回执之后收到
                if (statusMap.containsKey(key) && statusMap.get(key) == RcsImServiceConstants.EN_MTC_IMDN_STATE_DISPLAY) {
                    continue;
                }
                statusMap.put(key, value);
            }

            StringBuilder buf = new StringBuilder();
            for (String key : statusMap.keySet()) {
                Integer value = statusMap.get(key);
                buf.append(key + ":" + value + ";");
            }

            ContentValues values = new ContentValues();
            if (!TextUtils.isEmpty(address)) {
                String[] numbers = address.split(";");
                if (numbers.length == statusMap.size())
                    values.put(Rms.STATUS, Rms.STATUS_RECEIVED);
            }
            values.put(Rms.IMDN_STATUS, buf.toString());
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values, selection, new String[]{imdn});

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void dealImdnTransIdUpdate(JSONObject jsonObj) {
        String cookie = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);
        if (TextUtils.isEmpty(cookie)) {
            return;
        }
        ContentValues values = new ContentValues();
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        if (!TextUtils.isEmpty(transId)) {
            values.put(Rms.TRANS_ID, transId);
        }
        String convId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CONV_ID);
        if (!TextUtils.isEmpty(convId)) {
            values.put(Rms.CONVERSATION_ID, convId);
        }
        values.put(Rms.IMDN_STRING, jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID));
        try {
            Factory.get().getApplicationContext().getContentResolver().update(Rms.CONTENT_URI_LOG, values, Rms._ID + "=" + Integer.valueOf(cookie), null);
        } catch (NumberFormatException e) {
        }
    }

    public static void dealImRecvCapResult(JSONObject jsonObj) {
        String phone = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_PHONE);
        if (!TextUtils.isEmpty(phone)) {
            String normalizedNumber = RcsNumberUtils.formatPhoneWithCountryPrefixIfNo(phone, RcsNumberUtils.getCountryPrefix(RcsServiceManager.getUserName()));
            String cap = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_CAP);
            boolean isRcs = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_RCS);
            long timeStamp = jsonObj.optLong(RcsJsonParamConstants.RCS_JSON_CAP_UPDATE_TIME);
            ContentValues values = new ContentValues();
            StringBuilder extensions = new StringBuilder();
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_IM)) {
                values.put(RmsDefine.Capability.CAPABILITY_IM_SESSION, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_IM_SESSION, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_FT)) {
                values.put(RmsDefine.Capability.CAPABILITY_FILE_TRANSFER, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_FILE_TRANSFER, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_VOICE_CALL)) {
                values.put(RmsDefine.Capability.CAPABILITY_IP_VOICE_CALL, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_IP_VOICE_CALL, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_VIDEO_CALL)) {
                values.put(RmsDefine.Capability.CAPABILITY_IP_VIDEO_CALL, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_IP_VIDEO_CALL, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_GEOPUSH)) {
                values.put(RmsDefine.Capability.CAPABILITY_GEOLOC_PUSH, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_GEOLOC_PUSH, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_FTHTTP)) {
                values.put(RmsDefine.Capability.CAPABILITY_FTHTTP, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_FTHTTP, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_UP_FTSMS)) {
                values.put(RmsDefine.Capability.CAPABILITY_UP_FTSMS, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_UP_FTSMS, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_UP_GEOSMS)) {
                values.put(RmsDefine.Capability.CAPABILITY_UP_GEOSMS, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_UP_GEOSMS, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_CALL_COMPOSER)) {
                values.put(RmsDefine.Capability.CAPABILITY_CALL_COMPOSER, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_CALL_COMPOSER, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_CALL_UNANSWERED)) {
                values.put(RmsDefine.Capability.CAPABILITY_CALL_UNANSWERED, 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_CALL_UNANSWERED, 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_CPM_SESSION)) {
                values.put(RmsDefine.Capability.CAPABILITY_CPM_SESSION , 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_CPM_SESSION , 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_CPM_STANDALONE)) {
                values.put(RmsDefine.Capability.CAPABILITY_CPM_STANDALONE , 1);
            } else {
                values.put(RmsDefine.Capability.CAPABILITY_CPM_STANDALONE , 0);
            }
            if (cap.contains(RcsCapServiceConstants.MTC_CAP_PUBLIC_MSG)) {
                if (extensions.length() > 0) {
                    extensions.append(";");
                }
                extensions.append(RcsCapServiceConstants.CAP_TAG_PUBLIC_MSG);
            } else if (cap.contains(RcsCapServiceConstants.MTC_CAP_BURN_MSG)) {
                if (extensions.length() > 0) {
                    extensions.append(";");
                }
                extensions.append(RcsCapServiceConstants.CAP_TAG_BURN_MSG);
            }
            values.put(RmsDefine.Capability.CAPABILITY_EXTENSIONS, extensions.toString());
            values.put(RmsDefine.Capability.RCS_ENABLE, isRcs);
            values.put(RmsDefine.Capability.TIMESTAMP, timeStamp);
            int count = Factory.get().getApplicationContext().getContentResolver().update(RmsDefine.Capability.CONTENT_URI, values, RmsDefine.Capability.CONTACT_NUMBER + "=?", new String[] { normalizedNumber });
            if (count == 0) {
                values.put(RmsDefine.Capability.CONTACT_NUMBER, normalizedNumber);
                Factory.get().getApplicationContext().getContentResolver().insert(RmsDefine.Capability.CONTENT_URI, values);
            }

        }
    }

    private static String genChatbotTip(boolean card, boolean file, String contentType) {
        String messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_normal_message);
        if (card) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_card_message);
        } else if (file) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_file_message);
        }
        if (contentType.startsWith("image")) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_image_message);
        } else if (contentType.startsWith("audio")) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_audio_message);
        } else if (contentType.startsWith("video")) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_video_message);
        } else if (contentType.startsWith("text/x-vcard") || contentType.startsWith("text/x-vCard") || contentType.startsWith("text/vcard")) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_vcard_message);
        } else if (contentType.startsWith("video")) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_geo_message);
        } else if (contentType.startsWith("application/commontemplate+xml")) {
            messageTip = Factory.get().getApplicationContext().getString(R.string.rcs_chatbot_a2p_message);
        }
        return messageTip;
    }

    private static int getChatbotRmsMessageType(String contentType) {
        if (contentType.startsWith("image")) {
            return Rms.RMS_MESSAGE_TYPE_CHATBOT_IMAGE;
        } else if (contentType.startsWith("audio")) {
            return Rms.RMS_MESSAGE_TYPE_CHATBOT_AUDIO;
        } else if (contentType.startsWith("video")) {
            return Rms.RMS_MESSAGE_TYPE_CHATBOT_VIDEO;
        } else if (contentType.startsWith("text/x-vcard") || contentType.startsWith("text/x-vCard") || contentType.startsWith("text/vcard")) {
            return Rms.RMS_MESSAGE_TYPE_CHATBOT_VCARD;
        } else if (contentType.startsWith("video")) {
            return Rms.RMS_MESSAGE_TYPE_CHATBOT_VIDEO;
        }
        return Rms.RMS_MESSAGE_TYPE_CHATBOT_FILE;
    }

}
