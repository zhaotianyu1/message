package com.juphoon.chatbotmaap.chatbotMessages;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.juphoon.chatbotmaap.R;
import com.juphoon.helper.RcsChatbotHelper;

public class RcsChatBotConversationListItemData {
    private String mConversationId;
    private String mName;
    private String mIcon;
    private boolean mIsRead;
    private long mTimestamp;
    private String mSnippetText;
    private Uri mPreviewUri;
    private String mPreviewContentType;
    private long mParticipantContactId;
    private String mParticipantLookupKey;
    private String mOtherParticipantNormalizedDestination;
    private String mSelfId;
    private int mParticipantCount;
    private boolean mNotificationEnabled;
    private String mNotificationSoundUri;
    private boolean mNotificationVibrate;
    private boolean mIncludeEmailAddress;
    private int mMessageStatus;
    private int mMessageRawTelephonyStatus;
    private boolean mShowDraft;
    private Uri mDraftPreviewUri;
    private String mDraftPreviewContentType;
    private String mDraftSnippetText;
    private boolean mIsArchived;
    private String mSubject;
    private String mDraftSubject;
    private String mSnippetSenderFirstName;
    private String mSnippetSenderDisplayDestination;
    private boolean mIsEnterprise;
    // juphoon
    private int mRmsThreadType;
    private long mPriority;
    private long mSmsThreadId;


    private static final int INDEX_ID = 0;
    private static final int INDEX_CONVERSATION_NAME = 1;
    private static final int INDEX_CONVERSATION_ICON = 2;
    private static final int INDEX_SNIPPET_TEXT = 3;
    private static final int INDEX_SORT_TIMESTAMP = 4;
    private static final int INDEX_READ = 5;
    private static final int INDEX_PREVIEW_URI = 6;
    private static final int INDEX_PREVIEW_CONTENT_TYPE = 7;
    private static final int INDEX_PARTICIPANT_CONTACT_ID = 8;
    private static final int INDEX_PARTICIPANT_LOOKUP_KEY = 9;
    private static final int INDEX_OTHER_PARTICIPANT_NORMALIZED_DESTINATION = 10;
    private static final int INDEX_PARTICIPANT_COUNT = 11;
    private static final int INDEX_SELF_ID = 12;
    private static final int INDEX_NOTIFICATION_ENABLED = 13;
    private static final int INDEX_NOTIFICATION_SOUND_URI = 14;
    private static final int INDEX_NOTIFICATION_VIBRATION = 15;
    private static final int INDEX_INCLUDE_EMAIL_ADDRESS = 16;
    private static final int INDEX_MESSAGE_STATUS = 17;
    private static final int INDEX_SHOW_DRAFT = 18;
    private static final int INDEX_DRAFT_PREVIEW_URI = 19;
    private static final int INDEX_DRAFT_PREVIEW_CONTENT_TYPE = 20;
    private static final int INDEX_DRAFT_SNIPPET_TEXT = 21;
    private static final int INDEX_ARCHIVE_STATUS = 22;
    private static final int INDEX_MESSAGE_ID = 23;
    private static final int INDEX_SUBJECT_TEXT = 24;
    private static final int INDEX_DRAFT_SUBJECT_TEXT = 25;
    private static final int INDEX_MESSAGE_RAW_TELEPHONY_STATUS = 26;
    private static final int INDEX_SNIPPET_SENDER_FIRST_NAME = 27;
    private static final int INDEX_SNIPPET_SENDER_DISPLAY_DESTINATION = 28;
    private static final int INDEX_IS_ENTERPRISE = 29;
    private static final int INDEX_RMS_THREAD_TYPE = 30;
    private static final int INDEX_PRIORITY = 31;
    private static final int INDEX_SMS_THREAD_ID = 32;

    private static final String DIVIDER_TEXT = ", ";

