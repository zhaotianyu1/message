package com.juphoon.chatbotmaap;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotCardBean;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbot.RcsChatbotSuggestionsBean;
import com.juphoon.chatbotmaap.Location.RcsBaiduLocationActivity;
import com.juphoon.chatbotmaap.bottomMenu.ListViewDialog;
import com.juphoon.chatbotmaap.chatbotcard.ChatbotCardCSSBean;
import com.juphoon.chatbotmaap.tcl.BorderRelativeLayout;
import com.juphoon.chatbotmaap.tcl.ListViewDialogs;
import com.juphoon.chatbotmaap.tcl.RadiusCardView;
import com.juphoon.chatbotmaap.tcl.RoundCornerImageView;
import com.juphoon.chatbotmaap.tcl.VideoData;
import com.juphoon.chatbotmaap.view.RoundLayoutView;
import com.juphoon.helper.RcsBitmapCache;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.helper.other.AudioPlayHelper;
import com.juphoon.rcs.tool.RcsNetUtils;
import com.juphoon.service.RmsDefine;
import com.tcl.ff.component.animer.glow.view.AllCellsGlowLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class RcsChatbotCardView extends FrameLayout implements RcsChatbotCardHelper.OnGetCSSCallBack{

    private final String TAG = RcsChatbotCardView.class.getSimpleName();
    private Context mContext;
    private String mConversationId;
    private String mRmsUri;
    private String mServiceId;
    private String mCardJson;
    private String mTrafficType;
    private String mContributionId;
    private String mAudioCookie;
    private String mCookie;
    private ChatbotCardCSSBean mCardCssBean;
    private RcsChatbotCardViewsAdapter rcsChatbotCardViewsAdapter;
    private RcsChatbotCardBean mCardBean;
    private int mMaxWeight;

    private static int RECORD_VIDEO = 0;
    private static int RECORD_AUDIO = 2;

    public static int sRoundCorner = 0;

    public RcsChatbotCardView(final Context context, final AttributeSet attrs) {
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

    public void bindMessagePartData(final String json, final String conversationid, String rmsUri, int maxWeight) {
        stopAudioIfNeed();
        mCookie = UUID.randomUUID().toString();
        mConversationId = conversationid;
        mRmsUri = rmsUri;
        View cardView = null;
        mMaxWeight = maxWeight;
        sRoundCorner = (int) getResources().getDimension(R.dimen.chatbot_card_round_corner);
        try {
            JSONObject jsonObject = new JSONObject(json);
            mServiceId = jsonObject.optString(RmsDefine.Rms.CHATBOT_SERVICE_ID);
            mCardJson = jsonObject.optString(RmsDefine.Rms.RMS_EXTRA);
            mTrafficType = jsonObject.optString(RmsDefine.Rms.TRAFFIC_TYPE);
            mContributionId = jsonObject.optString(RmsDefine.Rms.CONTRIBUTION_ID);
            mCardBean = new Gson().fromJson(mCardJson, RcsChatbotCardBean.class);
            if (mCardBean.message != null) {

                if (mCardBean.message.generalPurposeCard != null) {
                    cardView = initSingleCardView(mCardBean.message);
                    ViewGroup.LayoutParams params = cardView.getLayoutParams();
//                    params.width = 600;
                    params.width = 400;
                    params.height = 360;
                    cardView.setLayoutParams(params);
                    cardView.setFocusable(true);
                    cardView.setFocusableInTouchMode(true);
                } else if (mCardBean.message.generalPurposeCardCarousel != null) {
                    cardView = initListCard(mCardBean.message);

                    ViewGroup.LayoutParams params = cardView.getLayoutParams();

                      params.width = maxWeight;
                      params.height = 360;

                 //   params.height = RecyclerView.LayoutParams.WRAP_CONTENT;
                    cardView.setLayoutParams(params);

                    setClipChildren(false);
                    setClipToPadding(false);
                }
//                params.width = mMaxWeight;
                //修改底部图片

                this.removeAllViews();
                this.addView(cardView);
            } else {
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private View initListCard(RcsChatbotCardBean.MessageBean messageBean) {
        Log.i("qwe","initListCard--staty");
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.rcs_chatbot_mutiple_cards, this, false);
        RecyclerView recyclerView = view.findViewById(R.id.cards_recycleview);
        ExceptionLayoutManager layoutManager = new ExceptionLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        mCardCssBean = RcsChatbotCardHelper.TransFileToCSSBean(RcsChatbotCardHelper.getCSSFilePath(messageBean.generalPurposeCardCarousel.layout.style, mServiceId, mCookie, this));
        rcsChatbotCardViewsAdapter = new RcsChatbotCardViewsAdapter(messageBean.generalPurposeCardCarousel);
        recyclerView.setFocusable(true);
        recyclerView.setClickable(true);
        recyclerView.setAdapter(rcsChatbotCardViewsAdapter);
        return view;
    }

    private View initSingleCardView(RcsChatbotCardBean.MessageBean messageBean) {

        View singleCardView;
        final RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card = messageBean.generalPurposeCard;
        boolean isHorizontal = false;
        RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean.LayoutBean cardLayout = card.layout;
        if (cardLayout != null) {
            isHorizontal = TextUtils.equals(cardLayout.cardOrientation, RcsChatbotCardHelper.CARD_ORIENTATION_HORIZONTAL);
            mCardCssBean = RcsChatbotCardHelper.TransFileToCSSBean(RcsChatbotCardHelper.getCSSFilePath(cardLayout.style, mServiceId, mCookie, this));
        } else {
            return null;
        }
        Log.i("qwe","isHorizontal---:"+isHorizontal);
        if (isHorizontal) {
            singleCardView = initSingleCardViewHorizontal(messageBean);
        } else {
            singleCardView = initSingleCardViewVertical(messageBean);
        }
        return singleCardView;
    }

    int count =0;

    int index = 0;

    static VideoData videoData = new VideoData();

    private View initSingleCardViewVertical(RcsChatbotCardBean.MessageBean messageBean) {

        final RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card = messageBean.generalPurposeCard;
        View view = LayoutInflater.from(getContext()).inflate(R.layout.chatbot_card_view_single_vertical, this, false);
        final TextView contentView = (TextView) view.findViewById(R.id.card_content);
        contentView.setFocusable(false);
        contentView.setFocusableInTouchMode(false);
        TextView titleTopView = (TextView) view.findViewById(R.id.card_title_top);//title在图片上方情况
        TextView titleView = (TextView) view.findViewById(R.id.card_title);
        final RoundCornerImageView imageView = view.findViewById(R.id.card_image);
        final RcsChatbotVideoPlayer videoPlayer = view.findViewById(R.id.rcs_chatbot_video_player);
        final ImageView audioPlayImageView = view.findViewById(R.id.card_image_audio_play);
        final AllCellsGlowLayout relativeLayouts = view.findViewById(R.id.card_layout);
        LinearLayout buttonLayout = view.findViewById(R.id.card_action);
        imageView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    relativeLayouts.setBackgroundResource(R.drawable.act_chart_chose);
                }else{
                    relativeLayouts.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
        contentView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    relativeLayouts.setBackgroundResource(R.drawable.act_chart_chose);
                }else{
                    relativeLayouts.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
//        relativeLayouts.setOnFocusChangeListener(new OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                Log.i("mnmn","hafocus--:"+hasFocus);
//                if(hasFocus){
//                    relativeLayouts.setBackgroundResource(R.drawable.act_chart_chose);
//                }else{
//                    relativeLayouts.setBackgroundColor(Color.TRANSPARENT);
//                }
//            }
//        });
        final RcsFileDownloadHelper.FileInfo thumbFileInfo = new RcsFileDownloadHelper.FileInfo(card.content.media.thumbnailUrl, card.content.media.thumbnailFileSize).setConTenType(card.content.media.thumbnailContentType);
        final RcsFileDownloadHelper.FileInfo mediaInfo = new RcsFileDownloadHelper.FileInfo(card.content.media.mediaUrl, card.content.media.mediaFileSize).setConTenType(card.content.media.mediaContentType);
        relativeLayouts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (card.content.media != null && card.content.media.mediaContentType.contains("audio")) {
                    final RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(card.content.media.mediaUrl, card.content.media.mediaFileSize).setConTenType(card.content.media.mediaContentType);
                    mAudioCookie = card.content.media.mediaUrl;
                    String audioPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_CHATBO_PATH);
                    if (TextUtils.isEmpty(audioPath)) {
                        Toast.makeText(getContext(), "未下载完成", Toast.LENGTH_SHORT).show();
                        RcsFileDownloadHelper.clearLastDownloadTime(card.content.media.mediaUrl);
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
                                    String thumbPath = RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(card.content.media.thumbnailUrl).setConTenType(card.content.media.thumbnailContentType), RmsDefine.RMS_THUMB_PATH);
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

                } else if (card.content.media != null && card.content.media.mediaContentType.contains("image")) {
                    Log.i("coo", "打开图片===========");
                    RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(card.content.media.mediaUrl, card.content.media.mediaFileSize).setConTenType(card.content.media.mediaContentType);
                    if (TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_CHATBO_PATH))) {
                        RcsFileDownloadHelper.downloadFile("", fileInfo,
                                new RcsFileDownloadHelper.Callback() {
                                    @Override
                                    public void onDownloadResult(String cookie, boolean succ, final String filePath) {
                                        if (succ) {
                                            Intent intent = new Intent(getContext(), RcsChatbotImageFragmentActivity.class);
                                            intent.putExtra(RcsChatbotImageFragmentActivity.FILE_PATH, RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(card.content.media.mediaUrl).setConTenType(card.content.media.mediaContentType), RmsDefine.RMS_CHATBO_PATH));
                                            getContext().startActivity(intent);
                                        }
                                    }
                                }, null, RmsDefine.RMS_CHATBO_PATH);
                        return;
                    }

                    Intent intent = new Intent(getContext(), RcsChatbotImageFragmentActivity.class);
                    intent.putExtra(RcsChatbotImageFragmentActivity.FILE_PATH, RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(card.content.media.mediaUrl).setConTenType(card.content.media.mediaContentType), RmsDefine.RMS_CHATBO_PATH));
                    getContext().startActivity(intent);
                } else if (card.content.media != null && card.content.media.mediaContentType.contains("video")) {
                    Log.i("coo", "position--:focus123");

                    if (!RcsNetUtils.checkNet(getContext()) && TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH))) {
                        Toast.makeText(getContext(), "当前无网络。", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(mediaInfo, RmsDefine.RMS_CHATBO_PATH))) {
                        Toast.makeText(getContext(), "未下载完成", Toast.LENGTH_SHORT).show();
                        tryDownloadMedia(mediaInfo);
                    } else {
                        Log.i("coo", "Handler--:1");
                        Log.i("coo", "count:" + count);
                        Log.i("coo", "videoData.getState() --:" + videoData.getState());

                        if (count == 2 && videoData.getState() == 4 || videoData.getState() == 7) {
                            videoPlayer.restart();
                            count = 1;
                        } else if (count == 1 && videoData.getState() == 3) {
                            videoPlayer.pause();
                            count = 2;
//                        }else if (count == 0 && videoData.getState() == 0) {
                        } else {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    count = 1;

                                    Log.i("coo", "run--:1");
                                    AudioPlayHelper.getInstance().stop();
                                    audioPlayImageView.setVisibility(View.GONE);
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
                }
            }
        });

        int suggestionSize = 0;
        int mediaHeightType = -1;
        if (card.content.suggestions != null) {
            suggestionSize = card.content.suggestions.size();
            initSuggestionView(buttonLayout, card.content.suggestions);
        }
        if (card.content.media != null && card.content.media.height != null) {
            float height = RcsChatbotCardHelper.getCardHeight(getContext(), card.content.media.height);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) height);
            imageView.setLayoutParams(new LayoutParams(params));
            mediaHeightType = RcsChatbotCardHelper.getCardHeightType(card.content.media.height);
        }else {
            View mediaLayout = view.findViewById(R.id.chatbot_card_view);
            mediaLayout.setVisibility(GONE);
        }
        //处理media
        if (card.content.media != null && card.content.media.mediaContentType.contains("audio")) {
            initChatbotAudioView(card.content, imageView, audioPlayImageView);
        } else if (card.content.media != null && card.content.media.mediaContentType.contains("image")) {
            initChatbotImageView(card.content, imageView);
        } else if (card.content.media != null && card.content.media.mediaContentType.contains("video")) {
            initChatbotVideoView(card.content, imageView, videoPlayer, audioPlayImageView);
        }
        if (mCardCssBean != null) {
            if (card.content.isTop) {
                RcsChatbotCardHelper.setCardStyle(mCardCssBean, contentView, titleTopView, view);
            } else {
                RcsChatbotCardHelper.setCardStyle(mCardCssBean, contentView, titleView, view);
            }
        }
        final String contentTitleText = card.content.title;
        if (!TextUtils.isEmpty(contentTitleText)) {
            if (card.content.isTop) {
                Log.i("qwe","titleTopView--:visi:"+contentTitleText);
                titleView.setVisibility(GONE);
                titleTopView.setVisibility(VISIBLE);
                titleTopView.setText(contentTitleText);
                RcsChatbotCardHelper.setTextViewFlags(card.layout.titleFontStyle, titleTopView);
            } else {
                Log.i("qwe","titleView--:visi:"+contentTitleText);
                titleTopView.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                titleView.setText(contentTitleText);
                RcsChatbotCardHelper.setTextViewFlags(card.layout.titleFontStyle, titleView);
            }
        }
        final String description = card.content.description;
        contentView.setVisibility(VISIBLE);
        if (!TextUtils.isEmpty(description)) {
            RcsChatbotCardHelper.setTextViewFlags(card.layout.descriptionFontStyle, contentView);
            contentView.setVisibility(View.VISIBLE);
            //如果是大卡片则根据按钮个数计算描述行数
            if (mediaHeightType == 3 && suggestionSize != 0) {
                int descriptionLine = 3 - suggestionSize;
                contentView.setMaxLines(descriptionLine <= 0 ? 2 : descriptionLine);
            } else {
                contentView.setMaxLines(2);
            }
            contentView.setText(description);
            Log.i("qwe","description--:"+description);
            contentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //点击显示全部的事件
                    Intent intent = new Intent(getContext(), RcsChatbotFullTextFragmentActivity.class);
                    intent.putExtra(RcsChatbotFullTextFragmentActivity.TITLE, contentTitleText);
                    intent.putExtra(RcsChatbotFullTextFragmentActivity.CONTENT, description);
                    getContext().startActivity(intent);
                }
            });
        }
        return view;
    }


    private View initSingleCardViewHorizontal(RcsChatbotCardBean.MessageBean messageBean) {
        final RcsChatbotCardBean.MessageBean.GeneralPurposeCardBean card = messageBean.generalPurposeCard;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.chatbot_card_view_single_horizontal, this, false);
        TextView titleView = view.findViewById(R.id.card_title_horizontal);
        TextView contentView = view.findViewById(R.id.card_content_horizontal);
        CardView imageLeft = view.findViewById(R.id.image_left);
        CardView imageRight = view.findViewById(R.id.image_right);
        LinearLayout buttonLayout = view.findViewById(R.id.card_action);
        RoundCornerImageView imageView = null;
        RcsChatbotVideoPlayer videoPlayer = null;
        ImageView audioPlayImageView = null;
        switch (card.layout.imageAlignment) {
            case RcsChatbotCardHelper.CARD_IMAGEALGINMENT_LEFT:
                imageLeft.setVisibility(VISIBLE);
                imageRight.setVisibility(GONE);
                imageView = imageLeft.findViewById(R.id.card_image);
                videoPlayer = imageLeft.findViewById(R.id.rcs_chatbot_video_player);
                audioPlayImageView = imageLeft.findViewById(R.id.card_image_audio_play);
                break;
            case RcsChatbotCardHelper.CARD_IMAGEALGINMENT_RIGHT:
                imageRight.setVisibility(VISIBLE);
                imageLeft.setVisibility(GONE);
                imageView = imageRight.findViewById(R.id.card_image);
                videoPlayer = imageRight.findViewById(R.id.rcs_chatbot_video_player);
                audioPlayImageView = imageRight.findViewById(R.id.card_image_audio_play);
                break;
            default:
                break;
        }
        if (card.content.media != null && card.content.media.height != null && imageView != null) {
            float height = RcsChatbotCardHelper.getCardHeight(getContext(), card.content.media.height);
            LayoutParams params = new LayoutParams((int) RcsChatbotCardHelper.getCardWeight(getContext()), (int) height);
            imageView.setLayoutParams(new LayoutParams(params));
        }else {
            imageLeft.setVisibility(GONE);
            imageRight.setVisibility(GONE);
        }
        final String contentTitleText = card.content.title;
        if (mCardCssBean != null) {
            RcsChatbotCardHelper.setCardStyle(mCardCssBean, contentView, titleView, view);
        }
        if (!TextUtils.isEmpty(contentTitleText)) {
            titleView.setVisibility(VISIBLE);
            titleView.getLayoutParams();
            titleView.setText(contentTitleText);
            titleView.setMaxLines(2);
            RcsChatbotCardHelper.setTextViewFlags(card.layout.titleFontStyle, titleView);
        }
        //处理media
        if (card.content.media != null && card.content.media.mediaContentType.contains("audio")) {
            initChatbotAudioView(card.content, imageView, audioPlayImageView);
        } else if (card.content.media != null && card.content.media.mediaContentType.contains("image")) {
            initChatbotImageView(card.content, imageView);
        } else if (card.content.media != null && card.content.media.mediaContentType.contains("video")) {
            initChatbotVideoView(card.content, imageView, videoPlayer, audioPlayImageView);
        }
        if (card.content.suggestions != null) {
            int suggestionSize = card.content.suggestions.size();
           // buttonLayout.setVisibility(VISIBLE);
            initSuggestionView(buttonLayout, card.content.suggestions);
        }
        final String description = card.content.description;
        if (!TextUtils.isEmpty(description)) {
            contentView.setVisibility(View.VISIBLE);
            //如果是大卡片则根据按钮个数计算描述行数
            contentView.setMaxLines(2);
            contentView.setText(description);
            contentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //点击显示全部的事件
                    Intent intent = new Intent(getContext(), RcsChatbotFullTextFragmentActivity.class);
                    intent.putExtra(RcsChatbotFullTextFragmentActivity.TITLE, contentTitleText);
                    intent.putExtra(RcsChatbotFullTextFragmentActivity.CONTENT, description);
                    getContext().startActivity(intent);
                }
            });
            RcsChatbotCardHelper.setTextViewFlags(card.layout.descriptionFontStyle, contentView);
        }
        if (view != null) {
        } else {
            return view;
        }
        return view;
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

    private void initSuggestionView(LinearLayout buttonLayout, List<RcsChatbotSuggestionsBean.SuggestionsBean> suggestions) {
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.public_chatbot_button_noside_height));
        buttonParams.setMargins(0, (int) mContext.getResources().getDimension(R.dimen.public_chatbot_button_noside_margintop)
                , 0, 0);
        Log.i("cxz","initSuggestionView--: start-------");
        for (final RcsChatbotSuggestionsBean.SuggestionsBean suggestion : suggestions) {
            if (suggestion != null) {
                if (suggestion.action != null) {
                    Log.i("cxz","initSuggestionView--: start-------1");
                    Button actionButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.rcs_chatbot_noside_button, null);
                    actionButton.setLayoutParams(buttonParams);
                    Log.i("cxz","actionyTextt-------:"+suggestion.action.displayText);
                    actionButton.setText(suggestion.action.displayText);
                    RcsChatbotCardHelper.setButtonTextFontStyle(mCardCssBean, actionButton);
                    actionButton.setOnClickListener(new OnClickListener() {
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
                Log.i("cxz","initSuggestionView--: start-------2");
                if (suggestion.reply != null) {
                    Log.i("cxz","initSuggestionView--: start-------3");
                    Button replyButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.rcs_chatbot_noside_button,null);
                    replyButton.setLayoutParams(buttonParams);
                    Log.i("cxz","replyText-------:"+suggestion.reply.displayText);
                    replyButton.setText(suggestion.reply.displayText);
                    RcsChatbotCardHelper.setButtonTextFontStyle(mCardCssBean, replyButton);
                    replyButton.setOnClickListener(new OnClickListener() {
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
        Log.i("cxz","initSuggestionView--: start-------end");
    }

    /**
     * 卡片的音频
     *
     * @param contentBean
     * @param imageView
     * @param audioPlayImageView
     */
    private void initChatbotAudioView(final RcsChatbotCardBean.MessageBean.ContentBean contentBean, final ImageView imageView, final ImageView audioPlayImageView) {
        if (imageView == null) return;
        audioPlayImageView.setVisibility(View.VISIBLE);
        final RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(contentBean.media.mediaUrl, contentBean.media.mediaFileSize).setConTenType(contentBean.media.mediaContentType);
        tryDownloadMedia(fileInfo);
        mAudioCookie = contentBean.media.mediaUrl;
        dealShowThumbImage(imageView, contentBean.media.thumbnailUrl, contentBean.media.thumbnailContentType, contentBean.media.thumbnailFileSize, false);
        if (TextUtils.equals(AudioPlayHelper.getInstance().getCookie(), mAudioCookie)) {
            audioPlayImageView.setImageResource(AudioPlayHelper.getInstance().getState() == AudioPlayHelper.STATE_PLAYING ? R.drawable.pause : R.drawable.play);
        } else {
            audioPlayImageView.setImageResource(R.drawable.play);
        }
        imageView.setOnClickListener(new OnClickListener() {
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
        if (imageView == null) return;
        dealShowThumbImage(imageView, contentBean.media.thumbnailUrl, contentBean.media.thumbnailContentType, contentBean.media.thumbnailFileSize, false);
        imageView.setOnClickListener(new OnClickListener() {
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
        if (imageView == null) return;
        final RcsFileDownloadHelper.FileInfo thumbFileInfo = new RcsFileDownloadHelper.FileInfo(contentBean.media.thumbnailUrl, contentBean.media.thumbnailFileSize).setConTenType(contentBean.media.thumbnailContentType);
        String thumble_path = RcsFileDownloadHelper.getPathFromFileInfo(thumbFileInfo, RmsDefine.RMS_THUMB_PATH);
        playButton.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(thumble_path)) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(thumble_path));
        } else if (RcsNetUtils.checkNet(getContext())) {
            RcsFileDownloadHelper.downloadFile("", thumbFileInfo, new RcsFileDownloadHelper.Callback() {
                @Override
                public void onDownloadResult(String cookie, boolean succ, String filePath) {
                    if (succ) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
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
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("coo","position--:focus");
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
                Log.i("nbv","--------RcsChatbotWebActivity");
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
                new AlertDialog.Builder(context, R.style.Juphoon_Dialog_style).setTitle(R.string.chatbot_share_user_date)
                        .setPositiveButton(R.string.chatbot_sure, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.putExtra(RcsChatbotDefine.KEY_RMSURI, rmsUri);
                                intent.putExtra(RcsChatbotDefine.KEY_CHATBOT_ID, serviceId);
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

    @Override
    public void onCSSLoadOK(final ChatbotCardCSSBean chatbotCardCSSBean) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mCardCssBean = chatbotCardCSSBean;
                if (mCardBean.message.generalPurposeCardCarousel != null) {
                    rcsChatbotCardViewsAdapter.notifyDataSetChanged();
                } else {
                    View cardView = initSingleCardView(mCardBean.message);
                    ViewGroup.LayoutParams params = cardView.getLayoutParams();
                    params.width = mMaxWeight;
                    cardView.setLayoutParams(params);
                    RcsChatbotCardView.this.removeAllViews();
                    RcsChatbotCardView.this.addView(cardView);
                }
                RcsChatbotCardHelper.removeGetCSSCallback(mCookie);
            }
        });
    }


    private ListViewDialogs dialog;

    /************* RcsChatbotCardViewsAdapter *************/

    private class RcsChatbotCardViewsAdapter extends RecyclerView.Adapter<RcsChatbotCardViewsAdapter.CardViewHolder> {
        private RcsChatbotCardBean.MessageBean.GeneralPurposeCardCarouselBean mCardCarouselBean;

        public RcsChatbotCardViewsAdapter(RcsChatbotCardBean.MessageBean.GeneralPurposeCardCarouselBean generalPurposeCardCarousel) {
            this.mCardCarouselBean = generalPurposeCardCarousel;
        }

        @Override
        public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chatbot_card_view_single_vertical, parent, false);
            RecyclerView.LayoutParams cardParams = (RecyclerView.LayoutParams) view.getLayoutParams();
//            cardParams.width = (int) (getResources().getDimension(R.dimen.public_mutipe_width_medium) - (int) getResources().getDimension(R.dimen.chatbot_card_marginleft));
//            cardParams.width = 400;
//            cardParams.height = 500;
            cardParams.width = 400;
            cardParams.height = 360;

           // cardParams.height = RecyclerView.LayoutParams.WRAP_CONTENT;

            Log.i("qwe","cardParams.height:"+cardParams.height);
            float scale = 8 * mContext.getResources().getDisplayMetrics().density + 0.5f;
            Log.i("qwe","(int) scale:"+(int) scale);
            cardParams.setMargins(0, 0, (int) scale, 0);
            view.setLayoutParams(cardParams);
            return new CardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final CardViewHolder holder, final int position) {
            RcsChatbotCardBean.MessageBean.ContentBean contentBean = mCardCarouselBean.content.get(position);
            BindCardView(contentBean,holder);
        }

        @Override
        public int getItemCount() {
            return mCardCarouselBean.content.size();
        }

        private void BindCardView(final RcsChatbotCardBean.MessageBean.ContentBean card, final CardViewHolder holder) {
//            holder.setIsRecyclable(false);
            TextView contentView = holder.contentView;
            TextView titleTopView = holder.titleTopView;//title在图片上方情况
            TextView titleView = holder.titleView;
            ImageView imageView = holder.imageView;
            View mediaCardView = holder.mediaCardView;
            RcsChatbotVideoPlayer videoPlayer = holder.videoPlayer;
            ImageView audioPlayImageView = holder.audioPlayImageView;
            LinearLayout buttonLayout = holder.buttonLayout;
            LinearLayout content_layout = holder.content_layout;
            RelativeLayout relativeLayoutsm = holder.relativeLayoutsm;
            final AllCellsGlowLayout relativeLayoutss = holder.relativeLayoutss;

//            holder.itemView.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Log.i("sxx","holder.itemView.---------ontoch");
//                    if (card.suggestions != null) {
//                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
//                                ,mTrafficType,mContributionId);
//                        dialog.show();
//                    }
//                }
//            });
//
//            content_layout.setOnTouchListener(new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    Log.i("sxx","content_layout.---------ontoch");
//                    if (card.suggestions != null) {
//                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
//                                ,mTrafficType,mContributionId);
//                        dialog.show();
//                    }
//
//                    return false;
//                }
//            });
//            holder.itemView.setOnTouchListener(new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    Log.i("sxx","holder.itemView-----------ontoch");
//                    if (card.suggestions != null) {
//                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
//                                ,mTrafficType,mContributionId);
//                        dialog.show();
//                    }
//                    return false;
//                }
//            });
//            relativeLayoutss.setOnFocusChangeListener(new OnFocusChangeListener() {
//                @Override
//                public void onFocusChange(View v, boolean hasFocus) {
//                    if(hasFocus) {
//                        //ofFloatAnimator(relativeLayoutss, 1f, 1.02f);//放大
////                        relativeLayoutss.setBorderStrokeWidth(10);
////                        relativeLayoutss.setBorderColor(Color.WHITE);
//                        relativeLayoutss.setBackgroundResource(R.drawable.act_chart_choses);
//                    }else {
//                        //ofFloatAnimator(relativeLayoutss, 1.02f, 1f);
////                        relativeLayoutss.setBorderStrokeWidth(0);
////                        relativeLayoutss.setBorderColor(Color.TRANSPARENT);
//                        relativeLayoutss.setBackgroundColor(Color.TRANSPARENT);
//                    }
//                }
//            });
//            relativeLayoutsm.setOnTouchListener(new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    Log.i("sxx","relativeLayoutsm-----------ontoch");
//                    if (card.suggestions != null) {
//                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
//                                ,mTrafficType,mContributionId);
//                        dialog.show();
//                    }
//                    return false;
//                }
//            });
//            card_image_layout.setOnTouchListener(new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    Log.i("sxx","card_image_layout-----------ontoch");
//                    if (card.suggestions != null) {
//                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
//                                ,mTrafficType,mContributionId);
//                        dialog.show();
//                    }
//                    return false;
//                }
//            });
            mediaCardView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.i("sxx","mediaCardView-----------ontoch");
                    if (card.suggestions != null) {
                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
                                ,mTrafficType,mContributionId);
                        dialog.show();
                    }
                    return false;
                }
            });
            imageView.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.i("sxx","imageView-----------ontoch");
                    if (card.suggestions != null) {
                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
                                ,mTrafficType,mContributionId);
                        dialog.show();
                    }
                    return false;
                }
            });

            relativeLayoutss.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("sxx", "relativeLayoutss-----------OnClick");
                    if (card.suggestions != null) {
                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description, mConversationId, mRmsUri, mServiceId
                                , mTrafficType, mContributionId);
                        dialog.show();
                    }
                  }
                });

