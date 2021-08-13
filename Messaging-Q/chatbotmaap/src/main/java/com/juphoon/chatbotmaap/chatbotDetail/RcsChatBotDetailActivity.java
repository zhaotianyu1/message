package com.juphoon.chatbotmaap.chatbotDetail;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotComplaintActivity;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.chatbotmaap.RcsChatbotImageFragmentActivity;
import com.juphoon.chatbotmaap.RcsChatbotSettingActivity;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.RcsChatbotVerifiActivity;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.tcl.ff.component.animer.glow.view.AllCellsGlowLayout;
import com.tcl.uicompat.TCLButton;

public class RcsChatBotDetailActivity extends AppCompatActivity implements View.OnClickListener,View.OnFocusChangeListener {
    public final static String KEY_CHATBOT_NUMBER = "chatbot_number";
    private RcsChatbotHelper.RcsChatbot mChatbotInfo;
    private RcsChatbotInfoBean mRcsChatbotInfoBean;
    private ImageView mServiceIconImageView,mBackGroundImageView;
    private TCLButton mSaveBtn;
            private TextView mServiceNameTextView, mServiceIdTextView, mDescriptionTextView;
//    private Switch mBlackSwitch, mNotifySwitch;
    private boolean mIsSaveLocal; //是否关注
    private boolean mIsLocalBlack;     //是否在黑名单
    private boolean mIsNotify; //是否免打扰


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_activity_details);
        initChatBotData();
        initActionBar();
        initChatbotInfoView();
//        Runnable run2 = new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    View rootview = RcsChatBotDetailActivity.this.getWindow().getDecorView();
//                    View aaa = rootview.findFocus();
//                    Log.i("xxx", aaa.getId()+"");
//                    Log.i("xxx","id = 0x"+Integer.toHexString(aaa.getId()));
//                    Log.i("xxx", aaa.toString());
//                }
//            }
//        };
//        new Thread(run2).start();
    }

    private void initChatBotData() {
        mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(getIntent().getStringExtra(KEY_CHATBOT_NUMBER));
        if (mChatbotInfo == null) {
            finish();
            return;
        } else {
            mRcsChatbotInfoBean = new Gson().fromJson(mChatbotInfo.json, RcsChatbotInfoBean.class);
        }
        mIsSaveLocal = mChatbotInfo.saveLocal;
        mIsLocalBlack = RcsChatbotHelper.isLocalBlockChatbot(mChatbotInfo.serviceId);
        mIsNotify = mChatbotInfo.muteNotify;
        // if (mChatbotInfo.deadline < System.currentTimeMillis() / 1000) {
        if (true) {
            RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
                @Override
                public void run(boolean succ, String resultCode, String token) {
                    if (succ) {
                        RcsCallWrapper.rcsGetChatbotInfo(null, token, mChatbotInfo.serviceId, mChatbotInfo.etag);
                    }
                }
            });
        }
        RcsChatbotHelper.addCallback(new RcsChatbotHelper.Callback() {
            @Override
            public void onChatbotInfoChange() {
                mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(getIntent().getStringExtra(KEY_CHATBOT_NUMBER));
                initChatbotInfoView();
            }
        });
    }

    private TCLButton start_conversation_btns;
    private void initChatbotInfoView() {
        mBackGroundImageView = (ImageView) findViewById(R.id.back_ground);
        start_conversation_btns = findViewById(R.id.start_conversation_btn);
        start_conversation_btns.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    start_conversation_btns.setTextColor(Color.BLACK);
                }else{
                    start_conversation_btns.setTextColor(Color.WHITE);
                }
            }
        });
        start_conversation_btns.setFocusable(true);
        start_conversation_btns.setFocusableInTouchMode(true);
        start_conversation_btns.requestFocus();
        if (!TextUtils.isEmpty(mChatbotInfo.backgroundImage)) {
            RcsChatbotUtils.setIcon(mBackGroundImageView, mChatbotInfo.backgroundImage, false);
        }
        mDescriptionTextView = findViewById(R.id.description_text_view);
        if (!TextUtils.isEmpty(mChatbotInfo.colour)) {
            setTextColor();
        }
        mServiceIconImageView = findViewById(R.id.service_icon);
        RcsChatbotUtils.getDefaultIcon(mServiceIconImageView, mChatbotInfo.icon);
        mServiceNameTextView = findViewById(R.id.service_name);
        mServiceNameTextView.setText(mChatbotInfo.name);
        mServiceIdTextView = findViewById(R.id.service_id);
        mServiceIdTextView.setText(RcsChatbotHelper.parseChatbotServierIdToNumber(mChatbotInfo.serviceId));
        if (!TextUtils.isEmpty(mChatbotInfo.serviceDescription)) {
            mDescriptionTextView.setVisibility(View.VISIBLE);
            mDescriptionTextView.setText(mChatbotInfo.serviceDescription);
        }
//        mSaveBtn = findViewById(R.id.save_chatBot_btn);
//        mSaveBtn.setText(mIsSaveLocal ? R.string.chatbot_unfollow_chatbot : R.string.chatbot_follow_chatbot);
//        mSaveBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if(hasFocus){
//                    mSaveBtn.setTextColor(Color.BLACK);
//                }else{
//                    mSaveBtn.setTextColor(Color.WHITE);
//                }
//            }
//        });
//        mBlackSwitch = findViewById(R.id.black_switch);
        //应急小程序无法加入黑名单
