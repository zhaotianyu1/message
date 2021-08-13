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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.text.BidiFormatter;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.binding.ImmutableBindingRef;
import com.android.messaging.datamodel.data.ConversationData;
import com.android.messaging.datamodel.data.ConversationData.ConversationDataListener;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.ConversationParticipantsData;
import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.datamodel.data.DraftMessageData.DraftMessageDataListener;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.datamodel.data.SubscriptionListData.SubscriptionListEntry;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.ui.SearchActivity;
import com.android.messaging.ui.SnackBar;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.UIIntentsImpl;
import com.android.messaging.ui.contact.AddContactsConfirmationDialog;
import com.android.messaging.ui.conversation.ComposeMessageView.IComposeMessageViewHost;
import com.android.messaging.ui.conversation.ConversationInputManager.ConversationInputHost;
import com.android.messaging.ui.conversation.ConversationMessageView.ConversationMessageViewHost;
import com.android.messaging.ui.mediapicker.MediaPicker;
import com.android.messaging.util.AccessibilityUtil;
import com.android.messaging.util.Assert;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.ChangeDefaultSmsAppHelper;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.ImeUtil;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.SafeAsyncTask;
import com.android.messaging.util.TextUtil;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;
import com.google.common.annotations.VisibleForTesting;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbotmaap.RcsChatbotComplaintActivity;
import com.juphoon.chatbotmaap.bottomMenu.RcsChatbotMenuView;
import com.juphoon.chatbotmaap.chatbotDetail.RcsChatBotDetailActivity;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.helper.mms.RcsDatabaseMessages;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.RcsMsgItemTouchHelper;
import com.juphoon.helper.mms.RcsRecipientHelper;
import com.juphoon.helper.mms.ui.RcsBaiduLocationActivity;
import com.juphoon.helper.mms.ui.RcsGroupDetailActivity;
import com.juphoon.helper.mms.ui.RcsPeopleActivity;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsServiceConstants;
import com.juphoon.service.RmsDefine;
import com.juphoon.ui.JuphoonStyleDialog;
import com.tcl.ff.component.animer.glow.view.AllCellsGlowLayout;
import com.tcl.uicompat.TCLButton;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Shows a list of messages/parts comprising a conversation.
 */
public class ConversationFragment extends Fragment implements ConversationDataListener,
        IComposeMessageViewHost, ConversationMessageViewHost, ConversationInputHost,
        DraftMessageDataListener {

    public interface ConversationFragmentHost extends ImeUtil.ImeStateHost {
        void onStartComposeMessage();
        void onConversationMetadataUpdated();
        boolean shouldResumeComposeMessage();
        void onFinishCurrentConversation();
        void invalidateActionBar();
        ActionMode startActionMode(ActionMode.Callback callback);
        void dismissActionMode();
        ActionMode getActionMode();
        void onConversationMessagesUpdated(int numberOfMessages);
        void onConversationParticipantDataLoaded(int numberOfParticipants);
        boolean isActiveAndFocused();
    }

    public static final String FRAGMENT_TAG = "conversation";

    static final int REQUEST_CHOOSE_ATTACHMENTS = 2;
    private static final int JUMP_SCROLL_THRESHOLD = 15;
    // We animate the message from draft to message list, if we the message doesn't show up in the
    // list within this time limit, then we just do a fade in animation instead
    public static final int MESSAGE_ANIMATION_MAX_WAIT = 500;

    private ComposeMessageView mComposeMessageView;
    private RecyclerView mRecyclerView;
    private ConversationMessageAdapter mAdapter;
    private ConversationFastScroller mFastScroller;

    private View mConversationComposeDivider;
    private ChangeDefaultSmsAppHelper mChangeDefaultSmsAppHelper;

    private String mConversationId;
    // If the fragment receives a draft as part of the invocation this is set
    private MessageData mIncomingDraft;

    // This binding keeps track of our associated ConversationData instance
    // A binding should have the lifetime of the owning component,
    //  don't recreate, unbind and bind if you need new data
    @VisibleForTesting
    final Binding<ConversationData> mBinding = BindingBase.createBinding(this);

    // Saved Instance State Data - only for temporal data which is nice to maintain but not
    // critical for correctness.
    private static final String SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY = "conversationViewState";
    private Parcelable mListState;

    private ConversationFragmentHost mHost;

    protected List<Integer> mFilterResults;

    // The minimum scrolling distance between RecyclerView's scroll change event beyong which
    // a fling motion is considered fast, in which case we'll delay load image attachments for
    // perf optimization.
    private int mFastFlingThreshold;

    // ConversationMessageView that is currently selected
    // juphoon ConversationMessageView that is currently selected
//    private ConversationMessageView mSelectedMessage; juphoon
    private HashSet<Integer> mSelectMessageSet = new HashSet<>(); //juphoon

    // Attachment data for the attachment within the selected message that was long pressed
    private MessagePartData mSelectedAttachment;

    // Normally, as soon as draft message is loaded, we trust the UI state held in
    // ComposeMessageView to be the only source of truth (incl. the conversation self id). However,
    // there can be external events that forces the UI state to change, such as SIM state changes
    // or SIM auto-switching on receiving a message. This receiver is used to receive such
    // local broadcast messages and reflect the change in the UI.
    private final BroadcastReceiver mConversationSelfIdChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String conversationId =
                    intent.getStringExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID);
            final String selfId =
                    intent.getStringExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_SELF_ID);
            Assert.notNull(conversationId);
            Assert.notNull(selfId);
            if (TextUtils.equals(mBinding.getData().getConversationId(), conversationId)) {
                mComposeMessageView.updateConversationSelfIdOnExternalChange(selfId);
            }
        }
    };

    // Flag to prevent writing draft to DB on pause
    private boolean mSuppressWriteDraft;

    // Indicates whether local draft should be cleared due to external draft changes that must
    // be reloaded from db
    private boolean mClearLocalDraft;
    private ImmutableBindingRef<DraftMessageData> mDraftMessageDataModel;

    /**
     * juphoon
     **/
    private SearchView mSearchView;
    private MenuItem mSearchItem;
    private SearchView.SearchAutoComplete mSearchEditText;
    public static final int SEARCH_MAX_LENGTH = 512;

    private RcsChatbotMenuView mRcsChatbotMenuView;
    private ImageButton mShowChatbotMenu;
    private ImageButton mHideChatbotMenu;
    private boolean mIsShowChatbotMenu = true;
    private ImageButton mShowPublicMenu;
    private ImageButton mHidePublicMenu;
    private boolean mIsShowMenu = true;
    private boolean mMenuSelectAll = false;

    private TCLButton conversiton_back;


    private View.OnClickListener mShowOrHideChatbotClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.show_chatbot_menu:
                    Log.i("qwe","点击多选按钮-------------------");
                    String address = mBinding.getData().getOtherParticipantNormalizedDestination();
                    RcsChatbotHelper.RcsChatbot chatbot = RcsChatbotHelper.getChatbotInfoBySmsOrServiceId(address);
                    mIsShowChatbotMenu = true;
                    Log.i("oneone","updateGroupStatus----3");
                    mComposeMessageView.setVisibility(View.GONE);
                    mRcsChatbotMenuView.setVisibility(View.VISIBLE);
                    mRcsChatbotMenuView.bind(RcsChatbotMenuView.pauseFromJson(chatbot.persistentMenu),mConversationId,chatbot.serviceId);
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(ConversationActivity.INPUT_METHOD_SERVICE);
                    if (imm != null && getActivity().getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                    }
                    mHideChatbotMenu.setFocusable(true);
                    mHideChatbotMenu.setFocusableInTouchMode(true);
                    mHideChatbotMenu.requestFocus();
                    break;
                case R.id.hide_chatbot_menu:
                    Log.i("qwe","返回吧-------------------");
