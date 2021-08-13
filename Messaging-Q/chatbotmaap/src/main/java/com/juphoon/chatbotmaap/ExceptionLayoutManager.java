package com.juphoon.chatbotmaap;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ExceptionLayoutManager extends LinearLayoutManager {
    private int[] mMeasuredDimension = new int[2];
    private int MaxHeight = 0;

    public ExceptionLayoutManager(Context context) {
        super(context);
        this.setAutoMeasureEnabled(false);
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return false;
    }

    @Override
    public void onMeasure(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, int widthSpec, int heightSpec) {
        super.onMeasure(recycler, state, widthSpec, heightSpec);
        for (int i = 0; i < state.getItemCount(); i++) {
            try {
                View view = recycler.getViewForPosition(i);
                measureScrapChild(view,
                        widthSpec,
                        heightSpec,
                        mMeasuredDimension);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        //绘制当前view宽高
        setMeasuredDimension(widthSpec, mMeasuredDimension[1] + 70);
    }

    private void measureScrapChild(View view, int widthSpec, int heightSpec, int[] measuredDimension) {
        if (view != null) {
            RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
            int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                    getPaddingTop() + getPaddingBottom(), p.height);
            view.measure(widthSpec, childHeightSpec);
            measuredDimension[0] = view.getMeasuredWidth() + p.leftMargin + p.rightMargin;
            measuredDimension[1] = Math.max(measuredDimension[1], view.getMeasuredHeight() + p.bottomMargin + p.topMargin);
            //  recycler.recycleView(view);
        }
    }
}
