package com.android.messaging.tcl;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.messaging.R;


public class VideoActivity extends Activity
        implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, View.OnClickListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener  {

    private ImageView playOrPauseIv;
    private SurfaceView videoSuf;
    private MediaPlayer mPlayer;
    private ProgressBar mProgressBar;
    private String path;
    private RelativeLayout rootViewRl;
    private LinearLayout controlLl;
    private TextView startTime, endTime;

    private boolean isShow = false;

    public static final int UPDATE_TIME = 0x0001;
    public static final int HIDE_CONTROL = 0x0002;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TIME:
                    updateTime();
                    mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 500);
                    break;
                case HIDE_CONTROL:
                    hideControl();
                    break;
            }
        }
    };

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_video);

        initViews();
        initData();
        initSurfaceView();
        initPlayer();
        initEvent();

    }

    private void initData() {
        intent = getIntent();
       // path = Environment.getExternalStorageDirectory().getPath() + "/478102.mp4";//这里写上你的视频地址
        path=intent.getStringExtra("uri");
    }

    private void initEvent() {
        playOrPauseIv.setOnClickListener(this);
        rootViewRl.setOnClickListener(this);
    }

    private void initSurfaceView() {
        videoSuf = (SurfaceView) findViewById(R.id.surfaceView);
        videoSuf.setZOrderOnTop(false);
        videoSuf.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        videoSuf.getHolder().addCallback(this);
    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnSeekCompleteListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);
        try {
            mPlayer.setDataSource(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        playOrPauseIv = (ImageView) findViewById(R.id.playOrPause);
        startTime = (TextView) findViewById(R.id.tv_start_time);
        endTime = (TextView) findViewById(R.id.tv_end_time);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_progress);
        rootViewRl = (RelativeLayout) findViewById(R.id.root_rl);
        controlLl = (LinearLayout) findViewById(R.id.control_ll);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPlayer.setDisplay(holder);
        mPlayer.prepareAsync();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        startTime.setText(formatLongToTimeStr(mp.getCurrentPosition()));
        endTime.setText(formatLongToTimeStr(mp.getDuration()));
        mProgressBar.setMax(mp.getDuration());
        mProgressBar.setProgress(mp.getCurrentPosition());
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    private void play() {
        if (mPlayer == null) {
            return;
        }
        Log.i("playPath", path);
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            mHandler.removeMessages(UPDATE_TIME);
            mHandler.removeMessages(HIDE_CONTROL);
            playOrPauseIv.setVisibility(View.VISIBLE);
            playOrPauseIv.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mPlayer.start();
            mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 500);
            mHandler.sendEmptyMessageDelayed(HIDE_CONTROL, 5000);
            playOrPauseIv.setVisibility(View.INVISIBLE);
            playOrPauseIv.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //TODO
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playOrPause:
                play();
                break;
            case R.id.root_rl:
                showControl();
                break;
        }
    }

    /**
     * 更新播放时间
     */
    private void updateTime() {
        startTime.setText(formatLongToTimeStr(mPlayer.getCurrentPosition()));
        mProgressBar.setProgress(mPlayer.getCurrentPosition());
    }

    /**
     * 隐藏进度条
     */
    private void hideControl() {
        isShow = false;
        mHandler.removeMessages(UPDATE_TIME);
        controlLl.animate().setDuration(300).translationY(controlLl.getHeight());
    }

    /**
     * 显示进度条
     */
    private void showControl() {
        if (isShow) {
            play();
        }
        isShow = true;
        mHandler.removeMessages(HIDE_CONTROL);
        mHandler.sendEmptyMessage(UPDATE_TIME);
        mHandler.sendEmptyMessageDelayed(HIDE_CONTROL, 5000);
        controlLl.animate().setDuration(300).translationY(0);
    }

    public static String formatLongToTimeStr(int time) {
        int minute = 0;
        int second = 0;
        String tmp;
        minute = time / 1000 / 60;
        tmp = String.valueOf(minute).length() == 2 ? minute + "" : "0" + minute + ":";
        second = time / 1000 % 60;
        tmp += String.valueOf(second).length() == 2 ? second + "" : "0" + second;
        return tmp;
    }

}

