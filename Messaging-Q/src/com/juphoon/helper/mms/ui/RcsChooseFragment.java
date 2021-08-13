package com.juphoon.helper.mms.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.messaging.R;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.util.AvatarUriUtil;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.rcs.tool.RcsNumberUtils;
import com.juphoon.rcs.tool.Trans2PinYin;
import com.juphoon.service.RmsDefine.RmsGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RcsChooseFragment extends Fragment {

    // 常量
    public static final int MODE_CONTACTS = 0;
    public static final int MODE_GROUPS = MODE_CONTACTS + 1;
    public static final String FRAGMENT_MODE = "fragment_mode";

    private static final String[] PROJECTION_PHONE = new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI, ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY };
    private static final int COLUMN_CONTACT_ID = 0;
    private static final int COLUMN_DISPLAY_NAME = 1;
    private static final int COLUMN_NUMBER = 2;
    private static final int COLUMN_PHOTO_URI = 3;
    private static final int COLUMN_SORT_KEY_PRIMARY = 4;

    private static final String[] PROJECTION_GROUP = new String[] { RmsGroup.GROUP_CHAT_ID, RmsGroup.NAME };
    private static final String SELECTION_GROUP = RmsGroup.STATE + " = " + RmsGroup.STATE_STARTED;

    // 变量
    private int mMode;
    protected List<Group> mListGroups;
    protected List<Group> mOriginalListGroups;
    protected List<Contact> mListContacts;
    protected List<Contact> mOriginalListContacts;
    protected List<Contact> mAddMemberList;
    protected List<String> mListSelectPhones;
    protected List<String> mListExcludePhones;
    protected List<String> mListNotShowPhones;
    protected int mLimitMinSelect;
    protected int mLimitMaxSelect;
    protected String mSelectGroupChatId;

    // view
    private ListView mListView;
    private ContactsAdapter mAdapter;
    private RecyclerView mMemberListRecyclerView; //底部的成员list
    private RcsBottomMemberListAdapter mMemberListAdapter;


    protected class Contact {
        public long mId;
        public String mName;
        public String mSortKey;
        public boolean mEnabled;
        public boolean mIsChecked;
        public String mPhone;
        public String mPhotoUri;
    }

    protected class Group {
        public String mName;
        public String mGroupChatId;
    }

    public static final RcsChooseFragment newInstance(Bundle bundle) {
        RcsChooseFragment rcsChooseFragment = new RcsChooseFragment();
        rcsChooseFragment.setArguments(bundle);
        return rcsChooseFragment;
    }

    private OnSelectedChangeListener mOnSelectedChangeListener;

    public interface OnSelectedChangeListener {
        void onContactsSelectedChanged(List<String> selectedContacts);

        void onGroupSelectedChanged(String groupChatId);
    }

    public void setOnSelectedChangeListener(OnSelectedChangeListener listener) {
        mOnSelectedChangeListener = listener;
    }

    private void init() {
        mLimitMinSelect = getArguments().getInt(RcsChooseActivity.SAVE_MIN_SELECT, 1);
        mLimitMaxSelect = getArguments().getInt(RcsChooseActivity.SAVE_MAX_SELECT, -1);
        mListExcludePhones = getArguments().getStringArrayList(RcsChooseActivity.SAVE_EXCLUDE_PHONES);
        mListNotShowPhones = getArguments().getStringArrayList(RcsChooseActivity.SAVE_NOTSHOW_PHONES);
        mListSelectPhones = getArguments().getStringArrayList(RcsChooseActivity.SAVE_SELECT_PHONES);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 确保外部调用是newInstance
        mMode = getArguments().getInt(FRAGMENT_MODE, MODE_CONTACTS);
        if (mMode == MODE_CONTACTS) {
            init();
            new ContactLoadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (mMode == MODE_GROUPS) {
            new GroupLoadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return inflater.inflate(R.layout.fragment_choose_contacts_or_groups, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = (ListView) view.findViewById(R.id.fragment_choose_list);
        if (mMode == MODE_CONTACTS) {
            mAdapter = new ContactsAdapter();
            mListContacts = new ArrayList<>();
            mAddMemberList = new ArrayList<>();
            mOriginalListContacts = new ArrayList<>();
            mListView.setAdapter(mAdapter);
            mMemberListRecyclerView = (RecyclerView) view.findViewById(R.id.bottm_view_member_list);
            mMemberListRecyclerView.setVisibility(View.VISIBLE);
            mMemberListAdapter = new RcsBottomMemberListAdapter(getActivity(), mAddMemberList);
            final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
            manager.setOrientation(LinearLayoutManager.HORIZONTAL);
            mMemberListRecyclerView.setHasFixedSize(true);
            mMemberListRecyclerView.setLayoutManager(manager);
            mMemberListRecyclerView.setAdapter(mMemberListAdapter);
            mMemberListAdapter.setOnItemClickLitener(new RcsBottomMemberListAdapter.OnItemClickLitener() {
                
                @Override
                public void onItemClick(View view, int position) {
                    List<Contact> temp = new ArrayList<>();
                    String phone = mAddMemberList.get(position).mPhone;
                    for (Contact addlistContact : mAddMemberList) {
                        if (TextUtils.equals(phone, addlistContact.mPhone)) {
                            temp.add(addlistContact);
                        }
                    }
                    for (Contact contact : temp) {
                        mAddMemberList.remove(contact);
                    }
                    for (Contact contact : mListContacts) {
                        if (TextUtils.equals(phone, contact.mPhone)) {
                            contact.mIsChecked = false;
                        }
                    }
                    mListSelectPhones.remove(phone);
                    mAdapter.notifyDataSetChanged();
                    mMemberListAdapter.notifyDataSetChanged();
                    mOnSelectedChangeListener.onContactsSelectedChanged(mListSelectPhones);
                }
            });
        } else if (mMode == MODE_GROUPS) {
            mListGroups = new ArrayList<>();
            mOriginalListGroups = new ArrayList<>();
            mListView.setAdapter(new GroupAdapter());
        }
    }

    /**
     * 查询存在的群组
     */
    private class GroupLoadTask extends AsyncTask<Void, Void, List<Group>> {

        @Override
        protected List<Group> doInBackground(Void... arg0) {
            List<Group> listGroups = new ArrayList<>();
            Cursor cursor = getActivity().getContentResolver().query(RmsGroup.CONTENT_URI, PROJECTION_GROUP,
                    SELECTION_GROUP, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        Group group = new Group();
                        group.mGroupChatId = cursor.getString(cursor.getColumnIndex(RmsGroup.GROUP_CHAT_ID));
                        group.mName = cursor.getString(cursor.getColumnIndex(RmsGroup.NAME));
                        listGroups.add(group);
                    }
                } finally {
                    cursor.close();
                }
            }
            return listGroups;
        }

        @Override
        protected void onPostExecute(List<Group> result) {
            super.onPostExecute(result);
            mListGroups.clear();
            mOriginalListGroups.clear();
            mListGroups.addAll(result);
            mOriginalListGroups.addAll(result);
            ((GroupAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }

    }

    /**
     * 查询联系人
     */
    private class ContactLoadTask extends AsyncTask<Void, Void, List<Contact>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected List<Contact> doInBackground(Void... arg0) {
            List<Contact> listContacts = new ArrayList<>();
            List<Contact> listOtherContacts = new ArrayList<>();
            Cursor cursor = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    PROJECTION_PHONE, null, null, ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        Contact contact = new Contact();
                        contact.mId = cursor.getLong(COLUMN_CONTACT_ID);
                        contact.mName = cursor.getString(COLUMN_DISPLAY_NAME);
                        contact.mPhone = RcsNumberUtils.formatPhone86(cursor.getString(COLUMN_NUMBER));
                        contact.mSortKey = cursor.getString(COLUMN_SORT_KEY_PRIMARY);
                        contact.mPhotoUri = cursor.getString(COLUMN_PHOTO_URI);
                        if (mListNotShowPhones != null && mListNotShowPhones.contains(contact.mPhone)) {
                            continue;
                        }
                        if (TextUtils.isEmpty(contact.mSortKey)) {
                            contact.mSortKey = "#";
                        } else {
                            contact.mSortKey = getSortKey(Trans2PinYin.trans2PinYin(contact.mSortKey));
                        }
                        if (mListExcludePhones != null && mListExcludePhones.contains(contact.mPhone)) {
                            contact.mEnabled = false;
                            contact.mIsChecked = true;
                        } else {
                            contact.mEnabled = true;
                            contact.mIsChecked = false;
                        }
                        if (TextUtils.equals(contact.mSortKey, "#")) {
                            listOtherContacts.add(contact);
                        } else {
                            listContacts.add(contact);
                        }
                        if (mListSelectPhones != null && mListSelectPhones.contains(contact.mPhone)) {
                            contact.mIsChecked = true;
                        }
                    }
                    listContacts.addAll(listOtherContacts);
                } finally {
                    cursor.close();
                }
            }
            return listContacts;
        }

        @Override
        protected void onPostExecute(List<Contact> result) {
            super.onPostExecute(result);
            mOriginalListContacts.clear();
            mListContacts.clear();
            mListContacts.addAll(result);
            mOriginalListContacts.addAll(result);
            ((ContactsAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }

    }

    private String getSortKey(String sortKeyString) {
        String key = sortKeyString.substring(0, 1).toUpperCase(Locale.getDefault());
        if (key.matches("[A-Z]")) {
            return key;
        }
        return "#";
    }

    private void checkContactPhone(boolean check, String phone) {
        if (!check) {
            for (Contact c : mListContacts) {
                if (TextUtils.equals(c.mPhone, phone)) {
                    c.mIsChecked = false;
                    if (mAddMemberList.contains(c)) {
                        mAddMemberList.remove(c);
                        mMemberListAdapter.notifyDataSetChanged();
                    }
                }
            }
            mListSelectPhones.remove(phone);
        } else {
            for (Contact c : mListContacts) {
                if (TextUtils.equals(c.mPhone, phone)) {
                    c.mIsChecked = true;
                    if (!mAddMemberList.contains(c)) {
                        mAddMemberList.add(c);
                        mMemberListAdapter.notifyDataSetChanged();
                        mMemberListRecyclerView.scrollToPosition(mAddMemberList.size() - 1);
                    }
                }
            }
            if (!mListSelectPhones.contains(phone)) {
                mListSelectPhones.add(phone);
            }
        }
        ((ContactsAdapter) mListView.getAdapter()).notifyDataSetChanged();
        mOnSelectedChangeListener.onContactsSelectedChanged(mListSelectPhones);
    }

    class GroupAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mListGroups == null ? 0 : mListGroups.size();
        }

        @Override
        public Object getItem(int position) {
            return mListGroups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View contentView, ViewGroup groupView) {
            final ViewHolder holder;
            if (contentView == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                contentView = inflater.inflate(R.layout.choose_contacts_list_item, null);
                holder = new ViewHolder();
                holder.imageView = (ContactIconView) contentView.findViewById(R.id.contact_photo);
                holder.textViewName = (TextView) contentView.findViewById(R.id.contact_name);
                holder.textViewPhone = (TextView) contentView.findViewById(R.id.contact_phone);
                holder.textViewLetter = (TextView) contentView.findViewById(R.id.contact_letter);
                holder.checkBox = (CheckBox) contentView.findViewById(R.id.contact_checkbox);
                contentView.setTag(holder);
            } else {
                holder = (ViewHolder) contentView.getTag();
            }
            holder.textViewPhone.setVisibility(View.GONE);
            holder.textViewLetter.setVisibility(View.GONE);
            final Group group = (Group) getItem(position);
            holder.textViewName.setText(group.mName);
            // juphoon 如果是群聊，人数小于4拼接头像，大于4取4个头像
            Uri iconUri = null;
            RcsGroupInfo rcsGroupInfo = RcsGroupHelper.getGroupInfo(group.mGroupChatId);
            if (rcsGroupInfo != null) {
                final int numParticipants = Math.min(rcsGroupInfo.mListGroupMembers.size(), 4);
                final ArrayList<Uri> avatarUris = new ArrayList<Uri>(numParticipants);
                for (int i = 0; i < numParticipants; i++) {
                    String name = TextUtils.isEmpty(rcsGroupInfo.mListGroupMembers.get(i).mDisplayName)
                            ? rcsGroupInfo.mListGroupMembers.get(i).mDisplayName
                            : rcsGroupInfo.mListGroupMembers.get(i).mPhone;
                    avatarUris.add(AvatarUriUtil.createAvatarUri(null, name, null, null));
                }
                if (avatarUris.size() > 0) {
                    iconUri = AvatarUriUtil.joinAvatarUriToGroup(avatarUris);
                } else {
                    iconUri = AvatarUriUtil.createAvatarUri(null, group.mName, null, null);
                }
            } else {
                iconUri = AvatarUriUtil.createAvatarUri(null, group.mName, null, null);
            }
            holder.imageView.setImageResourceUri(iconUri);

            holder.checkBox.setTag(group.mGroupChatId);
            holder.checkBox.setChecked(TextUtils.equals(group.mGroupChatId, mSelectGroupChatId));
            holder.checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String groupId = (String) buttonView.getTag();
                    if (isChecked && !TextUtils.equals(groupId, mSelectGroupChatId)) {
                        mSelectGroupChatId = groupId;
                    } else if (!isChecked && TextUtils.equals(groupId, mSelectGroupChatId)) {
                        mSelectGroupChatId = null;
                    }
                    notifyDataSetChanged();
                    mOnSelectedChangeListener.onGroupSelectedChanged(mSelectGroupChatId);
                }
            });
            return contentView;
        }

    }

    class ViewHolder {
        ContactIconView imageView;
        TextView textViewName, textViewPhone, textViewLetter;
        CheckBox checkBox;
    }

    class ContactsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mListContacts.size();
        }

        @Override
        public Object getItem(int position) {
            return mListContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View contentView, ViewGroup group) {
            final ViewHolder holder;
            if (contentView == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                contentView = inflater.inflate(R.layout.choose_contacts_list_item, null);
                holder = new ViewHolder();
                holder.imageView = (ContactIconView) contentView.findViewById(R.id.contact_photo);
                holder.textViewName = (TextView) contentView.findViewById(R.id.contact_name);
                holder.textViewPhone = (TextView) contentView.findViewById(R.id.contact_phone);
                holder.textViewLetter = (TextView) contentView.findViewById(R.id.contact_letter);
                holder.checkBox = (CheckBox) contentView.findViewById(R.id.contact_checkbox);
                contentView.setTag(holder);
            } else {
                holder = (ViewHolder) contentView.getTag();
            }

            Contact contact = mListContacts.get(position);
            if (isSectionHeader(position)) {
                holder.textViewLetter.setVisibility(View.VISIBLE);
                holder.textViewLetter.setText(contact.mSortKey);
            } else {
                holder.textViewLetter.setVisibility(View.INVISIBLE);
            }
            holder.textViewName.setText(contact.mName);
            holder.textViewPhone.setText(contact.mPhone);

            holder.imageView.setImageResourceUri(AvatarUriUtil.createAvatarUri(null, contact.mName, null, null),
                    contact.mId, null, null);
            if (contact.mEnabled) {
                holder.checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        checkContactPhone(isChecked, (String) buttonView.getTag());
                    }
                });
            } else {
                holder.checkBox.setOnCheckedChangeListener(null);
            }
            holder.checkBox.setTag(contact.mPhone);
            holder.checkBox.setChecked(contact.mIsChecked);
            holder.checkBox.setEnabled(contact.mEnabled);
            return contentView;
        }

        private boolean isSectionHeader(int position) {
            if (position == 0) {
                return true;
            }
            Contact prevContact = mListContacts.get(position - 1);
            Contact currContact = mListContacts.get(position);
            return !prevContact.mSortKey.equals(currContact.mSortKey);
        }

        public int getPositionForSection(String label) {
            for (int i = 0; i < mListContacts.size(); i++) {
                if (TextUtils.equals(mListContacts.get(i).mSortKey, label)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public void onSerach(String serachString) {
        if (mMode == MODE_GROUPS) {
            mListGroups.clear();
            mListGroups.addAll(mOriginalListGroups);
            if (!TextUtils.isEmpty(serachString)) {
                for (Group group : mOriginalListGroups) {
                    if (!group.mName.contains(serachString)) {
                        mListGroups.remove(group);
                    }
                }
            }
            ((GroupAdapter) mListView.getAdapter()).notifyDataSetChanged();
        } else {
            mListContacts.clear();
            mListContacts.addAll(mOriginalListContacts);
            if (!TextUtils.isEmpty(serachString)) {
                for (Contact contact : mOriginalListContacts) {
                    if (!contact.mPhone.contains(serachString) && !contact.mName.contains(serachString)) {
                        mListContacts.remove(contact);
                    }
                }
                if (mListContacts.size() == 0) {
                    String regEx = "^[+0-9][0-9]+$";
                    Pattern pattern = Pattern.compile(regEx);
                    Matcher matcher = pattern.matcher(serachString);
                    boolean result = matcher.find();
                    if (result) {
                        Contact contact = new Contact();
                        contact.mPhone = RcsNumberUtils.formatPhone86(serachString);
                        contact.mName = contact.mPhone;
                        contact.mEnabled = true;
                        mListContacts.add(contact);
                    }
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}

