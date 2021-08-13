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
package com.android.messaging.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.PendingAttachmentData;
import com.android.messaging.datamodel.data.PersonItemData;
import com.android.messaging.datamodel.data.VCardContactItemData;
import com.android.messaging.datamodel.media.FileImageRequestDescriptor;
import com.android.messaging.datamodel.media.ImageRequest;
import com.android.messaging.datamodel.media.ImageRequestDescriptor;
import com.android.messaging.datamodel.media.UriImageRequestDescriptor;
import com.android.messaging.ui.MultiAttachmentLayout.OnAttachmentClickListener;
import com.android.messaging.ui.PersonItemView.PersonItemViewListener;
import com.android.messaging.ui.mediapicker.GalleryGridItemView;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.ImageUtils;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;
import com.juphoon.helper.mms.ui.RcsFileAttachmentView;

/**
 * A view factory that creates previews for single/multiple attachments.
 */
public class AttachmentPreviewFactory {
    /** Standalone attachment preview */
    public static final int TYPE_SINGLE = 1;

    /** Attachment preview displayed in a multi-attachment layout */
    public static final int TYPE_MULTIPLE = 2;

    /** Attachment preview displayed in the attachment chooser grid view */
    public static final int TYPE_CHOOSER_GRID = 3;

    public static View createAttachmentPreview(final LayoutInflater layoutInflater,
            final MessagePartData attachmentData, final ViewGroup parent,
            final int viewType, final boolean startImageRequest,
            @Nullable final OnAttachmentClickListener clickListener) {
        final String contentType = attachmentData.getContentType();
        View attachmentView = null;
        if (attachmentData instanceof PendingAttachmentData) {
            attachmentView = createPendingAttachmentPreview(layoutInflater, parent,
                    (PendingAttachmentData) attachmentData);
        } else if (ContentType.isImageType(contentType)) {
            attachmentView = createImagePreview(layoutInflater, attachmentData, parent, viewType,
                    startImageRequest);
        } else if (ContentType.isAudioType(contentType)) {
            attachmentView = createAudioPreview(layoutInflater, attachmentData, parent, viewType);
        } else if (ContentType.isVideoType(contentType)) {
            attachmentView = createVideoPreview(layoutInflater, attachmentData, parent, viewType);
        } else if (ContentType.isVCardType(contentType)) {
            attachmentView = createVCardPreview(layoutInflater, attachmentData, parent, viewType);
        } else if(ContentType.isGeoType(contentType)){ // juphoon
            attachmentView = createGeoPreview(layoutInflater, attachmentData, parent, viewType);
        } else if (ContentType.isFileType(contentType)) { //juphoon
            attachmentView = createFilePreview(layoutInflater, attachmentData, parent, viewType);
        } else {
            Assert.fail("unsupported attachment type: " + contentType);
            return null;
        }

        // Some views have a caption, set the text/visibility if one exists
        final TextView captionView = (TextView) attachmentView.findViewById(R.id.caption);
        if (captionView != null) {
            final String caption = attachmentData.getText();
            captionView.setVisibility(TextUtils.isEmpty(caption) ? View.GONE : View.VISIBLE);
            captionView.setText(caption);
        }

        if (attachmentView != null) {
            attachmentView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        Log.i("nnnn","clickListener------start");
                        final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
                        clickListener.onAttachmentClick(attachmentData, bounds,
                                false /* longPress */);
                    }
                });
            attachmentView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(final View view) {
                        final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
                        return clickListener.onAttachmentClick(attachmentData, bounds,
                                true /* longPress */);
                    }
                });
        }
        return attachmentView;
    }

    public static MultiAttachmentLayout createMultiplePreview(final Context context,
            final OnAttachmentClickListener listener) {
        final MultiAttachmentLayout multiAttachmentLayout =
                new MultiAttachmentLayout(context, null);
        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        multiAttachmentLayout.setLayoutParams(layoutParams);
        multiAttachmentLayout.setOnAttachmentClickListener(listener);
        return multiAttachmentLayout;
    }

    public static ImageRequestDescriptor getImageRequestDescriptorForAttachment(
            final MessagePartData attachmentData, final int desiredWidth, final int desiredHeight) {
        final Uri uri = attachmentData.getContentUri();
        final String contentType = attachmentData.getContentType();
        if (ContentType.isImageType(contentType)) {
            final String filePath = UriUtil.getFilePathFromUri(uri);
            if (filePath != null) {
                return new FileImageRequestDescriptor(filePath, desiredWidth, desiredHeight,
                        attachmentData.getWidth(), attachmentData.getHeight(),
                        false /* canUseThumbnail */, true /* allowCompression */,
                        false /* isStatic */);
            } else {
                return new UriImageRequestDescriptor(uri, desiredWidth, desiredHeight,
                        attachmentData.getWidth(), attachmentData.getHeight(),
                        true /* allowCompression */, false /* isStatic */, false /*cropToCircle*/,
                        ImageUtils.DEFAULT_CIRCLE_BACKGROUND_COLOR /* circleBackgroundColor */,
                        ImageUtils.DEFAULT_CIRCLE_STROKE_COLOR /* circleStrokeColor */);
            }
        }
        return null;
    }

    private static View createImagePreview(final LayoutInflater layoutInflater,
            final MessagePartData attachmentData, final ViewGroup parent,
            final int viewType, final boolean startImageRequest) {
        int layoutId = R.layout.attachment_single_image;
        switch (viewType) {
            case AttachmentPreviewFactory.TYPE_SINGLE:
                layoutId = R.layout.attachment_single_image;
                break;
            case AttachmentPreviewFactory.TYPE_MULTIPLE:
                layoutId = R.layout.attachment_multiple_image;
                break;
            case AttachmentPreviewFactory.TYPE_CHOOSER_GRID:
                layoutId = R.layout.attachment_chooser_image;
                break;
            default:
                Assert.fail("unsupported attachment view type!");
                break;
        }

        final View view = layoutInflater.inflate(layoutId, parent, false /* attachToRoot */);
        if(layoutId == R.layout.attachment_multiple_image){
            FrameLayout asyn_gallery;
            asyn_gallery =view.findViewById(R.id.asyn_gallery);
            asyn_gallery.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        ofFloatAnimator(asyn_gallery, 1f, 1.02f);//放大
                    }else{
                        ofFloatAnimator(asyn_gallery, 1.02f, 1f);//放大
                    }
                }
            });
        }
        if(layoutId == R.layout.attachment_single_image){
            LinearLayout pictures = view.findViewById(R.id.pictures);
            pictures.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        ofFloatAnimator(pictures, 1f, 1.02f);//放大
                    }else{
                        ofFloatAnimator(pictures, 1.02f, 1f);//放大
                    }
                }
            });
        }
        final AsyncImageView imageView = (AsyncImageView) view.findViewById(
                R.id.attachment_image_view);
        int maxWidth = imageView.getMaxWidth();
        int maxHeight = imageView.getMaxHeight();
        if (viewType == TYPE_CHOOSER_GRID) {
            final Resources resources = layoutInflater.getContext().getResources();
            maxWidth = maxHeight = resources.getDimensionPixelSize(
                    R.dimen.attachment_grid_image_cell_size);
        }
        if (maxWidth <= 0 || maxWidth == Integer.MAX_VALUE) {
            maxWidth = ImageRequest.UNSPECIFIED_SIZE;
        }

        if (maxHeight <= 0 || maxHeight == Integer.MAX_VALUE) {
            maxHeight = ImageRequest.UNSPECIFIED_SIZE;
        }
        Log.i("cvvv","startImageRequest");
