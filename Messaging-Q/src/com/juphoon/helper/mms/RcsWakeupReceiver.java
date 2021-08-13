package com.juphoon.helper.mms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 *  用于 RcsApplication 唤醒 Messaging
 */

public class RcsWakeupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(RcsMmsInitHelper.getContext(), RcsImReceiverServiceEx.class);
        RcsImReceiverServiceEx.enqueueWork(RcsMmsInitHelper.getContext(), intent);
    }
}
