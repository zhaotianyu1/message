package com.juphoon.chatbotmaap;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotSearchBean;
import com.juphoon.chatbotmaap.chatbotDetail.RcsChatBotDetailActivity;
import com.juphoon.chatbotmaap.view.TextViewSnippet;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RcsChatbotSearchActivity extends AppCompatActivity implements RcsBroadcastHelper.IChatbotListener, SearchView.OnQueryTextListener {

    private static int PER_SEARCH_NUM = 3;

    private SearchView mSearchView;
    private RecyclerView mRecyclerViewRecommend;
    private RecyclerView mRecyclerViewLocal;
    private RecyclerView mRecyclerViewNet;
    private TextView mTextViewLocal;
    private TextView mTextViewNet;
    private TextView mTextViewRecommend;
    private ProgressBar mProgressBar;
    private RcsLocalChatbotAdapter mLocalChatbotAdapter;
    private RcsNetChatbotAdapter mNetChatBotAdapter;
    private RcsRecommandChatbotAdapter mRecommendChatBotAdapter;
    private List<RcsChatbotHelper.RcsChatbot> mNetChatBots = new ArrayList<>();
    private List<RcsChatbotHelper.RcsChatbot> mLocalChatbots = new ArrayList<>();
    private List<RcsChatbotHelper.RcsChatbot> mRecommendChatBots = new ArrayList<>();
    private String mKey;
    private int mStart;
    private int mTotalItems;
    private String mSearchCookie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_activity_search);

        initView();
        initAdapter();

        RcsBroadcastHelper.addChatbotListener(this);
    }


    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("搜索");
        }

        mSearchView = (SearchView) findViewById(R.id.search_view);
        mSearchView.clearFocus();
        mSearchView.setOnQueryTextListener(this);
        mRecyclerViewNet = (RecyclerView) findViewById(R.id.recycler_net);
        mRecyclerViewLocal = (RecyclerView) findViewById(R.id.recycler_local);
        mRecyclerViewRecommend = (RecyclerView) findViewById(R.id.recycler_recommend);
        mTextViewLocal = (TextView) findViewById(R.id.text_local_title);
        mTextViewNet = (TextView) findViewById(R.id.text_net_title);
        mTextViewRecommend = (TextView) findViewById(R.id.text_recommend_title);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mProgressBar.setVisibility(View.GONE);
    }

    private void initAdapter() {
        mNetChatBotAdapter = new RcsNetChatbotAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerViewNet.setLayoutManager(layoutManager);
        mRecyclerViewNet.setAdapter(mNetChatBotAdapter);
        mRecyclerViewNet.setNestedScrollingEnabled(false);
        mLocalChatbotAdapter = new RcsLocalChatbotAdapter();
        LinearLayoutManager localLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerViewLocal.setLayoutManager(localLinearLayoutManager);
        mRecyclerViewLocal.setAdapter(mLocalChatbotAdapter);
        mRecyclerViewLocal.setNestedScrollingEnabled(false);
        LinearLayoutManager recommendLinearLayoutManager = new LinearLayoutManager(this);
        mRecommendChatBotAdapter = new RcsRecommandChatbotAdapter();
        mRecyclerViewRecommend.setLayoutManager(recommendLinearLayoutManager);
        mRecyclerViewRecommend.setAdapter(mRecommendChatBotAdapter);
        mRecyclerViewRecommend.setNestedScrollingEnabled(false);
    }

    /**
     * 获取高亮字符串
     *
     * @param chatbotName
     * @return
     */
    private String getHighLightedString(String chatbotName) {
        String highLightedString = "";
        String key = mKey.toLowerCase();
        RcsChatbotSearchKeywordBean keywordBean = new RcsChatbotSearchKeywordBean(chatbotName, PinyinHelper.PinYin4j.getPinyin(chatbotName, true), PinyinHelper.PinYin4j.getPinyinList(chatbotName));
        //匹配精确的名称，比如唯品会：唯品会。输入唯品，高亮 唯品
        if (keywordBean.getName().contains(key)) {
            highLightedString = keywordBean.getName().substring(keywordBean.getName().indexOf(key), keywordBean.getName().indexOf(key) + key.length());
            return highLightedString;
        }
        //匹配首字母,比如唯品会，[wph,wpk]。输入ph，高亮 品会
        for (String py : keywordBean.getNamePYSet()) {
            if (py.contains(key)) {
                highLightedString = keywordBean.getName().substring(py.indexOf(key), py.indexOf(key) + key.length());
                return highLightedString;
            }
        }
        //匹配拼音，比如唯品会：[weipinhui,weipinkuai]。输入weipinh，高亮 唯品会
        //这样的话就要从每个字的拼音开始匹配起,[wei],[pin],[hui,kuai]
        for (int j = 0; j < keywordBean.getNamePinyinList().size(); j++) {
            Set<String> setMatch = PinyinHelper.PinYin4j.getPinyin(keywordBean.getName().substring(j), false);
            for (String string : setMatch) {
                if (string.toLowerCase().startsWith(key)) {
                    //匹配成功
                    int length = 0;
                    //比如输入是weip，或者weipi,或者weipin,这些都可以匹配上，从而就可以通过weipin>=weip,weipi,weipin判断
                    for (int k = j; k < keywordBean.getNamePinyinList().size(); k++) {
                        for (String s : keywordBean.getNamePinyinList().get(k)) {
                            length = length + s.length();
                            if (length >= key.length()) {
                                highLightedString = keywordBean.getName().substring(j, k + 1);
                                return highLightedString;
                            }
                        }
                    }
                }
            }
        }
        if (highLightedString.isEmpty()) {
            // 如果传""进高亮控件会发生错误，这里 && 作为保留字符串，不会在名字中出现，所以没影响
            return "&&";
        } else {
            return highLightedString;
        }
    }

    public class RcsNetChatbotAdapter extends RecyclerView.Adapter<RcsNetChatbotAdapter.ViewHolder> {
        private static final int NORMAL_VIEW = 0;
        private static final int FOOT_VIEW = 1;
        private static final int NO_RESULT_VIEW = 2;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextViewSnippet name;
            public ImageView icon;
            public LinearLayout footView;
            public TextView id;

            public ViewHolder(View v, int viewType) {
                super(v);
                if (viewType == NORMAL_VIEW) {
                    name = (TextViewSnippet) v.findViewById(R.id.name);
                    icon = (ImageView) v.findViewById(R.id.icon);
                    id = (TextView) v.findViewById(R.id.id);
                } else if (viewType == FOOT_VIEW) {
                    footView = (LinearLayout) v;
                }

            }
        }

        @Override
        public RcsNetChatbotAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == NORMAL_VIEW) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_net_chatbot, parent, false);
                return new ViewHolder(view, viewType);
            } else if (viewType == FOOT_VIEW) {
                View footView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_business_chat_bot_footview, parent, false);
                footView.setVisibility(View.GONE);
                footView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        searchNet();
                    }
                });
                return new ViewHolder(footView, viewType);
            } else if (viewType == NO_RESULT_VIEW) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_footview, parent, false);
                return new ViewHolder(view, viewType);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            if (getItemViewType(position) == NO_RESULT_VIEW) {
                return;
            }
            if (getItemViewType(position) == NORMAL_VIEW) {
                final RcsChatbotHelper.RcsChatbot chatbot = mNetChatBots.get(position);
                holder.name.setText(chatbot.name, mKey);
                holder.id.setText(RcsChatbotHelper.parseChatbotServierIdToNumber(chatbot.serviceId));
                RcsChatbotUtils.getDefaultIcon(holder.icon, chatbot.icon);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        jump2ChatbotConversationORDetailActivity(chatbot.serviceId);
                    }
                });
            } else {
                if (mTotalItems > mNetChatBots.size()) {
                    holder.footView.setVisibility(View.VISIBLE);
                } else {
                    holder.footView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mNetChatBots.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (mNetChatBots.size() == 0) {
                return NO_RESULT_VIEW;
            }
            if (position == getItemCount() - 1) {
                return FOOT_VIEW;
            }
            return NORMAL_VIEW;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNetChatBots.clear();
        mLocalChatbots.clear();
        mRecommendChatBots.clear();
        RcsBroadcastHelper.removeChatbotListener(this);
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
            //处理推荐列表
            List<RcsChatbotHelper.RcsChatbot> recommendBots = RcsChatbotHelper.parseChatbotRecommendListJson(json);
            if (recommendBots == null || recommendBots.size() == 0) {
                if (mRecommendChatBots.size() == 0) {
                    mTextViewRecommend.setVisibility(View.GONE);
                }
            } else {
                mRecommendChatBots.addAll(recommendBots);
                mTextViewRecommend.setVisibility(View.VISIBLE);
                mRecyclerViewRecommend.setVisibility(View.VISIBLE);
                mRecommendChatBotAdapter.notifyDataSetChanged();
            }
            //处理搜索结果
            RcsChatbotSearchBean rcsChatbotSearchBean = new Gson().fromJson(json, RcsChatbotSearchBean.class);
            mStart = rcsChatbotSearchBean.startIndex + rcsChatbotSearchBean.itemsReturned;
            mTotalItems = rcsChatbotSearchBean.totalItems;
            List<RcsChatbotHelper.RcsChatbot> temp = RcsChatbotHelper.parseChatbotListJson(json);
            if (temp != null) {
                mNetChatBots.addAll(temp);
                mTextViewNet.setVisibility(View.VISIBLE);
                mRecyclerViewNet.setVisibility(View.VISIBLE);
            }
            mNetChatBotAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(RcsChatbotSearchActivity.this, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
        }
        finishSearch();
    }

    @Override
    public void onChatbotInfo(String cookie, boolean b, String s) {

    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        if (TextUtils.isEmpty(s)) {
            Toast.makeText(this, R.string.chatbot_enter_search_key, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, R.string.chatbot_searching, Toast.LENGTH_SHORT).show();
            return true;
        }
        mKey = s;
        mStart = 0;
        mTotalItems = 0;
        mSearchView.clearFocus();

        mLocalChatbots.clear();
        mLocalChatbotAdapter.notifyDataSetChanged();

        mNetChatBots.clear();
        mNetChatBotAdapter.notifyDataSetChanged();

        mRecommendChatBots.clear();
        mRecommendChatBotAdapter.notifyDataSetChanged();
        searchLocal();
        searchNet();

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
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

    private void searchNet() {
        if (!RcsServiceManager.isLogined()) {
            Toast.makeText(this, R.string.chatbot_not_login, Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        mTextViewNet.setVisibility(View.GONE);
        RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
            @Override
            public void run(boolean succ, String resultCode,String token) {
                if (succ) {
                    mSearchCookie = UUID.randomUUID().toString();
                    if (RcsCallWrapper.rcsGetChatbotList(mSearchCookie, token, PER_SEARCH_NUM, mStart, mKey) > 0) {

                    } else {
                        Toast.makeText(RcsChatbotSearchActivity.this, R.string.chatbot_search_fail, Toast.LENGTH_SHORT).show();
                        finishSearch();
                    }
                } else {
                    finishSearch();
                    Toast.makeText(RcsChatbotSearchActivity.this, "get token fail", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void finishSearch() {
        mProgressBar.setVisibility(View.GONE);
    }

    private void searchLocal() {
        mTextViewLocal.setVisibility(View.GONE);
        mRecyclerViewLocal.setVisibility(View.GONE);
        new AsyncTask<Void, Void, List<RcsChatbotHelper.RcsChatbot>>() {
            @Override
            protected List<RcsChatbotHelper.RcsChatbot> doInBackground(Void... voids) {
                List<RcsChatbotHelper.RcsChatbot> result = new ArrayList<>();
                Cursor cursor = getApplicationContext().getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, null,
                        RmsDefine.ChatbotInfo.SEARCHKEYWORD + " like ?  and " + RmsDefine.ChatbotInfo.SAVELOCAL + " = 1 ", new String[]{"%" + mKey.replace("?", "[?]").replace("_", "[_]") + "%"}, null);
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
                super.onPostExecute(rcsChatbots);
                mLocalChatbots = rcsChatbots;
                mLocalChatbotAdapter.notifyDataSetChanged();
                if (mLocalChatbots.size() > 0) {
                    mTextViewLocal.setVisibility(View.VISIBLE);
                    mRecyclerViewLocal.setVisibility(View.VISIBLE);
                }
            }
        }.execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public class RcsLocalChatbotAdapter extends RecyclerView.Adapter<RcsLocalChatbotAdapter.ViewHolder> {
        public final int MaxDisplayCount = 4;
        public final int FLOOR_TYPE = 1;
        public final int NORMAL_TYPE = 0;
        public final int NO_RESULT_TYPE = 2;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == NORMAL_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_item, parent, false);
                return new ViewHolder(view);
            } else if (viewType == FLOOR_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_look_more_item, parent, false);
                return new ViewHolder(view);
            } else if (viewType == NO_RESULT_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_footview, parent, false);
                return new ViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) == NO_RESULT_TYPE) {
                return;
            }
            if (getItemViewType(position) == FLOOR_TYPE) {
                holder.more.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(RcsChatbotSearchActivity.this, RcsChatbotMoreBotsActivity.class);
                        intent.putExtra(RcsChatbotMoreBotsActivity.REQUEST_CHATBOTS_TYPE, RcsChatbotMoreBotsActivity.REQUEST_ALL_LOCAL_CHATBOT);
                        startActivity(intent);
                    }
                });
                return;
            }
            final RcsChatbotHelper.RcsChatbot chatbot = mLocalChatbots.get(position);
            holder.name.setText(chatbot.name, getHighLightedString(chatbot.name));
            holder.id.setText(RcsChatbotHelper.parseChatbotServierIdToNumber(chatbot.serviceId));
            RcsChatbotUtils.getDefaultIcon(holder.icon, chatbot.icon);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    jump2ChatbotConversationORDetailActivity(chatbot.serviceId);

                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            if (mLocalChatbots.size() == 0) {
                return NO_RESULT_TYPE;
            }
            if (position == MaxDisplayCount - 1) {
                return FLOOR_TYPE;
            }
            return NORMAL_TYPE;
        }

        @Override
        public int getItemCount() {
            if (mLocalChatbots.size() == 0) {
                return 1;
            }
            if (mLocalChatbots.size() >= MaxDisplayCount) {
                return MaxDisplayCount;
            }
            return mLocalChatbots.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextViewSnippet name;
            ImageView icon;
            TextView id;
            TextView more;

            public ViewHolder(View v) {
                super(v);
                name = (TextViewSnippet) v.findViewById(R.id.name);
                icon = (ImageView) v.findViewById(R.id.icon);
                id = (TextView) v.findViewById(R.id.id);
                more = (TextView) v.findViewById(R.id.id_lookMore);
            }
        }
    }

    public class RcsRecommandChatbotAdapter extends RecyclerView.Adapter<RcsRecommandChatbotAdapter.ViewHolder> {
        private final int NO_RESULT_TYPE = -1;
        private final int NORMAL_TYPE = 0;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == NORMAL_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_item, parent, false);
                return new ViewHolder(view);
            } else if (viewType == NO_RESULT_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_footview, parent, false);
                return new ViewHolder(view);
            }
            return null;
        }

        @Override
        public int getItemViewType(int position) {
            if (mRecommendChatBots.size() == 0) {
                return NO_RESULT_TYPE;
            } else {
                return NORMAL_TYPE;
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) == NO_RESULT_TYPE) {
                return;
            }
            final RcsChatbotHelper.RcsChatbot chatbot = mRecommendChatBots.get(position);
            holder.name.setText(chatbot.name, getHighLightedString(chatbot.name));
            holder.id.setVisibility(View.GONE);
            RcsChatbotUtils.getDefaultIcon(holder.icon, chatbot.icon);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + RcsChatbotHelper.formatServiceIdWithNoSip(chatbot.serviceId)));
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mRecommendChatBots.size() == 0) {
                return 1;
            }
            return mRecommendChatBots.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextViewSnippet name;
            ImageView icon;
            TextView id;
            TextView description;

            public ViewHolder(View v) {
                super(v);
                name = (TextViewSnippet) v.findViewById(R.id.name);
                icon = (ImageView) v.findViewById(R.id.icon);
                id = (TextView) v.findViewById(R.id.id);
            }
        }
    }

    private void jump2ChatbotConversationORDetailActivity(String serviceId) {
        if (RcsChatbotUtils.checkChatBotIsInConversation(RcsChatbotSearchActivity.this, serviceId) || RcsChatbotUtils.checkChatBotIsSaveLocal(serviceId)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + RcsChatbotHelper.formatServiceIdWithNoSip(serviceId)));
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, RcsChatBotDetailActivity.class);
            intent.putExtra(RcsChatBotDetailActivity.KEY_CHATBOT_NUMBER, serviceId);
            this.startActivity(intent);
        }
    }
}