//                    BugleApplication.getInstance().setGong(true);
                    mIsShowChatbotMenu = false;
                    Log.i("oneone","updateGroupStatus----4");
                    mComposeMessageView.setVisibility(View.VISIBLE);
                    mRcsChatbotMenuView.setVisibility(View.GONE);

                    mShowChatbotMenu.setFocusable(true);
                    mShowChatbotMenu.setFocusableInTouchMode(true);
                    mShowChatbotMenu.requestFocus();
                    break;

                default:
                    break;
            }
        }

    };

    private boolean isScrolledToBottom() {
        if (mRecyclerView.getChildCount() == 0) {
            Log.i("fragment","isScrolledToBottom --- true");
            return true;
        }
        final View lastView = mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1);
        int lastVisibleItem = ((LinearLayoutManager) mRecyclerView
                .getLayoutManager()).findLastVisibleItemPosition();
        if (lastVisibleItem < 0) {
            // If the recyclerView height is 0, then the last visible item position is -1
            // Try to compute the position of the last item, even though it's not visible
            final long id = mRecyclerView.getChildItemId(lastView);
            final RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForItemId(id);
            if (holder != null) {
                lastVisibleItem = holder.getAdapterPosition();
            }
        }
        final int totalItemCount = mRecyclerView.getAdapter().getItemCount();
        final boolean isAtBottom = (lastVisibleItem + 1 == totalItemCount);
        return isAtBottom && lastView.getBottom() <= mRecyclerView.getHeight();
    }

    private void scrollToBottom(final boolean smoothScroll) {
        if (mAdapter.getItemCount() > 0) {
            scrollToPosition(mAdapter.getItemCount() - 1, smoothScroll);
        }
    }

    private int mScrollToDismissThreshold;
    //juphoon
    private final RecyclerView.OnScrollListener mListScrollListener =
        new RecyclerView.OnScrollListener() {
            // Keeps track of cumulative scroll delta during a scroll event, which we may use to
            // hide the media picker & co.
            private int mCumulativeScrollDelta;
            private boolean mScrollToDismissHandled;
            private boolean mWasScrolledToBottom = true;
            private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;

            @Override
            public void onScrollStateChanged(final RecyclerView view, final int newState) {
                /**
                 * by juphoon 消息拖影问题
                 */
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    // Reset scroll states.
//                    mCumulativeScrollDelta = 0;
//                    mScrollToDismissHandled = false;
//                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
//                    mRecyclerView.getItemAnimator().endAnimations();
//                }
//                mScrollState = newState;
            }

            @Override
            public void onScrolled(final RecyclerView view, final int dx, final int dy) {
                /**
                 * by juphoon 消息拖影问题
                 */
//                if (mScrollState == RecyclerView.SCROLL_STATE_DRAGGING &&
//                        !mScrollToDismissHandled) {
//                    mCumulativeScrollDelta += dy;
//                    // Dismiss the keyboard only when the user scroll up (into the past).
//                    if (mCumulativeScrollDelta < -mScrollToDismissThreshold) {
//                        mComposeMessageView.hideAllComposeInputs(false /* animate */);
//                        mScrollToDismissHandled = true;
//                    }
//                }
//                if (mWasScrolledToBottom != isScrolledToBottom()) {
//                    mConversationComposeDivider.animate().alpha(isScrolledToBottom() ? 0 : 1);
//                    mWasScrolledToBottom = isScrolledToBottom();
//                }
            }
    };

    private final ActionMode.Callback mMessageActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {
            // juphoon 为界面多选，全选做改动
            if (mSelectMessageSet.size() == 1) {
                final ConversationMessageData data = getOneSelectConversationMsgData();
                final boolean isRcsVisible = data.getIsRms(); // juphoon
                // 转发判断消息类型
                final boolean isGeoMessage = data.getParts().get(0).isGeo();
                final MenuInflater menuInflater = getActivity().getMenuInflater();
                menuInflater.inflate(R.menu.conversation_fragment_select_menu, menu);
                menu.findItem(R.id.action_download).setVisible(data.getShowDownloadMessage() && !isRcsVisible);
                menu.findItem(R.id.action_send).setVisible(data.getShowResendMessage());
                menu.findItem(R.id.details_menu).setVisible(true);
                //juphoon
                // ShareActionProvider does not work with ActionMode. So we use
                // a normal menu item.
                menu.findItem(R.id.share_message_menu)
                        .setVisible(data.getCanForwardMessage() && data.getParts().get(0).isText());
                menu.findItem(R.id.save_attachment).setVisible(mSelectedAttachment != null && !isRcsVisible
                        || (isRcsVisible && (data.getParts().get(0).isVideo() || data.getParts().get(0).isImage())
                        && (data.getStatus() == MessageData.BUGLE_STATUS_INCOMING_COMPLETE
                        || data.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE)));
                menu.findItem(R.id.forward_message_menu).setVisible(data.getCanForwardMessage() && !data.getParts().get(0).isAudio() && !data.getParts().get(0).isChatbotCard());
                // TODO: We may want to support copying attachments in the
                // future, but it's
                // unclear which attachment to pick when we make this context
                // menu at the message level
                // instead of the part level
                menu.findItem(R.id.copy_text).setVisible(data.getCanCopyMessageToClipboard());

                // juphoon
                menu.findItem(R.id.action_select_message).setIcon(mMenuSelectAll ? getResources().getDrawable(R.drawable.select_all)
                        : getResources().getDrawable(R.drawable.chatbot_all));
                menu.findItem(R.id.action_complain_message).setVisible(RcsChatbotHelper.isChatbotByServiceId(mBinding.getData().getOtherParticipantNormalizedDestination())
                        && data.getIsRms() && data.getIsIncoming());

                return true;
            } else {
                final MenuInflater menuInflater = getActivity().getMenuInflater();
                menuInflater.inflate(R.menu.conversation_fragment_select_menu, menu);
                menu.findItem(R.id.action_download).setVisible(false);
                menu.findItem(R.id.action_send).setVisible(false);
                menu.findItem(R.id.details_menu).setVisible(false);
                menu.findItem(R.id.share_message_menu).setVisible(false);
                menu.findItem(R.id.save_attachment).setVisible(false);
                menu.findItem(R.id.forward_message_menu).setVisible(false);
                menu.findItem(R.id.copy_text).setVisible(false);
                menu.findItem(R.id.action_select_message).setVisible(true);
                menu.findItem(R.id.action_delete_message).setVisible(mSelectMessageSet.size() == 0 ?
                        false : true);
                menu.findItem(R.id.action_select_message)
                        .setIcon(mMenuSelectAll ? getResources().getDrawable(R.drawable.select_all)
                                : getResources().getDrawable(R.drawable.chatbot_all));
                menu.findItem(R.id.action_complain_message).setVisible(false);
                return true;
            }
        }

        @Override
        public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
            return true;
        }

        //juphoon
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
            if (mSelectMessageSet.size() == 1) {
                final ConversationMessageData data = getOneSelectConversationMsgData();
                final String messageId = data.getMessageId();
                switch (menuItem.getItemId()) {
                    case R.id.save_attachment:
                        if (OsUtil.hasStoragePermission()) {
                            final SaveAttachmentTask saveAttachmentTask = new SaveAttachmentTask(
                                    getActivity());
                            for (final MessagePartData part : data.getAttachments()) {
                                saveAttachmentTask.addAttachmentToSave(part.getContentUri(),
                                        part.getContentType());
                            }
                            if (saveAttachmentTask.getAttachmentCount() > 0) {
                                saveAttachmentTask.executeOnThreadPool();
                                dismissActionMode();
                            }
                        } else {
                            getActivity().requestPermissions(
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        }
                        return true;
                    case R.id.action_download:
                        retryDownload(messageId);
                        mHost.dismissActionMode();
                        return true;
                    case R.id.action_send:
                        //juphoon
//                        if (mSelectedMessage != null) {
                        retrySend(messageId);
                        mHost.dismissActionMode();
//                        }
                        return true;
                    case R.id.copy_text:
                        Assert.isTrue(data.hasText());
                        final ClipboardManager clipboard = (ClipboardManager) getActivity()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(
                                ClipData.newPlainText(null /* label */, data.getText()));
                        mHost.dismissActionMode();
                        return true;
                    case R.id.details_menu:
                        MessageDetailsDialog.show(
                                getActivity(), data, mBinding.getData().getParticipants(),
                                mBinding.getData().getSelfParticipantById(data.getSelfParticipantId()));
                        mHost.dismissActionMode();
                        return true;
                    case R.id.action_complain_message:
                        RcsDatabaseMessages.RmsMessage rms = RcsMmsUtils.loadRms(Uri.parse(data.getSmsMessageUri()));
                        Intent intent = new Intent(getContext(), RcsChatbotComplaintActivity.class);
                        intent.putExtra(RcsChatbotComplaintActivity.SERVICEID, rms.mRmsChatbotServiceId);
                        intent.putExtra(RcsChatbotComplaintActivity.IMDN, rms.mImdn);
                        intent.putExtra(RcsChatbotComplaintActivity.BODY,rms.mBody);
                        startActivity(intent);

                        return true;
                    case R.id.share_message_menu:
                        shareMessage(data);
                        mHost.dismissActionMode();
                        return true;
                    case R.id.forward_message_menu:
                        // TODO: Currently we are forwarding one part at a time, instead of
                        // the entire message. Change this to forwarding the entire message when we
                        // use message-based cursor in conversation.
                        final MessageData message = mBinding.getData().createForwardedMessage(data);
                        UIIntents.get().launchForwardMessageActivity(getActivity(), message);
                        mHost.dismissActionMode();
                        return true;
                }
            }
            //juphoon
            switch (menuItem.getItemId()) {
                // juphoon
                case R.id.action_delete_message:
                    Log.i("xxx", "action_delete_message");
                    if (mSelectMessageSet.size() > 0) {
                        deleteMessage(mSelectMessageSet);
                        mSelectMessageSet.clear();
                        updateMenuSelectAll();
                    }
                    return true;
                case R.id.action_select_message: // 全选 juphoon
                    Log.i("xxx", "action_select_message");
                    if (!mMenuSelectAll) {
                        menuItem.setTitle(getResources().getString(R.string.menu_cancel_all));
                        menuItem.setIcon(getResources().getDrawable(R.drawable.select_all));
                        for (int i = 0; i < mAdapter.getItemCount(); i++) {
                            mSelectMessageSet.add(i);
                        }
                    } else {
                        menuItem.setTitle(getResources().getString(R.string.menu_select_all));
                        menuItem.setIcon(getResources().getDrawable(R.drawable.chatbot_all));
                        mSelectMessageSet.clear();
                    }
                    updateMenuSelectAll();
                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        mAdapter.updateCheckBoxState(i, mMenuSelectAll, false);
                    }
                    invalidateOptionsMenu();
                    return true;
            }
            //juphoon
            mSelectMessageSet.clear(); // home事件
            mAdapter.showCheckBox(false);
            return false;
        }

        //juphoon
        private void forwardMessage(String smscontent) {
            MessageData forwardedMessage = new MessageData();
            MessagePartData forwardedPart;
            forwardedPart = MessagePartData.createTextMessagePart(smscontent);
            forwardedMessage.addPart(forwardedPart);
            UIIntents.get().launchForwardMessageActivity(getActivity(), forwardedMessage);
            dismissActionMode();
        }

        private void shareMessage(final ConversationMessageData data) {
            // Figure out what to share.
            MessagePartData attachmentToShare = mSelectedAttachment;
            // If the user long-pressed on the background, we will share the text (if any)
            // or the first attachment.
            if (mSelectedAttachment == null
                    && TextUtil.isAllWhitespace(data.getText())) {
                final List<MessagePartData> attachments = data.getAttachments();
                if (attachments.size() > 0) {
                    attachmentToShare = attachments.get(0);
                }
            }

            final Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            if (attachmentToShare == null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, data.getText());
                shareIntent.setType("text/plain");
            } else {
                shareIntent.putExtra(
                        Intent.EXTRA_STREAM, attachmentToShare.getContentUri());
                shareIntent.setType(attachmentToShare.getContentType());
            }
            final CharSequence title = getResources().getText(R.string.action_share);
            startActivity(Intent.createChooser(shareIntent, title));
        }

        @Override
        public void onDestroyActionMode(final ActionMode actionMode) {
            selectMessage(null);
        }
    };

    /**
     * {@inheritDoc} from Fragment
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFastFlingThreshold = getResources().getDimensionPixelOffset(
                R.dimen.conversation_fast_fling_threshold);
        mAdapter = new ConversationMessageAdapter(getActivity(), null, this,
                null,
                // Sets the item click listener on the Recycler item views.
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        mSelectMessageSet.clear();//juphoon
                        dismissActionMode();//juphoon
                        final ConversationMessageView messageView = (ConversationMessageView) v;
                        handleMessageClick(messageView);
                    }
                },
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(final View view) {
                   //     selectMessage((ConversationMessageView) view);
                    //    invalidateOptionsMenu();//juphoon
                        return true;
                    }//juphoon
                }, new CheckBox.OnCheckedChangeListener() {
            //juphoon
            @Override
            public void onCheckedChanged(CompoundButton checkBox, boolean checked) {
//                int index = (int) checkBox.getTag();// 事件处理前要判断状态
//                if (index != -1) {
//                    if (checked) {
//                        mAdapter.updateCheckBoxState(index, true, false);
//                        mSelectMessageSet.add(index);
//                    } else {
//                        mAdapter.updateCheckBoxState(index, false, false);
//                        mSelectMessageSet.remove(index);
//                    }
//                    updateMenuSelectAll();
//                    invalidateOptionsMenu();
//                }
            }
        }
        );


    }

    /**
     * setConversationInfo() may be called before or after onCreate(). When a user initiate a
     * conversation from compose, the ConversationActivity creates this fragment and calls
     * setConversationInfo(), so it happens before onCreate(). However, when the activity is
     * restored from saved instance state, the ConversationFragment is created automatically by
     * the fragment, before ConversationActivity has a chance to call setConversationInfo(). Since
     * the ability to start loading data depends on both methods being called, we need to start
     * loading when onActivityCreated() is called, which is guaranteed to happen after both.
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Delay showing the message list until the participant list is loaded.
        mRecyclerView.setVisibility(View.INVISIBLE);
        mBinding.ensureBound();
        mBinding.getData().init(getLoaderManager(), mBinding);

        // Build the input manager with all its required dependencies and pass it along to the
        // compose message view.
        final ConversationInputManager inputManager = new ConversationInputManager(
                getActivity(), this, mComposeMessageView, mHost, getFragmentManagerToUse(),
                mBinding, mComposeMessageView.getDraftDataModel(), savedInstanceState);
        mComposeMessageView.setInputManager(inputManager);
        mComposeMessageView.setConversationDataModel(BindingBase.createBindingReference(mBinding));
        mHost.invalidateActionBar();

        mDraftMessageDataModel =
                BindingBase.createBindingReference(mComposeMessageView.getDraftDataModel());
        mDraftMessageDataModel.getData().addListener(this);
        if (getComposeRecordingIndex() != -1) {
            mComposeMessageView.pickMediaView(getComposeRecordingIndex());
        }

    }

    public void onAttachmentChoosen() {
        // Attachment has been choosen in the AttachmentChooserActivity, so clear local draft
        // and reload draft on resume.
        mClearLocalDraft = true;
    }

    private int getScrollToMessagePosition() {
        final Activity activity = getActivity();
        if (activity == null) {
            return -1;
        }

        final Intent intent = activity.getIntent();
        if (intent == null) {
            return -1;
        }

        return intent.getIntExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, -1);
    }

    private int getComposeRecordingIndex() {
        final Activity activity = getActivity();
        if (activity == null) {
            return -1;
        }

        final Intent intent = activity.getIntent();
        if (intent == null) {
            return -1;
        }
        int index = (int) intent.getIntExtra(UIIntents.COMPOSE_RECORDING_INDEX, -1);
        return index;
    }

    private void clearScrollToMessagePosition() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        intent.putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, -1);
    }

    private final Handler mHandler = new Handler();
    private TextView benji;
    private TCLButton deletes;
    View composeBubbleViews;
    View composeBubbleView;
    /**
     * {@inheritDoc} from Fragment
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.conversation_fragment, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        benji = view.findViewById(R.id.benji);
        conversiton_back = view.findViewById(R.id.conversiton_back);
        inputs = view.findViewById(R.id.inputs);
        conversiton_back.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    conversiton_back.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            conversiton_back.setFocusable(true);
                            conversiton_back.setFocusableInTouchMode(true);
                            conversiton_back.requestFocus();
                        } }, 1);
                }
                return false;
            }
        });
        deletes = view.findViewById(R.id.deletes);
        deletes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isReadyForAction()) {
                    JuphoonStyleDialog.showDialog(getActivity(),
                            getResources().getQuantityString(
                                    R.plurals.delete_conversations_confirmation_dialog_title, 1),
                            null,
                            getString(R.string.delete_conversation_confirmation_button),
                            new JuphoonStyleDialog.INegativeListener() {
                                @Override
                                public void onClick(View v) {
                                    deleteConversation();
                                }
                            }
                            , getString(R.string.delete_conversation_decline_button)
                            , null
                    );
                } else {
                    warnOfMissingActionConditions(false /*sending*/,
                            null /*commandToRunAfterActionConditionResolved*/);
                }
            }
        });
        deletes.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    deletes.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            deletes.setFocusable(true);
                            deletes.setFocusableInTouchMode(true);
                            deletes.requestFocus();
                        } }, 1);
                }
                return false;
            }
        });

        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setStackFromEnd(true);
        manager.setReverseLayout(false);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        /**
         * by juphoon 去掉动画，可能出现拖影问题
         */
