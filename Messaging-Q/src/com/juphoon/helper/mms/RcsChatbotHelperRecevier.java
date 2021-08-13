package com.juphoon.helper.mms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.action.InsertNewMessageAction;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.sms.MmsSmsUtils;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.TextUtil;
import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.rcs.JApplication;

import java.io.File;
import java.util.ArrayList;

public class RcsChatbotHelperRecevier extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        if (TextUtils.equals(action, RcsChatbotDefine.ACTION_REPLY)) {
            sendReply(intent);
        } else if (TextUtils.equals(action, RcsChatbotDefine.ACTION_GEO)) {
            sendGeo(intent);
        } else if (TextUtils.equals(action, RcsChatbotDefine.ACTION_MENU_REPLY)) {
            sendMenuReply(intent);
        } else if (TextUtils.equals(action, RcsChatbotDefine.ACTION_SUGGESTION)) {
            sendSuggestion(intent);
        } else if (TextUtils.equals(action, RcsChatbotDefine.ACTION_SHARE_DATA)) {
            sendShareData(intent);
        } else if (TextUtils.equals(action, RcsChatbotDefine.ACTION_LAUNCH_CONVERSATION)) {
            launchConversationActivity(intent);
        }
    }

    private void sendShareData(Intent intent) {
        String rmsUri = intent.getStringExtra(RcsChatbotDefine.KEY_RMSURI);
        String chatbotId = intent.getStringExtra(RcsChatbotDefine.KEY_CHATBOT_ID);
        if (!TextUtils.isEmpty(rmsUri)) {
            RcsDatabaseMessages.RmsMessage rmsMessage = RcsMmsUtils.loadRms(Uri.parse(rmsUri));
            RcsCallWrapper.rcsSendMessage1To1("", rmsMessage.mAddress, RcsChatbotHelper.getShareDeviceData(0), RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SHAREDATA, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, rmsMessage.mConversationId, rmsMessage.mTrafficType, rmsMessage.mContributionId);
        } else {
            RcsCallWrapper.rcsSendMessage1To1("", chatbotId, RcsChatbotHelper.getShareDeviceData(0), RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SHAREDATA, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, RcsMmsUtils.getRmsConversationIdFromChatbotId(chatbotId), null, null);
        }

    }

    private void sendMenuReply(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = intent.getStringExtra(RcsChatbotDefine.KEY_REPLYJSON);
                String conversationId = intent.getStringExtra(RcsChatbotDefine.KEY_CONVERSATIONID);
                ParticipantData participantData = BugleDatabaseOperations.getOrCreateSelf(DataModel.get().getDatabase(), RcsServiceManager.getSubId());
                RcsChatbotReplyBean replyBean = new Gson().fromJson(json, RcsChatbotReplyBean.class);
                MessageData message = MessageData.createChatbotReplyMessage(conversationId, participantData.getId(), replyBean.response.reply.displayText, json, null, null);
                InsertNewMessageAction.insertNewMessage(message);
            }
        }).start();
    }

    private void sendReply(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = intent.getStringExtra(RcsChatbotDefine.KEY_REPLYJSON);
                String rmsUri = intent.getStringExtra(RcsChatbotDefine.KEY_RMSURI);
                String conversationId = intent.getStringExtra(RcsChatbotDefine.KEY_CONVERSATIONID);
                Log.i("vcx","sendReply----conversationId:"+conversationId);
                Log.i("vcx","sendReply----json:"+json);
                Log.i("vcx","sendReply----rmsUri:"+rmsUri);
                if (TextUtils.isEmpty(rmsUri)) {
                    return;
                }
                RcsDatabaseMessages.RmsMessage rmsMessage = RcsMmsUtils.loadRms(Uri.parse(rmsUri));
                if (rmsMessage == null) {
                    return;
                }
                ParticipantData participantData = BugleDatabaseOperations.getOrCreateSelf(DataModel.get().getDatabase(), RcsServiceManager.getSubId());
                RcsChatbotReplyBean replyBean = new Gson().fromJson(json, RcsChatbotReplyBean.class);
                MessageData message = MessageData.createChatbotReplyMessage(conversationId, participantData.getId(), replyBean.response.reply.displayText, json, rmsMessage.mContributionId, rmsMessage.mTrafficType);
                InsertNewMessageAction.insertNewMessage(message);
            }
        }).start();
    }

    private void sendGeo(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String conversationId = intent.getStringExtra(RcsChatbotDefine.KEY_CONVERSATIONID);
                ParticipantData participantData = BugleDatabaseOperations.getOrCreateSelf(DataModel.get().getDatabase(), RcsServiceManager.getSubId());
                MessageData messageData = MessageData.createDraftRmsMessage(conversationId, participantData.getId(), null);
                messageData.addPart(MessagePartData.createMediaMessagePart(ContentType.APP_GEO_MESSAGE, Uri.fromFile(new File(intent.getStringExtra(RcsChatbotDefine.KEY_FILEPATH))), 80, 80));
                InsertNewMessageAction.insertNewMessage(messageData);
            }
        }).start();
    }

    private void sendSuggestion(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = intent.getStringExtra(RcsChatbotDefine.KEY_REPLYJSON);
                String rmsUri = intent.getStringExtra(RcsChatbotDefine.KEY_RMSURI);
                RcsDatabaseMessages.RmsMessage rmsMessage = RcsMmsUtils.loadRms(Uri.parse(rmsUri));
                RcsChatbotReplyBean replyBean = new Gson().fromJson(json, RcsChatbotReplyBean.class);
                RcsCallWrapper.rcsSendMessage1To1("", rmsMessage.mAddress, new Gson().toJson(replyBean), RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SUGGESTION, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, rmsMessage.mConversationId, rmsMessage.mTrafficType, rmsMessage.mContributionId);
            }
        }).start();
    }

    private void launchConversationActivity(Intent intent) {
        String address = intent.getStringExtra(RcsChatbotDefine.KEY_ADDRESS);
        if (!TextUtils.isEmpty(address)) {
            final ArrayList<ParticipantData> participants = new ArrayList<>();
            participants.add(ParticipantData.getFromRawPhoneBySystemLocale(address));
            RcsChatbotHelper.RcsChatbot chatbot = RcsChatbotHelper.getChatbotInfoBySmsOrServiceId(participants.get(0).getNormalizedDestination());
            if (chatbot != null) {
                participants.clear();
                participants.add(ParticipantData.getFromRawPhoneBySystemLocale(RcsChatbotHelper.formatServiceIdWithNoSip(chatbot.serviceId)));
            }
            new Thread() {
                @Override
                public void run() {
                    long threadId =  MmsSmsUtils.Threads.getOrCreateThreadId(JApplication.sContext, participants.get(0).getNormalizedDestination());
                    String conversationId = BugleDatabaseOperations.getOrCreateConversation(DataModel.get().getDatabase(), threadId,
                            false, participants, false, false, null);
                    UIIntents.get().launchConversationActivity(JApplication.sContext, conversationId, null, null, false);
                }
            }.start();
        }
    }

}