//        if (startImageRequest) {
            Log.i("cvvv","startImageRequest---1");
            imageView.setImageResourceId(getImageRequestDescriptorForAttachment(attachmentData,
                    maxWidth, maxHeight));
//        }

        imageView.setContentDescription(
                parent.getResources().getString(R.string.message_image_content_description));
        return view;
    }
    private static void ofFloatAnimator(View view, float start, float end){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(200);//动画时间
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", start, end);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", start, end);
        animatorSet.setInterpolator(new DecelerateInterpolator());//插值器
        animatorSet.play(scaleX).with(scaleY);//组合动画,同时基于x和y轴放大
        animatorSet.start();
    }
    private static View createPendingAttachmentPreview(final LayoutInflater layoutInflater,
            final ViewGroup parent, final PendingAttachmentData attachmentData) {
        final View pendingItemView = layoutInflater.inflate(R.layout.attachment_pending_item,
                parent, false);
        final ImageView imageView = (ImageView)
                pendingItemView.findViewById(R.id.pending_item_view);
        final ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        final int defaultSize = layoutInflater.getContext().getResources().getDimensionPixelSize(
                R.dimen.pending_attachment_size);
        layoutParams.width = attachmentData.getWidth() == MessagePartData.UNSPECIFIED_SIZE ?
                defaultSize : attachmentData.getWidth();
        layoutParams.height = attachmentData.getHeight() == MessagePartData.UNSPECIFIED_SIZE ?
                defaultSize : attachmentData.getHeight();
        return pendingItemView;
    }

    private static View createVCardPreview(final LayoutInflater layoutInflater,
            final MessagePartData attachmentData, final ViewGroup parent,
            final int viewType) {
        int layoutId = R.layout.attachment_single_vcard;
        switch (viewType) {
            case AttachmentPreviewFactory.TYPE_SINGLE:
                layoutId = R.layout.attachment_single_vcard;
                break;
            case AttachmentPreviewFactory.TYPE_MULTIPLE:
                layoutId = R.layout.attachment_multiple_vcard;
                break;
            case AttachmentPreviewFactory.TYPE_CHOOSER_GRID:
                layoutId = R.layout.attachment_chooser_vcard;
                break;
            default:
                Assert.fail("unsupported attachment view type!");
                break;
        }
        final View view = layoutInflater.inflate(layoutId, parent, false /* attachToRoot */);
        final PersonItemView vcardPreview = (PersonItemView) view.findViewById(
                R.id.vcard_attachment_view);
        vcardPreview.setAvatarOnly(viewType != AttachmentPreviewFactory.TYPE_SINGLE);
        vcardPreview.bind(DataModel.get().createVCardContactItemData(layoutInflater.getContext(),
                attachmentData));
        vcardPreview.setListener(new PersonItemViewListener() {
            @Override
            public void onPersonClicked(final PersonItemData data) {
                Assert.isTrue(data instanceof VCardContactItemData);
                final VCardContactItemData vCardData = (VCardContactItemData) data;
                if (vCardData.hasValidVCard()) {
                    final Uri vCardUri = vCardData.getVCardUri();
                    UIIntents.get().launchVCardDetailActivity(vcardPreview.getContext(), vCardUri);
                }
            }

            @Override
            public boolean onPersonLongClicked(final PersonItemData data) {
                return false;
            }
        });
        return view;
    }
    private static Integer count =0 ;
    private static View createAudioPreview(final LayoutInflater layoutInflater,
                final MessagePartData attachmentData, final ViewGroup parent,
                final int viewType) {
        int layoutId = R.layout.attachment_single_audio;
        switch (viewType) {
            case AttachmentPreviewFactory.TYPE_SINGLE:
                layoutId = R.layout.attachment_single_audio;
                break;
            case AttachmentPreviewFactory.TYPE_MULTIPLE:
                layoutId = R.layout.attachment_multiple_audio;
                break;
            case AttachmentPreviewFactory.TYPE_CHOOSER_GRID:
                layoutId = R.layout.attachment_chooser_audio;
                break;
            default:
                Assert.fail("unsupported attachment view type!");
                break;
        }
        final View view = layoutInflater.inflate(layoutId, parent, false /* attachToRoot */);
        if(layoutId == R.layout.attachment_single_audio ){
            FrameLayout audio_attachment_background = view.findViewById(R.id.audio_attachment_background);
            AudioAttachmentPlayPauseButton mPlayPauseButton = audio_attachment_background.findViewById(R.id.play_pause_button);
            audio_attachment_background.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Log.i("mnb","  setOnFocusChangeListener.hasFocus();"+hasFocus);
                    if(hasFocus){
                        ofFloatAnimator(audio_attachment_background, 1f, 1.02f);//放大
                    }else{
                        ofFloatAnimator(audio_attachment_background, 1.02f, 1f);//放大
                    }
                }
            });
            audio_attachment_background.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){

                        if(count % 2 ==0){
                            Log.i("mnb","  mPlayPauseButton.performClick()");
                            mPlayPauseButton.performClick();
                            count = 1;
                        }else{
                            count = 0;
                        }
                        return true;
                    }
                    return false;
                }
            });
