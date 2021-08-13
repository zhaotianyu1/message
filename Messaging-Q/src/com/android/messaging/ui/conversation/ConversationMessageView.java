/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.messaging.ui.conversation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.Nullable;

import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.SubscriptionListData.SubscriptionListEntry;
import com.android.messaging.datamodel.media.ImageRequestDescriptor;
import com.android.messaging.datamodel.media.MessagePartImageRequestDescriptor;
import com.android.messaging.datamodel.media.UriImageRequestDescriptor;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.tcl.VideoActivity;
import com.android.messaging.ui.AsyncImageView;
import com.android.messaging.ui.AsyncImageView.AsyncImageViewDelayLoader;
import com.android.messaging.ui.AsyncImageViews;
import com.android.messaging.ui.AudioAttachmentView;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.ui.ConversationDrawables;
import com.android.messaging.ui.GeoAttachmentView;
import com.android.messaging.ui.MultiAttachmentLayout;
import com.android.messaging.ui.MultiAttachmentLayout.OnAttachmentClickListener;
import com.android.messaging.ui.PersonItemView;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.VideoThumbnailView;
import com.android.messaging.util.AccessibilityUtil;
import com.android.messaging.util.Assert;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.ImageUtils;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.YouTubeUtil;
import com.google.common.base.Predicate;
import com.juphoon.chatbotmaap.RcsChatbotCardView;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.RcsChatbotWebActivity;
import com.juphoon.chatbotmaap.tcl.RoundCornerImageView;
import com.juphoon.chatbotmaap.view.RoundImageView;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.RcsMsgItemTouchHelper;
import com.juphoon.helper.mms.RcsTransProgressManager;
import com.juphoon.helper.mms.ui.RcsFileAttachmentView;
import com.juphoon.service.RmsDefine;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * The view for a single entry in a conversation.
 */
public class ConversationMessageView extends FrameLayout implements View.OnClickListener,
        View.OnLongClickListener, OnAttachmentClickListener{


    public interface ConversationMessageViewHost {
        boolean onAttachmentClick(ConversationMessageView view, MessagePartData attachment,
                Rect imageBounds, boolean longPress);
        SubscriptionListEntry getSubscriptionEntryForSelfParticipant(String selfParticipantId,
                boolean excludeDefault);
    }

    private ConversationMessageData mData;
    ConversationMessageView conversationMessageView;
    private LinearLayout mMessageAttachmentsView;
    private MultiAttachmentLayout mMultiAttachmentView;
    private AsyncImageView mMessageImageView;
    private TextView mMessageTextView;
    private boolean mMessageTextHasLinks;
    private boolean mMessageHasYouTubeLink;
    private TextView mStatusTextView;
    private TextView mTitleTextView;
    private TextView mMmsInfoTextView;
    private LinearLayout mMessageTitleLayout;
    private TextView mSenderNameTextView;
    private ContactIconView mContactIconView;
    private ConversationMessageBubbleView mMessageBubble;
    private View mSubjectView;
    private TextView mSubjectLabel;
    private TextView mSubjectText;
    private View mDeliveredBadge;
    private ViewGroup mMessageMetadataView;
    private ViewGroup mMessageTextAndInfoView;
    private TextView mSimNameView;
    private CheckBox mSelectCheckbox;

    private boolean mOneOnOne;
    private ConversationMessageViewHost mHost;

    /**
     * juphoon 修改 begin
     */

    private TextView mNickNameView;
    private String mAddress;
    private int mThreadType;
    private boolean mIsSystemMsg;
    private ImageButton mUploadImageBtn;
    private ImageButton mDownloadImageBtn;
    private ImageButton mImgTransStatus;
    private ImageButton mImgBackground;
    private TextView mImgDownloadProgress;
    private RoundImageView mLoading;
    private TextView mMsgType;
    private TextView mSystemText;
    private boolean mIsChatbotCard;
    private RcsChatbotHelper.RcsChatbot mRcsChatbot;
    private View mDisplayedBadge;

    private FrameLayout attach_fragment;

    /**
     * juphoon 修改 end
     */

    public ConversationMessageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        // TODO: we should switch to using Binding and DataModel factory methods.
        mData = new ConversationMessageData();
    }
    LinearLayout mediaLayout;
    @Override
    protected void onFinishInflate() {
        conversationMessageView = findViewById(R.id.ConversationMessageView);
        attach_fragment = findViewById(R.id.attach_fragment);
//        attach_fragment.setFocusableInTouchMode(false);
//        attach_fragment.setFocusable(false);
        mContactIconView = (ContactIconView) findViewById(R.id.conversation_icon);
//        mContactIconView.setOnLongClickListener(new OnLongClickListener() {
//            @Override
//            public boolean onLongClick(final View view) {
//                //ConversationMessageView.this.performLongClick();
//                return true;
//            }
//        });

        mMessageAttachmentsView = (LinearLayout) findViewById(R.id.message_attachments);
        mMultiAttachmentView = (MultiAttachmentLayout) findViewById(R.id.multiple_attachments);
        mMultiAttachmentView.setOnAttachmentClickListener(this);

        mMessageImageView = (AsyncImageView) findViewById(R.id.message_image);
        mMessageImageView.setOnClickListener(this);
        mMessageImageView.setOnLongClickListener(this);

        mMessageTextView = (TextView) findViewById(R.id.message_text);
        mMessageTextView.setOnClickListener(this);
        IgnoreLinkLongClickHelper.ignoreLinkLongClick(mMessageTextView, this);
		//juphoon
        mSelectCheckbox = (CheckBox) findViewById(R.id.cb_conversation_msg_select);
        mStatusTextView = (TextView) findViewById(R.id.message_status);
        mTitleTextView = (TextView) findViewById(R.id.message_title);
        mMmsInfoTextView = (TextView) findViewById(R.id.mms_info);
        mMessageTitleLayout = (LinearLayout) findViewById(R.id.message_title_layout);
        mSenderNameTextView = (TextView) findViewById(R.id.message_sender_name);
        mMessageBubble = (ConversationMessageBubbleView) findViewById(R.id.message_content);
        mSubjectView = findViewById(R.id.subject_container);
        mSubjectLabel = (TextView) mSubjectView.findViewById(R.id.subject_label);
        mSubjectText = (TextView) mSubjectView.findViewById(R.id.subject_text);
        mDeliveredBadge = findViewById(R.id.smsDeliveredBadge);
        mMessageMetadataView = (ViewGroup) findViewById(R.id.message_metadata);
        mMessageTextAndInfoView = (ViewGroup) findViewById(R.id.message_text_and_info);
        mSimNameView = (TextView) findViewById(R.id.sim_name);
        // juphoon
        mSystemText = (TextView) findViewById(R.id.textSystem);
        mMsgType = (TextView) findViewById(R.id.message_type);
       // mNickNameView = (TextView) findViewById(R.id.message_nickname);
        mUploadImageBtn = (ImageButton) findViewById(R.id.button_upload);
        mDownloadImageBtn = (ImageButton) findViewById(R.id.button_download);
        mImgTransStatus = (ImageButton) findViewById(R.id.img_trans_status);
        mImgBackground = (ImageButton) findViewById(R.id.img_thumbnail_background);
        mImgDownloadProgress = (TextView) findViewById(R.id.tv_download_progerss);
        mLoading = (RoundImageView) findViewById(R.id.img_downloading);
        mDisplayedBadge = findViewById(R.id.rmsDisplayedBadge);
        mUploadImageBtn.setOnClickListener(new OnClickListener() {

            long lastTime = 0;

            @Override
            public void onClick(View arg0) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastTime) / 1000 > 1) {
                    RcsMsgItemTouchHelper.dealFailMessgeTask(mData.getSmsMessageUri(), mData.getMessageId());
                    lastTime = currentTime;
                }
            }
        });
        OnClickListener transClickListener = new OnClickListener() {

            long lastTime = 0;
            @Override
            public void onClick(View arg0) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastTime) / 1000 > 1) {
                    RcsMsgItemTouchHelper.dealFailMessgeTask(mData.getSmsMessageUri(), mData.getMessageId());
                    lastTime = currentTime;
                }
            }
        };

        mediaLayout = findViewById(R.id.message_imgs);

        mediaLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.i("has","hafocu------:"+hasFocus);
                if(hasFocus){
                    ofFloatAnimator(mediaLayout, 1f, 1.02f);//放大
                }else{
                    ofFloatAnimator(mediaLayout, 1.02f, 1f);//缩小
                }
            }
        });
        mediaLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mMessageImageView.performClick();
