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

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.messaging.BugleApplication;
import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MediaScratchFileProvider;
import com.android.messaging.datamodel.action.InsertNewMessageAction;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.binding.ImmutableBindingRef;
import com.android.messaging.datamodel.data.ConversationData;
import com.android.messaging.datamodel.data.ConversationData.ConversationDataListener;
import com.android.messaging.datamodel.data.ConversationData.SimpleConversationDataListener;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.datamodel.data.DraftMessageData.CheckDraftForSendTask;
import com.android.messaging.datamodel.data.DraftMessageData.CheckDraftTaskCallback;
import com.android.messaging.datamodel.data.DraftMessageData.DraftMessageDataListener;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.datamodel.data.PendingAttachmentData;
import com.android.messaging.datamodel.data.SubscriptionListData.SubscriptionListEntry;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.ui.AttachmentPreview;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.PlainTextEditText;
import com.android.messaging.ui.conversation.ConversationInputManager.ConversationInputSink;
import com.android.messaging.ui.mediapicker.MediaPicker;
import com.android.messaging.ui.mediapicker.RcsMoreChooser;
import com.android.messaging.util.AccessibilityUtil;
import com.android.messaging.util.Assert;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.ImeUtil;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.MediaUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbot.RcsChatbotSuggestionsBean;
import com.juphoon.chatbotmaap.RcsChatbotCardView;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.mms.RcsDatabaseMessages;
import com.juphoon.helper.mms.RcsMessageTransHelper;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.RcsVCardHelper;
import com.juphoon.helper.mms.ui.RcsBaiduLocationActivity;
import com.juphoon.helper.mms.ui.RcsChooseContactActivity;
import com.juphoon.helper.mms.ui.RcsPreviewImageActivity;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;
import com.tcl.ff.component.animer.glow.view.AllCellsGlowLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This view contains the UI required to generate and send messages.
 */
public class ComposeMessageView extends LinearLayout
implements TextView.OnEditorActionListener, DraftMessageDataListener, TextWatcher,
        ConversationInputSink{

    // startActivitForResul 因为无法回到这个类只能回到activity所以添加回调 juphoon
    public interface IMediaChoose {
        void onMediaActivityResult(final int requestCode, final int resultCode, final Intent data);
    }

    //juphoon
    public static WeakReference<IMediaChoose> sIMediaChoose;


    public interface IComposeMessageViewHost extends
            DraftMessageData.DraftMessageSubscriptionDataProvider {
        void sendMessage(MessageData message);
        void onComposeEditTextFocused();
        void onAttachmentsCleared();
        void onAttachmentsChanged(final boolean haveAttachments);
        void displayPhoto(Uri photoUri, Rect imageBounds, boolean isDraft);
        void promptForSelfPhoneNumber();
        boolean isReadyForAction();
        void warnOfMissingActionConditions(final boolean sending,
                final Runnable commandToRunAfterActionConditionResolved);
        void warnOfExceedingMessageLimit(final boolean showAttachmentChooser,
                boolean tooManyVideos);
        void notifyOfAttachmentLoadFailed();
        void showAttachmentChooser();
        boolean shouldShowSubjectEditor();
        boolean shouldHideAttachmentsWhenSimSelectorShown();
        Uri getSelfSendButtonIconUri();
        int overrideCounterColor();
        int getAttachmentsClearedFlags();
    }

    public static final int CODEPOINTS_REMAINING_BEFORE_COUNTER_SHOWN = 10;

    // There is no draft and there is no need for the SIM selector
    private static final int SEND_WIDGET_MODE_SELF_AVATAR = 1;
    // There is no draft but we need to show the SIM selector
    private static final int SEND_WIDGET_MODE_SIM_SELECTOR = 2;
    // There is a draft
    private static final int SEND_WIDGET_MODE_SEND_BUTTON = 3;

    private PlainTextEditText mComposeEditText;
    private PlainTextEditText mComposeSubjectText;
    private TextView mCharCounter;
    private TextView mMmsIndicator;
    private ImageButton mSelfSendIcon;
    private ImageButton mSendButton;
    private View mSubjectView;

    private View fragment;
    private ImageButton mDeleteSubjectButton;
    private AttachmentPreview mAttachmentPreview;
    private ImageButton mAttachMediaButton;
    private LinearLayout mActionLayout, mBlockedList; //juphoon

    private final Binding<DraftMessageData> mBinding;
    private IComposeMessageViewHost mHost;
    private final Context mOriginalContext;
    private int mSendWidgetMode = SEND_WIDGET_MODE_SELF_AVATAR;

    // Shared data model object binding from the conversation.
    private ImmutableBindingRef<ConversationData> mConversationDataModel;

    // Centrally manages all the mutual exclusive UI components accepting user input, i.e.
    // media picker, IME keyboard and SIM selector.
    private ConversationInputManager mInputManager;

    /** juphoon **/
    public static final int REQUEST_SEND_IMAGE_URI        = 202; // 原图发送uri
    public static final int REQUEST_CODE_NOTIFY_FILE      = 203; // 选择文件
    public static final int REQUEST_CODE_SEND_AUDIO       = 204;
    public static final int REQUEST_CODE_SEND_VIDEO       = 205;
    public static final int REQUEST_CODE_SEND_VCARD       = 206;
    public static final int REQUEST_CODE_SEND_GEO         = 207;
    private ArrayList<RcsMessageTransHelper.RcsFailMessage> mFailMsgs = new ArrayList<>(); // 记录发送失败需要转彩信的消息
    private static final int IMAGE_SEND_LIMIT_SIZE = 300 * 1024;// 大于300kb,需要选择是否原图发送
    private static final int IMAGE_SEND_MAX_LIMIT_SIZE = 10000* 1024;// 大于10M,需要选择是否原图发送
    private MediaPicker.ExternalViewController mExternalViewController;
    private boolean mIsRcsMsgMode = false;                      // 要发送的消息是否是Rcs消息
    private ConversationMessageData mConversationLastMessageData;
    private boolean mSupportBroadcast = true; //false: 是群发会话,但是不支持群发或者人数超过群发最大人数

    boolean isFirst = false;
    // juphoon 监听登录状态
    private final RcsServiceManager.IServiceManagerCallback mRcsServiceCallBack = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(boolean logined) {
            updateVisualsOnDraftChanged();
        };
    };

    // juphoon 监听消息发送状态
    private final RcsMessageTransHelper.FailMessageCallback mRcsSendMsgCallBack = new RcsMessageTransHelper.FailMessageCallback() {

        @Override
        public void onReceiveFailMessage(RcsMessageTransHelper.RcsFailMessage message) {
            if (mConversationDataModel.getData().getConversationThreadType() == RmsDefine.COMMON_THREAD
                    && !TextUtils.equals(mConversationDataModel.getData().getOtherParticipantNormalizedDestination(), RmsDefine.PC_ADDRESS)) {
                if (mBinding.getData().hasAttachments() || // 当前存在内容，加入彩信转队列
                        !TextUtils.isEmpty(mBinding.getData().getMessageText()) ||
                        !TextUtils.isEmpty(mBinding.getData().getMessageSubject())) {
                    synchronized (mFailMsgs) {
                        mFailMsgs.add(message);
                    }
                } else {
                    notifyTransToMms(message);
                }
            }
        }

        @Override
        public void onReceiveFailFile(RcsMessageTransHelper.RcsFailMessage message) {
            onReceiveFailMessage(message);
        }
    };

    private final ConversationDataListener mDataListener = new SimpleConversationDataListener() {
        @Override
        public void onConversationMetadataUpdated(ConversationData data) {
            mConversationDataModel.ensureBound(data);
            updateVisualsOnDraftChanged();
        }

        @Override
        public void onConversationParticipantDataLoaded(ConversationData data) {
            mConversationDataModel.ensureBound(data);
            updateVisualsOnDraftChanged();
        }

        @Override
        public void onSubscriptionListDataLoaded(ConversationData data) {
            mConversationDataModel.ensureBound(data);
            updateOnSelfSubscriptionChange();
            updateVisualsOnDraftChanged();
        }
    };

    public void pickMediaView(int selectedIndex) {
        mInputManager.showMediaPicker(true, true, selectedIndex);
    }

    public ComposeMessageView(final Context context, final AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.ColorAccentBlueOverrideStyle), attrs);
        mOriginalContext = context;
        mBinding = BindingBase.createBinding(this);
    }

    /**
     * Host calls this to bind view to DraftMessageData object
     */
    public void bind(final DraftMessageData data, final IComposeMessageViewHost host) {
        mHost = host;
        mBinding.bind(data);
        data.addListener(this);
        data.setSubscriptionDataProvider(host);

        final int counterColor = mHost.overrideCounterColor();
        if (counterColor != -1) {
            mCharCounter.setTextColor(counterColor);
        }
        
        // juphoon 添加登录状态的监听回调
        RcsServiceManager.addCallBack(mRcsServiceCallBack);
        // juphoon 添加对RCS消息发送状态的监听回调
        RcsMessageTransHelper.addRcsFailMessageCallback(mRcsSendMsgCallBack);
        sIMediaChoose = new WeakReference<>(iMediaChoose); // juphoon
    }

    /**
     * Host calls this to unbind view
     */
    public void unbind() {
        mBinding.unbind();
        mHost = null;
        mInputManager.onDetach();
        // juphoon 取消登录状态的监听回调
        RcsServiceManager.removeCallBack(mRcsServiceCallBack);
        // juphoon 取消对RCS消息发送状态的监听回调
        RcsMessageTransHelper.removeRcsFailMessageCallback();
        sIMediaChoose = null;// juphoon
    }

    private LinearLayout inputs;

    private FrameLayout fragment_attchch;
    @Override
    protected void onFinishInflate() {
        Log.i("ccc","----------------0123214214");
        mComposeEditText = (PlainTextEditText) findViewById(
                R.id.compose_message_text);

        fragment = findViewById(R.id.mediapicker_container);
        inputs = findViewById(R.id.inputs);

        mComposeMessageView = (ComposeMessageView)
                findViewById(R.id.message_compose_view_container);
        mComposeEditText.setOnEditorActionListener(this);
        mComposeEditText.addTextChangedListener(this);
        mComposeEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                if (v == mComposeEditText && hasFocus) {
                    mHost.onComposeEditTextFocused();
                    checkChatbotSendFirstMessage();//juphoon
                }
            }
        });
        mComposeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
