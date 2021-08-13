package com.juphoon.helper.mms;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.android.messaging.util.OsUtil;
import com.baidu.mapapi.SDKInitializer;
import com.juphoon.chatbotmaap.RcsChatbotImageUtil;
import com.juphoon.chatbotmaap.RcsChatbotImdnManager;
import com.juphoon.helper.RcsBitmapCache;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.mms.ui.RcsGroupNotificationActivity;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsGroupChatManager;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.rcs.JApplication;

public class RcsMmsInitHelper {

    private static boolean sInit = false;
    private static Context sContext;

    private final static RcsServiceManager.IServiceManagerCallback sIServiceManager = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(boolean logined) {
            if (logined) {
                RcsMessageSendService.enqueueWork(sContext, new Intent(RcsMessageSendService.ACTION_SEND_RCS_MESSAGE, null,
                        sContext, RcsMessageSendService.class));
                RcsMmsUtils.setLastLoginMsisdn(RcsServiceManager.getUserName());
                RcsReTransferManager.retransferIfNetChange(System.currentTimeMillis());
            } else {
                // 将正在rejoin的群列表清空
                RcsGroupChatManager.notifyAllRejoinFail();
            }
        }

        @Override
        public void onConnectedChange(boolean connected, String name) {
            super.onConnectedChange(connected, name);
            if (TextUtils.equals(RcsServiceManager.RCS, name) && connected) {
                RcsCallWrapper.rcsResetCpVersionIfNeed();
            } else if (TextUtils.equals(RcsServiceManager.IM, name) && !connected) {
                RcsMmsUtils.markQueueAndOutboxMsgFail();
                RcsMmsUtils.markInboxMsgFail();
            }
        }

    };

    private final static RcsBroadcastHelper.IGroupListener sIGroupListener = new RcsBroadcastHelper.IGroupListener() {

        @Override
        public void onGroupInfoChange(String groupChatId) {
            RcsMmsUtils.updateBugleGroupName(sContext, groupChatId);
        }

        @Override
        public void onGroupSessChange(String groupChatId, boolean have) {
            if (have) {
                RcsMessageSendService.enqueueWork(sContext, new Intent(RcsMessageSendService.ACTION_SEND_RCS_MESSAGE, null,
                        sContext, RcsMessageSendService.class));
            } else {
                RcsMmsUtils.updateGroupMessageFailed(groupChatId, true);// 发送中
                RcsMmsUtils.updateGroupMessageFailed(groupChatId, false);// 接收中
            }
        }

        @Override
        public void onGroupRecvInvite(String groupChatId) {
            RcsGroupNotificationActivity.UpdateNotification(sContext);
        }
    };

    private final static RcsBroadcastHelper.IMessageListener sIMessageListener = new RcsBroadcastHelper.IMessageListener() {

        @Override
        public void onMessageSendResult(boolean succ, final String imdn) {
            if (!succ) {
                RcsMessageTransHelper.dealFailMessage(sContext, imdn);
            }
        }

        @Override
        public void onMessageRecv(final String imdn) {
        }

        @Override
        public void onFileRecvInvite(final String transId) {
        }

        @Override
        public void onFileSendResult(boolean succ, final String transId, boolean isHttpFile) {
        }

        @Override
        public void onFileRecvResult(boolean b, String s) {

        }

        @Override
        public void onGeoSendResult(boolean succ, String transId) {
        }
    };

    private static RcsInitCallback mRcsInitCallback;

    public interface RcsInitCallback {
        void onInited(boolean inited);
    }

    public static void setRcsInitCallback(RcsInitCallback rcsInitCallback) {
        mRcsInitCallback = rcsInitCallback;
        if (sInit) {
            mRcsInitCallback.onInited(true);
        }
    }

    public static void init(Context context) {
        // 防止com.android.messaging:remote进程init导致收到2条消息
        if (!TextUtils.equals(getCurProcessName(context), context.getPackageName())) {
            return;
        }
        JApplication jApplication =new JApplication();
        JApplication.sContext=context;
        jApplication.init();
        if (!sInit) {
            if (!OsUtil.hasRequiredPermissions()) {
                return;
            }
            sInit = true;
            sContext = context;
            RcsServiceManager.init(context, context.getPackageName());
            RcsGroupChatManager.init(context);
            RcsGroupHelper.init(context);
            SDKInitializer.initialize(context);
            RcsBroadcastHelper.init(context, true);
            RcsBitmapCache.init(context);
            RcsChatbotImageUtil.init();
            RcsMsgItemTouchHelper.init(context);
            RcsMessageForwardHelper.init(context);

            RcsMmsUtils.dealTimeoutMessages();
            RcsMmsUtils.markQueueAndOutboxMsgFail();
            RcsMmsUtils.markInboxMsgFail();
            RcsServiceManager.addCallBack(sIServiceManager);
            RcsBroadcastHelper.addGroupListener(sIGroupListener);
            RcsBroadcastHelper.addMessageListener(sIMessageListener);
            RcsVCardCache.init(sContext);
            RcsVideoCompressorHelper.init(sContext);
            RcsAppNumberHelper.init(sContext);
            RcsFileDownloadHelper.init(sContext);
            RcsTransProgressManager.init();
            RcsChatbotHelper.init(sContext);
            RcsChatbotImdnManager.init(sContext);
            RcsImdnManager.init(sContext);
            if (mRcsInitCallback != null) {
                mRcsInitCallback.onInited(true);
            }
            RcsRecipientHelper.init(sContext);
        }
    }

    private static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    public static boolean getIsInit() {
        return sInit;
    }

    public static Context getContext() {
        return sContext;
    }
}
