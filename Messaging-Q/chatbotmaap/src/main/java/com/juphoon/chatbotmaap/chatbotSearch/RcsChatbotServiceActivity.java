package com.juphoon.chatbotmaap.chatbotSearch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;
import com.juphoon.chatbot.RcsChatbotSearchBean;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsBlackChatbotActivity;
import com.juphoon.chatbotmaap.chatbotMessages.RcsChatBotConversationListFragment;
import com.juphoon.chatbotmaap.tcl.SimpleItemDecoration;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RmsDefine;
import com.tcl.uicompat.TCLButton;
import com.tcl.uicompat.TCLEditText;
import com.tcl.uicompat.TCLNavigationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class RcsChatbotServiceActivity extends AppCompatActivity  implements ActionMode.Callback {


    private ViewPager2 mViewPager;
    List<Fragment> mFragments;

  //  private TCLNavigationItem chatbot_messages_list;
    private TextView chatbot_service_list;

    private TCLButton buttadd2;
    private TextView returnsss;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       setContentView(R.layout.activtiy_rcs_chatbot_service);
        mHost =this;
       // initViewData();
       // initView();
        initActionBar();
        initdate();

//        Runnable run2 = new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    View rootview = RcsChatbotServiceActivity.this.getWindow().getDecorView();
//                    View aaa = rootview.findFocus();
//                    Log.i("xxx", aaa.getId()+"");
//                    Log.i("xxx","id = 0x"+Integer.toHexString(aaa.getId()));
//                    Log.i("xxx", aaa.toString());
//                }
//            }
//        };
//        new Thread(run2).start();

    }

    private void initdate( ) {
        initSearchView();
        initProgressBar();
        initChatBotsListView();
     //   refreshLocalChatBotsData();
        Log.i("ooo","mChatBotsListAdapter.getItemCount() ："+mChatBotsListAdapter.getItemCount());
        if(mChatBotsListAdapter.getItemCount() ==0){
            Log.i("ooo","false");
            mChatBotListView.setFocusable(false);
        }else{
            Log.i("ooo","true");
            mChatBotListView.setFocusable(true);
        }


    }
    private TCLEditText mSearchView;
    private SearchInfo mSearchInfo;
    private RecyclerView mChatBotListView;
    private RcsChatbotListLoadMoreAdapter mChatBotsListAdapter;
    private Activity mHost;
    private ProgressBar mProgressBar;
    private ImageView mLocationView;
    private RcsBroadcastHelper.IChatbotListener mChatbotListner;
    private class SearchInfo {
        String searchKey;
        int start;
        int totalItems;
        String cookie = "";
        int perSearchNumber = 10;
    }
    private void initSearchView() {
        tags_log = findViewById(R.id.tags_log);
        mSearchView = findViewById(R.id.search_view);
        mSearchView.setFocusable(true);
        mSearchView.setFocusableInTouchMode(true);
        mSearchView.requestFocus();
//
//        returnsss = findViewById(R.id.returnsss);
//        returnsss.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onBackPressed();
//            }
//        });
        mSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        mSearchInfo = new SearchInfo();
                        mSearchInfo.start = 0;
                        mSearchInfo.searchKey = mSearchView.getText().toString();
                        mSearchInfo.totalItems = 0;
                        mSearchInfo.cookie = UUID.randomUUID().toString();
                        mSearchView.clearFocus();
                        searchNet(mSearchInfo);
                        showProgressBar();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        //搜索界面的动态查询
        mSearchView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                mSearchInfo = new SearchInfo();
                mSearchInfo.start = 0;
                mSearchInfo.searchKey = mSearchView.getText().toString();
                mSearchInfo.totalItems = 0;
                mSearchInfo.cookie = UUID.randomUUID().toString();
//                if(charSequence.length()>0){
//                    tags_log.setVisibility(View.GONE);
//                }else{
//                    tags_log.setVisibility(View.VISIBLE);
//                }
//                mSearchView.clearFocus();
                searchNet(mSearchInfo);
                showProgressBar();


            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mSearchInfo = new SearchInfo();
        mSearchInfo.start = 0;
        mSearchInfo.searchKey = "1";
        mSearchInfo.totalItems = 0;
        mSearchInfo.cookie = UUID.randomUUID().toString();
//                if(charSequence.length()>0){
//                    tags_log.setVisibility(View.GONE);
//                }else{
//                    tags_log.setVisibility(View.VISIBLE);
//                }
//                mSearchView.clearFocus();
        searchNet(mSearchInfo);

        mChatbotListner = new RcsBroadcastHelper.IChatbotListener() {
            @Override
            public void onChatbotRecommandList(String s, boolean b, String s1) {

            }

            @Override
            public void onChatbotList(String cookie, boolean result, String json) {
                cancelProgressBar();
                if (mSearchInfo == null || !TextUtils.equals(cookie, mSearchInfo.cookie)) {
                    return;
                }
                if (result) {
                    //处理推荐列表
                    List<RcsChatbotHelper.RcsChatbot> recommendBots = RcsChatbotHelper.parseChatbotRecommendListJson(json);
                    if (recommendBots == null || recommendBots.size() == 0) {

                    } else {

                    }
                    //处理搜索结果
                    RcsChatbotSearchBean rcsChatbotSearchBean = new Gson().fromJson(json, RcsChatbotSearchBean.class);
                    mSearchInfo.start = rcsChatbotSearchBean.startIndex + rcsChatbotSearchBean.itemsReturned;
                    mSearchInfo.totalItems = rcsChatbotSearchBean.totalItems;
                    List<RcsChatbotHelper.RcsChatbot> temp = RcsChatbotHelper.parseChatbotListJson(json);
                    if (mChatBotsListAdapter != null) {
                        mChatBotsListAdapter.setEmptyViewText(getString(R.string.chatbot_empty_net));
                        mChatBotsListAdapter.setChatbots(temp);
                        mChatBotsListAdapter.notifyDataSetChanged();
                        if (mSearchInfo.start >= mSearchInfo.totalItems) {
                            mChatBotsListAdapter.setLoadState(RcsChatbotListLoadMoreAdapter.LOADING_END);
                        }
                    }
                } else {
                    Toast.makeText(mHost, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChatbotInfo(String s, boolean b, String s1) {

            }
        };
        RcsBroadcastHelper.addChatbotListener(mChatbotListner);
    }


    private void initViewData() {
        mFragments = new ArrayList<>();
       // mFragments.add(new RcsChatBotConversationListFragment());
        mFragments.add(new RcsChatbotSearchFragment());
    }
    private void initProgressBar( ) {
        mProgressBar = findViewById(R.id.loading_progressBar);
    }

    private void showProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void cancelProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private void searchNet(final SearchInfo searchInfo) {
        //todo Dialog
        RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
            @Override
            public void run(boolean succ, String resultCode, String token) {
                boolean requestResult = succ;
                if (succ) {
                    if (RcsCallWrapper.rcsGetChatbotList(searchInfo.cookie, token, searchInfo.perSearchNumber, searchInfo.start, searchInfo.searchKey) > 0) {
                        requestResult = true;
                    } else {
                       // Toast.makeText(mHost, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
                    }
                } else {
                   // Toast.makeText(mHost, "get token fail", Toast.LENGTH_SHORT).show();
                }
                if (!requestResult) {
                    cancelProgressBar();
                }
            }
        });
    }


    private void clearChatbotLists() {
        if (mChatBotsListAdapter != null) {
            mChatBotsListAdapter.setChatbots(null);
            mChatBotsListAdapter.notifyDataSetChanged();
        }
    }

    private void refreshLocalChatBotsData() {
        showProgressBar();

        //刷新本地chatbot列表
        new AsyncTask<Void, Void, List<RcsChatbotHelper.RcsChatbot>>() {
            @Override
            protected List<RcsChatbotHelper.RcsChatbot> doInBackground(Void... voids) {
                List<RcsChatbotHelper.RcsChatbot> result = new ArrayList<>();
                Cursor cursor = mHost.getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, null, RmsDefine.ChatbotInfo.SAVELOCAL + "=1", null, null);
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            RcsChatbotHelper.RcsChatbot info = new RcsChatbotHelper.RcsChatbot();
                            info.name = cursor.getString(cursor.getColumnIndex(RmsDefine.ChatbotInfo.NAME));
                            info.icon = cursor.getString(cursor.getColumnIndex(RmsDefine.ChatbotInfo.ICON));
                            info.serviceId = cursor.getString(cursor.getColumnIndex(RmsDefine.ChatbotInfo.SERVICEID));
                            result.add(info);
                        }
                    } finally {
                        cursor.close();
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(List<RcsChatbotHelper.RcsChatbot> rcsChatbots) {
                if (mChatBotsListAdapter != null) {
                    mChatBotsListAdapter.setEmptyViewText(getString(R.string.chatbot_empty_local));
                    mChatBotsListAdapter.setChatbots(rcsChatbots);
                    mChatBotsListAdapter.setLoadState(RcsChatbotListLoadMoreAdapter.LOADING_COMPLETE);
                    mChatBotsListAdapter.notifyDataSetChanged();
                }
                cancelProgressBar();
            }
        }.execute();

    }

    private void initChatBotsListView( ) {
        mChatBotListView = findViewById(R.id.chatBots_recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mHost);

        mChatBotListView.setLayoutManager(linearLayoutManager);
        mChatBotsListAdapter = new RcsChatbotListLoadMoreAdapter(mHost);

        mChatBotListView.setAdapter(mChatBotsListAdapter);
        mChatBotListView.addItemDecoration(new SimpleItemDecoration());
    }




    private void initView() {
       // initActionBar();
       // initViewPager();
       // initButton();
       // initTabLayout();
    }
    private LinearLayout tags_log;
    private void initButton() {
        chatbot_service_list = findViewById(R.id.chatbot_service_list);
        chatbot_service_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatbot_service_list.requestFocus();

                mViewPager.setCurrentItem(1);

            }
        });

    }


    private void initViewPager() {
//        mViewPager = findViewById(R.id.view_pager);
//        mViewPager.setFocusable(true);
//        mViewPager.setFocusableInTouchMode(true);
//        mViewPager.setAdapter(new FragmentStateAdapter(this) {
//            @NonNull
//            @Override
//            public Fragment createFragment(int position) {
//                return mFragments.get(position);
//            }
//
//            @Override
//            public int getItemCount() {
//                return mFragments.size();
//            }
//        });
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rcs_chatbot_black_manage, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
            return true;
        } else if (i == R.id.menu_black_manage) {
            Intent intent = new Intent(RcsChatbotServiceActivity.this, RcsBlackChatbotActivity.class);
            startActivity(intent);
            return true;
        } else if (i == R.id.menu_scan) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1000);
            } else {
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.setAction(Intents.Scan.ACTION);
                startActivityForResult(intent, 1);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { //RESULT_OK = -1
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Intents.Scan.RESULT);
            RcsChatbotScanHelper rcsChatbotScanHelper = new RcsChatbotScanHelper();
            rcsChatbotScanHelper.dealScanResult(RcsChatbotServiceActivity.this, scanResult);
        }
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        View v = LayoutInflater.from(this).inflate(
                R.layout.chatbot_conversation_list_fragment, null);
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
