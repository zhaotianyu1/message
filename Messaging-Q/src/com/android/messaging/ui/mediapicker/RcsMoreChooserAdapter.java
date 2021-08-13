package com.android.messaging.ui.mediapicker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.messaging.R;
import com.android.messaging.ui.mediapicker.RcsMoreChooser.MoreEntity;

import java.util.List;

//juphoon
public class RcsMoreChooserAdapter
        extends RecyclerView.Adapter<RcsMoreChooserAdapter.ViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<MoreEntity> mDatas;

    public RcsMoreChooserAdapter(Context context, List<MoreEntity> datats) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDatas = datats;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View arg0) {
            super(arg0);
        }

        ImageView mImg;
        TextView mText;
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
        viewHolder.mImg.setBackgroundResource(mDatas.get(i).getImgResId());
//        if (mDatas.get(i).getIsEnable() && mDatas.get(i).getIsSelect()) {
//            viewHolder.mImg.setColorFilter(mContext.getColor(R.color.background_item_selected));
//        } else if (mDatas.get(i).getIsEnable()) {
//            viewHolder.mImg.setColorFilter(mContext.getColor(R.color.primary_color));
//        } else {
//            viewHolder.mImg.setColorFilter(null);
//        }
        viewHolder.mText.setText(mDatas.get(i).getStrResId());
        viewHolder.itemView.setEnabled(mDatas.get(i).getIsEnable());
        if (mOnItemClickLitener != null) {
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(viewHolder.itemView, i);
                }
            });
        }
        viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.i("zxc","hasFocus--:"+hasFocus);
                if(hasFocus){
                    viewHolder.mImg.setImageResource(R.drawable.local_focus);
                }else{
                    viewHolder.mImg.setImageResource(R.drawable.local_unfocus);
                }
            }
        });

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.mediapicker_more_item_view,
                viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.mImg = (ImageView) view.findViewById(R.id.more_item_image);
        viewHolder.mText = (TextView) view.findViewById(R.id.more_item_text);
        return viewHolder;
    }

    /**
     * ItemClick的回调接口
     */
    public interface OnItemClickLitener {
        void onItemClick(View view, int position);
    }

    private OnItemClickLitener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickLitener onItemClickLitener) {
        this.mOnItemClickLitener = onItemClickLitener;
    }
}
