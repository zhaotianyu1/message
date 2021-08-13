package com.juphoon.chatbotmaap.chatbotMessages;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.juphoon.chatbotmaap.R;

public class RcsChatBotConversationListAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> {
    private RcsChatbotConversationListItemView.HostInterface mHostInterface;

    private class ConversationViewHolder extends RecyclerView.ViewHolder {
        RcsChatbotConversationListItemView mView;

        public ConversationViewHolder(@NonNull RcsChatbotConversationListItemView itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    public RcsChatBotConversationListAdapter(Context context, Cursor c, RcsChatbotConversationListItemView.HostInterface hostInterface) {
        super(context, c, 0);
        mHostInterface = hostInterface;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder, Context context, Cursor cursor) {
        RcsChatBotConversationListItemData mData = new RcsChatBotConversationListItemData();
        mData.bind(cursor, false);
        if (holder instanceof ConversationViewHolder) {
            RcsChatbotConversationListItemView conversationListItemView = ((ConversationViewHolder) holder).mView;
            conversationListItemView.bind(cursor, mHostInterface);
        }
    }


    @Override
    public RecyclerView.ViewHolder createViewHolder(Context context, ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatbot_conversation_list_item, parent, false);
        return new ConversationViewHolder((RcsChatbotConversationListItemView) view);
    }
}