//            relativeLayoutss.setOnTouchListener(new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    Log.i("sxx","relativeLayoutss-----------ontoch");
//                    if (card.suggestions != null) {
//
//                        dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
//                                ,mTrafficType,mContributionId);
//                        dialog.show();
//                    }
//                    return false;
//                }
//            });


//                    if (card.media != null && card.media.mediaContentType.contains("image")) {
//                        Log.i("nbv","111111111111111111111");
//                        RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(card.media.mediaUrl, card.media.mediaFileSize).setConTenType(card.media.mediaContentType);
//                        if (TextUtils.isEmpty(RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_CHATBO_PATH))) {
//                            RcsFileDownloadHelper.downloadFile("", fileInfo,
//                                    new RcsFileDownloadHelper.Callback() {
//                                        @Override
//                                        public void onDownloadResult(String cookie, boolean succ, final String filePath) {
//                                            if (succ) {
//                                                Intent intent = new Intent(getContext(), RcsChatbotImageFragmentActivity.class);
//                                                intent.putExtra(RcsChatbotImageFragmentActivity.FILE_PATH, RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(card.media.mediaUrl).setConTenType(card.media.mediaContentType), RmsDefine.RMS_CHATBO_PATH));
//                                                getContext().startActivity(intent);
//                                            }
//                                        }
//                                    }, null, RmsDefine.RMS_CHATBO_PATH);
//                            return;
//                        }
//                        Intent intent = new Intent(getContext(), RcsChatbotImageFragmentActivity.class);
//                        intent.putExtra(RcsChatbotImageFragmentActivity.FILE_PATH, RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(card.media.mediaUrl).setConTenType(card.media.mediaContentType), RmsDefine.RMS_CHATBO_PATH));
//                        getContext().startActivity(intent);
//                    }
//                }

