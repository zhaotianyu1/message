package com.juphoon.helper.mms.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.util.AvatarUriUtil;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.helper.RcsGroupHelper.RcsGroupMember;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsContactHelp;
import com.juphoon.rcs.tool.RcsGroupChatManager;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.RmsGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RcsGroupDetailActivity extends BugleActionBarActivity {
    private final String TAG = RcsGroupDetailActivity.class.getSimpleName();
    protected static final int REQUEST_CONTACT = 100;
    protected static final int REQUEST_PHONE = 101;
    public static final String GROUP_CHAT_ID = "group_chat_id";
    private String mGroupChatId;
    private ArrayList<String> mPhones;// 记录添加的号码
    private TextView mMemberNumber;
    private LinearLayout mHeader;
    private LinearLayout mNameContainer, mDisplayNameContainer;
    private TextView mNameTitle, mDisplayNameTitle, mNameView, mDisplayNameView;
    private RcsGroupInfo mGroupInfo;
    private ProgressDialog mProgress;
    private CardView mDissloveGroup;
    private Runnable mRunnable;
    private List<LinearLayout> mListMemberContainer;

    private final RcsBroadcastHelper.IGroupListener mIGroupListener = new RcsBroadcastHelper.IGroupListener() {

        @Override
        public void onGroupSessChange(String groupChatId, boolean have) {
            if (TextUtils.equals(groupChatId, mGroupChatId)) {
                if (mProgress != null) {
                    mProgress.dismiss();
                    mProgress = null;
                }
                if (have) {
                    if (mRunnable != null) {
                        mRunnable.run();
                        mRunnable = null;
                    }
                } else {
                    if (mRunnable != null) {
                        mRunnable = null;
                        Toast.makeText(RcsGroupDetailActivity.this, R.string.rejoin_fail, Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        }

        @Override
        public void onGroupRecvInvite(String groupChatId) {
        }

        @Override
        public void onGroupInfoChange(String groupChatId) {
            if (TextUtils.equals(groupChatId, mGroupChatId)) {
                updateGroup();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcsgroup_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mGroupChatId = getIntent().getStringExtra(GROUP_CHAT_ID);
        if (TextUtils.isEmpty(mGroupChatId)) {
            finish();
            return;
        }
        RcsBroadcastHelper.addGroupListener(mIGroupListener);
        findView();
        setTitle(R.string.group_detail);
        updateGroup();
    }

    private void findView() {
        mMemberNumber = (TextView) findViewById(R.id.rcsgroup_detail_member_number);
        mHeader = (LinearLayout) findViewById(R.id.rcsgroup_detail_title);
        mNameContainer = (LinearLayout) findViewById(R.id.rcsgroup_detail_name);
        mDisplayNameContainer = (LinearLayout) findViewById(R.id.rcsgroup_detail_displayname);
        mNameTitle = (TextView) mNameContainer.findViewById(R.id.item_rcsgroup_detail_title);
        mDisplayNameTitle = (TextView) mDisplayNameContainer.findViewById(R.id.item_rcsgroup_detail_title);
        mNameView = (TextView) mNameContainer.findViewById(R.id.item_rcsgroup_detail_content);
        mDisplayNameView = (TextView) mDisplayNameContainer.findViewById(R.id.item_rcsgroup_detail_content);
        mDissloveGroup = (CardView) findViewById(R.id.rcsgroup_detail_disslove_button);
        mListMemberContainer = new ArrayList<>();
        mListMemberContainer.add((LinearLayout) findViewById(R.id.rcsgroup_detail_member1));
        mListMemberContainer.add((LinearLayout) findViewById(R.id.rcsgroup_detail_member2));
        mListMemberContainer.add((LinearLayout) findViewById(R.id.rcsgroup_detail_member3));
        mListMemberContainer.add((LinearLayout) findViewById(R.id.rcsgroup_detail_member4));
    }

    private void config() {
        if (mGroupInfo.mState != RmsDefine.RmsGroup.STATE_STARTED) {
            finish();
        }
        mMemberNumber.setText(String.format("(%1$d)", mGroupInfo.mListGroupMembers.size()));
        for (int i = 0; i < mGroupInfo.mListGroupMembers.size(); i++) {
            if (TextUtils.equals(mGroupInfo.mChairman, mGroupInfo.mListGroupMembers.get(i).mPhone)) {
                Collections.swap(mGroupInfo.mListGroupMembers, 0, i);
                break;
            }
        }

        for (int i = 0; i < mGroupInfo.mListGroupMembers.size() && i < 4; i++) {
            LinearLayout layout = mListMemberContainer.get(i);
            layout.setVisibility(View.VISIBLE);
            setImageAndText(layout, mGroupInfo.mListGroupMembers.get(i));
        }
        for (int i = 3; i >= mGroupInfo.mListGroupMembers.size(); i--) {
            mListMemberContainer.get(i).setVisibility(View.GONE);
        }

        mNameContainer.findViewById(R.id.item_rcsgroup_detail_icon).setBackground(getResources().getDrawable(R.drawable.icon_group_name));
        mDisplayNameContainer.findViewById(R.id.item_rcsgroup_detail_icon).setBackground(getResources().getDrawable(R.drawable.icon_my_group_name));
        mNameTitle.setText(R.string.group_name);
        mDisplayNameTitle.setText(R.string.group_business_card);
        mNameView.setText(mGroupInfo.mSubject);
        mDisplayNameView.setText(mGroupInfo.getDisplayName(RcsServiceManager.getUserName()));
        if (!mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
            mDissloveGroup.setVisibility(View.GONE);
        } else {
            mDissloveGroup.setVisibility(View.VISIBLE);
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.rcsgroup_detail_member_list:
            case R.id.rcsgroup_detail_member1:
            case R.id.rcsgroup_detail_member2:
            case R.id.rcsgroup_detail_member3:
            case R.id.rcsgroup_detail_member4:
                startGroupMemberActivity();
                break;

            case R.id.rcsgroup_detail_add:
                if (isLogined()) {
                    StringBuffer excludePhones = new StringBuffer();
                    for (RcsGroupMember member : mGroupInfo.mListGroupMembers) {
                        if (excludePhones.length() > 0) {
                            excludePhones.append(";");
                        }
                        excludePhones.append(member.mPhone);
                    }
                    Intent intent = new Intent(RcsGroupDetailActivity.this, RcsChooseActivity.class);
                    intent.putExtra(RcsChooseActivity.EXCLUDE_PHONES, excludePhones.toString());
                    intent.putExtra(RcsChooseActivity.TITLE, getString(R.string.choose_contact));
                    startActivityForResult(intent, REQUEST_CONTACT);
                }
                break;

            case R.id.rcsgroup_detail_name:
                if (isLogined() && mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
                    showEditerNameDialog();
                }
                break;

            case R.id.rcsgroup_detail_displayname:
                if (isLogined()) {
                    showEditerDisplayNameDialog();
                }
                break;

            default:
                break;
        }
    }

    public void setImageAndText(LinearLayout layout, final RcsGroupMember member) {
        final ContactIconView imageView = (ContactIconView) layout.findViewById(R.id.rcsgroup_detail_photo);
        final TextView textView = (TextView) layout.findViewById(R.id.rcsgroup_detail_text);
        final boolean me = TextUtils.equals(member.mPhone, RcsMmsUtils.getLastLoginMsisdn());
        textView.setTag(member.mPhone);
        if (me) {
            textView.setText(R.string.me);
        } else if (!TextUtils.isEmpty(member.mDisplayName)) {
            textView.setText(member.mDisplayName);
        } else {
            textView.setText(member.mPhone);
        }

        new AsyncTask<Void, Void, Void>() {
            long contactId;
            String name;
            Uri icon;

            @Override
            protected Void doInBackground(Void... arg0) {
                contactId = RcsContactHelp.getContactIdWithPhoneNumber(RcsGroupDetailActivity.this, member.mPhone);
                name = RcsContactHelp.getNameWithContactId(RcsGroupDetailActivity.this, contactId);
                icon = RcsContactHelp.getPhotoUriWithContactId(RcsGroupDetailActivity.this, contactId);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!TextUtils.equals(member.mPhone, (String) textView.getTag())) {
                    return;
                }
                if (icon != null) {
                    imageView.setImageResourceUri(
                            AvatarUriUtil.createAvatarUri(icon,
                                    name, null, null),
                            0, null,
                            member.mPhone);
                } else {
                    imageView.setImageResourceUri(
                            AvatarUriUtil.createAvatarUri(null, member.mDisplayName, null, null),
                            0, null,
                            member.mPhone);
                }
                if (!TextUtils.isEmpty(name) && !me) {
                    textView.setText(name);
                }
                imageView.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        startGroupMemberActivity();
                    }
                });
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onDestroy() {
        RcsBroadcastHelper.removeGroupListener(mIGroupListener);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CONTACT) {
                mPhones = data.getStringArrayListExtra(RcsChooseActivity.RESULT_PHONES);
                if (mPhones != null) {
                    if (isLogined()) {
                        doGroupAction(new Runnable() {

                            @Override
                            public void run() {
                                if (mPhones.size() > 1) {
                                    String numbers = TextUtils.join(";", mPhones);
                                    RcsCallWrapper.rcsGroupAddMembers(mGroupChatId, numbers);
                                } else {
                                    RcsCallWrapper.rcsGroupAddMember(mGroupChatId, mPhones.get(0));
                                }
                            }
                        });
                    }
                }
            } else if (requestCode == REQUEST_PHONE) {
                final String removePhones = data.getStringExtra(RcsGroupMemberActivity.REMOVE_PHONES);
                final String chairPhone = data.getStringExtra(RcsGroupMemberActivity.SET_CHAIRMAN_PHONE);
                if (!TextUtils.isEmpty(removePhones)) {
                    if (isLogined() && mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
                        doGroupAction(new Runnable() {

                            @Override
                            public void run() {
                                if (RcsCallWrapper.rcsGroupRemoveMember(mGroupChatId, removePhones)) {
                                    Toast.makeText(RcsGroupDetailActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(RcsGroupDetailActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
                if (!TextUtils.isEmpty(chairPhone)) {
                    doGroupAction(new Runnable() {

                        @Override
                        public void run() {
                            if (RcsCallWrapper.rcsGroupModifyChairman(mGroupChatId, chairPhone)) {
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }
    }

    private boolean isLogined() {
        if (RcsServiceManager.isLogined()) {
            return true;
        }
        Toast.makeText(this, R.string.not_login, Toast.LENGTH_SHORT).show();
        return false;
    }

    private boolean doGroupAction(Runnable runnable) {
        if (RcsCallWrapper.rcsGroupSessIsExist(mGroupChatId)) {
            runnable.run();
            return true;
        } else {
            if (mProgress == null) {
                mProgress = ProgressDialog.show(this, getString(R.string.label_rejoin), getString(R.string.msg_rejoining_group));
            }
            mRunnable = runnable;
            RcsGroupChatManager.rejoinGroup(mGroupChatId, mGroupInfo.mSessionIdentify, mGroupInfo.mSubject, mGroupInfo.mGroupNickName);
            return false;
        }
    }

    private void showNextChairmanDialog() {
        new AlertDialog.Builder(this).setMessage(R.string.msg_next_chairman_before_leave).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                startGroupMemberActivity();
            }
        }).create().show();
    }

    private void showEditerNameDialog() {
        final EditText editText = new EditText(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        linearLayout.setPadding(50, 0, 50, 0);
        editText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        editText.setText(mGroupInfo.mSubject);
        editText.setSingleLine();
        linearLayout.addView(editText);
        editText.setSelection(mGroupInfo.mSubject.length());
        new AlertDialog.Builder(this).setTitle(R.string.group_name).setView(linearLayout).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                final String groupName = editText.getText().toString();
                if (isLogined() && mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
                    doGroupAction(new Runnable() {
                        @Override
                        public void run() {
                            if (RcsCallWrapper.rcsGroupModifySubject(mGroupChatId, groupName)) {
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

//    private void showEditerNickNameDialog() {
//        final EditText editText = new EditText(this);
//        InputFilter[] filters = {new LengthFilter(10)};
//        LinearLayout linearLayout = new LinearLayout(this);
//        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
//        linearLayout.setPadding(50, 0, 50, 0);
//        editText.setFilters(filters);
//        editText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
//        editText.setText(mGroupInfo.mGroupNickName);
//        editText.setSingleLine();
//        linearLayout.addView(editText);
//        editText.setSelection(mGroupInfo.mGroupNickName.length());
//        new AlertDialog.Builder(this).setTitle(R.string.group_nickname).setView(linearLayout).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface arg0, int arg1) {
//                updateGroupNickName(editText.getText().toString());
//            }
//        }).setNegativeButton(android.R.string.cancel, null).show();
//    }

    private void updateGroupNickName(final String nickName) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... arg0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(RmsGroup.NICK_NAME, nickName);
                getContentResolver().update(RmsGroup.CONTENT_URI, contentValues, RmsGroup.GROUP_CHAT_ID + "=?", new String[]{mGroupChatId});
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                updateGroup();
            }


        }.execute();
    }

    private void showEditerDisplayNameDialog() {
        final EditText nameEditText = new EditText(this);
        InputFilter[] filters = {new LengthFilter(20)};
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        linearLayout.setPadding(50, 0, 50, 0);
        nameEditText.setFilters(filters);
        nameEditText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        nameEditText.setText(mGroupInfo.getDisplayName(RcsServiceManager.getUserName()));
        nameEditText.setSingleLine();
        linearLayout.addView(nameEditText);
        nameEditText.setSelection(mGroupInfo.getDisplayName(RcsServiceManager.getUserName()).length());
        new AlertDialog.Builder(this).setTitle(R.string.group_business_card).setView(linearLayout).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                final String displayName = nameEditText.getText().toString();
                if (displayName.length() > 0) {
                    if (isLogined()) {
                        doGroupAction(new Runnable() {

                            @Override
                            public void run() {
                                if (isLogined()) {
                                    if (RcsCallWrapper.rcsGroupModifyDisplayName(mGroupChatId, displayName)) {
                                        Toast.makeText(RcsGroupDetailActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(RcsGroupDetailActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    public void quitGroup(View view) {
        new AlertDialog.Builder(this).setTitle(R.string.exit_group).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
                    showNextChairmanDialog();
                } else if (isLogined()) {
                    doGroupAction(new Runnable() {
                        @Override
                        public void run() {
                            if (RcsCallWrapper.rcsGroupLeave(mGroupChatId)) {
                                setResult(RESULT_OK);
                                finish();
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).setNegativeButton(android.R.string.cancel, null).create().show();

    }

    public void onDissloveGroup(View view) {
        new AlertDialog.Builder(this).setTitle(R.string.disslove_group).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (isLogined() && mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
                    doGroupAction(new Runnable() {

                        @Override
                        public void run() {
                            if (RcsCallWrapper.rcsGroupDissolve(mGroupChatId)) {
                                setResult(RESULT_OK);
                                finish();
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RcsGroupDetailActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).setNegativeButton(android.R.string.cancel, null).create().show();

    }

    private void updateGroup() {
        new AsyncTask<Void, Void, RcsGroupInfo>() {

            @Override
            protected RcsGroupInfo doInBackground(Void... arg0) {
                return RcsGroupHelper.loadGroupInfo(mGroupChatId);
            }

            @Override
            protected void onPostExecute(RcsGroupInfo groupInfo) {
                mGroupInfo = groupInfo;
                config();
                if (mGroupInfo.mListGroupMembers.size() == 0) {
                    RcsCallWrapper.rcsImGetGroupInfo(mGroupInfo.mGroupChatId, mGroupInfo.mSessionIdentify);
                }
            }

        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGroup();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startGroupMemberActivity() {
        Intent intent = new Intent(RcsGroupDetailActivity.this, RcsGroupMemberActivity.class);
        intent.putExtra(RcsGroupMemberActivity.GROUP_CHAT_ID, mGroupChatId);
        startActivityForResult(intent, REQUEST_PHONE);
    }

    private boolean isInShowGroupMember(String phone) {
        if (mGroupInfo != null) {
            int i = 0;
            for (RcsGroupMember member : mGroupInfo.mListGroupMembers) {
                if (i >= 4) {
                    break;
                }
                if (TextUtils.equals(member.mPhone, phone)) {
                    return true;
                }
            }
        }
        return false;
    }
}
