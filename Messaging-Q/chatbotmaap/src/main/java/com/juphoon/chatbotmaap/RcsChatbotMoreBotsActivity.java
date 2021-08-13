package com.juphoon.chatbotmaap;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.juphoon.chatbotmaap.view.TextViewSnippet;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.service.RmsDefine;

import java.util.ArrayList;
import java.util.List;

public class RcsChatbotMoreBotsActivity extends AppCompatActivity {

    public static final String REQUEST_CHATBOTS_TYPE = "request_chatbot_type";
    public static final long REQUEST_ALL_LOCAL_CHATBOT = 0;
    public static final long REQUEST_NET_CHATBOT = 1;

    private List<RcsChatbotHelper.RcsChatbot> mChatBots = new ArrayList<>();
    private TextView mTitle;
    private RecyclerView mRecyclerChatbots;
    private RcsChatbotsAdapter mRcsChatbotAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_chatbot_more_bots);
        initView();
        initData();
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

    private void initData() {
        long requestType = getIntent().getLongExtra(REQUEST_CHATBOTS_TYPE, -1);
        if (requestType == REQUEST_ALL_LOCAL_CHATBOT) {
            new AsyncTask<Void, Void, List<RcsChatbotHelper.RcsChatbot>>() {
                @Override
                protected List<RcsChatbotHelper.RcsChatbot> doInBackground(Void... voids) {
                    List<RcsChatbotHelper.RcsChatbot> result = new ArrayList<>();
                    Cursor cursor = getApplicationContext().getContentResolver().query(RmsDefine.ChatbotInfo.CONTENT_URI, null, RmsDefine.ChatbotInfo.SAVELOCAL + "=1", null, null);
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
                    mChatBots = rcsChatbots;
                    mRcsChatbotAdapter.notifyDataSetChanged();
                }
            }.execute();
        }
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot_more);
        }
        mTitle = (TextView) findViewById(R.id.title);
        mRecyclerChatbots = (RecyclerView) findViewById(R.id.recycler_chatbots);
        mRcsChatbotAdapter = new RcsChatbotsAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerChatbots.setLayoutManager(linearLayoutManager);
        mRecyclerChatbots.setAdapter(mRcsChatbotAdapter);
    }


    public class RcsChatbotsAdapter extends RecyclerView.Adapter<RcsChatbotsAdapter.ViewHolder> {
        public final int FLOOR_TYPE = 1;
        public final int NORMAL_TYPE = 0;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == NORMAL_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_item, parent, false);
                return new ViewHolder(view);
            } else if (viewType == FLOOR_TYPE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_look_more_item, parent, false);
                return new ViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final RcsChatbotHelper.RcsChatbot chatbot = mChatBots.get(position);
            holder.name.setText(chatbot.name);
            holder.id.setText(RcsChatbotHelper.parseChatbotServierIdToNumber(chatbot.serviceId));
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
        public int getItemViewType(int position) {

            return NORMAL_TYPE;
        }

        @Override
        public int getItemCount() {
            return mChatBots.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
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
}
