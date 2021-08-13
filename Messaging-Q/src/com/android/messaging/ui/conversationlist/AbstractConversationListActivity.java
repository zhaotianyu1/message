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
package com.android.messaging.ui.conversationlist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.action.DeleteConversationAction;
import com.android.messaging.datamodel.action.UpdateConversationArchiveStatusAction;
import com.android.messaging.datamodel.action.UpdateConversationOptionsAction;
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.tcl.ui.fragment.ActConversationListFragment;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.SnackBar;
import com.android.messaging.ui.SnackBarInteraction;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.contact.AddContactsConfirmationDialog;
import com.android.messaging.ui.conversationlist.ConversationListFragment.ConversationListFragmentHost;
import com.android.messaging.ui.conversationlist.MultiSelectActionModeCallback.SelectedConversation;
import com.android.messaging.util.BugleGservices;
import com.android.messaging.util.BugleGservicesKeys;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.Trace;
import com.android.messaging.util.UiUtils;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.ui.RcsChooseActivity;
import com.juphoon.helper.mms.ui.RcsCreateCroupActivity;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.ui.JuphoonStyleDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class for many Conversation List activities. This will handle the common actions of multi
 * select and common launching of intents.
 */
public abstract class AbstractConversationListActivity  extends BugleActionBarActivity
    implements ConversationListFragmentHost, MultiSelectActionModeCallback.Listener {

    private static final int REQUEST_SET_DEFAULT_SMS_APP = 1;

    protected ConversationListFragment mConversationListFragment;

    protected ActConversationListFragment conversationListFragment;

    @Override
    public void onAttachFragment(final Fragment fragment) {
        Trace.beginSection("RcsAddBusinessActivity.onAttachFragment");
        // Fragment could be debug dialog
        if (fragment instanceof ConversationListFragment) {
            mConversationListFragment = (ConversationListFragment) fragment;
            mConversationListFragment.setHost(this);
        }
        if(fragment instanceof ActConversationListFragment){
            conversationListFragment = (ActConversationListFragment) fragment;
            conversationListFragment.setHost((ActConversationListFragment.ActConversationListFragmentHost) this);
        }
        Trace.endSection();
    }

    @Override
    public void onBackPressed() {
        // If action mode is active dismiss it
        if (getActionMode() != null) {
            dismissActionMode();
            return;
        }
        super.onBackPressed();
    }

    protected void startMultiSelectActionMode() {
        startActionMode(new MultiSelectActionModeCallback(this));
    }

    protected void exitMultiSelectState() {
        mConversationListFragment.showFab();
        dismissActionMode();
        mConversationListFragment.updateUi();
    }

    protected boolean isInConversationListSelectMode() {
        return getActionModeCallback() instanceof MultiSelectActionModeCallback;
    }

    @Override
    public boolean isSelectionMode() {
        return isInConversationListSelectMode();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActionBarDelete(final Collection<SelectedConversation> conversations) {
        if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
            // TODO: figure out a good way to combine this with the implementation in
            // ConversationFragment doing similar things
            final Activity activity = this;
            UiUtils.showSnackBarWithCustomAction(this,
                    getWindow().getDecorView().getRootView(),
                    getString(R.string.requires_default_sms_app),
                    SnackBar.Action.createCustomAction(new Runnable() {
                            @Override
                            public void run() {
                                final Intent intent =
                                        UIIntents.get().getChangeDefaultSmsAppIntent(activity);
                                startActivityForResult(intent, REQUEST_SET_DEFAULT_SMS_APP);
                            }
                        },
                        getString(R.string.requires_default_sms_change_button)),
                    null /* interactions */,
                    null /* placement */);
            return;
        }
        JuphoonStyleDialog.showDialog(this,
                getResources().getQuantityString(R.plurals.delete_conversations_confirmation_dialog_title, conversations.size()),
                null,
                getString(R.string.delete_conversation_decline_button),
                null,
                getString(R.string.delete_conversation_confirmation_button),
                new JuphoonStyleDialog.IPositiveListener() {
                    @Override
                    public void onClick(View v) {
                        for (final SelectedConversation conversation : conversations) {
                            DeleteConversationAction.deleteConversation(
                                    conversation.conversationId,
                                    conversation.timestamp);
                        }
                        exitMultiSelectState();
                    }
                }
        );
    }

    @Override
    public void onActionBarArchive(final Iterable<SelectedConversation> conversations,
            final boolean isToArchive) {
        final ArrayList<String> conversationIds = new ArrayList<String>();
        for (final SelectedConversation conversation : conversations) {
            final String conversationId = conversation.conversationId;
            conversationIds.add(conversationId);
            if (isToArchive) {
                UpdateConversationArchiveStatusAction.archiveConversation(conversationId);
            } else {
                UpdateConversationArchiveStatusAction.unarchiveConversation(conversationId);
            }
        }

        final Runnable undoRunnable = new Runnable() {
            @Override
            public void run() {
                for (final String conversationId : conversationIds) {
                    if (isToArchive) {
                        UpdateConversationArchiveStatusAction.unarchiveConversation(conversationId);
                    } else {
                        UpdateConversationArchiveStatusAction.archiveConversation(conversationId);
                    }
                }
            }
        };

        final int textId =
                isToArchive ? R.string.archived_toast_message : R.string.unarchived_toast_message;
        final String message = getResources().getString(textId, conversationIds.size());
        UiUtils.showSnackBar(this, findViewById(android.R.id.list), message, undoRunnable,
                SnackBar.Action.SNACK_BAR_UNDO,
                mConversationListFragment.getSnackBarInteractions());
        exitMultiSelectState();
    }

    @Override
    public void onActionBarNotification(final Iterable<SelectedConversation> conversations,
            final boolean isNotificationOn) {
        for (final SelectedConversation conversation : conversations) {
            UpdateConversationOptionsAction.enableConversationNotifications(
                    conversation.conversationId, isNotificationOn);
        }

        final int textId = isNotificationOn ?
                R.string.notification_on_toast_message : R.string.notification_off_toast_message;
        final String message = getResources().getString(textId, 1);
        UiUtils.showSnackBar(this, findViewById(android.R.id.list), message,
            null /* undoRunnable */,
            SnackBar.Action.SNACK_BAR_UNDO, mConversationListFragment.getSnackBarInteractions());
        exitMultiSelectState();
    }

    @Override
    public void onActionBarAddContact(final SelectedConversation conversation) {
        final Uri avatarUri;
        if (conversation.icon != null) {
            avatarUri = Uri.parse(conversation.icon);
        } else {
            avatarUri = null;
        }
        final AddContactsConfirmationDialog dialog = new AddContactsConfirmationDialog(
                this, avatarUri, conversation.otherParticipantNormalizedDestination);
        dialog.showJuphoonStyleDialog(this);
        exitMultiSelectState();
    }

    @Override
    public void onActionBarBlock(final SelectedConversation conversation) {
        final Resources res = getResources();
        JuphoonStyleDialog.showDialog(this,
                res.getString(R.string.block_confirmation_title, conversation.otherParticipantNormalizedDestination),
                res.getString(R.string.block_confirmation_message),
                getString(android.R.string.cancel),
                null,
                getString(android.R.string.ok),
                new JuphoonStyleDialog.IPositiveListener() {
                    @Override
                    public void onClick(View v) {
                        final Context context = AbstractConversationListActivity.this;
                        final View listView = findViewById(android.R.id.list);
                        final List<SnackBarInteraction> interactions =
                                mConversationListFragment.getSnackBarInteractions();
                        final UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener
                                undoListener =
                                new UpdateDestinationBlockedActionSnackBar(
                                        context, listView, null /* undoRunnable */,
                                        interactions);
                        final Runnable undoRunnable = new Runnable() {
                            @Override
                            public void run() {
                                UpdateDestinationBlockedAction.updateDestinationBlocked(
                                        conversation.otherParticipantNormalizedDestination, false,
                                        conversation.conversationId,
                                        undoListener);
                            }
                        };
                        final UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener
                                listener = new UpdateDestinationBlockedActionSnackBar(
                                context, listView, undoRunnable, interactions);
                        UpdateDestinationBlockedAction.updateDestinationBlocked(
                                conversation.otherParticipantNormalizedDestination, true,
                                conversation.conversationId,
                                listener);
                        exitMultiSelectState();
                    }
                }
        );
    }

    @Override
    public void onConversationClick(final ConversationListData listData,
                                    final ConversationListItemData conversationListItemData,
                                    final boolean isLongClick,
                                    final ConversationListItemView conversationView) {
        if (isLongClick && !isInConversationListSelectMode()) {
            startMultiSelectActionMode();
        }

        if (isInConversationListSelectMode()) {
            final MultiSelectActionModeCallback multiSelectActionMode =
                    (MultiSelectActionModeCallback) getActionModeCallback();
            multiSelectActionMode.toggleSelect(listData, conversationListItemData);
            mConversationListFragment.updateUi();
        } else {
            final String conversationId = conversationListItemData.getConversationId();
            BugleApplication.getInstance().setConversionid(conversationId);
            Bundle sceneTransitionAnimationOptions = null;
            boolean hasCustomTransitions = false;

            UIIntents.get().launchConversationActivity(
                    this, conversationId, null,
                    sceneTransitionAnimationOptions,
                    hasCustomTransitions);
        }
    }

    // juphoon 置顶设置
    @Override
    public void onActionBarPriority(
            final Iterable<SelectedConversation> conversations,
            final boolean isTopPriority) {
        final long currentTimeMillis = System.currentTimeMillis();
        for (final SelectedConversation conv : conversations) {
            setPriority(isTopPriority, currentTimeMillis, conv);
        }
        exitMultiSelectState();
    }

    @Override
    public void onCreateConversationClick() {
        // juphoon
        createNewMessage();
    }


    @Override
    public boolean isConversationSelected(final String conversationId) {
        return isInConversationListSelectMode() &&
                ((MultiSelectActionModeCallback) getActionModeCallback()).isSelected(
                        conversationId);
    }

    public void onActionBarDebug() {
        DebugUtils.showDebugOptions(this);
    }

    private static class UpdateDestinationBlockedActionSnackBar
            implements UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener {
        private final Context mContext;
        private final View mParentView;
        private final Runnable mUndoRunnable;
        private final List<SnackBarInteraction> mInteractions;

        UpdateDestinationBlockedActionSnackBar(final Context context,
                @NonNull final View parentView, @Nullable final Runnable undoRunnable,
                @Nullable List<SnackBarInteraction> interactions) {
            mContext = context;
            mParentView = parentView;
            mUndoRunnable = undoRunnable;
            mInteractions = interactions;
        }

        @Override
        public void onUpdateDestinationBlockedAction(
            final UpdateDestinationBlockedAction action,
            final boolean success, final boolean block,
            final String destination) {
            if (success) {
                final int messageId = block ? R.string.blocked_toast_message
                        : R.string.unblocked_toast_message;
                final String message = mContext.getResources().getString(messageId, 1);
                UiUtils.showSnackBar(mContext, mParentView, message, mUndoRunnable,
                        SnackBar.Action.SNACK_BAR_UNDO, mInteractions);
            }
        }
    }

    /** juphoon 群聊 单聊入口 */
    private void createNewMessage() {
//        PopupMenu popupMenu = new PopupMenu(mConversationListFragment.getActivity(),
//                mConversationListFragment.getView().findViewById(R.id.for_popupmenu_show));// 第二个参数是绑定的那个view
//        MenuInflater inflater = popupMenu.getMenuInflater();
//        inflater.inflate(R.menu.menu_create_msg, popupMenu.getMenu());
//        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                case R.id.create_msg_1To1:
            UIIntents.get().launchCreateNewConversationActivity(AbstractConversationListActivity.this, null);
//                    break;

//                case R.id.create_msg_group:
//                    if (RcsServiceManager.isLogined()) {
//                        Intent intent = new Intent(AbstractConversationListActivity.this, RcsCreateCroupActivity.class);
//                        intent.putExtra(RcsCreateCroupActivity.TITLE, getString(R.string.create_group_chat));
//                        intent.putExtra(RcsCreateCroupActivity.NOT_SHOW_PHONES, RcsServiceManager.getUserName());
//                        intent.putExtra(RcsCreateCroupActivity.LIMIT_MIN_SELECT, 2);
//                        intent.putExtra(RcsCreateCroupActivity.LIMIT_MAX_SELECT,
//                                RcsServiceManager.getGroupMaxMemberSize() - 1);
//                        intent.putExtra(RcsCreateCroupActivity.EXCLUDE_PHONES, RcsServiceManager.getUserName());
//                        intent.putExtra(RcsCreateCroupActivity.CHOOSE_MODE, RcsChooseActivity.MODE_CONTACTS_AND_GROUPS);
//                        startActivity(intent);
//                    } else {
//                        Toast.makeText(AbstractConversationListActivity.this, R.string.not_login, Toast.LENGTH_SHORT)
//                                .show();
//                    }
//                    break;
//
////                    case R.id.add_business:
////                        Intent intent = new Intent(AbstractConversationListActivity.this, RcsAddBusinessActivity.class);
////                        startActivity(intent);
////                        break;
////
////                    case R.id.local_business:
////                        Intent intent1 = new Intent(AbstractConversationListActivity.this, RcsLocalBusinessActivity.class);
////                        startActivity(intent1);
////                        break;
//                default:
//                    break;
//                }
//                return false;
//            }
//        });
//        popupMenu.show();
    }

    /** juphoon 置顶或取消置顶 */
    private void setPriority(final boolean isTopPriority, final long currentTimeMillis, final SelectedConversation conv) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                final DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
                dbWrapper.beginTransaction();
                try {
                    if (isTopPriority) {
                        if (conv.priority == 0) {
                            BugleDatabaseOperations.updateConversationPriorityInTransaction(
                                    dbWrapper, conv.conversationId, currentTimeMillis);
                        }
                    } else {
                        if (conv.priority > 0) {
                            BugleDatabaseOperations.updateConversationPriorityInTransaction(
                                    dbWrapper, conv.conversationId, 0);
                        }
                    }
                    dbWrapper.setTransactionSuccessful();
                } finally {
                    dbWrapper.endTransaction();
                }
            }
        }).start();
    }
}
