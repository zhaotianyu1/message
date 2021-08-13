package com.juphoon.chatbotmaap;

import android.content.Intent;
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

import com.juphoon.chatbotmaap.chatbotDetail.RcsChatBotDetailActivity;
import com.juphoon.helper.RcsChatbotHelper;

import java.util.ArrayList;
import java.util.List;

public class RcsBlackChatbotActivity extends AppCompatActivity {
    public static final int REQUEST_CHATBOT_INFO = 700;

    private RecyclerView mBlackChatbotRv;
    private TextView mEmptyTextv;
    private BlackChatbotAdapter mBlackChatbotAdapter;
    private ArrayList<RcsChatbotHelper.RcsChatbot> mBlackChatbotList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_black_chatbot);

        initView();
        searchBlackChatbotList();//搜索黑名单中的 chatbot
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.chatbot_black_manage));

        mBlackChatbotRv = (RecyclerView) findViewById(R.id.rv_black_chatbot);

        LinearLayoutManager blackLinearLayoutManager = new LinearLayoutManager(this);
        mBlackChatbotRv.setLayoutManager(blackLinearLayoutManager);
        mBlackChatbotAdapter = new BlackChatbotAdapter(mBlackChatbotList);
        mBlackChatbotRv.setAdapter(mBlackChatbotAdapter);
        mEmptyTextv = (TextView) findViewById(R.id.empty_text);
    }

    private void searchBlackChatbotList() {
        new AsyncTask<Void, Void, List<RcsChatbotHelper.RcsChatbot>>() {
            @Override
            protected List<RcsChatbotHelper.RcsChatbot> doInBackground(Void... voids) {
                return RcsChatbotHelper.getLocalBlockChatbots();
            }

            @Override
            protected void onPostExecute(List<RcsChatbotHelper.RcsChatbot> rcsChatbots) {
                super.onPostExecute(rcsChatbots);
                if (rcsChatbots == null) {
                    return;
                }
                mBlackChatbotList.clear();
                mBlackChatbotList.addAll(rcsChatbots);
                if (rcsChatbots.size() == 0) {
                    mEmptyTextv.setVisibility(View.VISIBLE);
                } else {
                    mEmptyTextv.setVisibility(View.GONE);
                }
                mBlackChatbotAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class BlackChatbotAdapter extends RecyclerView.Adapter<RcsBlackChatbotActivity.BlackChatbotAdapter.ViewHolder> {
        ArrayList<RcsChatbotHelper.RcsChatbot> mData;

        public BlackChatbotAdapter(ArrayList<RcsChatbotHelper.RcsChatbot> data) {
            this.mData = data;
        }

        @Override
        public RcsBlackChatbotActivity.BlackChatbotAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_item, parent, false);
            return new RcsBlackChatbotActivity.BlackChatbotAdapter.ViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(RcsBlackChatbotActivity.BlackChatbotAdapter.ViewHolder holder, final int position) {
            holder.name.setText(mData.get(position).name);
            holder.id.setText(RcsChatbotHelper.parseChatbotServierIdToNumber(mData.get(position).serviceId));
            holder.more.setVisibility(View.VISIBLE);
            RcsChatbotUtils.getDefaultIcon(holder.icon, mData.get(position).icon);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(RcsBlackChatbotActivity.this, RcsChatBotDetailActivity.class);
                    intent.putExtra(RcsChatBotDetailActivity.KEY_CHATBOT_NUMBER, mData.get(position).serviceId);
                    startActivityForResult(intent, REQUEST_CHATBOT_INFO);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public ImageView icon;
            public TextView id;
            public ImageView more;

            public ViewHolder(View v, int viewType) {
                super(v);
                name = (TextView) v.findViewById(R.id.name);
                icon = (ImageView) v.findViewById(R.id.icon);
                id = (TextView) v.findViewById(R.id.id);
                more = (ImageView) v.findViewById(R.id.chatbot_more);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHATBOT_INFO) {
            searchBlackChatbotList();//搜索黑名单中的 chatbot, 用户移出黑名单再回到该界面需要调用该方法进行刷新
        }
    }
}
