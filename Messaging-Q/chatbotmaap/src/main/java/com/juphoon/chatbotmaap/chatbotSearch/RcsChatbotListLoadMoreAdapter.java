package com.juphoon.chatbotmaap.chatbotSearch;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.view.TextViewSnippet;
import com.juphoon.helper.RcsChatbotHelper;

import java.util.ArrayList;
import java.util.List;

public class RcsChatbotListLoadMoreAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mHost;
    //chatbot布局
    private static final int CHAT_BOT_VIEW = 0;
    //角布局
    private static final int FOOT_VIEW = 1;
    //空布局
    private static final int EMPTY_VIEW = 2;

    private static final int First = 3;
    // 当前加载状态，默认为加载完成
    private int loadState = 2;
    // 正在加载
    public static final int LOADING = 1;
    // 加载完成
    public static final int LOADING_COMPLETE = 2;
    // 加载到底
    public static final int LOADING_END = 3;
    //空布局提示语
    private String mEmptyTextString;
    private List<RcsChatbotHelper.RcsChatbot> mChatbots = new ArrayList<>();

    public RcsChatbotListLoadMoreAdapter(Context context) {
        mHost = context;
    }

    public void setEmptyViewText(String emptyViewText) {
        mEmptyTextString = emptyViewText;
    }

    public void setChatbots(List<RcsChatbotHelper.RcsChatbot> chatbots) {
        if (chatbots == null) {
            chatbots = new ArrayList<>();
        }
        mChatbots = chatbots;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        Log.i("ooo","viewType---:"+viewType);
        Log.i("ooo","mChatbots---:"+mChatbots.size());
        if (mChatbots.size() == 0) {
//            return EMPTY_VIEW;
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_footview, parent, false);
            return new EmptyViewHolder(view);
        }
        if(viewType == 0){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbots_list_item, parent, false);
            view.setBackgroundResource(R.drawable.act_list_item);
        }else if (viewType == mChatbots.size()-1){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbots_list_item, parent, false);
            view.setBackgroundResource(R.drawable.act_list_item_last);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbots_list_item, parent, false);
            view.setBackgroundResource(R.drawable.act_list_item_center);
        }

//        if (viewType == CHAT_BOT_VIEW) {
//            Log.i("ooo","viewType---ssssssssssssss:"+viewType);
//            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbots_list_item, parent, false);

           // view.setBackgroundResource(R.drawable.act_list_item_center);

            final ChatbotViewHolder viewHolder = new ChatbotViewHolder(view);
            viewHolder.itemView.setFocusable(false);
            viewHolder.itemView.setFocusableInTouchMode(false);
            viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        Log.i("ooo","viewType---: facus");
                        viewHolder.id.setTextColor(Color.BLACK);
                        viewHolder.name.setTextColor(Color.BLACK);
                    }else{
                        Log.i("ooo","viewType---: nofacus");
                        viewHolder.id.setTextColor(Color.WHITE);
                        viewHolder.name.setTextColor(Color.WHITE);
                    }
                }
            });

            return new ChatbotViewHolder(view);
      //  } else if (viewType == FOOT_VIEW) {
//            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbots_list_item, parent, false);
//            view.setBackgroundResource(R.drawable.act_list_item_last);
//            return new ChatbotViewHolder(view);
//            View footView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_refresh_footer, parent, false);
      //      return new FootViewHolder(footView);
      //  } else if (viewType == EMPTY_VIEW) {
//            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_footview, parent, false);
//            return new EmptyViewHolder(view);
    //    } else if(viewType == First){
