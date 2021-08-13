package com.juphoon.helper.mms;

import android.net.Uri;

public class RcsMessage {

    private final int mType; // 消息类型，参见RmsDefine.java
    private Uri mUri;
    private String mText;
    private String mContributionId;
    private String mRmsExtra;
    private final long mDate;
    private String mTrafficType;

    public RcsMessage(int type, String text) {
        mType = type;
        mText = text;
        mDate = System.currentTimeMillis();
    }

    public RcsMessage(int type, String text, String rmsExtra) {
        mType = type;
        mText = text;
        mRmsExtra = rmsExtra;
        mDate = System.currentTimeMillis();
    }

    public RcsMessage(int type, String text, String contributionId, String rmsExtra, String trafficType) {
        mType = type;
        mText = text;
        mRmsExtra = rmsExtra;
        mContributionId = contributionId;
        mDate = System.currentTimeMillis();
        mTrafficType = trafficType;
    }

    public RcsMessage(int type, Uri uri) {
        mType = type;
        mUri = uri;
        mDate = System.currentTimeMillis();
    }

    public RcsMessage(int type, String text, Uri uri) {
        mType = type;
        mText = text;
        mUri = uri;
        mDate = System.currentTimeMillis();
    }

    public RcsMessage(int type, String text, Uri uri, String rmsExtra) {
        mType = type;
        mText = text;
        mUri = uri;
        mDate = System.currentTimeMillis();
        mRmsExtra = rmsExtra;
    }

    public int getType() {
        return mType;
    }

    public String getText() {
        return mText;
    }

    public Uri getUri() {
        return mUri;
    }

    public long getDate() {
        return mDate;
    }

    public String getRmsExtra() {
        return mRmsExtra;
    }

    public String getContributionId() {
        return mContributionId;
    }

    public String getTrafficType() {
        return mTrafficType;
    }
}
