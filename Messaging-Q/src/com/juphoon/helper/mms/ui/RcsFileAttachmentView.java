package com.juphoon.helper.mms.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.juphoon.chatbotmaap.RcsChatbotFileProvider;
import com.juphoon.helper.mms.RcsMsgItemTouchHelper;
import com.juphoon.helper.mms.RcsTransProgressManager;
import com.juphoon.helper.mms.RcsTransProgressManager.TransProgress;
import com.juphoon.rcs.tool.RcsFileUtils;

import java.io.File;
import java.util.List;

public class RcsFileAttachmentView extends LinearLayout {

    private Context mContext;
    private TextView mFileMsgView;
    private TextView mMsgTypeView;
    private ProgressBar mFileTransProgress;

    public RcsFileAttachmentView(Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.file_attachment_view, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mMsgTypeView = (TextView) findViewById(R.id.file_view_type);
        mFileMsgView = (TextView) findViewById(R.id.file_view_name);
        mFileTransProgress = (ProgressBar) findViewById(R.id.file_trans_progress);
    }

    /**
     * Bind the burn message attachment view with a MessagePartData.
     */
    public void bindMessagePartData(final MessagePartData messagePartData, final ConversationMessageData mData) {
        Assert.isTrue(messagePartData != null);
        int strId = R.string.file_message;
        String path = null;
        // 收到的文件消息 text 字段会存文件名
        String fileName = messagePartData.getText();
        if (messagePartData.getContentUri() != null) {
            path = RcsFileUtils.getFilePathByUri(mContext, messagePartData.getContentUri());
            if (TextUtils.isEmpty(path)) {
                return;
            }
            if (TextUtils.isEmpty(fileName)) {
                fileName = path.substring(path.lastIndexOf("/") + 1, path.length());
            }
        }
        final String fileType = RcsFileUtils.getFileType(fileName);
        if (ContentType.isVideoType(fileType)) {
            strId = R.string.video_message;
        } else if (ContentType.isAudioType(fileType)) {
            strId = R.string.voice_message;
        } else if (ContentType.isImageType(fileType)) {
            strId = R.string.image_message;
        }
        mFileMsgView.setText(fileName);
        mMsgTypeView.setText("[" + getResources().getString(strId) + "]");
        final String filePath = path;
        this.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mData == null) {
                    return;
                }
                if (mData.getIsIncoming() && !mData.getIsDownloadComplete()) {
                    RcsMsgItemTouchHelper.dealFailMessgeTask(mData.getSmsMessageUri(), mData.getMessageId());
                    return;
                }
                if (TextUtils.isEmpty(filePath)) {
                    return;
                }
                List<String> pathSegments = Uri.parse(mData.getSmsMessageUri()).getPathSegments();
                long rmsMsgId = Long.valueOf(pathSegments.get(pathSegments.size() - 1));
                if (ContentType.isVideoType(fileType)) {
                    RcsMessageShowActivity.startVideoActivity(mContext, rmsMsgId, filePath);
                } else if (ContentType.isAudioType(fileType)) {
                    RcsMessageShowActivity.startVoiceActivity(mContext, rmsMsgId, filePath);
                } else if (ContentType.isImageType(fileType)) {
                    RcsMessageShowActivity.startImageActivity(mContext, rmsMsgId, filePath);
                } else {
                    File file = new File(filePath);
                    openFile(mContext, file);
                }
            }
        });
        if (mData != null) {
            TransProgress progress = RcsTransProgressManager.getTransProgress(mData.getMessageId());
            if (progress != null && progress.getMaxSize() > 0) {
                mFileTransProgress.setProgress((int)(progress.getTransSize() * 100 / progress.getMaxSize()));
            } else {
                if (!(mData.getStatus() == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
                        || mData.getStatus() == MessageData.BUGLE_STATUS_INCOMING_COMPLETE)) {
                    mFileTransProgress.setProgress(0);
                }
            }
        }
        
    }

    public void setFileMsgTextColor(final int color) {
        mFileMsgView.setTextColor(color);
    }

    public void setMsgTypeTextColor(final int color) {
        mMsgTypeView.setTextColor(color);
    }

    public void setFileTransProgress(final int progress) {
        mFileTransProgress.setProgress(progress);
    }

    public void setProgressVisibility(final Boolean visibility) {
        mFileTransProgress.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * 调用系统应用打开文件
     * 
     * @param context
     * @param file
     */
    public static void openFile(Context context, File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        String type = getMIMEType(file);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = RcsChatbotFileProvider.getUriForFile(context, RcsChatbotFileProvider.AUTHORITY, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri, type);
        // 跳转
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            context.startActivity(i);
             Toast.makeText(context, "找不到打开此文件的应用！", Toast.LENGTH_SHORT).show();
        }
    }

    /*** 根据文件后缀回去MIME类型 ****/

    private static String getMIMEType(File file) {
        String type = "*/*";
        String fName = file.getName();
        // 获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* 获取文件的后缀名 */
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (TextUtils.equals(end, "")) {
            return type;
        }
        for (int i = 0; i < MIME_MapTable.length; i++) {
            if (end.equals(MIME_MapTable[i][0])) {
                type = MIME_MapTable[i][1];
                break;
            }
        }
        return type;
    }

    private static final String[][] MIME_MapTable = {
            // {后缀名，MIME类型}
            { ".3gp", "video/3gpp" }, { ".apk", "application/vnd.android.package-archive" },
            { ".asf", "video/x-ms-asf" }, { ".avi", "video/x-msvideo" }, { ".bin", "application/octet-stream" },
            { ".bmp", "image/bmp" }, { ".c", "text/plain" }, { ".class", "application/octet-stream" },
            { ".conf", "text/plain" }, { ".cpp", "text/plain" }, { ".doc", "application/msword" },
            { ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
            { ".xls", "application/vnd.ms-excel" },
            { ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
            { ".exe", "application/octet-stream" }, { ".gif", "image/gif" }, { ".gtar", "application/x-gtar" },
            { ".gz", "application/x-gzip" }, { ".h", "text/plain" }, { ".htm", "text/html" }, { ".html", "text/html" },
            { ".jar", "application/java-archive" }, { ".java", "text/plain" }, { ".jpeg", "image/jpeg" },
            { ".jpg", "image/jpeg" }, { ".js", "application/x-javascript" }, { ".log", "text/plain" },
            { ".m3u", "audio/x-mpegurl" }, { ".m4a", "audio/mp4a-latm" }, { ".m4b", "audio/mp4a-latm" },
            { ".m4p", "audio/mp4a-latm" }, { ".m4u", "video/vnd.mpegurl" }, { ".m4v", "video/x-m4v" },
            { ".mov", "video/quicktime" }, { ".mp2", "audio/x-mpeg" }, { ".mp3", "audio/x-mpeg" },
            { ".mp4", "video/mp4" }, { ".mpc", "application/vnd.mpohun.certificate" }, { ".mpe", "video/mpeg" },
            { ".mpeg", "video/mpeg" }, { ".mpg", "video/mpeg" }, { ".mpg4", "video/mp4" }, { ".mpga", "audio/mpeg" },
            { ".msg", "application/vnd.ms-outlook" }, { ".ogg", "audio/ogg" }, { ".pdf", "application/pdf" },
            { ".png", "image/png" }, { ".pps", "application/vnd.ms-powerpoint" },
            { ".ppt", "application/vnd.ms-powerpoint" },
            { ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation" },
            { ".prop", "text/plain" }, { ".rc", "text/plain" }, { ".rmvb", "audio/x-pn-realaudio" },
            { ".rtf", "application/rtf" }, { ".sh", "text/plain" }, { ".tar", "application/x-tar" },
            { ".tgz", "application/x-compressed" }, { ".txt", "text/plain" }, { ".wav", "audio/x-wav" },
            { ".wma", "audio/x-ms-wma" }, { ".wmv", "audio/x-ms-wmv" }, { ".wps", "application/vnd.ms-works" },
            { ".xml", "text/plain" }, { ".z", "application/x-compress" }, { ".zip", "application/x-zip-compressed" },
            { "", "*/*" } };
}
