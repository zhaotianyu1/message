package com.android.messaging.tcl.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.ContactListItemData;
import com.android.messaging.ui.contact.ContactListAdapter;
import com.android.messaging.ui.contact.ContactListItemView;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>{

    final List<ContactListItemData> contactListItemData = new ArrayList<>();
    ContactListItemData contactListItemDatas;
    private List<Contact> mData = new ArrayList<>();//联系人实体列表对象
    private ContactListItemView.HostInterface mHostInterface;

    String [] names = {"13061410029","18802670085","14704778428"};
    String [] discrp = {"刘欣","张三","李四"};
    public void init(){
        for (int i = 0; i < 3; i++) {
            String name = names[i];
            String dis = discrp[i];
            mData.add(new Contact(name,dis));
        }
    }

    private Context mContext;
    //构造函数，初始化实体列表
    public ContactsAdapter(final Context context, final Cursor cursor,
                          final ContactListItemView.HostInterface clivHostInterface,
                           final boolean needAlphabetHeade){
        this.mContext = context;
        this.mHostInterface = clivHostInterface;
        init();
        Log.i("cxz","--------ContactsAdapter--------:");
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.i("cxz","--------onCreateViewHolder--------:");
        View view = LayoutInflater.from(mContext).inflate(R.layout.act_contact_list, parent, false);
        if(viewType == 0){
            view.setBackgroundResource(R.drawable.act_list_item);
        }else if (viewType == contactListItemData.size()-1){
            view.setBackgroundResource(R.drawable.act_list_item_last);
        }else{
            view.setBackgroundResource(R.drawable.act_list_item_center);
        }
        ContactViewHolder viewHolder = new ContactViewHolder(view);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("cxz","position-------:"+viewType);
                Log.i("cxz","--------contactListItemData.size---:"+contactListItemData.size());
                mHostInterface.onContactListItemClicked(contactListItemData.get(viewType), null);
            }
        });
        viewHolder.itemView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("cxz","onTouch-------:");
                mHostInterface.onContactListItemClicked(contactListItemData.get(viewType), null);
                return false;
            }
        });
        viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    viewHolder.name.setTextColor(Color.BLACK);
                    viewHolder.contact_details.setTextColor(Color.BLACK);
                }else{
                    viewHolder.name.setTextColor(Color.WHITE);
                    viewHolder.contact_details.setTextColor(Color.WHITE);
                }
            }
        });
        viewHolder.itemView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if(viewType == 0){
                        viewHolder.itemView.setFocusable(true);
                        viewHolder.itemView.requestFocus();
                    }
                }
                return false;
            }
        });
       return viewHolder;
    }
    int[] photos = new int[]{R.drawable.man};

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Log.i("cxz","--------onBindViewHolder--------:"+position);
        contactListItemDatas = new ContactListItemData();
        contactListItemDatas.binds(null,null,position);
        contactListItemData.add(contactListItemDatas);
        Drawable draPhoto = BugleApplication.getContext().getResources().getDrawable(photos[0]);
        holder.mContactIconView.setBackground(draPhoto);
        holder.name.setText(mData.get(position).getDisplayName());
        holder.contact_details.setText(mData.get(position).getDestination());

    }

    @Override
    public int getItemCount() {
        Log.i("cxz","--------getItemCount--------:"+mData.size());
        return mData == null ? 0 : mData.size();
    }

    class ContactViewHolder extends  RecyclerView.ViewHolder {

        TextView name;
        ImageView photo;
        TextView contact_details;
        TextView contact_detail_type;
//        ImageView contact_checkmark;
        ImageView mContactIconView;
        public ContactViewHolder(@NonNull View itemView) {

            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            contact_details = itemView.findViewById(R.id.contact_details);
            contact_detail_type = itemView.findViewById(R.id.contact_detail_type);

            mContactIconView = itemView.findViewById(R.id.contact_iconsss);
        }
    }
}
