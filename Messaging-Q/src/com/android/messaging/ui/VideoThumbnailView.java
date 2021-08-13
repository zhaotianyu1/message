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

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.media.ImageRequest;
import com.android.messaging.datamodel.media.MessagePartImageRequestDescriptor;
import com.android.messaging.datamodel.media.MessagePartVideoThumbnailRequestDescriptor;
import com.android.messaging.datamodel.media.VideoThumbnailRequest;
import com.android.messaging.tcl.VideoActivity;
import com.android.messaging.util.Assert;
import com.juphoon.chatbotmaap.view.RoundImageView;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.RcsMsgItemTouchHelper;
import com.juphoon.helper.mms.RcsTransProgressManager;


/**
 * View that encapsulates a video preview (either as a thumbnail image, or video player), and the
 * a play button to overlay it.  Ensures that the video preview maintains the aspect ratio of the
 * original video while trying to respect minimum width/height and constraining to the available
 * bounds
 */
public class VideoThumbnailView extends FrameLayout {
    /**
     * When in this mode the VideoThumbnailView is a lightweight AsyncImageView with an ImageButton
     * to play the video.  Clicking play will launch a full screen player
     */
    private static final int MODE_IMAGE_THUMBNAIL = 0;

    /**
     * When in this mode the VideoThumbnailVideo will include a VideoView, and the play button will
     * play the video inline.  When in this mode, the loop and playOnLoad attributes can be applied
     * to auto-play or loop the video.
     */
    private static final int MODE_PLAYABLE_VIDEO = 1;

    private final int mMode;
    private final boolean mPlayOnLoad;
    private final boolean mAllowCrop;
    private final VideoView mVideoView;
    private final ImageButton mPlayButton;
    private final AsyncImageView mThumbnailImage;
    private int mVideoWidth;
    private int mVideoHeight;
    private Uri mVideoSource;
    private boolean mAnimating;
    private boolean mVideoLoaded;
    // juphoon
    private ConversationMessageData mMessageData;
    private final RoundImageView mLoading;
    private final ImageButton mDownloadStatus;
    private final ImageButton mViewBackground;
    private final TextView mDownloadProgress;
    public static OnClickListener mTransEventClickListener ;

