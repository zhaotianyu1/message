package com.android.messaging.tcl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.messaging.BugleApplication;
import com.android.messaging.ui.UIIntents;

/**
 * 对外提供接口
 */
public class ApiReceiver extends BroadcastReceiver {


    public final static String ACTION_SHUTDOWN = "com.aodlanucher.intent.startMessage";//广播类型
    Bundle sceneTransitionAnimationOptions = null;//数据传递
    boolean hasCustomTransitions = false;

    /**
     * 接收到广播
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(ACTION_SHUTDOWN.equals(action)){
            Log.i("ooo","接收到广播-----------");
            String messageInfo = intent.getStringExtra("messageInfo");
            if(null != messageInfo){
                Log.i("ooo","data-----------:"+messageInfo);
                //调用发送消息方法
                getSendMessage(messageInfo);
            }
        }
    }

    /**
     * 打开发送消息界面
     * @param conversationId
     */
    public void getSendMessage(String conversationId){

        if("".equals(conversationId)||conversationId !=null) {
            UIIntents.get().launchConversationActivity(
                    BugleApplication.getContext(), conversationId, null,
                    sceneTransitionAnimationOptions,
                    hasCustomTransitions);
        }

    }

}
