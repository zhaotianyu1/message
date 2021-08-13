package com.android.messaging.datamodel.action;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.messaging.BugleApplication;
import com.android.messaging.Factory;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.BugleNotifications;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.SyncManager;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.Api;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.juphoon.chatbotmaap.RcsChatbotImdnManager;
import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsPublicXmlMessage;
import com.juphoon.helper.mms.RcsDatabaseMessages.RmsMessage;
import com.juphoon.helper.mms.RcsImReceiverServiceEx;
import com.juphoon.helper.mms.RcsImdnManager;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.RcsTransProgressManager;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import java.io.File;
import java.util.ArrayList;

/**
 * juphoon 
 */
public class ReceiveRmsMessageAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;

    private static final String KEY_MESSAGE_VALUES = "message_values";

    public ReceiveRmsMessageAction(final ContentValues messageValues) {
        actionParameters.putParcelable(KEY_MESSAGE_VALUES, messageValues);
    }

    @Override
    protected Object executeAction() {
        final Context context = Factory.get().getApplicationContext();
        final DatabaseWrapper db = DataModel.get().getDatabase();
        final ContentValues messageValues = actionParameters.getParcelable(KEY_MESSAGE_VALUES);
        final String msgUri = messageValues.getAsString(MessageColumns.SMS_MESSAGE_URI);
        final int dealType = messageValues.getAsInteger(RcsImReceiverServiceEx.DEAL_TYPE);
        final int status = messageValues.getAsInteger(MessageColumns.STATUS);
        final Uri messageUri = Uri.parse(msgUri);
        RmsMessage rms = RcsMmsUtils.loadRms(messageUri);
        // 消息可能被删
        if (rms == null) {
            return null;
        }
        // 系统消息设置address
        if (rms.mMessageType == Rms.RMS_MESSAGE_TYPE_SYSTEM) {
            rms.mAddress = RcsMmsUtils.RMS_SYSTEM_MSG;
        }

        final ParticipantData rawSender = ParticipantData.getFromRawPhoneBySimLocale(rms.mAddress, rms.mSubId);
        final SyncManager syncManager = DataModel.get().getSyncManager();
        syncManager.onNewMessageInserted(rms.mTimestampInMillis);

        // 收到消息不要根据adress生成threadId了，用原始表的，对群聊做兼容
        final boolean blocked = BugleDatabaseOperations.isBlockedDestination(db, rawSender.getNormalizedDestination());
        final String conversationId = BugleDatabaseOperations.getOrCreateConversationFromThreadId(db, rms.mThreadId, blocked, rms.mSubId);

        MessageData message = null;
        if (!OsUtil.isSecondaryUser()) {
            if (dealType == RcsImReceiverServiceEx.DEAL_MESSAGE_RECV) {
                message = dealMsgRecv(context, db, rms, rawSender, conversationId, messageUri, blocked);

                RcsGroupHelper.RcsGroupInfo groupInfo = null;
                if (!TextUtils.isEmpty(rms.mGroupChatId)) {
                    groupInfo = RcsGroupHelper.getGroupInfo(rms.mGroupChatId);
                }
                if (groupInfo != null && groupInfo.mRecvType != RmsDefine.RmsGroup.RECV_TYPE_NORMAL) {
                    // 群聊设置不提醒
                } else if ((rms.mMixType & Rms.MIX_TYPE_SILENCE) > 0 || (rms.mMixType & Rms.MIX_TYPE_CC) > 0) {
                    BugleNotifications.update(true, conversationId, BugleNotifications.UPDATE_ALL);
                } else {
                    RcsPublicXmlMessage publicXmlMessage = null;

                    //juphoon chatbot消息 如果在此界面创建消息时候回复已读回执
                    if (message.getMessageRead()) {
                        if (!TextUtils.isEmpty(rms.mRmsChatbotServiceId)) {
                            if ((rms.mImdnType & MtcImConstants.MTC_PMSG_IMDN_DISP) > 0) {
                                RcsChatbotImdnManager.sendMessageDisplay(rms.mImdn, rms.mRmsChatbotServiceId, rms.mConversationId);
                            }
                        } else if (TextUtils.isEmpty(rms.mGroupChatId)) {
                            //一对一的RCS消息 在当前会话界面时收到消息,回复已读回执
                            if ((rms.mImdnType & MtcImConstants.MTC_PMSG_IMDN_DISP) > 0) {
                                RcsImdnManager.sendMessageDisplay(rms.mImdn, rms.mAddress, rms.mConversationId);
                            }
                        }
                    }
                    if (rms.isImage() || rms.isVideo()) {
                        // 如果缩略图不存在则延迟2秒通知，http消息此期间可能下完缩略图
                        if (TextUtils.isEmpty(rms.mThumbPath) || !new File(rms.mThumbPath).exists()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    BugleNotifications.update(false, conversationId, BugleNotifications.UPDATE_ALL);
                                }
                            }).start();
                        } else {
                            BugleNotifications.update(false, conversationId, BugleNotifications.UPDATE_ALL);
                        }
                    }
                    else {
                        //Chatbot 消息免打扰
                        if (!RcsChatbotHelper.isMuteNotify(rms.mRmsChatbotServiceId)) {
                            BugleNotifications.update(false, conversationId, BugleNotifications.UPDATE_ALL);
                        }
                    }
                }
            } else if (dealType == RcsImReceiverServiceEx.UPDATE_MESSAGE_STATUS) {
                message = BugleDatabaseOperations.readMessageData(db, messageUri);
                if (message != null) {
                    BugleDatabaseOperations.readMessagePartsData(db, message, false);
                    updateMsgStatus(context, db, rms, message, status);
                }
            } else if (dealType == RcsImReceiverServiceEx.UPDATE_FILE_STATUS) {
                message = BugleDatabaseOperations.readMessageData(db, messageUri);
                if (message != null) {
                    BugleDatabaseOperations.readMessagePartsData(db, message, false);
                    updateFileMessageStatus(context, db, rms, message, status);
                }
            } else if (dealType == RcsImReceiverServiceEx.UPDATE_THUMB_PATH) {
                message = BugleDatabaseOperations.readMessageData(db, messageUri);
                if (message != null) {
                    BugleDatabaseOperations.readMessagePartsData(db, message, false);
                    updateFileMessageThumbPath(db, rms, message);
                }
            }
        } else {
            if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
                LogUtil.d(TAG, "ReceiveSmsMessageAction: Not inserting received SMS message for "
                        + "secondary user.");
            }
        }

        MessagingContentProvider.notifyMessagesChanged(conversationId);
        MessagingContentProvider.notifyPartsChanged();

        return message;
    }

    private ReceiveRmsMessageAction(final Parcel in) {
        super(in);
    }

    public static final Creator<ReceiveRmsMessageAction> CREATOR
            = new Creator<ReceiveRmsMessageAction>() {
        @Override
        public ReceiveRmsMessageAction createFromParcel(final Parcel in) {
            return new ReceiveRmsMessageAction(in);
        }

        @Override
        public ReceiveRmsMessageAction[] newArray(final int size) {
            return new ReceiveRmsMessageAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }

    /**
     * 处理收到的消息
     * @param context
     * @param db
     * @param rms
     * @param rawSender
     * @param conversationId
     * @param messageUri
     * @param blocked
     */
    private MessageData dealMsgRecv(Context context, DatabaseWrapper db, RmsMessage rms, ParticipantData rawSender,
                                    String conversationId, Uri messageUri, boolean blocked) {
        final boolean messageInFocusedConversation = DataModel.get().isFocusedConversation(conversationId);
        final boolean messageInObservableConversation = DataModel.get().isNewMessageObservable(conversationId);
        final boolean read = rms.mRead || messageInFocusedConversation;
        final boolean seen = read || messageInObservableConversation || blocked;
        MessageData message = null;
        // 更新原始表read和seen字段
        ContentValues msgValues = new ContentValues();
        msgValues.put(Rms.READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
        msgValues.put(Rms.SEEN, 1);
        context.getContentResolver().update(Rms.CONTENT_URI_LOG, msgValues, Rms._ID + " = " + rms.getId(), null);
        final ParticipantData self = ParticipantData.getSelfParticipant(rms.mSubId);
        String conversationServiceCenter = null;
        db.beginTransaction();
        try {
            final String participantId = BugleDatabaseOperations.getOrCreateParticipantInTransaction(db, rawSender);
            final String selfId = BugleDatabaseOperations.getOrCreateParticipantInTransaction(db, self);
            message = MessageData.createReceivedRmsMessage(messageUri, conversationId,
                    participantId, selfId, seen, read, rms);
            final MessagePartData messagePart = RcsMmsUtils.createRmsMessagePart(rms);
            if (messagePart != null) {
                message.addPart(messagePart);
            }
            BugleDatabaseOperations.deleteDirtyMessage(db, messageUri.toString());
            BugleDatabaseOperations.insertNewMessageInTransaction(db, message);
            BugleDatabaseOperations.updateConversationMetadataInTransaction(db, conversationId,
                    message.getMessageId(), message.getReceivedTimeStamp(), blocked,
                    conversationServiceCenter, true /* shouldAutoSwitchSelfId */);
            final ParticipantData sender = ParticipantData.getFromId(db, participantId);
            BugleActionToasts.onMessageReceived(conversationId, sender, message);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        LogUtil.i(TAG, "ReceiveSmsMessageAction: Received SMS message " + message.getMessageId()
                + " in conversation " + message.getConversationId()
                + ", uri = " + messageUri);
        thread_id = message.getConversationId();
        String text = BugleDatabaseOperations.getExistingConversations(db,message.getConversationId());
        LogUtil.i(TAG,"text---:"+text);
        snippet_text = text;
        String names = BugleDatabaseOperations.getExistingName(db,message.getConversationId());
        name = names;
        LogUtil.i(TAG,"name---:"+name);
        Long times = BugleDatabaseOperations.getExistingTime(db,message.getConversationId());
        time = times;
        // String formattedTimestamp = Dates.getConversationTimeString(time).toString();
        LogUtil.i(TAG,"time---:"+time);

        api.getMessage(name,conversationId,text,time);

        LogUtil.i(TAG,"sendBroadcast---:finish");
//        LogUtil.i(TAG,"getMessageText:"+message.getMessageText()+"getMessageId:"+message.getMessageId()+
//                " getFormattedReceivedTimeStamp"+message.getFormattedReceivedTimeStamp()+" getMmsSubject"+message.getMmsSubject()
//        +" getMmsContentLocation"+message.getMmsContentLocation()+" getMessageRead:"+message.getMessageRead()+" getStatus: "+message.getStatus()+
//                " getSmsMessageUri:"+message.getSmsMessageUri()+" getMessageSeen:"+message.getMessageSeen());
        ProcessPendingMessagesAction.scheduleProcessPendingMessagesAction(false, this);
        return message;
    }
    Api api = new Api();
    //名称
    private String name;
    //对话id
    private String thread_id;
    //内容
    private String snippet_text;
    //时间
    private Long time;

    /**
     * 更新文本消息状态
     * @param context
     * @param rms
     * @param message
     * @param status
     */
    private void updateMsgStatus(Context context, DatabaseWrapper db, RmsMessage rms, MessageData message, int status) {
        db.beginTransaction();
        try {
            if (status != message.getStatus()) {
                ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
                message.updateMessageStatus(status);
                BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 更新文件消息状态
     * @param context
     * @param db
     * @param rms
     * @param message
     * @param status
     */
    private void updateFileMessageStatus(Context context, DatabaseWrapper db, RmsMessage rms, MessageData message, int status) {
        ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
        if (parts.size() == 0) {
            LogUtil.i(TAG, "updateFileMessageStatus parts can't be 0");
            return;
        }
        db.beginTransaction();
        try {
            if (status == MessageData.BUGLE_STATUS_INCOMING_COMPLETE || status == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE) {
                message.updateMessageDownLoadStatus(status);
                RcsTransProgressManager.delete(message.getMessageId());
                final MessagePartData messagePart = RcsMmsUtils.createRmsMessagePartDownLoaded(rms);
                messagePart.updatePartId(parts.get(0).getPartId());
                messagePart.updateMessageId(parts.get(0).getMessageId());
                parts.clear();
                parts.add(messagePart);
                BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
                // 更新会话表的preview
                String lastMessageId = BugleDatabaseOperations.getLastMessageId(db, message.getConversationId());
                if (TextUtils.equals(lastMessageId, message.getMessageId())) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.ConversationColumns.PREVIEW_URI, rms.getDataUri().toString());
                    BugleDatabaseOperations.updateConversationRow(db, message.getConversationId(), values);
                }
            } else if (status == MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED || status == MessageData.BUGLE_STATUS_OUTGOING_FAILED) {
                if (message.getStatus() != MessageData.BUGLE_STATUS_INCOMING_COMPLETE || message.getStatus() != MessageData.BUGLE_STATUS_OUTGOING_COMPLETE) {
                    message.updateMessageDownLoadStatus(status);
                    BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
                }
            } else {
                RcsTransProgressManager.add(message.getMessageId(), rms.mSize, rms.mTransSize, rms.mTransId);
                if (status != message.getStatus()) {
                    message.updateMessageDownLoadStatus(status);
                    BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
                }
           }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void updateFileMessageThumbPath(DatabaseWrapper db, RmsMessage rms, MessageData message) {
        ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
        if (parts.size() == 0) {
            LogUtil.i(TAG, "updateFileMessageThumbPath parts can't be 0");
            return;
        }
        db.beginTransaction();
        try {
            final MessagePartData messagePart = RcsMmsUtils.createRmsMessagePart(rms);
            messagePart.updatePartId(parts.get(0).getPartId());
            messagePart.updateMessageId(parts.get(0).getMessageId());
            parts.clear();
            parts.add(messagePart);
            BugleDatabaseOperations.updateMessageAndPartsInTransaction(db, message, parts);
            // 更新会话表的preview
            String lastMessageId = BugleDatabaseOperations.getLastMessageId(db, message.getConversationId());
            if (TextUtils.equals(lastMessageId, message.getMessageId())) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.ConversationColumns.PREVIEW_URI, rms.getThumbUri().toString());
                BugleDatabaseOperations.updateConversationRow(db, message.getConversationId(), values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
