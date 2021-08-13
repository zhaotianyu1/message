package com.juphoon.chatbotmaap;

import android.content.Context;
import android.media.MediaPlayer;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import static android.content.Context.TELEPHONY_SERVICE;

public class JuphoonMediaPlayer extends MediaPlayer {

    private CallListener mCallListener;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mCallListener != null) {
                        mCallListener.onCallIdle();
                    }
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mCallListener != null) {
                        mCallListener.onCallRinging();
                    }
                    break;
            }
        }
    };

    public void setPhoneStateListener(Context context, CallListener listener) {
        mCallListener = listener;
        mTelephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void release() {
        super.release();
        if (mCallListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mCallListener = null;
        }
    }

    interface CallListener {
        void onCallIdle();

        void onCallRinging();
    }
}
