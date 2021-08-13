package com.android.messaging.tcl.ui.adapter;


import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.ui.CursorRecyclerAdapter;
import com.android.messaging.ui.conversationlist.ConversationListItemView;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.juphoon.chatbotmaap.RcsChatbotUtils;


import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends CursorRecyclerAdapter<MessageAdapter.ConversationListViewHolder> implements ConversationListData.ConversationListDataListener {
    private static final String TAG = "ZTY_MessageAdapter";

    final Resources resources = BugleApplication.getContext().getResources();
    private List<ConversationListItemData> conversationData = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context mContext;
    private final HostInterface mClivHostInterface;
    int mListItemReadColor = resources.getColor(R.color.conversation_list_item_read);
    int pressColor = resources.getColor(R.color.conversation_list_item_unread);

    int mListItemUnreadColor = resources.getColor(R.color.message_download_failed_timestamp_text);
    private ConversationListItemData mData = new ConversationListItemData();

    public  boolean isShow;
    public int read;
    int []photo = new int[]{R.drawable.ic_popup_avatar_nor};

    public interface HostInterface {
        boolean isConversationSelected(final String conversationId);
        void onConversationClicked(final ConversationListItemData conversationListItemData,
                                   boolean isLongClick, final ConversationListItemView conversationView);
        boolean isSwipeAnimatable();
        void startFullScreenPhotoViewer(final Uri initialPhoto, final Rect initialPhotoBounds,
                                        final Uri photosUri);
        void startFullScreenVideoViewer(final Uri videoUri);
        boolean isSelectionMode();
    }

    public MessageAdapter(Context context,  final Cursor cursor,final HostInterface clivHostInterface) throws IOException, JSONException {
        super(context, cursor, 0);
        Log.i(TAG, "Message");
        this.mContext=context;
        mInflater=LayoutInflater.from(context);
        mClivHostInterface =clivHostInterface;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.tcl.tv5g.message.adapter.MessageAdapter");

        setHasStableIds(true);

    }
    int i = 0;

    @Override
    public void bindViewHolder(ConversationListViewHolder holder, Context context, Cursor cursor,int position) {
//        mData.bind(cursor);
//        conversationData.add(position,mData);
//
//        //头像
////        Drawable draPhoto= mContext.getResources().getDrawable(photo[0]);
////        holder.photo.setBackground(draPhoto);
//        Uri iconUri = null;
//        if (mData.getIcon() != null) {
//            iconUri = Uri.parse(mData.getIcon());
//            Log.i("bbb","不是空");
//        }
//        holder.photo.setImageResourceUri(iconUri, mData.getParticipantContactId(),
//                mData.getParticipantLookupKey(), mData.getOtherParticipantNormalizedDestination());
//        //内容
//
//        Log.i(TAG,"getSnippetText-----:"+getSnippetText());
//        if(null != getSnippetText()) {
//            holder.content.setText(getSnippetText());
//        }
//        getTimeContext(holder);
//
//        holder.name.setText(mData.getName());
//        int color;
//        if (mData.getIsRead() || mData.getShowDraft()) {
//            color = mListItemReadColor;
//        } else {
//            color = mListItemUnreadColor;
//        }
//        holder.name.setTextColor(color);
//        if(mData.getIsFailedStatus()){
//            Log.i(TAG,"发送失败-----:");
//            holder.content.setTextColor(Color.RED);
//        }
//        Log.i(TAG,"mData.getIsRead()123-:"+mData.getIsRead());
//        Log.i(TAG,"mData.getIsFailedStatus()123-:"+mData.getIsFailedStatus());
//
//        Log.i(TAG,"mData.getIsRead()-:"+conversationData.get(position).getIsRead());
//        Log.i(TAG,"mData.getIsFailedStatus()-:"+conversationData.get(position).getIsFailedStatus());
//        final boolean isSelected = mClivHostInterface.isConversationSelected(mData.getConversationId());

    }

    @Override
    public void bindViewHolder_message(ConversationListViewHolder holder, Context context, Cursor cursor, int position, List<ConversationMessageData> conversationListItemDataList) {

    }

    String avatarType;
    int[] photos = new int[]{R.drawable.man};
    @Override
    public void bindViewHolder_list(ConversationListViewHolder holder, Context context, Cursor cursor, int position, List<ConversationListItemData> conversationListItemDataList) {
        Log.i("bbb","position--:"+position);
        Log.i("bbb","position--:"+conversationListItemDataList.size());
        if(position == cursor.getCount()){
            return;
        }
        mData.bind(cursor);
        conversationData = conversationListItemDataList;
        Uri iconUri = null;
        if (mData.getIcon() != null) {
            iconUri = Uri.parse(mData.getIcon());
           // avatarType = AvatarUriUtil.getAvatarType(iconUri);
            Log.i("bbb","iconUri--:"+iconUri);
        }
//        if (AvatarUriUtil.TYPE_DEFAULT_URI.equals(avatarType)) {
//            Log.i("bbb","进来了--:");
//            Drawable draPhoto = mContext.getResources().getDrawable(photos[0]);
//            holder.photo.setBackground(draPhoto);
//        }else {
        holder.photo.setImageResourceUri(iconUri, mData.getParticipantContactId(),
                mData.getParticipantLookupKey(), mData.getOtherParticipantNormalizedDestination());
//        }
        //内容
        Log.i(TAG,"getSnippetText-----:"+getSnippetText());
        if(null != getSnippetText()) {
            holder.content.setText(getSnippetText());
        }
        getTimeContext(holder);

        holder.name.setText(mData.getName());
        int color;
        if(null != BugleApplication.getInstance().getPosition() && BugleApplication.getInstance().getPosition() == position)
        {
            color = pressColor;
//            color = mListItemReadColor;
            Log.i(TAG,"进入-----:");
            holder.name.setTextColor(color);

        }else{
            Log.i(TAG,"正常进入-----:");
            if (mData.getIsRead() || mData.getShowDraft()) {
                color = mListItemReadColor;
            } else {
                color = mListItemUnreadColor;
            }
            holder.name.setTextColor(color);
        }

        if(mData.getIsFailedStatus()){
            Log.i(TAG,"发送失败-----:");
            holder.content.setTextColor(Color.RED);
        }
        Log.i(TAG,"mData.getIsRead()123-:"+mData.getIsRead());
        Log.i(TAG,"mData.getIsFailedStatus()123-:"+mData.getIsFailedStatus());
    }
    boolean isbegin = false;
    @Override
    public MessageAdapter.ConversationListViewHolder createViewHolder(Context context, ViewGroup parent, int viewType) {


        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view=mInflater.inflate(R.layout.act_conversitionlist_item,parent,false);
        if(viewType == 0){
            view.setBackgroundResource(R.drawable.act_list_item);
        }else if (viewType == conversationData.size()-1){
            view.setBackgroundResource(R.drawable.act_list_item_last);
        }else{
            view.setBackgroundResource(R.drawable.act_list_item_center);
        }
        ConversationListViewHolder holder=new ConversationListViewHolder(view);

        Log.i(TAG,"viewType---:"+viewType);

        if(onItemClickListener!=null){
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                   onItemClickListener.setOnClick(conversationData,viewType);
                   BugleApplication.getInstance().setPosition(viewType);
                }
            });
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    onItemClickListener.setOnClick(conversationData,viewType);
                    BugleApplication.getInstance().setPosition(viewType);
                    return false;
                }
            });
            holder.itemView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                    }
                    onItemClickListener.onItemRightClick(keyCode, holder.itemView, viewType);
                    return false;
                }
            });
            //焦点聚焦的操作逻辑
            holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                        if (b) {
                            Log.i(TAG, "viewType---:" + viewType);
                            Log.i(TAG, "mData.getIsRead()123-:" + conversationData.get(viewType).getIsRead());
                            Log.i(TAG, "mData.getIsFailedStatus()123-:" + conversationData.get(viewType).getIsFailedStatus());
                            if (conversationData.get(viewType).getIsRead() && !conversationData.get(viewType).getIsFailedStatus()) {
                                holder.name.setTextColor(Color.BLACK);
                                holder.time.setTextColor(Color.BLACK);
                                holder.content.setTextColor(Color.BLACK);

                                // ofFloatAnimator(holder.itemView, 1f, 1.02f);//放大
                            } else if (!conversationData.get(viewType).getIsRead() && !conversationData.get(viewType).getIsFailedStatus()) {
                                holder.time.setTextColor(Color.BLACK);
                                holder.content.setTextColor(Color.BLACK);
                            } else if (conversationData.get(viewType).getIsFailedStatus()) {
                                holder.time.setTextColor(Color.BLACK);
                                holder.name.setTextColor(Color.BLACK);
                            }
                        } else {
                            BugleApplication.getInstance().setPosition(null);
                            Log.i(TAG, "viewType---:" + viewType);
                            if (conversationData.get(viewType).getIsRead() && !conversationData.get(viewType).getIsFailedStatus()) {
                                holder.name.setTextColor(Color.WHITE);
                                holder.time.setTextColor(Color.WHITE);
                                holder.content.setTextColor(Color.WHITE);
                                // ofFloatAnimator(holder.itemView, 1.02f, 1f);//缩小
                            } else if (!conversationData.get(viewType).getIsRead() && !conversationData.get(viewType).getIsFailedStatus()) {
                                holder.time.setTextColor(Color.WHITE);
                                holder.content.setTextColor(Color.WHITE);
                            } else if (conversationData.get(viewType).getIsFailedStatus()) {
                                holder.time.setTextColor(Color.WHITE);
                                holder.name.setTextColor(Color.WHITE);
                            }

                        }


                }
            });
        }
        return  holder;
    }


    @Override
    public void onConversationListCursorUpdated(ConversationListData data, Cursor cursor) {

    }

    @Override
    public void setBlockedParticipantsAvailable(boolean blockedAvailable) {

    }

    /**
     * ViewHolder that holds a ConversationListItemView.
     */
    public static class ConversationListViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView content;
        TextView time;
        ContactIconView photo;
        private LinearLayout rootView;
        public ConversationListViewHolder(View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.peo_photo_contact);
            name = itemView.findViewById(R.id.name_list);
            time = itemView.findViewById(R.id.time_list);
            content = itemView.findViewById(R.id.content_list);
            rootView=itemView.findViewById(R.id.contacts_list);
        }
    }


    @Override
    public int getItemViewType(int position) {
        return position;
    }



    public void deleteData(int pos){
       // conversationData.remove(pos);
        notifyItemRemoved(pos);
    }

    /**
     * 放大效果
     * @param view
     * @param start
     * @param end
     */
    private void ofFloatAnimator(View view,float start,float end){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(200);//动画时间
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", start, end);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", start, end);
        animatorSet.setInterpolator(new DecelerateInterpolator());//插值器
        animatorSet.play(scaleX).with(scaleY);//组合动画,同时基于x和y轴放大
        animatorSet.start();
    }

    public interface OnItemClickListener {
        void setOnItemClick(int position, boolean isCheck);
        boolean setOnItemLongClick(int position);
        void setOnItemCheckedChanged(int position, boolean isCheck);
        void setOnClick(List<ConversationListItemData> conversationListItemData,int position);
        void onItemRightClick(int keyCode, View view, int position);

    }

    private MessageAdapter.OnItemClickListener onItemClickListener;

    public void setOnItemListener(MessageAdapter.OnItemClickListener onItemClickListener){
        this.onItemClickListener=onItemClickListener;
    }
    private String getSnippetText() {
        String snippetText = mData.getShowDraft() ?
                mData.getDraftSnippetText() : mData.getSnippetText();
        Log.i(TAG,"信息是-----"+snippetText);
        final String previewContentType = mData.getShowDraft() ?
                mData.getDraftPreviewContentType() : mData.getPreviewContentType();
        Resources resources = BugleApplication.getContext().getResources();
        if (TextUtils.isEmpty(snippetText)) {
            // Use the attachment type as a snippet so the preview doesn't look odd
            if (ContentType.isAudioType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_audio_clip);
            } else if (ContentType.isImageType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_picture);
            } else if (ContentType.isVideoType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_video);
            } else if (ContentType.isVCardType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_vcard);
            } else if (ContentType.isGeoType(previewContentType)) { // juphoon
                snippetText = resources.getString(R.string.conversation_list_snippet_geo);
            } else if (ContentType.isFileType(previewContentType)) { // juphoon
                snippetText = resources.getString(R.string.snippet_file);
            } else if (ContentType.isChatbotCard(previewContentType)) { // juphoon
                snippetText = resources.getString(R.string.snippet_public);
            }
        }
        if (!TextUtils.isEmpty(snippetText)) {
            if (RcsChatbotUtils.isGeoSms(snippetText)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_geo);
            }
        }
        return snippetText;
    }
    private void getTimeContext(ConversationListViewHolder holder) {

        if (mData.getIsFailedStatus()) {
        //    holder.time.setTextColor(resources.getColor(R.color.red));
            int failureMessageId = R.string.messagefailed;
//            if (mData.getIsMessageTypeOutgoing()) {
//                failureMessageId = MmsUtils.mapRawStatusToErrorResourceId(mData.getMessageStatus(),
//                        mData.getMessageRawTelephonyStatus());
//            }

            holder.content.setTextColor(resources.getColor(R.color.red));
            holder.content.setText(resources.getString(failureMessageId));
          //  holder.time.setText(resources.getString(failureMessageId));
            Log.i(TAG,"信息失败-----：");
        } else if (mData.getShowDraft()
                ||mData.getMessageStatus() == MessageData.BUGLE_STATUS_OUTGOING_DRAFT
                // also check for unknown status which we get because sometimes the conversation
                // row is left with a latest_message_id of a no longer existing message and
                // therefore the join values come back as null (or in this case zero).
                || mData.getMessageStatus() == MessageData.BUGLE_STATUS_UNKNOWN) {
            holder.time.setTextColor(mListItemReadColor);
            holder.time.setText(resources.getString(
                    R.string.conversation_list_item_view_draft_message));
        } else {
          //  holder.content.setTextColor(resources.getColor(R.color.red));
            final String formattedTimestamp = mData.getFormattedTimestamp();
            Log.i(TAG,"formattedTimestamp-----："+formattedTimestamp);
            if (mData.getIsSendRequested()) {
                holder.time.setText(R.string.message_status_sending);
            } else {
                holder.time.setText(formattedTimestamp);
            }
        }
        final String formattedTimestamp = mData.getFormattedTimestamp();
        holder.time.setText(formattedTimestamp);
    }
}





