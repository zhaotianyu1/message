package com.juphoon.helper.mms.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.messaging.R;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.util.AvatarUriUtil;
import com.juphoon.helper.mms.ui.RcsChooseFragment.Contact;

import java.util.List;


public class RcsBottomMemberListAdapter extends RecyclerView.Adapter<RcsBottomMemberListAdapter.ViewHolder> {

    private LayoutInflater mInflater;
    private List<Contact> mAddList;

    public RcsBottomMemberListAdapter(Context context, List<Contact> datats) {
        mInflater = LayoutInflater.from(context);
        mAddList = datats;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View arg0) {
            super(arg0);
        }

        ContactIconView mImg;
        TextView mName;
        ImageButton mRemoveMember;
    }

    public interface OnItemClickLitener {
        void onItemClick(View view, int position);
    }

    private OnItemClickLitener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public int getItemCount() {
        return mAddList.size();
    }

    /**
     * 创建ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.member_list_item_view, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.mImg = (ContactIconView) view.findViewById(R.id.rcsgroup_member_photo);
        viewHolder.mName = (TextView) view.findViewById(R.id.tv_member_name);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        Contact contact = mAddList.get(position);
        viewHolder.mName.setText(TextUtils.isEmpty(contact.mName) ? contact.mPhone : contact.mName);
        viewHolder.mImg.setImageResourceUri(AvatarUriUtil.createAvatarUri(null, contact.mName, null, null),
                contact.mId, null, null);
        viewHolder.mImg.setClickable(false);
        if (mOnItemClickLitener != null) {
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(viewHolder.itemView, position);
                }
            });

        }
    }

}