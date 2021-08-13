package com.juphoon.chatbotmaap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotCardBean;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbot.RcsChatbotSuggestionsBean;
import com.juphoon.chatbotmaap.Location.RcsBaiduLocationActivity;
import com.juphoon.chatbotmaap.view.AlignTextView;
import com.juphoon.chatbotmaap.view.RoundImageView;
import com.juphoon.helper.RcsBitmapCache;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.other.AudioPlayHelper;
import com.juphoon.rcs.tool.RcsNetUtils;
import com.juphoon.service.RmsDefine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RcsChatbotAttachmentView extends FrameLayout {

    private final static String CARD_ORIENTATION_VERTICAL = "VERTICAL";
    private final static String CARD_ORIENTATION_HORIZONTAL = "HORIZONTAL";

    private Context mContext;
    private String mConversationId;
    private String mCookie;
    private String mAudioCookie;
    private String mRmsUri;
    private String mContributionId;
    private String mTrafficType;
    private String mServiceId;
    private String mCardJson;
    private SpannableString elipseString;//收起的文字
    //用来计算卡片文字占据的高度
    private int mSingleCardWidth;
    private int mMutipeCardWidth = (int) getResources().getDimension(R.dimen.public_mutipe_width_medium) - (int) getResources().getDimension(R.dimen.chatbot_card_marginleft)
            - (int) getResources().getDimension(R.dimen.chatbot_card_marginright);
    private int mSingleHorizontalCardWidth;
    //卡片标题内容字体大小已经dp转px
    private int mTitleTextSize = (int) getResources().getDimension(R.dimen.chatbot_card_title_textsize);
    private int mContentTextSize = (int) getResources().getDimension(R.dimen.chatbot_card_content_textsize);
    private int mCardShape = (int) getResources().getDimension(R.dimen.chatbot_card_button_radios);
    private int mSuggestionsSize = (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_height);

    private int mWidth;
    private int mHeight;
    private int mContentWidth;
    private int mTruncationHeight = (mSuggestionsSize + (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)) * 4
            + (int) getResources().getDimension(R.dimen.chatbot_card_title_margintop) + (int) getResources().getDimension(R.dimen.chatbot_card_titletop_marginbottom)
            + (int) getResources().getDimension(R.dimen.chatbot_card_content_margintop) + (int) getResources().getDimension(R.dimen.chatbot_card_content_marginbottom)
            + (int) getResources().getDimension(R.dimen.public_chatbot_single_height_tall)
            + (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_list_marginbottom);

    private static WeakReference<OnPickMediaViewListener> sCallbackRef;

    private static int RECORD_VIDEO = 0;
    private static int RECORD_AUDIO = 2;

    private RcsChatbotCardViewsAdapter adapter;

    public interface OnPickMediaViewListener {
        void pickMediaView(int selectedIndex);
    }

    public static void addPickMediaViewCallback(OnPickMediaViewListener callback) {
        sCallbackRef = new WeakReference<>(callback);
    }

    public RcsChatbotAttachmentView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void unbind() {
        stopAudioIfNeed();
    }

    public void bindMessagePartData(final String json, final String conversationid, String rmsUri, int padding) {
        stopAudioIfNeed();
        mCookie = UUID.randomUUID().toString();
        mConversationId = conversationid;
        mRmsUri = rmsUri;
        measureCardMaxWidth(padding);
        try {
            JSONObject jsonObject = new JSONObject(json);
            mServiceId = jsonObject.optString(RmsDefine.Rms.CHATBOT_SERVICE_ID);
            mCardJson = jsonObject.optString(RmsDefine.Rms.RMS_EXTRA);
            mTrafficType = jsonObject.optString(RmsDefine.Rms.TRAFFIC_TYPE);
            mContributionId = jsonObject.optString(RmsDefine.Rms.CONTRIBUTION_ID);
            RcsChatbotCardBean cardBean = new Gson().fromJson(mCardJson, RcsChatbotCardBean.class);
            // 移动要调整 title 和 Media 的位置
            dealCardTitleMediaPosition(mCardJson, cardBean);
            if (cardBean.message != null) {
                if (cardBean.message.generalPurposeCard != null) {
                    initCardView(cardBean.message);
                } else if (cardBean.message.generalPurposeCardCarousel != null) {
                    initListCardView(cardBean.message);//多卡片
                }
            } else {
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void measureCardMaxWidth(int padding) {
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mWidth = wm.getDefaultDisplay().getWidth() - getResources()
                .getDimensionPixelSize(R.dimen.conversation_message_contact_icon_size) - padding - getResources()
                .getDimensionPixelSize(R.dimen.message_bubble_arrow_width);
        //如果大于最大卡片宽度则 设置宽度为480dp
        int maxmun = getResources().getDimensionPixelSize(R.dimen.chatbot_card_maximum_width);
        if (mWidth > maxmun) {
            mWidth = maxmun;
        }
        //用来计算卡片文字占据的高度
        mSingleCardWidth = mWidth - (int) mContext.getResources().getDimension(R.dimen.chatbot_card_marginleft)
                - (int) mContext.getResources().getDimension(R.dimen.chatbot_card_marginright);
        mSingleHorizontalCardWidth = mWidth - (int) mContext.getResources().getDimension(R.dimen.chatbot_card_marginleft) -
                (int) mContext.getResources().getDimension(R.dimen.chatbot_card_marginright) - (int) mContext.getResources().getDimension(R.dimen.public_single_image_horizontal_width);
    }

    private void initCardView(RcsChatbotCardBean.MessageBean messageBean) {
        final RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card = messageBean.generalPurposeCard;
        boolean isHorizontal = false;
        boolean isRight = false; //水平卡片图片位置 false是左边
        ChatbotCardCSSBean cardCssBean = null;
        if (card.layout != null) {
            isHorizontal = TextUtils.equals(card.layout.cardOrientation, CARD_ORIENTATION_HORIZONTAL);
            String cssFile = getCSSFilePath(messageBean, false);
            cardCssBean = parseCSS(readCss(cssFile));
            setCSSTextSize(cardCssBean);
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.rcs_chatbot_single_card, this, false);
        view.setBackgroundResource(isSelected() ? R.drawable.shape_radius_selected_item : R.drawable.shape_radius_item);

        if (card.content.media == null || TextUtils.isEmpty(card.content.media.mediaUrl)) {
            final ConstraintLayout imageLayout = (ConstraintLayout) view.findViewById(R.id.card_image_layout);
            imageLayout.setVisibility(View.GONE);
            //设置纯文本宽高
            initChatbotPlainText(view, card);
        } else if (isHorizontal) {
            switch (card.layout.imageAlignment) {
                case "LEFT":
                    view = inflater.inflate(R.layout.public_chatbot_single_card_horizontal_left, this, false);
                    isRight = false;
                    break;
                case "RIGHT":
                    view = inflater.inflate(R.layout.public_chatbot_single_card_horizontal_right, this, false);
                    isRight = true;
                    break;
            }
            initChatbotHorizontal(view, card, isRight);
        } else {
            initChatbotVertical(view, card);
        }
        //新增css样式设置
        setCardStyle(cardCssBean, view, isHorizontal, isRight);
        parseFontStyle(card.layout.titleFontStyle, true, isHorizontal, view);
        parseFontStyle(card.layout.descriptionFontStyle, false, isHorizontal, view);

        FrameLayout moreLayout = (FrameLayout) findViewById(R.id.public_layout);
        moreLayout.removeAllViews();
        moreLayout.addView(view);
        RoundImageView imageView = setImageCorners(view, mCardShape, mCardShape, 0, 0);
        //处理media 和 suggestion
        if (card.content.media != null && card.content.media.mediaContentType.contains("audio")) {
            ImageView audioPlayImageView = (ImageView) view.findViewById(R.id.card_image_audio_play);
            initChatbotAudioView(card.content, imageView, audioPlayImageView);
        } else if (card.content.media != null && card.content.media.mediaContentType.contains("image")) {
            initChatbotImageView(card.content, imageView);
        } else if (card.content.media != null && card.content.media.mediaContentType.contains("video")) {
            ImageView audioPlayImageView = (ImageView) view.findViewById(R.id.card_image_audio_play);
            RcsChatbotVideoPlayer videoPlayer = (RcsChatbotVideoPlayer) view.findViewById(R.id.rcs_chatbot_video_player);
            initChatbotVideoView(card.content, imageView, videoPlayer, audioPlayImageView);
        }
        if (card.content.suggestions != null) {
            initSuggestionView(view, card.content.suggestions, cardCssBean);
        }
        if (card.content.media != null || card.content.title != null || card.content.suggestions != null) {
            view.setMinimumWidth(mWidth);
        }
    }

    private String getCSSFilePath(RcsChatbotCardBean.MessageBean messageBean, boolean isListCard) {
        String cssStyleUrl;
        String cssFile = "";
        if (isListCard) {
            cssStyleUrl = messageBean.generalPurposeCardCarousel.layout.style;
        } else {
            cssStyleUrl = messageBean.generalPurposeCard.layout.style;
        }

        if (!TextUtils.isEmpty(cssStyleUrl)) {
            cssFile = RcsFileDownloadHelper.getPathFromFileInfo(
                    new RcsFileDownloadHelper.FileInfo(cssStyleUrl).setConTenType("css/css"),
                    RmsDefine.RMS_CHATBO_PATH);
        } else {
            RcsChatbotHelper.RcsChatbot mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(mConversationId);
            if (mChatbotInfo != null) {
                RcsChatbotInfoBean mRcsChatbotInfoBean = new Gson().fromJson(mChatbotInfo.json, RcsChatbotInfoBean.class);
                cssFile = RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(mRcsChatbotInfoBean.genericCSStemplate).setConTenType("css/css"), RmsDefine.RMS_CHATBO_PATH);
            }
        }
        if (TextUtils.isEmpty(cssFile)) {
            tryDownloadCss(cssStyleUrl, isListCard, messageBean);
        } else {
            return cssFile;
        }
        return cssFile;
    }

    private void initListCardView(RcsChatbotCardBean.MessageBean messageBean) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        FrameLayout moreLayout = (FrameLayout) findViewById(R.id.public_layout);
        View view = inflater.inflate(R.layout.rcs_chatbot_mutiple_cards, this, false);
        moreLayout.removeAllViews();
        moreLayout.addView(view);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.cards_recycleview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RcsChatbotCardViewsAdapter(messageBean.generalPurposeCardCarousel.content);
        adapter.setCardLayoutParam(messageBean);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 根据 title 和 media 字段出现顺序来决定显示 - 移动要求
     */
    private void dealCardTitleMediaPosition(String cardJson, RcsChatbotCardBean cardBean) {
        if (cardBean.message.generalPurposeCard != null) {
            int title = cardJson.indexOf("title");
            int media = cardJson.indexOf("media");
            if (title > media || title == -1) {
                cardBean.message.generalPurposeCard.content.isTop = false;
            } else {
                cardBean.message.generalPurposeCard.content.isTop = true;
            }
        } else if (cardBean.message.generalPurposeCardCarousel != null) {
            int cardCount = cardBean.message.generalPurposeCardCarousel.content.size();
            try {
                JSONObject jsonObject = new JSONObject(cardJson);
                JSONArray contentArray = jsonObject.getJSONObject("message").getJSONObject("generalPurposeCardCarousel").getJSONArray("content");
                for (int i = 0; i < cardCount; i++) {
                    JSONObject object = contentArray.optJSONObject(i);
                    if (object != null) {
                        int title = object.toString().indexOf("title");
                        int media = object.toString().indexOf("media");
                        if (title > media || title == -1) {
                            cardBean.message.generalPurposeCardCarousel.content.get(i).isTop = false;
                        } else {
                            cardBean.message.generalPurposeCardCarousel.content.get(i).isTop = true;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAudioIfNeed();
    }

    private void stopAudioIfNeed() {
        if (TextUtils.equals(AudioPlayHelper.getInstance().getCookie(), mAudioCookie)) {
            AudioPlayHelper.getInstance().stop();
        }
        if (TextUtils.equals(RcsChatbotVideoPlayerManager.instance().getCookie(), mCookie)) {
            RcsChatbotVideoPlayerManager.instance().releaseNiceVideoPlayer();
        }
    }

    private void initSuggestionView(View view, List<RcsChatbotSuggestionsBean.SuggestionsBean> suggestions, ChatbotCardCSSBean chatbotCardCSSBean) {
        LinearLayout buttonLayout = (LinearLayout) view.findViewById(R.id.card_action);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        //设置按钮大小
        itemParams.setMargins(0, (int) mContext.getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)
                , 0, 0);
        for (final RcsChatbotSuggestionsBean.SuggestionsBean suggestion : suggestions) {
            if (suggestion != null) {
                if (suggestion.action != null) {
                    Button actionButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.rcs_chatbot_noside_button, null);
                    itemParams.height = mSuggestionsSize;
                    setSuggestionFontText(chatbotCardCSSBean, actionButton);
                    actionButton.setLayoutParams(itemParams);
                    actionButton.setText(suggestion.action.displayText);
                    actionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
                            replyBean.response = new RcsChatbotReplyBean.ResponseBean();
                            replyBean.response.action = new RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean();
                            replyBean.response.action.displayText = suggestion.action.displayText;
                            replyBean.response.action.postback = suggestion.action.postback;
                            Intent intent = new Intent();
                            intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
                            intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationId);
                            intent.putExtra(RcsChatbotDefine.KEY_RMSURI, mRmsUri);
                            intent.setAction(RcsChatbotDefine.ACTION_SUGGESTION);
                            intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                            getContext().sendBroadcast(intent);
                            dealSugesstion(suggestion.action, getContext(), mConversationId, mServiceId, mRmsUri, mTrafficType, mContributionId);
                        }
                    });
                    buttonLayout.addView(actionButton);
                }
                if (suggestion.reply != null) {
                    final Button replyButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.rcs_chatbot_noside_button, null);
                    itemParams.height = mSuggestionsSize;
                    replyButton.setLayoutParams(itemParams);
                    replyButton.setText(suggestion.reply.displayText);
                    setSuggestionFontText(chatbotCardCSSBean, replyButton);
                    replyButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
                            replyBean.response = new RcsChatbotReplyBean.ResponseBean();
                            replyBean.response.reply = new RcsChatbotSuggestionsBean.SuggestionsBean.ReplyBean();
                            replyBean.response.reply.displayText = suggestion.reply.displayText;
                            replyBean.response.reply.postback = suggestion.reply.postback;
                            Intent intent = new Intent();
                            intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
                            intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationId);
                            intent.putExtra(RcsChatbotDefine.KEY_RMSURI, mRmsUri);
                            intent.setAction(RcsChatbotDefine.ACTION_REPLY);
                            intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                            getContext().sendBroadcast(intent);
                        }
                    });
                    buttonLayout.addView(replyButton);
                }
            }
        }
    }

    /**
     * 卡片的音频
     *
     * @param contentBean
     * @param imageView
     * @param audioPlayImageView
     */
    private void initChatbotAudioView(final RcsChatbotCardBean.MessageBean.ContentBean contentBean, final ImageView imageView, final ImageView audioPlayImageView) {
        audioPlayImageView.setVisibility(View.VISIBLE);
        // 下载音频文件，已经下载不会再去下载
        final RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(contentBean.media.mediaUrl, contentBean.media.mediaFileSize).setConTenType(contentBean.media.mediaContentType);
        tryDownloadMedia(fileInfo);
        // 下载缩略图，已经下载不会再去下载
        mAudioCookie = contentBean.media.mediaUrl;
        dealShowThumbImage(imageView, contentBean.media.thumbnailUrl, contentBean.media.thumbnailContentType, contentBean.media.thumbnailFileSize, false);
        if (TextUtils.equals(AudioPlayHelper.getInstance().getCookie(), mAudioCookie)) {
            audioPlayImageView.setImageResource(AudioPlayHelper.getInstance().getState() == AudioPlayHelper.STATE_PLAYING ? R.drawable.pause : R.drawable.play);
        } else {
            audioPlayImageView.setImageResource(R.drawable.play);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioCookie = contentBean.media.mediaUrl;
                String audioPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_CHATBO_PATH);
                if (TextUtils.isEmpty(audioPath)) {
                    Toast.makeText(getContext(), "未下载完成", Toast.LENGTH_SHORT).show();
                    RcsFileDownloadHelper.clearLastDownloadTime(contentBean.media.mediaUrl);
                    tryDownloadMedia(fileInfo);
                } else {
                    RcsChatbotVideoPlayerManager.instance().releaseNiceVideoPlayer();
                    if (TextUtils.equals(AudioPlayHelper.getInstance().getCookie(), mAudioCookie)) {
                        if (AudioPlayHelper.getInstance().getState() == AudioPlayHelper.STATE_PLAYING) {
                            AudioPlayHelper.getInstance().pause();
                            audioPlayImageView.setImageResource(R.drawable.play);
                        } else {
                            AudioPlayHelper.getInstance().play();
                            audioPlayImageView.setImageResource(R.drawable.pause);
                        }
                    } else {
                        AudioPlayHelper.getInstance().start(getContext(), audioPath, new AudioPlayHelper.Callback() {
                            @Override
                            public void onStateChange(int state) {
                                String thumbPath = RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(contentBean.media.thumbnailUrl).setConTenType(contentBean.media.thumbnailContentType), RmsDefine.RMS_THUMB_PATH);
                                if (TextUtils.isEmpty(thumbPath)) {
                                    return;
                                }
                                if (state == AudioPlayHelper.STATE_PLAYING) {
                                    audioPlayImageView.setImageResource(R.drawable.pause);
                                } else {
                                    audioPlayImageView.setImageResource(R.drawable.play);
                                }
                            }

                            @Override
                            public void onProgress(int current, int duration) {

                            }
                        }, mAudioCookie);
                    }
                }
            }
        });
    }

    /**
     * 封装一次媒体下载函数
     */
    private void tryDownloadMedia(RcsFileDownloadHelper.FileInfo fileInfo) {
        RcsFileDownloadHelper.downloadFile("",
                fileInfo,
                new RcsFileDownloadHelper.Callback() {
                    @Override
                    public void onDownloadResult(String cookie, boolean succ, String filePath) {
                    }
                }, null, RmsDefine.RMS_CHATBO_PATH);
    }

    public void tryDownloadCss(String mediaUrl, final boolean isListCard, final RcsChatbotCardBean.MessageBean messageBean) {
        RcsFileDownloadHelper.downloadFile(mCookie,
                new RcsFileDownloadHelper.FileInfo(mediaUrl).setConTenType("css/css"),
                new RcsFileDownloadHelper.Callback() {
                    @Override
                    public void onDownloadResult(String cookie, boolean succ, String filePath) {
                        if (!succ) return;
                        if (!TextUtils.equals(cookie, mCookie)) return;
                        if (adapter != null && isListCard) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    initListCardView(messageBean);
                                }
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    initCardView(messageBean);
                                }
                            });
                        }
                    }
                }, null, RmsDefine.RMS_CHATBO_PATH);
    }

    private void dealShowThumbImage(final ImageView imageView, String thumbUrl, String thumbType, int thumbSize, final boolean circle) {
        if (!TextUtils.isEmpty(thumbUrl)) {
            RcsFileDownloadHelper.FileInfo thumbInfo = new RcsFileDownloadHelper.FileInfo(thumbUrl, thumbSize).setConTenType(thumbType);
            String thumbPath = RcsFileDownloadHelper.getPathFromFileInfo(thumbInfo, RmsDefine.RMS_THUMB_PATH);
            if (TextUtils.isEmpty(thumbPath)) {
                RcsFileDownloadHelper.downloadFile("", thumbInfo, new RcsFileDownloadHelper.Callback() {
                    @Override
                    public void onDownloadResult(String cookie, boolean succ, String filePath) {
                        if (succ) {
                            RcsBitmapCache.getBitmapFromPath(imageView, filePath, circle);
                        }
                    }
                }, null, RmsDefine.RMS_THUMB_PATH);
            } else {
                RcsBitmapCache.getBitmapFromPath(imageView, thumbPath, circle);
            }
        }
    }

    private void initChatbotImageView(final RcsChatbotCardBean.MessageBean.ContentBean contentBean, final ImageView imageView) {
        // 缩略图不清楚
        dealShowThumbImage(imageView, contentBean.media.thumbnailUrl, contentBean.media.thumbnailContentType, contentBean.media.thumbnailFileSize, false);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(contentBean.media.mediaUrl, contentBean.media.mediaFileSize).setConTenType(contentBean.media.mediaContentType);
                if (TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_CHATBO_PATH))) {
                    RcsFileDownloadHelper.downloadFile("", fileInfo,
                            new RcsFileDownloadHelper.Callback() {
                                @Override
                                public void onDownloadResult(String cookie, boolean succ, final String filePath) {
                                    if (succ) {
                                        Intent intent = new Intent(getContext(), RcsChatbotImageFragmentActivity.class);
                                        intent.putExtra(RcsChatbotImageFragmentActivity.FILE_PATH, RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(contentBean.media.mediaUrl).setConTenType(contentBean.media.mediaContentType), RmsDefine.RMS_CHATBO_PATH));
                                        getContext().startActivity(intent);
                                    }
                                }
                            }, null, RmsDefine.RMS_CHATBO_PATH);
                    return;
                }
                Intent intent = new Intent(getContext(), RcsChatbotImageFragmentActivity.class);
                intent.putExtra(RcsChatbotImageFragmentActivity.FILE_PATH, RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(contentBean.media.mediaUrl).setConTenType(contentBean.media.mediaContentType), RmsDefine.RMS_CHATBO_PATH));
                getContext().startActivity(intent);
            }
        });
    }

    private void initChatbotVideoView(final RcsChatbotCardBean.MessageBean.ContentBean contentBean, final ImageView imageView, final RcsChatbotVideoPlayer videoPlayer, final ImageView playButton) {
        final RcsFileDownloadHelper.FileInfo thumbFileInfo = new RcsFileDownloadHelper.FileInfo(contentBean.media.thumbnailUrl, contentBean.media.thumbnailFileSize).setConTenType(contentBean.media.thumbnailContentType);
        String thumble_path = RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH);
        playButton.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(thumble_path)) {
            //合成图片可能会出现解析度过低的问题
//            Bitmap bmp = RcsChatbotImageUtil.createWaterMaskImage(BitmapFactory.decodeFile(thumble_path), BitmapFactory.decodeResource(getContext().getResources(), R.drawable.play));
            imageView.setImageBitmap(BitmapFactory.decodeFile(thumble_path));
        } else if (RcsNetUtils.checkNet(getContext())) {
            RcsFileDownloadHelper.downloadFile("", thumbFileInfo, new RcsFileDownloadHelper.Callback() {
                @Override
                public void onDownloadResult(String cookie, boolean succ, String filePath) {
                    if (succ) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
//                                Bitmap bmp = RcsChatbotImageUtil.createWaterMaskImage(BitmapFactory.decodeFile(RcsChatbotDownloadHelper.getPathFromUrl(contentBean.media.thumbnailUrl, contentBean.media.thumbnailContentType)), BitmapFactory.decodeResource(getContext().getResources(), R.drawable.play));
                                imageView.setImageBitmap(BitmapFactory.decodeFile(RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH)));

                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "下载失败请重试", Toast.LENGTH_SHORT).show();
                    }
                }
            }, null, RmsDefine.RMS_THUMB_PATH);
        }
        final RcsFileDownloadHelper.FileInfo mediaInfo = new RcsFileDownloadHelper.FileInfo(contentBean.media.mediaUrl, contentBean.media.mediaFileSize).setConTenType(contentBean.media.mediaContentType);
        tryDownloadMedia(mediaInfo);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!RcsNetUtils.checkNet(getContext()) && TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH))) {
                    Toast.makeText(getContext(), "当前无网络。", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(mediaInfo, RmsDefine.RMS_CHATBO_PATH))) {
                    Toast.makeText(getContext(), "未下载完成", Toast.LENGTH_SHORT).show();
                    tryDownloadMedia(mediaInfo);
                } else {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            AudioPlayHelper.getInstance().stop();
                            playButton.setVisibility(View.GONE);
                            imageView.setVisibility(View.GONE);
                            videoPlayer.setVisibility(View.VISIBLE);
                            videoPlayer.setPlayerType(RcsChatbotVideoPlayer.TYPE_NATIVE);
                            videoPlayer.setUp(RcsFileDownloadHelper.getPathFromFileInfo(mediaInfo, RmsDefine.RMS_CHATBO_PATH), null);
                            JuphoonVideoPlayerController controller = new JuphoonVideoPlayerController(getContext());
                            if (!TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH))) {
                                controller.setImage(RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH));
                            }
                            videoPlayer.setController(controller);
                            RcsChatbotVideoPlayerManager.instance().setCookie(mCookie);
                            videoPlayer.start();
                        }
                    });
                }
            }
        });
    }

    private void initChatbotVertical(View view, RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card) {
        final AlignTextView contentView = (AlignTextView) view.findViewById(R.id.card_content);
        AlignTextView titleView = (AlignTextView) view.findViewById(R.id.card_title);
        //当图片高度为short时独特的标题
        AlignTextView titleTop = (AlignTextView) view.findViewById(R.id.card_title_top);
        RoundImageView imageView = (RoundImageView) view.findViewById(R.id.card_image);
        RcsChatbotVideoPlayer videoPlayer = (RcsChatbotVideoPlayer) view.findViewById(R.id.rcs_chatbot_video_player);

        //判断卡片主题是否为空 为空则内容可以有2行
        int sumHeight = 0;
        int titleHeight = 0;
        int descriptionHeight = 0;
        int mediaHeight = 0;
        TextView textView = new TextView(mContext);
        //卡片的截取高度 默认是选标题顶部 标题顶部比正常标题高了8dp
        //截取高度 = 一行标题的高度 + 一行内容的高度 + 4个按钮的高度 + 4个按钮的间距 + 标题内容和底部的间距 + 最大图片的高度
        int truncationHeight = getStaticLayout(textView, "title", 20, mSingleCardWidth).getHeight()
                + getStaticLayout(textView, "description", 16, mSingleCardWidth).getHeight()
                + mTruncationHeight;
        contentView.setMinWidth(mSingleCardWidth);
        titleView.setMinWidth(mSingleCardWidth);
        titleTop.setMinWidth(mSingleCardWidth);
        if (!TextUtils.isEmpty(card.content.title)) {
            if (card.content.isTop) {
                //顶部标题多8dp
                sumHeight += (int) getResources().getDimension(R.dimen.chatbot_card_titletop_marginbottom);
                titleHeight = getCardTitleHeight(titleTop, card.content.title, mSingleCardWidth);
            } else {
                titleHeight = getCardTitleHeight(titleView, card.content.title, mSingleCardWidth);
            }
            //内容上边距16dp
            sumHeight += titleHeight + (int) getResources().getDimension(R.dimen.chatbot_card_title_margintop);
        }
        if (!TextUtils.isEmpty(card.content.description)) {
            descriptionHeight = getCardDescriptionHeight(contentView, card.content.description, mSingleCardWidth);
            //标题上下间距 8dp 2dp
            sumHeight += descriptionHeight + (int) getResources().getDimension(R.dimen.chatbot_card_content_margintop) + (int) getResources().getDimension(R.dimen.chatbot_card_content_marginbottom);
        }

        //判断图片高度
        if (card.content.media != null) {
            mediaHeight = setImageHeight(card.content.media.height, imageView, false, videoPlayer);
        } else {
            mediaHeight = 0;
        }
        sumHeight += mediaHeight;
        //卡片底部需要留8dp 有无按钮都需要留
        sumHeight += (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_list_marginbottom);
        if (card.content.suggestions != null) {
            sumHeight += (mSuggestionsSize + (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)) * card.content.suggestions.size();
        }
        if (sumHeight > truncationHeight) {
            if (card.content.isTop) {
                truncationCard(contentView, titleTop, sumHeight, truncationHeight, descriptionHeight, titleHeight, mSingleCardWidth, card.content);
            } else {
                truncationCard(contentView, titleView, sumHeight, truncationHeight, descriptionHeight, titleHeight, mSingleCardWidth, card.content);
            }
            sumHeight = truncationHeight;
        }
        view.setMinimumHeight(sumHeight);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.height = sumHeight;
        view.setLayoutParams(itemParams);
        mHeight = sumHeight - mediaHeight;
    }

    private void initChatbotHorizontal(View view, RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card, boolean isRight) {
        AlignTextView contentView = (AlignTextView) view.findViewById(R.id.card_content_horizontal);
        AlignTextView titleView = (AlignTextView) view.findViewById(R.id.card_title_horizontal);
        RoundImageView imageView = (RoundImageView) view.findViewById(R.id.card_image);
        RcsChatbotVideoPlayer videoPlayer = (RcsChatbotVideoPlayer) view.findViewById(R.id.rcs_chatbot_video_player);
        if (isRight) {
            imageView.setCorners(0, mCardShape, 0, 0);
        } else {
            imageView.setCorners(mCardShape, 0, 0, 0);
        }
        contentView.setMinWidth(mSingleHorizontalCardWidth);
        titleView.setMinWidth(mSingleHorizontalCardWidth);
        int sumHeight = 0;
        int titleHeight = 0;
        int descriptionHeight = 0;

        TextView textView = new TextView(mContext);
        //截取高度 = 一行标题的高度 + 一行内容的高度 + 4个按钮的高度 + 4个按钮的间距 + 标题内容的间距 + 按钮底部间距
        int truncationHeight = getStaticLayout(textView, "title", 20, mSingleHorizontalCardWidth).getHeight()
                + getStaticLayout(textView, "description", 16, mSingleHorizontalCardWidth).getHeight()
                + mTruncationHeight - (int) getResources().getDimension(R.dimen.chatbot_card_titletop_marginbottom)
                - (int) mContext.getResources().getDimension(R.dimen.public_chatbot_single_height_tall);
        if (!TextUtils.isEmpty(card.content.title)) {
            titleHeight = getCardTitleHeight(titleView, card.content.title, mSingleHorizontalCardWidth);
            //内容上边距16dp
            sumHeight += titleHeight + (int) getResources().getDimension(R.dimen.chatbot_card_title_margintop);
        }
        if (!TextUtils.isEmpty(card.content.description)) {
            descriptionHeight = getCardDescriptionHeight(contentView, card.content.description, mSingleHorizontalCardWidth);
            //标题上下间距 8dp 2dp
            sumHeight += descriptionHeight + (int) getResources().getDimension(R.dimen.chatbot_card_content_margintop) + (int) getResources().getDimension(R.dimen.chatbot_card_content_marginbottom);
        }
        sumHeight += (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_list_marginbottom);
        if (card.content.suggestions != null) {
            //单个按钮顶部距离6dp
            sumHeight += (mSuggestionsSize + (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)) * card.content.suggestions.size();
        }
        int mediaHeight;
        if (card.content.media != null) {
            mediaHeight = setImageHeight(card.content.media.height, imageView, true, videoPlayer);
        } else {
            mediaHeight = 0;
        }
        //卡片的高度大于截断高度
        if (sumHeight >= truncationHeight) {
            //图片高度超过截断高度 按图片高度来截断
            if (mediaHeight >= truncationHeight) {
                view.setMinimumHeight(mediaHeight);
                truncationCard(contentView, titleView, sumHeight, mediaHeight, descriptionHeight, titleHeight, mSingleHorizontalCardWidth, card.content);
                //图片高度小于截断高度 按截断高度来截断
            } else if (mediaHeight < truncationHeight) {
                //按截断高度截断
                view.setMinimumHeight(truncationHeight);
                //图片高度小于截断高度 按截断高度来截断
                truncationCard(contentView, titleView, sumHeight, truncationHeight, descriptionHeight, titleHeight, mSingleHorizontalCardWidth, card.content);
            }
            //图片高度大于卡片高度 卡片高度小于截断高度
        } else if (mediaHeight > sumHeight) {
            view.setMinimumHeight(mediaHeight);
            imageView.setCorners(mCardShape, 0, 0, mCardShape);
        }
        mHeight = mediaHeight;
        mContentWidth = mWidth - (int) mContext.getResources().getDimension(R.dimen.public_single_image_horizontal_width);
    }

    private void initChatbotPlainText(View view, RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card) {
        final AlignTextView contentView = (AlignTextView) view.findViewById(R.id.card_content);
        final AlignTextView titleView = (AlignTextView) view.findViewById(R.id.card_title);
        TextView textView = new TextView(mContext);
        int sumHeight = 0;
        int titleHeight = 0;
        int descriptionHeight = 0;
        int truncationHeight = getStaticLayout(textView, "title", mTitleTextSize, mSingleCardWidth).getHeight()
                + getStaticLayout(textView, "description", mContentTextSize, mSingleCardWidth).getHeight()
                + mTruncationHeight - (int) getResources().getDimension(R.dimen.chatbot_card_content_margintop);
        contentView.setMinWidth(mSingleCardWidth);
        titleView.setMinWidth(mSingleCardWidth);
        if (!TextUtils.isEmpty(card.content.title)) {
            titleHeight = getCardTitleHeight(titleView, card.content.title, mSingleCardWidth);
            //内容上边距16dp
            sumHeight += titleHeight + (int) getResources().getDimension(R.dimen.chatbot_card_title_margintop);
        }
        if (!TextUtils.isEmpty(card.content.description)) {
            descriptionHeight = getCardDescriptionHeight(contentView, card.content.description, mSingleCardWidth);
            //标题上下间距 8dp 2dp
            sumHeight += descriptionHeight + (int) getResources().getDimension(R.dimen.chatbot_card_content_margintop) + (int) getResources().getDimension(R.dimen.chatbot_card_content_marginbottom);
        }
        //如果只有内容没有按钮 底部需要增加8dp 如果有按钮底部也需要增加8dp 所以不隐藏buttonlist
        sumHeight += (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_list_marginbottom);
        //计算按钮高度
        if (card.content.suggestions != null) {
            sumHeight += (mSuggestionsSize + (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)) * card.content.suggestions.size();
        }
        if (sumHeight > truncationHeight) {
            truncationCard(contentView, titleView, sumHeight, truncationHeight, descriptionHeight, titleHeight, mSingleCardWidth, card.content);
            sumHeight = truncationHeight;
        }
        view.setMinimumHeight(sumHeight);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.height = sumHeight;
        view.setLayoutParams(itemParams);
        mHeight = sumHeight;
    }

    public static void dealSugesstion(final RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean action, final Context context, final String conversationId, final String serviceId, final String rmsUri, final String trafficType, final String contributionId) {
        if (action.urlAction != null && action.urlAction.openUrl != null && RcsNetUtils.checkNet(context)) {
            String url = action.urlAction.openUrl.url;
            Intent intent;
            if (TextUtils.isEmpty(url)) {
                return;
            }
            if (TextUtils.equals(action.urlAction.openUrl.application, "browser")) {
                Uri uri = Uri.parse(url);
                if (uri.getScheme() != null) {
                    try {
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, "找不到要打开的应用程序", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                intent = new Intent(context, RcsChatbotWebActivity.class);
                intent.putExtra(RcsChatbotWebActivity.URL, url);
                intent.putExtra(RcsChatbotWebActivity.PARAMETERS, action.urlAction.openUrl.parameters);
                intent.putExtra(RcsChatbotWebActivity.VIEWMODE, action.urlAction.openUrl.viewMode);
                context.startActivity(intent);
            }
        } else if (action.dialerAction != null) {
            if (action.dialerAction.dialPhoneNumber != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                Uri data = Uri.parse("tel:" + action.dialerAction.dialPhoneNumber.phoneNumber);
                intent.setData(data);
                context.startActivity(intent);
            } else if (action.dialerAction.dialEnrichedCall != null) {
                // TODO
                Log.d("Chatbot", "dialEnrichedCall not support");
            } else if (action.dialerAction.dialVideoCall != null) {
                // TODO
                Log.d("Chatbot", "dialVideoCall not support");
            }
        } else if (action.mapAction != null) {
            if (action.mapAction.showLocation != null) {
                Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
                intent.putExtra(RcsBaiduLocationActivity.LOCATION_LATITUDE, action.mapAction.showLocation.location.latitude);
                intent.putExtra(RcsBaiduLocationActivity.CONVERSATIONID, conversationId);
                intent.putExtra(RcsBaiduLocationActivity.LOCATION_LONGITUDE, action.mapAction.showLocation.location.longitude);
                context.startActivity(intent);
                //显示地理位置 要求网可用
            } else if (action.mapAction.requestLocationPush != null && RcsNetUtils.checkNet(context)) {
                //发送地理位置+
                if (Build.VERSION.SDK_INT >= 23) {
                    //申请权限android.permission.WRITE_CALENDAR
                    RcsChatbotPermission.with(context)
                            .permisson(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
                            .callback(new RcsChatbotPermissionActivity.PermissionCallback() {
                                //权限通过
                                @Override
                                public void onPermissionGranted() {
                                    Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
                                    intent.putExtra(RcsBaiduLocationActivity.CONVERSATIONID, conversationId);
                                    context.startActivity(intent);
                                }

                                //被拒绝
                                @Override
                                public void shouldShowRational(String permisson) {
                                }

                                //被拒绝且勾选不再提示
                                @Override
                                public void onPermissonReject(String permisson) {
                                }
                            }).request();
                } else {
                    Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
                    intent.putExtra(RcsBaiduLocationActivity.CONVERSATIONID, conversationId);
                    context.startActivity(intent);
                }
            }
        } else if (action.calendarAction != null) {
            if (action.calendarAction.createCalendarEvent != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    //申请权限android.permission.WRITE_CALENDAR
                    RcsChatbotPermission.with(context)
                            .permisson(new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR})
                            .callback(new RcsChatbotPermissionActivity.PermissionCallback() {
                                //权限通过
                                @Override
                                public void onPermissionGranted() {
                                    RcsChatbotCalendarReminderUtils.addCalendarEvent(context, action.calendarAction.createCalendarEvent.title, action.calendarAction.createCalendarEvent.description, action.calendarAction.createCalendarEvent.startTime, action.calendarAction.createCalendarEvent.endTime);
                                }

                                //被拒绝
                                @Override
                                public void shouldShowRational(String permisson) {
                                }

                                //被拒绝且勾选不再提示
                                @Override
                                public void onPermissonReject(String permisson) {
                                }
                            }).request();
                } else {
                    RcsChatbotCalendarReminderUtils.addCalendarEvent(context, action.calendarAction.createCalendarEvent.title, action.calendarAction.createCalendarEvent.description, action.calendarAction.createCalendarEvent.startTime, action.calendarAction.createCalendarEvent.endTime);
                }
            }
        } else if (action.composeAction != null) {
            if (action.composeAction.composeTextMessage != null) {
                //写一条rcs消息 有号码和 text
                String url = "sms:" + action.composeAction.composeTextMessage.phoneNumber;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra("sms_body", action.composeAction.composeTextMessage.text);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
            } else if (action.composeAction.composeRecordingMessage != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String url = "sms:" + action.composeAction.composeRecordingMessage.phoneNumber;
                int index;
                if (TextUtils.equals(action.composeAction.composeRecordingMessage.type, "VIDEO")) {
                    index = RECORD_VIDEO;
                } else if (TextUtils.equals(action.composeAction.composeRecordingMessage.type, "AUDIO")) {
                    index = RECORD_AUDIO;
                } else {
                    return;
                }
                intent.putExtra("composeMessageIndex", index);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
            }
        } else if (action.deviceAction != null && RcsNetUtils.checkNet(context)) {
            if (action.deviceAction.requestDeviceSpecifics != null) {
                new AlertDialog.Builder(context,R.style.Juphoon_Dialog_style).setTitle(R.string.chatbot_share_user_date)
                        .setPositiveButton(R.string.chatbot_sure, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.putExtra(RcsChatbotDefine.KEY_RMSURI, rmsUri);
                                intent.putExtra(RcsChatbotDefine.KEY_CHATBOT_ID,serviceId);
                                intent.setAction(RcsChatbotDefine.ACTION_SHARE_DATA);
                                intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                                context.sendBroadcast(intent);

                            }
                        }).setNegativeButton(R.string.chatbot_cancel, null).show();
            }
        } else if (action.settingsAction != null) {
            if (action.settingsAction.disableAnonymization != null) {
                // TODO
                Log.d("Chatbot", "disableAnonymization not support");
            } else if (action.settingsAction.enableDisplayedNotifications != null) {
                // TODO
                Log.d("Chatbot", "enableDisplayedNotifications not support");
            }
        }
    }

    /************* RcsChatbotCardViewsAdapter *************/

    private class RcsChatbotCardViewsAdapter extends RecyclerView.Adapter<RcsChatbotCardViewsAdapter.VH> {
        private List<RcsChatbotCardBean.MessageBean.ContentBean> mListContentBeans;
        private ArrayList<Integer> maxDescriptionLine = new ArrayList();
        private ArrayList<Integer> maxTitleLine = new ArrayList();
        private List<String> mTitleFontStyle;
        private List<String> mDescriptionStyle;
        private ChatbotCardCSSBean mChatbotCardCSSBean = null;

        int mMaxheight;

        public RcsChatbotCardViewsAdapter(List<RcsChatbotCardBean.MessageBean.ContentBean> listContentBeans) {
            this.mListContentBeans = listContentBeans;
            AlignTextView textView = new AlignTextView(mContext);
            int truncationHeight = getStaticLayout(textView, "title", mTitleTextSize, mMutipeCardWidth).getHeight()
                    + getStaticLayout(textView, "description", mContentTextSize, mMutipeCardWidth).getHeight()
                    + mTruncationHeight;
            for (RcsChatbotCardBean.MessageBean.ContentBean contentBean : listContentBeans) {
                int titleHeight = 0;
                int descriptionHeight = 0;
                int sumHeight = 0;
                if (!TextUtils.isEmpty(contentBean.description)) {
                    descriptionHeight = getCardDescriptionHeight(textView, contentBean.description, mMutipeCardWidth);
                    //标题上下间距 8dp 2dp
                    sumHeight += descriptionHeight + (int) getResources().getDimension(R.dimen.chatbot_card_content_margintop) + (int) getResources().getDimension(R.dimen.chatbot_card_content_marginbottom);
                }
                if (!TextUtils.isEmpty(contentBean.title)) {
                    titleHeight = getCardTitleHeight(textView, contentBean.title, mMutipeCardWidth);
                    //内容上边距16dp
                    sumHeight += titleHeight + (int) getResources().getDimension(R.dimen.chatbot_card_title_margintop);
                }
                if (contentBean.media != null) {
                    sumHeight += setImageHeight(contentBean.media.height, null, false, null);
                    if (contentBean.isTop) {
                        //标题在顶部需要额外加8dp
                        sumHeight += (int) getResources().getDimension(R.dimen.chatbot_card_titletop_marginbottom);
                    }
                }
                sumHeight += (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_list_marginbottom);
                //计算卡片按钮高度
                if (contentBean.suggestions != null) {
                    sumHeight += (mSuggestionsSize + (int) getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)) * contentBean.suggestions.size();
                }
                if (sumHeight <= truncationHeight) {
                    if (mMaxheight < sumHeight) {
                        mMaxheight = sumHeight;
                    }
                    maxDescriptionLine.add(0);
                    maxTitleLine.add(0);
                } else {
                    mMaxheight = truncationHeight;
                    sumHeight -= descriptionHeight;
                    int contentLineHeight = 1;
                    int titleLineHeight = 1;
                    if (descriptionHeight != 0) {
                        StaticLayout contentStaticLayout = getStaticLayout(textView, contentBean.description, mContentTextSize, mMutipeCardWidth);
                        contentLineHeight = contentStaticLayout.getHeight() / contentStaticLayout.getLineCount();
                    }
                    if (titleHeight != 0) {
                        StaticLayout titleStaticLayout = getStaticLayout(textView, contentBean.title, mTitleTextSize, mMutipeCardWidth);
                        titleLineHeight = titleStaticLayout.getHeight() / titleStaticLayout.getLineCount();
                    }
                    if (sumHeight + contentLineHeight > truncationHeight) {
                        sumHeight = sumHeight + contentLineHeight - titleHeight;
                        int line = (truncationHeight - sumHeight) / titleLineHeight;
                        maxDescriptionLine.add(1);
                        maxTitleLine.add(line > 0 ? line : 1);
                    } else {
                        maxDescriptionLine.add((truncationHeight - sumHeight) / contentLineHeight);
                        maxTitleLine.add(0);
                    }
                }
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.rcs_chatbot_single_card, parent, false);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            itemParams.width = (int) mContext.getResources().getDimension(R.dimen.public_mutipe_width_medium);
            view.setLayoutParams(itemParams);
            view.setBackgroundResource(RcsChatbotAttachmentView.this.isSelected() ? R.drawable.shape_radius_selected_item : R.drawable.shape_radius_item);
            float scale = 8 * mContext.getResources().getDisplayMetrics().density + 0.5f;
            itemParams.setMargins(0, 0, (int) scale, 8);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(final RcsChatbotCardViewsAdapter.VH holder, final int position) {
            holder.setIsRecyclable(false);
            holder.flag = true;
            holder.audioPlayImageView.setVisibility(View.INVISIBLE);
            holder.mCardTitle.setText(mListContentBeans.get(position).title);
            holder.mCardTitleTop.setText(mListContentBeans.get(position).title);
            holder.mImageView.setCorners(mCardShape, mCardShape, 0, 0);
            holder.mCardLayout.setMinimumHeight(mMaxheight);
            holder.mCardContent.setMinWidth(mMutipeCardWidth);
            holder.mCardTitle.setMinWidth(mMutipeCardWidth);
            holder.mCardTitleTop.setMinWidth(mMutipeCardWidth);
            RcsChatbotCardBean.MessageBean.ContentBean contentBean = mListContentBeans.get(position);
            mWidth = (int) mContext.getResources().getDimension(R.dimen.public_mutipe_width_medium);
            if (contentBean.media != null) {
                mHeight = mMaxheight - setImageHeight(contentBean.media.height, null, false, holder.mRcsChatbotVideoPlayer);
            } else {
                mHeight = mMaxheight;
            }
            setCardStyle(mChatbotCardCSSBean, holder.itemView, false,false);
            parseFontStyle(mTitleFontStyle, true, false, holder.itemView);
            parseFontStyle(mDescriptionStyle, false, false, holder.itemView);
            if (contentBean.media != null) {
                setImageHeight(contentBean.media.height, holder.mImageView, false, holder.mRcsChatbotVideoPlayer);
            }
            if (contentBean.media != null && contentBean.media.mediaContentType.contains("audio")) {
                initChatbotAudioView(contentBean, holder.mImageView, holder.audioPlayImageView);
            } else if (contentBean.media != null && contentBean.media.mediaContentType.contains("image")) {
                initChatbotImageView(contentBean, holder.mImageView);
            } else if (contentBean.media != null && contentBean.media.mediaContentType.contains("video")) {
                initChatbotVideoView(contentBean, holder.mImageView, holder.mRcsChatbotVideoPlayer, holder.audioPlayImageView);
            } else {
                holder.mImageLayout.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(contentBean.title)) {
                holder.mCardTitle.setVisibility(View.VISIBLE);
                if (contentBean.isTop) {
                    holder.mCardTitle.setVisibility(View.GONE);
                    holder.mCardTitleTop.setVisibility(View.VISIBLE);
                    if (maxTitleLine.get(position) > 0) {
                        holder.mCardTitleTop.setMaxLines(maxTitleLine.get(position));
                    }
                } else if (maxTitleLine.get(position) > 0) {
                    holder.mCardTitle.setMaxLines(maxTitleLine.get(position));
                }
            }
            if (!TextUtils.isEmpty(contentBean.description)) {
                holder.mCardContent.setVisibility(View.VISIBLE);
                holder.mCardContent.setText(contentBean.description);
                if (maxDescriptionLine.get(position) > 0) {
                    getLastIndexForLimit(holder.mCardContent, maxDescriptionLine.get(position), contentBean, mContentTextSize, mMutipeCardWidth);
                }
            }
            holder.buttonLayout.removeAllViews();
            if (contentBean.suggestions != null) {
                initSuggestionView(holder.buttonLayout, contentBean.suggestions, mChatbotCardCSSBean);
            }
        }

        @Override
        public int getItemCount() {
            return mListContentBeans.size();
        }

        public void setCardLayoutParam(RcsChatbotCardBean.MessageBean messageBean) {
            if (messageBean.generalPurposeCardCarousel.layout == null) return;
            mTitleFontStyle = messageBean.generalPurposeCardCarousel.layout.titleFontStyle;
            mDescriptionStyle = messageBean.generalPurposeCardCarousel.layout.descriptionFontStyle;
            String cssFile = getCSSFilePath(messageBean, true);
            mChatbotCardCSSBean = parseCSS(readCss(cssFile));
            setCSSTextSize(mChatbotCardCSSBean);
        }

        class VH extends RecyclerView.ViewHolder {
            AlignTextView mCardTitle, mCardTitleTop;
            AlignTextView mCardContent;
            RoundImageView mImageView;
            LinearLayout buttonLayout, mCardLayout;
            ConstraintLayout mImageLayout;
            ImageView audioPlayImageView;
            RcsChatbotVideoPlayer mRcsChatbotVideoPlayer;
            boolean flag;
            //flag true显示全文 false是收起全文

            public VH(View itemView) {
                super(itemView);
                mCardContent = (AlignTextView) itemView.findViewById(R.id.card_content);
                mCardTitle = (AlignTextView) itemView.findViewById(R.id.card_title);
                mCardTitleTop = (AlignTextView) itemView.findViewById(R.id.card_title_top);
                mImageView = (RoundImageView) itemView.findViewById(R.id.card_image);
                buttonLayout = (LinearLayout) itemView.findViewById(R.id.card_action);
                audioPlayImageView = (ImageView) itemView.findViewById(R.id.card_image_audio_play);
                mRcsChatbotVideoPlayer = (RcsChatbotVideoPlayer) itemView.findViewById(R.id.rcs_chatbot_video_player);
                mCardLayout = (LinearLayout) itemView.findViewById(R.id.card_layout);
                mImageLayout = (ConstraintLayout) itemView.findViewById(R.id.card_image_layout);
            }
        }

    }

    //给Textview设置行数和显示全文按钮
    private void getLastIndexForLimit(AlignTextView tv, int maxLine, final RcsChatbotCardBean.MessageBean.ContentBean contentBean, int textSize, int textViewWidth) {
        String content;
        content = contentBean.description;
        if (TextUtils.isEmpty(content)) {
            return;
        }
        //获取TextView的画笔对象
        TextPaint paint = tv.getPaint();
        //空格会换行的问题
        String replaceContent = content.replace(" ", "s");
        paint.setTextSize(textSize);
        //实例化StaticLayout 传入相应参数
        StaticLayout staticLayout = new StaticLayout(replaceContent, paint, textViewWidth, Layout.Alignment.ALIGN_NORMAL, 1, (int) getResources().getDimension(R.dimen.chatbot_card_lineSpacingExtra), false);
        //判断content是行数是否超过最大限制行数
        if (staticLayout.getLineCount() > maxLine && maxLine != 0) {
            //获取到第maxline行最后一个文字的下标
            int index = staticLayout.getLineStart(maxLine) - 1;
            //定义收起后的文本内容
            String substring = content.substring(0, index - 5) + "..." + ">>";
            elipseString = new SpannableString(substring);
            //给查看设成蓝色
            elipseString.setSpan(new ForegroundColorSpan(Color.parseColor("#0079e2")), substring.length() - 5, substring.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            //设置收起后的文本内容
            tv.setFullText(true);
            tv.setText(elipseString);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //点击显示全部的事件
                    Intent intent = new Intent(getContext(), RcsChatbotFullTextFragmentActivity.class);
                    intent.putExtra(RcsChatbotFullTextFragmentActivity.TITLE, contentBean.title);
                    intent.putExtra(RcsChatbotFullTextFragmentActivity.CONTENT, contentBean.description);
                    getContext().startActivity(intent);
                }
            });
        } else {
            //没有超过 直接设置文本
            tv.setText(content);
            tv.setOnClickListener(null);
        }
    }

    //获取文本高度
    private StaticLayout getStaticLayout(TextView tv, String content, int textSize, int textViewWidth) {
        //获取TextView的画笔对象
        TextPaint paint = tv.getPaint();
        paint.setTextSize(textSize);
        //实例化StaticLayout 传入相应参数
        StaticLayout staticLayout = new StaticLayout(content, paint, textViewWidth, Layout.Alignment.ALIGN_NORMAL, 1, (int) getResources().getDimension(R.dimen.chatbot_card_lineSpacingExtra), true);
        return staticLayout;
    }

    private void truncationCard(AlignTextView contentView, AlignTextView titleView, int sumHeight, int truncationHeight, int descriptionHeight, int titleHeight, int textWidth, RcsChatbotCardBean.MessageBean.ContentBean contentBean) {
        sumHeight -= descriptionHeight;
        int contentLineHeight = 1;
        int titleLineHeight = 1;
        if (descriptionHeight != 0) {
            StaticLayout contentStaticLayout = getStaticLayout(contentView, contentBean.description, mContentTextSize, textWidth);
            contentLineHeight = contentStaticLayout.getHeight() / contentStaticLayout.getLineCount();
        }
        if (titleHeight != 0) {
            StaticLayout titleStaticLayout = getStaticLayout(contentView, contentBean.title, mContentTextSize, textWidth);
            titleLineHeight = titleStaticLayout.getHeight() / titleStaticLayout.getLineCount();
        }
        if (sumHeight + contentLineHeight > truncationHeight) {
            sumHeight += contentLineHeight - titleHeight;
            int line = (truncationHeight - sumHeight) / titleLineHeight;
            getLastIndexForLimit(contentView, 1, contentBean, mContentTextSize, textWidth);
            titleView.setLines(line > 0 ? line : 1);
        } else {
            getLastIndexForLimit(contentView, (truncationHeight - sumHeight) / contentLineHeight, contentBean, mContentTextSize, textWidth);
        }
    }

    //设置圆角 按照绘画顺序
    private RoundImageView setImageCorners(View view, int leftTop, int rightTop, int rightBottom, int leftBottom) {
        RoundImageView imageView = (RoundImageView) view.findViewById(R.id.card_image);
        imageView.setCorners(leftTop, rightTop, rightBottom, leftBottom);
        return imageView;
    }

    //判断图片高度
    private int setImageHeight(String type, ImageView imageView, boolean isHorizontal, RcsChatbotVideoPlayer rcsChatbotVideoPlayer) {
        ConstraintLayout.LayoutParams params;
        int imageHeight;
        switch (type.toUpperCase()) {
            case "SHORT_HEIGHT":
                params = new ConstraintLayout.LayoutParams(
                        isHorizontal ? (int) getContext().getResources().getDimension(R.dimen.public_single_image_horizontal_width) : FrameLayout.LayoutParams.MATCH_PARENT,
                        (int) getContext().getResources().getDimension(R.dimen.public_chatbot_single_height_short));//
                imageHeight = (int) mContext.getResources().getDimension(R.dimen.public_chatbot_single_height_short);
                break;
            case "TALL_HEIGHT":
                params = new ConstraintLayout.LayoutParams(
                        isHorizontal ? (int) getContext().getResources().getDimension(R.dimen.public_single_image_horizontal_width) : FrameLayout.LayoutParams.MATCH_PARENT,
                        (int) getContext().getResources().getDimension(R.dimen.public_chatbot_single_height_tall));//
                imageHeight = (int) mContext.getResources().getDimension(R.dimen.public_chatbot_single_height_tall);
                break;
            case "MEDIUM_HEIGHT":
            default:
                params = new ConstraintLayout.LayoutParams(
                        isHorizontal ? (int) getContext().getResources().getDimension(R.dimen.public_single_image_horizontal_width) : FrameLayout.LayoutParams.MATCH_PARENT,
                        (int) getContext().getResources().getDimension(R.dimen.public_chatbot_single_height_medium));//
                imageHeight = (int) mContext.getResources().getDimension(R.dimen.public_chatbot_single_height_medium);
                break;
        }
        if (imageView != null) {
            imageView.setLayoutParams(params);
        }
        if (rcsChatbotVideoPlayer != null) {
            rcsChatbotVideoPlayer.setLayoutParams(params);
        }
        return imageHeight;
    }

    //计算标题高度
    private int getCardTitleHeight(AlignTextView titleView, String title, int textWidth) {
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(title);
        StaticLayout staticLayout = getStaticLayout(titleView, title, mTitleTextSize, textWidth);
        int titleHeight = staticLayout.getHeight() / staticLayout.getLineCount() * titleView.getCount(textWidth, mTitleTextSize);
        if (staticLayout.getLineCount() - titleView.getCount(textWidth, mTitleTextSize) < 0) {
            titleHeight += (int) getResources().getDimension(R.dimen.chatbot_card_lineSpacingExtra);
        }
        return titleHeight;
    }

    //计算内容高度
    private int getCardDescriptionHeight(AlignTextView contentView, String description, int textWidth) {
        contentView.setVisibility(View.VISIBLE);
        contentView.setText(description);
        StaticLayout staticLayout = getStaticLayout(contentView, description, mContentTextSize, textWidth);
        int descriptionHeight = staticLayout.getHeight() / staticLayout.getLineCount() * contentView.getCount(textWidth, mContentTextSize);
        if (staticLayout.getLineCount() - contentView.getCount(textWidth, mContentTextSize) < 0) {
            descriptionHeight += (int) getResources().getDimension(R.dimen.chatbot_card_lineSpacingExtra);
        }
        return descriptionHeight;
    }


    public ChatbotCardCSSBean parseCSS(String css) {
        if (TextUtils.isEmpty(css)) return null;
        try {
            String message;
            String title;
            String description;
            String suggestions;
            ChatbotCardCSSBean chatbotCardCSS = new ChatbotCardCSSBean();
            css.trim();
            if (css.contains("Message")) {
                message = css.substring(css.indexOf("{", css.indexOf("Message")) + 1, css.indexOf("}"));
                if (!TextUtils.isEmpty(message)) {
                    String[] setList = message.trim().split(";");
                    int i = 0;
                    while (i < setList.length) {
                        String text = setList[i].substring(0, setList[i].indexOf(":")).trim();
                        switch (text) {
                            case "text-align":
                                chatbotCardCSS.messageBean.textAlign = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-size":
                                chatbotCardCSS.messageBean.fontSize = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-family":
                                chatbotCardCSS.messageBean.fontFamily = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-weight":
                                chatbotCardCSS.messageBean.fontWeight = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "color":
                                chatbotCardCSS.messageBean.color = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "background-color":
                                chatbotCardCSS.messageBean.backgroudColor = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "background-image":
                                String url = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                chatbotCardCSS.messageBean.backgroudImage = url.contains("url") ? url.substring("url(\"".length(), url.length() - 2) : url;
                                break;
                        }
                        i++;
                    }
                }
            }
            if (css.contains("message.content.title")) {
                title = css.substring(css.indexOf("{", css.indexOf("message.content.title")) + 1, css.indexOf("}", css.indexOf("message.content.title"))).trim();
                if (!TextUtils.isEmpty(title)) {
                    String[] setList = title.trim().split(";");

                    int i = 0;
                    while (i < setList.length) {
                        String text = setList[i].substring(0, setList[i].indexOf(":")).trim();
                        switch (text) {
                            case "text-align":
                                chatbotCardCSS.titleBean.textAlign = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-size":
                                chatbotCardCSS.titleBean.fontSize = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-family":
                                chatbotCardCSS.titleBean.fontFamily = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-weight":
                                chatbotCardCSS.titleBean.fontWeight = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "color":
                                chatbotCardCSS.titleBean.color = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                        }
                        i++;
                    }
                }
            }
            if (css.contains("message.content.description")) {
                description = css.substring(css.indexOf("{", css.indexOf("message.content.description")) + 1, css.indexOf("}", css.indexOf("message.content.description"))).trim();
                if (!TextUtils.isEmpty(description)) {
                    String[] setList = description.trim().split(";");
                    int i = 0;
                    while (i < setList.length) {
                        String text = setList[i].substring(0, setList[i].indexOf(":")).trim();
                        switch (text) {
                            case "text-align":
                                chatbotCardCSS.descriptionBean.textAlign = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-size":
                                chatbotCardCSS.descriptionBean.fontSize = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-family":
                                chatbotCardCSS.descriptionBean.fontFamily = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-weight":
                                chatbotCardCSS.descriptionBean.fontWeight = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "color":
                                chatbotCardCSS.descriptionBean.color = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                        }
                        i++;
                    }
                }
            }
            if (css.contains("message.content.suggestions")) {
                suggestions = css.substring(css.indexOf("{", css.indexOf("message.content.suggestions")) + 1, css.indexOf("}", css.indexOf("message.content.suggestions"))).trim();
                if (!TextUtils.isEmpty(suggestions)) {
                    String[] setList = suggestions.trim().split(";");
                    int i = 0;
                    while (i < setList.length) {
                        String text = setList[i].substring(0, setList[i].indexOf(":")).trim();
                        switch (text) {
                            case "text-align":
                                chatbotCardCSS.suggestionsBean.textAlign = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-size":
                                chatbotCardCSS.suggestionsBean.fontSize = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-family":
                                chatbotCardCSS.suggestionsBean.fontFamily = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "font-weight":
                                chatbotCardCSS.suggestionsBean.fontWeight = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                            case "color":
                                chatbotCardCSS.suggestionsBean.color = setList[i].substring(setList[i].indexOf(":") + 1).trim();
                                break;
                        }
                        i++;
                    }
                }
            }
            return chatbotCardCSS;
        } catch (Exception e) {
            Log.d("CSS", "parseCSS: " + e.getMessage().toString());
            return null;
        }
    }

    class ChatbotCardCSSBean {
        MessageBean messageBean;
        TitleBean titleBean;
        DescriptionBean descriptionBean;
        SuggestionsBean suggestionsBean;

        public ChatbotCardCSSBean(MessageBean messageBean, TitleBean titleBean, DescriptionBean descriptionBean, SuggestionsBean suggestionsBean) {
            this.messageBean = messageBean;
            this.titleBean = titleBean;
            this.descriptionBean = descriptionBean;
            this.suggestionsBean = suggestionsBean;
        }

        public ChatbotCardCSSBean() {
            messageBean = new MessageBean();
            titleBean = new TitleBean();
            descriptionBean = new DescriptionBean();
            suggestionsBean = new SuggestionsBean();
        }

        class MessageBean {
            String color;
            String fontSize;
            String fontFamily;
            String fontWeight;
            String textAlign;
            String backgroudColor;
            String backgroudImage;

            private MessageBean() {

            }
        }

        class TitleBean {
            String color;
            String fontSize;
            String fontWeight;
            String fontFamily;
            String textAlign;

            private TitleBean() {

            }
        }

        class DescriptionBean {
            String color;
            String fontSize;
            String fontWeight;
            String fontFamily;
            String textAlign;

            private DescriptionBean() {

            }
        }

        class SuggestionsBean {
            String color;
            String fontSize;
            String fontWeight;
            String fontFamily;
            String textAlign;

            private SuggestionsBean() {

            }
        }
    }

    private String readCss(String filePath) {
        if (TextUtils.isEmpty(filePath)) return null;
        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = fis.read(buf)) != -1) {
                stream.write(buf, 0, len);
            }
            fis.close();
            stream.close();
            return stream.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setCardStyle(ChatbotCardCSSBean chatbotCardCSSBean, View view, final boolean isHorizontal, boolean isRight) {
        if (chatbotCardCSSBean == null) return;
        try {
            AlignTextView contentView, titleView;
            AlignTextView topTitleView = null;
            final LinearLayout linearLayout;
            GradientDrawable myGrad = (GradientDrawable) view.getBackground();
            if (isHorizontal) {
                contentView = (AlignTextView) view.findViewById(R.id.card_content_horizontal);
                titleView = (AlignTextView) view.findViewById(R.id.card_title_horizontal);
                linearLayout = (LinearLayout) view.findViewById(R.id.content_layout_horizontal);
            } else {
                contentView = (AlignTextView) view.findViewById(R.id.card_content);
                titleView = (AlignTextView) view.findViewById(R.id.card_title);
                topTitleView = (AlignTextView) view.findViewById(R.id.card_title_top);
                linearLayout = (LinearLayout) view.findViewById(R.id.content_layout);
            }
            if (chatbotCardCSSBean.titleBean != null) {
                setFontText(chatbotCardCSSBean, titleView, true);
                if (!isHorizontal && topTitleView != null) {
                    setFontText(chatbotCardCSSBean, topTitleView, true);
                }
            }
            if (chatbotCardCSSBean.messageBean != null) {
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("color", chatbotCardCSSBean.messageBean.color);
                hashMap.put("fontFamily", chatbotCardCSSBean.messageBean.fontFamily);
                hashMap.put("fontSize", chatbotCardCSSBean.messageBean.fontSize);
                hashMap.put("fontWeight", chatbotCardCSSBean.messageBean.fontWeight);
                hashMap.put("textAlign", chatbotCardCSSBean.messageBean.textAlign);
                hashMap.put("backgroudColor", chatbotCardCSSBean.messageBean.backgroudColor);
                hashMap.put("backgroudImage", chatbotCardCSSBean.messageBean.backgroudImage);
                Iterator<Map.Entry<String, String>> iterator = hashMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (TextUtils.isEmpty(value)) {
                        continue;
                    }
                    switch (key) {
                        case "color":
                            break;
                        case "backgroudColor":
                            if (!RcsChatbotAttachmentView.this.isSelected()) {
                                myGrad.mutate();
                                try{
                                    int backroudColor = Color.parseColor(value);
                                    myGrad.setColor(backroudColor);
                                } catch (Exception e) {
                                    Log.d("CSS", "color can't parse " + e.getMessage());
                                }
                            }
                            break;
                        case "backgroudImage":
                            final float[] rids;
                            if (!isHorizontal) {
                                rids = new float[]{0, 0, 0, 0, mCardShape, mCardShape, mCardShape, mCardShape};
                            } else {
                                if (isRight) {
                                    rids = new float[]{mCardShape, mCardShape, 0, 0, 0, 0, mCardShape, mCardShape};
                                } else {
                                    rids = new float[]{0, 0, mCardShape, mCardShape, mCardShape, mCardShape, 0, 0};
                                }
                            }
                            if (!TextUtils.isEmpty(value)) {
                                RcsFileDownloadHelper.FileInfo cssFileInfo = new RcsFileDownloadHelper.FileInfo(value).setConTenType("css/css");
                                String cssThump = RcsFileDownloadHelper.getPathFromFileInfo(cssFileInfo, RmsDefine.RMS_CHATBO_PATH);
                                if (TextUtils.isEmpty(cssThump)) {
                                    RcsFileDownloadHelper.downloadFile("", cssFileInfo, new RcsFileDownloadHelper.Callback() {
                                        @Override
                                        public void onDownloadResult(String cookie, boolean succ, String filePath) {
                                            if (!succ) return;
                                            if (mHeight > 0 && !isHorizontal) {
                                                linearLayout.setMinimumHeight(mHeight);
                                                linearLayout.setBackground(new BitmapDrawable(roundBottomBitmapByShader(BitmapFactory.decodeFile(filePath), isHorizontal ? mContentWidth : mWidth, mHeight, mCardShape, rids)));
                                            }
                                        }
                                    }, null, RmsDefine.RMS_CHATBO_PATH);
                                } else if (mHeight > 0) {
                                    linearLayout.setMinimumHeight(mHeight);
                                    linearLayout.setBackground(new BitmapDrawable(roundBottomBitmapByShader(BitmapFactory.decodeFile(cssThump), isHorizontal ? mContentWidth : mWidth, mHeight, mCardShape, rids)));
                                }
                            } else {
                                continue;
                            }
                            break;
                    }
                }
            }
            if (chatbotCardCSSBean.descriptionBean != null) {
                setFontText(chatbotCardCSSBean, contentView, false);
            }
        } catch (Exception e) {
            Log.d("CSS", "setCardStyle: " + e.getMessage().toString());
        }
    }

    private void setFontText(ChatbotCardCSSBean chatbotCardCSSBean, AlignTextView alignTextView, boolean isTitle) {
        if (chatbotCardCSSBean == null || alignTextView == null) return;
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("color", isTitle ? chatbotCardCSSBean.titleBean.color : chatbotCardCSSBean.descriptionBean.color);
        hashMap.put("fontFamily", isTitle ? chatbotCardCSSBean.titleBean.fontFamily : chatbotCardCSSBean.descriptionBean.fontFamily);
        hashMap.put("fontSize", isTitle ? chatbotCardCSSBean.titleBean.fontSize : chatbotCardCSSBean.descriptionBean.fontSize);
        hashMap.put("fontWeight", isTitle ? chatbotCardCSSBean.titleBean.fontWeight : chatbotCardCSSBean.descriptionBean.fontWeight);
        hashMap.put("textAlign", isTitle ? chatbotCardCSSBean.titleBean.textAlign : chatbotCardCSSBean.descriptionBean.textAlign);
        Iterator<Map.Entry<String, String>> iterator = hashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (TextUtils.isEmpty(value)) {
                HashMap<String, String> MessageMap = new HashMap<>();
                MessageMap.put("color", chatbotCardCSSBean.messageBean.color);
                MessageMap.put("fontFamily", chatbotCardCSSBean.messageBean.fontFamily);
                MessageMap.put("fontSize", chatbotCardCSSBean.messageBean.fontSize);
                MessageMap.put("fontWeight", chatbotCardCSSBean.messageBean.fontWeight);
                MessageMap.put("textAlign", chatbotCardCSSBean.messageBean.textAlign);
                if (!TextUtils.isEmpty(MessageMap.get(key))) {
                    value = MessageMap.get(key);
                } else {
                    continue;
                }
            }
            switch (key) {
                case "color":
                    try {
                        alignTextView.setTextColor(Color.parseColor(value));
                    }catch (Exception e){
                        Log.d("CSS", "setFontText color can't parse " + e.getMessage());
                    }

                    break;
                case "fontFamily":
                    switch (value) {
                        case "sans":
                            alignTextView.setTypeface(Typeface.SANS_SERIF);
                            break;
                        case "noraml":
                            alignTextView.setTypeface(Typeface.DEFAULT);
                            break;
                        case "serif":
                            alignTextView.setTypeface(Typeface.SERIF);
                            break;
                        case "monospace":
                            alignTextView.setTypeface(Typeface.MONOSPACE);
                            break;
                        case "sans_serif":
                            alignTextView.setTypeface(Typeface.SANS_SERIF);
                            break;
                        default:
                            alignTextView.setTypeface(Typeface.DEFAULT);
                            break;
                    }
                    break;
                case "fontSize":
                    if (getCssTextSize(value) > 0) {
                        alignTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getCssTextSize(value));
                    }
                    break;
                case "fontWeight":
                    //400为普通 700为粗体 Android 没有设置weight的方法。
                    if (Integer.valueOf(value) > 400) {
                        TextPaint paint = alignTextView.getPaint();
                        paint.setFakeBoldText(true);
                        alignTextView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    }
                    break;
                case "textAlign":
                    if (TextUtils.equals(value, "right")) {
                        alignTextView.setAlign(AlignTextView.Align.ALIGN_RIGHT);
                    } else if (TextUtils.equals(value, "left")) {
                        alignTextView.setAlign(AlignTextView.Align.ALIGN_LEFT);
                    } else {
                        alignTextView.setAlign(AlignTextView.Align.ALIGN_CENTER);
                    }
                    break;
            }
        }
    }

    private void parseFontStyle(List<String> list, boolean isTitle, boolean isHorizontal, View View) {
        if (list == null || View == null) return;
        try {
            AlignTextView textView;
            if (isTitle) {
                textView = (AlignTextView) View.findViewById(isHorizontal ? R.id.card_title_horizontal : R.id.card_title);
            } else {
                textView = (AlignTextView) View.findViewById(isHorizontal ? R.id.card_content_horizontal : R.id.card_content);
            }
            for (String style : list) {
                switch (style) {
                    case "underline":
                        textView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                        break;
                    case "bold":
                        textView.getPaint().setFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                        break;
                    case "italics":
                        textView.setTypeface(textView.getTypeface(), Typeface.BOLD_ITALIC);
                        break;
                }
            }
        } catch (Exception e) {
            Log.d("CSS", "parseFontStyle: " + e.getMessage().toString());
        }
    }

    private void setSuggestionFontText(ChatbotCardCSSBean chatbotCardCSSBean, Button button) {
        if (chatbotCardCSSBean == null || button == null) return;
        try {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("color", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.color) ? chatbotCardCSSBean.messageBean.color : chatbotCardCSSBean.suggestionsBean.color);
            hashMap.put("fontFamily", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.fontFamily) ? chatbotCardCSSBean.messageBean.fontFamily : chatbotCardCSSBean.suggestionsBean.fontFamily);
            hashMap.put("fontSize", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.fontSize) ? chatbotCardCSSBean.messageBean.fontSize : chatbotCardCSSBean.suggestionsBean.fontSize);
            hashMap.put("fontWeight", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.fontWeight) ? chatbotCardCSSBean.messageBean.fontWeight : chatbotCardCSSBean.suggestionsBean.fontWeight);
            hashMap.put("textAlign", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.textAlign) ? chatbotCardCSSBean.messageBean.textAlign : chatbotCardCSSBean.suggestionsBean.textAlign);
            int i = 0;
            Iterator<Map.Entry<String, String>> iterator = hashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String key = entry.getKey();
                String value = entry.getValue();
                if (TextUtils.isEmpty(value)) {
                    continue;
                }
                switch (key) {
                    case "color":
                        try {
                            button.setTextColor(Color.parseColor(value));
                        }catch (Exception e){
                            Log.d("CSS", "setSuggestionFontText color can't parse " + e.getMessage());
                        }

                        break;
                    case "fontFamily":
                        switch (value) {
                            case "sans":
                                button.setTypeface(Typeface.SANS_SERIF);
                                break;
                            case "noraml":
                                button.setTypeface(Typeface.DEFAULT);
                                break;
                            case "serif":
                                button.setTypeface(Typeface.SERIF);
                                break;
                            case "monospace":
                                button.setTypeface(Typeface.MONOSPACE);
                                break;
                            case "sans_serif":
                                button.setTypeface(Typeface.SANS_SERIF);
                                break;
                            default:
                                button.setTypeface(Typeface.DEFAULT);
                                break;
                        }
                        break;
                    case "fontSize":
                        if (getCssTextSize(value) > 0) {
                            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getCssTextSize(value));
                        }
                        break;
                    case "fontWeight":
                        if (Integer.valueOf(value) > 400) {
                            TextPaint paint = button.getPaint();
                            paint.setFakeBoldText(true);
                            button.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                        }
                        break;
                    case "textAlign":
                        if (TextUtils.equals(value, "right")) {
                            button.setGravity(Gravity.RIGHT);
                        } else if (TextUtils.equals(value, "left")) {
                            button.setGravity(Gravity.LEFT);
                        } else {
                            button.setGravity(Gravity.CENTER);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            Log.d("CSS", "setSuggestionFontText: " + e.getMessage().toString());
        }
    }

    private void setCSSTextSize(ChatbotCardCSSBean chatbotCardCSSBean) {
        if (chatbotCardCSSBean == null) return;
        try {
            String titleTextSize = chatbotCardCSSBean.titleBean.fontSize;
            String descriptionTextSize = chatbotCardCSSBean.descriptionBean.fontSize;
            String suggestionTextSize = chatbotCardCSSBean.suggestionsBean.fontSize;
            String messageTextSize = chatbotCardCSSBean.messageBean.fontSize;
            if (getCssTextSize(titleTextSize) > 0) {
                mTitleTextSize = getCssTextSize(titleTextSize);
            } else if (getCssTextSize(messageTextSize) > 0) {
                mTitleTextSize = getCssTextSize(messageTextSize);
            }
            if (getCssTextSize(descriptionTextSize) > 0) {
                mContentTextSize = getCssTextSize(descriptionTextSize);
            } else if (getCssTextSize(messageTextSize) > 0) {
                mContentTextSize = getCssTextSize(messageTextSize);
            }
            if (getCssTextSize(suggestionTextSize) > 0) {
                mSuggestionsSize = getCssTextSize(suggestionTextSize) + RcsChatbotNiceImageView.dp2px(getContext(), 10);
            } else if (getCssTextSize(messageTextSize) > 0) {
                mSuggestionsSize = getCssTextSize(messageTextSize) + RcsChatbotNiceImageView.dp2px(getContext(), 10);
            }
        } catch (Exception e) {
            Log.d("CSS", "setCSSTextSize: " + e.getMessage().toString());
        }
    }

    private int getCssTextSize(String testcss) {
        if (TextUtils.isEmpty(testcss)) return 0;
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(testcss);
        m.find();
        Pattern pattern = Pattern.compile("[0-9]*");
        if (!pattern.matcher(m.group()).matches()) {
            return 0;
        }
        return Integer.valueOf(m.group());
    }


    /**
     * 利用BitmapShader绘制底部圆角图片
     *
     * @param bitmap    待处理图片
     * @param outWidth  结果图片宽度，一般为控件的宽度
     * @param outHeight 结果图片高度，一般为控件的高度
     * @param radius    圆角半径大小
     * @return 结果图片
     */

    private Bitmap roundBottomBitmapByShader(Bitmap bitmap, int outWidth, int outHeight, int radius, float[] rids) {
        if (bitmap == null) {
            Log.d("css", "Bitmap can't be null");
            return null;
        }
        // 初始化缩放比
        float widthScale = outWidth * 1.0f / bitmap.getWidth();
        float heightScale = outHeight * 1.0f / bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.setScale(widthScale, heightScale);

        // 初始化绘制纹理图
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        // 根据控件大小对纹理图进行拉伸缩放处理
        bitmapShader.setLocalMatrix(matrix);

        // 初始化目标bitmap
        Bitmap targetBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);

        // 初始化目标画布
        Canvas targetCanvas = new Canvas(targetBitmap);

        // 初始化画笔
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(bitmapShader);

        Path path = new Path();
        path.addRoundRect(new RectF(0, 0, outWidth, outHeight), rids, Path.Direction.CW);
        // 利用画笔绘制底部圆角
        targetCanvas.drawPath(path, paint);

        return targetBitmap;
    }
}