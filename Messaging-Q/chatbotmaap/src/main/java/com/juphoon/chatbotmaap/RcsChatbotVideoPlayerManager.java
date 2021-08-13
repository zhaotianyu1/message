package com.juphoon.chatbotmaap;

/**
 * 视频播放器管理器.
 */
public class RcsChatbotVideoPlayerManager {

    private RcsChatbotVideoPlayer mVideoPlayer;

    private String mCookie;

    public void setCookie(String cookie){
        mCookie = cookie;
    }

    public String getCookie(){
        return mCookie;
    }

    private RcsChatbotVideoPlayerManager() {
    }

    private static RcsChatbotVideoPlayerManager sInstance;

    public static synchronized RcsChatbotVideoPlayerManager instance() {
        if (sInstance == null) {
            sInstance = new RcsChatbotVideoPlayerManager();
        }
        return sInstance;
    }

    public void setCurrentNiceVideoPlayer(RcsChatbotVideoPlayer videoPlayer) {
        if (mVideoPlayer != videoPlayer) {
            releaseNiceVideoPlayer();
            mVideoPlayer = videoPlayer;
        }
    }

    public void releaseNiceVideoPlayer() {
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
            mVideoPlayer = null;
        }
    }

    public boolean onBackPressd() {
        if (mVideoPlayer != null) {
            if (mVideoPlayer.isFullScreen()) {
                return mVideoPlayer.exitFullScreen();
            }
        }
        return false;
    }
}
