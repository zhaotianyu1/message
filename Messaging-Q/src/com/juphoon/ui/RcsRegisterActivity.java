package com.juphoon.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.juphoon.chatbotmaap.RcsChatbotWebActivity;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.messaging.R;
import com.android.messaging.util.BuglePrefs;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsNumberUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsJsonParamConstants;

public class RcsRegisterActivity extends AppCompatActivity implements TextWatcher {
    private EditText mPhoneEt,mPasswordEt;
    private Button mRegisterBtn;
    private TextView mChoseCountryView;
    private boolean mIsOnCreate = true;
    public static final int COUNTRY_REQUEST_CODER = 12;
    public static final String COUNTRY_NAME = "countryName";
    public static final String COUNTRY_NUMBER = "countryNumber";
    private Context mContext;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        RcsConfigHelper.ShowDialogTime.saveValue(this, System.currentTimeMillis());
        setContentView(R.layout.rcs_google_register_activity);
        initView();
        initToolbar();
        mPhoneEt.setFocusable(true);
        mPhoneEt.setFocusableInTouchMode(true);
        mPhoneEt.requestFocus();
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(mPhoneEt, 0);
        mIsOnCreate = RcsServiceManager.isLogined();
        if (true) {//若需要则需判断RcsServiceManager是否初始化
            RcsServiceManager.addCallBack(sIServiceManager);
        }
    }

    private void initView() {
        mPhoneEt = (EditText) findViewById(R.id.rcs_register_number);
        SpannableString s = new SpannableString("请输入账号");
        AbsoluteSizeSpan textSize = new AbsoluteSizeSpan(14,true);
        s.setSpan(textSize,0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mPhoneEt.setHint(s);
        mPhoneEt.setText("17857021320");
        mPasswordEt = (EditText)findViewById(R.id.rcs_register_password);
        SpannableString s1 = new SpannableString("请输入密码");
        AbsoluteSizeSpan textSize1 = new AbsoluteSizeSpan(14,true);
        s1.setSpan(textSize1,0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mPasswordEt.setHint(s1);
        mPasswordEt.setText("Qwe12345");
        mRegisterBtn = (Button) findViewById(R.id.rcs_register_btn);
        mRegisterBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    mRegisterBtn.setTextColor(Color.BLACK);
                }else{
                    mRegisterBtn.setTextColor(Color.WHITE);
                }
            }
        });
//        mChoseCountryView = (TextView) findViewById(R.id.chose_country_num);
        mPhoneEt.addTextChangedListener(this);
//        mChoseCountryView.setText("+" + 86);
        String account = RcsConfigHelper.LastAccount.getAccount(this);

        if (!TextUtils.isEmpty(account)) {
//            mChoseCountryView.setText("+" + 86);
            mPhoneEt.setText(RcsNumberUtils.formatPhoneNoCountryPrefix(account));
        } else {
//            mRegisterBtn.setEnabled(false);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (true) {//初始化
            RcsServiceManager.removeCallBack(sIServiceManager);
        }
    }

    private void initToolbar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().hide();
        getSupportActionBar().setTitle(R.string.rcs_login);
    }

    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.rcs_register_btn) {
            if (TextUtils.isEmpty(mPhoneEt.getText().toString())) {
                return;
            }
            if(TextUtils.isEmpty(mPasswordEt.getText().toString())){
                return;
            }
            RcsCallWrapper.rcsLoginByMsisdn("+86"+ mPhoneEt.getText().toString(), mPasswordEt.getText().toString());
            RcsConfigHelper.LastAccount.saveAccount(RcsRegisterActivity.this, "+86" + mPhoneEt.getText().toString());
          //  mRegisterBtn.setEnabled(false);
            mPhoneEt.setEnabled(false);
//            mChoseCountryView.setEnabled(false);
            showProgress();
        }
        if (i == R.id.register_text) {
            Intent intent = new Intent(this, RcsChatbotWebActivity.class);
            final String registerAddress = "https://www.rcsdev.juphoon.com/app/login.html";
            intent.putExtra(RcsChatbotWebActivity.URL, registerAddress);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if (charSequence.length() > 50) {
//            mRegisterBtn.setEnabled(false);
//            mRegisterBtn.setClickable(false);
            //  Toast.makeText(RcsRegisterActivity.this, getString(R.string.rcs_google_max_length), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(charSequence)) {
//            mRegisterBtn.setEnabled(true);
//            mRegisterBtn.setClickable(true);
        } else {
//            mRegisterBtn.setEnabled(false);
//            mRegisterBtn.setClickable(false);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }


    private RcsServiceManager.IServiceManagerCallback sIServiceManager = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(final boolean logined) {
            if (mIsOnCreate) {
                mIsOnCreate = false;
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRegisterBtn.setEnabled(true);
                    mPhoneEt.setEnabled(true);
//                    mChoseCountryView.setEnabled(true);
                    hideProgress();
                    Log.i("ccc","logined--:"+logined);
                    if (logined) {
                        setResult(1);
                        finish();
                    } else {
                        setResult(-1);
                        finish();
                    }
                }
            });
        }
    };


    private void showProgress() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(this);
            mProgress.setTitle(R.string.rcs_logining);
            mProgress.setCancelable(false);
        }
        mProgress.show();
    }


    private void hideProgress() {
        if (mProgress != null) {
            mProgress.dismiss();
        }
    }

}