//            audio_attachment_background.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Log.i("mnb","  mPlayPauseButton.performClick();");
//
//                    mPlayPauseButton.performClick();
//                }
//            });
        }
        final AudioAttachmentView audioView = (AudioAttachmentView)
                view.findViewById(R.id.audio_attachment_view);
        audioView.bindMessagePartData(
                attachmentData, false /* incoming */, false /* showAsSelected */);
        return view;
    }

    private static View createVideoPreview(final LayoutInflater layoutInflater,
            final MessagePartData attachmentData, final ViewGroup parent,
            final int viewType) {
        int layoutId = R.layout.attachment_single_video;
        switch (viewType) {
            case AttachmentPreviewFactory.TYPE_SINGLE:
                layoutId = R.layout.attachment_single_video;
                break;
            case AttachmentPreviewFactory.TYPE_MULTIPLE:
                layoutId = R.layout.attachment_multiple_video;
                break;
            case AttachmentPreviewFactory.TYPE_CHOOSER_GRID:
                layoutId = R.layout.attachment_chooser_video;
                break;
            default:
                Assert.fail("unsupported attachment view type!");
                break;
        }
        final View view = layoutInflater.inflate(layoutId, parent, false /* attachToRoot */);
        if(layoutId == R.layout.attachment_single_video){
            FrameLayout video_attachment_background = view.findViewById(R.id.video_attachment_background);
            video_attachment_background.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        ofFloatAnimator(video_attachment_background, 1f, 1.02f);//放大
                    }else{
                        ofFloatAnimator(video_attachment_background, 1.02f, 1f);//放大
                    }
                }
            });

        }