//                final Object tag = view.getTag();
////                Log.i("nnnn","-----tag----:"+tag.toString());
//                if (tag instanceof MessagePartData) {
//                    final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
//                    onAttachmentClick((MessagePartData) tag, bounds, false /* longPress */);
//                } else if (tag instanceof String) {
//                    // Currently the only object that would make a tag of a string is a youtube preview
//                    // image
//                    UIIntents.get().launchBrowserForUrl(getContext(), (String) tag);
//                }
            }
        });

        mImgBackground.setOnClickListener(transClickListener);
        mDownloadImageBtn.setOnClickListener(transClickListener);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int horizontalSpace = MeasureSpec.getSize(widthMeasureSpec);
        final int iconSize = getResources()
                .getDimensionPixelSize(R.dimen.conversation_message_contact_icon_size);

        final int unspecifiedMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int iconMeasureSpec = MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY);

        mContactIconView.measure(iconMeasureSpec, iconMeasureSpec);

        final int arrowWidth =
                getResources().getDimensionPixelSize(R.dimen.message_bubble_arrow_width);

        // We need to subtract contact icon width twice from the horizontal space to get
        // the max leftover space because we want the message bubble to extend no further than the
        // starting position of the message bubble in the opposite direction.
        /**
         * juphoon 如果是 chatbot 卡片消息则去掉头像空间计算
         */
        final int maxLeftoverSpace = horizontalSpace - ((mData.getIsIncoming() && mIsChatbotCard) ?  0 :  mContactIconView.getMeasuredWidth() * 2 + arrowWidth)
                - getPaddingLeft() - getPaddingRight();
        final int messageContentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxLeftoverSpace,
                MeasureSpec.AT_MOST);
        Log.i("cxz","messageContentWidthMeasureSpec-:"+messageContentWidthMeasureSpec);
        Log.i("cxz","unspecifiedMeasureSpec-:"+unspecifiedMeasureSpec);
        mMessageBubble.measure(messageContentWidthMeasureSpec, unspecifiedMeasureSpec);
        mSystemText.measure(messageContentWidthMeasureSpec, unspecifiedMeasureSpec); //juphoon
        final int maxHeight = Math.max(mContactIconView.getMeasuredHeight(),
                mMessageBubble.getMeasuredHeight());
        //juphoon
        if (mIsSystemMsg) {
            setMeasuredDimension(horizontalSpace, mSystemText.getMeasuredHeight() + getPaddingBottom() + getPaddingTop());
        } else {
            setMeasuredDimension(horizontalSpace, maxHeight + getPaddingBottom() + getPaddingTop());
        }
    }
    private void ofFloatAnimator(View view,float start,float end){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(200);//动画时间
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", start, end);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", start, end);
        animatorSet.setInterpolator(new DecelerateInterpolator());//插值器
        animatorSet.play(scaleX).with(scaleY);//组合动画,同时基于x和y轴放大
        animatorSet.start();
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
        final boolean isRtl = AccessibilityUtil.isLayoutRtl(this);

        /**
         * juphoon chatbot card 头像宽度为 0
         */
        final int iconWidth = mContactIconView.getMeasuredWidth();
        final int iconShowWidth = (mData.getIsIncoming() && mIsChatbotCard) ? 0 : iconWidth; //juphoon
        final int iconHeight = mContactIconView.getMeasuredHeight();
        final int iconTop = getPaddingTop();
        final int contentWidth = (right -left) - ((mData.getIsIncoming() && mIsChatbotCard && mSelectCheckbox.getVisibility() == View.VISIBLE)? iconWidth : 0) - getPaddingLeft() - getPaddingRight();
        final int contentHeight = mMessageBubble.getMeasuredHeight();
        final int contentTop = iconTop;
        final int systemTextWidth = mSystemText.getMeasuredWidth();//juphoon
        final int systemTextHeight = mSystemText.getMeasuredHeight();//juphoon
        final int systemTextTop = iconTop;//juphoon

        final int iconLeft;
        final int contentLeft;
        final int systemTextLeft;//juphoon
        //juphoon
        final int checkboxLeft = (right - left) - getPaddingRight() - iconWidth;
        final int checkboxWidth;
        if (mSelectCheckbox.getVisibility() == View.VISIBLE) {
            checkboxWidth = iconWidth;
        } else {
            checkboxWidth = 0;
        }
        if (mData.getIsIncoming()) {
            if (isRtl) {
                iconLeft = (right - left) - getPaddingRight() - iconShowWidth - checkboxWidth;//juphoon
                contentLeft = iconLeft - contentWidth;
            } else {
                iconLeft = getPaddingLeft();
                contentLeft = iconLeft + iconShowWidth;//juphoon
            }
        } else {
            if (isRtl) {
                iconLeft = getPaddingLeft();
                contentLeft = iconLeft + iconShowWidth;//juphoon
            } else {
                iconLeft = (right - left) - getPaddingRight() - iconShowWidth - checkboxWidth;//juphoon
                contentLeft = iconLeft - contentWidth;
            }
        }

        mContactIconView.layout(iconLeft, iconTop, iconLeft + iconShowWidth, iconTop + iconHeight);//juphoon 会话消息多选不显示头像

        // juphoon 系统消息布局位置
        if (mIsSystemMsg) {
            systemTextLeft = (right - left - systemTextWidth) / 2;
            mSystemText.layout(systemTextLeft - getPaddingLeft(), systemTextTop, systemTextLeft + systemTextWidth + getPaddingLeft(), systemTextTop + systemTextHeight);
        } else {
            mMessageBubble.layout(contentLeft, contentTop, contentLeft + contentWidth, contentTop + contentHeight);
            if (mSelectCheckbox.getVisibility() == View.VISIBLE) {
                mSelectCheckbox.layout(checkboxLeft, iconTop, checkboxLeft + iconWidth, iconTop + iconHeight);
            }
        }
    }

    /**
     * Fills in the data associated with this view.
     *
     * @param cursor The cursor from a MessageList that this view is in, pointing to its entry.
     */
    public void bind(final Cursor cursor) {
        bind(cursor, true, null);
    }

    /**
     * Fills in the data associated with this view.
     *
     * @param cursor The cursor from a MessageList that this view is in, pointing to its entry.
     * @param oneOnOne Whether this is a 1:1 conversation
     */
      public void bind(final Cursor cursor,
                     final boolean oneOnOne, final String selectedMessageId ) {
        mOneOnOne = oneOnOne;

        // Update our UI model
        mData.bind(cursor);
        // mData = conversationListItemDataLists;
        // juphoon
        mAddress = mData.getConversationParticipantNormalizedDestination();
        mThreadType = mData.getConversationRmsThreadType();
        mIsSystemMsg = TextUtils.equals(mData.getSenderNormalizedDestination(), RcsMmsUtils.RMS_SYSTEM_MSG);
        if (mData.getIsSms()) {
            String text = mData.getParts().get(0).getText();
            if (!TextUtils.isEmpty(text) && RcsChatbotUtils.isGeoSms(text)) {
                mData.setProtocol(MessageData.PROTOCOL_RMS);
                mData.getParts().clear();
                mData.getParts().add(MessagePartData.createMediaMessagePart(ContentType.APP_GEO_MESSAGE, Uri.fromFile(new File(RcsChatbotUtils.getGeoFileBySms(text))), 80, 80));
            }
        }
        mIsChatbotCard = false;
        mRcsChatbot = RcsChatbotHelper.getChatbotInfoByServiceId(mAddress);
        // Update text and image content for the view.
        //juphoon
        if (mData.getProtocol() != MessageData.PROTOCOL_RMS) {
            updateViewContent();
        } else {
            if (mData.getParts().size() > 0) {
                mIsChatbotCard = mData.getParts().get(0).isChatbotCard();
            }
            updateRmsViewContent();
        }

        // Update colors and layout parameters for the view.
        updateViewAppearance();

        updateContentDescription();
    }

    public void setHost(final ConversationMessageViewHost host) {
        mHost = host;
    }

    /**
     * Sets a delay loader instance to manage loading / resuming of image attachments.
     */
    public void setImageViewDelayLoader(final AsyncImageViewDelayLoader delayLoader) {
        Assert.notNull(mMessageImageView);
        mMessageImageView.setDelayLoader(delayLoader);
        mMultiAttachmentView.setImageViewDelayLoader(delayLoader);
    }

    public ConversationMessageData getData() {
        return mData;
    }

    /**
     * Returns whether we should show simplified visual style for the message view (i.e. hide the
     * avatar and bubble arrow, reduce padding).
     */
    private boolean shouldShowSimplifiedVisualStyle() {
        return mData.getCanClusterWithPreviousMessage();
    }

    /**
     * Returns whether we need to show message bubble arrow. We don't show arrow if the message
     * contains media attachments or if shouldShowSimplifiedVisualStyle() is true.
     */
    private boolean shouldShowMessageBubbleArrow() {
        return !shouldShowSimplifiedVisualStyle()
                && !(mData.hasAttachments() || mMessageHasYouTubeLink);
    }

    /**
     * Returns whether we need to show a message bubble for text content.
     */
    private boolean shouldShowMessageTextBubble() {
        if (mData.hasText()) {
            return true;
        }
        final String subjectText = MmsUtils.cleanseMmsSubject(getResources(),
                mData.getMmsSubject());
        if (!TextUtils.isEmpty(subjectText)) {
            return true;
        }
        return false;
    }

    private void updateViewContent() {
        updateMessageContent();
        int titleResId = -1;
        int statusResId = -1;
        String statusText = null;
        switch(mData.getStatus()) {
            case MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
                titleResId = R.string.message_title_downloading;
                statusResId = R.string.message_status_downloading;
                break;

            case MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD:
                if (!OsUtil.isSecondaryUser()) {
                    titleResId = R.string.message_title_manual_download;
                    if (isSelected()) {
                        statusResId = R.string.message_status_download_action;
                    } else {
                        statusResId = R.string.message_status_download;
                    }
                }
                break;

            case MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE:
                if (!OsUtil.isSecondaryUser()) {
                    titleResId = R.string.message_title_download_failed;
                    statusResId = R.string.message_status_download_error;
                }
                break;

            case MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED:
                if (!OsUtil.isSecondaryUser()) {
                    titleResId = R.string.message_title_download_failed;
                    if (isSelected()) {
                        statusResId = R.string.message_status_download_action;
                    } else {
                        statusResId = R.string.message_status_download;
                    }
                }
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND:
            case MessageData.BUGLE_STATUS_OUTGOING_SENDING:
                statusResId = R.string.message_status_sending;
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_RESENDING:
            case MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY:
                statusResId = R.string.message_status_send_retrying;
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER:
                statusResId = R.string.message_status_send_failed_emergency_number;
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_FAILED:
                // don't show the error state unless we're the default sms app
                if (PhoneUtils.getDefault().isDefaultSmsApp()) {
                    if (isSelected()) {
                        statusResId = R.string.message_status_resend;
                    } else {
                        statusResId = MmsUtils.mapRawStatusToErrorResourceId(
                                mData.getStatus(), mData.getRawTelephonyStatus());
                    }
                    break;
                }
                // FALL THROUGH HERE

            case MessageData.BUGLE_STATUS_OUTGOING_COMPLETE:
            case MessageData.BUGLE_STATUS_INCOMING_COMPLETE:
            default:
                if (!mData.getCanClusterWithNextMessage()) {
                    statusText = mData.getFormattedReceivedTimeStamp();
                }
                break;
        }

        final boolean titleVisible = (titleResId >= 0);
        if (titleVisible) {
            final String titleText = getResources().getString(titleResId);
            Log.i("lll"," mTitleTextView："+titleText);
            mTitleTextView.setText(titleText);

            final String mmsInfoText = getResources().getString(
                    R.string.mms_info,
                    Formatter.formatFileSize(getContext(), mData.getSmsMessageSize()),
                    DateUtils.formatDateTime(
                            getContext(),
                            mData.getMmsExpiry(),
                            DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_NUMERIC_DATE |
                            DateUtils.FORMAT_NO_YEAR));

            Log.i("lll"," mMessageTitleLayout.setVisibility(View.VISIBLE);");
            mMmsInfoTextView.setText(mmsInfoText);
            mMessageTitleLayout.setVisibility(View.VISIBLE);
        } else {
            mMessageTitleLayout.setVisibility(View.GONE);
        }

        final String subjectText = MmsUtils.cleanseMmsSubject(getResources(),
                mData.getMmsSubject());
        final boolean subjectVisible = !TextUtils.isEmpty(subjectText);

        final boolean senderNameVisible = !mOneOnOne && !mData.getCanClusterWithNextMessage()
                && mData.getIsIncoming();
        if (senderNameVisible) {
            Log.i("qsqs"," mSenderNameTextView--1："+mData.getSenderDisplayName());
            mSenderNameTextView.setText(mData.getSenderDisplayName());
            mSenderNameTextView.setVisibility(View.VISIBLE);
        } else {
            Log.i("qsqs"," mSenderNameTextView--null：");
            mSenderNameTextView.setVisibility(View.GONE);
        }

        if (statusResId >= 0) {
            statusText = getResources().getString(statusResId);
            Log.i("qsqs"," statusText--："+statusText);
        }

        // We set the text even if the view will be GONE for accessibility
        mStatusTextView.setText(statusText);
        final boolean statusVisible = !TextUtils.isEmpty(statusText);
        Log.i("qsqs"," statusVisible--："+statusVisible);
        if (statusVisible) {
            mStatusTextView.setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setVisibility(View.GONE);
        }

        final boolean deliveredBadgeVisible =
                mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED;
        mDeliveredBadge.setVisibility(deliveredBadgeVisible ? View.VISIBLE : View.GONE);

        // Update the sim indicator.
        final boolean showSimIconAsIncoming = mData.getIsIncoming() &&
                (!mData.hasAttachments() || shouldShowMessageTextBubble());
        final SubscriptionListEntry subscriptionEntry =
                mHost.getSubscriptionEntryForSelfParticipant(mData.getSelfParticipantId(),
                        true /* excludeDefault */);
        final boolean simNameVisible = subscriptionEntry != null &&
                !TextUtils.isEmpty(subscriptionEntry.displayName) &&
                !mData.getCanClusterWithNextMessage();
        if (simNameVisible) {
            final String simNameText = mData.getIsIncoming() ? getResources().getString(
                    R.string.incoming_sim_name_text, subscriptionEntry.displayName) :
                        subscriptionEntry.displayName;
            mSimNameView.setText(simNameText);
            mSimNameView.setTextColor(showSimIconAsIncoming ? getResources().getColor(
                    R.color.timestamp_text_incoming) : subscriptionEntry.displayColor);
            mSimNameView.setVisibility(VISIBLE);
        } else {
            mSimNameView.setText(null);
            mSimNameView.setVisibility(GONE);
        }

        final boolean metadataVisible = senderNameVisible || statusVisible
                || deliveredBadgeVisible || simNameVisible;
        mMessageMetadataView.setVisibility(metadataVisible ? View.VISIBLE : View.GONE);

        final boolean messageTextAndOrInfoVisible = titleVisible || subjectVisible
                || mData.hasText() || metadataVisible;
        Log.i("rere","messageTextAndOrInfoVisible:"+messageTextAndOrInfoVisible);
        mMessageTextAndInfoView.setVisibility(
                messageTextAndOrInfoVisible ? View.VISIBLE : View.GONE);

        if (shouldShowSimplifiedVisualStyle()) {
            Log.i("lll","---------================10");
            mContactIconView.setVisibility(View.GONE);
            mContactIconView.setImageResourceUri(null);
        } else if (mRcsChatbot != null && mData.getIsIncoming()) {
            /**
             * Chatbot 会话里有短信需要自己设置头像 juphoon
             */
            mContactIconView.setVisibility(GONE);
//            mContactIconView.setVisibility(View.VISIBLE);
//            mContactIconView.setImageResourceUri(getChatbotLogoUri(), -1, null, mAddress);
        } else if (!mData.getIsIncoming()) {
            Log.i("lll","---------================6");
            mContactIconView.setVisibility(View.VISIBLE);
            Uri avatarUri = AvatarUriUtil.createAvatarUri(
                    null, null, null, null);
            mContactIconView.setImageResourceUri(avatarUri);
        } else {
            Log.i("lll","---------================7");
            mContactIconView.setVisibility(View.GONE);
            final Uri avatarUri = AvatarUriUtil.createAvatarUri(
                    mData.getSenderProfilePhotoUri(),
                    mData.getSenderFullName(),
                    mData.getSenderNormalizedDestination(),
                    mData.getSenderContactLookupKey());
            mContactIconView.setImageResourceUri(avatarUri, mData.getSenderContactId(),
                    mData.getSenderContactLookupKey(), mData.getSenderNormalizedDestination());
        }

        //juphoon
        if (mData.getProtocol() == MessageData.PROTOCOL_SMS) {
            mMsgType.setText(R.string.msg_type_sms);
        } else {
            mMsgType.setText(R.string.msg_type_mms);
        }
    }

    private void updateMessageContent() {
        // We must update the text before the attachments since we search the text to see if we
        // should make a preview youtube image in the attachments
        updateMessageText();
        updateMessageAttachments();
        updateMessageSubject();
        mMessageBubble.bind(mData);
    }

    private void updateMessageAttachments() {
        // Bind video, audio, and VCard attachments. If there are multiple, they stack vertically.
        bindAttachmentsOfSameType(sVideoFilter,
                R.layout.message_video_attachment, mVideoViewBinder, VideoThumbnailView.class);
        bindAttachmentsOfSameType(sAudioFilter,
                R.layout.message_audio_attachment, mAudioViewBinder, AudioAttachmentView.class);
        bindAttachmentsOfSameType(sVCardFilter,
                R.layout.message_vcard_attachment, mVCardViewBinder, PersonItemView.class);
        // juphoon 地理位置
        bindAttachmentsOfSameType(sGeoFilter,
                R.layout.message_geo_attachment, mGeoViewBinder, GeoAttachmentView.class);
        //  juphoon文件消息
        bindAttachmentsOfSameType(sFileFilter,
                R.layout.message_file_attachment, mFileViewBinder, RcsFileAttachmentView.class);
       
        // juphoon chatbot消息
        bindAttachmentsOfSameType(sChatbotCardFilter,
                R.layout.message_chatbot_attachment, mChatbotViewBinder, RcsChatbotCardView.class);

        // Bind image attachments. If there are multiple, they are shown in a collage view.
        final List<MessagePartData> imageParts = mData.getAttachments(sImageFilter);
        Log.i("vvvv","initListCard--imageParts--:"+imageParts.size());
        if (imageParts.size() > 1) {
            Log.i("vvvv","initListCard--imageParts--1");
            Collections.sort(imageParts, sImageComparator);
            mMultiAttachmentView.bindAttachments(imageParts, null, imageParts.size());
            mMultiAttachmentView.setVisibility(View.VISIBLE);
        } else {
            Log.i("vvvv","initListCard--imageParts--2");
            mMultiAttachmentView.setVisibility(View.GONE);
        }

        // In the case that we have no image attachments and exactly one youtube link in a message
        // then we will show a preview.
        String youtubeThumbnailUrl = null;
        String originalYoutubeLink = null;
        if (mMessageTextHasLinks && imageParts.size() == 0) {
            Log.i("vvvv","initListCard--imageParts--3");
            CharSequence messageTextWithSpans = mMessageTextView.getText();
            final URLSpan[] spans = ((Spanned) messageTextWithSpans).getSpans(0,
                    messageTextWithSpans.length(), URLSpan.class);
            for (URLSpan span : spans) {
                String url = span.getURL();
                String youtubeLinkForUrl = YouTubeUtil.getYoutubePreviewImageLink(url);
                if (!TextUtils.isEmpty(youtubeLinkForUrl)) {
                    if (TextUtils.isEmpty(youtubeThumbnailUrl)) {
                        // Save the youtube link if we don't already have one
                        youtubeThumbnailUrl = youtubeLinkForUrl;
                        originalYoutubeLink = url;
                    } else {
                        // We already have a youtube link. This means we have two youtube links so
                        // we shall show none.
                        youtubeThumbnailUrl = null;
                        originalYoutubeLink = null;
                        break;
                    }
                }
            }
        }
        // We need to keep track if we have a youtube link in the message so that we will not show
        // the arrow
        mMessageHasYouTubeLink = !TextUtils.isEmpty(youtubeThumbnailUrl);

        // We will show the message image view if there is one attachment or one youtube link
        if (imageParts.size() == 1 || mMessageHasYouTubeLink) {
            // Get the display metrics for a hint for how large to pull the image data into
            final WindowManager windowManager = (WindowManager) getContext().
                    getSystemService(Context.WINDOW_SERVICE);
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);

            final int iconSize = getResources()
                    .getDimensionPixelSize(R.dimen.conversation_message_contact_icon_size);
            final int desiredWidth = displayMetrics.widthPixels - iconSize - iconSize;

            if (imageParts.size() == 1) {
                final MessagePartData imagePart = imageParts.get(0);
                // If the image is big, we want to scale it down to save memory since we're going to
                // scale it down to fit into the bubble width. We don't constrain the height.
                final ImageRequestDescriptor imageRequest =
                        new MessagePartImageRequestDescriptor(imagePart,
                                desiredWidth,
                                MessagePartData.UNSPECIFIED_SIZE,
                                false);
                /** juphoon 公众帐号图片消息显示下载 chatbot同理 **/
                if (mData.getIsRms() && imagePart.getContentUri().toString().startsWith("http")) {
                    Log.i("vvvv","initListCard--imageParts--4");
                    RcsMsgItemTouchHelper.downloadPublicThumbToUpdate(Long.parseLong(mData.getMessageId()), imagePart.getContentUri().toString());
                    adjustImageViewBounds(imagePart);
                    mMessageImageView.setTag(imagePart);
                    mMessageImageView.setImageResource(R.drawable.ic_image_dark);
                    return;
                } else {
                    Log.i("vvvv","initListCard--imageParts--5");
                    adjustImageViewBounds(imagePart);
                    mMessageImageView.setImageResourceId(imageRequest);
                    mMessageImageView.setTag(imagePart);
                }
            } else {
                // Youtube Thumbnail image
                final ImageRequestDescriptor imageRequest =
                        new UriImageRequestDescriptor(Uri.parse(youtubeThumbnailUrl), desiredWidth,
                            MessagePartData.UNSPECIFIED_SIZE, true /* allowCompression */,
                            true /* isStatic */, false /* cropToCircle */,
                            ImageUtils.DEFAULT_CIRCLE_BACKGROUND_COLOR /* circleBackgroundColor */,
                            ImageUtils.DEFAULT_CIRCLE_STROKE_COLOR /* circleStrokeColor */);
                mMessageImageView.setImageResourceId(imageRequest);
                mMessageImageView.setTag(originalYoutubeLink);
            }
            Log.i("vvvv","initListCard--imageParts--6");
            mMessageImageView.setVisibility(View.VISIBLE);
            mediaLayout.setVisibility(VISIBLE);
            BugleApplication.getInstance().setPicture(true);
            mMessageImageView.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent event) {
                    Log.i("vvvv","initListCard--onKey");
                    if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                        final Object tag = view.getTag();
                        Log.i("nnnn","-----tag----:"+tag.toString());
                        if (tag instanceof MessagePartData) {
                            final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
                            onAttachmentClick((MessagePartData) tag, bounds, false /* longPress */);
                        } else if (tag instanceof String) {
                            // Currently the only object that would make a tag of a string is a youtube preview
                            // image
                            UIIntents.get().launchBrowserForUrl(getContext(), (String) tag);
                        }
                    }
                    return false;
                }
            });
        } else {
            Log.i("vvvv","initListCard--imageParts--7");
            mMessageImageView.setImageResourceId(null);
            mMessageImageView.setVisibility(View.GONE);
            mediaLayout.setVisibility(View.GONE);
        }

        // Show the message attachments container if any of its children are visible
        boolean attachmentsVisible = false;
        for (int i = 0, size = mMessageAttachmentsView.getChildCount(); i < size; i++) {
            final View attachmentView = mMessageAttachmentsView.getChildAt(i);
            attachmentView.setFocusable(true);
            attachmentView.setFocusableInTouchMode(true);
            if (attachmentView.getVisibility() == View.VISIBLE) {
                attachmentsVisible = true;
                break;
            }
        }
        Log.i("vvvv","initListCard--attachmentsVisible--:"+attachmentsVisible);
        mMessageAttachmentsView.setVisibility(attachmentsVisible ? View.VISIBLE : View.GONE);
        mMessageAttachmentsView.setFocusable(true);
        mMessageAttachmentsView.setFocusableInTouchMode(true);
    }

    private void bindAttachmentsOfSameType(final Predicate<MessagePartData> attachmentTypeFilter,
            final int attachmentViewLayoutRes, final AttachmentViewBinder viewBinder,
            final Class<?> attachmentViewClass) {
        final LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        // Iterate through all attachments of a particular type (video, audio, etc).
        // Find the first attachment index that matches the given type if possible.
        int attachmentViewIndex = -1;
        View existingAttachmentView;
        do {
            existingAttachmentView = mMessageAttachmentsView.getChildAt(++attachmentViewIndex);

        } while (existingAttachmentView != null &&
                !(attachmentViewClass.isInstance(existingAttachmentView)));

        for (final MessagePartData attachment : mData.getAttachments(attachmentTypeFilter)) {
            View attachmentView = mMessageAttachmentsView.getChildAt(attachmentViewIndex);
//            if(attachmentViewLayoutRes == R.layout.message_video_attachment){
//                FrameLayout video_attachment_background = (FrameLayout) layoutInflater.inflate(attachmentViewLayoutRes,
//                        mMessageAttachmentsView, false /* attachToRoot */);
//                video_attachment_background.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//                    @Override
//                    public void onFocusChange(View v, boolean hasFocus) {
//                        if(hasFocus){
//                            ofFloatAnimator(video_attachment_background, 1f, 1.02f);//放大
//                        }else{
//                            ofFloatAnimator(video_attachment_background, 1.02f, 1f);//放大
//                        }
//                    }
//                });
//                video_attachment_background.setOnClickListener(this);
//                video_attachment_background.setOnLongClickListener(this);
//                video_attachment_background.setFocusable(true);
//                video_attachment_background.setFocusableInTouchMode(true);
//                mMessageAttachmentsView.addView(video_attachment_background, attachmentViewIndex);
//            }else
                if (!attachmentViewClass.isInstance(attachmentView)) {
                attachmentView = layoutInflater.inflate(attachmentViewLayoutRes,
                        mMessageAttachmentsView, false /* attachToRoot */);
                attachmentView.setOnClickListener(this);
                attachmentView.setOnLongClickListener(this);
                attachmentView.setFocusable(true);
                attachmentView.setFocusableInTouchMode(true);
                if(attachmentViewLayoutRes == R.layout.message_video_attachment) {
                    View finalAttachmentView = attachmentView;
                    attachmentView.setOnFocusChangeListener(new OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            Log.i("dxc","hascoucue--:"+hasFocus);
                            if(hasFocus){
                                ofFloatAnimator(finalAttachmentView, 1f, 1.02f);//放大
                            }else{
                                ofFloatAnimator(finalAttachmentView, 1.02f, 1f);//放大
                            }
                        }
                    });
                    finalAttachmentView.setOnClickListener(VideoThumbnailView.mTransEventClickListener);
//                    attachmentView.setOnClickListener(new OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            Log.i("dxc","onClick--======:");
//                            finalAttachmentView.setOnClickListener(VideoThumbnailView.mTransEventClickListener);
//                        }
//                    });
                }
                mMessageAttachmentsView.addView(attachmentView, attachmentViewIndex);
            }
            viewBinder.bindView(attachmentView, attachment);
            attachmentView.setTag(attachment);
            attachmentView.setVisibility(View.VISIBLE);
            attachmentViewIndex++;
        }
        // If there are unused views left over, unbind or remove them.
        while (attachmentViewIndex < mMessageAttachmentsView.getChildCount()) {
            final View attachmentView = mMessageAttachmentsView.getChildAt(attachmentViewIndex);
            if (attachmentViewClass.isInstance(attachmentView)) {
                mMessageAttachmentsView.removeViewAt(attachmentViewIndex);
            } else {
                // No more views of this type; we're done.
                break;
            }
        }
    }

    private void updateMessageSubject() {
        final String subjectText = MmsUtils.cleanseMmsSubject(getResources(),
                mData.getMmsSubject());
        final boolean subjectVisible = !TextUtils.isEmpty(subjectText);

        if (subjectVisible) {
            Log.i("ppp","updateMessageSubject--------:1");
            mSubjectText.setText(subjectText);
            mSubjectView.setVisibility(View.VISIBLE);
        } else {
            Log.i("ppp","updateMessageSubject--------:2");
            mSubjectView.setVisibility(View.GONE);
        }
    }

    private void updateMessageText() {
        final String text = mData.getText();
        Log.i("ppp","text--------:"+text);
        mMessageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        //juphoon
        if (mData.getProtocol() == MessageData.PROTOCOL_SMS && mData.getIsIncoming()) {
            if (!TextUtils.isEmpty(text) && !mIsSystemMsg) { //阅后即焚不显示文本
                Log.i("ppp","text--------:1");
                mMessageTextView.setVisibility(View.VISIBLE);
                mMessageTextView.setText(text);
                mMessageTextHasLinks = Linkify.addLinks(mMessageTextView, Linkify.ALL);
                interceptHyperLink(mMessageTextView);
            } else {
                Log.i("ppp","text--------:2");
                mMessageTextView.setVisibility(View.GONE);
                mMessageTextHasLinks = false;
            }
        } else {
            if (!TextUtils.isEmpty(text)) { //阅后即焚不显示文本
                Log.i("ppp","text--------:3");
                mMessageTextView.setVisibility(View.VISIBLE);
                mMessageTextView.setText(text);
                mSystemText.setText(text);
                // Linkify phone numbers, web urls, emails, and map addresses to allow users to
                // click on them and take the default intent.
                mMessageTextHasLinks = Linkify.addLinks(mMessageTextView, Linkify.ALL);
                interceptHyperLink(mMessageTextView);
                // juphoon 系统消息的数字不能link
            } else {
                Log.i("ppp","text--------:4");
                mMessageTextView.setVisibility(View.GONE);
                mSystemText.setVisibility(GONE);
                mMessageTextHasLinks = false;
            }
        }
    }

    //juphoon
    private void interceptHyperLink(TextView tv) {
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        CharSequence text = tv.getText();
        if (text instanceof Spannable) {
            int end = text.length();
            Spannable spannable = (Spannable) tv.getText();
            URLSpan[] urlSpans = spannable.getSpans(0, end, URLSpan.class);
            if (urlSpans.length == 0) {
                return;
            }
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
            for (URLSpan uri : urlSpans) {
                String url = uri.getURL();
                if (url.indexOf("http://") == 0 || url.indexOf("https://") == 0) {
                    CustomUrlSpan customUrlSpan = new CustomUrlSpan(getContext(), url);
                    spannableStringBuilder.setSpan(customUrlSpan, spannable.getSpanStart(uri),
                            spannable.getSpanEnd(uri), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
            tv.setText(spannableStringBuilder);
        }
    }
    Drawable textBackgrounds;
    private void updateViewAppearance() {
        final Resources res = getResources();
        final ConversationDrawables drawableProvider = ConversationDrawables.get();
        final boolean incoming = mData.getIsIncoming();
        final boolean outgoing = !incoming;
        final boolean showArrow =  shouldShowMessageBubbleArrow();

        final int messageTopPaddingClustered =
                res.getDimensionPixelSize(R.dimen.message_padding_same_author);
        final int messageTopPaddingDefault =
                res.getDimensionPixelSize(R.dimen.message_padding_default);
        final int arrowWidth = res.getDimensionPixelOffset(R.dimen.message_bubble_arrow_width);
        final int messageTextMinHeightDefault = res.getDimensionPixelSize(
                R.dimen.conversation_message_contact_icon_size);
        final int messageTextLeftRightPadding = res.getDimensionPixelOffset(
                R.dimen.message_text_left_right_padding);
        final int textTopPaddingDefault = res.getDimensionPixelOffset(
                R.dimen.message_text_top_padding);
        final int textBottomPaddingDefault = res.getDimensionPixelOffset(
                R.dimen.message_text_bottom_padding);

        // These values depend on whether the message has text, attachments, or both.
        // We intentionally don't set defaults, so the compiler will tell us if we forget
        // to set one of them, or if we set one more than once.
        final int contentLeftPadding, contentRightPadding;
        final Drawable textBackground;
        final int textMinHeight;
        final int textTopMargin;
        final int textTopPadding, textBottomPadding;
        final int textLeftPadding, textRightPadding;

        if (mData.hasAttachments()) {
            if (shouldShowMessageTextBubble()) {
                // Text and attachment(s)
                contentLeftPadding = incoming ? arrowWidth : 0;
                contentRightPadding = outgoing ? arrowWidth : 0;
                Log.i("rere","shouldShowMessageTextBubble");
                //juphoon: 增加根据会话类型显示不同颜色
                textBackground = drawableProvider.getBubbleDrawable(
                        isSelected(),
                        incoming,
                        false /* needArrow */,
                        mData.hasIncomingErrorStatus(),
                        mThreadType,
                        mRcsChatbot);
                textMinHeight = messageTextMinHeightDefault;
                textTopMargin = messageTopPaddingClustered;
                textTopPadding = textTopPaddingDefault;
                textBottomPadding = textBottomPaddingDefault;
                textLeftPadding = messageTextLeftRightPadding;
                textRightPadding = messageTextLeftRightPadding;
            } else if(!isImage && !isChatbotss){
                Log.i("rere","Attachment(s) only");
                // Attachment(s) only
               // contentLeftPadding = incoming ? arrowWidth : 0;
                contentLeftPadding = 0;
                contentRightPadding = outgoing ? arrowWidth : 0;
                textBackground = null;
                textMinHeight = 0;
                textTopMargin = 0;
                textTopPadding = 0;
                textBottomPadding = 0;
                textLeftPadding = 0;
                textRightPadding = 0;

                mMessageTextAndInfoView.setVisibility(GONE);
            }else{
                Log.i("rere","Attachment(s) only---image");
                // Attachment(s) only
                // contentLeftPadding = incoming ? arrowWidth : 0;
                contentLeftPadding = 65;
                contentRightPadding = outgoing ? arrowWidth : 0;
                textBackground = null;
                textMinHeight = 0;
                textTopMargin = 0;
                textTopPadding = 0;
                textBottomPadding = 0;
                textLeftPadding = 0;
                textRightPadding = 0;
                mMessageTextAndInfoView.setVisibility(GONE);
                isImage = false;
                isChatbotss = false;
            }
        } else {
            Log.i("rere","Text only");
            // Text only
            //contentLeftPadding = (!showArrow && incoming) ? arrowWidth : 0;
            contentLeftPadding = 0;
            contentRightPadding = 13;

            // juphoon 设置系统消息背景
            Drawable systemDrawable = getResources().getDrawable(R.drawable.btn_keyboard_key_light_normal_holo);
            mSystemText.setBackgroundDrawable(ImageUtils.getTintedDrawable(getContext(), systemDrawable,
                    getResources().getColor(R.color.system_background)));
            Log.i("lll","shouldShowMessageTextBubble-------sssss:"+shouldShowMessageBubbleArrow());
            //juphoon: 增加根据会话类型显示不同颜色
            textBackground = drawableProvider.getBubbleDrawable(
                    isSelected(),
                    incoming,
                   // shouldShowMessageBubbleArrow(),
                    true,
                    mData.hasIncomingErrorStatus(),
                    mThreadType,
                    mRcsChatbot);
            textMinHeight = messageTextMinHeightDefault;
            textTopMargin = 0;
            textTopPadding = 15;
            textBottomPadding = 15;
//            textTopPadding = textTopPaddingDefault;
//            textBottomPadding = textBottomPaddingDefault;
            if (showArrow && incoming) {
                Log.i("rere","Text only---incoming");
               // textLeftPadding = messageTextLeftRightPadding + arrowWidth;
//                textLeftPadding = messageTextLeftRightPadding;
                textLeftPadding = 22;
            } else {
                Log.i("rere","Text only---not incoming");
//                textLeftPadding = messageTextLeftRightPadding;
                  textLeftPadding = 22;
            }
            if (showArrow && outgoing) {
                Log.i("rere","Text only--- outgoing");
               // textRightPadding = messageTextLeftRightPadding + arrowWidth;
//                textRightPadding = messageTextLeftRightPadding;
                textRightPadding = 12;
            } else {
                Log.i("rere","Text only---not outgoing");
//                textRightPadding = messageTextLeftRightPadding;
                textRightPadding = 12;
            }
        }

        // These values do not depend on whether the message includes attachments
        // juphoon 设置系统消息位置
        final int gravity = mIsSystemMsg ? Gravity.CENTER
                : incoming ? (Gravity.START | Gravity.CENTER_VERTICAL) :
                                (Gravity.END | Gravity.CENTER_VERTICAL);
        final int messageTopPadding = shouldShowSimplifiedVisualStyle() ?
                messageTopPaddingClustered : messageTopPaddingDefault;
        final int metadataTopPadding = res.getDimensionPixelOffset(
                R.dimen.message_metadata_top_padding);
        textBackgrounds = textBackground;

        mMessageTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    Log.i("mmm","text---------------触摸");
                    Drawable textBackgroundss;
                    textBackgroundss = drawableProvider.getBubbleDrawables(
                            isSelected(),
                            incoming,
                            // shouldShowMessageBubbleArrow(),
                            true,
                            mData.hasIncomingErrorStatus(),
                            mThreadType,
                            mRcsChatbot);
                    ofFloatAnimator(mMessageTextAndInfoView, 1f, 1.1f);//放大
                    ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackgroundss);

                }else{
                    ofFloatAnimator(mMessageTextAndInfoView, 1.1f, 1f);//放大
                    ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackgrounds);
                    Log.i("mmm","text---------------放弃");
                }
            }
        });
        mMessageTextAndInfoView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    Log.i("mmm","---------------触摸");
                    Drawable textBackgroundss;
                    textBackgroundss = drawableProvider.getBubbleDrawables(
                            isSelected(),
                            incoming,
                            // shouldShowMessageBubbleArrow(),
                            true,
                            mData.hasIncomingErrorStatus(),
                            mThreadType,
                            mRcsChatbot);
                    ofFloatAnimator(mMessageTextAndInfoView, 1f, 1.1f);//放大
                    ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackgroundss);

                }else{
                    ofFloatAnimator(mMessageTextAndInfoView, 1.1f, 1f);//放大
                    ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackgrounds);
                    Log.i("mmm","---------------放弃");
                }
            }
        });
        attach_fragment.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    Log.i("mmm","---------------触摸");
                    Drawable textBackgroundss;
                    textBackgroundss = drawableProvider.getBubbleDrawables(
                            isSelected(),
                            incoming,
                            // shouldShowMessageBubbleArrow(),
                            true,
                            mData.hasIncomingErrorStatus(),
                            mThreadType,
                            mRcsChatbot);
                    ofFloatAnimator(mMessageTextAndInfoView, 1f, 1.1f);//放大
                    ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackgroundss);
                    mMessageTextAndInfoView.requestFocus();

                }else{
                    ofFloatAnimator(mMessageTextAndInfoView, 1.1f, 1f);//放大
                    ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackgrounds);
                    Log.i("mmm","---------------放弃");
                }
            }
        });
        // Update the message text/info views
        ImageUtils.setBackgroundDrawableOnView(mMessageTextAndInfoView, textBackground);
        mMessageTextAndInfoView.setMinimumHeight(textMinHeight);
        final LinearLayout.LayoutParams textAndInfoLayoutParams =
                (LinearLayout.LayoutParams) mMessageTextAndInfoView.getLayoutParams();
        textAndInfoLayoutParams.topMargin = textTopMargin;

        Log.i("rere","contentLeftPadding-:"+contentLeftPadding);
        Log.i("rere","contentRightPadding-:"+contentRightPadding);

        Log.i("rere","textRightPadding-:"+textRightPadding);
        Log.i("rere","textLeftPadding-:"+textLeftPadding);
        Log.i("rere","textBottomPadding-:"+textBottomPadding);
        Log.i("rere","textTopPadding-:"+textTopPadding);
        if (UiUtils.isRtlMode()) {
            Log.i("rere","isRtlMode----123");
            // Need to switch right and left padding in RtL mode
            mMessageTextAndInfoView.setPadding(textRightPadding, textTopPadding, textLeftPadding,
                    textBottomPadding);
            mMessageBubble.setPadding(contentRightPadding, 0, contentLeftPadding, 0);
        } else {
            Log.i("rere","isRtlMode----456");
            mMessageTextAndInfoView.setPadding(textLeftPadding, textTopPadding, textRightPadding,
                    textBottomPadding);
            mMessageBubble.setPadding(contentLeftPadding, 0, contentRightPadding, 0);
        }

        // Update the message row and message bubble views
        setPadding(getPaddingLeft(), messageTopPadding, getPaddingRight(), 0);
        mMessageBubble.setGravity(gravity);
        updateMessageAttachmentsAppearance(gravity);
        Log.i("rere","metadataTopPadding-:"+metadataTopPadding);
        mMessageMetadataView.setPadding(0, metadataTopPadding, 0, 0);

        updateTextAppearance();
		//juphoon
        if (mData.getProtocol() == MessageData.PROTOCOL_RMS) {
            if (incoming) {
                mMessageTextView.setTextColor(getResources().getColor(R.color.message_text_color_incoming));
            } else {
                mMessageTextView.setTextColor(getResources().getColor(R.color.message_text_color_incoming));
            }
            mStatusTextView.setTextColor(getResources().getColor(R.color.message_text_color_incoming));
        }
        requestLayout();
    }

    private void updateContentDescription() {
        StringBuilder description = new StringBuilder();

        Resources res = getResources();
        String separator = res.getString(R.string.enumeration_comma);

        // Sender information
        boolean hasPlainTextMessage = !(TextUtils.isEmpty(mData.getText()) ||
                mMessageTextHasLinks);
        if (mData.getIsIncoming()) {
            int senderResId = hasPlainTextMessage
                ? R.string.incoming_text_sender_content_description
                : R.string.incoming_sender_content_description;
            description.append(res.getString(senderResId, mData.getSenderDisplayName()));
        } else {
            int senderResId = hasPlainTextMessage
                ? R.string.outgoing_text_sender_content_description
                : R.string.outgoing_sender_content_description;
            description.append(res.getString(senderResId));
        }

        if (mSubjectView.getVisibility() == View.VISIBLE) {
            description.append(separator);
            description.append(mSubjectText.getText());
        }

        if (mMessageTextView.getVisibility() == View.VISIBLE) {
            // If the message has hyperlinks, we will let the user navigate to the text message so
            // that the hyperlink can be clicked. Otherwise, the text message does not need to
            // be reachable.
            if (mMessageTextHasLinks) {
                mMessageTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            } else {
                mMessageTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                description.append(separator);
                description.append(mMessageTextView.getText());
            }
        }

        if (mMessageTitleLayout.getVisibility() == View.VISIBLE) {
            description.append(separator);
            description.append(mTitleTextView.getText());

            description.append(separator);
            description.append(mMmsInfoTextView.getText());
        }

        if (mStatusTextView.getVisibility() == View.VISIBLE) {
            description.append(separator);
            description.append(mStatusTextView.getText());
        }

        if (mSimNameView.getVisibility() == View.VISIBLE) {
            description.append(separator);
            description.append(mSimNameView.getText());
        }

        if (mDeliveredBadge.getVisibility() == View.VISIBLE) {
            description.append(separator);
            description.append(res.getString(R.string.delivered_status_content_description));
        }

        if (mDisplayedBadge.getVisibility() == View.VISIBLE) {
            description.append(separator);
            description.append(res.getString(R.string.displayed_status));
        }

        setContentDescription(description);
    }

    private void updateMessageAttachmentsAppearance(final int gravity) {
        mMessageAttachmentsView.setGravity(gravity);

        // Tint image/video attachments when selected
        final int selectedImageTint = getResources().getColor(R.color.message_image_selected_tint);
        if (mMessageImageView.getVisibility() == View.VISIBLE) {
            if (isSelected()) {
                mMessageImageView.setColorFilter(selectedImageTint);
            } else {
                mMessageImageView.clearColorFilter();
            }
        }
        if (mMultiAttachmentView.getVisibility() == View.VISIBLE) {
            if (isSelected()) {
                mMultiAttachmentView.setColorFilter(selectedImageTint);
            } else {
                mMultiAttachmentView.clearColorFilter();
            }
        }
        for (int i = 0, size = mMessageAttachmentsView.getChildCount(); i < size; i++) {
            final View attachmentView = mMessageAttachmentsView.getChildAt(i);
            if (attachmentView instanceof VideoThumbnailView
                    && attachmentView.getVisibility() == View.VISIBLE) {
                final VideoThumbnailView videoView = (VideoThumbnailView) attachmentView;
                if (isSelected()) {
                    videoView.setColorFilter(selectedImageTint);
                } else {
                    videoView.clearColorFilter();
                }
            }
        }

        // If there are multiple attachment bubbles in a single message, add some separation.
        final int multipleAttachmentPadding =
                getResources().getDimensionPixelSize(R.dimen.message_padding_same_author);

        boolean previousVisibleView = false;
        for (int i = 0, size = mMessageAttachmentsView.getChildCount(); i < size; i++) {
            final View attachmentView = mMessageAttachmentsView.getChildAt(i);
            if (attachmentView.getVisibility() == View.VISIBLE) {
                final int margin = previousVisibleView ? multipleAttachmentPadding : 0;
                ((LinearLayout.LayoutParams) attachmentView.getLayoutParams()).topMargin = margin;
                // updateViewAppearance calls requestLayout() at the end, so we don't need to here
                previousVisibleView = true;
            }
        }
    }

    private void updateTextAppearance() {
        int messageColorResId;
        int statusColorResId = -1;
        int infoColorResId = -1;
        int timestampColorResId;
        int subjectLabelColorResId;
        if (isSelected()) {
            messageColorResId = R.color.message_text_color_incoming;
            statusColorResId = R.color.message_action_status_text;
            //infoColorResId = R.color.message_action_info_text;
            infoColorResId = R.color.white;
            if (shouldShowMessageTextBubble()) {
                timestampColorResId = R.color.message_action_timestamp_text;
                subjectLabelColorResId = R.color.message_action_timestamp_text;
            } else {
                // If there's no text, the timestamp will be shown below the attachments,
                // against the conversation view background.
                timestampColorResId = R.color.timestamp_text_outgoing;
                subjectLabelColorResId = R.color.timestamp_text_outgoing;
            }
        } else {
            messageColorResId = (mData.getIsIncoming() ?
                    R.color.message_text_color_incoming : R.color.message_text_color_outgoing);
            statusColorResId = messageColorResId;
          //  infoColorResId = R.color.timestamp_text_incoming;
            infoColorResId = R.color.white;
            switch(mData.getStatus()) {

                case MessageData.BUGLE_STATUS_OUTGOING_FAILED:
                case MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER:
                    timestampColorResId = R.color.message_failed_timestamp_text;
                    subjectLabelColorResId = R.color.timestamp_text_outgoing;
                    break;

                case MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND:
                case MessageData.BUGLE_STATUS_OUTGOING_SENDING:
                case MessageData.BUGLE_STATUS_OUTGOING_RESENDING:
                case MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY:
                case MessageData.BUGLE_STATUS_OUTGOING_COMPLETE:
                case MessageData.BUGLE_STATUS_OUTGOING_DELIVERED:
                case MessageData.BUGLE_STATUS_OUTGOING_DISPLAYED    :
                    timestampColorResId = R.color.timestamp_text_outgoing;
                    subjectLabelColorResId = R.color.timestamp_text_outgoing;
                    break;

                case MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE:
                case MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED:
                    messageColorResId = R.color.message_text_color_incoming_download_failed;
                    timestampColorResId = R.color.message_download_failed_timestamp_text;
                    subjectLabelColorResId = R.color.message_text_color_incoming_download_failed;
                    statusColorResId = R.color.message_download_failed_status_text;
                    infoColorResId = R.color.message_info_text_incoming_download_failed;
                    break;

                case MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
                case MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
                case MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
                case MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
                case MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD:
                    timestampColorResId = R.color.message_text_color_incoming;
                    subjectLabelColorResId = R.color.message_text_color_incoming;
                  //  infoColorResId = R.color.timestamp_text_incoming;
                    infoColorResId = R.color.white;
                    break;

                case MessageData.BUGLE_STATUS_INCOMING_COMPLETE:
                default:
                    timestampColorResId = R.color.timestamp_text_incoming;
                    subjectLabelColorResId = R.color.timestamp_text_incoming;
                    infoColorResId = -1; // Not used
                    break;
            }
        }
        final int messageColor = getResources().getColor(messageColorResId);

        mMessageTextView.setTextColor(messageColor);
        mMessageTextView.setLinkTextColor(messageColor);
        mSubjectText.setTextColor(messageColor);
        if (statusColorResId >= 0) {
            mTitleTextView.setTextColor(getResources().getColor(statusColorResId));
        }
        if (infoColorResId >= 0) {
            mMmsInfoTextView.setTextColor(getResources().getColor(infoColorResId));
        }
        if (timestampColorResId == R.color.timestamp_text_incoming &&
                mData.hasAttachments() && !shouldShowMessageTextBubble()) {
            timestampColorResId = R.color.timestamp_text_outgoing;
        }
        mStatusTextView.setTextColor(getResources().getColor(timestampColorResId));

        mSubjectLabel.setTextColor(getResources().getColor(subjectLabelColorResId));
        mSenderNameTextView.setTextColor(getResources().getColor(timestampColorResId));
    }

    /**
     * If we don't know the size of the image, we want to show it in a fixed-sized frame to
     * avoid janks when the image is loaded and resized. Otherwise, we can set the imageview to
     * take on normal layout params.
     */
    private void adjustImageViewBounds(final MessagePartData imageAttachment) {
        Assert.isTrue(ContentType.isImageType(imageAttachment.getContentType()));
        final ViewGroup.LayoutParams layoutParams = mMessageImageView.getLayoutParams();
        if (imageAttachment.getWidth() == MessagePartData.UNSPECIFIED_SIZE ||
                imageAttachment.getHeight() == MessagePartData.UNSPECIFIED_SIZE) {
            // We don't know the size of the image attachment, enable letterboxing on the image
            // and show a fixed sized attachment. This should happen at most once per image since
            // after the image is loaded we then save the image dimensions to the db so that the
            // next time we can display the full size.
//            layoutParams.width = getResources()
//                    .getDimensionPixelSize(R.dimen.image_attachment_fallback_width);
//            layoutParams.height = getResources()
//                    .getDimensionPixelSize(R.dimen.image_attachment_fallback_height);
//            mMessageImageView.setScaleType(ScaleType.CENTER_CROP);
            mMessageImageView.setScaleType(ScaleType.FIT_XY);
            Log.i("vvvv","adjustImageViewBounds--1");
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            // ScaleType.CENTER_INSIDE and FIT_CENTER behave similarly for most images. However,
            // FIT_CENTER works better for small images as it enlarges the image such that the
            // minimum size ("android:minWidth" etc) is honored.
//            mMessageImageView.setScaleType(ScaleType.FIT_CENTER);
            mMessageImageView.setScaleType(ScaleType.FIT_XY);
            Log.i("vvvv","adjustImageViewBounds--2");
        }
    }

    @Override
    public void onClick(final View view) {
        final Object tag = view.getTag();
//        Log.i("nnnn","-----tag----:"+tag.toString());
        if (tag instanceof MessagePartData) {
            final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
            onAttachmentClick((MessagePartData) tag, bounds, false /* longPress */);
        } else if (tag instanceof String) {
            // Currently the only object that would make a tag of a string is a youtube preview
            // image
            UIIntents.get().launchBrowserForUrl(getContext(), (String) tag);
        }
    }



    @Override
    public boolean onLongClick(final View view) {
//        if (view == mMessageTextView) {
//            // Preemptively handle the long click event on message text so it's not handled by
//            // the link spans.
//            Log.i("bbbb","performLongClick---");
//            return performLongClick();
//        }
//
//        final Object tag = view.getTag();
//        if (tag instanceof MessagePartData) {
//            final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
//            return onAttachmentClick((MessagePartData) tag, bounds, true /* longPress */);
//        }
        return false;
    }

    @Override
    public boolean onAttachmentClick(final MessagePartData attachment,
            final Rect viewBoundsOnScreen, final boolean longPress) {

        return mHost.onAttachmentClick(this, attachment, viewBoundsOnScreen, longPress);
    }

    public ContactIconView getContactIconView() {
        return mContactIconView;
    }

    // Sort photos in MultiAttachLayout in the same order as the ConversationImagePartsView
    static final Comparator<MessagePartData> sImageComparator = new Comparator<MessagePartData>(){
        @Override
        public int compare(final MessagePartData x, final MessagePartData y) {
            return x.getPartId().compareTo(y.getPartId());
        }
    };

    static final Predicate<MessagePartData> sVideoFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isVideo();
        }
    };

    static final Predicate<MessagePartData> sAudioFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isAudio();
        }
    };

    static final Predicate<MessagePartData> sVCardFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isVCard();
        }
    };
    static  boolean isChatbotss = false;
    //juphoon
    static final Predicate<MessagePartData> sChatbotCardFilter = new Predicate<MessagePartData>(){
        @Override
        public boolean apply(final MessagePartData part) {
            Log.i("vvv"," part.isChatbotCard():"+ part.isChatbotCard());
            if(part.isChatbotCard()){
                isChatbotss = true;
            }
            return part.isChatbotCard();// 判断是chatbot消息
        }
    };


    boolean isImage = false;
    final Predicate<MessagePartData> sImageFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            Log.i("vvv"," part.isImage():"+ part.isImage());
            if(part.isImage() == true){
                isImage = true;
            }
            return part.isImage();
        }
    };

    interface AttachmentViewBinder {
        void bindView(View view, MessagePartData attachment);
        void unbind(View view);
    }

    final AttachmentViewBinder mVideoViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            // juphoon 传入 data
            ((VideoThumbnailView) view).setSource(mData, attachment, mData.getIsIncoming());
        }

        @Override
        public void unbind(final View view) {
            ((VideoThumbnailView) view).setSource((Uri) null, mData.getIsIncoming());
        }
    };

    final AttachmentViewBinder mAudioViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            final AudioAttachmentView audioView = (AudioAttachmentView) view;
            // juphoon
            audioView.bindMessagePartData(mData, attachment, isSelected());
            //juphoon: 增加根据会话类型显示不同颜色
            audioView.setBackground(ConversationDrawables.get().getBubbleDrawable(
                    isSelected(), mData.getIsIncoming(), false /* needArrow */,
                    mData.hasIncomingErrorStatus(),
                    mThreadType,
                    mRcsChatbot));
        }

        @Override
        public void unbind(final View view) {
            ((AudioAttachmentView) view).bindMessagePartData(null, mData.getIsIncoming(), false);
        }
    };

    final AttachmentViewBinder mVCardViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            final PersonItemView personView = (PersonItemView) view;
            personView.bind(DataModel.get().createVCardContactItemData(getContext(),
                    attachment));
            //juphoon: 增加根据会话类型显示不同颜色
            personView.setBackground(ConversationDrawables.get().getBubbleDrawable(
                    isSelected(), mData.getIsIncoming(), false /* needArrow */,
                    mData.hasIncomingErrorStatus(),
                    mThreadType,
                    mRcsChatbot));
            final int nameTextColorRes;
            final int detailsTextColorRes;
            if (isSelected()) {
                nameTextColorRes = R.color.message_text_color_incoming;
                detailsTextColorRes = R.color.message_text_color_incoming;
            } else {
                nameTextColorRes = mData.getIsIncoming() ? R.color.message_text_color_incoming
                        : R.color.message_text_color_outgoing;
                detailsTextColorRes = mData.getIsIncoming() ? R.color.timestamp_text_incoming
                        : R.color.timestamp_text_outgoing;
            }
            personView.setNameTextColor(getResources().getColor(nameTextColorRes));
            personView.setDetailsTextColor(getResources().getColor(detailsTextColorRes));
        }

        @Override
        public void unbind(final View view) {
            ((PersonItemView) view).bind(null);
        }
    };

    /**
     * A helper class that allows us to handle long clicks on linkified message text view (i.e. to
     * select the message) so it's not handled by the link spans to launch apps for the links.
     */
    private static class IgnoreLinkLongClickHelper implements OnLongClickListener, OnTouchListener {
        private boolean mIsLongClick;
        private final OnLongClickListener mDelegateLongClickListener;

        /**
         * Ignore long clicks on linkified texts for a given text view.
         * @param textView the TextView to ignore long clicks on
         * @param longClickListener a delegate OnLongClickListener to be called when the view is
         *        long clicked.
         */
        public static void ignoreLinkLongClick(final TextView textView,
                @Nullable final OnLongClickListener longClickListener) {
            final IgnoreLinkLongClickHelper helper =
                    new IgnoreLinkLongClickHelper(longClickListener);
            textView.setOnLongClickListener(helper);
            textView.setOnTouchListener(helper);
        }

        private IgnoreLinkLongClickHelper(@Nullable final OnLongClickListener longClickListener) {
            mDelegateLongClickListener = longClickListener;
        }

        @Override
        public boolean onLongClick(final View v) {
            // Record that this click is a long click.
            mIsLongClick = true;
            if (mDelegateLongClickListener != null) {
                return mDelegateLongClickListener.onLongClick(v);
            }
            return false;
        }

        @Override
        public boolean onTouch(final View v, final MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP && mIsLongClick) {
                // This touch event is a long click, preemptively handle this touch event so that
                // the link span won't get a onClicked() callback.
                mIsLongClick = false;
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mIsLongClick = false;
            }
            return false;
        }
    }

    // juphoon 
    static final Predicate<MessagePartData> sGeoFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isGeo();
        }
    };

    // juphoon
    static final Predicate<MessagePartData> sFileFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isFile();
        }
    };

    // juphoon
    final AttachmentViewBinder mGeoViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            final GeoAttachmentView geoView = (GeoAttachmentView) view;
            geoView.bindMessagePartData(attachment, true);
            geoView.setBackgroundColor(isSelected() ? getResources().getColor(R.color.message_bubble_color_selected) : Color.WHITE);
        }

        @Override
        public void unbind(final View view) {
            ((GeoAttachmentView) view).bindMessagePartData(null, false);
        }
    };
    boolean isChatbot;
    // juphoon
    final AttachmentViewBinder mChatbotViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            // juphoon 传入 data
            isChatbot = true;
            final RcsChatbotCardView RcschatbotCardView = (RcsChatbotCardView) view;
            int padding = getPaddingRight() + getPaddingLeft();
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            int maxWidth = Math.min(wm.getDefaultDisplay().getWidth(), wm.getDefaultDisplay().getHeight()) - padding * 2;
            Log.i("qwe","maxWidth--:"+maxWidth);
            RcschatbotCardView.bindMessagePartData(attachment.getText(), mData.getConversationId(), mData.getSmsMessageUri(), maxWidth);
        }

        @Override
        public void unbind(final View view) {
            ((RcsChatbotCardView) view).unbind();
        }
    };

    // juphoon
    final AttachmentViewBinder mFileViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            final RcsFileAttachmentView fileView = (RcsFileAttachmentView) view;
            fileView.bindMessagePartData(attachment, mData);
            fileView.setBackgroundColor(isSelected() ? getResources().getColor(R.color.message_bubble_color_selected) : Color.WHITE);
            RcsTransProgressManager.TransProgress progress = RcsTransProgressManager.getTransProgress(mData.getMessageId());
            if (progress != null && progress.getMaxSize() > 0) {
                fileView.setFileTransProgress((int)(progress.getTransSize() * 100 / progress.getMaxSize()));
            }
            boolean visibility = !(mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
                    || mData.getStatus() == MessageData.BUGLE_STATUS_INCOMING_COMPLETE
                    || mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED
                    || mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_DISPLAYED);
            fileView.setProgressVisibility(visibility);
        }

        @Override
        public void unbind(final View view) {
            ((RcsFileAttachmentView) view).bindMessagePartData(null, mData);
        }
    };

    // juphoon 状态显示
    private void updateRmsViewContent() {
        updateMessageContent();
        int statusResId = -1;
        String statusText = null;
        if (mData.getParts().size() == 0) return;
        String partContentType = mData.getParts().get(0).getContentType();
        //文件传输是否可以暂停，文件，图片，视频可以暂停，公众账号接收文件，图片，视频时不可以暂停
        boolean isNeedPause = (ContentType.isFileType(partContentType)
                 || (ContentType.isGeoType(partContentType) && mData.getStatus() == MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED));
        switch (mData.getStatus()) {
            case MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
                statusResId = R.string.downloading_rcs_message;// 正在下载
                mDownloadImageBtn.setImageResource(R.drawable.icon_file_cancel);
                mUploadImageBtn.setVisibility(View.GONE);
                mDownloadImageBtn.setVisibility(isNeedPause ? View.VISIBLE : View.GONE);
                break;
            case MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD:
                statusResId = R.string.not_start_download;// 未下载
                mDownloadImageBtn.setImageResource(R.drawable.icon_file_download);
                mUploadImageBtn.setVisibility(View.GONE);
                mDownloadImageBtn.setVisibility(isNeedPause ? View.VISIBLE : View.GONE);
                break;
            case MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED:
                statusResId = R.string.download_rcs_message_fail;// 下载失败
                mDownloadImageBtn.setImageResource(R.drawable.icon_file_download);
                mUploadImageBtn.setVisibility(View.GONE);
                mDownloadImageBtn.setVisibility(isNeedPause ? View.VISIBLE : View.GONE);
                break;
            case MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND:
                statusResId = R.string.not_start_upload;// 未上传
                mUploadImageBtn.setImageResource(R.drawable.icon_file_upload);
                mDownloadImageBtn.setVisibility(View.GONE);
                mUploadImageBtn.setVisibility(isNeedPause ? View.VISIBLE : View.GONE);
                break;
            case MessageData.BUGLE_STATUS_OUTGOING_SENDING:
            case MessageData.BUGLE_STATUS_OUTGOING_RESENDING:
            case MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY:
                statusResId = R.string.sending_rcs_message;// 正在发送
                mUploadImageBtn.setImageResource(R.drawable.icon_file_cancel);
                mDownloadImageBtn.setVisibility(View.GONE);
                mUploadImageBtn.setVisibility(isNeedPause ? View.VISIBLE : View.GONE);
                break;
            case MessageData.BUGLE_STATUS_OUTGOING_FAILED:
                statusResId = R.string.message_status_send_failed_emergency_number;// 发送失败
                mUploadImageBtn.setImageResource(R.drawable.icon_file_upload);
                mDownloadImageBtn.setVisibility(View.GONE);
                mUploadImageBtn.setVisibility(isNeedPause ? View.VISIBLE : View.GONE);
                break;
            case MessageData.BUGLE_STATUS_OUTGOING_COMPLETE:
            case MessageData.BUGLE_STATUS_INCOMING_COMPLETE:
            default:
                if (!mData.getCanClusterWithNextMessage()) {
                    statusText = mData.getFormattedReceivedTimeStamp();// 发送接收成功显示时间
                }
                mUploadImageBtn.setVisibility(View.GONE);
                mDownloadImageBtn.setVisibility(View.GONE);
                break;
        }
        updateImgView(partContentType);
        if (statusResId >= 0) {
            statusText = getResources().getString(statusResId);
        }
        Log.i("qsqs"," statusText--other："+statusText);
        mStatusTextView.setText(statusText);
        final boolean statusVisible = !TextUtils.isEmpty(statusText) || getFileSizeAndDurationVisible();
        Log.i("qsqs"," statusVisible--other："+statusVisible);
        if (statusVisible) {
            mStatusTextView.setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setVisibility(View.GONE);
        }

        final String subjectText = MmsUtils.cleanseMmsSubject(getResources(), mData.getMmsSubject());
        final boolean subjectVisible = !TextUtils.isEmpty(subjectText);

        final boolean senderNameVisible = !mOneOnOne && !mData.getCanClusterWithNextMessage() && mData.getIsIncoming();
        if (senderNameVisible) {
            Log.i("qsqs"," mSenderNameTextView----other："+mData.getSenderDisplayName());
            mSenderNameTextView.setText(mData.getSenderDisplayName());
            mSenderNameTextView.setVisibility(View.VISIBLE);
        } else {
            Log.i("qsqs"," mSenderNameTextView----other123：");
            mSenderNameTextView.setVisibility(View.GONE);
        }

        final boolean deliveredBadgeVisible = mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED;
        mDeliveredBadge.setVisibility(deliveredBadgeVisible ? View.VISIBLE : View.GONE);

        // juphoon displayed view
        boolean displayedBadgeVisible =
                mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_DISPLAYED;
        mDisplayedBadge.setVisibility(displayedBadgeVisible ? View.VISIBLE : View.GONE);

        // Update the sim indicator.
        final boolean showSimIconAsIncoming = mData.getIsIncoming() && (!mData.hasAttachments() || shouldShowMessageTextBubble());
        final SubscriptionListEntry subscriptionEntry = mHost.getSubscriptionEntryForSelfParticipant(mData.getSelfParticipantId(), true /* excludeDefault */);

        final boolean simNameVisible = subscriptionEntry != null && !TextUtils.isEmpty(subscriptionEntry.displayName) && !mData.getCanClusterWithNextMessage();
        if (simNameVisible) {
            Log.i("lll","---------================1");
            final String simNameText = mData.getIsIncoming() ? getResources().getString(R.string.incoming_sim_name_text, subscriptionEntry.displayName)
                    : subscriptionEntry.displayName;
            mSimNameView.setText(simNameText);
            // modify for bug 543852 begin
            mSimNameView.setTextColor(subscriptionEntry.displayColor);
            // modify for bug 543852 end
            mSimNameView.setVisibility(VISIBLE);
        } else {
            Log.i("lll","---------================2");
            mSimNameView.setText(null);
            mSimNameView.setVisibility(GONE);
        }

        final boolean metadataVisible = senderNameVisible || statusVisible || deliveredBadgeVisible || simNameVisible;
        mMessageMetadataView.setVisibility(metadataVisible ? View.VISIBLE : View.GONE);

        final boolean messageTextAndOrInfoVisible = subjectVisible || mData.hasText() || metadataVisible;
        Log.i("ppp","mMessageTextAndInfoView:"+messageTextAndOrInfoVisible);

        mMessageTextAndInfoView.setVisibility(messageTextAndOrInfoVisible ? View.VISIBLE : View.GONE);

        if (shouldShowSimplifiedVisualStyle() || mIsSystemMsg) {
            Log.i("lll","---------================11");
            mContactIconView.setVisibility(View.GONE);
            mContactIconView.setImageResourceUri(null);
        } else if (mRcsChatbot != null && mData.getIsIncoming()) {
            Log.i("lll","---------================3");
            /**
             * Chatbot 会话里有短信需要自己设置头像
             */
            mContactIconView.setVisibility(GONE);
//            mContactIconView.setVisibility(View.VISIBLE);
//            mContactIconView.setImageResourceUri(getChatbotLogoUri(), -1, null, mAddress);
        } else if (!mData.getIsIncoming()) {
            Log.i("lll","---------================4");
            mContactIconView.setVisibility(View.VISIBLE);
            Uri avatarUri = AvatarUriUtil.createAvatarUri(
                    null, null, null, null);
            mContactIconView.setImageResourceUri(avatarUri);
        } else {
            Log.i("lll","---------================5");
            mContactIconView.setVisibility(View.GONE);
            final Uri avatarUri = AvatarUriUtil.createAvatarUri(
                    mData.getSenderProfilePhotoUri(),
                    mData.getSenderFullName(),
                    mData.getSenderNormalizedDestination(),
                    mData.getSenderContactLookupKey());
            mContactIconView.setImageResourceUri(avatarUri, mData.getSenderContactId(),
                    mData.getSenderContactLookupKey(), mData.getSenderNormalizedDestination());
        }

        boolean showNickName = mData.getIsIncoming() && mThreadType == RmsDefine.RMS_GROUP_THREAD
                && !shouldShowSimplifiedVisualStyle() && !mIsSystemMsg;

        if (showNickName) {
            // 需要显示群中的昵称的，优先本地名字，再群昵称，没有昵称显示格式后号码
            RcsGroupHelper.RcsGroupInfo rcsGroupInfo = RcsGroupHelper.getGroupInfo(mAddress);
            String groupMemberName;
            if (rcsGroupInfo == null) {
                groupMemberName = mData.getSenderNormalizedDestination();
            } else {
                groupMemberName = TextUtils.isEmpty(RcsGroupHelper.getGroupInfo(mAddress).getDisplayName(mData.getSenderNormalizedDestination())) ?
                    mData.getSenderNormalizedDestination()
                    : rcsGroupInfo.getDisplayName(mData.getSenderNormalizedDestination());
            }
           // mNickNameView.setText(TextUtils.isEmpty(mData.getSenderContactLookupKey()) ? groupMemberName
          //          : mData.getSenderDisplayName());
            mContactIconView.setClickable(TextUtils.isEmpty(mData.getSenderContactLookupKey()) ? false : true);
          //  mNickNameView.setVisibility(View.VISIBLE);
        } else {
          //  mNickNameView.setVisibility(View.GONE);
        }

        mStatusTextView.setVisibility(mIsSystemMsg ? View.GONE : View.VISIBLE);
        final String statusString = TextUtils.isEmpty(statusText) ? " " : statusText;
        if (mData != null && mData.getIsRms() && getFileSizeAndDurationVisible()) {
            if (mStatusTextView.getTag() != null && mStatusTextView.getTag(R.id.last_file_duration_text) != null
                    && TextUtils.equals(mData.getMessageId(), mStatusTextView.getTag().toString())) {
                mStatusTextView.setText(new StringBuffer().append(mStatusTextView.getTag(R.id.last_file_duration_text).toString())
                        .append("\t").append(statusString).toString());
            } else {
                mStatusTextView.setText(statusString); // 防止布局没有占位
                new AsyncTask<Void, Void, String>() {

                    @Override
                    protected String doInBackground(Void... arg0) {
                        mStatusTextView.setTag(mData.getMessageId());
                        return RcsMmsUtils.getFileSizeAndDuration(Uri.parse(mData.getSmsMessageUri()));
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if (TextUtils.equals(mData.getMessageId(), mStatusTextView.getTag().toString())) {
                            mStatusTextView.setText(new StringBuffer().append(statusString).append("\t").append(result).toString());
                            mStatusTextView.setTag(R.id.last_file_duration_text, result);
                        }
                    }

                }.execute();
            }
        }
        Log.i("ppp","mIsSystemMsg:"+mIsSystemMsg);

        mMessageBubble.setVisibility(mIsSystemMsg?View.GONE : View.VISIBLE);
        mSystemText.setVisibility(mIsSystemMsg?View.VISIBLE : View.GONE);
        if (mIsSystemMsg) {
        } else {
            mMsgType.setText(R.string.msg_type_rcs);
        }
    }

    // juphoon
    private void updateImgView(String partContentType) {
        if (ContentType.isImageType(partContentType)) {
            switch (mData.getStatus()) {
            case MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
            case MessageData.BUGLE_STATUS_OUTGOING_SENDING:
            case MessageData.BUGLE_STATUS_OUTGOING_RESENDING:
                RcsTransProgressManager.TransProgress progress = RcsTransProgressManager.getTransProgress(mData.getMessageId());
                if (progress != null && progress.getMaxSize() > 0) {
                    mLoading.setVisibility(View.VISIBLE);
                    mImgBackground.setVisibility(View.VISIBLE);
                    mImgTransStatus.setVisibility(View.INVISIBLE);
                    mImgDownloadProgress.setVisibility(View.VISIBLE);
                    mImgDownloadProgress.setText(progress.getTransSize() * 100 / progress.getMaxSize() + "%");
                }
                break;
            case MessageData.BUGLE_STATUS_OUTGOING_COMPLETE:
            case MessageData.BUGLE_STATUS_INCOMING_COMPLETE:
            case MessageData.BUGLE_STATUS_OUTGOING_DELIVERED:
            case MessageData.BUGLE_STATUS_OUTGOING_DISPLAYED:
                mLoading.setVisibility(View.INVISIBLE);
                mImgTransStatus.setVisibility(View.INVISIBLE);
                mImgDownloadProgress.setVisibility(View.INVISIBLE);
                mImgBackground.setVisibility(View.GONE);
                break;
            default:
                mImgDownloadProgress.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                mImgBackground.setVisibility(View.VISIBLE);
                mImgTransStatus.setVisibility(View.VISIBLE);
                mImgTransStatus.setImageDrawable(
                        mData.getIsIncoming() ? getResources().getDrawable(R.drawable.icon_video_download)
                                : getResources().getDrawable(R.drawable.icon_video_upload));
                break;
            }
        } else {
            mLoading.setVisibility(View.GONE);
            mImgTransStatus.setVisibility(View.GONE);
            mImgDownloadProgress.setVisibility(View.GONE);
        }
    }

    //juphoon
    private boolean getFileSizeAndDurationVisible() {
        return mData != null && mData.getIsRms()
                && (mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND
                || mData.getStatus() == MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD
                || mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
                || mData.getStatus() == MessageData.BUGLE_STATUS_INCOMING_COMPLETE)
                && (ContentType.isFileType(mData.getParts().get(0).getContentType())
                || ContentType.isImageType(mData.getParts().get(0).getContentType())
                || ContentType.isVideoType(mData.getParts().get(0).getContentType())
                || ContentType.isAudioType(mData.getParts().get(0).getContentType()));
    }

    // juphoon
    private Uri getChatbotLogoUri() {
        String logo = RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(mRcsChatbot.icon), RmsDefine.RMS_ICON_PATH);
        if (logo == null) {
            logo = new StringBuffer()
                    .append("android.resource://").append(RcsMmsInitHelper.getContext().getPackageName())
                    .append("/").append(R.drawable.chatbot_avatar).toString();
        } else {
            logo = Uri.fromFile(new File(logo)).toString();
        }
        return AvatarUriUtil.createAvatarUri(Uri.parse(logo), mRcsChatbot.name, null, null);
    }

    // juphoon
    public class CustomUrlSpan extends ClickableSpan {

        private Context context;
        private String url;

        public CustomUrlSpan(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        public void onClick(View widget) {
            Intent intent = new Intent(context, RcsChatbotWebActivity.class);
            intent.putExtra(RcsChatbotWebActivity.URL, url);
            context.startActivity(intent);
        }
    }
}
