package com.juphoon.helper.mms.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.util.AvatarUriUtil;
import com.juphoon.helper.RcsGroupHelper.RcsGroupMember;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsContactHelp;
import com.juphoon.rcs.tool.RcsIconManager;

import java.util.ArrayList;
import java.util.List;

public class RcsGroupMemberAdapter extends BaseAdapter {
    private final Context mContext;
    private List<RcsGroupMember> mMembers;
    // 保存选中的成员
    private List<String> mSelectMembers;
    private String mChairMan;
    private boolean mCheckBoxVisibility;
    private String mSelectedPhone;

    public RcsGroupMemberAdapter(Context context) {
        mContext = context;
        mSelectMembers = new ArrayList<>();
    }

    public void setInfo(String chairMan, List<RcsGroupMember> mListGroupMembers) {
        mChairMan = chairMan;
        mMembers = mListGroupMembers;
    }

    public List<String> getSelectMembers() {
        return mSelectMembers;
    }

    public void setCheckBoxVisibility(int visibility) {
        mCheckBoxVisibility = visibility == View.VISIBLE ? true : false;
    }

    public void setmSelectedPhone(String phone) {
        mSelectedPhone = phone;
    }

    @Override
    public int getCount() {
        return mMembers == null ? 0 : mMembers.size();
    }

    @Override
    public Object getItem(int arg0) {
        return getItem(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup arg2) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.item_groupmember, null);
            holder = new ViewHolder();
            holder.mImageView = (ContactIconView) convertView.findViewById(R.id.rcsgroupmember_icon);
            holder.mTextView = (TextView) convertView.findViewById(R.id.rcsgroupmember_name);
            holder.mNickNameTextView = (TextView) convertView.findViewById(R.id.rcsgroupmember_nickname);
            holder.mSetIcon = (ImageView) convertView.findViewById(R.id.rcsgroupmember_set);
            holder.mMemberSelected = (CheckBox) convertView.findViewById(R.id.rcsgroupmember_selected);
            holder.mMemberSelected.setFocusable(false);
            holder.mMemberSelected.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                    if (checked) {
                        final boolean isChairMan = TextUtils.equals(mMembers.get(position).mPhone, mChairMan);
                        if (!isChairMan) {
                            mSelectMembers.add(mMembers.get(position).mPhone);
                        }
                    } else {
                        mSelectMembers.remove(mMembers.get(position).mPhone);
                    }
                }
            });

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        RcsGroupMember item = mMembers.get(position);
        setImageAndText(holder.mImageView, holder.mTextView, holder.mNickNameTextView, holder.mSetIcon, holder.mMemberSelected, item);

        return convertView;
    }

    class ViewHolder {
        ContactIconView mImageView;
        TextView mTextView;
        TextView mNickNameTextView;
        ImageView mSetIcon;
        // 多选踢人选中状态
        CheckBox mMemberSelected;
    }

    @SuppressLint("StaticFieldLeak")
    private void setImageAndText(final ContactIconView imageView, final TextView textView,
                                 final TextView nickNameTextView, final ImageView imageButton, final CheckBox checkBox, final RcsGroupMember item) {
        final boolean isChairMan = TextUtils.equals(item.mPhone, mChairMan);
        final boolean isMe = TextUtils.equals(item.mPhone, RcsMmsUtils.getLastLoginMsisdn());
        textView.setTag(item.mPhone);
        new AsyncTask<Void, Void, Void>() {
            long contactId;
            String name;
            Uri icon;

            @Override
            protected Void doInBackground(Void... arg0) {
                contactId = RcsContactHelp.getContactIdWithPhoneNumber(mContext, item.mPhone);
                name = RcsContactHelp.getNameWithContactId(mContext, contactId);
                String path = RcsIconManager.getIconPathByPhone(item.mPhone);
                icon = TextUtils.isEmpty(path) ? null : Uri.parse("file://" + path);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!TextUtils.equals(item.mPhone, (String) textView.getTag())) {
                    return;
                }
                String dispName = null;
                if (!TextUtils.isEmpty(name)) {
                    dispName = name;
                } else {
                    dispName = TextUtils.isEmpty(item.mDisplayName) ? item.mPhone
                            : item.mDisplayName;
                }
                textView.setText(formateText(dispName, isMe, isChairMan));
                if (!TextUtils.isEmpty(item.mDisplayName) && !TextUtils.equals(dispName, item.mDisplayName) && !isMe) {
                    nickNameTextView.setVisibility(View.VISIBLE);
                    nickNameTextView
                            .setText(mContext.getString(R.string.group_business_card) + ": " + item.mDisplayName);
                } else {
                    nickNameTextView.setVisibility(View.GONE);
                }
                imageButton.setVisibility(TextUtils.equals(mChairMan, RcsMmsUtils.getLastLoginMsisdn()) && !isChairMan
                        ? View.VISIBLE : View.GONE);
                imageView.setImageResourceUri(AvatarUriUtil.createAvatarUri(icon, name, null, null), contactId, null,
                        item.mPhone);
                if (mCheckBoxVisibility) {
                    imageButton.setVisibility(View.GONE);
                    checkBox.setVisibility(TextUtils.equals(mChairMan, RcsMmsUtils.getLastLoginMsisdn()) && !isChairMan
                            ? View.VISIBLE : View.GONE);
                    checkBox.setChecked(TextUtils.equals(item.mPhone, mSelectedPhone));
                }
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String formateText(String text, boolean isMe, boolean isChairMan) {
        StringBuilder builder = new StringBuilder();
        if (isMe) {
            builder.append(mContext.getString(R.string.me));
        } else {
            builder.append(text);
        }
        if (isChairMan) {
            builder.append(" - ").append(mContext.getString(R.string.chairman));
        }
        return builder.toString();
    }
}