//            });
            int suggestionSize = 0;
            int mediaHeightType = -1;

            if (card.media != null && card.media.height != null) {
                Log.i("nbv","card.isTop-------------------------------------------");
                View mediaLayout = holder.content_layout.findViewById(R.id.content_layout);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mediaLayout.getLayoutParams();
                layoutParams.setMargins(2,0,2,2);
                mediaLayout.setLayoutParams(layoutParams);

                float height = RcsChatbotCardHelper.getCardHeight(getContext(), card.media.height);
                LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) height);
                imageView.setLayoutParams(new LayoutParams(params));
                mediaHeightType = RcsChatbotCardHelper.getCardHeightType(card.media.height);
            } else {
                View mediaLayout = holder.mediaCardView.findViewById(R.id.chatbot_card_view);
                mediaLayout.setVisibility(GONE);
            }
           // buttonLayout.setVisibility(VISIBLE);
            if (card.suggestions != null) {
              //  buttonLayout.setVisibility(VISIBLE);

                suggestionSize = card.suggestions.size();
                initSuggestionView(buttonLayout, card.suggestions);
                //设置布局
                View mediaLayout = holder.content_layout.findViewById(R.id.content_layout);
                holder.content_layout.setBackgroundResource(R.drawable.card_backgrounds);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mediaLayout.getLayoutParams();
                layoutParams.setMargins(2,0,2,0);
                mediaLayout.setLayoutParams(layoutParams);

            }

            //处理media
            if (card.media != null && card.media.mediaContentType.contains("audio")) {
                initChatbotAudioView(card, imageView, audioPlayImageView);
            } else if (card.media != null && card.media.mediaContentType.contains("image")) {
                initChatbotImageView(card, imageView);
            } else if (card.media != null && card.media.mediaContentType.contains("video")) {
                initChatbotVideoView(card, imageView, videoPlayer, audioPlayImageView);
            }
            final String contentTitleText = card.title;
            Log.i("qwe","contentTitleText--title:"+contentTitleText);
