package com.juphoon.helper.mms.ui;


import android.app.ActionBar;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.util.PhoneUtils;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.service.RmsDefine.RmsGroupNotification;

import java.util.HashSet;
import java.util.Set;

public class RcsGroupNotificationActivity extends ListActivity {

    private RcsGroupNotificationAdapter mAdapter;
//    private ProgressDialog mProgressDialog;
//    private GroupNotificationItem mAcceptingItem;
    private AsyncQueryHandler mAsyncQuery;

    private static int GROUPNOTIFICATION_ID = 3350;
    private static boolean sShow;

    public static void UpdateNotification(final Context context) {
        if (!PhoneUtils.getDefault().isSmsEnabled()) {
            return;
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (sShow) {
                    Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(500);
                    return;
                }
                Cursor cursor = context.getContentResolver().query(RmsGroupNotification.CONTENT_URI, new String[] { RmsGroupNotification._ID },
                        RmsGroupNotification.INFO + "=" + RmsGroupNotification.INFO_INVITED, null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(context, RcsGroupNotificationActivity.class), 0);
                        Notification.Builder builder = new Notification.Builder(context)
                                .setTicker(context.getString(R.string.group_invitation))
                                .setSmallIcon(R.drawable.stat_notify_sms)
                                .setAutoCancel(true)
                                .setWhen(System.currentTimeMillis())
                                .setContentIntent(pi)
                                .setContentTitle(context.getString(R.string.group_invitation))
                                .setContentText(context.getString(R.string.group_invitation_receive, +cursor.getCount()))
                                .setDefaults(Notification.DEFAULT_SOUND);
                        nm.notify(GROUPNOTIFICATION_ID, builder.build());
                    }
                    cursor.close();
                }
            }
        }).start();

    }

    private final RcsBroadcastHelper.IGroupListener mIGroupListener = new RcsBroadcastHelper.IGroupListener() {

        @Override
        public void onGroupInfoChange(String groupChatId) {
        }

        @Override
        public void onGroupSessChange(String groupChatId, boolean have) {
//            if (mAcceptingItem != null && TextUtils.equals(mAcceptingItem.mGroupChatId, groupChatId)) {
//                if (mProgressDialog != null) {
//                    mProgressDialog.dismiss();
//                    mProgressDialog = null;
//                }
//                mAcceptingItem = null;
//            }
        }

        @Override
        public void onGroupRecvInvite(String groupChatId) {

        }
    };

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        mAsyncQuery = new AsyncQueryHandler(getContentResolver()) {

            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                super.onDeleteComplete(token, cookie, result);
//                if (mProgressDialog != null) {
//                    mProgressDialog.dismiss();
//                }
            }

            @Override
            protected void onQueryComplete(int token, Object cookie,
                    Cursor cursor) {
                super.onQueryComplete(token, cookie, cursor);
                mAdapter = new RcsGroupNotificationAdapter(RcsGroupNotificationActivity.this, cursor, true);
                setListAdapter(mAdapter);
            }

        };
        mAsyncQuery.startQuery(-1, null, RmsGroupNotification.CONTENT_URI, RcsGroupNotificationAdapter.SELECTION, null, null, "date desc");

        ListView l = getListView();
        l.setItemsCanFocus(true);
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(new ModeCallback());

        ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE
                | ActionBar.DISPLAY_HOME_AS_UP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sShow = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sShow = true;
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(GROUPNOTIFICATION_ID);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        RcsBroadcastHelper.removeGroupListener(mIGroupListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteNotifications(Set<Long> setIds) {
        if (setIds.size() == 0)
            return;
        StringBuilder builder = new StringBuilder();
        for (Long id : setIds) {
            if (builder.length() > 0)
                builder.append(",");
            builder.append(id);
        }
        mAsyncQuery.startDelete(-1, null, RmsGroupNotification.CONTENT_URI, "_id in ("+builder.toString()+")", null);
//        mProgressDialog = ProgressDialog.show(RcsGroupNotificationActivity.this, null, getString(R.string.deleting));
    }

    class ModeCallback implements MultiChoiceModeListener {

        private View mMultiSelectActionBarView;
        private TextView mSelectedCount;
        private HashSet<Long> mSelectedIds;

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    if (mSelectedIds.size() > 0) {
                        deleteNotifications(mSelectedIds);
                    }
                    mode.finish();
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            mSelectedIds = new HashSet<Long>();
            inflater.inflate(R.menu.group_notification_menu, menu);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(RcsGroupNotificationActivity.this)
                    .inflate(R.layout.group_notification_actionbar, null);

                mSelectedCount =
                    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((TextView)mMultiSelectActionBarView.findViewById(R.id.title))
                .setText(R.string.group_notification);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectedIds = null;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu arg1) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup) LayoutInflater.from(RcsGroupNotificationActivity.this)
                    .inflate(R.layout.group_notification_actionbar, null);
                mode.setCustomView(v);

                mSelectedCount = (TextView)v.findViewById(R.id.selected_count);
            }
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode arg0, int position, long id,
                                              boolean checked) {
            ListView listView = getListView();
            final int checkedCount = listView.getCheckedItemCount();
            mSelectedCount.setText(Integer.toString(checkedCount));

            if (checked) {
                mSelectedIds.add(id);
            } else {
                mSelectedIds.remove(id);
            }
        }

    }
    
}
