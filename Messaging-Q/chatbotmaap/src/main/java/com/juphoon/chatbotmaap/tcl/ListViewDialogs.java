package com.juphoon.chatbotmaap.tcl;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbot.RcsChatbotSuggestionsBean;
import com.juphoon.chatbotmaap.Location.RcsBaiduLocationActivity;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotCalendarReminderUtils;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.chatbotmaap.RcsChatbotPermission;
import com.juphoon.chatbotmaap.RcsChatbotPermissionActivity;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.RcsChatbotWebActivity;
import com.juphoon.chatbotmaap.bottomMenu.ListViewDialog;
import com.juphoon.rcs.tool.RcsNetUtils;

import java.util.List;

public class ListViewDialogs extends Dialog {

    private final Context context;
    List<RcsChatbotSuggestionsBean.SuggestionsBean> suggestion;
    private RecyclerView listview;
    private MainAdapter adapter;
    private String titles;
    private String contents;

//    TextView card_titles;
//    TextView card_contents;

    String mConversationIds;
    String mRmsUris;
    String mServiceIds;
    String mTrafficTypes;
    String mContributionIds;

    public ListViewDialogs(@NonNull Context context, List<RcsChatbotSuggestionsBean.SuggestionsBean> suggestions,String title,String content,
                           String mConversationId,String mRmsUri,String mServiceId,String mTrafficType,String mContributionId) {
        super(context);
        this.context = context;
        this.suggestion  = suggestions;
        this.titles = title;
        this.contents = content;
        this.mConversationIds = mConversationId;
        this.mRmsUris = mRmsUri;
        this.mServiceIds = mServiceId;
        this.mTrafficTypes = mTrafficType;
        this.mContributionIds = mContributionId;
        initViews();
    }

    private void initViews() {

        View view = LayoutInflater.from(context).inflate(R.layout.ac_dialog_mains, null);
        Window dialogWindow = getWindow();
        dialogWindow.setBackgroundDrawableResource(android.R.color.transparent);

//        card_titles = view.findViewById(R.id.card_titles);
//        card_contents = view.findViewById(R.id.card_contents);
        listview = view.findViewById(R.id.main_listview);

        LinearLayoutManager linearLayoutManager_record = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        listview.setLayoutManager(linearLayoutManager_record);
//        if(titles != null){
//            card_titles.setVisibility(View.VISIBLE);
//            card_titles.setText(titles);
//        }
//        if(contents !=null){
//            card_contents.setVisibility(View.VISIBLE);
//            card_contents.setText(contents);
//        }
        adapter = new MainAdapter(context,suggestion);
        listview.setAdapter(adapter);
        listview.addItemDecoration(new SimpleItemDecoration());
        listview.requestFocus();
        listview.setFocusable(true);
        listview.setFocusableInTouchMode(true);

//        listview.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
//                Log.d("ooo", "listview----positionvvvv: "+position);
//                adapter.setCurrentItem(position);
//                adapter.setClick(true);
//                adapter.notifyDataSetChanged();
////                view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
////                    @Override
////                    public void onFocusChange(View v, boolean hasFocus) {
////                        Log.d("ooo", "listview----hasFocus: "+position+hasFocus);
////                    }
////                });
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
//        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d("ooo", "listview----onItemClick: "+position);
//                adapter.onClickMenuItem(position);
//            }
//        });

        setContentView(view);

    }
    /**
     * 列表的adapter
     */
    public class MainAdapter extends  RecyclerView.Adapter<MainAdapter.ContactsViewHolder> {

        private final Context context;
        List<RcsChatbotSuggestionsBean.SuggestionsBean> suggestions;

        private MainAdapter(Context context, List<RcsChatbotSuggestionsBean.SuggestionsBean> entries) {
            this.context = context;
            suggestions = entries;
            Log.d("ooo", "mMenuEntriesList:size "+suggestions.size());
        }
        private int mCurrentItem=0;
        private boolean isClick=false;
        public void setCurrentItem(int currentItem){
            this.mCurrentItem=currentItem;
        }
//        @Override
//        public int getCount() {
//            return suggestions.size();
//        }
        public void setClick(boolean click){
            this.isClick=click;
        }

//        @Override
//        public Object getItem(int position) {
//            return null;
//        }

        @NonNull
        @Override
        public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.chatbot_menu_item_layout, null);

            ContactsViewHolder viewHolder = new ContactsViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final ContactsViewHolder holder, final int position) {
            RcsChatbotSuggestionsBean.SuggestionsBean current = suggestions.get(position);
            if(position == 0){
                Log.i("ooo","----:---:0");
                holder.itemView.setBackgroundResource(R.drawable.act_list_items);
            }else if (position == suggestions.size()-1){
                Log.i("ooo","----:---:-11");
                holder.itemView.setBackgroundResource(R.drawable.act_list_item_lasts);
            }else{
                Log.i("ooo","----:---:-2");
                holder.itemView.setBackgroundResource(R.drawable.act_list_item_centers);
            }
            if (current.action != null) {
                String text = current.action.displayText;
                holder.menu_text.setText(text);
            }
            if (current.reply != null) {
                String text = current.reply.displayText;
                holder.menu_text.setText(text);
            }
            holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        holder.menu_text.setTextColor(Color.BLACK);
                    }else{
                        holder.menu_text.setTextColor(Color.WHITE);
                    }
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickMenuItem(position);
                }
            });
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }

//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View view = LayoutInflater.from(context).inflate(R.layout.chatbot_menu_item_layout, null);
//            RcsChatbotSuggestionsBean.SuggestionsBean current = suggestions.get(position);
//            TextView textView = view.findViewById(com.juphoon.chatbotmaap.R.id.menu_text);
//            if (current.action != null) {
//                String text = current.action.displayText;
//                textView.setText(text);
//            }
//            if (current.reply != null) {
//                String text = current.reply.displayText;
//                textView.setText(text);
//            }
//
//            if (mCurrentItem == position && isClick){
//                Log.d("ooo", "istouch: ");
//                view.setBackgroundResource(R.color.action_bar_background_color_dark);
//                textView.setTextColor(context.getResources().getColor(R.color.biaoti));
//            }else{
//                Log.d("ooo", "notouch: ");
//                view.setBackgroundResource(R.color.biaoti);
//                textView.setTextColor(context.getResources().getColor(R.color.action_bar_background_color_dark));
//            }
//
//            return view;
//        }

        class ContactsViewHolder extends  RecyclerView.ViewHolder {
            TextView menu_text;
            public ContactsViewHolder(@NonNull View itemView) {
                super(itemView);
                menu_text = itemView.findViewById(R.id.menu_text);
            }
        }

        public void onClickMenuItem(int position) {
            final RcsChatbotSuggestionsBean.SuggestionsBean suggestion = suggestions.get(position);
            if (suggestion.action != null) {
                Log.i("vcx","------------suggestion.action");
                RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
                replyBean.response = new RcsChatbotReplyBean.ResponseBean();
                replyBean.response.action = new RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean();
                replyBean.response.action.displayText = suggestion.action.displayText;
                replyBean.response.action.postback = suggestion.action.postback;
                Intent intent = new Intent();
                intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
                intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationIds);
                intent.putExtra(RcsChatbotDefine.KEY_RMSURI, mRmsUris);
                intent.setAction(RcsChatbotDefine.ACTION_SUGGESTION);
                intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                getContext().sendBroadcast(intent);
                dealSugesstion(suggestion.action, getContext(), mConversationIds, mServiceIds, mRmsUris, mTrafficTypes, mContributionIds);
                dismiss();
            }
            if(suggestion.reply !=null){
                Log.i("vcx","------------suggestion.reply");
                RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
                replyBean.response = new RcsChatbotReplyBean.ResponseBean();
                replyBean.response.reply = new RcsChatbotSuggestionsBean.SuggestionsBean.ReplyBean();
                replyBean.response.reply.displayText = suggestion.reply.displayText;
                replyBean.response.reply.postback = suggestion.reply.postback;
                Intent intent = new Intent();
                intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
                intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationIds);
                intent.putExtra(RcsChatbotDefine.KEY_RMSURI, mRmsUris);
                intent.setAction(RcsChatbotDefine.ACTION_REPLY);
                intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                getContext().sendBroadcast(intent);
                dismiss();
            }

        }
    }
    private static int RECORD_VIDEO = 0;
    private static int RECORD_AUDIO = 2;
    public static void dealSugesstion(final RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean action, final Context context, final String conversationId, final String serviceId, final String rmsUri, final String trafficType, final String contributionId) {
        if (action.urlAction != null && action.urlAction.openUrl != null && RcsNetUtils.checkNet(context)) {
            String url = action.urlAction.openUrl.url;
            Intent intent;
            if (TextUtils.isEmpty(url)) {
                return;
            }
            if (TextUtils.equals(action.urlAction.openUrl.application, "browser")) {
                Uri uri = Uri.parse(url);
                if (uri.getScheme() != null) {
                    try {
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, "找不到要打开的应用程序", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                intent = new Intent(context, RcsChatbotWebActivity.class);
                intent.putExtra(RcsChatbotWebActivity.URL, url);
                intent.putExtra(RcsChatbotWebActivity.PARAMETERS, action.urlAction.openUrl.parameters);
                intent.putExtra(RcsChatbotWebActivity.VIEWMODE, action.urlAction.openUrl.viewMode);
                context.startActivity(intent);
            }
        } else if (action.dialerAction != null) {
            if (action.dialerAction.dialPhoneNumber != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                Uri data = Uri.parse("tel:" + action.dialerAction.dialPhoneNumber.phoneNumber);
                intent.setData(data);
                context.startActivity(intent);
            } else if (action.dialerAction.dialEnrichedCall != null) {
                // TODO
                Log.d("Chatbot", "dialEnrichedCall not support");
            } else if (action.dialerAction.dialVideoCall != null) {
                // TODO
                Log.d("Chatbot", "dialVideoCall not support");
            }
        } else if (action.mapAction != null) {
            if (action.mapAction.showLocation != null) {
                Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
                intent.putExtra(RcsBaiduLocationActivity.LOCATION_LATITUDE, action.mapAction.showLocation.location.latitude);
                intent.putExtra(RcsBaiduLocationActivity.CONVERSATIONID, conversationId);
                intent.putExtra(RcsBaiduLocationActivity.LOCATION_LONGITUDE, action.mapAction.showLocation.location.longitude);
                context.startActivity(intent);
                //显示地理位置 要求网可用
            } else if (action.mapAction.requestLocationPush != null && RcsNetUtils.checkNet(context)) {
                //发送地理位置+
                if (Build.VERSION.SDK_INT >= 23) {
                    //申请权限android.permission.WRITE_CALENDAR
                    RcsChatbotPermission.with(context)
                            .permisson(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
                            .callback(new RcsChatbotPermissionActivity.PermissionCallback() {
                                //权限通过
                                @Override
                                public void onPermissionGranted() {
                                    Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
                                    intent.putExtra(RcsBaiduLocationActivity.CONVERSATIONID, conversationId);
                                    context.startActivity(intent);
                                }

                                //被拒绝
                                @Override
                                public void shouldShowRational(String permisson) {
                                }

                                //被拒绝且勾选不再提示
                                @Override
                                public void onPermissonReject(String permisson) {
                                }
                            }).request();
                } else {
                    Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
                    intent.putExtra(RcsBaiduLocationActivity.CONVERSATIONID, conversationId);
                    context.startActivity(intent);
                }
            }
        } else if (action.calendarAction != null) {
            if (action.calendarAction.createCalendarEvent != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    //申请权限android.permission.WRITE_CALENDAR
                    RcsChatbotPermission.with(context)
                            .permisson(new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR})
                            .callback(new RcsChatbotPermissionActivity.PermissionCallback() {
                                //权限通过
                                @Override
                                public void onPermissionGranted() {
                                    RcsChatbotCalendarReminderUtils.addCalendarEvent(context, action.calendarAction.createCalendarEvent.title, action.calendarAction.createCalendarEvent.description, action.calendarAction.createCalendarEvent.startTime, action.calendarAction.createCalendarEvent.endTime);
                                }

                                //被拒绝
                                @Override
                                public void shouldShowRational(String permisson) {
                                }

                                //被拒绝且勾选不再提示
                                @Override
                                public void onPermissonReject(String permisson) {
                                }
                            }).request();
                } else {
                    RcsChatbotCalendarReminderUtils.addCalendarEvent(context, action.calendarAction.createCalendarEvent.title, action.calendarAction.createCalendarEvent.description, action.calendarAction.createCalendarEvent.startTime, action.calendarAction.createCalendarEvent.endTime);
                }
            }
        } else if (action.composeAction != null) {
            if (action.composeAction.composeTextMessage != null) {
                //写一条rcs消息 有号码和 text
                String url = "sms:" + action.composeAction.composeTextMessage.phoneNumber;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra("sms_body", action.composeAction.composeTextMessage.text);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
            } else if (action.composeAction.composeRecordingMessage != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String url = "sms:" + action.composeAction.composeRecordingMessage.phoneNumber;
                int index;
                if (TextUtils.equals(action.composeAction.composeRecordingMessage.type, "VIDEO")) {
                    index = RECORD_VIDEO;
                } else if (TextUtils.equals(action.composeAction.composeRecordingMessage.type, "AUDIO")) {
                    index = RECORD_AUDIO;
                } else {
                    return;
                }
                intent.putExtra("composeMessageIndex", index);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
            }
        } else if (action.deviceAction != null && RcsNetUtils.checkNet(context)) {
            if (action.deviceAction.requestDeviceSpecifics != null) {
                new AlertDialog.Builder(context, R.style.Juphoon_Dialog_style).setTitle(R.string.chatbot_share_user_date)
                        .setPositiveButton(R.string.chatbot_sure, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.putExtra(RcsChatbotDefine.KEY_RMSURI, rmsUri);
                                intent.putExtra(RcsChatbotDefine.KEY_CHATBOT_ID, serviceId);
                                intent.setAction(RcsChatbotDefine.ACTION_SHARE_DATA);
                                intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                                context.sendBroadcast(intent);

                            }
                        }).setNegativeButton(R.string.chatbot_cancel, null).show();
            }
        } else if (action.settingsAction != null) {
            if (action.settingsAction.disableAnonymization != null) {
                // TODO
                Log.d("Chatbot", "disableAnonymization not support");
            } else if (action.settingsAction.enableDisplayedNotifications != null) {
                // TODO
                Log.d("Chatbot", "enableDisplayedNotifications not support");
            }
        }
    }

}