//                if (mHost.shouldHideAttachmentsWhenSimSelectorShown()) {
//                    hideSimSelector();
//                }
                Log.i("xxx","弹出键盘");
//                mComposeEditText.setFocusable(true);
//                mComposeEditText.setFocusableInTouchMode(true);
//                mComposeEditText.requestFocus();
//                ImeUtil.get().showImeKeyboard(getContext(), mComposeEditText);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        InputMethodManager manager =(InputMethodManager) mComposeEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                        manager.showSoftInput(mComposeEditText, 0);
//                    }
//                }, 100);

            }
        });
        mComposeEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    mComposeEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mComposeEditText.setFocusable(true);
                            mComposeEditText.setFocusableInTouchMode(true);
                            mComposeEditText.requestFocus();
                        } }, 1);
                }
                if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                    Log.i("xxx","弹出键盘s");
                    mComposeEditText.setFocusable(true);
                    mComposeEditText.setFocusableInTouchMode(true);
                    mComposeEditText.requestFocus();
                    ImeUtil.get().showImeKeyboard(getContext(), mComposeEditText);
                }
                return false;
            }
        });
        // onFinishInflate() is called before self is loaded from db. We set the default text
        // limit here, and apply the real limit later in updateOnSelfSubscriptionChange().
        mComposeEditText.setFilters(new InputFilter[] {
                new LengthFilter(MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID)
                        .getMaxTextLimit()) });

//        mSelfSendIcon = (ImageButton) findViewById(R.id.self_send_icon);
//        mSelfSendIcon.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                boolean shown = mInputManager.toggleSimSelector(true /* animate */,
//                        getSelfSubscriptionListEntry());
//                hideAttachmentsWhenShowingSims(shown);
//            }
//        });
//        mSelfSendIcon.setOnLongClickListener(new OnLongClickListener() {
//            @Override
//            public boolean onLongClick(final View v) {
//                /** juphoon 是pc，公众账号，群会话，长按发送处头像不弹发送彩信的主题，只切卡 **/
//                if (!isOnlyRcsConveration() && mHost.shouldShowSubjectEditor()) {
//                    showSubjectEditor();
//                } else {
//                    boolean shown = mInputManager.toggleSimSelector(true /* animate */,
//                            getSelfSubscriptionListEntry());
//                    hideAttachmentsWhenShowingSims(shown);
//                }
//                return true;
//            }
//        });

        mComposeSubjectText = (PlainTextEditText) findViewById(
                R.id.compose_subject_text);
        // We need the listener to change the avatar to the send button when the user starts
        // typing a subject without a message.
        mComposeSubjectText.addTextChangedListener(this);
        // onFinishInflate() is called before self is loaded from db. We set the default text
        // limit here, and apply the real limit later in updateOnSelfSubscriptionChange().
        mComposeSubjectText.setFilters(new InputFilter[] {
                new LengthFilter(MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID)
                        .getMaxSubjectLength())});

        mDeleteSubjectButton = (ImageButton) findViewById(R.id.delete_subject_button);
        mDeleteSubjectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View clickView) {
                hideSubjectEditor();
                mComposeSubjectText.setText(null);
                mBinding.getData().setMessageSubject(null);
                //juphoon 删除彩信模式主题后取消彩信模式,并刷新
                mBinding.getData().setMmsModel(false);
                updateVisualsOnDraftChanged();
            }
        });

        mSubjectView = findViewById(R.id.subject_view);
        mSendButton = (ImageButton) findViewById(R.id.send_message_button);
        mSendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View clickView) {
                /** juphoon 登陆中在PC，公众账号，群会话中，不是Rcs作发送卡，点击发送提示不支持 **/
                if (isOnlyRcsConveration() && RcsServiceManager.isLogined() &&
                        getConversationSubId() != RcsServiceManager.getSubId()) {
                    UiUtils.showToastAtBottom(R.string.not_support_rcs);
                    return;
                }
                if (RcsMmsUtils.getFirstUseRcs() && RcsServiceManager.isLogined()) {
                    notifyUseRcs();
                    RcsMmsUtils.setFirstUseRcs(false);
                    return;
                }
                sendMessageInternal(true /* checkMessageSize */);
                isFirst_count = 0;
            }
        });
