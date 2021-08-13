package com.juphoon.chatbotmaap.chatbotMessages;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsImageHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.service.RmsDefine;

public class RcsChatbotConversationListItemView extends RelativeLayout implements View.OnClickListener {
    RcsChatBotConversationListItemData mData;
    private ImageView mReadStatusView;
    private ImageView mIconView;
    private TextView mTitleView;
    private TextView mSnippetView;
    private TextView mTimeView;
    private HostInterface mHostInterface;

    public interface HostInterface {
        boolean isConversationSelected(final String conversationId);

        void onConversationClicked(final RcsChatBotConversationListItemData conversationListItemData, boolean isLongClick);

        boolean isSwipeAnimatable();
    }

    public RcsChatbotConversationListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mData = new RcsChatBotConversationListItemData();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = findViewById(R.id.conversation_icon);
        mTitleView = findViewById(R.id.conversation_title);
        mSnippetView = findViewById(R.id.conversation_draf);
        mReadStatusView = findViewById(R.id.message_status);
        mTimeView = findViewById(R.id.time_text);
    }

    public void bind(final Cursor cursor, final HostInterface hostInterface) {
        mHostInterface = hostInterface;
        mData.bind(cursor, false);
        //设置头像
        setIcon(getContext(), mData.getIcon(), mIconView);
        //设置会话名称
        mTitleView.setText(mData.getName());
        //设置snippetText
        mSnippetView.setText(mData.getSnippetViewText(getContext()));
        //设置未读标记
        if (!mData.getIsRead()) {
            mReadStatusView.setVisibility(View.VISIBLE);
        } else {
            mReadStatusView.setVisibility(View.INVISIBLE);
        }
        String timeString = Dates.getConversationTimeString(getContext(), mData.getTimestamp()).toString();
        if (!TextUtils.isEmpty(timeString)) {
            mTimeView.setText(timeString);
        }
        final String previewContentType = mData.getShowDraft() ?
                mData.getDraftPreviewContentType() : mData.getPreviewContentType();
        this.setOnClickListener(this);
    }

    private void setIcon(Context context, String icon, ImageView imageView) {
        RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(icon);
        String iconPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_ICON_PATH);
        if (!TextUtils.isEmpty(iconPath)) {
            RcsImageHelper.setIcon(context, iconPath, imageView);
        }
    }

    /**
     * {@inheritDoc} from OnClickListener
     */
    @Override
    public void onClick(final View v) {
        processClick(v, false);
    }

    private boolean processClick(final View v, final boolean isLongClick) {
        if (mHostInterface != null) {
            mHostInterface.onConversationClicked(mData, isLongClick);
            return true;
        }
        return false;
    }
}
