package com.juphoon.chatbotmaap.chatbotSearch;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotSearchBean;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotDeepLink;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.RcsLocalChatbotActivity;
import com.juphoon.chatbotmaap.tcl.SimpleItemDecoration;
import com.juphoon.chatbotmaap.view.TextViewSnippet;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RmsDefine;
import com.tcl.uicompat.TCLButton;
import com.tcl.uicompat.TCLEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RcsChatbotSearchFragment extends Fragment {
    private String TAG = RcsChatbotSearchFragment.class.getSimpleName();
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

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return super.getLifecycle();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static RcsChatbotSearchFragment newInstance() {
        Bundle args = new Bundle();
        RcsChatbotSearchFragment fragment = new RcsChatbotSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.chatbot_search_fragment, container, false);
        mHost = getActivity();
     //   initLocationView(view);
        initSearchView(view);
        initProgressBar(view);
        initChatBotsListView(view);
        initRefreshView(view);
        refreshLocalChatBotsData();
        Log.i("ooo","mChatBotsListAdapter.getItemCount() ："+mChatBotsListAdapter.getItemCount());
        if(mChatBotsListAdapter.getItemCount() ==0){
            Log.i("ooo","false");
            mChatBotListView.setFocusable(false);
        }else{
            Log.i("ooo","true");
            mChatBotListView.setFocusable(true);
        }
        return view;
    }

    private void initRefreshView(View view) {
//
//        SwipeRefreshLayout refreshView = view.findViewById(R.id.swipe_refresh);
//        refreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//
//            }
//        });
    }
    private TCLButton buttadd2;

    private void initLocationView(View view) {
        mLocationView = view.findViewById(R.id.location_view);
        mLocationView.setPressed(true);
        mLocationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void initProgressBar(View view) {
        mProgressBar = view.findViewById(R.id.loading_progressBar);
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

    private void initSearchView(View view) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        final LinearLayout tags_log = activity.findViewById(R.id.tags_log);

        mSearchView = view.findViewById(R.id.search_view);
        mSearchView.setFocusable(true);
        mSearchView.setFocusableInTouchMode(true);
        mSearchView.requestFocus();

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
                if(charSequence.length()>0){
                    tags_log.setVisibility(View.GONE);
                }else{
                    tags_log.setVisibility(View.VISIBLE);
                }
//                mSearchView.clearFocus();
                searchNet(mSearchInfo);
                showProgressBar();


            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
      //  mSearchView.setIconified(false);
//        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//
//            }
//        });

//        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
//            @Override
//            public boolean onClose() {
//
//
//                return true;
//            }
//        });
//        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                if (TextUtils.isEmpty(query)) {
//                    Toast.makeText(mHost, R.string.chatbot_enter_search_key, Toast.LENGTH_SHORT).show();
//                    return true;
//                }
//                mSearchInfo = new SearchInfo();
//                mSearchInfo.start = 0;
//                mSearchInfo.searchKey = query;
//                mSearchInfo.totalItems = 0;
//                mSearchInfo.cookie = UUID.randomUUID().toString();
//                mSearchView.clearFocus();
//                searchNet(mSearchInfo);
//                showProgressBar();
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                if(TextUtils.isEmpty(newText)){
//                         refreshLocalChatBotsData();
//                }
//                return true;
//            }
//        });
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
                        Toast.makeText(mHost, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mHost, "get token fail", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "refreshLocalChatBotsData");
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
                    mChatBotsListAdapter.setEmptyViewText(getActivity().getString(R.string.chatbot_empty_local));
                    mChatBotsListAdapter.setChatbots(rcsChatbots);
                    mChatBotsListAdapter.setLoadState(RcsChatbotListLoadMoreAdapter.LOADING_COMPLETE);
                    mChatBotsListAdapter.notifyDataSetChanged();
                }
                cancelProgressBar();
            }
        }.execute();

    }

    private void initChatBotsListView(View view) {
        mChatBotListView = view.findViewById(R.id.chatBots_recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mHost);

        mChatBotListView.setLayoutManager(linearLayoutManager);
        mChatBotsListAdapter = new RcsChatbotListLoadMoreAdapter(mHost);

        mChatBotListView.setAdapter(mChatBotsListAdapter);
        mChatBotListView.addItemDecoration(new SimpleItemDecoration());
    }


    @Override
    public void onDestroyView() {
        RcsBroadcastHelper.removeChatbotListener(mChatbotListner);
        super.onDestroyView();

    }
}