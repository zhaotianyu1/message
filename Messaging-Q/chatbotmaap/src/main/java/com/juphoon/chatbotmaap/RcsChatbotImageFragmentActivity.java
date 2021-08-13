package com.juphoon.chatbotmaap;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;

import com.bm.library.PhotoView;
import com.juphoon.helper.RcsBitmapCache;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.service.RmsDefine;

import java.io.File;


public class RcsChatbotImageFragmentActivity extends FragmentActivity {

    public final static String FILE_PATH = "file_path";
    public final static String URL = "url";
    public final static String CHATBOT_ICON = "chatbot_icon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_view_image);
        Window window = getWindow();
        if (window != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            window.setLayout(width, height);
        }
        if (getIntent().getBooleanExtra(CHATBOT_ICON, false)) {
            initChatBotIcon();
        } else {
            initView();
        }
    }


    public void initView() {
        String filePath = getIntent().getStringExtra(FILE_PATH);
        String url = getIntent().getStringExtra(URL);
        final PhotoView imageView = (PhotoView) findViewById(R.id.image_layout);
        imageView.enable();
        if (!TextUtils.isEmpty(filePath)) {
            RcsImageHelper.setImage(this, filePath, imageView);
        } else if (!TextUtils.isEmpty(url)) {
            RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(url, 1000);
            String thumbPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_ICON_PATH);
            if (TextUtils.isEmpty(thumbPath)) {
                RcsFileDownloadHelper.downloadFile("", fileInfo, new RcsFileDownloadHelper.Callback() {
                    @Override
                    public void onDownloadResult(String cookie, boolean succ, String filePath) {
                        if (succ) {
                            RcsImageHelper.setImage(RcsChatbotImageFragmentActivity.this, filePath, imageView);
                        }
                    }
                }, null, RmsDefine.RMS_ICON_PATH);
            } else {
                imageView.setImageURI(Uri.fromFile(new File(thumbPath)));
            }
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void initChatBotIcon() {
        String filePath = getIntent().getStringExtra(FILE_PATH);
        String url = getIntent().getStringExtra(URL);
        final ImageView imageView = (ImageView) findViewById(R.id.image_layout);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(RcsChatbotNiceImageView.dp2px(this, 400), RcsChatbotNiceImageView.dp2px(this, 400)));
        imageView.setImageResource(R.drawable.chatbot_avatar);
        if (!TextUtils.isEmpty(filePath)) {
            RcsBitmapCache.getBitmapFromPath(imageView, filePath);
        } else if (!TextUtils.isEmpty(url)) {
            RcsFileDownloadHelper.FileInfo fileInfo = new RcsFileDownloadHelper.FileInfo(url, 1000);
            String thumbPath = RcsFileDownloadHelper.getPathFromFileInfo(fileInfo, RmsDefine.RMS_ICON_PATH);
            if (TextUtils.isEmpty(thumbPath)) {
                RcsFileDownloadHelper.downloadFile("", fileInfo, new RcsFileDownloadHelper.Callback() {
                    @Override
                    public void onDownloadResult(String cookie, boolean succ, String filePath) {
                        if (succ) {
                            RcsBitmapCache.getBitmapFromPath(imageView, filePath, false);
                        }
                    }
                }, null, RmsDefine.RMS_ICON_PATH);
            } else {
                RcsBitmapCache.getBitmapFromPath(imageView, thumbPath, false);
            }
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
