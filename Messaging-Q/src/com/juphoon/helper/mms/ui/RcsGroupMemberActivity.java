package com.juphoon.helper.mms.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.helper.RcsGroupHelper.RcsGroupMember;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsGroupChatManager;
import com.juphoon.rcs.tool.RcsServiceManager;

import java.util.ArrayList;
import java.util.List;

public class RcsGroupMemberActivity extends BugleActionBarActivity {
    public static final String GROUP_CHAT_ID = "group_chat_id";
    public static final String MEMBER_MODE = "mode";
    public static final String CHOOSE_NAME = "choose_name";
    public static final String CHOOSE_NUMBER = "choose_number";
    public static final String SELECTED_NUMBER = "selected_number";
    public static final int MEMBER_MODE_ALL = 1;
    public static final int MEMBER_MODE_AT = 2;
    public static final int MEMBER_MODE_REMOVE_MEMBER = 3;
    protected static final String SET_CHAIRMAN_PHONE = "chairman_phone";
    protected static final String REMOVE_PHONES = "remove_phones";
    private static final int GROUP_MEMBER_REMOVE_PHONE = 1001;
    private static final int GROUP_MEMBER_SET_CHAIRMAN = 1002;
    private ListView mListView;
    private RcsGroupMemberAdapter mAdapter;
    private String mGroupChatId;
    private RcsGroupInfo mGroupInfo;
    private int mMemberMode;
    private List<RcsGroupMember> mListMembers;
    private List<RcsGroupMember> mOriginalListMembers;
    private SearchView mSearchView;
    private Runnable mRunnable;

    private final RcsBroadcastHelper.IGroupListener mIGroupListener = new RcsBroadcastHelper.IGroupListener() {

        @Override
        public void onGroupSessChange(String groupChatId, boolean have) {
            if (TextUtils.equals(groupChatId, mGroupChatId)) {
                if (have) {
                    if (mRunnable != null) {
                        mRunnable.run();
                        mRunnable = null;
                    }
                } else {
                    if (mRunnable != null) {
                        mRunnable = null;
                        Toast.makeText(RcsGroupMemberActivity.this, R.string.rejoin_fail, Toast.LENGTH_SHORT).show();
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
        }

    };

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_rcsgroup_member);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        RcsBroadcastHelper.addGroupListener(mIGroupListener);
        mGroupChatId = getIntent().getStringExtra(GROUP_CHAT_ID);
        mMemberMode = getIntent().getIntExtra(MEMBER_MODE, MEMBER_MODE_ALL);
        if (TextUtils.isEmpty(mGroupChatId)) {
            finish();
            return;
        }
        mGroupInfo = RcsGroupHelper.getGroupInfo(mGroupChatId);
        if (mGroupInfo == null) {
            finish();
            return;
        } else {
            updateMemberList();
            mListView = (ListView) findViewById(R.id.rcsgroup_member_listview);
            mAdapter = new RcsGroupMemberAdapter(this);
            if (mMemberMode == MEMBER_MODE_AT) {
                mListView.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapter, View view, int index, long id) {
                        Intent intent = new Intent();
                        intent.putExtra(CHOOSE_NUMBER, mListMembers.get(index).mPhone);
                        intent.putExtra(CHOOSE_NAME, mListMembers.get(index).mDisplayName);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            } else if (mMemberMode == MEMBER_MODE_REMOVE_MEMBER) {
                mAdapter.setCheckBoxVisibility(View.VISIBLE);
                mAdapter.setmSelectedPhone(getIntent().getStringExtra(SELECTED_NUMBER));
                mAdapter.notifyDataSetChanged();
                mListView.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapter, View view, int index, long id) {
                        CheckBox itemCheckBox = (CheckBox) view.findViewById(R.id.rcsgroupmember_selected);
                        itemCheckBox.setChecked(!itemCheckBox.isChecked());
                    }
                });
            }
            mAdapter.setInfo(mGroupInfo.mChairman, mListMembers);
            mListView.setAdapter(mAdapter);
            if (mMemberMode == MEMBER_MODE_ALL && mGroupInfo.isChairMan(RcsMmsUtils.getLastLoginMsisdn())) {
                mListView.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        showItemPopMenu(view, position);
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RcsBroadcastHelper.removeGroupListener(mIGroupListener);
    }

