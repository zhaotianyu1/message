package com.juphoon.chatbotmaap;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClientOption;
import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotSearchBean;
import com.juphoon.chatbotmaap.Location.LocationService;
import com.juphoon.chatbotmaap.chatbotDetail.RcsChatBotDetailActivity;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RcsChatBotNearbyChatbotActivity extends AppCompatActivity implements RcsBroadcastHelper.IChatbotListener {

    private final static int PER_SEARCH_NUM = 10;

    private LocationService mLocationService;
    private LocationListener mLocationListener;
    private double mLatitude;
    private double mLongitude;
    private String mSearchCookie;
    private String mKey;
    private int mStart;
    private int mTotalItems;

    private TextView mTVLocationAddress;
    private RecyclerView mRecyclerViewNearbyChatbot;
    private ProgressBar mProgressBar;
    private List<RcsChatbotHelper.RcsChatbot> mNearbyChatBots = new ArrayList<>();
    private RcsNearbyChatBotsAdapter mNearbyChatBotsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_activity_nearby_chatbot);
        initView();
        initLocationView();
        RcsBroadcastHelper.addChatbotListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNearbyChatBots.clear();
        mLocationService.unregisterListener(mLocationListener);
        mLocationService.stop();
        RcsBroadcastHelper.removeChatbotListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot_nearby_chatbot);
        }
        mTVLocationAddress = (TextView) findViewById(R.id.location_address);
        mRecyclerViewNearbyChatbot = (RecyclerView) findViewById(R.id.recycler_nearby_chatbot);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mNearbyChatBotsAdapter = new RcsNearbyChatBotsAdapter();
        mRecyclerViewNearbyChatbot.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewNearbyChatbot.setAdapter(mNearbyChatBotsAdapter);
        mRecyclerViewNearbyChatbot.setNestedScrollingEnabled(false);
    }

    private void initLocationView() {
        mLocationService = new LocationService(this);
        LocationClientOption mOption = mLocationService.getDefaultLocationClientOption();
        mOption.setScanSpan(0);// 可选，默认0，即仅定位一次
        mOption.setCoorType("bd09ll");//如果不设置则默认gcj02 从而导致偏差
        mLocationService.setLocationOption(mOption);
        mLocationListener = new LocationListener();
        mLocationService.registerListener(mLocationListener);
        mLocationService.start();
    }

    private class LocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
            String address = location.getAddrStr();
            mTVLocationAddress.setText(address);
            showOrHideListView(false);
            //查询附近chatBot
            startGetNearbyChatBots();
        }
    }

    private void startGetNearbyChatBots() {
        RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
            @Override
            public void run(boolean succ, String resultCode, String token) {
                if (succ) {
                    mSearchCookie = UUID.randomUUID().toString();
                    if (RcsCallWrapper.rcsGetChatbotListByLoction(mSearchCookie, token, PER_SEARCH_NUM, mStart, mKey, mLatitude, mLongitude) > 0) {
                    } else {
                        Toast.makeText(RcsChatBotNearbyChatbotActivity.this, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
                        finishSearch();
                    }
                } else {
                    finishSearch();
                    Toast.makeText(RcsChatBotNearbyChatbotActivity.this, "get token fail", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onChatbotRecommandList(String cookie, boolean result, String json) {

    }

    @Override
    public void onChatbotList(String cookie, boolean result, String json) {
        if (!TextUtils.equals(cookie, mSearchCookie)) {
            return;
        }
        if (result) {
            RcsChatbotSearchBean rcsChatbotSearchBean = new Gson().fromJson(json, RcsChatbotSearchBean.class);
            mStart = rcsChatbotSearchBean.startIndex + rcsChatbotSearchBean.itemsReturned;
            mTotalItems = rcsChatbotSearchBean.totalItems;
            List<RcsChatbotHelper.RcsChatbot> temp = RcsChatbotHelper.parseChatbotListJson(json);
            mNearbyChatBots.addAll(temp);
            mNearbyChatBotsAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(RcsChatBotNearbyChatbotActivity.this, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
        }
        finishSearch();
    }

    @Override
    public void onChatbotInfo(String s, boolean b, String s1) {
    }

    private void finishSearch() {
        showOrHideListView(true);
    }

    private void showOrHideListView(boolean isShow) {
        if (isShow) {
            mProgressBar.setVisibility(View.GONE);
            mRecyclerViewNearbyChatbot.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            mRecyclerViewNearbyChatbot.setVisibility(View.GONE);
        }
    }

    private class RcsNearbyChatBotsAdapter extends RecyclerView.Adapter<RcsNearbyChatBotsAdapter.ViewHolder> {
        private static final int EMPTY_VIEW = -1;
        private static final int NORMAL_VIEW = 0;
        private static final int FOOT_VIEW = 1;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public ImageView icon;
            public LinearLayout footView;
            public TextView emptyText;

            public ViewHolder(View v, int viewType) {
                super(v);
                if (viewType == NORMAL_VIEW) {
                    name = (TextView) v.findViewById(R.id.name);
                    icon = (ImageView) v.findViewById(R.id.icon);
                } else if (viewType == FOOT_VIEW) {
                    footView = (LinearLayout) v;
                } else if (viewType == EMPTY_VIEW) {
                    emptyText = (TextView) v.findViewById(R.id.tv_empty);
                }
            }
        }

        @Override
        public RcsNearbyChatBotsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == NORMAL_VIEW) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_nearby_item, parent, false);
                return new ViewHolder(view, viewType);
            } else if (viewType == FOOT_VIEW) {
                View footView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_business_chat_bot_footview, parent, false);
                footView.setVisibility(View.GONE);
                footView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startGetNearbyChatBots();
                    }
                });
                return new ViewHolder(footView, viewType);
            } else {
                View emptyView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_empty_view, parent, false);
                return new ViewHolder(emptyView, viewType);
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            if (getItemViewType(position) == NORMAL_VIEW) {
                final RcsChatbotHelper.RcsChatbot chatbot = mNearbyChatBots.get(position);
                holder.name.setText(chatbot.name);
                RcsChatbotUtils.getDefaultIcon(holder.icon, chatbot.icon);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (RcsChatbotUtils.checkChatBotIsInConversation(RcsChatBotNearbyChatbotActivity.this, chatbot.serviceId) || RcsChatbotUtils.checkChatBotIsSaveLocal(chatbot.serviceId)) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + RcsChatbotHelper.formatServiceIdWithNoSip(chatbot.serviceId)));
                            startActivity(intent);
                        } else {
                            startChatbotDetailActivity(chatbot.serviceId);
                        }
                    }
                });
            } else if (getItemViewType(position) == FOOT_VIEW) {
                if (mTotalItems > mNearbyChatBots.size()) {
                    holder.footView.setVisibility(View.VISIBLE);
                } else {
                    holder.footView.setVisibility(View.GONE);
                }
            } else if (getItemViewType(position) == EMPTY_VIEW) {
                holder.emptyText.setText(R.string.chatbot_no_search_result);
            }
        }

        @Override
        public int getItemCount() {
            return mNearbyChatBots.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (mNearbyChatBots.size() == 0) {
                return EMPTY_VIEW;
            }
            if (position == getItemCount() - 1) {
                return FOOT_VIEW;
            }
            return NORMAL_VIEW;
        }
    }

    private void startChatbotDetailActivity(String serviceId) {
        Intent intent = new Intent(this, RcsChatBotDetailActivity.class);
        intent.putExtra(RcsChatBotDetailActivity.KEY_CHATBOT_NUMBER, serviceId);
        this.startActivity(intent);
        finish();
    }
}