    public void bind(final Cursor cursor, final boolean ignoreDraft) {
        mConversationId = cursor.getString(INDEX_ID);
        mName = cursor.getString(INDEX_CONVERSATION_NAME);
        mIcon = cursor.getString(INDEX_CONVERSATION_ICON);
        mSnippetText = cursor.getString(INDEX_SNIPPET_TEXT);
        mTimestamp = cursor.getLong(INDEX_SORT_TIMESTAMP);
        mIsRead = cursor.getInt(INDEX_READ) == 1;
        final String previewUriString = cursor.getString(INDEX_PREVIEW_URI);
        mPreviewUri = TextUtils.isEmpty(previewUriString) ? null : Uri.parse(previewUriString);
        mPreviewContentType = cursor.getString(INDEX_PREVIEW_CONTENT_TYPE);
        mParticipantContactId = cursor.getLong(INDEX_PARTICIPANT_CONTACT_ID);
        mParticipantLookupKey = cursor.getString(INDEX_PARTICIPANT_LOOKUP_KEY);
        mOtherParticipantNormalizedDestination = cursor.getString(
                INDEX_OTHER_PARTICIPANT_NORMALIZED_DESTINATION);
        mSelfId = cursor.getString(INDEX_SELF_ID);
        mParticipantCount = cursor.getInt(INDEX_PARTICIPANT_COUNT);
        mNotificationEnabled = cursor.getInt(INDEX_NOTIFICATION_ENABLED) == 1;
        mNotificationSoundUri = cursor.getString(INDEX_NOTIFICATION_SOUND_URI);
        mNotificationVibrate = cursor.getInt(INDEX_NOTIFICATION_VIBRATION) == 1;
        mIncludeEmailAddress = cursor.getInt(INDEX_INCLUDE_EMAIL_ADDRESS) == 1;
        mMessageStatus = cursor.getInt(INDEX_MESSAGE_STATUS);
        mMessageRawTelephonyStatus = cursor.getInt(INDEX_MESSAGE_RAW_TELEPHONY_STATUS);
        if (!ignoreDraft) {
            mShowDraft = cursor.getInt(INDEX_SHOW_DRAFT) == 1;
            final String draftPreviewUriString = cursor.getString(INDEX_DRAFT_PREVIEW_URI);
            mDraftPreviewUri = TextUtils.isEmpty(draftPreviewUriString) ?
                    null : Uri.parse(draftPreviewUriString);
            mDraftPreviewContentType = cursor.getString(INDEX_DRAFT_PREVIEW_CONTENT_TYPE);
            mDraftSnippetText = cursor.getString(INDEX_DRAFT_SNIPPET_TEXT);
            mDraftSubject = cursor.getString(INDEX_DRAFT_SUBJECT_TEXT);
        } else {
            mShowDraft = false;
            mDraftPreviewUri = null;
            mDraftPreviewContentType = null;
            mDraftSnippetText = null;
            mDraftSubject = null;
        }

        mIsArchived = cursor.getInt(INDEX_ARCHIVE_STATUS) == 1;
        mSubject = cursor.getString(INDEX_SUBJECT_TEXT);
        mSnippetSenderFirstName = cursor.getString(INDEX_SNIPPET_SENDER_FIRST_NAME);
        mSnippetSenderDisplayDestination =
                cursor.getString(INDEX_SNIPPET_SENDER_DISPLAY_DESTINATION);
        mIsEnterprise = cursor.getInt(INDEX_IS_ENTERPRISE) == 1;
        // juphoon
        mRmsThreadType = cursor.getInt(INDEX_RMS_THREAD_TYPE);
        mPriority = cursor.getLong(INDEX_PRIORITY);
        mSmsThreadId = cursor.getLong(INDEX_SMS_THREAD_ID);
    }


    public String getConversationId() {
        return mConversationId;
    }

    public String getName() {
        return mName;
    }