    public VideoThumbnailView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray typedAttributes =
                context.obtainStyledAttributes(attrs, R.styleable.VideoThumbnailView);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.video_thumbnail_view, this, true);

        mPlayOnLoad = typedAttributes.getBoolean(R.styleable.VideoThumbnailView_playOnLoad, false);
        final boolean loop =
                typedAttributes.getBoolean(R.styleable.VideoThumbnailView_loop, false);
        mMode = typedAttributes.getInt(R.styleable.VideoThumbnailView_mode, MODE_IMAGE_THUMBNAIL);
        mAllowCrop = typedAttributes.getBoolean(R.styleable.VideoThumbnailView_allowCrop, false);

		//juphoon
        mVideoWidth = RcsMmsUtils.dip2px(context, getResources().getDimension(R.dimen.img_video_width));
        mVideoHeight = RcsMmsUtils.dip2px(context, getResources().getDimension(R.dimen.img_video_height));

        if (mMode == MODE_PLAYABLE_VIDEO) {
            mVideoView = new VideoView(context);
            // Video view tries to request focus on start which pulls focus from the user's intended
            // focus when we add this control.  Remove focusability to prevent this.  The play
            // button can still be focused
            mVideoView.setFocusable(false);
            mVideoView.setFocusableInTouchMode(false);
            mVideoView.clearFocus();
            addView(mVideoView, 0, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mediaPlayer) {
                    mVideoLoaded = true;
					//juphoon
//                    mVideoWidth = mediaPlayer.getVideoWidth();
//                    mVideoHeight = mediaPlayer.getVideoHeight();
                    mediaPlayer.setLooping(loop);
                    trySwitchToVideo();
                }
            });
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mediaPlayer) {
                    mPlayButton.setVisibility(View.VISIBLE);
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(final MediaPlayer mediaPlayer, final int i, final int i2) {
                    return true;
                }
            });
        } else {
            mVideoView = null;
        }

        mPlayButton = (ImageButton) findViewById(R.id.video_thumbnail_play_button);
		//juphoon
        mLoading = (RoundImageView) findViewById(R.id.video_downloading);
        mDownloadStatus = (ImageButton) findViewById(R.id.video_download_status);
        mDownloadProgress = (TextView) findViewById(R.id.video_downloading_progress);
        mViewBackground = (ImageButton) findViewById(R.id.video_thumbnail_background);

        mTransEventClickListener = new OnClickListener() {
            @Override
            public void onClick(final View view) {
                // juphoon 点击下载
                if (mMessageData != null) {
                    int msgType = mMessageData.getProtocol();
                    int msgStatus = mMessageData.getStatus();
                    String msgUri = mMessageData.getSmsMessageUri();
                    if (msgType == MessageData.PROTOCOL_RMS) {
                        if (!(msgStatus == MessageData.BUGLE_STATUS_INCOMING_COMPLETE || msgStatus == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
                                || msgStatus == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED
                                || msgStatus == MessageData.BUGLE_STATUS_OUTGOING_DISPLAYED)) {
                            RcsMsgItemTouchHelper.dealFailMessgeTask(msgUri, mMessageData.getMessageId());
                            if (msgStatus == MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING || msgStatus == MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING
                                    || msgStatus == MessageData.BUGLE_STATUS_OUTGOING_SENDING || msgStatus == MessageData.BUGLE_STATUS_OUTGOING_RESENDING) {
                                mDownloadStatus.setVisibility(View.VISIBLE);
                                mDownloadProgress.setVisibility(View.INVISIBLE);
                            } else {
                                mDownloadStatus.setVisibility(View.INVISIBLE);
                                mDownloadProgress.setText("");
                                mDownloadProgress.setVisibility(View.VISIBLE);
                            }
                            return;
                        }
                    }
                }

                if (mVideoSource == null) {
                    return;
                }

                if (mMode == MODE_PLAYABLE_VIDEO) {
                    mVideoView.seekTo(0);
                    start();
                } else {
                    // juphoon
                    if (mMessageData != null && mMessageData.getProtocol() == MessageData.PROTOCOL_RMS) {
//                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = RcsMmsUtils.getVideoContentUri(context, mVideoSource.getPath());
//                        intent.setDataAndType(uri, androidx.appcompat.mms.pdu.ContentType.VIDEO_UNSPECIFIED);
//                        context.startActivity(intent);
                        Intent intent = new Intent(context, TestMovieActivity.class);
                        intent.putExtra("uri", uri.toString());
                        context.startActivity(intent);
                    } else {
                        UIIntents.get().launchFullScreenVideoViewer(getContext(), mVideoSource);
                    }
                }
            }
        };
        if (loop) {
            mPlayButton.setVisibility(View.GONE);
        } else {
            mViewBackground.setOnClickListener(mTransEventClickListener);
            mPlayButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(final View view) {
                    // Button prevents long click from propagating up, do it manually
                    VideoThumbnailView.this.performLongClick();
                    return true;
                }
            });
        }

        mThumbnailImage = (AsyncImageView) findViewById(R.id.video_thumbnail_image);

        if (mAllowCrop) {
            mThumbnailImage.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mThumbnailImage.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            mThumbnailImage.setScaleType(ScaleType.CENTER_CROP);
        } else {
            // This is the default setting in the layout, so No-op.
        }
        final int maxHeight = typedAttributes.getDimensionPixelSize(
                R.styleable.VideoThumbnailView_android_maxHeight, ImageRequest.UNSPECIFIED_SIZE);
        if (maxHeight != ImageRequest.UNSPECIFIED_SIZE) {
            mThumbnailImage.setMaxHeight(maxHeight);
            mThumbnailImage.setAdjustViewBounds(true);
        }

        typedAttributes.recycle();
    }

    @Override
    protected void onAnimationStart() {
        super.onAnimationStart();
        mAnimating = true;
    }

    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        mAnimating = false;
        trySwitchToVideo();
    }

    private void trySwitchToVideo() {
        if (mAnimating) {
            // Don't start video or hide image until after animation completes
            return;
        }

        if (!mVideoLoaded) {
            // Video hasn't loaded, nothing more to do
            return;
        }

        if (mPlayOnLoad) {
            start();
        } else {
            mVideoView.seekTo(0);
        }
    }

    private boolean hasVideoSize() {
        return mVideoWidth != ImageRequest.UNSPECIFIED_SIZE &&
                mVideoHeight != ImageRequest.UNSPECIFIED_SIZE;
    }

    public void start() {
        Assert.equals(MODE_PLAYABLE_VIDEO, mMode);
        mPlayButton.setVisibility(View.GONE);
        mThumbnailImage.setVisibility(View.GONE);
        mVideoView.start();
    }

    // TODO: The check could be added to MessagePartData itself so that all users of MessagePartData
    // get the right behavior, instead of requiring all the users to do similar checks.
    private static boolean shouldUseGenericVideoIcon(final boolean incomingMessage) {
        return incomingMessage && !VideoThumbnailRequest.shouldShowIncomingVideoThumbnails();
    }

    public void setSource(final MessagePartData part, final boolean incomingMessage) {
        if (part == null) {
            clearSource();
        } else {
            mVideoSource = part.getContentUri();
            if (shouldUseGenericVideoIcon(incomingMessage)) {
                mThumbnailImage.setImageResource(R.drawable.generic_video_icon);
				//juphoon
//                mVideoWidth = ImageRequest.UNSPECIFIED_SIZE;
//                mVideoHeight = ImageRequest.UNSPECIFIED_SIZE;
            } else {
			//juphoon
                mThumbnailImage.setScaleType(ScaleType.CENTER_CROP);
                mThumbnailImage.setImageResourceId(
                        new MessagePartVideoThumbnailRequestDescriptor(part));
                if (mVideoView != null) {
                    mVideoView.setVideoURI(mVideoSource);
                }
				//juphoon
//                mVideoWidth = part.getWidth();
//                mVideoHeight = part.getHeight();
            }
            // juphoon 为了发送预览处的播放按钮能显示
            mDownloadStatus.setVisibility(View.VISIBLE);
            mDownloadStatus.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_play_light));
        }
    }

    public void setSource(final Uri videoSource, final boolean incomingMessage) {
        if (videoSource == null) {
            clearSource();
        } else {
            mVideoSource = videoSource;
            if (shouldUseGenericVideoIcon(incomingMessage)) {
                mThumbnailImage.setImageResource(R.drawable.generic_video_icon);
				//juphoon
//                mVideoWidth = ImageRequest.UNSPECIFIED_SIZE;
//                mVideoHeight = ImageRequest.UNSPECIFIED_SIZE;
            } else {
                mThumbnailImage.setImageResourceId(
                        new MessagePartVideoThumbnailRequestDescriptor(videoSource));
                if (mVideoView != null) {
                    mVideoView.setVideoURI(videoSource);
                }
            }
        }
    }

    private void clearSource() {
        mVideoSource = null;
        mThumbnailImage.setImageResourceId(null);
		//juphoon
//        mVideoWidth = ImageRequest.UNSPECIFIED_SIZE;
//        mVideoHeight = ImageRequest.UNSPECIFIED_SIZE;
        if (mVideoView != null) {
            mVideoView.setVideoURI(null);
        }
    }

    @Override
    public void setMinimumWidth(final int minWidth) {
        super.setMinimumWidth(minWidth);
        if (mVideoView != null) {
            mVideoView.setMinimumWidth(minWidth);
        }
    }

    @Override
    public void setMinimumHeight(final int minHeight) {
        super.setMinimumHeight(minHeight);
        if (mVideoView != null) {
            mVideoView.setMinimumHeight(minHeight);
        }
    }

    public void setColorFilter(int color) {
        mThumbnailImage.setColorFilter(color);
        mPlayButton.setColorFilter(color);
    }

    public void clearColorFilter() {
        mThumbnailImage.clearColorFilter();
        mPlayButton.clearColorFilter();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mAllowCrop) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
		//juphoon
        int desiredWidth = RcsMmsUtils.dip2px(getContext(), getResources().getDimension(R.dimen.img_video_width));
        int desiredHeight = RcsMmsUtils.dip2px(getContext(), getResources().getDimension(R.dimen.img_video_height));
        if (mVideoView != null) {
            mVideoView.measure(widthMeasureSpec, heightMeasureSpec);
        }
        mThumbnailImage.measure(widthMeasureSpec, heightMeasureSpec);
		//juphoon
