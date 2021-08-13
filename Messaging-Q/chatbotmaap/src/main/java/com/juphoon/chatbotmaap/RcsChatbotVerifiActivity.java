package com.juphoon.chatbotmaap;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class RcsChatbotVerifiActivity extends AppCompatActivity {
    public static final String VERFIINFO = "verfi_info";
    public static final String VERFIPABY = "verfiby";

    private LinearLayout mVerfiInfoLayout;
    private LinearLayout mVerfiByLayout;
    private TextView mVerfiInfoText;
    private TextView mVerfiByText;

    private String mVerfiInfo;
    private String mVerfiBy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_chatbot_verifi);
        initData();
        initView();
    }

    private void initData() {
        mVerfiInfo = getIntent().getStringExtra(VERFIINFO);
        mVerfiBy = getIntent().getStringExtra(VERFIPABY);
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot_verfi);
        }
        mVerfiInfoLayout = (LinearLayout) findViewById(R.id.verifi_Info_layout);
        mVerfiByLayout = (LinearLayout) findViewById(R.id.verifi_by_layout);
        mVerfiInfoText = (TextView) findViewById(R.id.verifi_info_text);
        mVerfiByText = (TextView) findViewById(R.id.verifi_by_text);
        mVerfiInfoLayout.setVisibility(TextUtils.isEmpty(mVerfiInfo) ? View.GONE : View.VISIBLE);
        mVerfiByLayout.setVisibility(mVerfiBy == null ? View.GONE : View.VISIBLE);
        mVerfiInfoText.setText(mVerfiInfo);
        mVerfiByText.setText(mVerfiBy);
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

}
