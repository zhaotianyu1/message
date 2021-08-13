package com.juphoon.chatbotmaap.chatbotDetail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.RcsChatbotWebActivity;
import com.juphoon.chatbotmaap.ToastUtils;
import com.juphoon.chatbotmaap.chatbotSearch.RcsChatbotServiceActivity;
import com.juphoon.helper.RcsChatbotHelper;

public class RcsChatbotDetailMoreActivity extends AppCompatActivity  implements View.OnClickListener {
    public static final String SERVICEID = "service_id";
    private RcsChatbotHelper.RcsChatbot mChatbotInfo;
    private RcsChatbotInfoBean mRcsChatbotInfoBean;
    private LinearLayout mDescriptionLinearLayout, mSmsLinearLayout, mCallbackLinearLayout, tcPageLinearLayout,
            mEmailLinearLayout, mCategoryLinearlayout, mWebSiteLinearLayout;
    private TextView mDescriptionTextView, mCallBackTextView,
    mSmsTextView, mEmailTextView, mCategoryTextView, mWebSiteTextView, mWebTcPageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_activity_detail_more);
        initActionBar();
        initChatbotInfo();
        initChatbotInfoView();



    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot_detail_about);
        }
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


    private void initChatbotInfoView() {
        //服务描述
        mDescriptionTextView = (TextView) findViewById(R.id.description_text_view);
        mDescriptionLinearLayout = (LinearLayout) findViewById(R.id.description_linear_layout);
        mDescriptionLinearLayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.serviceDescription) ? View.GONE : View.VISIBLE);
        mDescriptionTextView.setText(mChatbotInfo.serviceDescription);
       //短信号码
        mSmsLinearLayout = (LinearLayout) findViewById(R.id.sms_linear_layout);
        mSmsLinearLayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.sms) ? View.GONE : View.VISIBLE);
        mSmsTextView = (TextView) findViewById(R.id.sms_text_view);
        mSmsTextView.setText(RcsChatbotUtils.parseSmsToNumber(mChatbotInfo.sms));
        //回乎号码
        mCallBackTextView = (TextView) findViewById(R.id.call_back_text_view);
        mCallbackLinearLayout = (LinearLayout) findViewById(R.id.call_back_linear_layout);
        mCallbackLinearLayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.callback) ? View.GONE : View.VISIBLE);
        mCallBackTextView.setText(mChatbotInfo.callback);
        //证书
        tcPageLinearLayout = (LinearLayout) findViewById(R.id.tcPage_linear_layout);
        tcPageLinearLayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.tcpage) ? View.GONE : View.VISIBLE);
        tcPageLinearLayout.setOnClickListener(this);

        //电子邮箱
        mEmailLinearLayout = (LinearLayout) findViewById(R.id.email_linear_layout);
        mEmailTextView = (TextView) findViewById(R.id.email_text_view);
        mEmailLinearLayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.email) ? View.GONE : View.VISIBLE);
        mEmailTextView.setText(mChatbotInfo.email);
        //法律条款
        mCategoryLinearlayout = (LinearLayout) findViewById(R.id.category_linear_layout);
        mCategoryTextView = (TextView) findViewById(R.id.chatbot_category);
        mCategoryLinearlayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.category) ? View.GONE : View.VISIBLE);
        mCategoryTextView.setText(mChatbotInfo.category);

        mWebTcPageTextView =(TextView)findViewById(R.id.chatbot_tcPage_text);
        mWebTcPageTextView.setText(mChatbotInfo.tcpage);

        mWebSiteLinearLayout = (LinearLayout) findViewById(R.id.website_linear_layout);
        mWebSiteTextView = (TextView) findViewById(R.id.chatbot_website_text);
        mWebSiteLinearLayout.setVisibility(TextUtils.isEmpty(mChatbotInfo.website) ? View.GONE : View.VISIBLE);
        mWebSiteTextView.setText(mChatbotInfo.website);
        mWebSiteTextView.setOnClickListener(this);
    }

    private void initChatbotInfo() {
        mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(getIntent().getStringExtra(SERVICEID));
        if (mChatbotInfo == null) {
            finish();
            return;
        } else {
            mRcsChatbotInfoBean = new Gson().fromJson(mChatbotInfo.json, RcsChatbotInfoBean.class);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.call_back_text_view) {
            if (TextUtils.isEmpty(mChatbotInfo.callback)) {
                ToastUtils.showToast(this, getString(R.string.chatbot_detail_no_call_back));
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mChatbotInfo.callback));
            startActivity(intent);
        } else if (id == R.id.tcPage_linear_layout) {
            Intent intentWebView = new Intent(this, RcsChatbotWebActivity.class);
            intentWebView.putExtra(RcsChatbotWebActivity.URL, mChatbotInfo.tcpage);
            startActivity(intentWebView);
        } else if (id == R.id.chatbot_website_text || id == R.id.chatbot_tcPage_text) {
            Intent intentWebView = new Intent(this, RcsChatbotWebActivity.class);
            intentWebView.putExtra(RcsChatbotWebActivity.URL, mWebSiteTextView.getText());
            startActivity(intentWebView);
        }
    }
}
