package com.juphoon.helper.mms.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.util.AvatarUriUtil;
import com.juphoon.helper.mms.RcsVCardHelper;
import com.juphoon.rcs.tool.Trans2PinYin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RcsChooseContactActivity extends BugleActionBarActivity {

    private static final int COLUMN_CONTACT_ID = 0;
    private static final int COLUMN_DISPLAY_NAME = 1;
    private static final int COLUMN_HAS_PHONE_NUMBER = 2;
    private static final int COLUMN_PHOTO_URI = 3;
    private static final int COLUMN_SORT_KEY_PRIMARY = 4;

    private RcsChooseContactActivity mContext;

    String[] projectContact = new String[]{
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.SORT_KEY_PRIMARY,
    };

    protected List<String> mListNotShowPhones;
    protected List<Contact> mListContacts;
    private ContactsAdapter mAdapter;
    private ListView mListView;
    protected ArrayList<Long> mListSelectContact;
    private MenuItem mSureMenu;
    private TextView mNoResultTextView;

    protected class Contact {
        public long mId;
        public String mName;
        public String mSortKey;
        public boolean mEnabled;
        public boolean mIsChecked;
        public String mPhone;
        public String mPhotoUri;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_choose_contact);
        mContext = this;
        initView();
        initData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rcs_choose, menu);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        mSureMenu = menu.findItem(R.id.action_sure);
        mSureMenu.setVisible(false);
        searchMenu.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_sure:
                doChooseContactsAction();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initData() {
        mListSelectContact = new ArrayList<>();
        new ContactLoadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initView() {
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mListView = (ListView) findViewById(R.id.contact_choose_list);
        mNoResultTextView=(TextView) findViewById(R.id.contact_no_search_result_text);
        mAdapter = new ContactsAdapter();
        mListContacts = new ArrayList<>();
        mListView.setAdapter(mAdapter);
    }

    protected void doChooseContactsAction() {
        if (mListSelectContact.size() > 0) {
            Intent intent = new Intent();
            Uri vcfUri = RcsVCardHelper.genVcards(this, RcsVCardHelper.exportVCards(this, false, mListSelectContact));
            if (vcfUri != null) {
                intent.setData(vcfUri);
                setResult(RESULT_OK, intent);
            }
        }
        finish();
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
            Cursor cursor = mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    projectContact, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        Contact contact = new Contact();
                        contact.mId = cursor.getLong(COLUMN_CONTACT_ID);
                        contact.mName = cursor.getString(COLUMN_DISPLAY_NAME);
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
                        contact.mEnabled = true;
                        contact.mIsChecked = false;
                        if (TextUtils.equals(contact.mSortKey, "#")) {
                            listOtherContacts.add(contact);
                        } else {
                            listContacts.add(contact);
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
            mListContacts.clear();
            mListContacts.addAll(result);
            ((ContactsAdapter) mListView.getAdapter()).notifyDataSetChanged();
            if (mListContacts.size() > 0) {
                mListView.setVisibility(View.VISIBLE);
                mNoResultTextView.setVisibility(View.GONE);
            } else {
                mListView.setVisibility(View.GONE);
                mNoResultTextView.setVisibility(View.VISIBLE);
            }
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
                LayoutInflater inflater = LayoutInflater.from(mContext);
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

            final Contact contact = mListContacts.get(position);
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
                holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        checkContactPhone(isChecked, contact.mId);
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

    private void checkContactPhone(boolean check, Long contactId) {
        if (!check) {
            for (Contact c : mListContacts) {
                if (c.mId == contactId) {
                    c.mIsChecked = false;
                }
            }
            mListSelectContact.remove(contactId);
        } else {
            for (Contact c : mListContacts) {
                if (c.mId == contactId) {
                    c.mIsChecked = true;
                }
            }
            if (!mListSelectContact.contains(contactId)) {
                mListSelectContact.add(contactId);
            }
        }
        onContactsSelectedChanged(mListSelectContact);
    }

    public void onContactsSelectedChanged(List<Long> selectedContacts) {
        mListSelectContact = (ArrayList<Long>) selectedContacts;
        setTitle(String.format(getString(R.string.choose_contact_num), "选择联系人", String.valueOf(mListSelectContact.size())));
        if (mListSelectContact.size() > 0) {
            mSureMenu.setVisible(true);
        } else {
            mSureMenu.setVisible(false);
        }
    }

    private String getSortKey(String sortKeyString) {
        String key = sortKeyString.substring(0, 1).toUpperCase(Locale.getDefault());
        if (key.matches("[A-Z]")) {
            return key;
        }
        return "#";
    }

}