//        mRecyclerView.setItemAnimator(new DefaultItemAnimator() {
//            private final List<ViewHolder> mAddAnimations = new ArrayList<ViewHolder>();
//            private PopupTransitionAnimation mPopupTransitionAnimation;
//
//            @Override
//            public boolean animateAdd(final ViewHolder holder) {
//                final ConversationMessageView view =
//                        (ConversationMessageView) holder.itemView;
//                final ConversationMessageData data = view.getData();
//                endAnimation(holder);
//                final long timeSinceSend = System.currentTimeMillis() - data.getReceivedTimeStamp();
//                if (data.getReceivedTimeStamp() ==
//                        InsertNewMessageAction.getLastSentMessageTimestamp() &&
//                        !data.getIsIncoming() &&
//                        timeSinceSend < MESSAGE_ANIMATION_MAX_WAIT) {
//                    final ConversationMessageBubbleView messageBubble =
//                            (ConversationMessageBubbleView) view
//                                    .findViewById(R.id.message_content);
//                    final Rect startRect = UiUtils.getMeasuredBoundsOnScreen(mComposeMessageView);

//                    final Rect composeBubbleRect =
//                            UiUtils.getMeasuredBoundsOnScreen(composeBubbleView);
//                    final AttachmentPreview attachmentView =
//                            (AttachmentPreview) mComposeMessageView.findViewById(
//                                    R.id.attachment_draft_view);
//                    final Rect attachmentRect = UiUtils.getMeasuredBoundsOnScreen(attachmentView);
//                    if (attachmentView.getVisibility() == View.VISIBLE) {
//                        startRect.top = attachmentRect.top;
//                    } else {
//                        startRect.top = composeBubbleRect.top;
//                    }
//                    startRect.top -= view.getPaddingTop();
//                    startRect.bottom =
//                            composeBubbleRect.bottom;
//                    startRect.left += view.getPaddingRight();
//
//                    view.setAlpha(0);
//                    mPopupTransitionAnimation = new PopupTransitionAnimation(startRect, view);
//                    mPopupTransitionAnimation.setOnStartCallback(new Runnable() {
//                        @Override
//                        public void run() {
//                            final int startWidth = composeBubbleRect.width();
//                            attachmentView.onMessageAnimationStart();
//                            messageBubble.kickOffMorphAnimation(startWidth,
//                                    messageBubble.findViewById(R.id.message_text_and_info)
//                                            .getMeasuredWidth());
//                        }
//                    });
//                    mPopupTransitionAnimation.setOnStopCallback(new Runnable() {
//                        @Override
//                        public void run() {
//                            view.setAlpha(1);
//                        }
//                    });
//                    mPopupTransitionAnimation.startAfterLayoutComplete();
//                    mAddAnimations.add(holder);
//                    return true;
//                } else {
//                    return super.animateAdd(holder);
//                }
//            }
//
//            @Override
//            public void endAnimation(final ViewHolder holder) {
//                if (mAddAnimations.remove(holder)) {
//                    holder.itemView.clearAnimation();
//                }
//                super.endAnimation(holder);
//            }
//
//            @Override
//            public void endAnimations() {
//                for (final ViewHolder holder : mAddAnimations) {
//                    holder.itemView.clearAnimation();
//                }
//                mAddAnimations.clear();
//                if (mPopupTransitionAnimation != null) {
//                    mPopupTransitionAnimation.cancel();
//                }
//                super.endAnimations();
//            }
//        });
        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY);
        }

        FrameLayout frameLayoutBalance = (FrameLayout)view.findViewById(R.id.fragment_conver);
        frameLayoutBalance.setBackgroundResource(R.drawable.element_btn_background_normal);

        mConversationComposeDivider = view.findViewById(R.id.conversation_compose_divider);
        mScrollToDismissThreshold = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
        mRecyclerView.addOnScrollListener(mListScrollListener);
        mFastScroller = ConversationFastScroller.addTo(mRecyclerView,
                UiUtils.isRtlMode() ? ConversationFastScroller.POSITION_LEFT_SIDE :
                    ConversationFastScroller.POSITION_RIGHT_SIDE);

        mComposeMessageView = (ComposeMessageView)
                view.findViewById(R.id.message_compose_view_container);
        // Bind the compose message view to the DraftMessageData
        mComposeMessageView.bind(DataModel.get().createDraftMessageData(
                mBinding.getData().getConversationId()), this);

        composeBubbleView = mComposeMessageView.findViewById(
                R.id.compose_message_text);
        composeBubbleView.setFocusable(true);
        composeBubbleView.setFocusableInTouchMode(true);
        composeBubbleView.requestFocus();

        composeBubbleViews = mComposeMessageView.findViewById(R.id.attach_media_button);


        // juphoon 增加chatbot菜单布局
        mRcsChatbotMenuView = (RcsChatbotMenuView) view.findViewById(R.id.compose_message_chatbot_menu);
        mShowChatbotMenu = (ImageButton)  view.findViewById(R.id.show_chatbot_menu);
        mHideChatbotMenu = (ImageButton) view.findViewById(R.id.hide_chatbot_menu);
        mShowChatbotMenu.setOnClickListener(mShowOrHideChatbotClickListener);
        mHideChatbotMenu.setOnClickListener(mShowOrHideChatbotClickListener);

        Log.i("result","--------oncreate");
        mShowChatbotMenu.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    mShowChatbotMenu.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mShowChatbotMenu.setFocusable(true);
                            mShowChatbotMenu.setFocusableInTouchMode(true);
                            mShowChatbotMenu.requestFocus();
                        } }, 1);
                }
                return false;
            }
        });
        //juphoon @add{
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getIntExtra(UIIntentsImpl.SEARCH_RESULT_INDEX_KEY, -1) >= 0) {
            scrollToPosition(intent.getIntExtra(UIIntentsImpl.SEARCH_RESULT_INDEX_KEY, -1), false);
        }
        //juphoon @add}
        return view;
    }

    private void scrollToPosition(final int targetPosition, final boolean smoothScroll) {
        if (smoothScroll) {
            final int maxScrollDelta = JUMP_SCROLL_THRESHOLD;

            final LinearLayoutManager layoutManager =
                    (LinearLayoutManager) mRecyclerView.getLayoutManager();
            final int firstVisibleItemPosition =
                    layoutManager.findFirstVisibleItemPosition();
            final int delta = targetPosition - firstVisibleItemPosition;
            final int intermediatePosition;

            if (delta > maxScrollDelta) {
                intermediatePosition = Math.max(0, targetPosition - maxScrollDelta);
            } else if (delta < -maxScrollDelta) {
                final int count = layoutManager.getItemCount();
                intermediatePosition = Math.min(count - 1, targetPosition + maxScrollDelta);
            } else {
                intermediatePosition = -1;
            }
            if (intermediatePosition != -1) {
                mRecyclerView.scrollToPosition(intermediatePosition);
            }
            mRecyclerView.smoothScrollToPosition(targetPosition);
        } else {
            //juphoon @modify{
            ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(targetPosition, 0);
            //juphoon @modify}
        }
    }

    private int getScrollPositionFromBottom() {
        final LinearLayoutManager layoutManager =
                (LinearLayoutManager) mRecyclerView.getLayoutManager();
        final int lastVisibleItem =
                layoutManager.findLastVisibleItemPosition();
        return Math.max(mAdapter.getItemCount() - 1 - lastVisibleItem, 0);
    }

    /**
     * Display a photo using the Photoviewer component.
     */
    @Override
    public void displayPhoto(final Uri photoUri, final Rect imageBounds, final boolean isDraft) {
        displayPhoto(photoUri, imageBounds, isDraft, mConversationId, getActivity());
    }

    public static void displayPhoto(final Uri photoUri, final Rect imageBounds,
            final boolean isDraft, final String conversationId, final Activity activity) {
        final Uri imagesUri =
                isDraft ? MessagingContentProvider.buildDraftImagesUri(conversationId)
                        : MessagingContentProvider.buildConversationImagesUri(conversationId);
        UIIntents.get().launchFullScreenPhotoViewer(
                activity, photoUri, imageBounds, imagesUri);
    }

    private void selectMessage(final ConversationMessageView messageView) {
        selectMessage(messageView, null /* attachment */);
    }

    private void selectMessage(final ConversationMessageView messageView,
            final MessagePartData attachment) {
        //juphoon
        if (messageView == null) {
            mSelectMessageSet.clear();
            mAdapter.showCheckBox(false);
            updateMenuSelectAll();
            return;
        }
        //juphoon 群系统消息及不支持的消息不需要弹出菜单
        boolean isSystemMsg = TextUtils.equals(RcsMmsUtils.RMS_SYSTEM_MSG, messageView.getData().getSenderNormalizedDestination());
        boolean notSupportMsg = TextUtils.equals(getString(R.string.not_support_msg), messageView.getData().getText());
        if (isSystemMsg || notSupportMsg) {
            return;
        }
        mAdapter.showCheckBox(true);
        mAdapter.updateCheckBoxState((int) messageView.getTag(), true, true);
        mSelectMessageSet.add((int) messageView.getTag());
        updateMenuSelectAll();
        mSelectedAttachment = attachment;
//        mAdapter.setSelectedMessage(messageView.getData().getMessageId()); juphoon
        mHost.startActionMode(mMessageActionModeCallback);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListState != null) {
            outState.putParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY, mListState);
        }
        mComposeMessageView.saveInputState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("result","--------onResume");
        if (mIncomingDraft == null) {
            mComposeMessageView.requestDraftMessage(mClearLocalDraft);
        } else {
            mComposeMessageView.setDraftMessage(mIncomingDraft);
            mIncomingDraft = null;
        }
        mClearLocalDraft = false;
        mComposeMessageView.setBlockedTips(mBinding.getData().getOtherParticipantNormalizedDestination());//juphoon
        // On resume, check if there's a pending request for resuming message compose. This
        // may happen when the user commits the contact selection for a group conversation and
        // goes from compose back to the conversation fragment.
        if (mHost.shouldResumeComposeMessage()) {
            mComposeMessageView.resumeComposeMessage();
        }

        setConversationFocus();

        // On resume, invalidate all message views to show the updated timestamp.
        mAdapter.notifyDataSetChanged();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mConversationSelfIdChangeReceiver,
                new IntentFilter(UIIntents.CONVERSATION_SELF_ID_CHANGE_BROADCAST_ACTION));
        SharedPreferences sp = getContext().getSharedPreferences("sp_demo", Context.MODE_PRIVATE);
        int age = sp.getInt("age", 0);
        if(age == 11){
            Log.i("ccc","6666...");
//                    mShowChatbotMenu.requestFocus();
            mShowChatbotMenu.performClick();
            sp.edit().putInt("age", 1).apply();
        }
    }

    void setConversationFocus() {
        if (mHost.isActiveAndFocused()) {
            mBinding.getData().setFocus();
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        if (mHost.getActionMode() != null) {
            return;
        }

        inflater.inflate(R.menu.conversation_menu, menu);

        final ConversationData data = mBinding.getData();

        // juphoon 菜单去掉rcs不需要的选项
        final boolean isGroupThread = data.getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD;
        // juphoon 我的电脑菜单的是否显示
        final boolean isPcThread = data.isPcConversation();
        final boolean isChatbot = RcsChatbotHelper.isChatbotByServiceId(mBinding.getData().getOtherParticipantNormalizedDestination());

        // Disable the "people & options" item if we haven't loaded participants yet.
        menu.findItem(R.id.action_people_and_options).setEnabled(data.getParticipantsLoaded() &&
                !(isGroupThread && !RcsServiceManager.isLogined()) && !isClosedGroup())
                .setVisible(!isPcThread && !isChatbot);//juphoon

        // See if we can show add contact action.
        final ParticipantData participant = data.getOtherParticipant();
        final boolean addContactActionVisible = (participant != null
                && TextUtils.isEmpty(participant.getLookupKey()));
        menu.findItem(R.id.action_add_contact).setVisible(addContactActionVisible && !isGroupThread && !isPcThread && !isChatbot);//juphoon

        // See if we should show archive or unarchive.
        final boolean isArchived = data.getIsArchived();
        menu.findItem(R.id.action_archive).setVisible(!isArchived);
        menu.findItem(R.id.action_unarchive).setVisible(isArchived);

        // Conditionally enable the phone call button.
        final boolean supportCallAction = (PhoneUtils.getDefault().isVoiceCapable() &&
                data.getParticipantPhoneNumber() != null) && !isGroupThread &&
                (!isChatbot || !TextUtils.isEmpty(RcsChatbotHelper.getChatbotInfoByServiceId(participant.getNormalizedDestination()).sms));//如果 chatbot 没有 sms 号码就隐藏拨号按钮 juphoon
        menu.findItem(R.id.action_call).setVisible(supportCallAction);

        //See if it is chatbot juphoon
        menu.findItem(R.id.action_detail).setVisible(isChatbot);
        if (isChatbot) {
            initSearchView(menu);
        } else {
            menu.removeItem(R.id.menu_search_item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_people_and_options:
                Assert.isTrue(mBinding.getData().getParticipantsLoaded());
                // juphoon
                if (mBinding.getData().getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), RcsGroupDetailActivity.class);
                    intent.putExtra(RcsGroupDetailActivity.GROUP_CHAT_ID, mBinding.getData().getOtherParticipantNormalizedDestination());
                    startActivity(intent);
                } else {
                    UIIntents.get().launchPeopleAndOptionsActivity(getActivity(), mConversationId);
                }
                return true;

            case R.id.action_call:
                final String phoneNumber = mBinding.getData().getParticipantPhoneNumber();
                Assert.notNull(phoneNumber);
                final View targetView = getActivity().findViewById(R.id.action_call);
                Point centerPoint;
                if (targetView != null) {
                    final int screenLocation[] = new int[2];
                    targetView.getLocationOnScreen(screenLocation);
                    final int centerX = screenLocation[0] + targetView.getWidth() / 2;
                    final int centerY = screenLocation[1] + targetView.getHeight() / 2;
                    centerPoint = new Point(centerX, centerY);
                } else {
                    // In the overflow menu, just use the center of the screen.
                    final Display display = getActivity().getWindowManager().getDefaultDisplay();
                    centerPoint = new Point(display.getWidth() / 2, display.getHeight() / 2);
                }
                UIIntents.get().launchPhoneCallActivity(getActivity(), phoneNumber, centerPoint);
                return true;

            case R.id.action_archive:
                mBinding.getData().archiveConversation(mBinding);
                closeConversation(mConversationId);
                return true;

            case R.id.action_unarchive:
                mBinding.getData().unarchiveConversation(mBinding);
                return true;

            case R.id.action_settings:
                return true;

            case R.id.action_add_contact:
                final ParticipantData participant = mBinding.getData().getOtherParticipant();
                Assert.notNull(participant);
                final String destination = participant.getNormalizedDestination();
                final Uri avatarUri = AvatarUriUtil.createAvatarUri(participant);
                (new AddContactsConfirmationDialog(getActivity(), avatarUri, destination)).show();
                return true;

            case R.id.action_delete:
                if (isReadyForAction()) {
                    JuphoonStyleDialog.showDialog(getActivity(),
                            getResources().getQuantityString(
                                    R.plurals.delete_conversations_confirmation_dialog_title, 1),
                            null,
                            getString(R.string.delete_conversation_confirmation_button),
                            new JuphoonStyleDialog.INegativeListener() {
                                @Override
                                public void onClick(View v) {
                                    deleteConversation();
                                }
                            }
                            , getString(R.string.delete_conversation_decline_button)
                            , null
                    );
                } else {
                    warnOfMissingActionConditions(false /*sending*/,
                            null /*commandToRunAfterActionConditionResolved*/);
                }
                return true;
            //juphoon
            case R.id.action_detail:
                Intent intent = new Intent(getActivity(), RcsChatBotDetailActivity.class);
                intent.putExtra(RcsChatBotDetailActivity.KEY_CHATBOT_NUMBER, mBinding.getData().getOtherParticipantNormalizedDestination());
                startActivity(intent);
                return true;
            //juphoon
            case R.id.menu_search_item:
                onSearchClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    public void onSearchClick() {
        mSearchView.setFocusable(true);
        mSearchView.setFocusableInTouchMode(true);
        mSearchView.requestFocus();
        clearSearchText();
    }

    /**
     * {@inheritDoc} from ConversationDataListener
     */
    @Override
    public void onConversationMessagesCursorUpdated(final ConversationData data,
            final Cursor cursor, final ConversationMessageData newestMessage,
            final boolean isSync) {
        mBinding.ensureBound(data);

        // This needs to be determined before swapping cursor, which may change the scroll state.
        final boolean scrolledToBottom = isScrolledToBottom();
        Log.i("fragment","isScrolledToBottom ---:"+scrolledToBottom);
        final int positionFromBottom = getScrollPositionFromBottom();

        // If participants not loaded, assume 1:1 since that's the 99% case
        final boolean oneOnOne =
                !data.getParticipantsLoaded() || data.getOtherParticipant() != null;
        mAdapter.setOneOnOne(oneOnOne, false /* invalidate */);

        // Ensure that the action bar is updated with the current data.
        invalidateOptionsMenu();
        final Cursor oldCursor = mAdapter.swapCursors(cursor);

        if (cursor != null && oldCursor == null) {
            if (mListState != null) {
                mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
                // RecyclerView restores scroll states without triggering scroll change events, so
                // we need to manually ensure that they are correctly handled.
                mListScrollListener.onScrolled(mRecyclerView, 0, 0);
            }
        }

        if (isSync) {
            // This is a message sync. Syncing messages changes cursor item count, which would
            // implicitly change RV's scroll position. We'd like the RV to keep scrolled to the same
            // relative position from the bottom (because RV is stacked from bottom), so that it
            // stays relatively put as we sync.
            final int position = Math.max(mAdapter.getItemCount() - 1 - positionFromBottom, 0);
            scrollToPosition(position, false /* smoothScroll */);
        } else if (newestMessage != null) {
            // Show a snack bar notification if we are not scrolled to the bottom and the new
            // message is an incoming message.
            if (!scrolledToBottom && newestMessage.getIsIncoming()) {
                // If the conversation activity is started but not resumed (if another dialog
                // activity was in the foregrond), we will show a system notification instead of
                // the snack bar.
                if (mBinding.getData().isFocused()) {
                    UiUtils.showSnackBarWithCustomAction(getActivity(),
                            getView().getRootView(),
                            getString(R.string.in_conversation_notify_new_message_text),
                            SnackBar.Action.createCustomAction(new Runnable() {
                                @Override
                                public void run() {
                                    scrollToBottom(true /* smoothScroll */);
                                    mComposeMessageView.hideAllComposeInputs(false /* animate */);
                                }
                            },
                            getString(R.string.in_conversation_notify_new_message_action)),
                            null /* interactions */,
                            SnackBar.Placement.above(mComposeMessageView));
                }
            } else {
                // We are either already scrolled to the bottom or this is an outgoing message,
                // scroll to the bottom to reveal it.
                // Don't smooth scroll if we were already at the bottom; instead, we scroll
                // immediately so RecyclerView's view animation will take place.

                /** juphoon 加个延迟，否则大文本可能会导致界面异常 */
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollToBottom(!scrolledToBottom);
                    }
                }, 100);
            }
        }

        if (cursor != null) {
            mHost.onConversationMessagesUpdated(cursor.getCount());

            // Are we coming from a widget click where we're told to scroll to a particular item?
            final int scrollToPos = getScrollToMessagePosition();
            if (scrollToPos >= 0) {
                if (LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.VERBOSE)) {
                    LogUtil.v(LogUtil.BUGLE_TAG, "onConversationMessagesCursorUpdated " +
                            " scrollToPos: " + scrollToPos +
                            " cursorCount: " + cursor.getCount());
                }
                scrollToPosition(scrollToPos, true /*smoothScroll*/);
                clearScrollToMessagePosition();
            }
        }
        //juphoon @add{
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getIntExtra(UIIntentsImpl.SEARCH_RESULT_INDEX_KEY, -1) >= 0) {
            scrollToPosition(intent.getIntExtra(UIIntentsImpl.SEARCH_RESULT_INDEX_KEY, -1), false);
            getActivity().getIntent().removeExtra(UIIntentsImpl.SEARCH_RESULT_INDEX_KEY);
        }
        //juphoon @add}
         mHost.invalidateActionBar();
        //juphoon
        mComposeMessageView.setLastConversationMessageData(getLastMsgData());
    }

    /**
     * {@inheritDoc} from ConversationDataListener
     */
    @Override
    public void onConversationMetadataUpdated(final ConversationData conversationData) {
        mBinding.ensureBound(conversationData);
        //juphoon
        if (mSelectMessageSet.size() == 1 && mSelectedAttachment != null) {
            // We may have just sent a message and the temp attachment we selected is now gone.
            // and it was replaced with some new attachment.  Since we don't know which one it
            // is we shouldn't reselect it (unless there is just one) In the multi-attachment
            // case we would just deselect the message and allow the user to reselect, otherwise we
            // may act on old temp data and may crash.
            //juphoon
            ConversationMessageData conversationMessageData = getOneSelectConversationMsgData();
            final List<MessagePartData> currentAttachments = conversationMessageData.getAttachments();
            if (currentAttachments.size() == 1) {
                mSelectedAttachment = currentAttachments.get(0);
            } else if (!currentAttachments.contains(mSelectedAttachment)) {
                selectMessage(null);
            }
        }
        // Ensure that the action bar is updated with the current data.
        invalidateOptionsMenu();
        mHost.onConversationMetadataUpdated();
        mAdapter.notifyDataSetChanged();

        /** juphoon 群聊在没登录情况 和chatbot未登录且无短信端口下隐藏底部菜单，登录情况下显示公众号显示 **/
        if (mBinding.getData() != null && !RcsServiceManager.isLogined() &&
                (mBinding.getData().getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD
                        || (RcsChatbotHelper.isChatbotByServiceId(mBinding.getData().getOtherParticipantNormalizedDestination()) &&
                        TextUtils.isEmpty(RcsChatbotHelper.getChatbotInfoByServiceId(mBinding.getData().getOtherParticipantNormalizedDestination()).sms))
                )) {
//            mBottomPanelMenu.setVisibility(View.GONE);
       //     mComposeMessageView.setVisibility(View.GONE);
            // juphoon 模板消息公众账号增加 
        }
        else if (mBinding.getData() != null && mBinding.getData().getConversationThreadType() == RmsDefine.COMMON_THREAD) {
            //juphoon chatbot底部菜单
            boolean isChatbot = RcsChatbotHelper.isChatbotByServiceId(mBinding.getData().getOtherParticipantNormalizedDestination());
            if (mIsShowChatbotMenu && isChatbot) {
                String address = mBinding.getData().getOtherParticipantNormalizedDestination();
                final RcsChatbotHelper.RcsChatbot chatbotInfo = RcsChatbotHelper.getChatbotInfoBySmsOrServiceId(address);
                //如果详情过期或者没有详情
                if (chatbotInfo.deadline < System.currentTimeMillis() / 1000) {
                    RcsChatbotHelper.addCallback(mRcsChatBotHelperListener);
                    RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
                        @Override
                        public void run(boolean succ, String resultCode, String token) {
                            if (succ) {
                                RcsCallWrapper.rcsGetChatbotInfo(null, token, chatbotInfo.serviceId, chatbotInfo.etag);
                            }
                        }
                    });
                }
                initChatbotBottonMenu(chatbotInfo);
            }
        }
        if (mBinding.getData() != null) {
            /** juphoon 增加登录监听 **/
            RcsServiceManager.addCallBack(mRcsServiceCallBack);
            if (mBinding.getData().getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD) {
                RcsGroupHelper.addCallBack(mGroupInfoChangeListener);
            }
            /** juphoon 群发不支持或者群聊人数超过限制**/
            if (mBinding.getData().getConversationThreadType() == RmsDefine.BROADCAST_THREAD) {
                mComposeMessageView.setBroadcastSupport(checkBroadcastSupport());
            }
        }
    }

    /**
     * juphoon 初始化Chatbot底部菜单
     *
     * @param chatbot*/
    private void initChatbotBottonMenu(RcsChatbotHelper.RcsChatbot chatbot) {
        RcsChatbotInfoBean.PersistentmenuBean menu = RcsChatbotMenuView.pauseFromJson(chatbot.persistentMenu);
        if (menu != null && menu.menu != null) {
            mRcsChatbotMenuView.setVisibility(View.GONE);
            mRcsChatbotMenuView.bind(menu,mConversationId,chatbot.serviceId);
            mHideChatbotMenu.setVisibility(View.VISIBLE);
            mShowChatbotMenu.setVisibility(View.VISIBLE);
            BugleApplication.getInstance().setGong(true);
            mComposeMessageView.setVisibility(View.VISIBLE);
        }
    }

    public void setConversationInfo(final Context context, final String conversationId,
            final MessageData draftData) {
        // TODO: Eventually I would like the Factory to implement
        // Factory.get().bindConversationData(mBinding, getActivity(), this, conversationId));
        if (!mBinding.isBound()) {
            mConversationId = conversationId;
            mIncomingDraft = draftData;
            mBinding.bind(DataModel.get().createConversationData(context, this, conversationId));
            if(mBinding.isBound()){
                Log.i("cccc","setConversationInfo---------------1:"+conversationId);
                Log.i("cccc","setConversationInfo---------------2:"+mBinding.getData().getConversationName());
            }
        } else {
            Log.i("cccc","setConversationInfo---------------null");
            Assert.isTrue(TextUtils.equals(mBinding.getData().getConversationId(), conversationId));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind all the views that we bound to data
        if (mComposeMessageView != null) {
            mComposeMessageView.unbind();
        }

        // juphoon
        RcsServiceManager.removeCallBack(mRcsServiceCallBack);
        RcsGroupHelper.removeCallBack(mGroupInfoChangeListener);
        // And unbind this fragment from its data
        mBinding.unbind();
        mConversationId = null;
    }

    void suppressWriteDraft() {
        mSuppressWriteDraft = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("result","--------onPause");
        if (mComposeMessageView != null && !mSuppressWriteDraft) {
            mComposeMessageView.writeDraftMessage();
        }
        mSuppressWriteDraft = false;
        mBinding.getData().unsetFocus();
        mListState = mRecyclerView.getLayoutManager().onSaveInstanceState();

        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mConversationSelfIdChangeReceiver);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // TODO: Remove isBound and replace it with ensureBound after b/15704674.
    public boolean isBound() {
        return mBinding.isBound();
    }

    private FragmentManager getFragmentManagerToUse() {
        return OsUtil.isAtLeastJB_MR1() ? getChildFragmentManager() : getFragmentManager();
    }

    public MediaPicker getMediaPicker() {
        return (MediaPicker) getFragmentManagerToUse().findFragmentByTag(
                MediaPicker.FRAGMENT_TAG);
    }

    @Override
    public void sendMessage(final MessageData message) {
        if (isReadyForAction()) {
            if (ensureKnownRecipients()) {
                // Merge the caption text from attachments into the text body of the messages
                //juphoon
                if (!message.getIsRms()) {
                    message.consolidateText();
                }

                mBinding.getData().sendMessage(mBinding, message);
                mComposeMessageView.resetMediaPickerState();
            } else {
                LogUtil.w(LogUtil.BUGLE_TAG, "Message can't be sent: conv participants not loaded");
            }
        } else {
            warnOfMissingActionConditions(true /*sending*/,
                    new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(message);
                        }
            });
        }
    }

    public void setHost(final ConversationFragmentHost host) {
        mHost = host;
    }

    public String getConversationName() {
        return mBinding.getData().getConversationName();
    }

    @Override
    public void onComposeEditTextFocused() {
        mHost.onStartComposeMessage();
    }

    @Override
    public void onAttachmentsCleared() {
        // When attachments are removed, reset transient media picker state such as image selection.
        mComposeMessageView.resetMediaPickerState();
    }

    /**
     * Called to check if all conditions are nominal and a "go" for some action, such as deleting
     * a message, that requires this app to be the default app. This is also a precondition
     * required for sending a draft.
     * @return true if all conditions are nominal and we're ready to send a message
     */
    @Override
    public boolean isReadyForAction() {
        return UiUtils.isReadyForAction();
    }

    /**
     * When there's some condition that prevents an operation, such as sending a message,
     * call warnOfMissingActionConditions to put up a snackbar and allow the user to repair
     * that condition.
     * @param sending - true if we're called during a sending operation
     * @param commandToRunAfterActionConditionResolved - a runnable to run after the user responds
     *                  positively to the condition prompt and resolves the condition. If null,
     *                  the user will be shown a toast to tap the send button again.
     */
    @Override
    public void warnOfMissingActionConditions(final boolean sending,
            final Runnable commandToRunAfterActionConditionResolved) {
        if (mChangeDefaultSmsAppHelper == null) {
            mChangeDefaultSmsAppHelper = new ChangeDefaultSmsAppHelper();
        }
        mChangeDefaultSmsAppHelper.warnOfMissingActionConditions(sending,
                commandToRunAfterActionConditionResolved, mComposeMessageView,
                getView().getRootView(),
                getActivity(), this);
    }

    private boolean ensureKnownRecipients() {
        final ConversationData conversationData = mBinding.getData();

        if (!conversationData.getParticipantsLoaded()) {
            // We can't tell yet whether or not we have an unknown recipient
            return false;
        }

        final ConversationParticipantsData participants = conversationData.getParticipants();
        for (final ParticipantData participant : participants) {


            if (participant.isUnknownSender()) {
                UiUtils.showToast(R.string.unknown_sender);
                return false;
            }
        }

        return true;
    }

    public void retryDownload(final String messageId) {
        if (isReadyForAction()) {
            mBinding.getData().downloadMessage(mBinding, messageId);
        } else {
            warnOfMissingActionConditions(false /*sending*/,
                    null /*commandToRunAfterActionConditionResolved*/);
        }
    }

    public void retrySend(final String messageId) {
        if (isReadyForAction()) {
            if (ensureKnownRecipients()) {
                mBinding.getData().resendMessage(mBinding, messageId);
            }
        } else {
            warnOfMissingActionConditions(true /*sending*/,
                    new Runnable() {
                        @Override
                        public void run() {
                            retrySend(messageId);
                        }

                    });
        }
    }

    //juphoon
    void deleteMessage(final HashSet<Integer> messageIdSet) {
        if (isReadyForAction()) {
            //juphoon
            final List<String> msgIdList = new ArrayList<String>();
            for (int position : messageIdSet) {
                msgIdList.add(String.valueOf(mAdapter.getItemId(position)));
            }
            JuphoonStyleDialog.IDismissListener dismissListener = null;
            JuphoonStyleDialog.ICancelListener cancelListener = null;
            if (OsUtil.isAtLeastJB_MR1()) {
                dismissListener = new JuphoonStyleDialog.IDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dismissActionMode();
                    }
                };

            } else {
                cancelListener = new JuphoonStyleDialog.ICancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dismissActionMode();
                    }
                };
            }
            JuphoonStyleDialog.showDialog(getActivity(), getString(R.string.delete_message_confirmation_dialog_title),
                    getString(R.string.delete_message_confirmation_dialog_text), null
                    , getString(android.R.string.cancel), null,
                    getString(R.string.delete_message_confirmation_button),
                    new JuphoonStyleDialog.IPositiveListener() {
                        @Override
                        public void onClick(View v) {
                            for (int i = 0; i < msgIdList.size(); i++) {
                                mBinding.getData().deleteMessage(mBinding, msgIdList.get(i));
                            }
                            mAdapter.showCheckBox(false);//juphoon
                        }
                    }, dismissListener, cancelListener
            );
        } else {
            warnOfMissingActionConditions(false /*sending*/,
                    null /*commandToRunAfterActionConditionResolved*/);
            dismissActionMode();//juphoon
        }
    }

    public void deleteConversation() {
        if (isReadyForAction()) {
            final Context context = getActivity();
            mBinding.getData().deleteConversation(mBinding);
            closeConversation(mConversationId);
        } else {
            warnOfMissingActionConditions(false /*sending*/,
                    null /*commandToRunAfterActionConditionResolved*/);
        }
    }

    //juphoon
    public void showCheckBox(boolean state) {
        mSelectMessageSet.clear();
        mAdapter.showCheckBox(state);
    }

    @Override
    public void closeConversation(final String conversationId) {
        if (TextUtils.equals(conversationId, mConversationId)) {
            mHost.onFinishCurrentConversation();
            // TODO: Explicitly transition to ConversationList (or just go back)?
        }
    }

    @Override
    public void onConversationParticipantDataLoaded(final ConversationData data) {
        mBinding.ensureBound(data);
        if (mBinding.getData().getParticipantsLoaded()) {
            final boolean oneOnOne = mBinding.getData().getOtherParticipant() != null;
            mAdapter.setOneOnOne(oneOnOne, true /* invalidate */);

            // refresh the options menu which will enable the "people & options" item.
            invalidateOptionsMenu();

            mHost.invalidateActionBar();

            mRecyclerView.setVisibility(View.VISIBLE);
            mHost.onConversationParticipantDataLoaded
                (mBinding.getData().getNumberOfParticipantsExcludingSelf());
        }
    }

    @Override
    public void onSubscriptionListDataLoaded(final ConversationData data) {
        mBinding.ensureBound(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void promptForSelfPhoneNumber() {
        if (mComposeMessageView != null) {
            // Avoid bug in system which puts soft keyboard over dialog after orientation change
            ImeUtil.hideSoftInput(getActivity(), mComposeMessageView);
        }

        final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        final EnterSelfPhoneNumberDialog dialog = EnterSelfPhoneNumberDialog
                .newInstance(getConversationSelfSubId());
        dialog.setTargetFragment(this, 0/*requestCode*/);
        dialog.show(ft, null/*tag*/);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (mChangeDefaultSmsAppHelper == null) {
            mChangeDefaultSmsAppHelper = new ChangeDefaultSmsAppHelper();
        }
        mChangeDefaultSmsAppHelper.handleChangeDefaultSmsResult(requestCode, resultCode, null);
    }

    public boolean hasMessages() {
        return mAdapter != null && mAdapter.getItemCount() > 0;
    }

    public boolean onBackPressed() {
        if (mComposeMessageView.onBackPressed()) {
            return true;
        }
        return false;
    }

    public boolean onNavigationUpPressed() {
        return mComposeMessageView.onNavigationUpPressed();
    }

    @Override
    public boolean onAttachmentClick(final ConversationMessageView messageView,
            final MessagePartData attachment, final Rect imageBounds, final boolean longPress) {
        if (longPress) {
            Log.i("bbbb","长安了------------");
            //如果长按
            mAdapter.showCheckBox(true);    //juphoon
            mSelectMessageSet.clear();    //juphoon
            selectMessage(messageView, attachment);
            return true;
        } else if (messageView.getData().getOneClickResendMessage()) {
            handleMessageClick(messageView);
            return true;
        }
        // juphoon 点击下载
        ConversationMessageData data = messageView.getData();
        if (data.getProtocol() == MessageData.PROTOCOL_RMS) {
            if (data.getStatus() == MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD || data.getStatus() == MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED) {
                RcsMsgItemTouchHelper.dealFailMessgeTask(data.getSmsMessageUri(), data.getMessageId());
                return true;
            } else if (data.getStatus() > MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD && data.getStatus() < MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED) {
                return true;
            }
        }

        if (attachment.isImage()) {
            Log.i("bbbb","123------------");
            displayPhoto(attachment.getContentUri(), imageBounds, false /* isDraft */);
        }

        if (attachment.isVCard()) {
            UIIntents.get().launchVCardDetailActivity(getActivity(), attachment.getContentUri());
        }
        //juphoon
        if (attachment.isGeo()) {
            RcsBaiduLocationActivity.startWithLocation(getActivity(), attachment.getContentUri());
        }

        return false;
    }

    private void handleMessageClick(final ConversationMessageView messageView) {
        //juphoon
        if (mSelectMessageSet.size() == 0 || !mSelectMessageSet.contains((int) messageView.getTag())) {
            Log.i("xxx","handleMessageClick....ready");
            final ConversationMessageData data = messageView.getData();
            final boolean isReadyToSend = isReadyForAction();
            // juphoon 点击status textview
            if (data.getProtocol() == MessageData.PROTOCOL_RMS) {
                if (data.getConversationRmsThreadType() == RmsDefine.BROADCAST_THREAD) {
                    Intent intent = new Intent(getActivity(), RcsPeopleActivity.class);
                    intent.putExtra(RcsPeopleActivity.CONVERSATION_ID, data.getConversationId());
                    intent.putExtra(RcsPeopleActivity.MEASSAGE_ID, data.getMessageId());
                    getActivity().startActivity(intent);
                }
                return;
            } else {
                if (data.getOneClickResendMessage()) {
                    Log.i("xxx","data.getOneClickResendMessage()");
                    // Directly resend the message on tap if it's failed
                    retrySend(data.getMessageId());
                    selectMessage(null);
                } else if (data.getShowResendMessage() && isReadyToSend) {
                    Log.i("xxx",".data.getShowResendMessage() && isReadyToSend()");
                    // Select the message to show the resend/download/delete options
                    selectMessage(messageView);
                } else if (data.getShowDownloadMessage() && isReadyToSend) {
                    Log.i("xxx","data.getShowDownloadMessage() && isReadyToSend)");
                    // Directly download the message on tap
                    retryDownload(data.getMessageId());
                } else {
                    Log.i("xxx","selectMessage....");
                    // Let the toast from warnOfMissingActionConditions show and skip
                    // selecting
                    warnOfMissingActionConditions(false /*sending*/,
                            null /*commandToRunAfterActionConditionResolved*/);
                    selectMessage(null);
                }
            }
        } else {
            Log.i("xxx","handleMessageClick....null");
            selectMessage(null);
        }
    }

    private static class AttachmentToSave {
        public final Uri uri;
        public final String contentType;
        public Uri persistedUri;

        AttachmentToSave(final Uri uri, final String contentType) {
            this.uri = uri;
            this.contentType = contentType;
        }
    }

    public static class SaveAttachmentTask extends SafeAsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final List<AttachmentToSave> mAttachmentsToSave = new ArrayList<>();

        public SaveAttachmentTask(final Context context, final Uri contentUri,
                final String contentType) {
            mContext = context;
            addAttachmentToSave(contentUri, contentType);
        }

        public SaveAttachmentTask(final Context context) {
            mContext = context;
        }

        public void addAttachmentToSave(final Uri contentUri, final String contentType) {
            mAttachmentsToSave.add(new AttachmentToSave(contentUri, contentType));
        }

        public int getAttachmentCount() {
            return mAttachmentsToSave.size();
        }

        @Override
        protected Void doInBackgroundTimed(final Void... arg) {
            final File appDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),
                    mContext.getResources().getString(R.string.app_name));
            final File downloadDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            for (final AttachmentToSave attachment : mAttachmentsToSave) {
                final boolean isImageOrVideo = ContentType.isImageType(attachment.contentType)
                        || ContentType.isVideoType(attachment.contentType);
                attachment.persistedUri = UriUtil.persistContent(attachment.uri,
                        isImageOrVideo ? appDir : downloadDir, attachment.contentType);
           }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            int failCount = 0;
            int imageCount = 0;
            int videoCount = 0;
            int otherCount = 0;
            for (final AttachmentToSave attachment : mAttachmentsToSave) {
                if (attachment.persistedUri == null) {
                   failCount++;
                   continue;
                }

                // Inform MediaScanner about the new file
                final Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanFileIntent.setData(attachment.persistedUri);
                mContext.sendBroadcast(scanFileIntent);

                if (ContentType.isImageType(attachment.contentType)) {
                    imageCount++;
                } else if (ContentType.isVideoType(attachment.contentType)) {
                    videoCount++;
                } else {
                    otherCount++;
                    // Inform DownloadManager of the file so it will show in the "downloads" app
                    final DownloadManager downloadManager =
                            (DownloadManager) mContext.getSystemService(
                                    Context.DOWNLOAD_SERVICE);
                    final String filePath = attachment.persistedUri.getPath();
                    final File file = new File(filePath);

                    if (file.exists()) {
                        downloadManager.addCompletedDownload(
                                file.getName() /* title */,
                                mContext.getString(
                                        R.string.attachment_file_description) /* description */,
                                        true /* isMediaScannerScannable */,
                                        attachment.contentType,
                                        file.getAbsolutePath(),
                                        file.length(),
                                        false /* showNotification */);
                    }
                }
            }

            String message;
            if (failCount > 0) {
                message = mContext.getResources().getQuantityString(
                        R.plurals.attachment_save_error, failCount, failCount);
            } else {
                int messageId = R.plurals.attachments_saved;
                if (otherCount > 0) {
                    if (imageCount + videoCount == 0) {
                        messageId = R.plurals.attachments_saved_to_downloads;
                    }
                } else {
                    if (videoCount == 0) {
                        messageId = R.plurals.photos_saved_to_album;
                    } else if (imageCount == 0) {
                        messageId = R.plurals.videos_saved_to_album;
                    } else {
                        messageId = R.plurals.attachments_saved_to_album;
                    }
                }
                final String appName = mContext.getResources().getString(R.string.app_name);
                final int count = imageCount + videoCount + otherCount;
                message = mContext.getResources().getQuantityString(
                        messageId, count, count, appName);
            }
            UiUtils.showToastAtBottom(message);
        }
    }

    private void invalidateOptionsMenu() {
        final Activity activity = getActivity();
        // TODO: Add the supportInvalidateOptionsMenu call to the host activity.
        if (activity == null || !(activity instanceof BugleActionBarActivity)) {
            return;
        }
        ((BugleActionBarActivity) activity).supportInvalidateOptionsMenu();
    }

    @Override
    public void setOptionsMenuVisibility(final boolean visible) {
        setHasOptionsMenu(visible);
    }

    @Override
    public int getConversationSelfSubId() {
        final String selfParticipantId = mComposeMessageView.getConversationSelfId();
        final ParticipantData self = mBinding.getData().getSelfParticipantById(selfParticipantId);
        // If the self id or the self participant data hasn't been loaded yet, fallback to
        // the default setting.
        return self == null ? ParticipantData.DEFAULT_SELF_SUB_ID : self.getSubId();
    }

    @Override
    public void invalidateActionBar() {
        mHost.invalidateActionBar();
    }

    @Override
    public void dismissActionMode() {
        mHost.dismissActionMode();
        //juphoon
        mAdapter.showCheckBox(false);
        updateMenuSelectAll();
    }

    @Override
    public void selectSim(final SubscriptionListEntry subscriptionData) {
        mComposeMessageView.selectSim(subscriptionData);
        mHost.onStartComposeMessage();
    }

    @Override
    public void onStartComposeMessage() {
        mHost.onStartComposeMessage();
    }

    @Override
    public SubscriptionListEntry getSubscriptionEntryForSelfParticipant(
            final String selfParticipantId, final boolean excludeDefault) {
        // TODO: ConversationMessageView is the only one using this. We should probably
        // inject this into the view during binding in the ConversationMessageAdapter.
        return mBinding.getData().getSubscriptionEntryForSelfParticipant(selfParticipantId,
                excludeDefault);
    }

    @Override
    public SimSelectorView getSimSelectorView() {
        return (SimSelectorView) getView().findViewById(R.id.sim_selector);
    }

    @Override
    public MediaPicker createMediaPicker() {
        return new MediaPicker(getActivity());
    }

    @Override
    public void notifyOfAttachmentLoadFailed() {
        UiUtils.showToastAtBottom(R.string.attachment_load_failed_dialog_message);
    }

    @Override
    public void warnOfExceedingMessageLimit(final boolean sending, final boolean tooManyVideos) {
        warnOfExceedingMessageLimit(sending, mComposeMessageView, mConversationId,
                getActivity(), tooManyVideos);
    }

    public static void warnOfExceedingMessageLimit(final boolean sending,
            final ComposeMessageView composeMessageView, final String conversationId,
            final Activity activity, final boolean tooManyVideos) {
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity)
                    .setTitle(R.string.mms_attachment_limit_reached);

        if (sending) {
            if (tooManyVideos) {
                builder.setMessage(R.string.video_attachment_limit_exceeded_when_sending);
            } else {
                builder.setMessage(R.string.attachment_limit_reached_dialog_message_when_sending)
                        .setNegativeButton(R.string.attachment_limit_reached_send_anyway,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog,
                                            final int which) {
                                        composeMessageView.sendMessageIgnoreMessageSizeLimit();
                                    }
                                });
            }
            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    showAttachmentChooser(conversationId, activity);
                }});
        } else {
            builder.setMessage(R.string.attachment_limit_reached_dialog_message_when_composing)
                    .setPositiveButton(android.R.string.ok, null);
        }
        builder.show();
    }

    @Override
    public void showAttachmentChooser() {
        showAttachmentChooser(mConversationId, getActivity());
    }

    public static void showAttachmentChooser(final String conversationId,
            final Activity activity) {
        UIIntents.get().launchAttachmentChooserActivity(activity,
                conversationId, REQUEST_CHOOSE_ATTACHMENTS);
    }

    private void updateActionAndStatusBarColor(final ActionBar actionBar) {
//        final int themeColor = ConversationDrawables.get().getConversationThemeColor();
        final int themeColor = getResources().getColor(R.color.white);
        actionBar.setBackgroundDrawable(new ColorDrawable(themeColor));
        UiUtils.setStatusBarColor(getActivity(), themeColor);
    }
    private LinearLayout inputs;

    public void updateActionBar(final ActionBar actionBar) {
        Log.i("result","--------updateActionBar");
        if (mComposeMessageView == null || !mComposeMessageView.updateActionBar(actionBar)) {
            updateActionAndStatusBarColor(actionBar);
            // We update this regardless of whether or not the action bar is showing so that we
            // don't get a race when it reappears.
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setDisplayHomeAsUpEnabled(true);
            // Reset the back arrow to its default
            actionBar.setHomeAsUpIndicator(0);
            View customView = actionBar.getCustomView();


            Log.i("ccc","12312312...");
//            inputs.setVisibility(View.VISIBLE);
            if(BugleApplication.getInstance().isIsmap()){
                Log.i("ccc","566...");
                inputs.setVisibility(View.VISIBLE);
                composeBubbleViews.requestFocus();
                composeBubbleViews.performClick();
                BugleApplication.getInstance().setIsmap(false);
            }else{
                composeBubbleView.requestFocus();

//                SharedPreferences sp = getContext().getSharedPreferences("sp_demo", Context.MODE_PRIVATE);
//                int age = sp.getInt("age", 0);
//                if(age == 11){
//                    Log.i("ccc","6666...");
////                    mShowChatbotMenu.requestFocus();
//                    mShowChatbotMenu.performClick();
//                    sp.edit().putInt("age", 1).apply();
//                }
            }
            if (customView == null || customView.getId() != R.id.conversation_title_container) {
                Log.i("ccc","3333333333...");
                final LayoutInflater inflator = (LayoutInflater)
                        getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                customView = inflator.inflate(R.layout.action_bar_conversation_name, null);
                customView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        Log.i("ccc","退出对话...");
                        mAdapter.showCheckBox(false);    //juphoon
                        onBackPressed();
                    }
                });
                actionBar.setCustomView(customView);
            }
            Log.i("ccc","44444444...");
            final TextView conversationNameView =
                    (TextView) customView.findViewById(R.id.conversation_title);

            //@juphoon{
            final TextView conversationAddressView = (TextView) customView.findViewById(R.id.conversation_title_public_address);
            final ContactIconView conversationImageView = (ContactIconView) customView.findViewById(R.id.conversation_icon);
            final String conversationName = getConversationName();
            Uri iconUri = null;
            if (mBinding.getData().getIcon() != null) {
                iconUri = Uri.parse(mBinding.getData().getIcon());
            }

            final RcsChatbotHelper.RcsChatbot chatbot = RcsChatbotHelper.getChatbotInfoByServiceId(mBinding.getData().getOtherParticipantNormalizedDestination());
            if (chatbot != null) {
                conversationAddressView.setText(chatbot != null ? RcsChatbotHelper.parseChatbotServierIdToNumber(chatbot.serviceId) : mBinding.getData().getOtherParticipantNormalizedDestination());
                conversationAddressView.setVisibility(View.VISIBLE);
                conversationImageView.setVisibility(View.VISIBLE);
                conversationImageView.setImageResourceUri(iconUri);
            }
            //@juphoon}
            if (!TextUtils.isEmpty(conversationName)) {
                // RTL : To format conversation title if it happens to be phone numbers.
                final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                final String formattedName = bidiFormatter.unicodeWrap(
                        UiUtils.commaEllipsize(
                                conversationName,
                                conversationNameView.getPaint(),
                                conversationNameView.getWidth(),
                                getString(R.string.plus_one),
                                getString(R.string.plus_n)).toString(),
                        TextDirectionHeuristicsCompat.LTR);
                Log.i("ccc","conversationName:"+conversationName);
                conversationNameView.setText(formattedName);
                benji.setText(formattedName);
                // In case phone numbers are mixed in the conversation name, we need to vocalize it.
                final String vocalizedConversationName =
                        AccessibilityUtil.getVocalizedPhoneNumber(getResources(), conversationName);
                conversationNameView.setContentDescription(vocalizedConversationName);
//                getActivity().setTitle(conversationName); juphoon
            } else {
                final String appName = getString(R.string.app_name);
                conversationNameView.setText(appName);
                benji.setText(appName);
                getActivity().setTitle(appName);
            }

            // When conversation is showing and media picker is not showing, then hide the action
            // bar only when we are in landscape mode, with IME open.
            if (mHost.isImeOpen() && UiUtils.isLandscapeMode()) {
                actionBar.hide();
            } else {
                actionBar.hide();
            }
            actionBar.hide();
            //juphoon
            RcsRecipientHelper.checkRcsIcon(mBinding.getData().getOtherParticipantNormalizedDestination());
        }
    }

    @Override
    public boolean shouldShowSubjectEditor() {
        return true;
    }

    @Override
    public boolean shouldHideAttachmentsWhenSimSelectorShown() {
        return false;
    }

    @Override
    public void showHideSimSelector(final boolean show) {
        // no-op for now
    }

    @Override
    public int getSimSelectorItemLayoutId() {
        return R.layout.sim_selector_item_view;
    }

    @Override
    public Uri getSelfSendButtonIconUri() {
        return null;    // use default button icon uri
    }

    @Override
    public int overrideCounterColor() {
        return -1;      // don't override the color
    }

    @Override
    public void onAttachmentsChanged(final boolean haveAttachments) {
        // no-op for now
    }

    @Override
    public void onDraftChanged(final DraftMessageData data, final int changeFlags) {
        mDraftMessageDataModel.ensureBound(data);
        // We're specifically only interested in ATTACHMENTS_CHANGED from the widget. Ignore
        // other changes. When the widget changes an attachment, we need to reload the draft.
        if (changeFlags ==
                (DraftMessageData.WIDGET_CHANGED | DraftMessageData.ATTACHMENTS_CHANGED)) {
            mClearLocalDraft = true;        // force a reload of the draft in onResume
        }
    }

    @Override
    public void onDraftAttachmentLimitReached(final DraftMessageData data) {
        // no-op for now
    }

    @Override
    public void onDraftAttachmentLoadFailed() {
        // no-op for now
    }

    @Override
    public int getAttachmentsClearedFlags() {
        return DraftMessageData.ATTACHMENTS_CHANGED;
    }

    /**
     * juphoon 增加登录监听，处理群聊,公众账号会话和我的电脑
     **/
    private final RcsServiceManager.IServiceManagerCallback mRcsServiceCallBack = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(boolean logined) {
            if (mBinding.getData() != null && (mBinding.getData().getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD
                    || mBinding.getData().isPcConversation())) {
                if (mBinding.getData().getConversationThreadType() == RmsDefine.RMS_GROUP_THREAD && isClosedGroup()) {
                    Log.i("oneone","updateGroupStatus----5");
                    mComposeMessageView.setVisibility(View.GONE);
//                    mBottomPanelMenu.setVisibility(View.GONE);
                } else {
                    if (!logined) {
                        Log.i("oneone","updateGroupStatus----6");
                        mComposeMessageView.setVisibility(View.GONE);
//                        mBottomPanelMenu.setVisibility(View.GONE);
                    } else {
                        Log.i("oneone","updateGroupStatus----7");
                        mComposeMessageView.setVisibility(View.VISIBLE);
                    }
                    invalidateOptionsMenu();
                    mHost.invalidateActionBar();
                }
            }
        }
    };

    /**
     * juphoon 群状态是否为解散等
     **/
    private boolean isClosedGroup() {
        RcsGroupHelper.RcsGroupInfo rcsGroupInfo = RcsGroupHelper.getGroupInfo(mBinding.getData().getOtherParticipantNormalizedDestination());
        if (rcsGroupInfo != null && (rcsGroupInfo.mState == RmsDefine.RmsGroup.STATE_TERMINATED
                || rcsGroupInfo.mState == RmsDefine.RmsGroup.STATE_ABORTED
                || rcsGroupInfo.mState == RmsDefine.RmsGroup.STATE_CLOSED_BY_USER
                || rcsGroupInfo.mState == RmsDefine.RmsGroup.STATE_FAILED)) {
            return true;
        }
        return false;
    }

    private RcsChatbotHelper.Callback mRcsChatBotHelperListener = new RcsChatbotHelper.Callback() {
        @Override
        public void onChatbotInfoChange() {
            if (mBinding.isBound()) {
                String address = mBinding.getData().getOtherParticipantNormalizedDestination();
                final RcsChatbotHelper.RcsChatbot chatbotInfo = RcsChatbotHelper.getChatbotInfoBySmsOrServiceId(address);
                initChatbotBottonMenu(chatbotInfo);
            }
        }
    };

    private RcsGroupHelper.IGroupInfoChangeCallback mGroupInfoChangeListener = new RcsGroupHelper.IGroupInfoChangeCallback() {

        @Override
        public void onGroupInfoUpdate(String greoupChatId) {
            updateGroupStatus();
        }
    };

    private void updateGroupStatus() {
        if (isClosedGroup()) {
            Log.i("oneone","updateGroupStatus----1");
            mComposeMessageView.setVisibility(View.GONE);
        } else {
            Log.i("oneone","updateGroupStatus----2");
            mComposeMessageView.setVisibility(View.VISIBLE);
        }
        invalidateOptionsMenu();
    }

    // juphoon 外层确保 mSelectMessageSet == 1 时调用
    private ConversationMessageData getOneSelectConversationMsgData() {
        int position = mSelectMessageSet.iterator().next();
        ConversationMessageData conversationMessageData = new ConversationMessageData();
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        conversationMessageData.bind(cursor);
        return conversationMessageData;
    }

    private ConversationMessageData getLastMsgData() {
        int position = mAdapter.getItemCount() - 2;
        if (position < 0) return null;
        ConversationMessageData conversationMessageData = new ConversationMessageData();
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        conversationMessageData.bind(cursor);
        return conversationMessageData;
    }

    //juphoon  更新判断全选状态的mMenuSelectAll值
    private void updateMenuSelectAll() {
        if (mSelectMessageSet.size() == mAdapter.getItemCount()) {
            Log.i("xxx","     mMenuSelectAll = true;---:");
            mMenuSelectAll = true;
        } else {
            Log.i("xxx","     mMenuSelectAll = false;--:");
            mMenuSelectAll = false;
        }
    }

    //juphoon  增加当页聊天记录搜索功能 begin
    public void initSearchView(Menu menu) {
        mSearchItem = menu.findItem(R.id.menu_search_item);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setIconified(false);
        mSearchView.clearFocus();
        setCloseBtnGone(true);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (mSearchItem != null) {
                        mSearchItem.collapseActionView();
                    }
                }
            }
        });
        MenuItemCompat.setOnActionExpandListener(mSearchItem, new SearchViewExpandListener());
    }


    public void setCloseBtnGone(boolean bool) {
        int closeBtnId = getResources().getIdentifier(
                "android:id/search_close_btn", null, null);
        ImageView mCloseButton = null;
        if (mSearchView != null) {
            mCloseButton = (ImageView) mSearchView
                    .findViewById(closeBtnId);
        }
        if (mCloseButton != null) {
            mCloseButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_cancel_small_light));
        }
    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(getContext(), SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            intent.putExtra(SearchActivity.CONVERSATION_ID, mBinding.getData().getConversationId());
            startActivity(intent);
            if (mSearchItem != null) {
                mSearchItem.collapseActionView();
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            System.out.println("onQueryTextChange()");
            if (newText != null && newText.length() > SEARCH_MAX_LENGTH) {
                mSearchView.setQuery(
                        newText.substring(0, SEARCH_MAX_LENGTH - 1), false);
                Toast.makeText(getContext(),
                        getString(R.string.search_max_length),
                        Toast.LENGTH_LONG).show();
            }
            setCloseBtnGone(true);
            return true;
        }
    };

    class SearchViewExpandListener implements MenuItemCompat.OnActionExpandListener {
        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            // Do something when collapsed
            hideSoftInput(mSearchView);
            return true;  // Return true to collapse action view
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            // Do something when expanded
            // if (!isSelectionMode()) {
            openKeyboard();
            // }
            return true;  // Return true to expand action view
        }
    }

    private SearchView.SearchAutoComplete getSearchEditText() {
        return mSearchEditText;
    }

    private void clearSearchText() {
        System.out.println("enter clearSearchText()");
        if (getSearchEditText() != null) {
            System.out.println("getSearchEditText()!=null,the SearchEditText = "
                    + getSearchEditText().getText());
            if (!TextUtils.isEmpty(getSearchEditText().getText())) {
                getSearchEditText().setText("");
            }
        }
    }

    private void openKeyboard() {
        System.out.println("enter openKeyboard()");
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void hideSoftInput(View view) {
        System.out.println("enter hideSoftInput()");
        InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    //juphoon  增加当页聊天记录搜索功能 end

    /** juphoon 检查是否支持群发 **/
    private boolean checkBroadcastSupport() {
        int maxMember = Integer.valueOf(RcsCallWrapper.rcsGetConfig(RcsServiceConstants.CONFIG_KEY_MAX_1_TO_MANY_RECIPIENTS));
        int recipients = mBinding.getData().getParticipants().getNumberOfParticipantsExcludingSelf();
        if (maxMember == 0) {
            return false;
        } else if (maxMember == 1) {
            return true;
        } else if (recipients > maxMember) {
            return false;
        } else {
            return true;
        }
    }


}