//            titleView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            if (!TextUtils.isEmpty(contentTitleText)) {
                if (card.isTop) {
                    Log.i("qwe","card.isTop");
                    titleView.setVisibility(GONE);
                    titleTopView.setVisibility(VISIBLE);
                    titleTopView.setText(contentTitleText);
                    titleTopView.setMaxLines(1);
//                    RcsChatbotCardHelper.setTextViewFlags(mCardCarouselBean.layout.titleFontStyle, titleTopView);
                } else {
                    Log.i("qwe","card.not isTop");
                    titleTopView.setVisibility(GONE);
                    titleView.setVisibility(VISIBLE);
                    titleView.setText(contentTitleText);
                    titleView.setMaxLines(1);
//                    RcsChatbotCardHelper.setTextViewFlags(mCardCarouselBean.layout.titleFontStyle, titleView);
                }
            }
            final String description = card.description;
            Log.i("qwe","description--title:"+description);

            contentView.setVisibility(View.VISIBLE);
            contentView.setText("请选择您需要的服务");

            if (!TextUtils.isEmpty(description)) {
                RcsChatbotCardHelper.setTextViewFlags(mCardCarouselBean.layout.descriptionFontStyle, contentView);
                contentView.setVisibility(View.VISIBLE);
                //如果是大卡片则根据按钮个数计算描述行数
                if (mediaHeightType == 3 && suggestionSize != 0) {
                    int descriptionLine = 3 - suggestionSize;
                    contentView.setMaxLines(descriptionLine <= 0 ? 1 : descriptionLine);
                } else {
                    contentView.setMaxLines(2);
                }
//                contentView.setText(description);
                contentView.setText("请选择您需要的服务");
                contentView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (card.suggestions != null) {

                            dialog = new ListViewDialogs(getContext(), card.suggestions, card.title, card.description,mConversationId,mRmsUri,mServiceId
                                    ,mTrafficType,mContributionId);
                            dialog.show();
                        }
                        return false;
                    }
                });
                contentView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //点击显示全部的事件
                        Intent intent = new Intent(getContext(), RcsChatbotFullTextFragmentActivity.class);
                        intent.putExtra(RcsChatbotFullTextFragmentActivity.TITLE, contentTitleText);
                        intent.putExtra(RcsChatbotFullTextFragmentActivity.CONTENT, description);
                        getContext().startActivity(intent);
                    }
                });
            }
            if (mCardCssBean != null) {
                if (card.isTop) {
                    RcsChatbotCardHelper.setCardStyle(mCardCssBean, contentView, titleTopView, holder.itemView);
                } else {
//                    RcsChatbotCardHelper.setCardStyle(mCardCssBean, contentView, titleView, holder.itemView);
                }
            }
        }
        private void ofFloatAnimator(View view,float start,float end){
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(200);//动画时间
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", start, end);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", start, end);
            animatorSet.setInterpolator(new DecelerateInterpolator());//插值器
           // view.setBackgroundResource(R.drawable.act_chart_choses);
            animatorSet.play(scaleX).with(scaleY);//组合动画,同时基于x和y轴放大
            animatorSet.start();
        }
        class CardViewHolder extends RecyclerView.ViewHolder {
            View mediaCardView;
            TextView contentView;
            TextView titleTopView;
            TextView titleView;
            RoundCornerImageView imageView;
            RcsChatbotVideoPlayer videoPlayer;
            ImageView audioPlayImageView;
            LinearLayout buttonLayout;
            AllCellsGlowLayout relativeLayoutss;
            LinearLayout content_layout;
            RelativeLayout relativeLayoutsm;

            RadiusCardView card_image_layout;

            public CardViewHolder(View view) {
                super(view);
                contentView = (TextView) view.findViewById(R.id.card_content);
                titleTopView = (TextView) view.findViewById(R.id.card_title_top);
                titleView = (TextView) view.findViewById(R.id.card_title);
                imageView = view.findViewById(R.id.card_image);
                videoPlayer = (RcsChatbotVideoPlayer) view.findViewById(R.id.rcs_chatbot_video_player);
                audioPlayImageView = (ImageView) view.findViewById(R.id.card_image_audio_play);
                buttonLayout = view.findViewById(R.id.card_action);
                mediaCardView = view.findViewById(R.id.chatbot_card_view);
                relativeLayoutsm = view.findViewById(R.id.card_radius_layout);
                relativeLayoutss = view.findViewById(R.id.card_layout);
                content_layout = view.findViewById(R.id.content_layout);
                card_image_layout = view.findViewById(R.id.card_image_layout);
            }
        }

    }
}