package com.juphoon.chatbotmaap;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.juphoon.chatbotmaap.tcl.VideoData;

import java.io.IOException;
import java.util.Map;

/**
 * 播放器
 */
public class RcsChatbotVideoPlayer extends FrameLayout
        implements RcsChatbotVideoPlayerInterface,
        TextureView.SurfaceTextureListener {


    /**
     * 播放错误
     **/
    public static final int STATE_ERROR = -1;
    /**
     * 播放未开始
     **/
    public static final int STATE_IDLE = 0;
    /**
     * 播放准备中
     **/
    public static final int STATE_PREPARING = 1;
    /**
     * 播放准备就绪
     **/
    public static final int STATE_PREPARED = 2;
    /**
     * 正在播放
     **/
    public static final int STATE_PLAYING = 3;
    /**
     * 暂停播放
     **/
    public static final int STATE_PAUSED = 4;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，缓冲区数据足够后恢复播放)
     **/
    public static final int STATE_BUFFERING_PLAYING = 5;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，此时暂停播放器，继续缓冲，缓冲区数据足够后恢复暂停
     **/
    public static final int STATE_BUFFERING_PAUSED = 6;
    /**
     * 播放完成
     **/
    public static final int STATE_COMPLETED = 7;

    /**
     * 普通模式
     **/
    public static final int MODE_NORMAL = 10;
    /**
     * 全屏模式
     **/
    public static final int MODE_FULL_SCREEN = 11;
    /**
     * 小窗口模式
     **/
    public static final int MODE_TINY_WINDOW = 12;

    /**
     * IjkPlayer
     **/
    public static final int TYPE_IJK = 111;
    /**
     * MediaPlayer
     **/
    public static final int TYPE_NATIVE = 222;

    private int mPlayerType = TYPE_NATIVE;
    private int mCurrentState = STATE_IDLE;
    private int mCurrentMode = MODE_NORMAL;

    private Context mContext;
    private AudioManager mAudioManager;
    private JuphoonMediaPlayer mMediaPlayer;
    private FrameLayout mContainer;
    private RcsChatbotTextureView mTextureView;
    private RcsChatbotVideoPlayerController mController;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private String mUrl;
    private Map<String, String> mHeaders;
    private int mBufferPercentage;

    public RcsChatbotVideoPlayer(Context context) {
        this(context, null);
    }

    public RcsChatbotVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mContainer = new FrameLayout(mContext);
        mContainer.setBackgroundColor(Color.BLACK);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);
    }

    public void setUp(String url, Map<String, String> headers) {
        mUrl = url;
        mHeaders = headers;
    }

    public void setController(RcsChatbotVideoPlayerController controller) {
        mContainer.removeView(mController);
        mController = controller;
        mController.reset();
        mController.setNiceVideoPlayer(this);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mController, params);
    }

    /**
     * 设置播放器类型
     *
     * @param playerType IjkPlayer or MediaPlayer.
     */
    public void setPlayerType(int playerType) {
        mPlayerType = playerType;
    }

    @Override
    public void setSpeed(float speed) {
    }

    @Override
    public void start() {
        Log.i("oye","mCurrentState---:"+mCurrentState);
        if (mCurrentState == STATE_IDLE) {
            RcsChatbotVideoPlayerManager.instance().setCurrentNiceVideoPlayer(this);
            initAudioManager();
            initMediaPlayer();
            initTextureView();
            addTextureView();
        } else {
            Log.d("oye", "NiceVideoPlayer只有在mCurrentState == STATE_IDLE时才能调用start方法.");
        }
    }

    @Override
    public void start(long position) {
        start();
    }

    @Override
    public void restart() {
        if (mCurrentState == STATE_PAUSED ) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            RcsChatbotCardView.videoData.setState(STATE_PLAYING);

            mController.onPlayStateChanged(mCurrentState);
            Log.d("oye", "STATE_PLAYING");
        } else if (mCurrentState == STATE_BUFFERING_PAUSED) {
            mMediaPlayer.start();
            mCurrentState = STATE_BUFFERING_PLAYING;
            RcsChatbotCardView.videoData.setState(STATE_BUFFERING_PLAYING);

            mController.onPlayStateChanged(mCurrentState);
            Log.d("oye", "STATE_BUFFERING_PLAYING");
        } else if (mCurrentState == STATE_COMPLETED || mCurrentState == STATE_ERROR) {
            mMediaPlayer.reset();
            openMediaPlayer();
        } else {
            Log.d("oye", "NiceVideoPlayer在mCurrentState == " + mCurrentState + "时不能调用restart()方法.");
        }
    }

    @Override
    public void pause() {
        if (mCurrentState == STATE_PLAYING) {
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
            mController.onPlayStateChanged(mCurrentState);


            RcsChatbotCardView.videoData.setState(STATE_PAUSED);
            Log.d("oye", "STATE_PAUSED");
        }
        if (mCurrentState == STATE_BUFFERING_PLAYING) {
            mMediaPlayer.pause();
            mCurrentState = STATE_BUFFERING_PAUSED;
            mController.onPlayStateChanged(mCurrentState);
            RcsChatbotCardView.videoData.setState(STATE_BUFFERING_PLAYING);

            Log.d("oye", "STATE_BUFFERING_PAUSED");
        }
    }

    @Override
    public void seekTo(long pos) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo((int) pos);
        }
    }

    @Override
    public void setVolume(int volume) {
        if (volume == 0) {
//            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            mMediaPlayer.setVolume(0f, 0f);
        } else {
            mMediaPlayer.setVolume(1f, 1f);
        }
    }

    @Override
    public boolean isIdle() {
        return mCurrentState == STATE_IDLE;
    }

    @Override
    public boolean isPreparing() {
        return mCurrentState == STATE_PREPARING;
    }

    @Override
    public boolean isPrepared() {
        return mCurrentState == STATE_PREPARED;
    }

    @Override
    public boolean isBufferingPlaying() {
        return mCurrentState == STATE_BUFFERING_PLAYING;
    }

    @Override
    public boolean isBufferingPaused() {
        return mCurrentState == STATE_BUFFERING_PAUSED;
    }

    @Override
    public boolean isPlaying() {
        return mCurrentState == STATE_PLAYING;
    }

    @Override
    public boolean isPaused() {
        return mCurrentState == STATE_PAUSED;
    }

    @Override
    public boolean isError() {
        return mCurrentState == STATE_ERROR;
    }

    @Override
    public boolean isCompleted() {
        return mCurrentState == STATE_COMPLETED;
    }

    @Override
    public boolean isFullScreen() {
        return mCurrentMode == MODE_FULL_SCREEN;
    }

    @Override
    public boolean isTinyWindow() {
        return mCurrentMode == MODE_TINY_WINDOW;
    }

    @Override
    public boolean isNormal() {
        return mCurrentMode == MODE_NORMAL;
    }

    @Override
    public int getMaxVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public int getVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer != null ? mMediaPlayer.getDuration() : 0;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    @Override
    public float getSpeed(float speed) {

        return 0;
    }

    @Override
    public long getTcpSpeed() {
        return 0;
    }

    private void initAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            switch (mPlayerType) {
                case TYPE_NATIVE:
                    mMediaPlayer = new JuphoonMediaPlayer();
                    break;
                case TYPE_IJK:
                default:
//                    ((IjkMediaPlayer)mMediaPlayer).setOption(1, "analyzemaxduration", 100L);
//                    ((IjkMediaPlayer)mMediaPlayer).setOption(1, "probesize", 10240L);
//                    ((IjkMediaPlayer)mMediaPlayer).setOption(1, "flush_packets", 1L);
//                    ((IjkMediaPlayer)mMediaPlayer).setOption(4, "packet-buffering", 0L);
//                    ((IjkMediaPlayer)mMediaPlayer).setOption(4, "framedrop", 1L);
                    break;
            }
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }

    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new RcsChatbotTextureView(mContext);
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    private void addTextureView() {
        mContainer.removeView(mTextureView);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        mContainer.addView(mTextureView, 0, params);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surfaceTexture;
            openMediaPlayer();
        } else {
            mTextureView.setSurfaceTexture(mSurfaceTexture);
        }
    }

    private void openMediaPlayer() {
        // 屏幕常亮
        mContainer.setKeepScreenOn(true);
        // 设置监听
        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
        mMediaPlayer.setOnInfoListener(mOnInfoListener);
        mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mMediaPlayer.setPhoneStateListener(mContext, mCallListener);
        // 设置dataSource
        try {
            mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(mUrl), mHeaders);
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
            RcsChatbotCardView.videoData.setState(STATE_PREPARING);
            mController.onPlayStateChanged(mCurrentState);
            Log.d("oye", "STATE_PREPARING");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("oye", "打开播放器发生错误", e);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return mSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    private JuphoonMediaPlayer.CallListener mCallListener
            = new JuphoonMediaPlayer.CallListener() {
        boolean isAutoPause = false;
        @Override
        public void onCallIdle() {
            if (mMediaPlayer != null && isAutoPause) {
                isAutoPause = false;
                restart();
            }
        }

        @Override
        public void onCallRinging() {
            if(mMediaPlayer!=null && mMediaPlayer.isPlaying()){
                isAutoPause = true;
                pause();
            }
        }
    };

    private MediaPlayer.OnPreparedListener mOnPreparedListener
            = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            mController.onPlayStateChanged(mCurrentState);
            Log.d("oye", "onPrepared ——> STATE_PREPARED");
            mp.start();
        }
    };

    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener
            = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            mTextureView.adaptVideoSize(width, height);
            Log.d("oye", "onVideoSizeChanged ——> width：" + width + "， height：" + height);
        }
    };

    private MediaPlayer.OnCompletionListener mOnCompletionListener
            = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_COMPLETED;
            mController.onPlayStateChanged(mCurrentState);
            Log.d("oye", "onCompletion ——> STATE_COMPLETED");
            RcsChatbotCardView.videoData.setState(STATE_COMPLETED);


            // 清除屏幕常亮
            mContainer.setKeepScreenOn(false);
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener
            = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            // 直播流播放时去调用mediaPlayer.getDuration会导致-38和-2147483648错误，忽略该错误
            if (what != -38 && what != -2147483648 && extra != -38 && extra != -2147483648) {
                mCurrentState = STATE_ERROR;
                mController.onPlayStateChanged(mCurrentState);
                Log.d("oye", "onError ——> STATE_ERROR ———— what：" + what + ", extra: " + extra);
            }
            return true;
        }
    };

    private MediaPlayer.OnInfoListener mOnInfoListener
            = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // 播放器开始渲染
                mCurrentState = STATE_PLAYING;

                RcsChatbotCardView.videoData.setState(STATE_PLAYING);
                mController.onPlayStateChanged(mCurrentState);
                Log.d("oye", "onInfo ——> MEDIA_INFO_VIDEO_RENDERING_START：STATE_PLAYING");
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                // MediaPlayer暂时不播放，以缓冲更多的数据
                if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_BUFFERING_PAUSED) {
                    mCurrentState = STATE_BUFFERING_PAUSED;
                    Log.d("oye", "onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PAUSED");
                } else {
                    mCurrentState = STATE_BUFFERING_PLAYING;
                    Log.d("oye", "onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PLAYING");
                }
                mController.onPlayStateChanged(mCurrentState);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                // 填充缓冲区后，MediaPlayer恢复播放/暂停
                if (mCurrentState == STATE_BUFFERING_PLAYING) {
                    mCurrentState = STATE_PLAYING;

                    RcsChatbotCardView.videoData.setState(STATE_PLAYING);
                    mController.onPlayStateChanged(mCurrentState);
                    Log.d("oye", "onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PLAYING");
                }
                if (mCurrentState == STATE_BUFFERING_PAUSED) {
                    mCurrentState = STATE_PAUSED;
                    RcsChatbotCardView.videoData.setState(STATE_PAUSED);
                    mController.onPlayStateChanged(mCurrentState);
                    Log.d("oye", "onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PAUSED");
                }
            } else if (what == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
                Log.d("oye", "视频不能seekTo，为直播视频");
            } else {
                Log.d("oye", "onInfo ——> what：" + what);
            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener
            = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mBufferPercentage = percent;
        }
    };

    /**
     * 全屏，将mContainer(内部包含mTextureView和mController)从当前容器中移除，并添加到android.R.content中.
     * 切换横屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期
     */
    @Override
    public void enterFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN) return;

        // 隐藏ActionBar、状态栏，并横屏
        RcsChatbotVideoUtil.hideActionBar(mContext);
        RcsChatbotVideoUtil.scanForActivity(mContext)
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        ViewGroup contentView = (ViewGroup) RcsChatbotVideoUtil.scanForActivity(mContext)
                .findViewById(android.R.id.content);
        if (mCurrentMode == MODE_TINY_WINDOW) {
            contentView.removeView(mContainer);
        } else {
            this.removeView(mContainer);
        }
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.addView(mContainer, params);

        mCurrentMode = MODE_FULL_SCREEN;
        mController.onPlayModeChanged(mCurrentMode);
        Log.d("oye", "MODE_FULL_SCREEN");
    }

    /**
     * 退出全屏，移除mTextureView和mController，并添加到非全屏的容器中。
     * 切换竖屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期.
     *
     * @return true退出全屏.
     */
    @Override
    public boolean exitFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN) {
            RcsChatbotVideoUtil.showActionBar(mContext);
            RcsChatbotVideoUtil.scanForActivity(mContext)
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            ViewGroup contentView = (ViewGroup) RcsChatbotVideoUtil.scanForActivity(mContext)
                    .findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mCurrentMode = MODE_NORMAL;
            mController.onPlayModeChanged(mCurrentMode);
            Log.d("oye", "MODE_NORMAL");
            return true;
        }
        return false;
    }

    @Override
    public void releasePlayer() {
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
            mAudioManager = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mContainer.removeView(mTextureView);
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mCurrentState = STATE_IDLE;

        RcsChatbotCardView.videoData.setState(STATE_IDLE);
        Log.d("oye", "releasePlayer-------");
    }

    @Override
    public void release() {
        // 退出全屏或小窗口
        if (isFullScreen()) {
            exitFullScreen();
        }
        mCurrentMode = MODE_NORMAL;

        // 释放播放器
        releasePlayer();

        // 恢复控制器
        if (mController != null) {
            mController.reset();
        }
        RcsChatbotCardView.videoData.setState(STATE_IDLE);
        Log.d("oye", "releasePlayer-------");
        Runtime.getRuntime().gc();
    }

}
