package com.juphoon.helper.mms.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cursoradapter.widget.CursorAdapter;

import com.android.messaging.R;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.util.AvatarUriUtil;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsContactHelp;
import com.juphoon.rcs.tool.RcsGroupChatManager;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RmsDefine.RmsGroupNotification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class RcsGroupNotificationAdapter extends CursorAdapter {
    private static ThreadLocal<SimpleDateFormat> DateLocal = new ThreadLocal<SimpleDateFormat>();

    public final static String[] SELECTION = new String[] {
            RmsGroupNotification._ID,
            RmsGroupNotification.DATE,
            RmsGroupNotification.NAME,
            RmsGroupNotification.ORGANIZER_PHONE,
            RmsGroupNotification.GROUP_CHAT_ID,
            RmsGroupNotification.SESSION_IDENTITY,
            RmsGroupNotification.INFO,
    };

    private final int COLOUMN_ID = 0;
    private final int COLOUMN_DATE = 1;
    private final int COLOUMN_NAME = 2;
    private final int COLOUMN_ORGANIZER_PHONE = 3;
    private final int COLOUMN_GROUP_CHAT_ID = 4;
    private final int COLOUMN_SESSION_IDENTITY = 5;
    private final int COLOUMN_INFO = 6;

    public static class GroupNotificationItem {
        int mId;
        long mDate;
        String mName;
        String mOrganizerPhone;
        String mGroupChatId;
        String mSessionIdentity;
        int mInfo;
    }

    public RcsGroupNotificationAdapter(Context context, Cursor c,
                                       boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        final GroupNotificationItem item = getItem(cursor);

        TextView content = (TextView)view.findViewById(R.id.textview_content);
        TextView date = (TextView)view.findViewById(R.id.textview_date);
        TextView info = (TextView)view.findViewById(R.id.textview_info);
        Button invtAccept = (Button)view.findViewById(R.id.btn_accept);
        Button invtReject = (Button)view.findViewById(R.id.btn_reject);
        final LinearLayout invtView = (LinearLayout)view.findViewById(R.id.group_notify_invt_view);
        invtView.setVisibility(View.GONE);
        invtAccept.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                acceptGroupInvite(context, item);
                invtView.setVisibility(View.GONE);
            }
        });
        invtReject.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                rejectGroupInvite(context, item);
                invtView.setVisibility(View.GONE);
            }
        });
        final ContactIconView iconView = (ContactIconView)view.findViewById(R.id.rcsgroup_notify_photo);
        setIcon(context, item.mOrganizerPhone, iconView);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm");
        SimpleDateFormat timeSdf = new SimpleDateFormat("yyyy-MM-dd kk:mm");
        try {
            if (IsToday(sdf.format(item.mDate))) {
                timeSdf = new SimpleDateFormat("kk:mm");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        date.setText(timeSdf.format(item.mDate));
        info.setText(RcsContactHelp.getNickNameByPhone(context, item.mOrganizerPhone));
        if (item.mInfo == RmsGroupNotification.INFO_INVITED) {
            invtView.setVisibility(View.VISIBLE);
            content.setText(String.format(context.getString(R.string.invite_by), item.mName));
        } else if (item.mInfo == RmsGroupNotification.INFO_ACCEPTED) {
            content.setText(String.format(context.getString(R.string.already_agree), item.mName));
        } else if (item.mInfo == RmsGroupNotification.INFO_REJECTED) {
            content.setText(String.format(context.getString(R.string.already_refuse), item.mName));
        } else if (item.mInfo == RmsGroupNotification.INFO_KICKOUT) {
            content.setText(String.format(context.getString(R.string.kick_out_of_group), item.mName));
        } else if (item.mInfo == RmsGroupNotification.INFO_DISSOLVE) {
            info.setText(item.mName);
            iconView.setImageResourceUri(
                    AvatarUriUtil.createAvatarUri(null, item.mName, null, null));
            content.setText(context.getString(R.string.already_dismiss));
        }
    }

    private void setIcon(final Context context, final String phone, final ContactIconView iconView) {
        new AsyncTask<Void, Void, Void>() {
            long contactId;
            String name;
            Uri icon;
            
            @Override
            protected Void doInBackground(Void... arg0) {
                contactId = RcsContactHelp.getContactIdWithPhoneNumber(context, phone);
                name = RcsContactHelp.getNameWithContactId(context, contactId);
                icon = RcsContactHelp.getPhotoUriWithContactId(context, contactId);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (TextUtils.isEmpty(phone)) {
                    return;
                }
                if (icon != null) {
                    iconView.setImageResourceUri(
                            AvatarUriUtil.createAvatarUri(icon,
                                    name, null, null),
                            0, null,
                            phone);
                } else {
                    iconView.setImageResourceUri(
                            AvatarUriUtil.createAvatarUri(null, RcsContactHelp.getNickNameByPhone(context, phone), null, null),
                            0, null,
                            phone);
                }
                iconView.setClickable(false);
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return vi.inflate(R.layout.group_notification_item, viewGroup, false);
    }

    @Override
    public Object getItem(int position) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(position)) {
            return getItem(cursor);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        GroupNotificationItem item = (GroupNotificationItem)getItem(position);
        if (item != null)
            return item.mId;
        return -1;
    }

    private GroupNotificationItem getItem(Cursor cursor) {
        GroupNotificationItem item = new GroupNotificationItem();
        item.mId = cursor.getInt(COLOUMN_ID);
        item.mDate = cursor.getLong(COLOUMN_DATE);
        item.mName = cursor.getString(COLOUMN_NAME);
        item.mOrganizerPhone = cursor.getString(COLOUMN_ORGANIZER_PHONE);
        item.mGroupChatId = cursor.getString(COLOUMN_GROUP_CHAT_ID);
        item.mSessionIdentity = cursor.getString(COLOUMN_SESSION_IDENTITY);
        item.mInfo = cursor.getInt(COLOUMN_INFO);
        return item;
    }

    private void acceptGroupInvite(Context context, final RcsGroupNotificationAdapter.GroupNotificationItem item) {
        if (!RcsServiceManager.isLogined()) {
            Toast.makeText(context, R.string.not_login, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean result = true;
        if (RcsCallWrapper.rcsGroupSessIsExist(item.mGroupChatId)) {
            result = RcsCallWrapper.rcsGroupChatAccept(item.mGroupChatId, "");
        } else {
            RcsGroupChatManager.rejoinGroup(item.mGroupChatId, item.mSessionIdentity, item.mName, "");
        }
        if (!result) {
            Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void rejectGroupInvite(final Context context, final RcsGroupNotificationAdapter.GroupNotificationItem item) {
        if (!RcsServiceManager.isLogined()) {
            Toast.makeText(context, R.string.not_login, Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                if (RcsCallWrapper.rcsGroupSessIsExist(item.mGroupChatId)) {
                    RcsCallWrapper.rcsGroupChatReject(item.mGroupChatId, RcsImServiceConstants.EN_MTC_IM_SESS_REJECT_REASON_BUSY);
                }
                ContentValues values = new ContentValues();
                values.put(RmsGroupNotification.DATE, System.currentTimeMillis());
                values.put(RmsGroupNotification.INFO, RmsGroupNotification.INFO_REJECTED);
                context.getContentResolver().update(RmsGroupNotification.CONTENT_URI, values, RmsGroupNotification._ID+"="+item.mId, null);
                return null;
            }

        }.execute();
    }

    private boolean IsToday(String day) throws ParseException {

        Calendar pre = Calendar.getInstance();
        Date predate = new Date(System.currentTimeMillis());
        pre.setTime(predate);

        Calendar cal = Calendar.getInstance();
        Date date = getDateFormat().parse(day);
        cal.setTime(date);

        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {
            int diffDay = cal.get(Calendar.DAY_OF_YEAR) - pre.get(Calendar.DAY_OF_YEAR);

            if (diffDay == 0) {
                return true;
            }
        }
        return false;
    }

    private SimpleDateFormat getDateFormat() {
        if (null == DateLocal.get()) {
            DateLocal.set(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA));
        }
        return DateLocal.get();
    }
}
