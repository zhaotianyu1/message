package com.android.messaging;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.messaging.BugleApplication;
import com.android.messaging.FactoryImpl;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DataModelImpl;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.ui.UIIntents;


import java.util.ArrayList;
import java.util.List;

/**
 * 对外接口
 */
public class Api {

    Bundle sceneTransitionAnimationOptions = null;
    boolean hasCustomTransitions = false;
    //名称
    private String name;
    //对话id
    private String thread_id;
    //内容
    private String snippet_text;
    //时间
    private Long time;



    private Context context;
    private BugleApplication bugleApplication;



    public void setContext(Context context) {
        this.context = context;
    }

    public BugleApplication getBugleApplication() {
        return bugleApplication;
    }

    public void setBugleApplication(BugleApplication bugleApplication) {
        this.bugleApplication = bugleApplication;
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

    /**
     * 接收消息接口
     */
    @SuppressLint("WrongConstant")
    public void getReceiveMessage(){

        Log.i("ooo","发送广播------");
        String UNIQUE_STRING="com.tcl.messaging.receiveMessage";
        Intent intent=new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(UNIQUE_STRING);
        intent.setComponent(new ComponentName("com.tcl.aodlauncher",
                "com.tcl.aodlauncher.receiver.MessageBroadcastReceiver"));
        intent.putExtra("thread_id",thread_id);
        intent.putExtra("name",name);
        intent.putExtra("snippet_text",snippet_text);
        intent.putExtra("time",time);
        BugleApplication.getInstance().sendBroadcast(intent);
        Log.i("ooo","发送广播------结束");

    }

    public void getMessage(String name,String conversitionId,String snippet_text,Long time){

        this.name = name;
        this.thread_id = conversitionId;
        this.snippet_text =snippet_text;
        this.time = time;
        getReceiveMessage();

    }

    /**
     * 获取消息列表接口
     */
    public  List<ConversationListItemData> getAllMessage(){

//        if(context== null){
//            context = BugleApplication.getContext();
//            Log.i("ooo","BugleApplication==null");
//        }

        DataModel dataModel =  new DataModelImpl(BugleApplication.getContext());
        DatabaseWrapper  db = dataModel.getDatabase();

        List<ConversationListItemData> conversationListItemData = new ArrayList<>();
        conversationListItemData= BugleDatabaseOperations.getExistingConversation(db);
        Log.i("ooo","api-getAllMessage---:"+conversationListItemData.size());
        return conversationListItemData;

    }


}