//        if (hasVideoSize()) {
//            desiredWidth = mVideoWidth;
//            desiredHeight = mVideoHeight;
//        } else {
//            desiredWidth = mThumbnailImage.getMeasuredWidth();
//            desiredHeight = mThumbnailImage.getMeasuredHeight();
//        }

        final int minimumWidth = getMinimumWidth();
        final int minimumHeight = getMinimumHeight();

        // Constrain the scale to fit within the supplied size
        final float maxScale = Math.max(
                MeasureSpec.getSize(widthMeasureSpec) / (float) desiredWidth,
                MeasureSpec.getSize(heightMeasureSpec) / (float) desiredHeight);

        // Scale up to reach minimum width/height
        final float widthScale = Math.max(1, minimumWidth / (float) desiredWidth);
        final float heightScale = Math.max(1, minimumHeight / (float) desiredHeight);
        final float scale = Math.min(maxScale, Math.max(widthScale, heightScale));
        desiredWidth = (int) (desiredWidth * scale);
        desiredHeight = (int) (desiredHeight * scale);

        setMeasuredDimension(desiredWidth, desiredHeight);
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child instanceof TextView) {
                // juphoon 设置进度的位置
                int width = RcsMmsUtils.dip2px(getContext(), 30);
                int height = RcsMmsUtils.dip2px(getContext(), 20);
                child.layout(left + (right - left) / 2 - width/2, top + (bottom - top) / 2 - height/2,
                        left + (right - left) / 2 + width/2, top + (bottom - top) / 2 + height/2);


            } else if (child instanceof RoundImageView) {
                // juphoon 设置传输中gif图的位置
                int width = RcsMmsUtils.dip2px(getContext(), 45);
                int height = RcsMmsUtils.dip2px(getContext(), 45);
                child.layout(left + (right - left) / 2 - width/2, top + (bottom - top) / 2 - height/2,
                        left + (right - left) / 2 + width/2, top + (bottom - top) / 2 + height/2);


            } else {
                child.layout(0, 0, right - left, bottom - top);
                
            }
        }
    }
    
    // juphoon
    public void setSource(ConversationMessageData data, final MessagePartData part, final boolean incomingMessage) {
        mMessageData = data;
        if (part == null || data == null) {
            clearSource();
        } else {
            mVideoSource = part.getContentUri();
            if (!incomingMessage) {
                setSource(part, incomingMessage);
            }
            if (data.getStatus() == MessageData.BUGLE_STATUS_INCOMING_COMPLETE
                    || data.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
                    ||data.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED) {
                mDownloadProgress.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                setSource(part, incomingMessage);
            } else {
//                if (shouldUseGenericVideoIcon(incomingMessage)) {
//                    mThumbnailImage.setImageResource(R.drawable.generic_video_icon);
////                    mVideoWidth = ImageRequest.UNSPECIFIED_SIZE;
////                    mVideoHeight = ImageRequest.UNSPECIFIED_SIZE;
//                } else {
                switch (data.getStatus()) {
                case MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
                case MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
                case MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
                case MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
                case MessageData.BUGLE_STATUS_OUTGOING_SENDING:
                case MessageData.BUGLE_STATUS_OUTGOING_RESENDING:
                    RcsTransProgressManager.TransProgress progress = RcsTransProgressManager.getTransProgress(data.getMessageId());
                    if (progress != null && progress.getMaxSize() > 0) {
                        mLoading.setVisibility(View.VISIBLE);
                        mDownloadStatus.setVisibility(View.INVISIBLE);
                        mDownloadProgress.setVisibility(View.VISIBLE);
                        mDownloadProgress.setGravity(Gravity.CENTER);
                        mDownloadProgress.setText(progress.getTransSize() * 100 / progress.getMaxSize() + "%");
                    }
                    break;
                default:
                    mDownloadProgress.setVisibility(View.INVISIBLE);
                    mLoading.setVisibility(View.INVISIBLE);
                    mDownloadStatus.setVisibility(View.VISIBLE);
                    mDownloadStatus.setImageDrawable(
                            incomingMessage ? getResources().getDrawable(R.drawable.icon_video_download)
                                    : getResources().getDrawable(R.drawable.icon_video_upload));
                    break;
                }
                if (mVideoSource.toString().startsWith("http")) {
                    RcsMsgItemTouchHelper.downloadPublicThumbToUpdate(Long.parseLong(data.getMessageId()),
                            part.getContentUri().toString());
                } else {
                    mThumbnailImage.setImageResourceId(new MessagePartImageRequestDescriptor(part,
                            MessagePartData.UNSPECIFIED_SIZE, MessagePartData.UNSPECIFIED_SIZE, false));
                }
                // }
            }
        }
    }

}
