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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;

import android.provider.Telephony;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.messaging.BugleApplication;
import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.tcl.ui.fragment.ActConversationListFragment;
import com.android.messaging.ui.SearchActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.Trace;
import com.android.messaging.util.UiUtils;
import com.juphoon.chatbotmaap.chatbotSearch.RcsChatbotServiceActivity;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.mms.RcsAppNumberHelper;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.helper.mms.ui.RcsGroupNotificationActivity;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ConversationListActivity extends AbstractConversationListActivity implements ActConversationListFragment.ActConversationListFragmentHost {
    private TextView notifyNumber;
    private CardView mToGroupNotify;
    private CardView mChatBotView;

    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private SearchView.SearchAutoComplete mSearchEditText;
    public static final int SEARCH_MAX_LENGTH = 512;


    String[] permissions = new String[]{Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.RECEIVE_MMS};


    // juphoon 监听融合通信登录状态
    private final RcsServiceManager.IServiceManagerCallback mRcsServiceCallBack = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(final boolean logined) {
            mChatBotView.setVisibility(logined ? View.VISIBLE : View.GONE);
        }
    };

    RcsMmsInitHelper.RcsInitCallback mRcsInitCallback = new RcsMmsInitHelper.RcsInitCallback() {

        @Override
        public void onInited(boolean inited) {
            // juphoon 添加是否使用融合通信的监听回调
            RcsServiceManager.addCallBack(mRcsServiceCallBack);
        }
    };


    private final RcsBroadcastHelper.IGroupListener mIGroupListener = new RcsBroadcastHelper.IGroupListener() {

        @Override
        public void onGroupInfoChange(String groupChatId) {
        }

        @Override
        public void onGroupSessChange(String groupChatId, boolean have) {
        }

        @Override
        public void onGroupRecvInvite(String groupChatId) {
            updateGroupNotify(notifyNumber);
        }
    };

    private static boolean sHasNotifyPermission = false;

    private final RcsServiceManager.IServiceManagerCallback mIServiceManagerCallback = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(boolean logined) {
        }

        @Override
        public void onConnectedChange(boolean connected, String name) {
            super.onConnectedChange(connected, name);
            if (TextUtils.equals(RcsServiceManager.RCS, name) && connected) {
                if (RcsCallWrapper.rcsGetUseRcs() && !RcsCallWrapper.rcsHasPermissions() && !sHasNotifyPermission) {
                    // Rcs开关打开 && 没有权限 && 没进行过权限申请 -> 进行权限申请
                    sHasNotifyPermission = true;
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.juphoon.service", "com.juphoon.service.rcs.GrantPermissionActivity"));
                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
                    if (activities.isEmpty()) {
                        return;
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        }

    };
    private static DatabaseWrapper db = DataModel.get().getDatabase();
    private static final String BINDING_ID = "bindingId";
    private static final String WHERE_ARCHIVED =
            "(" +  DatabaseHelper.ConversationColumns.ARCHIVE_STATUS + " = 1)";
    public static final String WHERE_NOT_ARCHIVED =
            "(" +  DatabaseHelper.ConversationColumns.ARCHIVE_STATUS + " = 0)";
    public static final String SORT_ORDER =
            DatabaseHelper.ConversationColumns.PRIORITY + " DESC, " +
                   DatabaseHelper.ConversationColumns.SORT_TIMESTAMP + " DESC ";
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Trace.beginSection("ConversationListActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_list_activity);
        initPermission();
        initChatbotServiceView();
        setToGroupNotify();//juphoon
        Trace.endSection();
//        invalidateActionBar();
      //  RcsMmsInitHelper.setRcsInitCallback(mRcsInitCallback);
        //juphoon
        RcsBroadcastHelper.addGroupListener(mIGroupListener);
        startSetDefault(this,getPackageName());

//        Intent setSmsAppIntent =
//                new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
//        setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
//                getPackageName());
//        startActivityForResult(setSmsAppIntent, 100);

//        ConversationListItemData conversationListItemData = ConversationListItemData.getExistingConversations(db);
//        Log.i("ooo","getName----------:"+conversationListItemData.getName());
//        Log.i("ooo","getConversationId----------:"+     conversationListItemData.getConversationId());



    }
    private final int mRequestCode = 100;//权限请求码
    List<String> mPermissionList = new ArrayList<>();
    private void initPermission() {

        mPermissionList.clear();//清空已经允许的没有通过的权限
        //逐个判断是否还有未通过的权限
        for (int i = 0;i<permissions.length;i++){
            if (ContextCompat.checkSelfPermission(this,permissions[i])!=
                    PackageManager.PERMISSION_GRANTED){
                mPermissionList.add(permissions[i]);//添加还未授予的权限到mPermissionList中
            }
        }
        //申请权限
        if (mPermissionList.size()>0){//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this,permissions,mRequestCode);
        }else {

        }
    }


    public void startSetDefault(Context context,String myPackageName){
        Log.i("zty","myPackageName----------:"+myPackageName);
        String ACTION_CHANGE_DEFAULT = "android.provider.Telephony.ACTION_CHANGE_DEFAULT";
        String EXTRA_PACKAGE_NAME = "package";
        int currentapiVersion=android.os.Build.VERSION.SDK_INT;
        if(currentapiVersion>=android.os.Build.VERSION_CODES.KITKAT){
            Log.i("zty","isDefaultSms----------:"+isDefaultSms(context,myPackageName));
            if(!isDefaultSms(context,myPackageName)){
//                Intent intent = new Intent(ACTION_CHANGE_DEFAULT);
//                intent.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
//                context.startActivity(intent);
                convert_network();
            }
        }
    }
    @SuppressLint("PrivateApi")
    public void convert_network(){

        Class<?> c = null;
        try {
            c = Class.forName("com.android.internal.telephony.SmsApplication");
            if(c == null){
                Log.i("zty","c是空----------:");
                return ;
            }
            Log.i("zty","c----------:"+c);
            Log.i("zty","c不是空----------:");

            Method set = c.getDeclaredMethod("setDefaultApplication", String.class, Context.class);
            set.invoke(c, getPackageName(),  Factory.get().getApplicationContext());
            Log.i("zty","Factory.get().getApplicationContext()"+Factory.get().getApplicationContext());
            Log.i("zty","完成修改----------:");
            String DefaultApplication = Telephony.Sms.getDefaultSmsPackage(this);
            Log.i("zty","DefaultApplication----------:"+DefaultApplication);

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    //数字键1的编辑框的onclick函数
    public void onNum1(View v) {
        onActionBarSettings();
    }
    public static boolean isDefaultSms(Context context,String myPackageName){
        boolean isDefault=false;
        try {
            int currentapiVersion=android.os.Build.VERSION.SDK_INT;
            if(currentapiVersion>=android.os.Build.VERSION_CODES.KITKAT){
                String defaultSmsApplication = Telephony.Sms.getDefaultSmsPackage(context);
                Log.i("zty","defaultSmsApplication---:"+defaultSmsApplication);
                if (defaultSmsApplication != null && defaultSmsApplication.equals(myPackageName)) {
                    isDefault=true;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return isDefault;
    }

    private void initChatbotServiceView() {
        mChatBotView = (CardView) findViewById(R.id.chatbot_view);
        mChatBotView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ConversationListActivity.this, RcsChatbotServiceActivity.class);
                startActivity(intent);
            }
        });
    }

        /**
         *  juphoon
         *  初始化及设置 群通知相关
         */
    private void setToGroupNotify() {
        mToGroupNotify = (CardView) findViewById(R.id.to_group_notify);
        notifyNumber = (TextView)findViewById(R.id.group_notify_unread_number);
        mToGroupNotify.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                startActivity(new Intent(ConversationListActivity.this, RcsGroupNotificationActivity.class));
            }
        });
        updateGroupNotify(notifyNumber);
    }

    private void updateGroupNotify(final TextView notifyNumber) {
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... arg0) {
                Cursor cursor = getContentResolver().query(RmsDefine.RmsGroupNotification.CONTENT_URI, new String[] { RmsDefine.RmsGroupNotification._ID },
                        RmsDefine.RmsGroupNotification.INFO + "=" + RmsDefine.RmsGroupNotification.INFO_INVITED, null, null);
                return cursor == null ? 0 : cursor.getCount() ;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == 0) {
                    mToGroupNotify.setVisibility(View.GONE);
                    notifyNumber.setText(null);
                } else {
                    mToGroupNotify.setVisibility(View.VISIBLE);
                    notifyNumber.setText(String.valueOf(result));
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    @Override
    protected void updateActionBar(final ActionBar actionBar) {
//        actionBar.setTitle(getString(R.string.app_name));
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setDisplayHomeAsUpEnabled(false);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//        actionBar.setBackgroundDrawable(new ColorDrawable(
//                getResources().getColor(R.color.action_bar_background_color)));
//        actionBar.show();
//        super.updateActionBar(actionBar);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Invalidate the menu as items that are based on settings may have changed
        // while not in the app (e.g. Talkback enabled/disable affects new conversation
        // button)
        supportInvalidateOptionsMenu();
		//juphoon
        updateGroupNotify(notifyNumber);
        RcsAppNumberHelper.getUnreadCount();
        if (getSearchEditText() != null && getSearchEditText().getVisibility() == View.VISIBLE){
            getSearchEditText().setFocusable(true);
            getSearchEditText().setFocusableInTouchMode(true);
            getSearchEditText().requestFocus();
        }
        RcsServiceManager.addCallBack(mIServiceManagerCallback);
        //检查是否为登出状态,此处触发登录
        RcsCallWrapper.rcsCheckNeedReLogin();
    }

    @Override
    public void onPause() {
        super.onPause();
        RcsServiceManager.removeCallBack(mIServiceManagerCallback);
    }

    @Override
    public void onBackPressed() {
        BugleApplication.getInstance().setPosition(null);
        if (isInConversationListSelectMode()) {
            exitMultiSelectState();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.conversation_list_fragment_menu, menu);
        final MenuItem item = menu.findItem(R.id.action_debug_options);
        if (item != null) {
            final boolean enableDebugItems = DebugUtils.isDebugEnabled();
            item.setVisible(enableDebugItems).setEnabled(enableDebugItems);
        }
        initSearchView(menu);//juphoon
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch(menuItem.getItemId()) {
		//juphoon
            case R.id.menu_search_item:
                mSearchView.setFocusable(true);
                mSearchView.setFocusableInTouchMode(true);
                mSearchView.requestFocus();
                clearSearchText();
                return true;
            case R.id.action_start_new_conversation:
                onActionBarStartNewConversation();
                return true;
            case R.id.action_settings:
                onActionBarSettings();
                return true;
            case R.id.action_debug_options:
                onActionBarDebug();
                return true;
            case R.id.action_show_archived:
                onActionBarArchived();
                return true;
            case R.id.action_show_blocked_contacts:
                onActionBarBlockedParticipants();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActionBarHome() {
        exitMultiSelectState();
    }

    public void onActionBarStartNewConversation() {
        UIIntents.get().launchCreateNewConversationActivity(this, null);
    }

    public void onActionBarSettings() {
        UIIntents.get().launchSettingsActivity(this);
    }

    public void onActionBarBlockedParticipants() {
        UIIntents.get().launchBlockedParticipantsActivity(this);
    }

    public void onActionBarArchived() {
        UIIntents.get().launchArchivedConversationsActivity(this);
    }

    @Override
    public void onConversationClick(ConversationListData listData, ConversationListItemData conversationListItemData) {
        final String conversationId = conversationListItemData.getConversationId();
        Log.i("ddd","conversationId------:"+conversationId);
        Bundle sceneTransitionAnimationOptions = null;
        boolean hasCustomTransitions = false;
        Log.i("ddd","跳转到发消息界面------");
        UIIntents.get().launchConversationActivity(
                this, conversationId, null,
                sceneTransitionAnimationOptions,
                hasCustomTransitions);

    }

    @Override
    public boolean isSwipeAnimatable() {
        return !isInConversationListSelectMode();
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        final ActConversationListFragment conversationListFragment =
                (ActConversationListFragment) getFragmentManager().findFragmentById(
                        R.id.conversation_list_fragment);
        // When the screen is turned on, the last used activity gets resumed, but it gets
        // window focus only after the lock screen is unlocked.
        if (hasFocus && conversationListFragment != null) {
            conversationListFragment.setScrolledToNewestConversationIfNeeded();
        }
		//juphoon
        if(hasFocus && !isSelectionMode() && getSearchEditText() != null && getSearchEditText().getVisibility() == View.VISIBLE){
            getSearchEditText().setFocusable(true);
            getSearchEditText().setFocusableInTouchMode(true);
            getSearchEditText().requestFocus();
        }

    }
	//juphoon
    @Override
    protected void onDestroy() {
        RcsBroadcastHelper.removeGroupListener(mIGroupListener);
        RcsAppNumberHelper.removeNumberChangeListener();
        if (RcsMmsInitHelper.getIsInit()) {
            RcsServiceManager.removeCallBack(mRcsServiceCallBack);
        }

        super.onDestroy();
    }

    public void initSearchView(Menu menu){
        mSearchItem = menu.findItem(R.id.menu_search_item);
        //add for switch of Search feature
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        // mSearchView.setSearchTextSize(16);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setIconified(false);
        mSearchView.clearFocus();
        int searchSrcTextId = mSearchView.getResources().getIdentifier(
                "android:id/search_src_text", null, null);
//        mSearchEditText = (SearchView.SearchAutoComplete) (mSearchView.findViewById(searchSrcTextId)); 测试时找不到系统资源
//        mSearchEditText.setPadding(0, 10, 0, 10);
//        mSearchEditText.setTextColor(Color.WHITE);
//        mSearchEditText.setHintTextColor(Color.LTGRAY);
//        mSearchEditText.setTextSize(18);
//        mSearchEditText.setGravity(Gravity.CENTER_VERTICAL);
        setCloseBtnGone(true);
        ImageView search_button = (ImageView) mSearchView
                .findViewById(androidx.appcompat.R.id.search_button);
        mSearchView.setSubmitButtonEnabled(false);
        MenuItemCompat.setOnActionExpandListener(mSearchItem,new SearchViewExpandListener());
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchManager != null) {
            System.out.println("searchManager != null");
            SearchableInfo info = searchManager.getSearchableInfo(this
                    .getComponentName());
            mSearchView.setSearchableInfo(info);
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
    public void setCloseBtnGone(boolean bool){
        int closeBtnId = getResources().getIdentifier(
                "android:id/search_close_btn", null, null);
        ImageView mCloseButton = null;
        if (mSearchView!=null) {
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
            intent.setClass(ConversationListActivity.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            System.out.println("onQueryTextChange()");
            if (newText != null && newText.length() > SEARCH_MAX_LENGTH) {
                mSearchView.setQuery(
                        newText.substring(0, SEARCH_MAX_LENGTH - 1), false);
                Toast.makeText(ConversationListActivity.this,
                        getString(R.string.search_max_length),
                        Toast.LENGTH_LONG).show();
            }
            setCloseBtnGone(true);
            return true;
        }
    };

    public void setting(View view){
        onActionBarSettings();
    }

    @Override
    public boolean onSearchRequested() {
        System.out.println("enter onSearchRequested()");
        if (mSearchItem != null) {
            mSearchItem.expandActionView();
            mSearchView.setFocusable(true);
            mSearchView.setFocusableInTouchMode(true);
            mSearchView.requestFocus();
            clearSearchText();
        }
        return true;
    }

    class SearchViewExpandListener implements MenuItemCompat.OnActionExpandListener{
        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            // Do something when collapsed
            hideSoftInput(mSearchView);
            return true;  // Return true to collapse action view
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            // Do something when expanded
            if (!isSelectionMode()) {
                openKeyboard();
            }
            return true;  // Return true to expand action view
        }
    }

    private void openKeyboard() {
        System.out.println("enter openKeyboard()");
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void hideSoftInput(View view) {
        System.out.println("enter hideSoftInput()");
        InputMethodManager inputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