//            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbots_list_item, parent, false);
//            view.setBackgroundResource(R.drawable.act_list_item);
//            return new ChatbotViewHolder(view);
     //   }
        //return null;
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if (holder instanceof EmptyViewHolder) {
            EmptyViewHolder emptyViewHolder = (EmptyViewHolder) holder;
            if (!TextUtils.isEmpty(mEmptyTextString)) {
                emptyViewHolder.emptyView.setText(mEmptyTextString);
            }
            return;
        }
        Log.i("ooo"," mChatbots.get(position):"+position);
        if (holder instanceof ChatbotViewHolder) {
            ChatbotViewHolder chatbotViewHolder = (ChatbotViewHolder) holder;
            final RcsChatbotHelper.RcsChatbot chatbot = mChatbots.get(position);
            final String serviceId = chatbot.serviceId;
            chatbotViewHolder.name.setText(chatbot.name);
            chatbotViewHolder.id.setText(RcsChatbotHelper.parseChatbotServierIdToNumber(serviceId));
            RcsChatbotUtils.getDefaultIcon(chatbotViewHolder.icon, chatbot.icon);
            chatbotViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (RcsChatbotUtils.checkChatBotIsInConversation(mHost, serviceId) || RcsChatbotUtils.checkChatBotIsSaveLocal(serviceId)) {
                        RcsChatbotUtils.startChatBotConversation(mHost, serviceId, null, null);
                    } else {
//                        RcsChatbotUtils.startChatBotDetailActivity(mHost, serviceId);
                        RcsChatbotUtils.startChatBotConversation(mHost, serviceId, null, null);
                    }
                //    mHost.finish();
                }
            });
            chatbotViewHolder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.i("ooo"," mChatbots.get(setOnTouchListener):");
                    RcsChatbotUtils.startChatBotConversation(mHost, serviceId, null, null);
                    return false;
                }
            });
        }else if (holder instanceof FootViewHolder) {
            FootViewHolder footViewHolder = (FootViewHolder) holder;
            switch (loadState) {
                case LOADING: // 正在加载
          //          footViewHolder.pbLoading.setVisibility(View.VISIBLE);
                    footViewHolder.tvLoading.setVisibility(View.VISIBLE);
                    footViewHolder.llEnd.setVisibility(View.GONE);
                    break;

                case LOADING_COMPLETE: // 加载完成
            //        footViewHolder.pbLoading.setVisibility(View.INVISIBLE);
                    footViewHolder.tvLoading.setVisibility(View.INVISIBLE);
                    footViewHolder.llEnd.setVisibility(View.GONE);
                    break;

                case LOADING_END: // 加载到底
//                    footViewHolder.pbLoading.setVisibility(View.GONE);
                    footViewHolder.tvLoading.setVisibility(View.GONE);
                    footViewHolder.llEnd.setVisibility(View.VISIBLE);
                    break;

                default:
                    break;
            }
        }


    }

    @Override
    public int getItemCount() {
        if (mChatbots != null && !mChatbots.isEmpty()) {
            return mChatbots.size();
        }
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
//        if (mChatbots.size() == 0) {
//            return EMPTY_VIEW;
//        }
//        if(position == 1){
//            return First;
//        }
//        if (position == getItemCount() - 1) {
//            return FOOT_VIEW;
//        }
//        return CHAT_BOT_VIEW;
        return position;
    }

    /**
     * 设置上拉加载状态
     *
     * @param loadState 0.正在加载 1.加载完成 2.加载到底
     */
    public void setLoadState(int loadState) {
        this.loadState = loadState;
        notifyDataSetChanged();
    }

    public class ChatbotViewHolder extends RecyclerView.ViewHolder {
        public TextViewSnippet name;
        public ImageView icon;
        public TextView id;

        public ChatbotViewHolder(View v) {
            super(v);
            name = (TextViewSnippet) v.findViewById(R.id.name);
            icon = (ImageView) v.findViewById(R.id.icon);
            id = (TextView) v.findViewById(R.id.id);
        }
    }

    private class EmptyViewHolder extends RecyclerView.ViewHolder {
        public TextView emptyView;

        public EmptyViewHolder(View itemView) {
            super(itemView);
            emptyView = itemView.findViewById(R.id.empty_textView);
        }
    }

    private class FootViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout footView;
        ProgressBar pbLoading;
        TextView tvLoading;
        LinearLayout llEnd;

        FootViewHolder(View itemView) {
            super(itemView);
            footView = (LinearLayout) itemView;
           // pbLoading = (ProgressBar) itemView.findViewById(R.id.pb_loading);
            tvLoading = (TextView) itemView.findViewById(R.id.tv_loading);
            llEnd = (LinearLayout) itemView.findViewById(R.id.ll_end);
        }
    }
}

