package com.android.messaging.ui.mediapicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MediaPickerMessagePartData;
import com.android.messaging.ui.mediapicker.MediaPicker.ExternalViewClick;
import com.android.messaging.ui.mediapicker.MediaPicker.ExternalViewController;
import com.android.messaging.ui.mediapicker.RcsMoreChooserAdapter.OnItemClickLitener;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.UiUtils;
import com.juphoon.helper.mms.ui.RcsBaiduLocationActivity;

import java.util.ArrayList;

//juphoon
public class RcsMoreChooser extends MediaChooser
        implements ExternalViewController {

    private Context mContext;
    private RecyclerView mMoreRecyclerView;
    private RcsMoreChooserAdapter mAdapter;
    private ArrayList<MoreEntity> list = new ArrayList<MoreEntity>();
    private ExternalViewClick externalViewClick;
    public static final String ITEM_SEND_FILE = "send_file";
    public static final String ITEM_SEND_AUDIO = "send_audio";
    public static final String ITEM_SEND_VIDEO = "send_video";
    public static final String ITEM_SEND_VCARD = "send_vcard";
    public static final String ITEM_SEND_GEO = "send_geo";

    RcsMoreChooser(MediaPicker mediaPicker, Context context) {
        super(mediaPicker);
        this.mContext = context;
        initData();
    }

    @Override
    public int getSupportedMediaTypes() {
        return MediaPicker.MEDA_TYPE_MORE;
    }

    @Override
    int getIconResource() {
//        return R.drawable.ic_add_white;
          return R.drawable.act_local;
    }
    @Override
    int getIconFoucusResource() {
        return R.drawable.voice_focus;
    }

    @Override
    int getIconDescriptionResource() {
        return R.string.mediapicker_moreChooserDescription;
    }

    @Override
    int getActionBarTitleResId() {
        return R.string.mediapicker_more_title;
    }
    public static final int REQUEST_CODE_SEND_GEO         = 207;
    @Override
    protected View createView(ViewGroup container) {
        mMediaPicker.dispatchOnExternalViewListener(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.mediapicker_more_chooser,
                container /* root */, false /* attachToRoot */);
        mMoreRecyclerView = (RecyclerView) view
                .findViewById(R.id.mediapicker_more_chooser_recyclerview);
        // 设置布局管理器
        mMoreRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(3,
                StaggeredGridLayoutManager.VERTICAL));
        // 设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mAdapter = new RcsMoreChooserAdapter(mContext, list);

        mAdapter.setOnItemClickLitener(new OnItemClickLitener() {

            @Override
            public void onItemClick(View view, int position) {
                if (TextUtils.equals(list.get(position).getItemViewTag(), ITEM_SEND_GEO)
                        && !OsUtil.hasLocationPermission()) {
                    requestLocationPermission();
                } else {
                    externalViewClick.onItemClick(list.get(position).getItemViewTag());
                }
            }
        });
        mMoreRecyclerView.setAdapter(mAdapter);
        return view;
    }

    private void initData() {
//        MoreEntity sendAudio = new MoreEntity();
//        sendAudio.setItemViewTag(ITEM_SEND_AUDIO);
//        sendAudio.setStrResId(R.string.voice_message);
//        sendAudio.setImgResId(R.drawable.chatbot_voice);
//        sendAudio.setEnable(true);
//        sendAudio.setSelect(false);
//        list.add(sendAudio);
//
//        MoreEntity sendVideo = new MoreEntity();
//        sendVideo.setItemViewTag(ITEM_SEND_VIDEO);
//        sendVideo.setStrResId(R.string.video_message);
//        sendVideo.setImgResId(R.drawable.chatbot_video);
//        sendVideo.setEnable(true);
//        sendVideo.setSelect(false);
//        list.add(sendVideo);
//
//        MoreEntity sendFile = new MoreEntity();
//        sendFile.setItemViewTag(ITEM_SEND_FILE);
//        sendFile.setStrResId(R.string.file_message);
//        sendFile.setImgResId(R.drawable.chatbot_document);
//        sendFile.setEnable(true);
//        sendFile.setSelect(false);
//        list.add(sendFile);

//        MoreEntity sendVcard = new MoreEntity();
////        sendVcard.setItemViewTag(ITEM_SEND_VCARD);
////        sendVcard.setStrResId(R.string.vcard_message);
////        sendVcard.setImgResId(R.drawable.chatbot_card);
////        sendVcard.setEnable(true);
////        sendVcard.setSelect(false);
////        list.add(sendVcard);

        MoreEntity sendGeo = new MoreEntity();
        sendGeo.setItemViewTag(ITEM_SEND_GEO);
        sendGeo.setStrResId(R.string.conversation_list_snippet_geo);
//        sendGeo.setImgResId(R.drawable.chatbot_positionicon);
//        sendGeo.setImgResId(R.drawable.chatbot_position);
        sendGeo.setImgResId(R.drawable.act_local);
        sendGeo.setEnable(true);
        sendGeo.setSelect(false);
        list.add(sendGeo);
        
    }

    class MoreEntity {
        String itemViewTag;
        int imgResId;
        int strResId;
        boolean isSelect;
        boolean isEnable;

        MoreEntity() {
        }

        public String getItemViewTag() {
            return itemViewTag;
        }

        public void setItemViewTag(String itemViewTag) {
            this.itemViewTag = itemViewTag;
        }

        public int getStrResId() {
            return strResId;
        }

        public void setStrResId(int strResId) {
            this.strResId = strResId;
        }

        public int getImgResId() {
            return imgResId;
        }

        public void setImgResId(int imgResId) {
            this.imgResId = imgResId;
        }

        public boolean getIsSelect() {
            return isSelect;
        }

        public void setSelect(boolean isSelect) {
            this.isSelect = isSelect;
        }

        public boolean getIsEnable() {
            return isEnable;
        }

        public void setEnable(boolean isEnable) {
            this.isEnable = isEnable;
        }

    }

    @Override
    public void setOnClickListener(ExternalViewClick click) {
        externalViewClick = click;
    }

    @Override
    public void setSelectState(String tag, boolean select) {
        for (MoreEntity moreEntity : list) {
            if (moreEntity.getItemViewTag().equals(tag)) {
                moreEntity.setSelect(select);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setEnableState(String tag, boolean enable) {
        for (MoreEntity moreEntity : list) {
            if (moreEntity.getItemViewTag().equals(tag)) {
                moreEntity.setEnable(enable);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setMoreChooserMessage(String tag, String msg) {
        switch (tag) {
        case ITEM_SEND_AUDIO:
            Log.i("cccc","语音项目。。。。。。。。。。");
            final Rect startRectA = new Rect();
            mMediaPicker.dispatchItemsSelected(new MediaPickerMessagePartData(startRectA, null, ContentType.AUDIO_UNSPECIFIED, Uri.parse("file://" + msg), 80, 80), true /* dismissMediaPicker */);
            break;
        case ITEM_SEND_VIDEO:
            final Rect startRectV = new Rect();
            mMediaPicker.dispatchItemsSelected(new MediaPickerMessagePartData(startRectV, null, ContentType.VIDEO_UNSPECIFIED, Uri.parse("file://" + msg), 80, 80), true /* dismissMediaPicker */);
            break;
        case ITEM_SEND_FILE:
            final Rect startRect = new Rect();
            mMediaPicker.dispatchItemsSelected(new MediaPickerMessagePartData(startRect, null, ContentType.APP_FILE_MESSAGE, Uri.parse("file://" + msg), 80, 80), true /* dismissMediaPicker */);
            break;
        case ITEM_SEND_VCARD:
            final Rect startRectVcard = new Rect();
            mMediaPicker.dispatchItemsSelected(new MediaPickerMessagePartData(startRectVcard, null, ContentType.TEXT_VCARD, Uri.parse("file://" + msg), 80, 80), true /* dismissMediaPicker */);
            break;
        case ITEM_SEND_GEO:
            Log.i("cccc","地理位置项目。。。。。。。。。。");
            final Rect startRectGeo = new Rect();
            mMediaPicker.dispatchItemsSelected(new MediaPickerMessagePartData(startRectGeo, null, ContentType.APP_GEO_MESSAGE, Uri.parse("file://" + msg), 80, 80), true /* dismissMediaPicker */);
            break;
        default:
            break;
        }
    }

    private void requestLocationPermission() {
        mMediaPicker.requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                MediaPicker.LOCATION_PERMISSION_REQUEST_CODE);
    }
}
