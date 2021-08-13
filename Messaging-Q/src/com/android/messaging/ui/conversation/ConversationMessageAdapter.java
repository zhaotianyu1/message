/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.messaging.ui.conversation;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.ui.CursorRecyclerAdapter;
import com.android.messaging.ui.AsyncImageView.AsyncImageViewDelayLoader;
import com.android.messaging.ui.conversation.ConversationMessageView.ConversationMessageViewHost;

import java.util.List;

/**
 * Provides an interface to expose Conversation Message Cursor data to a UI widget like a
 * RecyclerView.
 */
public class ConversationMessageAdapter extends
    CursorRecyclerAdapter {

    private final ConversationMessageViewHost mHost;
    private final AsyncImageViewDelayLoader mImageViewDelayLoader;
    private final View.OnClickListener mViewClickListener;
    private final View.OnLongClickListener mViewLongClickListener;
    private final CheckBox.OnCheckedChangeListener mCheckBoxChangeListener; //juphoon
    private boolean mOneOnOne;
    private String mSelectedMessageId;
 	private boolean showCheckBox = false; // juphoon
    // by juphoon 记录选中状态
    private SparseBooleanArray mCheckBoxState = new SparseBooleanArray();

    public ConversationMessageAdapter(final Context context, final Cursor cursor,
        final ConversationMessageViewHost host,
        final AsyncImageViewDelayLoader imageViewDelayLoader,
        final View.OnClickListener viewClickListener,
        final View.OnLongClickListener longClickListener) {
        super(context, cursor, 0);
        mHost = host;
        mViewClickListener = viewClickListener;
        mViewLongClickListener = longClickListener;
        mImageViewDelayLoader = imageViewDelayLoader;
        mCheckBoxChangeListener = null;//juphoon
        setHasStableIds(true);
    }

	//juphoon
    public ConversationMessageAdapter(final Context context, final Cursor cursor,
            final ConversationMessageViewHost host,
            final AsyncImageViewDelayLoader imageViewDelayLoader,
            final View.OnClickListener viewClickListener,
            final View.OnLongClickListener longClickListener,
            final CheckBox.OnCheckedChangeListener checkBoxChangeListener) {
            super(context, cursor, 0);
            mHost = host;
            mViewClickListener = viewClickListener;
            mViewLongClickListener = longClickListener;
            mImageViewDelayLoader = imageViewDelayLoader;
            mCheckBoxChangeListener = checkBoxChangeListener;
            setHasStableIds(true);
        }
    private Cursor cursors;
    @Override
    public void bindViewHolder(final RecyclerView.ViewHolder holder,
            final Context context, final Cursor cursor,int positiona) {
        if (holder instanceof ConversationMessageViewHolder){
            cursors = cursor;
            int position = cursor.getPosition(); // juphoon
            final ConversationMessageView conversationMessageView =
                    (ConversationMessageView) ((RecyclerView.ViewHolder)holder).itemView;
            //juphoon
            conversationMessageView.setSelected(mCheckBoxState.get(position));
            conversationMessageView.bind(cursor, mOneOnOne, null);
        }


//        final CheckBox checkbox = (CheckBox) holder.mCheckbox;
//        checkbox.setOnCheckedChangeListener(null);
//        checkbox.setVisibility(View.GONE);
////        if (showCheckBox) {
////            holder.mCheckbox.setVisibility(View.VISIBLE);
////        } else {
////            holder.mCheckbox.setVisibility(View.GONE);
////        }
//        checkbox.setChecked(mCheckBoxState.get(position));
//        checkbox.setTag(cursor.getPosition());
//        conversationMessageView.setTag(cursor.getPosition());
//        checkbox.setOnCheckedChangeListener(mCheckBoxChangeListener);

    }

    @Override
    public void bindViewHolder_list(RecyclerView.ViewHolder holder, Context context, Cursor cursor, int position, List list) {

    }

    @Override
    public void bindViewHolder_message(RecyclerView.ViewHolder holder, Context context, Cursor cursor, int position, List conversationListItemDataList) {

    }


    @Override
    public RecyclerView.ViewHolder createViewHolder(final Context context,
            final ViewGroup parent, final int viewType) {
        if (viewType == ITEM_CONTENT) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            final ConversationMessageView conversationMessageView = (ConversationMessageView)
                    layoutInflater.inflate(R.layout.conversation_message_view, null);
            conversationMessageView.setHost(mHost);
            conversationMessageView.setImageViewDelayLoader(mImageViewDelayLoader);

            return new ConversationMessageViewHolder(conversationMessageView,
                    mViewClickListener, mViewLongClickListener);
        }
        if (viewType == ITEM_FOOT){
            final LayoutInflater minflater = LayoutInflater.from(context);
            View footView = minflater.inflate(R.layout.act_layout, parent, false);
            HeaderViewHdlder headerViewHdlder =new HeaderViewHdlder(footView);
//            headerViewHdlder.record_mode.setFocusable(false);
//            headerViewHdlder.record_mode.setFocusableInTouchMode(false);

            headerViewHdlder.itemView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.KEYCODE_DPAD_DOWN) {
                        Log.i("ooo","KEYCODE_DPAD_DOWN:");
                        keyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                        headerViewHdlder.record_mode.setFocusable(false);

                    }
                    if(event.getAction() == KeyEvent.KEYCODE_DPAD_UP){
                        Log.i("ooo","KEYCODE_DPAD_UP:");
                        headerViewHdlder.record_mode.setFocusable(true);
                    }
                    return false;
                }
            });

            return headerViewHdlder;
        }

        return null;
    }

    public void setSelectedMessage(final String messageId) {
        mSelectedMessageId = messageId;
        notifyDataSetChanged();
    }

    public void setOneOnOne(final boolean oneOnOne, final boolean invalidate) {
        if (mOneOnOne != oneOnOne) {
            mOneOnOne = oneOnOne;
            if (invalidate) {
                notifyDataSetChanged();
            }
        }
    }

	//juphoon
    public void showCheckBox(boolean showCheckbox) {
        showCheckBox = showCheckbox;
        if (!showCheckbox) {
            mCheckBoxState.clear();
        }
        notifyDataSetChanged();
    }

	//juphoon
    public void updateCheckBoxState(int position, boolean value, boolean only/*清除所有选中，以该次设置为准*/) {
        if (only) {
            mCheckBoxState.clear();
        }
        mCheckBoxState.put(position, value);
        notifyDataSetChanged();
    }

    /**
    * ViewHolder that holds a ConversationMessageView.
    */
    public static class ConversationMessageViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final CheckBox mCheckbox; // juphoon
        /**
         * @param viewClickListener a View.OnClickListener that should define the interaction when
         *        an item in the RecyclerView is clicked.
         */
        public ConversationMessageViewHolder(final View itemView,
                final View.OnClickListener viewClickListener,
                final View.OnLongClickListener viewLongClickListener) {
            super(itemView);
            mView = itemView;

            mView.setOnClickListener(viewClickListener);
            mView.setOnLongClickListener(viewLongClickListener);
            mCheckbox = (CheckBox) itemView.findViewById(R.id.cb_conversation_msg_select);
        }
    }

    private final static int ITEM_CONTENT=0;
    private final static int ITEM_FOOT=1;
    private int pos;
    private int mFoot=1;

    @Override
    public int getItemViewType(int position) {
        Log.i("ooo","position:"+position);
        Log.i("ooo","BugleApplication.getInstance().getCursor().getCount():"+BugleApplication.getInstance().getCursor().getCount());
        pos = position;
        if (mFoot != 0 && position == BugleApplication.getInstance().getCursor().getCount()) {
            Log.i("ooo","position:isfront");
            return ITEM_FOOT;

        }
        return ITEM_CONTENT;
    }

    class HeaderViewHdlder extends RecyclerView.ViewHolder{
        ImageView record_mode;
        public HeaderViewHdlder(@NonNull View itemView) {
            super(itemView);
            record_mode = itemView.findViewById(R.id.record_mode);
        }
    }


}
