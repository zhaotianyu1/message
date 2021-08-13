package com.juphoon.chatbotmaap.bottomMenu;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbot.RcsChatbotSuggestionsBean;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotCardView;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.tcl.SimpleItemDecoration;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RcsImServiceConstants;

import java.util.List;

public class ListViewDialog extends Dialog {

    private final Context context;
    private String mConversationId;
    private String mChatbotServiceId;
    private RecyclerView listview;
    private MainAdapter adapter;
    List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> entriess;

    public ListViewDialog(@NonNull Context context , List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> entries
    , String mConversationIds,String mChatbotServiceIds) {
        super(context);
        this.context = context;
        this.entriess = entries;
        this.mConversationId = mConversationIds;
        this.mChatbotServiceId = mChatbotServiceIds;
        initViews();
    }

    private void initViews() {
        View view = LayoutInflater.from(context).inflate(R.layout.ac_dialog_recyclew, null);
        Window dialogWindow = getWindow();
        dialogWindow.setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayoutManager linearLayoutManager_record = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        listview = view.findViewById(R.id.recycle_listviews);
        listview.setLayoutManager(linearLayoutManager_record);
//        listview.setBackgroundResource(R.drawable.launcher_bg);
        adapter = new MainAdapter(context,entriess);
        listview.setAdapter(adapter);
        listview.addItemDecoration(new SimpleItemDecoration());
        listview.requestFocus();
        listview.setFocusable(true);
        listview.setFocusableInTouchMode(true);
        setContentView(view);
//        listview.setItemsCanFocus(true);
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

    }



    /**
     * 列表的adapter
     */
    public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ContactViewHolder> {

        private final Context context;

        List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> mMenuEntriesList;
        private TextView titleTv;

        private MainAdapter(Context context, List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> entries) {
            this.context = context;
            mMenuEntriesList = entries;
            Log.d("ooo", "mMenuEntriesList:size "+mMenuEntriesList.size());
        }
        private int mCurrentItem=0;
        private boolean isClick=false;
        public void setCurrentItem(int currentItem){
            this.mCurrentItem=currentItem;
        }

        public void setClick(boolean click){
            this.isClick=click;
        }



        @NonNull
        @Override
        public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.i("ooo","onCreateViewHolder----:---:");
            View view = LayoutInflater.from(context).inflate(R.layout.chatbot_menu_item_layout, null);

            ContactViewHolder viewHolder = new ContactViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final ContactViewHolder holder, final int position) {
            final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries currentItemEntries = mMenuEntriesList.get(position);
            if(position == 0){
                Log.i("ooo","----:---:0");
                holder.itemView.setBackgroundResource(R.drawable.act_list_items);
            }else if (position == mMenuEntriesList.size()-1){
                Log.i("ooo","----:---:-11");
                holder.itemView.setBackgroundResource(R.drawable.act_list_item_lasts);
            }else{
                Log.i("ooo","----:---:-2");
                holder.itemView.setBackgroundResource(R.drawable.act_list_item_centers);
            }
            String text = getDisplayText(currentItemEntries);
            holder.menu_text.setText(text);
            Log.i("ooo","text----:---:"+text);
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
            Log.i("ooo","getItemCount----:---:"+ mMenuEntriesList.size());
            return mMenuEntriesList.size();
        }

//        @SuppressLint("ResourceAsColor")
//        @Override
//        public View getView(final int position, View convertView, ViewGroup parent) {
//            final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries currentItemEntries = mMenuEntriesList.get(position);
//            String text = getDisplayText(currentItemEntries);
//            View view = LayoutInflater.from(context).inflate(R.layout.chatbot_menu_item_layout, null);
//          //  AllCellsGlowLayout items = view.findViewById(R.id.items);
//            TextView textView = view.findViewById(com.juphoon.chatbotmaap.R.id.menu_text);
//            textView.setText(text);
//
//            Log.d("ooo", "getView: "+position);
//            if (mCurrentItem == position && isClick){
//                Log.d("ooo", "istouch: ");
//                view.setBackgroundResource(R.color.action_bar_background_color_dark);
//                textView.setTextColor(context.getResources().getColor(R.color.biaoti));
//            }else{
//                Log.d("ooo", "notouch: ");
//                view.setBackgroundResource(R.color.biaoti);
//                textView.setTextColor(context.getResources().getColor(R.color.action_bar_background_color_dark));
//            }
////            textView.setOnClickListener(new View.OnClickListener() {
////                @Override
////                public void onClick(View v) {
////                    Log.i("ooo","123123132131");
////                    onClickMenuItem(position);
////                }
////            });
////            items.setOnClickListener(new View.OnClickListener() {
////                @Override
////                public void onClick(View v) {
////                    Log.i("ooo","123123132131");
////                    onClickMenuItem(position);
////                }
////            });
//
//
//            return view;
//
//        }
        public void onClickMenuItem(int position) {
            final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries currentItemEntries = mMenuEntriesList.get(position);
            if (currentItemEntries.menu == null) {
                doMenuAction(currentItemEntries);
            } else {
                //不支持二级菜单
                //  doSubMenuItemClick(currentItemEntries, view);
            }
        }
        class ContactViewHolder extends  RecyclerView.ViewHolder {
            TextView menu_text;
            public ContactViewHolder(@NonNull View itemView) {
                super(itemView);
                menu_text = itemView.findViewById(R.id.menu_text);
            }
        }
    }


    public static String getDisplayText(RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries entrie) {
        if (entrie.action != null) {
            return entrie.action.displayText;
        } else if (entrie.menu != null) {
            return entrie.menu.displayText;
        } else if (entrie.reply != null) {
            return entrie.reply.displayText;
        }
        return "";
    }
    public void doMenuAction(RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries item) {
        if (item.action != null) {
            Log.i("nbv","----------doMenuAction---action");
            RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
            replyBean.response = new RcsChatbotReplyBean.ResponseBean();
            replyBean.response.action = new RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean();
            replyBean.response.action.displayText = item.action.displayText;
            replyBean.response.action.postback = item.action.postback;
            RcsCallWrapper.rcsSendMessage1To1("", mChatbotServiceId, new Gson().toJson(replyBean), RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SUGGESTION, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, null, null, null);
            RcsChatbotCardView.dealSugesstion(item.action, context, mConversationId, mChatbotServiceId, null, null, null);
            SharedPreferences sp = context.getSharedPreferences("sp_demo", Context.MODE_PRIVATE);
            sp.edit().putInt("age", 11).apply();
        } else if (item.reply != null) {
            Log.i("nbv","----------doMenuAction---reply");
            RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
            replyBean.response = new RcsChatbotReplyBean.ResponseBean();
            replyBean.response.reply = new RcsChatbotSuggestionsBean.SuggestionsBean.ReplyBean();
            replyBean.response.reply.displayText = item.reply.displayText;
            replyBean.response.reply.postback = item.reply.postback;
            Intent intent = new Intent();
            intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
            intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationId);
            intent.setAction(RcsChatbotDefine.ACTION_MENU_REPLY);
            intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
            getContext().sendBroadcast(intent);
        }
        dismiss();
    }
}