//        final VideoThumbnailView videoThumbnail = (VideoThumbnailView) layoutInflater.inflate(
//                layoutId, parent, false /* attachToRoot */);
        final VideoThumbnailView videoThumbnail = view.findViewById(R.id.attachment_video_thumbnail);

        videoThumbnail.setSource(attachmentData, false /* incomingMessage */);
        return view;
    }
    

    // juphoon
    private static View createGeoPreview(final LayoutInflater layoutInflater, final MessagePartData attachmentData, final ViewGroup parent, final int viewType) {
        int layoutId = R.layout.attachment_single_geo;
        switch (viewType) {
            case AttachmentPreviewFactory.TYPE_SINGLE:
                layoutId = R.layout.attachment_single_geo;
                break;
            default:
                break;
        }
        final View view = layoutInflater.inflate(layoutId, parent, false /* attachToRoot */);
        LinearLayout geo_attachment_background = view.findViewById(R.id.geo_attachment_background);
        geo_attachment_background.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    ofFloatAnimator(geo_attachment_background, 1f, 1.02f);//放大
                }else{
                    ofFloatAnimator(geo_attachment_background, 1.02f, 1f);//放大
                }
            }
        });
        final GeoAttachmentView geoView = (GeoAttachmentView) view.findViewById(R.id.geo_attachment_view);
        geoView.bindMessagePartData(attachmentData, true);
        return view;
    }

    // juphoon
    private static View createFilePreview(final LayoutInflater layoutInflater, final MessagePartData attachmentData, final ViewGroup parent, final int viewType) {
        int layoutId = R.layout.attachment_single_file;
        switch (viewType) {
        case AttachmentPreviewFactory.TYPE_SINGLE:
            layoutId = R.layout.attachment_single_file;
            break;
        default:
            break;
        }
        final View view = layoutInflater.inflate(layoutId, parent, false /* attachToRoot */);
        final RcsFileAttachmentView fileView = (RcsFileAttachmentView) view.findViewById(R.id.file_attachment_view);
        fileView.bindMessagePartData(attachmentData, null);
        return view;
    }
}
