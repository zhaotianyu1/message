package com.juphoon.helper.mms;

import android.text.TextUtils;
import android.util.Log;

import com.juphoon.cmcc.lemon.MtcImConstants;
import com.juphoon.cmcc.lemon.MtcImSessConstants;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.helper.RcsGroupHelper.RcsGroupInfo;
import com.juphoon.helper.RcsPublicAddressHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsCheckUtils;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsGeoUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsImServiceConstants;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class RcsMessageSendHelper {

    private final static String TAG = RcsMessageSendHelper.class.getSimpleName();

    public static class WaitToSendMessage {
        public int mId;
        public int mThreadId;
        public int mMessageType;
        public String mAddress;
        public String mBody;
        public int mStatus;
        public long mDate;
        public String mFilePath;
        public String mFileType;
        public String mThumbPath;
        public String mGroupChatId;
        public String mPaUuid;
        public int mFileDuration;
        public int mType;
        public String mTransId;
        public String mImdnString;
        public int mTransSize;
        public int mFileSize;
        public String mRmsExtra;
        public String mCmccToken;
        public String mConversationId; // 消息的ConversationId
        public String mTrafficType; // 消息的 trafficType
        public String mContributionId; // 消息的ContributionId
    }

    public static String sendTextMessage(WaitToSendMessage message) {
        int threadType = getThreadType(message);
        Log.d("zty", "------------threadType--------"+threadType);
        // chatbot
        RcsChatbotHelper.RcsChatbot chatbotinfo = RcsChatbotHelper.getChatbotInfoByServiceId(message.mAddress);
        if (chatbotinfo != null) {
            Log.d("zty", "------------chatbotinfo--------");
            // 发送 chatbot reply 消息
            if (message.mRmsExtra != null) {
                return RcsCallWrapper.rcsSendMessage1To1(String.valueOf(message.mId), chatbotinfo.serviceId, message.mRmsExtra, RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SUGGESTION, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, message.mConversationId, message.mTrafficType, message.mContributionId);
            } else {
                return RcsCallWrapper.rcsSendMessage1To1(String.valueOf(message.mId), chatbotinfo.serviceId, message.mBody, RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, message.mConversationId, null, null);
            }
        }

        if (threadType == RmsDefine.COMMON_THREAD) {
            Log.d("zty", "------------RmsDefine.COMMON_THREAD--------");
            if (RcsCallWrapper.rcsIsSession1To1()) {
                Log.d("zty", "------------RmsDefine.COMMON_THREAD1--------");
                return RcsCallWrapper.rcsSendOTOSessMsg(String.valueOf(message.mId), message.mAddress, message.mBody);
            } else {
                Log.d("zty", "------------RmsDefine.COMMON_THREAD2--------");
                return RcsCallWrapper.rcsSendMessage1To1(String.valueOf(message.mId), message.mAddress, message.mBody, RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_DEFAULT, message.mConversationId, null, null);
            }
        } else if (threadType == RmsDefine.BROADCAST_THREAD) {
            Log.d("zty", "------------RmsDefine.COMMON_THREAD3--------");
            return RcsCallWrapper.rcsSendMessage1ToM(String.valueOf(message.mId), message.mAddress, message.mBody, RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT);
        } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
            // 进这里表示 session 已经存在
            Log.d("zty", "------------RmsDefine.COMMON_THREAD4--------");
            RcsGroupInfo info = RcsGroupHelper.getGroupInfo(message.mGroupChatId);
            if (info != null) {
                return RcsCallWrapper.rcsSendGroupMsg(String.valueOf(message.mId), message.mGroupChatId, message.mBody, MtcImSessConstants.EN_MTC_IM_SESS_CONT_MSG_TXT_PLAIN,message.mRmsExtra);
            }
            return null;
        }
        Log.d("zty", "------------null123--------");
        return null;
    }

    public static String sendGeoMessage(WaitToSendMessage message) {
        int threadType = getThreadType(message);
        RcsLocationHelper.RcsLocationItem rcsLocationItem = RcsLocationHelper.parseLocationJson(RcsMmsUtils.geoFileToString(message.mFilePath));
        if (rcsLocationItem != null) {
            Double latitude = rcsLocationItem.mLatitude;
            Double longtitude = rcsLocationItem.mLongitude;
            float radius = rcsLocationItem.mRadius;
            String locationName = rcsLocationItem.mAddress;
            RcsChatbotHelper.RcsChatbot chatbotinfo = RcsChatbotHelper.getChatbotInfoByServiceId(message.mAddress);
            if (chatbotinfo != null) {
                return RcsCallWrapper.rcsSendMessage1To1(String.valueOf(message.mId), chatbotinfo.serviceId, RcsGeoUtils.genGeoString(latitude, longtitude, radius, locationName), RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, message.mConversationId, message.mTrafficType, message.mContributionId);
            }
            if (threadType == RmsDefine.COMMON_THREAD) {
                return RcsCallWrapper.rcsSendMessage1To1(String.valueOf(message.mId), message.mAddress, RcsGeoUtils.genGeoString(latitude, longtitude, radius, locationName), RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_DEFAULT, message.mConversationId, null, null);
            } else if (threadType == RmsDefine.BROADCAST_THREAD) {
                return RcsCallWrapper.rcsSendMessage1ToM(String.valueOf(message.mId), message.mAddress, RcsGeoUtils.genGeoString(latitude, longtitude, radius, locationName), RcsImServiceConstants.RCS_MESSAGE_TYPE_TEXT);
            } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
                RcsGroupInfo info = RcsGroupHelper.getGroupInfo(message.mGroupChatId);
                if (info != null) {
                    return RcsCallWrapper.rcsSendGroupMsg(String.valueOf(message.mId), info.mGroupChatId, RcsGeoUtils.genGeoString(latitude, longtitude, radius, locationName), MtcImSessConstants.EN_MTC_IM_SESS_CONT_MSG_TXT_PLAIN, null);
                }
            }
        }
        return null;
    }

    public static String sendEmoticonMessage(WaitToSendMessage message) {
        int threadType = getThreadType(message);
        if (threadType == RmsDefine.COMMON_THREAD) {
            if (!RcsCheckUtils.checkTextOverMaxUtf8(message.mRmsExtra, RcsServiceManager.getMaxPageLimit())) {
                return RcsCallWrapper.rcsSendEmoticonPMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            } else {
                return RcsCallWrapper.rcsSendEmoticonLMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            }
        } else if (threadType == RmsDefine.BROADCAST_THREAD) {
            String[] arrayAddress = message.mAddress.split(";");
            if (arrayAddress.length <= RcsCheckUtils.caclMaxPersonNum(message.mRmsExtra, RcsServiceManager.getMaxPageLimit())) {
                return RcsCallWrapper.rcsSendOTMEmoticonPMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            } else {
                return RcsCallWrapper.rcsSendOTMEmoticonPMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            }
        } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
            RcsGroupInfo info = RcsGroupHelper.getGroupInfo(message.mGroupChatId);
            if (info != null) {
                return RcsCallWrapper.rcsSendGroupEmoticonMsg(String.valueOf(message.mId), message.mGroupChatId, message.mRmsExtra, null);
            }
            return null;
        }
        return null;
    }

    public static String sendCloudMessage() {
        return null;
    }

    public static String sendCardMessage(WaitToSendMessage message) {
        int threadType = getThreadType(message);
        if (threadType == RmsDefine.COMMON_THREAD) {
            if (!RcsCheckUtils.checkTextOverMaxUtf8(message.mRmsExtra, RcsServiceManager.getMaxPageLimit())) {
                return RcsCallWrapper.rcsSendCardPMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            } else {
                return RcsCallWrapper.rcsSendCardLMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            }
        } else if (threadType == RmsDefine.BROADCAST_THREAD) {
            String[] arrayAddress = message.mAddress.split(";");
            if (arrayAddress.length <= RcsCheckUtils.caclMaxPersonNum(message.mRmsExtra, RcsServiceManager.getMaxPageLimit())) {
                return RcsCallWrapper.rcsSendOTMCardPMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            } else {
                return RcsCallWrapper.rcsSendOTMCardLMsg(String.valueOf(message.mId), message.mAddress, message.mRmsExtra);
            }
        } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
            RcsGroupInfo info = RcsGroupHelper.getGroupInfo(message.mGroupChatId);
            if (info != null) {
                return RcsCallWrapper.rcsSendGroupCardMsg(String.valueOf(message.mId), message.mGroupChatId, message.mRmsExtra, null);
            }
        }
        return null;
    }

    public static String sendFileMessage(WaitToSendMessage message) {
        int threadType = getThreadType(message);
        byte[] thumbData = null;
        if (!TextUtils.isEmpty(message.mThumbPath) && new File(message.mThumbPath).exists()) {
            thumbData = RcsFileUtils.fileToBytes(message.mThumbPath);
        }
        // chatbot
        RcsChatbotHelper.RcsChatbot chatbotinfo = RcsChatbotHelper.getChatbotInfoByServiceId(message.mAddress);
        if (chatbotinfo != null) {
            return RcsCallWrapper.rcsHttpUploadFile(String.valueOf(message.mId), message.mCmccToken, message.mFilePath, message.mFileType, message.mFileDuration, thumbData, 0, RcsMmsUtils.getMessageDisposition(message.mMessageType));
        }
        if (threadType == RmsDefine.COMMON_THREAD) {
            return RcsCallWrapper.rcsSendFile(String.valueOf(message.mId), message.mAddress, message.mFilePath, message.mFileType, message.mFileDuration, thumbData);
        } else if (threadType == RmsDefine.BROADCAST_THREAD) {
            return RcsCallWrapper.rcsSendOTMFile(String.valueOf(message.mId), message.mAddress, message.mFilePath, message.mFileType, message.mFileDuration, thumbData);
        } else if (threadType == RmsDefine.RMS_GROUP_THREAD) {
            RcsGroupInfo info = RcsGroupHelper.getGroupInfo(message.mGroupChatId);
            if (info != null) {
                return RcsCallWrapper.rcsSendGroupFile(String.valueOf(message.mId), info.mSubject, info.mSessionIdentify, info.mGroupChatId, message.mFilePath, message.mFileType,
                        message.mFileDuration, thumbData);
            }
        }

        return null;
    }

    public static String sendHttpFileMessage(WaitToSendMessage message) {
        byte[] thumbData = null;
        if (!TextUtils.isEmpty(message.mThumbPath) && new File(message.mThumbPath).exists()) {
            thumbData = RcsFileUtils.fileToBytes(message.mThumbPath);
        }
        String filePath = message.mFilePath;
        String endSuffix = RcsFileUtils.getFileSuffix(filePath);
        String fileType = RcsFileUtils.getHttpFileType(message.mMessageType, endSuffix);
        return RcsCallWrapper.rcsHttpUploadFile(String.valueOf(message.mId), message.mCmccToken, message.mFilePath, fileType, message.mFileDuration, thumbData, 0, RcsMmsUtils.getMessageDisposition(message.mMessageType));
    }


    private static int getThreadType(WaitToSendMessage message) {
        if (!TextUtils.isEmpty(message.mGroupChatId)) {
            return RmsDefine.RMS_GROUP_THREAD;
        } else {
            String[] arrayAddress = message.mAddress.split(";");
            if (arrayAddress.length > 1) {
                return RmsDefine.BROADCAST_THREAD;
            }
            return RmsDefine.COMMON_THREAD;
        }
    }

}
