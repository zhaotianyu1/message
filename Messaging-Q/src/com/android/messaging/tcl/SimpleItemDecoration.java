package com.android.messaging.tcl;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class SimpleItemDecoration extends RecyclerView.ItemDecoration {
    /**
     * @param outRect   全为0的rect，用来指定偏移区域
     * @param view      指RecyclerView中的Item
     * @param parent    指RecyclerView本身
     * @param state     状态
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (parent.getChildAdapterPosition(view) != 0) {
            //直接设置为1px
            outRect.top = 1;

        }
    }
}