    public String getIcon() {
        String icon = null;
        RcsChatbotHelper.RcsChatbot mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(mOtherParticipantNormalizedDestination);
        if (mChatbotInfo != null) {
            icon = mChatbotInfo.icon;
        }
        return icon;
    }

    public boolean getIsRead() {
        return mIsRead;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getSnippetText() {
        return mSnippetText;
    }

    public Uri getPreviewUri() {
        return mPreviewUri;
    }

    public String getPreviewContentType() {
        return mPreviewContentType;
    }

    public long getParticipantContactId() {
        return mParticipantContactId;
    }

    public boolean isEnterprise() {
        return mIsEnterprise;
    }

    public String getParticipantLookupKey() {
        return mParticipantLookupKey;
    }

    public String getOtherParticipantNormalizedDestination() {
        return mOtherParticipantNormalizedDestination;
    }

    public String getSelfId() {
        return mSelfId;
    }

    public int getParticipantCount() {
        return mParticipantCount;
    }

    public boolean getIsGroup() {
        // Participant count excludes self
        return (mParticipantCount > 1);
    }

    public boolean getIncludeEmailAddress() {
        return mIncludeEmailAddress;
    }

    public boolean getNotificationEnabled() {
        return mNotificationEnabled;
    }

    public String getNotificationSoundUri() {
        return mNotificationSoundUri;
    }

    public boolean getNotifiationVibrate() {
        return mNotificationVibrate;
    }
//
//    public final boolean getIsFailedStatus() {
//        return (mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_FAILED ||
//                mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER ||
//                mMessageStatus == MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED ||
//                mMessageStatus == MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE);
//    }
//
//    public final boolean getIsSendRequested() {
//        return (mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND ||
//                mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY ||
//                mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_SENDING ||
//                mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_RESENDING);
//    }
//
//    public boolean getIsMessageTypeOutgoing() {
//        return !MessageData.getIsIncoming(mMessageStatus);
//    }

    public int getMessageRawTelephonyStatus() {
        return mMessageRawTelephonyStatus;
    }

    public int getMessageStatus() {
        return mMessageStatus;
    }

    public boolean getShowDraft() {
        return mShowDraft;
    }

    public String getDraftSnippetText() {
        return mDraftSnippetText;
    }

    public Uri getDraftPreviewUri() {
        return mDraftPreviewUri;
    }

    public String getDraftPreviewContentType() {
        return mDraftPreviewContentType;
    }

    public boolean getIsArchived() {
        return mIsArchived;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getDraftSubject() {
        return mDraftSubject;
    }

    public String getSnippetSenderName() {
        if (!TextUtils.isEmpty(mSnippetSenderFirstName)) {
            return mSnippetSenderFirstName;
        }
        return mSnippetSenderDisplayDestination;
    }


    public String getSnippetViewText(Context context) {
        Resources resources = context.getResources();
        String snippetText = this.getShowDraft() ?
                resources.getString(R.string.conversation_list_snippet_draf) + this.getDraftSnippetText() : this.getSnippetText();
        final String previewContentType = this.getShowDraft() ?
                this.getDraftPreviewContentType() : this.getPreviewContentType();
        if (TextUtils.isEmpty(snippetText)) {
            // Use the attachment type as a snippet so the preview doesn't look odd
            if (ContentType.isAudioType(previewContentType)) {
                snippetText = "[" + resources.getString(
                        R.string.conversation_list_snippet_audio_clip) + "]";
            } else if (ContentType.isImageType(previewContentType)) {
                snippetText = "[" + resources.getString(R.string.conversation_list_snippet_picture) + "]";
            } else if (ContentType.isVideoType(previewContentType)) {
                snippetText = "[" + resources.getString(R.string.conversation_list_snippet_video) + "]";
            } else if (ContentType.isVCardType(previewContentType)) {
                snippetText = "[" + resources.getString(R.string.conversation_list_snippet_vcard) + "]";
            }
        }
        return snippetText;
    }
}
