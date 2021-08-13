package com.juphoon.chatbotmaap;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RcsImServiceConstants;

import java.util.Arrays;

public class RcsChatbotComplaintActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String SERVICEID = "service_id";
    public static final String IMDN = "imdn_string";
    public static final String BODY = "body";

    RcsChatbotHelper.RcsChatbot mChatbotInfo;
    private RadioGroup mTypeRadioGroup;
    private TextView mCommitTV;
    private EditText mReasonET;
    private ImageView mServiceIconIV;
    private TextView mServiceNameTV;
    private TextView mServiceIdTV;
    private LinearLayout mComplaintMessageLinearLayout;
    private LinearLayout mComplaintUserLinearLayout;
    private String mServiceId;
    private String mImdnString;
    private String mBody;
    private TextView mMessageContent;
    private TextView mComplaintTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_chatbot_complaint);
        initData();
        initView();
    }

    private void initData() {
        mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(getIntent().getStringExtra(SERVICEID));
        mServiceId = getIntent().getStringExtra(SERVICEID);
        mImdnString = getIntent().getStringExtra(IMDN);
        mBody = getIntent().getStringExtra(BODY);
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot_complain);
        }
        mComplaintUserLinearLayout = (LinearLayout) findViewById(R.id.complaint_user);
        mServiceIconIV = (ImageView) findViewById(R.id.service_icon);
        mServiceNameTV = (TextView) findViewById(R.id.service_name);
        mServiceIdTV = (TextView) findViewById(R.id.service_id);
        mComplaintMessageLinearLayout = (LinearLayout) findViewById(R.id.complaint_message);
        mMessageContent = (TextView) findViewById(R.id.message_content);
        mComplaintTitle = (TextView) findViewById(R.id.complaint_title);
        mReasonET = (EditText) findViewById(R.id.complaint_reason);
        mCommitTV = (TextView) findViewById(R.id.commit);
        mCommitTV.setOnClickListener(this);
        if (TextUtils.isEmpty(mImdnString)) {
            mComplaintTitle.setText(R.string.chatbot_complaint_user);
            mComplaintMessageLinearLayout.setVisibility(View.GONE);
            mComplaintUserLinearLayout.setVisibility(View.VISIBLE);
        } else {
            mComplaintTitle.setText(R.string.chatbot_complaint_content);
            mComplaintUserLinearLayout.setVisibility(View.GONE);
            mComplaintMessageLinearLayout.setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(mBody)) {
                mMessageContent.setText(mBody);
            }
        }
        mTypeRadioGroup = (RadioGroup) findViewById(R.id.complaint_type_radioGroup);
        RcsChatbotUtils.getDefaultIcon(mServiceIconIV, mChatbotInfo.icon);
        mServiceNameTV.setText(mChatbotInfo.name);
        mServiceIdTV.setText(mChatbotInfo.sms);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.commit) {
            if (TextUtils.isEmpty(mReasonET.getText().toString())) {
                Toast.makeText(RcsChatbotComplaintActivity.this, R.string.chatbot_complaint_reason_null, Toast.LENGTH_SHORT).show();
                return;
            }
            mTypeRadioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = (RadioButton) findViewById(mTypeRadioGroup.getCheckedRadioButtonId());
            String imdnString = RcsCallWrapper.rcsSendMessage1To1("", mServiceId,
                    RcsChatbotHelper.genComplainXml(getIntent().getStringExtra(SERVICEID), mImdnString == null ? null : Arrays.asList(mImdnString), radioButton.getText().toString(), mReasonET.getText().toString()), RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SPAN, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, null, null, null);
            Toast.makeText(RcsChatbotComplaintActivity.this, imdnString == null ? com.juphoon.chatbotmaap.R.string.chatbot_complain_failed : com.juphoon.chatbotmaap.R.string.chatbot_complain_success, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
