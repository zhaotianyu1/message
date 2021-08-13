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
package com.android.messaging.ui.mediapicker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.data.GalleryGridItemData;
import com.android.messaging.ui.AsyncImageView;
import com.android.messaging.ui.ConversationDrawables;
import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.TimeUnit;

/**
 * Shows an item in the gallery picker grid view. Hosts an FileImageView with a checkbox.
 */
public class GalleryGridItemView extends FrameLayout {
    /**
     * Implemented by the owner of this GalleryGridItemView instance to communicate on media
     * picking and selection events.
     */
    public interface HostInterface {
        void onItemClicked(View view, GalleryGridItemData data, boolean longClick);
        boolean isItemSelected(GalleryGridItemData data);
        boolean isMultiSelectEnabled();
    }

    GalleryGridItemView gallery;
    @VisibleForTesting
    GalleryGridItemData mData;
    private AsyncImageView mImageView;
    private CheckBox mCheckBox;
    private HostInterface mHostInterface;
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            Log.i("oneone","12321321----");
            if(mData ==null){

            }
            mHostInterface.onItemClicked(GalleryGridItemView.this, mData, false /*longClick*/);
        }
    };

    public GalleryGridItemView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mData = DataModel.get().createGalleryGridItemData();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        gallery = findViewById(R.id.gallery);
        mImageView = (AsyncImageView) findViewById(R.id.image);
        mCheckBox = (CheckBox) findViewById(R.id.checkbox);
        mCheckBox.setOnClickListener(mOnClickListener);
        setOnClickListener(mOnClickListener);
        final OnLongClickListener longClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                mHostInterface.onItemClicked(v, mData, true /* longClick */);
                return true;
            }
        };
        gallery.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.i("bnm","hasfucus:"+hasFocus);
                if(hasFocus){
                    ofFloatAnimator(gallery, 1f, 1.02f);//放大
                }else{
                    ofFloatAnimator(gallery, 1.02f, 1f);//缩小
                }
            }
        });
        setOnLongClickListener(longClickListener);
        mCheckBox.setOnLongClickListener(longClickListener);
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                // Enlarge the clickable region for the checkbox to fill the entire view.
                final Rect region = new Rect(0, 0, getWidth(), getHeight());
                setTouchDelegate(new TouchDelegate(region, mCheckBox) {
                    @Override
                    public boolean onTouchEvent(MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                setPressed(true);
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                setPressed(false);
                                break;
                        }
                        return super.onTouchEvent(event);
                    }
                });
            }
        });
    }
    private void ofFloatAnimator(View view,float start,float end){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(200);//动画时间
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", start, end);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", start, end);
        animatorSet.setInterpolator(new DecelerateInterpolator());//插值器
        animatorSet.play(scaleX).with(scaleY);//组合动画,同时基于x和y轴放大
        animatorSet.start();
    }
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        // The grid view auto-fit the columns, so we want to let the height match the width
        // to make the image square.
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public void bind(final Cursor cursor, final HostInterface hostInterface) {
        final int desiredSize = getResources()
                .getDimensionPixelSize(R.dimen.gallery_image_cell_size);
        Log.i("vcx","-------bind-");
        mData.bind(cursor, desiredSize, desiredSize);
        mHostInterface = hostInterface;
        updateViewState();
    }

    private void updateViewState() {
        updateImageView();
        if (mHostInterface.isMultiSelectEnabled() && !mData.isDocumentPickerItem()) {
            BugleApplication.getInstance().setMulit(true);
            mCheckBox.setVisibility(VISIBLE);
            mCheckBox.setClickable(true);
            mCheckBox.setChecked(mHostInterface.isItemSelected(mData));
        } else {
            mCheckBox.setVisibility(GONE);
            mCheckBox.setClickable(false);
        }
    }

    private void updateImageView() {
        if (mData.isDocumentPickerItem()) {
            Log.i("vcx","-------isDocumentPickerItem-");
//            mImageView.setVisibility(GONE);
//            gallery.setVisibility(GONE);
//            setVisibility(GONE);

//            mImageView.setScaleType(ScaleType.CENTER_CROP);
//            mImageView.setImageResourceId(null);
//            mImageView.setImageResource(R.drawable.image);
//            gallery.setFocusable(false);
//         //   gallery.setFocusableInTouchMode(false);
//            mImageView.setContentDescription(getResources().getString(
//                    R.string.pick_image_from_document_library_content_description));

            mImageView.setScaleType(ScaleType.CENTER);
//            setBackgroundColor(ConversationDrawables.get().getConversationThemeColor());
            mImageView.setImageResourceId(null);
            gallery.setFocusable(false);
            mImageView.setImageResource(R.drawable.image);
            mImageView.setContentDescription(getResources().getString(
                    R.string.pick_image_from_document_library_content_description));
            BugleApplication.getInstance().setIsblank(BugleApplication.getInstance().getIsblank()+1);
        } else {
            Log.i("vcx","-------true-");
            BugleApplication.getInstance().setIsblank(0);
            mImageView.setScaleType(ScaleType.CENTER_CROP );
            mImageView.setFocusable(true);
            mImageView.setFocusableInTouchMode(true);
            //setBackgroundColor(getResources().getColor(R.color.gallery_image_default_background));
            mImageView.setImageResourceId(mData.getImageRequestDescriptor());
            final long dateSeconds = mData.getDateSeconds();
            final boolean isValidDate = (dateSeconds > 0);
            final int templateId = isValidDate ?
                    R.string.mediapicker_gallery_image_item_description :
                    R.string.mediapicker_gallery_image_item_description_no_date;
            String contentDescription = String.format(getResources().getString(templateId),
                    dateSeconds * TimeUnit.SECONDS.toMillis(1));
            mImageView.setContentDescription(contentDescription);
        }
    }
}
