package com.juphoon.chatbotmaap.chatbotSearch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotDeepLink;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsTokenHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;

import java.util.UUID;

public class RcsChatbotScanHelper {

    public void dealScanResult(Context context, String scanResult) {
        RcsChatbotDeepLink deepLink = new RcsChatbotDeepLink(Uri.parse(scanResult));
        if (deepLink.isValid()) {
            startChatbotConversation(context, deepLink);
        } else {
            showValidQC(context);
        }
    }

    private void startChatbotConversation(final Context context, final RcsChatbotDeepLink deepLink) {
        if (!TextUtils.isEmpty(deepLink.getServiceId())) {
            RcsChatbotHelper.RcsChatbot chatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(deepLink.getServiceId());
            if (chatbotInfo == null) {
                RcsChatbotHelper.addCallback(new chatbotInfoListener(context,deepLink));
                RcsTokenHelper.getToken(new RcsTokenHelper.ResultOperation() {
                    @Override
                    public void run(boolean succ, String resultCode, String token) {
                        if (succ) {
                            String cookie = UUID.randomUUID().toString();
                            int ret = RcsCallWrapper.rcsGetChatbotInfo(cookie, token, deepLink.getServiceId(), "");
                            if (ret == -1) {
                                showValidChatbot(context);
                            }
                        } else {
                            //结束
                            showValidChatbot(context);
                        }
                    }
                });
            } else {
                jumpToChatbotConversation(context,deepLink);
            }
        } else if (!TextUtils.isEmpty(deepLink.getSms())) {
            String smsNumber = deepLink.getSms();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + smsNumber));
            if (!TextUtils.isEmpty(deepLink.getBody())) {
                String mSmsBody = deepLink.getBody();
                intent.putExtra("sms_body", mSmsBody);
            }
            context.startActivity(intent);
            //finish();
        }
    }

    private void showValidChatbot(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("未能查询到相关chatbot信息")
                .setNegativeButton(R.string.chatbot_sure, null)
                .show();
    }

    private void showValidQC(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.chatbot_no_chatbot_QC))
                .setNegativeButton(R.string.chatbot_sure, null)
                .show();
    }


    public class chatbotInfoListener implements RcsChatbotHelper.Callback {
        RcsChatbotDeepLink mDeepLink;
        Context mContext;

        public chatbotInfoListener(Context context,RcsChatbotDeepLink deepLink) {
            super();
            mDeepLink = deepLink;
            mContext = context;
        }

        @Override
        public void onChatbotInfoChange() {
            RcsChatbotHelper.RcsChatbot chatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(mDeepLink.getServiceId());
            if (chatbotInfo != null) {
                jumpToChatbotConversation(mContext,mDeepLink);
            } else {
                showValidChatbot(mContext);
            }
        }
    }


    private void jumpToChatbotConversation(Context context, RcsChatbotDeepLink deepLink) {
        if (!TextUtils.isEmpty(deepLink.getSuggestions())) {
            RcsChatbotUtils.sendDeepLinkBroadCast(context, deepLink.getServiceId(), deepLink.getSuggestions());
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + RcsChatbotHelper.formatServiceIdWithNoSip(deepLink.getServiceId())));
        if (!TextUtils.isEmpty(deepLink.getBody())) {
            String mSmsBody = deepLink.getBody();
            intent.putExtra("sms_body", mSmsBody);
        }
        context.startActivity(intent);
    }
}
