package com.juphoon.receiver;

import android.net.Uri;
import android.text.TextUtils;

import com.juphoon.rcs.app.tool.RcsChatbotDealEx;
import com.juphoon.rcs.app.tool.RcsChatbotInfoGetTools;
import com.juphoon.rcs.app.tool.RcsGroupDealEx;
import com.juphoon.rcs.app.tool.RcsImDealEx;
import com.juphoon.rcs.app.tool.RcsUtilsEx;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;

import org.json.JSONException;
import org.json.JSONObject;

public class RcsReceiverServiceEx {
    /**
     * 与 RcsService 中基本一致
     *
     * @param jsonObj
     */
    public static boolean dealIm(JSONObject jsonObj) {
        String jsonAction = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_ACTION);
        // 过滤抄送的消息
        if (RcsUtilsEx.getCCDisable()) {
            if (jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_MESSAGE_CC, false)) {
                return false;
            }
        }
        try {
            if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_RECV_MSG)) {
                Uri uri = RcsImDealEx.dealImRecvMsg(jsonObj);
                if (uri != null) {
                    String id = uri.getPathSegments().get(1);
                    jsonObj.put(RcsJsonParamConstants.RCS_JSON_COOKIE, id);
                } else {
                    return false;
                }
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_OK)) {
                RcsImDealEx.dealImSendResult(jsonObj, true);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IM_MSG_SEND_FAILED)) {
                if (com.juphoon.helper.mms.RcsReTransferManager.retransferIfNeed(jsonObj)) {
                    return false;
                }
                RcsImDealEx.dealImSendResult(jsonObj, false);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_INVITE)) {
                if (isNeedReject(jsonObj)) {
                    RcsCallWrapper.rcsRejectFile(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID));
                    return false;
                }
                Uri uri = RcsImDealEx.dealImRecvFileInvite(jsonObj);
                if (uri != null) {
                    String id = uri.getPathSegments().get(1);
                    jsonObj.put(RcsJsonParamConstants.RCS_JSON_COOKIE, id);
                } else {
                    // 数据库插入失败，不通知上层
                    return false;
                }
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECVING_PROGRESS)) {
                RcsImDealEx.dealImRecvFileProgress(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_DONE)) {
                RcsImDealEx.dealImRecvFileDone(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_RECV_FAILED)) {
                if (com.juphoon.helper.mms.RcsReTransferManager.retransferIfNeed(jsonObj)) {
                    return false;
                }
                RcsImDealEx.dealImRecvFileFailed(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SENDING_PROGRESS)) {
                RcsImDealEx.dealImSendFileProgress(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_OK)) {
                RcsImDealEx.dealImSendFileOk(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_SEND_FAILED)) {
                if (com.juphoon.helper.mms.RcsReTransferManager.retransferIfNeed(jsonObj)) {
                    return false;
                }
                RcsImDealEx.dealImSendFileFailed(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_FILE_FETCH_REJECT)) {
                RcsImDealEx.dealImFecthReject(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_INVITE)) {
                if (isNeedReject(jsonObj)) {
                    RcsCallWrapper.rcsGeoReject(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_TRANS_ID));
                    return false;
                }
                Uri uri = RcsImDealEx.dealImRecvGsInvite(jsonObj);
                if (uri != null) {
                    String id = uri.getPathSegments().get(1);
                    jsonObj.put(RcsJsonParamConstants.RCS_JSON_COOKIE, id);
                } else {
                    // 数据库插入失败，不通知上层
                    return false;
                }
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_DONE)) {
                RcsImDealEx.dealImRecvGsOk(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_RECV_FAILED)) {
                if (com.juphoon.helper.mms.RcsReTransferManager.retransferIfNeed(jsonObj)) {
                    return false;
                }
                RcsImDealEx.dealImRecvGsFailed(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_OK)) {
                RcsImDealEx.dealImShareGsOk(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GS_SHARE_FAILED)) {
                if (com.juphoon.helper.mms.RcsReTransferManager.retransferIfNeed(jsonObj)) {
                    return false;
                }
                RcsImDealEx.dealImShareGsFailed(jsonObj);
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_1To1_RECV_INVITE)) {
                RcsImDealEx.dealImRecv1To1Invite(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_RECV_INVITE)) {
                RcsGroupDealEx.dealGroupNotificationRecvInvite(jsonObj);
                RcsGroupDealEx.dealGroupRecvInvite(jsonObj);
                //收到群激活的回调时,不通知上层
                if (jsonObj.optInt(RcsJsonParamConstants.RCS_JSON_GROUP_INVITE_CREATE) == 0) {
                    return false;
                }
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_ACCEPTED)) {
                RcsGroupDealEx.dealGroupNotificationAccepted(jsonObj);
                RcsGroupDealEx.dealGroupAccepted(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_CANCELED)) {
                RcsGroupDealEx.dealGroupNotificationCanceled(jsonObj);
                RcsGroupDealEx.dealGroupCanceled(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_REJECTED)) {
                RcsGroupDealEx.dealGroupNotificationRejected(jsonObj);
                RcsGroupDealEx.dealGroupRejected(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_RECV_MSG)) {
                if (isNeedReject(jsonObj)) {
                    return false;
                }
                String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
                if (!RcsGroupDealEx.haveGroupInDb(groupChatId)) {
                    RcsGroupDealEx.insertGroup(jsonObj, RmsDefine.RmsGroup.STATE_STARTED);
                }
                Uri uri = RcsGroupDealEx.dealGroupRecvMsg(jsonObj);
                if (uri != null) {
                    String id = uri.getPathSegments().get(1);
                    jsonObj.put(RcsJsonParamConstants.RCS_JSON_COOKIE, id);
                } else {
                    // 数据库插入失败，不通知上层
                    return false;
                }
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_PARTP_ADD_FAIL)) {
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_PARTP_EPL_FAIL)) {
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_PARTP_UPDATE)) {
                String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
                if (!RcsGroupDealEx.haveGroupInDb(groupChatId)) {
                    RcsGroupDealEx.insertGroup(jsonObj, RmsDefine.RmsGroup.STATE_STARTED);
                }
                RcsGroupDealEx.dealGroupPartpUpdate(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_MSG_SEND_RESULT)) {
                boolean sendOk = jsonObj.optBoolean(RcsJsonParamConstants.RCS_JSON_RESULT, false);
                if (sendOk) {
                    RcsGroupDealEx.dealGroupSendOk(jsonObj);
                } else {
                    if (com.juphoon.helper.mms.RcsReTransferManager.retransferIfNeed(jsonObj)) {
                        return false;
                    }
                    RcsGroupDealEx.dealGroupSendFailed(jsonObj);
                }
                com.juphoon.helper.mms.RcsReTransferManager.removeFromRetransferMap(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_IMDN_STATUS)) {
                RcsImDealEx.dealImRecvImdnStatus(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_DISPLAYNAME_MDFY_OK)) {
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_RELEASED)) {
                RcsGroupDealEx.dealGroupNotificationReleased(jsonObj);
                RcsGroupDealEx.dealGroupReleased(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_DISSOLVE_OK)) {
                RcsGroupDealEx.dealGroupDissolveOk(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_GROUPCHAT_LEAVE_OK)) {
                RcsGroupDealEx.dealGroupLeaveOk(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CONFS_SUB_LIST)) {
                RcsGroupDealEx.dealGroupRecvList(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CONFS_SUB_INFO)) {
                RcsGroupDealEx.dealGroupRecvOneInfo(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_TRANSID_IMDN_UPDATE)) {
                RcsImDealEx.dealImdnTransIdUpdate(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CHATBOT_LIST_OK)) {
                RcsChatbotDealEx.dealChatbotList(jsonObj);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CHATBOT_INFO_OK)) {
                String serviceId = RcsChatbotDealEx.dealChatbotInfo(jsonObj);
                if (TextUtils.isEmpty(serviceId)) {
                    RcsChatbotInfoGetTools.dealGetInfoResult(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE), false);
                    return false;
                }
                try {
                    jsonObj.put(RcsJsonParamConstants.RCS_JSON_CHATBOT_SERVICE_ID, serviceId);
                    RcsChatbotInfoGetTools.dealGetInfoResult(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE), true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CHATBOT_INFO_FAIL)) {
                RcsChatbotInfoGetTools.dealGetInfoResult(jsonObj.optString(RcsJsonParamConstants.RCS_JSON_COOKIE), false);
            } else if (jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CAP_OK)
                    || jsonAction.equals(RcsJsonParamConstants.RCS_JSON_ACTION_CAP_UPDATE)) {
                RcsImDealEx.dealImRecvCapResult(jsonObj);
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 群聊消息是否拒绝
    private static boolean isNeedReject(JSONObject jsonObj) {
        String groupChatId = jsonObj.optString(RcsJsonParamConstants.RCS_JSON_GROUP_CHAT_ID);
        if (!TextUtils.isEmpty(groupChatId)) {
            if (RcsGroupDealEx.isGroupRejectRecvMsg(groupChatId)) {
                return true;
            }
        }
        return false;
    }

}
