package com.juphoon.helper.mms;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.messaging.datamodel.ParticipantRefresh;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsPublicAddressHelper;
import com.juphoon.rcs.tool.RcsNetUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Rcs
 */
public class RcsRecipientHelper {
    private final static String TAG = RcsRecipientHelper.class.getSimpleName();

    private final static Object sLock = new Object();
    private static boolean sRecipientsLoaded = false;
    private final static Set<URI> sSetIconUris = new HashSet<>();
    private static Context sContext;
    private final static FileObserver sFileObserver = new FileObserver(RmsDefine.RMS_ICON_PATH, FileObserver.CLOSE_WRITE | FileObserver.DELETE | FileObserver.DELETE_SELF) {
        @Override
        public void onEvent(int event, @Nullable String path) {
            Log.d(TAG, event + " " + path);
            new Handler(sContext.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    ParticipantRefresh.rcsForceRefresh();
                }
            }, 1000);
        }
    };

    private final static RcsBroadcastHelper.IChatbotListener sIChatbotListener = new RcsBroadcastHelper.IChatbotListener() {
        @Override
        public void onChatbotRecommandList(String s, boolean b, String s1) {

        }

        @Override
        public void onChatbotList(String cookie, boolean succ, String jsonChatbots) {
            if (succ) {
                new Handler(sContext.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ParticipantRefresh.rcsForceRefresh();
                    }
                }, 1000);
            }
        }

        @Override
        public void onChatbotInfo(String cookie, boolean succ, String jsonInfo) {
            if (succ) {
                new Handler(sContext.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ParticipantRefresh.rcsForceRefresh();
                    }
                }, 1000);
            }
        }
    };

    private final static RcsBroadcastHelper.IGroupListener sIGroupListener = new RcsBroadcastHelper.IGroupListener() {
        @Override
        public void onGroupInfoChange(String groupChatId) {
            new Handler(sContext.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    ParticipantRefresh.rcsForceRefresh();
                }
            }, 1000);
        }

        @Override
        public void onGroupSessChange(String s, boolean b) {

        }

        @Override
        public void onGroupRecvInvite(String s) {

        }
    };

    private final static ContentObserver sPublicObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            new Handler(sContext.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    ParticipantRefresh.rcsForceRefresh();
                }
            }, 1000);
        }
    };

    public static void init(Context context) {
        if (sContext == null) {
            sContext = context;
            sFileObserver.startWatching();
            RcsBroadcastHelper.addChatbotListener(sIChatbotListener);
            RcsBroadcastHelper.addGroupListener(sIGroupListener);
            sContext.getContentResolver().registerContentObserver(RmsDefine.PublicAddress.CONTENT_URI, true, sPublicObserver);
        }
    }

    public static void unint() {
        if (sContext != null) {
            sFileObserver.stopWatching();
            RcsBroadcastHelper.removeChatbotListener(sIChatbotListener);
            RcsBroadcastHelper.removeGroupListener(sIGroupListener);
            sContext.getContentResolver().unregisterContentObserver(sPublicObserver);
        }
    }

    /**
     * 处理号码是否有icon，需要下载则下载完触发 Messaging 中 Recipient 的更新
     *
     * @param number
     */
    public static void checkRcsIcon(String number) {
        if (TextUtils.isEmpty(number)) {
            return;
        }
        final RcsChatbotHelper.RcsChatbot chatbot = RcsChatbotHelper.getChatbotInfoByServiceId(number);
        final RcsGroupHelper.RcsGroupInfo groupInfo = RcsGroupHelper.getGroupInfo(number);
        if (chatbot != null) {
            RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(chatbot.icon, 1000);
            if (RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_ICON_PATH) == null) {
                if (RcsServiceManager.isLogined()) {
                    RcsFileDownloadHelper.downloadFile(null, fileInfo, null, null, RmsDefine.RMS_ICON_PATH);
                }
            }
        } else if (groupInfo != null) {

        }
    }

}
