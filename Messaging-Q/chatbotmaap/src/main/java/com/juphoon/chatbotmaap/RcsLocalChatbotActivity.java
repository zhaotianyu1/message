package com.juphoon.chatbotmaap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RmsDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RcsLocalChatbotActivity extends AppCompatActivity implements RcsBroadcastHelper.IChatbotListener, View.OnClickListener {

    private RecyclerView mRecyclerViewLocal;
    private TextView mSearchTextView,mlocalViewTextView;
    private ImageView mScanImageView, mLocalImageView;
    private RcsChatbotDeepLink mDeepLink;
    private String mCookie;

    private RcsLocalChatbotAdapter mLocalChatbotAdapter;
    private List<RcsChatbotHelper.RcsChatbot> mLocalChatbots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_activity_local);

        initView();
        initViewData();
        RcsBroadcastHelper.addChatbotListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
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
            Intent intent = new Intent(RcsLocalChatbotActivity.this, RcsBlackChatbotActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot);
        }

        mRecyclerViewLocal = (RecyclerView) findViewById(R.id.recycler_local_chatbot);
        mlocalViewTextView = (TextView) findViewById(R.id.text_local_title);
        mSearchTextView = (TextView) findViewById(R.id.layout_search_jump);
        mScanImageView = (ImageView) findViewById(R.id.scan);
        mLocalImageView = (ImageView) findViewById(R.id.location_view);
        mScanImageView.setOnClickListener(this);
        mLocalImageView.setOnClickListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerViewLocal.setLayoutManager(linearLayoutManager);
        mLocalChatbotAdapter = new RcsLocalChatbotAdapter();
        mRecyclerViewLocal.setAdapter(mLocalChatbotAdapter);
        LinearLayoutManager recommendLinearLayoutManager = new LinearLayoutManager(this);
        mSearchTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RcsLocalChatbotActivity.this, RcsChatbotSearchActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initViewData() {
        refreshData();
    }

    @Override
    public void onChatbotRecommandList(String cookie, boolean result, String json) {

    }

    @Override
    public void onChatbotList(String s, boolean b, String s1) {

    }

    @Override
    public void onChatbotInfo(String cookie, boolean result, String s1) {
        if(TextUtils.equals(mCookie,cookie)){
            if(!result){
                showValidChatbot();
            }
        }
    }

    private void refreshData() {
        mlocalViewTextView.setVisibility(View.GONE);
        mRecyclerViewLocal.setVisibility(View.GONE);
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
                mLocalChatbots = rcsChatbots;
                mLocalChatbotAdapter.notifyDataSetChanged();
                if(mLocalChatbots.size()>0){
                    mlocalViewTextView.setVisibility(View.VISIBLE);
                    mRecyclerViewLocal.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.scan) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1000);
            } else {
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.setAction(Intents.Scan.ACTION);
                startActivityForResult(intent, 1);
            }
        } else if (i == R.id.location_view) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
            } else {
                Intent intent = new Intent(this, RcsChatBotNearbyChatbotActivity.class);
                startActivity(intent);
            }
        }
    }

    public class RcsLocalChatbotAdapter extends RecyclerView.Adapter<RcsLocalChatbotAdapter.ViewHolder> {
        public final int MaxDisplayCount = 4;
        public static final int NORMAL_TYPE = 0;
        public static final int NO_SEARCH_RESULT_TYPE = 1;

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView icon;
            TextView id;

            public ViewHolder(View v) {
                super(v);
                name = (TextView) v.findViewById(R.id.name);
                icon = (ImageView) v.findViewById(R.id.icon);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == NORMAL_TYPE) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_local_item, parent, false);
                return new ViewHolder(view);
            } else if (viewType == NO_SEARCH_RESULT_TYPE) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_footview, parent, false);
                TextView noSearchText = (TextView) view.findViewById(R.id.empty_textView);
                noSearchText.setText(R.string.chatbot_no_local_result);
                return new ViewHolder(view);
            }
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) == NO_SEARCH_RESULT_TYPE) {
                return;
            }
            if (position == MaxDisplayCount - 1) {
                holder.name.setText(getString(R.string.chatbot_more));
                holder.icon.setImageResource(R.drawable.more);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(RcsLocalChatbotActivity.this, RcsChatbotMoreBotsActivity.class);
                        intent.putExtra(RcsChatbotMoreBotsActivity.REQUEST_CHATBOTS_TYPE, RcsChatbotMoreBotsActivity.REQUEST_ALL_LOCAL_CHATBOT);
                        startActivity(intent);
                    }
                });
                return;
            }
            final RcsChatbotHelper.RcsChatbot chatbot = mLocalChatbots.get(position);
            holder.name.setText(chatbot.name);
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
            if (mLocalChatbots.size() == 0) {
                return 1;
            }
            if (mLocalChatbots.size() >= MaxDisplayCount) {
                return MaxDisplayCount;
            }
            return mLocalChatbots.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (mLocalChatbots.size() == 0) {
                return NO_SEARCH_RESULT_TYPE;
            } else {
                return NORMAL_TYPE;
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { //RESULT_OK = -1
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Intents.Scan.RESULT);
            dealScanResult(scanResult);
        }
    }

    private void dealScanResult(String scanResult) {
        mDeepLink = new RcsChatbotDeepLink(Uri.parse(scanResult));
        if (mDeepLink.isValid()) {
            startChatbotConversation(mDeepLink);
        } else {
            showValidQC();
        }
    }

    private void showValidQC() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chatbot_no_chatbot_QC))
                .setNegativeButton(R.string.chatbot_sure, null)
                .show();
    }

    private void showValidChatbot() {
        new AlertDialog.Builder(this)
                .setTitle("未能查询到相关chatbot信息")
                .setNegativeButton(R.string.chatbot_sure, null)
                .show();
    }

    private void startChatbotConversation(RcsChatbotDeepLink deepLink) {
        if (!TextUtils.isEmpty(mDeepLink.getServiceId())) {
            RcsChatbotHelper.RcsChatbot chatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(mDeepLink.getServiceId());
            if (chatbotInfo == null) {
                RcsChatbotHelper.addCallback(new chatbotInfoListener());
                RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
                    @Override
                    public void run(boolean succ, String resultCode, String token) {
                        if (succ) {
                            mCookie =  UUID.randomUUID().toString();
                            int ret = RcsCallWrapper.rcsGetChatbotInfo(mCookie, token, mDeepLink.getServiceId(), "");
                            if (ret == -1) {
                                showValidChatbot();
                            }
                        } else {
                            //结束
                            showValidChatbot();
                        }
                    }
                });
            } else {
                jumpToChatbotConversation(mDeepLink);
            }
        } else if (!TextUtils.isEmpty(mDeepLink.getSms())) {
            String smsNumber = deepLink.getSms();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + smsNumber));
            if (!TextUtils.isEmpty(deepLink.getBody())) {
                String mSmsBody = deepLink.getBody();
                intent.putExtra("sms_body", mSmsBody);
            }
            this.startActivity(intent);
            finish();
        }
    }

    public class chatbotInfoListener implements RcsChatbotHelper.Callback {

        @Override
        public void onChatbotInfoChange() {
            RcsChatbotHelper.RcsChatbot chatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(mDeepLink.getServiceId());
            if (chatbotInfo != null) {
                jumpToChatbotConversation(mDeepLink);
            } else {
                showValidChatbot();
            }
        }
    }

    private void jumpToChatbotConversation(RcsChatbotDeepLink deepLink) {
        if (!TextUtils.isEmpty(deepLink.getSuggestions())) {
            RcsChatbotUtils.sendDeepLinkBroadCast(RcsLocalChatbotActivity.this, deepLink.getServiceId(), deepLink.getSuggestions());
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + RcsChatbotHelper.formatServiceIdWithNoSip(deepLink.getServiceId())));
        if (!TextUtils.isEmpty(deepLink.getBody())) {
            String mSmsBody = deepLink.getBody();
            intent.putExtra("sms_body", mSmsBody);
        }
        startActivity(intent);
        finish();
    }
}
