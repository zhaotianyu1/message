package com.juphoon.helper.mms;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.action.DeleteMessageAction;
import com.android.messaging.datamodel.action.ReceiveRmsMessageAction;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.ui.appsettings.ApplicationSettingsActivity;
import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.cmcc.lemon.MtcImSessConstants;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.receiver.RcsReceiverServiceEx;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;
import com.juphoon.ui.RcsRegisterActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Rcs IM 接收消息处理，判断
 *
 */
public class RcsImReceiverServiceEx extends JobIntentService {
    private static final String TAG = "RcsImReceiverService";

    public static final String DEAL_TYPE                ="deal_type";
    public static final int DEAL_MESSAGE_RECV           = 0;
    public static final int UPDATE_MESSAGE_STATUS       = 1;
    public static final int UPDATE_FILE_STATUS          = 2;
    public static final int UPDATE_THUMB_PATH           = 3;

    public static final int JOB_ID = 1003;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, RcsImReceiverServiceEx.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        onHandleIntent(intent);
    }

    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final String json = intent.getStringExtra(RcsJsonParamConstants.RCS_JSON_KEY);
            if (json == null) {
                return;
            }
            try {
                JSONObject jsonObj = new JSONObject(json);
                if (TextUtils.equals(action, RcsJsonParamConstants.RCS_ACTION_IM_NOTIFY)) {
                    if (!RcsReceiverServiceEx.dealIm(jsonObj)) {
                        return;
                    }
                }
                dealRcsBroadcast(jsonObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    RcsBroadcastHelper.doReceive(action, json);
                }
            });
        }
    }

    private void dealRcsBroadcast(JSONObject jsonObj) {
        String jsonAction = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ACTION);
        if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_RECV_MSG) || jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_RECV_MSG)) {
            dealMsgRecv(jsonObj);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_RECV_INVITE)) {
            // GroupNotificationList.UpdateNotification(RcsImReceiverService.this);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_FAILED)) {
            updateMsgStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_FAILED);
            // 处理消息发送失败并返回 MTC_IM_ERR_BOT_CONV_NEEDED
            dealSendFailForChatbot(jsonObj);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_OK)) {
            updateMsgStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_INVITE)) {
            dealMsgRecv(jsonObj);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECVING_PROGRESS)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_DONE)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_INCOMING_COMPLETE);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_FAILED)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SENDING_PROGRESS)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_SENDING);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_OK)) {
            dealImSendFileOk(jsonObj);
            // 如果不是xml文件消息则更新成功状态
            if (TextUtils.isEmpty(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_MSG_XML))) {
                updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE);
            }
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_FAILED)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_FAILED);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_FETCH_REJECT)) {
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_OK/* 发送位置成功 */)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_FAILED)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_FAILED);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_INVITE)) {
            dealMsgRecv(jsonObj);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_DONE)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_INCOMING_COMPLETE);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_FAILED)) {
            updateFileMessageStatus(jsonObj, MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_MSG_SEND_RESULT)) {
            boolean sendOk = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_RESULT, false);
            if (sendOk) {
                updateMsgStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE);
            } else {
                updateMsgStatus(jsonObj, MessageData.BUGLE_STATUS_OUTGOING_FAILED);
            }
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_RECV_SYS_MSG)) {
            dealMsgRecv(jsonObj);
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IMDN_STATUS)) {
            int status = getStatusByImdnStatus(jsonObj);
            if (status != MessageData.BUGLE_STATUS_UNKNOWN) {
                updateMsgDeliOrDispStatus(jsonObj, status);
            }
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_HTTP_THUMB_DOWNLOAD_RESULT)) {
            boolean result = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_RESULT);
            if (result) {
                updateFileMessageThumb(jsonObj);
            }
        } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_REPEAT_LOGIN)) {
            Intent intent = new Intent(RcsMmsInitHelper.getContext(), ApplicationSettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(RcsJsonParamConstants.RCS_JSON_ACTION_REPEAT_LOGIN);
            startActivity(intent);
        }
    }

    public static void dealImSendFileOk(JSONObject jsonObj) {
        if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_REPORT, false)) {
            return;
        }
        String pcFileMsgXml = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_HTTP_MSG_XML);
        if (!TextUtils.isEmpty(pcFileMsgXml)) {
            String cookie = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);
            if (TextUtils.isEmpty(cookie) || Integer.valueOf(cookie) < 0) {
                // cookie 正常是表示消息 id，不合法则返回
                return;
            }
            Uri msgUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(cookie));
            if (msgUri == null) {
                return;
            }
            RcsDatabaseMessages.RmsMessage rms = RcsMmsUtils.loadRms(msgUri);
            String conversationId = RcsMmsUtils.getRmsConversationIdFromThreadID(rms.mThreadId);
            RcsChatbotHelper.RcsChatbot chatbotinfo = RcsChatbotHelper.getChatbotInfoByServiceId(rms.mAddress);
            String ret = null;
            if (RcsCallWrapper.rcsGetIsHttpOpen()) {
                int threadType = getThreadType(rms);
                //chatbot
                if (chatbotinfo != null) {
                    ret = RcsCallWrapper.rcsSendMessage1To1(cookie, chatbotinfo.serviceId, pcFileMsgXml, RcsImServiceConstants.RCS_MESSAGE_TYPE_HTTP, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, conversationId, null, null);
                } else {
                    if (threadType == RmsDefine.COMMON_THREAD) {
                        ret = RcsCallWrapper.rcsSendMessage1To1(cookie, rms.mAddress, pcFileMsgXml, RcsImServiceConstants.RCS_MESSAGE_TYPE_HTTP, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_DEFAULT, conversationId, null, null);
                    } else if (threadType == RmsDefine.BROADCAST_THREAD) {
                        ret = RcsCallWrapper.rcsSendMessage1ToM(cookie, rms.mAddress, pcFileMsgXml, RcsImServiceConstants.RCS_MESSAGE_TYPE_HTTP);
                    } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
                        RcsGroupHelper.RcsGroupInfo info = RcsGroupHelper.getGroupInfo(rms.mGroupChatId);
                        if (info != null) {
                            if (RcsCallWrapper.rcsGroupSessIsExist(rms.mGroupChatId)) {
                                ret = RcsCallWrapper.rcsSendGroupMsg(cookie, info.mGroupChatId, pcFileMsgXml, MtcImSessConstants.EN_MTC_IM_SESS_CONT_MSG_FTHTTP, null);
                            }
                        }
                    }
                }
            } else {
                if (chatbotinfo == null) {
                    return;
                }
                ret = RcsCallWrapper.rcsSendMessage1To1(cookie, chatbotinfo.serviceId, pcFileMsgXml, RcsImServiceConstants.RCS_MESSAGE_TYPE_HTTP, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, rms.mConversationId, rms.mTrafficType, rms.mContributionId);
            }
            if (TextUtils.isEmpty(ret)) {
                RcsMmsUtils.updateMessageFailed((int) rms.mRowId, true);
            }
        }
    }

    private void dealMsgRecv(JSONObject jsonObj) {
        String messageId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);
        if (TextUtils.isEmpty(messageId)) {
            return;
        }
        Uri msgUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(messageId));
        if (msgUri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DEAL_TYPE, DEAL_MESSAGE_RECV);
        values.put(MessageColumns.SMS_MESSAGE_URI, msgUri.toString());
        values.put(MessageColumns.STATUS, MessageData.BUGLE_STATUS_FIRST_INCOMING);
        ReceiveRmsMessageAction rmsAction = new ReceiveRmsMessageAction(values);
        rmsAction.start();
    }

    private void updateMsgStatus(final JSONObject jsonObj, final int status) {
        String messageId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);
        if (TextUtils.isEmpty(messageId)) {
            return;
        }
        Uri msgUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Long.valueOf(messageId));
        if (msgUri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DEAL_TYPE, UPDATE_MESSAGE_STATUS);
        values.put(MessageColumns.SMS_MESSAGE_URI, msgUri.toString());
        values.put(MessageColumns.STATUS, status);
        ReceiveRmsMessageAction rmsAction = new ReceiveRmsMessageAction(values);
        rmsAction.start();
    }

    private void updateFileMessageStatus(final JSONObject jsonObj, final int status) {
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        if (TextUtils.isEmpty(transId)) {
            return;
        }
        Uri msgUri = RcsMmsUtils.getRmsUriWithTransId(transId);
        if (msgUri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DEAL_TYPE, UPDATE_FILE_STATUS);
        values.put(MessageColumns.SMS_MESSAGE_URI, msgUri.toString());
        values.put(MessageColumns.STATUS, status);
        ReceiveRmsMessageAction rmsAction = new ReceiveRmsMessageAction(values);
        rmsAction.start();
    }

    private void updateMsgDeliOrDispStatus(final JSONObject jsonObj, final int status) {
        String imdn = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
        if (TextUtils.isEmpty(imdn)) {
            return;
        }
        Uri msgUri = RcsMmsUtils.getRmsUriWithImdnId(imdn);
        if (msgUri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DEAL_TYPE, UPDATE_MESSAGE_STATUS);
        values.put(MessageColumns.SMS_MESSAGE_URI, msgUri.toString());
        values.put(MessageColumns.STATUS, status);
        ReceiveRmsMessageAction rmsAction = new ReceiveRmsMessageAction(values);
        rmsAction.start();
    }

    private void updateFileMessageThumb(final JSONObject jsonObj) {
        String path = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_THUMB_PATH);
        String transId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID);
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(transId)) {
            return;
        }
        Uri msgUri = RcsMmsUtils.getRmsUriWithTransId(transId);
        if (msgUri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DEAL_TYPE, UPDATE_THUMB_PATH);
        values.put(MessageColumns.SMS_MESSAGE_URI, msgUri.toString());
        values.put(MessageColumns.STATUS, -1);
        ReceiveRmsMessageAction rmsAction = new ReceiveRmsMessageAction(values);
        rmsAction.start();
    }

    private int getStatusByImdnStatus(JSONObject jsonObj) {
        try {
            String imdn = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_IMDN_ID);
            HashMap<String, Integer> statusNew = new HashMap<String, Integer>();
            JSONObject obj = jsonObj.optJSONObject(RcsJsonParamConstants.RCS_JSON_IMDN_STATUS);

            if (obj == null || TextUtils.isEmpty(imdn)) {
                return MessageData.BUGLE_STATUS_UNKNOWN;
            }

            Iterator<?> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next().toString();
                statusNew.put(key, obj.getInt(key));
            }

            if (statusNew.size() == 0) {
                return MessageData.BUGLE_STATUS_UNKNOWN;
            }

            String selection = Rms.IMDN_STRING + "=?" +" AND "+ Rms.TYPE+"=" + Rms.MESSAGE_TYPE_SENT;
            Cursor c = RcsMmsInitHelper.getContext().getContentResolver().query(Rms.CONTENT_URI_LOG,
                    new String[] { Rms.IMDN_STATUS, Rms.ADDRESS }, selection, new String[]{ imdn }, null);
            String oldImdnStatus = "";

            if (c == null) return MessageData.BUGLE_STATUS_UNKNOWN;

            String address = null;
            if (c.moveToFirst()) {
                oldImdnStatus = c.getString(0);
                address = c.getString(1);
                if (oldImdnStatus == null) {
                    oldImdnStatus = "";
                }
            }
            c.close();

            HashMap<String, Integer> statusMap = new HashMap<String, Integer>();
            String[] statusArray = oldImdnStatus.split(";");
            for (String status : statusArray){
                String[] keyValue = status.split(":");
                if (keyValue.length == 2) {
                    statusMap.put(keyValue[0], Integer.valueOf(keyValue[1]));
                }
            }

            for (String key : statusNew.keySet()) {
                Integer value = statusNew.get(key);
                //防止已送达回执在已读回执之后收到
                if (statusMap.containsKey(key) && statusMap.get(key) == RcsImServiceConstants.EN_MTC_IMDN_STATE_DISPLAY) {
                    continue;
                }
                statusMap.put(key, value);
            }

            if (!TextUtils.isEmpty(address)) {
                String[] numbers = address.split(";");
                if (numbers.length == statusMap.size()) {
                    Iterator<?> iter = statusMap.entrySet().iterator();
                    int displayedCount = 0;
                    int legacySmsCount = 0;
                    int legacyMmsCount = 0;
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        String key = (String)entry.getKey();
                        Integer val = (Integer)entry.getValue();
                        if (val == RcsImServiceConstants.EN_MTC_IMDN_STATE_DISPLAY) {
                            displayedCount++;
//                        } else if (val == RcsImServiceConstants.EN_MTC_IMDN_STATE_LEGACYSMS) {
//                            legacySmsCount++;
//                        } else if (val == RcsImServiceConstants.EN_MTC_IMDN_STATE_LEGACYMMS) {
//                            legacyMmsCount++;
                        }
                    }
                    if (legacySmsCount == statusMap.size()) {
//                        return MessageData.BUGLE_STATUS_OUTGOING_LEGACYSMS;
                    } else if (legacyMmsCount == statusMap.size()) {
//                        return MessageData.BUGLE_STATUS_OUTGOING_LEGACYMMS;
                    } else if (displayedCount == statusMap.size()) {
                        return MessageData.BUGLE_STATUS_OUTGOING_DISPLAYED;
                    } else {
                        return MessageData.BUGLE_STATUS_OUTGOING_DELIVERED;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return MessageData.BUGLE_STATUS_UNKNOWN;
    }

    private void dealSendFailForChatbot(JSONObject jsonObject) {
        if (jsonObject.optInt(RcsJsonParamConstants.RCS_JSON_ERROR_CODE) == MtcImConstants.MTC_IM_ERR_BOT_CONV_NEEDED) {
            final String serviceId = jsonObject.optString(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID);
            if (!TextUtils.isEmpty(serviceId)) {
                if (!RcsChatbotHelper.isChatbotByServiceId(serviceId)) {
                    RcsChatbotHelper.saveChatbot(RcsChatbotHelper.formatServiceIdWithSip(serviceId));
                    RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
                        @Override
                        public void run(boolean succ, String resultCode,String token) {
                            if (succ) {
                                RcsCallWrapper.rcsGetChatbotInfo("", token, serviceId, null);
                            }
                        }
                    });
                }
                String rmsId = jsonObject.optString(RcsJsonParamConstants.RCS_JSON_COOKIE);

                Uri rmsUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, Integer.valueOf(rmsId));
                DatabaseWrapper db = DataModel.get().getDatabase();
                MessageData message = BugleDatabaseOperations.readMessageData(db, rmsUri);
                if (message == null) {
                    return;
                }

                // 以 chatbot serviceId 发送消息
                RcsMessageForwardHelper.forwardMessage(RmsDefine.COMMON_THREAD, Integer.valueOf(rmsId), RcsChatbotHelper.formatServiceIdWithNoSip(serviceId));
                // 触发发送
                Intent intent = new Intent(RcsMessageSendService.ACTION_SEND_RCS_MESSAGE, null, RcsMmsInitHelper.getContext(), RcsMessageSendService.class);
                // juphoon
                RcsMessageSendService.enqueueWork(Factory.get().getApplicationContext(), intent);
                // 删除原来的消息
                DeleteMessageAction.deleteMessage(message.getMessageId());
            }
        }
    }

    private static int getThreadType(RcsDatabaseMessages.RmsMessage message) {
        if (!TextUtils.isEmpty(message.mGroupChatId)) {
            return RmsDefine.RMS_GROUP_THREAD;
        } else {
            String[] arrayAddress = message.mAddress.split(";");
            if (arrayAddress.length > 1) {
                return RmsDefine.BROADCAST_THREAD;
            }
            return RmsDefine.COMMON_THREAD;
        }
    }

}