//        if (mChatbotInfo.special) {
//            mBlackSwitch.setEnabled(true);
//            ImageView verifiedImage = findViewById(R.id.verified_image);
//            verifiedImage.setImageDrawable(getDrawable(R.drawable.yingji));
//        }
//        //认证信息处理
//        if (mRcsChatbotInfoBean == null || mRcsChatbotInfoBean.botverification == null) {
//            View verifiedItem = findViewById(R.id.verified_more_btn);
//            ImageView verifiedMore = findViewById(R.id.verified_click_more);
//            ImageView verifiedImage = findViewById(R.id.verified_image);
//            verifiedItem.setVisibility(View.GONE);
//            verifiedImage.setVisibility(View.GONE);
//            verifiedMore.setVisibility(View.GONE);
//        }
//        mBlackSwitch.setChecked(mIsLocalBlack);
//        mNotifySwitch = findViewById(R.id.notify_switch);
//        mNotifySwitch.setChecked(mIsNotify);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.chatbot_details);
        }
    }

//    public  void rerurns(View view){
//        onBackPressed();
//    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
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


    private void setTextColor() {
        try {
            int textColor = Color.parseColor(mChatbotInfo.colour);
            mDescriptionTextView.setTextColor(textColor);
        } catch (Exception e) {

        }
    }
    @Override
    public void onFocusChange(View view, boolean hasFocus) {

    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
//        if (id == R.id.save_chatBot_btn) {
//            mIsSaveLocal = !mIsSaveLocal;
//            RcsChatbotHelper.saveChatbotAsContact(mChatbotInfo.serviceId, mIsSaveLocal);
//            mSaveBtn.setText(mIsSaveLocal ? R.string.chatbot_unfollow_chatbot : R.string.chatbot_follow_chatbot);
//        } else
            if (id == R.id.start_conversation_btn) {
            Intent intent = new Intent();
            intent.putExtra(RcsChatbotDefine.KEY_ADDRESS, RcsChatbotHelper.formatServiceIdWithNoSip(mChatbotInfo.serviceId));
            intent.setAction(RcsChatbotDefine.ACTION_LAUNCH_CONVERSATION);
            intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
            sendBroadcast(intent);
        } else if (id == R.id.service_icon) {
            Intent intent = new Intent(this, RcsChatbotImageFragmentActivity.class);
            intent.putExtra(RcsChatbotImageFragmentActivity.URL, mChatbotInfo.icon);
            intent.putExtra(RcsChatbotImageFragmentActivity.CHATBOT_ICON, true);
            startActivity(intent);
        }
//            else
//        if (id == R.id.complaint_more_btn) {
//            Intent intent = new Intent(this, RcsChatbotComplaintActivity.class);
//            intent.putExtra(RcsChatbotComplaintActivity.SERVICEID, mChatbotInfo.serviceId);
//            startActivity(intent);
//        } else if (id == R.id.verified_more_btn) {
//            Intent intent = new Intent(this, RcsChatbotVerifiActivity.class);
//            if (mRcsChatbotInfoBean != null && mRcsChatbotInfoBean.botverification != null) {
//                if (mRcsChatbotInfoBean.botverification.verificationinfo.expires != null) {
//                    intent.putExtra(RcsChatbotVerifiActivity.VERFIINFO, mRcsChatbotInfoBean.botverification.verificationinfo.expires);
//                }
//                if (mRcsChatbotInfoBean.botverification.verificationinfo.verifiedby != null) {
//                    intent.putExtra(RcsChatbotVerifiActivity.VERFIPABY, mRcsChatbotInfoBean.botverification.verificationinfo.verifiedby);
//                }
//                startActivity(intent);
//            }
//        } else if (id == R.id.notify_switch) {
//            mIsNotify = !mIsNotify;
////            mNotifySwitch.setChecked(mIsNotify);
//            updateChatbotMuteNotify(mChatbotInfo.serviceId, mIsNotify);
//        } else if (id == R.id.black_switch) {
//            mIsLocalBlack = !mIsLocalBlack;
////            mBlackSwitch.setChecked(mIsLocalBlack);
//            updateChatbotToBlackList(mChatbotInfo.serviceId, mIsLocalBlack);
//        } else if (id == R.id.detail_more_btn) {
//            Intent intent = new Intent(this, RcsChatbotDetailMoreActivity.class);
//            intent.putExtra(RcsChatbotComplaintActivity.SERVICEID, mChatbotInfo.serviceId);
//            startActivity(intent);
//        }
    }


    private void updateChatbotToBlackList(String number, final boolean black) {
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                return RcsChatbotHelper.updateChatbotToBlock(strings[0], Boolean.parseBoolean(strings[1]));
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    if (black) {
                        Toast toast = Toast.makeText(RcsChatBotDetailActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_add_blacklist));
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(RcsChatBotDetailActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_remove_blacklist));
                        toast.show();
                    }
                }
            }
        }.execute(number, String.valueOf(black));
    }

    private void updateChatbotMuteNotify(String number, final boolean muteNotify) {
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                return RcsChatbotHelper.updateChatbotMuteNotify(strings[0], Boolean.parseBoolean(strings[1]));
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    if (muteNotify) {
                        Toast toast = Toast.makeText(RcsChatBotDetailActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_set_mute_notify));
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(RcsChatBotDetailActivity.this, null, Toast.LENGTH_SHORT);
                        toast.setText(mChatbotInfo.name + getString(R.string.chatbot_cancel_mute_notify));
                        toast.show();
                    }
                }
            }
        }.execute(number, String.valueOf(muteNotify));
    }


}

