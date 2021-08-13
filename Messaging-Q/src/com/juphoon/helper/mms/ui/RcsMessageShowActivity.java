package com.juphoon.helper.mms.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.juphoon.chatbotmaap.view.RoundImageView;
import com.juphoon.helper.mms.RcsMmsUtils;

import java.io.File;


public class RcsMessageShowActivity extends Activity {

    private final static String TYPE = "type";
    private final static String VALUE = "text";
    private final static String MESSAGE_ID = "message_id";

    private final static int TYPE_IMAGE = 1;
    private final static int TYPE_VIDEO = 2;
    private final static int TYPE_TEXT = 3;
    private final static int TYPE_VOICE = 4;

    public static void startVoiceActivity(Context context, long messageId, String path) {
        Intent intent = new Intent(context, RcsMessageShowActivity.class);
        intent.putExtra(VALUE, path);
        intent.putExtra(TYPE, TYPE_VOICE);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }

    public static void startImageActivity(Context context, long messageId, String path) {
        Intent intent = new Intent(context, RcsMessageShowActivity.class);
        intent.putExtra(VALUE, path);
        intent.putExtra(TYPE, TYPE_IMAGE);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }

    public static void startVideoActivity(Context context, long messageId, String path) {
        Intent intent = new Intent(context, RcsMessageShowActivity.class);
        intent.putExtra(VALUE, path);
        intent.putExtra(TYPE, TYPE_VIDEO);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }

    public static void startTextActivity(Context context, long messageId, String text) {
        Intent intent = new Intent(context, RcsMessageShowActivity.class);
        intent.putExtra(VALUE, text);
        intent.putExtra(TYPE, TYPE_TEXT);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }

    private long mMessageId;
    private String mFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int type = getIntent().getIntExtra(TYPE, -1);
        mMessageId = getIntent().getLongExtra(MESSAGE_ID, -1);
        String value = getIntent().getStringExtra(VALUE);
        if (type != TYPE_IMAGE && type != TYPE_VIDEO && type != TYPE_TEXT && type != TYPE_VOICE) {
            finish();
            return;
        }
        if (TextUtils.isEmpty(value)) {
            finish();
            return;
        }
        if (type == TYPE_IMAGE) {
            mFilePath = value;
            RoundImageView imageView = new RoundImageView(this);
            imageView.setImageURI(Uri.fromFile(new File(value)));
            imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            imageView.setBackgroundColor(Color.BLACK);
            imageView.setImageBitmap(RcsMmsUtils.getCompressImage(mFilePath));
            setContentView(imageView);
        } else if (type == TYPE_VIDEO || type == TYPE_VOICE) {
            mFilePath = value;
            VideoView videoView = new VideoView(this);
            videoView.setVideoURI(Uri.fromFile(new File(value)));
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            videoView.start();
            videoView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            setContentView(videoView);
        } else if (type == TYPE_TEXT) {
            TextView textView = new TextView(this);
            textView.setText(value);
            textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            textView.setGravity(Gravity.CENTER);
            setContentView(textView);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