    private void updateMemberList() {
        String myPhone = RcsMmsUtils.getLastLoginMsisdn();
        mListMembers = new ArrayList<>();
        mOriginalListMembers = new ArrayList<>();
        for (RcsGroupMember member : mGroupInfo.mListGroupMembers) {
            if (mMemberMode == MEMBER_MODE_AT) {
                if (TextUtils.equals(member.mPhone, myPhone)) {
                    continue;
                }
            }
            mListMembers.add(member);
        }
        mOriginalListMembers.addAll(mListMembers);
    }

    private void showItemPopMenu(View view, final int position) {
        if (position == 0) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, view.findViewById(R.id.pop_loc_view));
        Menu menu = popupMenu.getMenu();
        menu.add(0, GROUP_MEMBER_REMOVE_PHONE, Menu.NONE, R.string.move_group);
        menu.add(0, GROUP_MEMBER_SET_CHAIRMAN, Menu.NONE, R.string.set_chairman);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                RcsGroupMember member = mListMembers.get(position);
                Intent intent = new Intent();
                switch (item.getItemId()) {
                    case GROUP_MEMBER_REMOVE_PHONE:
                        intent.setClass(RcsGroupMemberActivity.this, RcsGroupMemberActivity.class);
                        intent.putExtra(GROUP_CHAT_ID, mGroupChatId);
                        intent.putExtra(SELECTED_NUMBER, member.mPhone);
                        intent.putExtra(MEMBER_MODE, MEMBER_MODE_REMOVE_MEMBER);
                        startActivity(intent);
                        finish();
                        break;
                    case GROUP_MEMBER_SET_CHAIRMAN:
                        intent.putExtra(SET_CHAIRMAN_PHONE, member.mPhone);
                        setResult(RESULT_OK, intent);
                        finish();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private boolean isGroupMember(String phone) {
        if (mGroupInfo != null) {
            for (RcsGroupMember member : mGroupInfo.mListGroupMembers) {
                if (TextUtils.equals(member.mPhone, phone)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rcs_people, menu);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        MenuItem confirmMenu = menu.findItem(R.id.action_sure);
        if (mMemberMode == MEMBER_MODE_REMOVE_MEMBER) {
            confirmMenu.setVisible(true);
        } else {
            searchMenu.setVisible(true);
        }
        mSearchView = (SearchView) searchMenu.getActionView();
        SearchView.SearchAutoComplete textView = (SearchView.SearchAutoComplete) mSearchView.findViewById(R.id.search_src_text);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String arg0) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String string) {
                mListMembers.clear();
                mListMembers.addAll(mOriginalListMembers);
                if (!TextUtils.isEmpty(string)) {
                    for (RcsGroupMember rcsGroupMember : mOriginalListMembers) {
                        if (!rcsGroupMember.mDisplayName.contains(string)
                                && !rcsGroupMember.mPhone.contains(string)) {
                            mListMembers.remove(rcsGroupMember);
                        }
                    }
                }
                mAdapter.notifyDataSetChanged();
                return false;
            }
        });
        textView.setTextColor(Color.BLACK);
        textView.setHintTextColor(Color.GRAY);
        textView.setBackgroundColor(Color.WHITE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_search:
                return false;
            case R.id.action_sure:
                final List<String> selectList = mAdapter.getSelectMembers();
                if (selectList != null && selectList.size() != 0) {
                    final StringBuffer removePhones = new StringBuffer();
                    for (int i = 0; i < selectList.size(); i++) {
                        removePhones.append(selectList.get(i));
                        if (i != selectList.size() - 1) {
                            removePhones.append(";");
                        }
                    }
                    if (isLogined() && mGroupInfo.isChairMan(RcsServiceManager.getUserName())) {
                        doGroupAction(new Runnable() {

                            @Override
                            public void run() {
                                if (selectList.size() == 1 ? RcsCallWrapper.rcsGroupRemoveMember(mGroupChatId, selectList.get(0)) : RcsCallWrapper.rcsGroupRemoveMembers(mGroupChatId, removePhones.toString())) {
                                    finish();
                                    Toast.makeText(RcsGroupMemberActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(RcsGroupMemberActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
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
            mRunnable = runnable;
            RcsGroupChatManager.rejoinGroup(mGroupChatId, mGroupInfo.mSessionIdentify, mGroupInfo.mSubject, mGroupInfo.mGroupNickName);
            return false;
        }
    }


}