//        mSendButton.setOnLongClickListener(new OnLongClickListener() {
//            @Override-
//            public boolean onLongClick(final View arg0) {
//                boolean shown = mInputManager.toggleSimSelector(true /* animate */,
//                        getSelfSubscriptionListEntry());
//                hideAttachmentsWhenShowingSims(shown);
//                if (mHost.shouldShowSubjectEditor()) {
//                    /** juphoon 是PC，公众账号，群会话，长按发送按钮不弹出主题 **/
//                    if (isOnlyRcsConveration()) {
//                        return true;
//                    }
//                    showSubjectEditor();
//                }
//                return true;
//            }
//        });
        mSendButton.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                // When the send button is long clicked, we want TalkBack to announce the real
                // action (select SIM or edit subject), as opposed to "long press send button."
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                    event.getText().clear();
                    event.getText().add(getResources()
                            .getText(shouldShowSimSelector(mConversationDataModel.getData()) ?
                            R.string.send_button_long_click_description_with_sim_selector :
                                R.string.send_button_long_click_description_no_sim_selector));
                    // Make this an announcement so TalkBack will read our custom message.
                    event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                }
            }
        });
        mSendButton.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    mSendButton.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSendButton.setFocusable(true);
                            mSendButton.setFocusableInTouchMode(true);
                            mSendButton.requestFocus();
                        } }, 1);
                }
                return false;
            }
        });

        mAttachmentPreview = (AttachmentPreview) findViewById(R.id.attachment_draft_view);
        mAttachmentPreview.setComposeMessageView(this);
        //mAttachmentPreview.onWindowVisibilityChanged(View.GONE);
        fragment_attchch = mAttachmentPreview.findViewById(R.id.fragment_attchch);

        isFirst = true;
        mAttachMediaButton =
                (ImageButton) findViewById(R.id.attach_media_button);
        mAttachMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View clickView) {
                // Showing the media picker is treated as starting to compose the message.
                Log.i("ccc","点击+号......");
                inputs.setVisibility(GONE);
                mInputManager.showHideMediaPicker(true /* show */, true /* animate */);
                mAttachmentPreview.onWindowVisibilityChanged(View.VISIBLE);
                isFirst = false;
            }
        });
        mAttachMediaButton.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && ! BugleApplication.getInstance().isGong()) {
                    mAttachMediaButton.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mAttachMediaButton.setFocusable(true);
                            mAttachMediaButton.setFocusableInTouchMode(true);
                            mAttachMediaButton.requestFocus();
                        } }, 1);
                }
                if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                    mAttachMediaButton.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mAttachMediaButton.setFocusable(true);
                            mAttachMediaButton.setFocusableInTouchMode(true);
                            mAttachMediaButton.requestFocus();
                        } }, 1);
                }
                return false;
            }
        });


        mCharCounter = (TextView) findViewById(R.id.char_counter);
        mMmsIndicator = (TextView) findViewById(R.id.mms_indicator);
        mActionLayout = (LinearLayout) findViewById(R.id.action_layout); //juphoon
        mBlockedList = (LinearLayout) findViewById(R.id.blocked_tip);//juphoon
    }

    private void hideAttachmentsWhenShowingSims(final boolean simPickerVisible) {
        if (!mHost.shouldHideAttachmentsWhenSimSelectorShown()) {
            return;
        }
        final boolean haveAttachments = mBinding.getData().hasAttachments();
        Log.i("mmm","haveAttachments--:"+haveAttachments);
        Log.i("mmm","simPickerVisible--:"+simPickerVisible);
        if (simPickerVisible && haveAttachments) {
            mHost.onAttachmentsChanged(false);
            mAttachmentPreview.hideAttachmentPreview();
        } else {
            mHost.onAttachmentsChanged(haveAttachments);
            mAttachmentPreview.onAttachmentsChanged(mBinding.getData());
        }
    }

    public void setInputManager(final ConversationInputManager inputManager) {
        mInputManager = inputManager;
    }

    public void setConversationDataModel(final ImmutableBindingRef<ConversationData> refDataModel) {
        mConversationDataModel = refDataModel;
        mConversationDataModel.getData().addConversationDataListener(mDataListener);
    }

    ImmutableBindingRef<DraftMessageData> getDraftDataModel() {
        return BindingBase.createBindingReference(mBinding);
    }

    // returns true if it actually shows the subject editor and false if already showing
    private boolean showSubjectEditor() {
        // show the subject editor
        if (mSubjectView.getVisibility() == View.GONE) {
            mSubjectView.setVisibility(View.VISIBLE);
            mSubjectView.requestFocus();
            return true;
        }
        return false;
    }

    private void hideSubjectEditor() {
        mSubjectView.setVisibility(View.GONE);
        mComposeEditText.requestFocus();
    }

    /**
     * {@inheritDoc} from TextView.OnEditorActionListener
     */
    @Override // TextView.OnEditorActionListener.onEditorAction
    public boolean onEditorAction(final TextView view, final int actionId, final KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessageInternal(true /* checkMessageSize */);
            return true;
        }
        return false;
    }

    private void sendMessageInternal(final boolean checkMessageSize) {
        LogUtil.i(LogUtil.BUGLE_TAG, "UI initiated message sending in conversation " +
                mBinding.getData().getConversationId());
        if (mBinding.getData().isCheckingDraft()) {
            // Don't send message if we are currently checking draft for sending.
            LogUtil.w(LogUtil.BUGLE_TAG, "Message can't be sent: still checking draft");
            return;
        }
        // Check the host for pre-conditions about any action.
        if (mHost.isReadyForAction()) {

            mInputManager.showHideSimSelector(false /* show */, true /* animate */);
            final String messageToSend = mComposeEditText.getText().toString();
            mBinding.getData().setMessageText(messageToSend);
            final String subject = mComposeSubjectText.getText().toString();
            mBinding.getData().setMessageSubject(subject);
            /** juphoon 有主题设置彩信模式 **/
            if (!TextUtils.isEmpty(subject)) {
                mBinding.getData().setMmsModel(true);
            }
            // 登录状态下,subId值一致,不是彩信模式,则发rcs消息, 彩信模式在转发彩信，和发送失败转彩信2种场景置true
            if (mIsRcsMsgMode) {
                final ArrayList<MessageData> messageList = mBinding.getData()
                        .prepareMessageForRmsSendingList(mBinding);
                for (int i = 0; i < messageList.size(); i++) {
                    final MessageData message = messageList.get(i);
                    if (message != null && message.hasContent()) {
                        //juphoon发送多条消息将最后一条消息添加标记,用于更新draft
                        message.setRcsMessageIsEnd(i == messageList.size() - 1);
                        final MessagePartData part = ((ArrayList<MessagePartData>) message.getParts()).get(0);
                        //配置的文件大小限制
                        new AsyncTask<Void, Void, Boolean>() {

                            @Override
                            protected Boolean doInBackground(Void... arg0) {
                                if (part.getContentUri() == null) {
                                    return false;
                                }
                                return UriUtil.getContentSize(part.getContentUri()) > RcsCallWrapper.rcsGetMaxFileSize() * 1024;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                if (result) {
                                    UiUtils.showToastAtBottom(R.string.file_over_max);
                                } else {
                                    playSentSound();
                                    mHost.sendMessage(message);
                                    // juphoon发送消息后判断是否有失败消息转彩信
                                    checkNeedTransMms();
                                    // juphoon发送消息后将类型设置为非彩信
                                    mBinding.getData().setMmsModel(false);
                                }
                            }
                        }.execute();
                    }
                }
                return;
            // 在不满足上面的条件时,若为公众帐号或者群聊,则直接返回
            } else if (isOnlyRcsConveration()) {
                return;
            }
            // Asynchronously check the draft against various requirements before sending.
            mBinding.getData().checkDraftForAction(checkMessageSize,
                    mHost.getConversationSelfSubId(), new CheckDraftTaskCallback() {
                @Override
                public void onDraftChecked(DraftMessageData data, int result) {
                    mBinding.ensureBound(data);
                    switch (result) {
                        case CheckDraftForSendTask.RESULT_PASSED:
                            // Continue sending after check succeeded.
                            final MessageData message = mBinding.getData()
                                    .prepareMessageForSending(mBinding);
                            if (message != null && message.hasContent()) {
							//juphoon
                                ArrayList<MessagePartData> parts = (ArrayList<MessagePartData>) message.getParts();
                                for (int i = 0; i < parts.size(); i++) {
                                    MessagePartData part = parts.get(i);
                                    if (part.isGeo() || part.isFile() || part.isVCard()) {
                                        UiUtils.showToastAtBottom(R.string.not_support_msg);
                                        return;
                                    }
                                }
                                playSentSound();
                                mHost.sendMessage(message);
                                hideSubjectEditor();
                                if (AccessibilityUtil.isTouchExplorationEnabled(getContext())) {
                                    AccessibilityUtil.announceForAccessibilityCompat(
                                            ComposeMessageView.this, null,
                                            R.string.sending_message);
                                }
                                // juphoon发送消息后判断是否有失败消息转彩信
                                checkNeedTransMms();
                                // juphoon发送消息后将类型设置为非彩信
                                mBinding.getData().setMmsModel(false);
                            }
                            break;

                        case CheckDraftForSendTask.RESULT_HAS_PENDING_ATTACHMENTS:
                            // Cannot send while there's still attachment(s) being loaded.
                            UiUtils.showToastAtBottom(
                                    R.string.cant_send_message_while_loading_attachments);
                            break;

                        case CheckDraftForSendTask.RESULT_NO_SELF_PHONE_NUMBER_IN_GROUP_MMS:
                            mHost.promptForSelfPhoneNumber();
                            break;

                        case CheckDraftForSendTask.RESULT_MESSAGE_OVER_LIMIT:
                            Assert.isTrue(checkMessageSize);
                            mHost.warnOfExceedingMessageLimit(
                                    false /*sending juphoon 超过大小不允许发*/, false /* tooManyVideos */);
                            break;

                        case CheckDraftForSendTask.RESULT_VIDEO_ATTACHMENT_LIMIT_EXCEEDED:
                            Assert.isTrue(checkMessageSize);
                            mHost.warnOfExceedingMessageLimit(
                                    true /*sending*/, true /* tooManyVideos */);
                            break;

                        case CheckDraftForSendTask.RESULT_SIM_NOT_READY:
                            // Cannot send if there is no active subscription
                            UiUtils.showToastAtBottom(
                                    R.string.cant_send_message_without_active_subscription);
                            break;

                        default:
                            break;
                    }
                }
            }, mBinding);
        } else {
            mHost.warnOfMissingActionConditions(true /*sending*/,
                    new Runnable() {
                        @Override
                        public void run() {
                            sendMessageInternal(checkMessageSize);
                        }

            });
        }
    }

    public static void playSentSound() {
        // Check if this setting is enabled before playing
        final BuglePrefs prefs = BuglePrefs.getApplicationPrefs();
        final Context context = Factory.get().getApplicationContext();
        final String prefKey = context.getString(R.string.send_sound_pref_key);
        final boolean defaultValue = context.getResources().getBoolean(
                R.bool.send_sound_pref_default);
        if (!prefs.getBoolean(prefKey, defaultValue)) {
            return;
        }
        MediaUtil.get().playSound(context, R.raw.message_sent, null /* completionListener */);
    }

    private int count =0;
    private boolean haveData;
    /**
     * {@inheritDoc} from DraftMessageDataListener
     */
    @Override // From DraftMessageDataListener
    public void onDraftChanged(final DraftMessageData data, final int changeFlags) {
        // As this is called asynchronously when message read check bound before updating text
        mBinding.ensureBound(data);

        // We have to cache the values of the DraftMessageData because when we set
        // mComposeEditText, its onTextChanged calls updateVisualsOnDraftChanged,
        // which immediately reloads the text from the subject and message fields and replaces
        // what's in the DraftMessageData.

        final String subject = data.getMessageSubject();
        final String message = data.getMessageText();

        if ((changeFlags & DraftMessageData.MESSAGE_SUBJECT_CHANGED) ==
                DraftMessageData.MESSAGE_SUBJECT_CHANGED) {
            mComposeSubjectText.setText(subject);

            // Set the cursor selection to the end since setText resets it to the start
            mComposeSubjectText.setSelection(mComposeSubjectText.getText().length());
        }

        if ((changeFlags & DraftMessageData.MESSAGE_TEXT_CHANGED) ==
                DraftMessageData.MESSAGE_TEXT_CHANGED) {
            mComposeEditText.setText(message);

            // Set the cursor selection to the end since setText resets it to the start
            mComposeEditText.setSelection(mComposeEditText.getText().length());
        }

        if ((changeFlags & DraftMessageData.ATTACHMENTS_CHANGED) ==
                DraftMessageData.ATTACHMENTS_CHANGED) {
            final boolean haveAttachments = mAttachmentPreview.onAttachmentsChanged(data);
            haveData = haveAttachments;
            if(!isFirst && BugleApplication.getInstance().isMulit()) {
                mAttachmentPreview.onWindowVisibilityChanged(View.VISIBLE);
                BugleApplication.getInstance().setMulit(false);
            }
            Log.i("mmm","haveAttachments123:"+haveAttachments);

            if(haveAttachments){
                mComposeEditText.setOnKeyListener(new OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            Log.i("mmm","haveAttachments--KEYCODE_DPAD_UP:");
                            fragment_attchch.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    fragment_attchch.setFocusable(true);
                                    fragment_attchch.setFocusableInTouchMode(true);
                                    fragment_attchch.requestFocus();
                                } }, 1);

                        }
                        return false;
                    }
                });
            }
            mHost.onAttachmentsChanged(haveAttachments);

        }

        if ((changeFlags & DraftMessageData.SELF_CHANGED) == DraftMessageData.SELF_CHANGED) {
            updateOnSelfSubscriptionChange();
        }
        updateVisualsOnDraftChanged();
    }

    @Override   // From DraftMessageDataListener
    public void onDraftAttachmentLimitReached(final DraftMessageData data) {
        mBinding.ensureBound(data);
        mHost.warnOfExceedingMessageLimit(false /* sending */, false /* tooManyVideos */);
    }

    private void updateOnSelfSubscriptionChange() {
        // Refresh the length filters according to the selected self's MmsConfig.
        mComposeEditText.setFilters(new InputFilter[] {
                new LengthFilter(MmsConfig.get(mBinding.getData().getSelfSubId())
                        .getMaxTextLimit()) });
        mComposeSubjectText.setFilters(new InputFilter[] {
                new LengthFilter(MmsConfig.get(mBinding.getData().getSelfSubId())
                        .getMaxSubjectLength())});
    }

    @Override
    public void onMediaItemsSelected(final Collection<MessagePartData> items) {
	//juphoon
        MessagePartData msgPartData = null;
        for (MessagePartData data : items) {
            Log.i("bvc","-----------6");
            msgPartData = data;
            break;
        }
//        if (!addOriginalPicture(msgPartData)) {
            Log.i("bvc","-----------7");
            mBinding.getData().addAttachments(items);
            announceMediaItemState(true /* isSelected */);
//        }
    }

    @Override
    public void onMediaItemsUnselected(final MessagePartData item) {
        mBinding.getData().removeAttachment(item);
        announceMediaItemState(false /*isSelected*/);
    }

    @Override
    public void onPendingAttachmentAdded(final PendingAttachmentData pendingItem) {
	//juphoon
        if (!addOriginalPicture(pendingItem)) {
            mBinding.getData().addPendingAttachment(pendingItem, mBinding);
            resumeComposeMessage();
        }
    }

    private void announceMediaItemState(final boolean isSelected) {
        final Resources res = getContext().getResources();
        final String announcement = isSelected ? res.getString(
                R.string.mediapicker_gallery_item_selected_content_description) :
                    res.getString(R.string.mediapicker_gallery_item_unselected_content_description);
        AccessibilityUtil.announceForAccessibilityCompat(
                this, null, announcement);
    }

    private void announceAttachmentState() {
        if (AccessibilityUtil.isTouchExplorationEnabled(getContext())) {
            int attachmentCount = mBinding.getData().getReadOnlyAttachments().size()
                    + mBinding.getData().getReadOnlyPendingAttachments().size();
            final String announcement = getContext().getResources().getQuantityString(
                    R.plurals.attachment_changed_accessibility_announcement,
                    attachmentCount, attachmentCount);
            AccessibilityUtil.announceForAccessibilityCompat(
                    this, null, announcement);
        }
    }
    private ComposeMessageView mComposeMessageView;

    @Override
    public void resumeComposeMessage() {
        Log.i("oneone","mComposeMessageView---visiable");
        inputs.setVisibility(VISIBLE);
        mComposeMessageView.setVisibility(VISIBLE);
        mComposeEditText.requestFocus();
        mInputManager.showHideImeKeyboard(true, true);
        announceAttachmentState();
    }

    public void clearAttachments() {
        mBinding.getData().clearAttachments(mHost.getAttachmentsClearedFlags());
        mHost.onAttachmentsCleared();
        // juphoon发送消息后判断是否有失败消息转彩信
        checkNeedTransMms();
        // juphoon发送消息后将类型设置为非彩信
        mBinding.getData().setMmsModel(false);
        updateVisualsOnDraftChanged();
        mComposeEditText.requestFocus();

    }

    public void requestDraftMessage(boolean clearLocalDraft) {
        mBinding.getData().loadFromStorage(mBinding, null, clearLocalDraft);
    }

    public void setDraftMessage(final MessageData message) {
        mBinding.getData().loadFromStorage(mBinding, message, false);
    }

    /**
     * juphoon 增加 Chatbot 添加到黑名单提示
     *
     * @param serviceId
     */
    public void setBlockedTips(String serviceId) {
        mBlockedList.removeAllViews();
        mBlockedList.setVisibility(GONE);
        RcsChatbotHelper.RcsChatbot mRcsChatbot = RcsChatbotHelper.getChatbotInfoByServiceId(serviceId);
        if (mRcsChatbot != null && (mRcsChatbot.block || mRcsChatbot.black)) {
            TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.chatbot_blocked_list_text, null);
            Log.d("mnb","setBlockedTips---:"+textView);
            mBlockedList.addView(textView);
            mBlockedList.setVisibility(VISIBLE);
        }
    }
    private int isFirst_count = 0;
    /**
     * juphoon 控制 Chatbot 外部 Suggestion 逻辑
     * @param messageData
     */
    public void setLastConversationMessageData(ConversationMessageData messageData) {
        if (mConversationLastMessageData != null && messageData != null) {
            if (TextUtils.equals(mConversationLastMessageData.getSmsMessageUri(), messageData.getSmsMessageUri())) {
                return;
            }
        }
        mConversationLastMessageData = messageData;
        if (mConversationLastMessageData == null) {
            return;
        }
        if (!RcsChatbotHelper.isChatbotByServiceId(mConversationLastMessageData.getConversationParticipantNormalizedDestination())) {
            return;
        }

        mActionLayout.removeAllViews();
        if (!mConversationLastMessageData.getIsRms() || !mConversationLastMessageData.getIsIncoming()) {
            return;
        }
        new AsyncTask<String, Void, RcsDatabaseMessages.RmsMessage>() {

            @Override
            protected RcsDatabaseMessages.RmsMessage doInBackground(String... strings) {
                return RcsMmsUtils.loadRms(Uri.parse(strings[0]));
            }


            @Override
            protected void onPostExecute(final RcsDatabaseMessages.RmsMessage rmsMessage) {
                super.onPostExecute(rmsMessage);

                if (mConversationLastMessageData == null || !TextUtils.equals(mConversationLastMessageData.getSmsMessageUri(), rmsMessage.getUri())) {
                    return;
                }
                if (!TextUtils.isEmpty(rmsMessage.mRmsExtra2)) {
                    isFirst_count = 0;
                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    itemParams.height = (int) getContext().getResources().getDimension(R.dimen.public_chatbot_button_height);
                    itemParams.setMargins(15, 18, 5, 0);
                    LinearLayout.LayoutParams itemParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    itemParams2.height = (int) getContext().getResources().getDimension(R.dimen.public_chatbot_button_height);
                    itemParams2.setMargins(115, 18, 5, 0);
                    try {
                        RcsChatbotSuggestionsBean externalSuggestions = new Gson().fromJson(rmsMessage.mRmsExtra2, RcsChatbotSuggestionsBean.class);
                        for (final RcsChatbotSuggestionsBean.SuggestionsBean suggestion : externalSuggestions.suggestions) {

                            if (suggestion.action != null) {
                                isFirst_count += 1;
                                Button actionButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.public_button, null);
                                if(isFirst_count == 1){
                                    actionButton.setLayoutParams(itemParams2);
                                }else{
                                    actionButton.setLayoutParams(itemParams);
                                }
                                actionButton.setText(suggestion.action.displayText);
                                Log.i("mnb","suggestion.action.displayText-:"+suggestion.action.displayText);
                                actionButton.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
                                        replyBean.response = new RcsChatbotReplyBean.ResponseBean();
                                        replyBean.response.action = new RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean();
                                        replyBean.response.action.displayText = suggestion.action.displayText;
                                        replyBean.response.action.postback = suggestion.action.postback;
                                        Intent intent = new Intent();
                                        intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
                                        intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationLastMessageData.getConversationId());
                                        intent.putExtra(RcsChatbotDefine.KEY_RMSURI, mConversationLastMessageData.getSmsMessageUri());
                                        intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                                        intent.setAction(RcsChatbotDefine.ACTION_SUGGESTION);
                                        getContext().sendBroadcast(intent);
                                        RcsChatbotCardView.dealSugesstion(suggestion.action, getContext(), mConversationLastMessageData.getConversationId(), rmsMessage.mRmsChatbotServiceId, mConversationLastMessageData.getSmsMessageUri(), rmsMessage.mTrafficType, rmsMessage.mContributionId);
                                        isFirst_count = 0;
                                    }
                                });
                                mActionLayout.addView(actionButton);
                            } else if (suggestion.reply != null) {
                                isFirst_count+=1;
                                Button replyButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.public_button, null);
                                if(isFirst_count == 1){
                                    replyButton.setLayoutParams(itemParams2);
                                }else{
                                    replyButton.setLayoutParams(itemParams);
                                }
                                replyButton.setText(suggestion.reply.displayText);
                                Log.i("mnb","suggestion.reply.displayText-:"+suggestion.reply.displayText);
                                replyButton.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
                                        replyBean.response = new RcsChatbotReplyBean.ResponseBean();
                                        replyBean.response.reply = new RcsChatbotSuggestionsBean.SuggestionsBean.ReplyBean();
                                        replyBean.response.reply.displayText = suggestion.reply.displayText;
                                        replyBean.response.reply.postback = suggestion.reply.postback;
                                        Intent intent = new Intent();
                                        intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
                                        intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationLastMessageData.getConversationId());
                                        intent.putExtra(RcsChatbotDefine.KEY_RMSURI, mConversationLastMessageData.getSmsMessageUri());
                                        intent.setAction(RcsChatbotDefine.ACTION_REPLY);
                                        intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                                        getContext().sendBroadcast(intent);
                                        isFirst_count =0;
                                    }
                                });
                                mActionLayout.addView(replyButton);
                            }
                        }
                    } catch (JsonSyntaxException e) {

                    }
                }
            }
        }.execute(mConversationLastMessageData.getSmsMessageUri());
    }


    public void writeDraftMessage() {
        final String messageText = mComposeEditText.getText().toString();
        mBinding.getData().setMessageText(messageText);

        final String subject = mComposeSubjectText.getText().toString();
        mBinding.getData().setMessageSubject(subject);

        mBinding.getData().saveToStorage(mBinding);
    }

    private void updateConversationSelfId(final String selfId, final boolean notify) {
        mBinding.getData().setSelfId(selfId, notify);
    }

    private Uri getSelfSendButtonIconUri() {
        final Uri overridenSelfUri = mHost.getSelfSendButtonIconUri();
        if (overridenSelfUri != null) {
            return overridenSelfUri;
        }
        final SubscriptionListEntry subscriptionListEntry = getSelfSubscriptionListEntry();

        if (subscriptionListEntry != null) {
            return subscriptionListEntry.selectedIconUri;
        }

        // Fall back to default self-avatar in the base case.
        final ParticipantData self = mConversationDataModel.getData().getDefaultSelfParticipant();
        return self == null ? null : AvatarUriUtil.createAvatarUri(self);
    }

    private SubscriptionListEntry getSelfSubscriptionListEntry() {
        return mConversationDataModel.getData().getSubscriptionEntryForSelfParticipant(
                mBinding.getData().getSelfId(), false /* excludeDefault */);
    }

    private boolean isDataLoadedForMessageSend() {
        // Check data loading prerequisites for sending a message.
        return mConversationDataModel != null && mConversationDataModel.isBound() &&
                mConversationDataModel.getData().getParticipantsLoaded();
    }

    private void updateVisualsOnDraftChanged() {
        final String messageText = mComposeEditText.getText().toString();
        final DraftMessageData draftMessageData = mBinding.getData();
        draftMessageData.setMessageText(messageText);

        final String subject = mComposeSubjectText.getText().toString();
        draftMessageData.setMessageSubject(subject);
        if (!TextUtils.isEmpty(subject)) {
             mSubjectView.setVisibility(View.VISIBLE);
        }

        final boolean hasMessageText = (TextUtils.getTrimmedLength(messageText) > 0);
        final boolean hasSubject = (TextUtils.getTrimmedLength(subject) > 0);
        final boolean hasWorkingDraft = hasMessageText || hasSubject ||
                mBinding.getData().hasAttachments();

        // Update the SMS text counter.
        final int messageCount = draftMessageData.getNumMessagesToBeSent();
        final int codePointsRemaining = draftMessageData.getCodePointsRemainingInCurrentMessage();
        // Show the counter only if:
        // - We are not in MMS mode
        // - We are going to send more than one message OR we are getting close
        boolean showCounter = false;
        if (!draftMessageData.getIsMms() && (messageCount > 1 ||
                 codePointsRemaining <= CODEPOINTS_REMAINING_BEFORE_COUNTER_SHOWN)) {
            showCounter = true;
        }

        if (showCounter) {
            // Update the remaining characters and number of messages required.
            final String counterText = messageCount > 1 ? codePointsRemaining + " / " +
                    messageCount : String.valueOf(codePointsRemaining);
            mCharCounter.setText(counterText);
            mCharCounter.setVisibility(View.VISIBLE);
        } else {
            mCharCounter.setVisibility(View.GONE);
        }

        // Update the send message button. Self icon uri might be null if self participant data
        // and/or conversation metadata hasn't been loaded by the host.
        final Uri selfSendButtonUri = getSelfSendButtonIconUri();
        int sendWidgetMode = SEND_WIDGET_MODE_SELF_AVATAR;
        if (selfSendButtonUri != null) {
            if (hasWorkingDraft && isDataLoadedForMessageSend()) {
                UiUtils.revealOrHideViewWithAnimation(mSendButton, VISIBLE, null);
                if (isOverriddenAvatarAGroup()) {
                    // If the host has overriden the avatar to show a group avatar where the
                    // send button sits, we have to hide the group avatar because it can be larger
                    // than the send button and pieces of the avatar will stick out from behind
                    // the send button.
                   // UiUtils.revealOrHideViewWithAnimation(mSelfSendIcon, GONE, null);
                }
                // juphoon 登录状态下 且 使用rcs卡
                if (RcsServiceManager.isLogined() &&
                        !mBinding.getData().getMmsModel() || isOnlyRcsConveration()) {
                    mMmsIndicator.setVisibility(INVISIBLE);
                } else {
                    mMmsIndicator.setVisibility(draftMessageData.getIsMms() ? VISIBLE : INVISIBLE);
                }
                sendWidgetMode = SEND_WIDGET_MODE_SEND_BUTTON;
            } else {
             //   mSelfSendIcon.setImageResource(R.drawable.send_normal);
                //mSelfSendIcon.setImageResourceUri(selfSendButtonUri);
                if (isOverriddenAvatarAGroup()) {
                    //UiUtils.revealOrHideViewWithAnimation(mSelfSendIcon, VISIBLE, null);
                }
                UiUtils.revealOrHideViewWithAnimation(mSendButton, VISIBLE, null);
                mMmsIndicator.setVisibility(INVISIBLE);
                if (shouldShowSimSelector(mConversationDataModel.getData())) {
                    sendWidgetMode = SEND_WIDGET_MODE_SIM_SELECTOR;
                }
            }
        } else {
           // mSelfSendIcon.setBackground(null);
        }

        if (mSendWidgetMode != sendWidgetMode || sendWidgetMode == SEND_WIDGET_MODE_SIM_SELECTOR) {
            setSendButtonAccessibility(sendWidgetMode);
            mSendWidgetMode = sendWidgetMode;
        }

        // Update the text hint on the message box depending on the attachment type.
        final List<MessagePartData> attachments = draftMessageData.getReadOnlyAttachments();
        final int attachmentCount = attachments.size();
        if (attachmentCount == 0) {
            final SubscriptionListEntry subscriptionListEntry =
                    mConversationDataModel.getData().getSubscriptionEntryForSelfParticipant(
                            mBinding.getData().getSelfId(), false /* excludeDefault */);
            // juphoon
            if (subscriptionListEntry == null) {
                // 单卡
                if (RcsServiceManager.isLogined() && mSupportBroadcast) {
                    mComposeEditText.setHint(R.string.compose_rcsmessage_view_hint_text);
                } else if (isOnlyRcsConveration()) {
                    mComposeEditText.setHint(R.string.not_login);
                } else {
                    mComposeEditText.setHint(R.string.compose_message_view_hint_text);
                }
            } else {
                // 双卡

                if (RcsServiceManager.isLogined() && getConversationSubId() == RcsServiceManager.getSubId() && mSupportBroadcast) {
                    mComposeEditText.setHint(R.string.compose_rcsmessage_view_hint_text);
                } else if (isOnlyRcsConveration() && !(getConversationSubId() == RcsServiceManager.getSubId())) {
                    mComposeEditText.setHint(R.string.not_support_rcs);
                } else if (!RcsServiceManager.isLogined() && isOnlyRcsConveration() && (getConversationSubId() == RcsServiceManager.getSubId())) {
                    mComposeEditText.setHint(R.string.not_login);
                } else {
                    mComposeEditText.setHint(Html.fromHtml(getResources().getString(R.string.compose_message_view_hint_text_multi_sim, subscriptionListEntry.displayName)));
                }
            }
        } else {
            int type = -1;
            for (final MessagePartData attachment : attachments) {
                int newType;
                if (attachment.isImage()) {
                    newType = ContentType.TYPE_IMAGE;
                } else if (attachment.isAudio()) {
                    newType = ContentType.TYPE_AUDIO;
                } else if (attachment.isVideo()) {
                    newType = ContentType.TYPE_VIDEO;
                } else if (attachment.isVCard()) {
                    newType = ContentType.TYPE_VCARD;
                } else if (attachment.isGeo()) { // juphoon
                    newType = ContentType.TYPE_GEO;
                } else {
                    newType = ContentType.TYPE_OTHER;
                }

                if (type == -1) {
                    type = newType;
                } else if (type != newType || type == ContentType.TYPE_OTHER) {
                    type = ContentType.TYPE_OTHER;
                    break;
                }
            }

            switch (type) {
                case ContentType.TYPE_IMAGE:
                    mComposeEditText.setHint(getResources().getQuantityString(
                            R.plurals.compose_message_view_hint_text_photo, attachmentCount));
                    break;

                case ContentType.TYPE_AUDIO:
                    mComposeEditText.setHint(getResources().getQuantityString(
                            R.plurals.compose_message_view_hint_text_audio, attachmentCount));
                    break;

                case ContentType.TYPE_VIDEO:
                    mComposeEditText.setHint(getResources().getQuantityString(
                            R.plurals.compose_message_view_hint_text_video, attachmentCount));
                    break;

                case ContentType.TYPE_VCARD:
                    mComposeEditText.setHint(getResources().getQuantityString(
                            R.plurals.compose_message_view_hint_text_vcard, attachmentCount));
                    break;

                // juphoon
                case ContentType.TYPE_GEO:
                    mComposeEditText.setHint(getResources().getQuantityString(
                            R.plurals.compose_message_view_hint_text_geo, attachmentCount));
                    break;

                case ContentType.TYPE_OTHER:
                    mComposeEditText.setHint(getResources().getQuantityString(
                            R.plurals.compose_message_view_hint_text_attachments, attachmentCount));
                    break;

                default:
                    Assert.fail("Unsupported attachment type!");
                    break;
            }
        }
        // juphoon

        mIsRcsMsgMode = RcsServiceManager.isLogined() &&
                !mBinding.getData().getMmsModel() && mSupportBroadcast;
        RcsMmsUtils.setIsRcsMode(mIsRcsMsgMode);
        updateRcsInput();
        checkChatbotSendFirstMessage();
    }

    private void setSendButtonAccessibility(final int sendWidgetMode) {
        switch (sendWidgetMode) {
            case SEND_WIDGET_MODE_SELF_AVATAR:
                // No send button and no SIM selector; the self send button is no longer
                // important for accessibility.
//                mSelfSendIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
//                mSelfSendIcon.setContentDescription(null);
               // mSendButton.setVisibility(View.GONE);
                setSendWidgetAccessibilityTraversalOrder(SEND_WIDGET_MODE_SELF_AVATAR);
                break;

            case SEND_WIDGET_MODE_SIM_SELECTOR:
//                mSelfSendIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
//                mSelfSendIcon.setContentDescription(getSimContentDescription());
                setSendWidgetAccessibilityTraversalOrder(SEND_WIDGET_MODE_SIM_SELECTOR);
                break;

            case SEND_WIDGET_MODE_SEND_BUTTON:
                mMmsIndicator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
                mMmsIndicator.setContentDescription(null);
                setSendWidgetAccessibilityTraversalOrder(SEND_WIDGET_MODE_SEND_BUTTON);
                break;
        }
    }

    private String getSimContentDescription() {
        final SubscriptionListEntry sub = getSelfSubscriptionListEntry();
        if (sub != null) {
            return getResources().getString(
                    R.string.sim_selector_button_content_description_with_selection,
                    sub.displayName);
        } else {
            return getResources().getString(
                    R.string.sim_selector_button_content_description);
        }
    }

    // Set accessibility traversal order of the components in the send widget.
    private void setSendWidgetAccessibilityTraversalOrder(final int mode) {
        if (OsUtil.isAtLeastL_MR1()) {
       //     mAttachMediaButton.setAccessibilityTraversalBefore(R.id.compose_message_text);
            switch (mode) {
                case SEND_WIDGET_MODE_SIM_SELECTOR:
                  //  mComposeEditText.setAccessibilityTraversalBefore(R.id.send_message_button);
                    break;
                case SEND_WIDGET_MODE_SEND_BUTTON:
                 //   mComposeEditText.setAccessibilityTraversalBefore(R.id.send_message_button);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void afterTextChanged(final Editable editable) {
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count,
            final int after) {
        if (mHost.shouldHideAttachmentsWhenSimSelectorShown()) {
            hideSimSelector();
        }
    }

    private void hideSimSelector() {
        if (mInputManager.showHideSimSelector(false /* show */, true /* animate */)) {
            // Now that the sim selector has been hidden, reshow the attachments if they
            // have been hidden.
            Log.i("xxx","hideAttachmentsWhenShowingSims");
            hideAttachmentsWhenShowingSims(false /*simPickerVisible*/);
        }
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before,
            final int count) {
        final BugleActionBarActivity activity = (mOriginalContext instanceof BugleActionBarActivity)
                ? (BugleActionBarActivity) mOriginalContext : null;
        if (activity != null && activity.getIsDestroyed()) {
            LogUtil.v(LogUtil.BUGLE_TAG, "got onTextChanged after onDestroy");

            // if we get onTextChanged after the activity is destroyed then, ah, wtf
            // b/18176615
            // This appears to have occurred as the result of orientation change.
            return;
        }

        mBinding.ensureBound();
        updateVisualsOnDraftChanged();
    }

    @Override
    public PlainTextEditText getComposeEditText() {
        return mComposeEditText;
    }

    public void displayPhoto(final Uri photoUri, final Rect imageBounds) {
        mHost.displayPhoto(photoUri, imageBounds, true /* isDraft */);
    }

    public void updateConversationSelfIdOnExternalChange(final String selfId) {
        updateConversationSelfId(selfId, true /* notify */);
    }

    /**
     * The selfId of the conversation. As soon as the DraftMessageData successfully loads (i.e.
     * getSelfId() is non-null), the selfId in DraftMessageData is treated as the sole source
     * of truth for conversation self id since it reflects any pending self id change the user
     * makes in the UI.
     */
    public String getConversationSelfId() {
        return mBinding.getData().getSelfId();
    }

    public void selectSim(SubscriptionListEntry subscriptionData) {
        final String oldSelfId = getConversationSelfId();
        final String newSelfId = subscriptionData.selfParticipantId;
        Assert.notNull(newSelfId);
        // Don't attempt to change self if self hasn't been loaded, or if self hasn't changed.
        if (oldSelfId == null || TextUtils.equals(oldSelfId, newSelfId)) {
            return;
        }
        updateConversationSelfId(newSelfId, true /* notify */);
        updateRcsInput(); // juphoon
        if (getConversationSubId() != RcsServiceManager.getSubId()) { // juphoon
            updateVisualsOnDraftChanged();
        }
    }

    public void hideAllComposeInputs(final boolean animate) {
        mInputManager.hideAllInputs(animate);
    }

    public void saveInputState(final Bundle outState) {
        mInputManager.onSaveInputState(outState);
    }

    public void resetMediaPickerState() {
        mInputManager.resetMediaPickerState();
    }


    int isSecond = 1;

    public boolean onBackPressed() {
        Log.i("mmm","asdxzcxzas");
        if (!isFirst && BugleApplication.getInstance().isChose()) {
            Log.i("mmm", "isFirst回退进来了");
            BugleApplication.getInstance().setIsblank(0);
            mAttachMediaButton.setFocusable(true);
            mAttachMediaButton.setFocusableInTouchMode(true);
            mAttachMediaButton.requestFocus();
            if (haveData) {
                mAttachmentPreview.onWindowVisibilityChanged(View.GONE);
            }
            isFirst = true;
            BugleApplication.getInstance().setChose(false);

        }else{
            mInputManager.onBackPressed();
            //退出重置标签
            isFirst_count = 0;
        }
        inputs.setVisibility(VISIBLE);
//        Log.i("mmm","mInputManager.onBackPressed:+"+mInputManager.onBackPressed());
        return mInputManager.onBackPressed();


    }

    public boolean onNavigationUpPressed() {
        return mInputManager.onNavigationUpPressed();
    }

    public boolean updateActionBar(final ActionBar actionBar) {
        return mInputManager != null ? mInputManager.updateActionBar(actionBar) : false;
    }

    public static boolean shouldShowSimSelector(final ConversationData convData) {
        return OsUtil.isAtLeastL_MR1() &&
                convData.getSelfParticipantsCountExcludingDefault(true /* activeOnly */) > 1;
    }

    public void sendMessageIgnoreMessageSizeLimit() {
        sendMessageInternal(false /* checkMessageSize */);
    }

    public void onAttachmentPreviewLongClicked() {
        mHost.showAttachmentChooser();
    }

    @Override
    public void onDraftAttachmentLoadFailed() {
        mHost.notifyOfAttachmentLoadFailed();
    }

    private boolean isOverriddenAvatarAGroup() {
        final Uri overridenSelfUri = mHost.getSelfSendButtonIconUri();
        if (overridenSelfUri == null) {
            return false;
        }
        return AvatarUriUtil.TYPE_GROUP_URI.equals(AvatarUriUtil.getAvatarType(overridenSelfUri));
    }

    @Override
    public void setAccessibility(boolean enabled) {
        if (enabled) {
            mAttachMediaButton.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            mComposeEditText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            mSendButton.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setSendButtonAccessibility(mSendWidgetMode);
        } else {
         //   mSelfSendIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            mComposeEditText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            mSendButton.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            mAttachMediaButton.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    //juphoon
    public final ComposeMessageView.IMediaChoose iMediaChoose = new ComposeMessageView.IMediaChoose() {
        @Override
        public void onMediaActivityResult(int requestCode, int resultCode, Intent data) {
            Log.i("nnn","requestCode---:"+requestCode);
            switch (requestCode) {
                case REQUEST_SEND_IMAGE_URI:
                    Log.i("nnn","REQUEST_SEND_IMAGE_URI");
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        Uri uri = data.getData();
                        MessagePartData partData = MessagePartData.createMediaMessagePart(ContentType.IMAGE_UNSPECIFIED, uri,
                                MessagePartData.UNSPECIFIED_SIZE, MessagePartData.UNSPECIFIED_SIZE);
                        String filePath = RcsFileUtils.getFilePathByUri(getContext(), partData.getContentUri());
                        File file = null;
                        if (TextUtils.isEmpty(filePath)) {
                            file = MediaScratchFileProvider.getFileFromUri(partData.getContentUri());
                        } else {
                            file = new File(filePath);
                        }
                        if (file.exists() && file.length() > IMAGE_SEND_MAX_LIMIT_SIZE) {
                            Toast.makeText(getContext(), R.string.file_over_max, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayList<MessagePartData> list = new ArrayList<MessagePartData>();
                        list.add(partData);
                        mBinding.getData().addAttachments(list);
                        writeDraftMessage();
                        requestDraftMessage(true);
                    }
                    break;
                case REQUEST_CODE_NOTIFY_FILE:
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        Uri uri = data.getData();
                        File file;
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                            String path = RcsFileUtils.getFilePathByUri(mOriginalContext, uri);
                            String type = MimeTypeMap.getFileExtensionFromUrl(path);
                            if (type.contains("apk")) {
                                Toast.makeText(getContext(), R.string.chatbot_file_type_support, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (!TextUtils.isEmpty(path)) {
                                mExternalViewController.setMoreChooserMessage(RcsMoreChooser.ITEM_SEND_FILE, path);
                                requestDraftMessage(false);
                            }
                            if (TextUtils.isEmpty(path) || !new File(path).exists()) {
                                String fileName = RcsMmsUtils.getFileNameFromContentUri(uri, getContext());
                                path = RmsDefine.RMS_FILE_PATH + "/" + fileName;
                                if (path.endsWith("apk")) {
                                    Toast.makeText(getContext(), R.string.chatbot_file_type_support, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                file = new File(path);
                                try {
                                    RcsMmsUtils.copyToFile(getContext().getContentResolver().openInputStream(uri), file);
                                    mExternalViewController.setMoreChooserMessage(RcsMoreChooser.ITEM_SEND_FILE, path);
                                    requestDraftMessage(false);
                                } catch (FileNotFoundException e) {
                                    Toast.makeText(getContext(), R.string.chatbot_file_not_found, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), R.string.chatbot_file_not_found, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                    break;
                case REQUEST_CODE_SEND_AUDIO:
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        Uri uri = data.getData();
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                            String path = RcsFileUtils.getFilePathByUri(mOriginalContext, uri);
                            if (!TextUtils.isEmpty(path)) {
                                mExternalViewController.setMoreChooserMessage(RcsMoreChooser.ITEM_SEND_AUDIO, path);
                            } else {
                                Toast.makeText(getContext(), R.string.chatbot_file_not_found, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    break;
                case REQUEST_CODE_SEND_VIDEO:
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        Uri uri = data.getData();
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                            String path = RcsFileUtils.getFilePathByUri(mOriginalContext, uri);
                            mExternalViewController.setMoreChooserMessage(RcsMoreChooser.ITEM_SEND_VIDEO, path);
                        }
                    }
                    break;
                case REQUEST_CODE_SEND_VCARD:
                    if (data != null) {
                        Uri uri = RcsVCardHelper.exportVcard(getContext(), data.getData());
                        if (uri != null) {
                            String filePath = RcsFileUtils.getFilePathByUri(getContext(), uri);
                            mExternalViewController.setMoreChooserMessage(RcsMoreChooser.ITEM_SEND_VCARD, filePath);
                        }
                    }
                    break;
                case REQUEST_CODE_SEND_GEO:
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        mExternalViewController.setMoreChooserMessage(RcsMoreChooser.ITEM_SEND_GEO, data.getStringExtra(RcsBaiduLocationActivity.LOCATION_FILE_PATH));
                        requestDraftMessage(false);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * juphoon转彩信提示
     */
    private void notifyTransToMms(final RcsMessageTransHelper.RcsFailMessage result) {
        if (RcsMmsUtils.getFirstTransMessage()) {
//            if (!Settings.canDrawOverlays(getContext())) {
//                UiUtils.showToastAtBottom(R.string.no_can_draw_overlays_permission);
//                return;
//            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.auto_msg_trans_send);
            builder.setNegativeButton(R.string.rcs_message_use_setting, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    RcsMmsUtils.startRcsSettingActivity();
                }

            });
            builder.setPositiveButton(R.string.rcs_message_use_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    if (result.mIsFile) {
                        transMmsByTransId(result);
                    } else {
                        transMmsByImdn(result);
                    }
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
        } else {
            if (result.mIsFile) {
                transMmsByTransId(result);
            } else {
                transMmsByImdn(result);
            }
        }
    }

    /**
     * juphoon提示第一次使用rcs
     */
    private void notifyUseRcs() {
//        JuphoonStyleDialog.showDialog(getContext(), getContext().getString(R.string.rcs_message_use_tip), null, getContext().getString(R.string.rcs_message_use_setting), new JuphoonStyleDialog.INegativeListener() {
//            @Override
//            public void onClick(View v) {
//                RcsMmsUtils.startRcsSettingActivity();
//            }
//        }, getContext().getString(R.string.rcs_message_use_ok), null);
    }

    /**
     * 判断是否有需要转彩信的消息
     */
    private void checkNeedTransMms() {
        synchronized (mFailMsgs) {
            if (mFailMsgs.size() > 0) {
                RcsMessageTransHelper.RcsFailMessage message = mFailMsgs.get(0);
                mFailMsgs.remove(0);
                if (message.mIsFile) {
                    transMmsByTransId(message);
                } else {
                    transMmsByImdn(message);
                }
            }
        }
    }

    /**
     * 将失败的文件消息转成彩信,进入编辑模式
     * 
     * @param message
     */
    private void transMmsByTransId(final RcsMessageTransHelper.RcsFailMessage message) {
        if (message != null && message.mIsFile) {
            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... arg0) {
                    Cursor cursor = mOriginalContext.getContentResolver().query(RmsDefine.Rms.CONTENT_URI_LOG, new String[] { RmsDefine.Rms._ID }, RmsDefine.Rms.TRANS_ID + "=?", new String[] { message.mId }, null);
                    String conversationId = null;
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                DatabaseWrapper db = DataModel.get().getDatabase();
                                long rmsMsgId = cursor.getLong(0);
                                Uri rmsUri = ContentUris.withAppendedId(RmsDefine.Rms.CONTENT_URI_LOG, rmsMsgId);
                                mOriginalContext.getContentResolver().delete(rmsUri, null, null);
                                MessageData messageData = BugleDatabaseOperations.readMessageData(db, rmsUri);
                                String msgId = messageData.getMessageId();
                                conversationId = messageData.getConversationId();
                                BugleDatabaseOperations.deleteMessage(db, msgId);
                                mOriginalContext.getContentResolver().delete(RmsDefine.Rms.CONTENT_URI_LOG, RmsDefine.Rms.TRANS_ID + "=?", new String[] { message.mId });
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                    return conversationId;
                }

                @Override
                protected void onPostExecute(String result) {
                    MessagePartData partData = MessagePartData.createMediaMessagePart(RcsMmsUtils.getContentType(message.mFileType), Uri.parse("file://" + message.mFilePath),
                            MessagePartData.UNSPECIFIED_SIZE, MessagePartData.UNSPECIFIED_SIZE);
                    ArrayList<MessagePartData> list = new ArrayList<MessagePartData>();
                    list.add(partData);
                    mBinding.getData().addAttachments(list);
                    mBinding.getData().setMmsModel(true);
                    updateVisualsOnDraftChanged();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * 将失败的大文本消息转成彩信,进入编辑模式
     * 
     * @param message
     */
    private void transMmsByImdn(final RcsMessageTransHelper.RcsFailMessage message) {
        if (message != null && !message.mIsFile) {
            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... arg0) {
                    Cursor cursor = mOriginalContext.getContentResolver().query(RmsDefine.Rms.CONTENT_URI_LOG, new String[] { RmsDefine.Rms._ID }, RmsDefine.Rms.IMDN_STRING + "=?", new String[] { message.mId }, null);
                    String conversationId = null;
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                DatabaseWrapper db = DataModel.get().getDatabase();
                                long rmsMsgId = cursor.getLong(0);
                                Uri rmsUri = ContentUris.withAppendedId(RmsDefine.Rms.CONTENT_URI_LOG, rmsMsgId);
                                mOriginalContext.getContentResolver().delete(rmsUri, null, null);
                                MessageData messageData = BugleDatabaseOperations.readMessageData(db, rmsUri);
                                String msgId = messageData.getMessageId();
                                conversationId = messageData.getConversationId();
                                BugleDatabaseOperations.deleteMessage(db, msgId);
                                mOriginalContext.getContentResolver().delete(RmsDefine.Rms.CONTENT_URI_LOG, RmsDefine.Rms.IMDN_STRING + "=?", new String[] { message.mId });
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                    return conversationId;
                }

                @Override
                protected void onPostExecute(String result) {
                    if (!TextUtils.isEmpty(result)) {
                        mComposeSubjectText.setText(R.string.rcs_text_message_trans_to_mms);
                        mComposeEditText.setText(message.mText);
                        mComposeEditText.setSelection(message.mText.length());
                        mBinding.getData().setMmsModel(true);
                        updateVisualsOnDraftChanged();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * 获取会话subid
     */
    private int getConversationSubId() {
        return mHost.getConversationSelfSubId() == ParticipantData.DEFAULT_SELF_SUB_ID ?
                PhoneUtils.getDefault().getDefaultSmsSubscriptionId() : mHost.getConversationSelfSubId();
    }

    /**
     * pc 公众账号 群会话
     * @return
     */
    private boolean isOnlyRcsConveration() {
        return mConversationDataModel.getData() != null &&
                (mConversationDataModel.getData().getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD ||
                TextUtils.equals(mConversationDataModel.getData().getOtherParticipantNormalizedDestination(), RmsDefine.PC_ADDRESS)); 
    }

    /** juphoon 更新输入框和附件按钮状态 **/
    private void updateRcsInput() {
        if (isOnlyRcsConveration()) {
            if (getConversationSubId() == RcsServiceManager.getSubId()) {
                mComposeEditText.setFocusable(true);
                mComposeEditText.setFocusableInTouchMode(true);
                mComposeEditText.requestFocus();
                mAttachMediaButton.setClickable(true);
            } else {
                mComposeEditText.setFocusable(false);
                mAttachMediaButton.setClickable(false);
            }
        }
    }

    /**
     * 增加原图附件，
     */
    private boolean addOriginalPicture(MessagePartData msgPartData) {
        if (msgPartData != null) {
            if (ContentType.isImageType(msgPartData.getContentType())) {
                String filePath = RcsFileUtils.getFilePathByUri(getContext(), msgPartData.getContentUri());
                File file = null;
                if (TextUtils.isEmpty(filePath)) {
                    file = MediaScratchFileProvider.getFileFromUri(msgPartData.getContentUri());
                } else {
                    file = new File(filePath);
                }
                if (file.exists() && file.length() > IMAGE_SEND_LIMIT_SIZE) {
                    Intent intent = new Intent(mOriginalContext, RcsPreviewImageActivity.class);
                    intent.putExtra(RcsPreviewImageActivity.FILE_URI, msgPartData.getContentUri());
                    ActivityCompat.startActivityForResult(UiUtils.getActivity(getContext()), intent, REQUEST_SEND_IMAGE_URI, null);
//                    UiUtils.getActivity(getContext()).startActivityForResult(intent, REQUEST_SEND_IMAGE_URI);
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * juphoon
     */
    @Override
    public void onExternalViewListener(
            MediaPicker.ExternalViewController externalViewListener) {
        if (mExternalViewController == null) {
            mExternalViewController = externalViewListener;
            mExternalViewController.setOnClickListener(new MediaPicker.ExternalViewClick() {
                @Override
                public void onItemClick(String itemTag) {
                    if (itemTag.equals(RcsMoreChooser.ITEM_SEND_FILE)) {
                        Log.i("cccc","其他的");
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");// 无类型限制
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        UiUtils.getActivity(getContext()).startActivityForResult(intent, REQUEST_CODE_NOTIFY_FILE);
                    } else if (itemTag.equals(RcsMoreChooser.ITEM_SEND_AUDIO)) {
                        Log.i("cccc","音频。。。。。。。。。");
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType(ContentType.AUDIO_UNSPECIFIED);// 音频
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        UiUtils.getActivity(getContext()).startActivityForResult(intent, REQUEST_CODE_SEND_AUDIO);
                    } else if (itemTag.equals(RcsMoreChooser.ITEM_SEND_VIDEO)) {
                        Log.i("cccc","视频。。。。。。。。。");
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType(ContentType.VIDEO_UNSPECIFIED);// 视频
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        UiUtils.getActivity(getContext()).startActivityForResult(intent, REQUEST_CODE_SEND_VIDEO);
                    } else if (itemTag.equals(RcsMoreChooser.ITEM_SEND_VCARD)) {
                        Log.i("cccc","图片。。。。。。。。。");
                        Intent intent = new Intent(getContext(), RcsChooseContactActivity.class);
                        UiUtils.getActivity(getContext()).startActivityForResult(intent, REQUEST_CODE_SEND_VCARD);
                    } else if (itemTag.equals(RcsMoreChooser.ITEM_SEND_GEO)){
                        Log.i("cccc","地理位置的拉。。。。。。。。。");
                        Intent intent = new Intent(getContext(), RcsBaiduLocationActivity.class);
                        UiUtils.getActivity(getContext()).startActivityForResult(intent, REQUEST_CODE_SEND_GEO);
                    }
                    hideAllComposeInputs(true);
                }
            });
        }
    }

    private boolean mHasDealFirstSendChat = false;

    private void checkChatbotSendFirstMessage() {
        //已登录Rcs 输入框已存在焦点 Model不为空 只发一次
        if (RcsServiceManager.isLogined() && mConversationDataModel != null && mComposeEditText.hasFocus() && !mHasDealFirstSendChat) {
            String address = mConversationDataModel.getData().getOtherParticipantNormalizedDestination();
            final String conversationId = mBinding.getData().getConversationId();
            if (RcsChatbotHelper.isChatbotByServiceId(address)) {
                mHasDealFirstSendChat = true;
                // 判断没有消息
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor cursor = DataModel.get().getDatabase().rawQuery(
                                ConversationMessageData.getConversationMessageIdsQuerySql(),
                                new String [] { conversationId });
                        if (cursor != null) {
                            if (cursor.getCount() == 0) {
                                ParticipantData participantData = BugleDatabaseOperations.getOrCreateSelf(DataModel.get().getDatabase(), RcsServiceManager.getSubId());
                                MessageData messageData = MessageData.createDraftRmsMessage(conversationId, participantData.getId(), "你好");
                                InsertNewMessageAction.insertNewMessage(messageData);
                            }
                            cursor.close();
                        }
                    }
                }).start();
            }
        }
    }

    public void setBroadcastSupport(boolean support){
        mSupportBroadcast = support;
    }

}
