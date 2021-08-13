package com.juphoon.helper.mms.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.core.app.ActivityCompat;

import com.android.messaging.R;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.service.RmsDefine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RcsPreviewImageActivity extends Activity {
    public static final String RESULT_IS_FULL_IMAGE_SEND = "isFullImageSend";
    public static final String FILE_URI = "uri";

    private ImageView mPrevireImageView;
    private CheckBox mCheckBox;
    private Uri mUri;
    private ProgressBar mProgressBar;
    private RelativeLayout mRelativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_preview_image);
        mUri = (Uri) getIntent().getParcelableExtra(FILE_URI);
        mPrevireImageView = (ImageView) findViewById(R.id.iv_rcspreviewimageactivity_preview);
        mCheckBox = (CheckBox) findViewById(R.id.btn_rcspreviewimageactivity_full_image);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_rcspreviewimageactivity);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.relative);

        checkBoxSetListener();
        //用压缩图显示，防止大图片占用内存
        new AsyncTask<Uri, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Uri... arg0) {
                return RcsMmsUtils.genPreviewImage(RcsFileUtils.getFilePathByUri(RcsPreviewImageActivity.this, arg0[0]));
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                mProgressBar.setVisibility(View.GONE);
                if (result != null) {
                    mRelativeLayout.setVisibility(View.VISIBLE);
                    mPrevireImageView.setImageBitmap(result);
                }
            }
            
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Uri[] { mUri });
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void checkBoxSetListener() {
        mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                if (isChecked) {
                    checkBoxSetText();
                    mCheckBox.setTextColor(Color.WHITE);
                } else {
                    mCheckBox.setText(R.string.full_image);
                    mCheckBox.setTextColor(Color.BLACK);
                }
            }
        });
    }

    private void checkBoxSetText() {
        InputStream input = null;
        try {
            input = getContentResolver().openInputStream(mUri);
            long size = input.available();
            mCheckBox.setText(new StringBuffer().append(getString(R.string.full_image)).append("(").append(RcsMmsUtils.formatFileSize(size)).append(")").toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_rcspreviewimageactivity_send:
                finshActivity(mCheckBox.isChecked());
                break;
            default:
                break;
        }
    }

    private void finshActivity(final boolean isFullImage) {
        new AsyncTask<Uri, Void, Uri>() {

            @Override
            protected void onPreExecute() {
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Uri doInBackground(Uri... arg0) {
                if (!isFullImage) {
                    File file = new File(RcsFileUtils.getFilePathByUri(RcsPreviewImageActivity.this, mUri));
                    if (file.getAbsolutePath().contains(getPackageName())) {
                        String copyPath = RcsFileUtils.getNotExistFilePath(RmsDefine.RMS_FILE_PATH, file.getName());
                        file = new File(copyPath);
                        try {
                            RcsMmsUtils.copyToFile(getContentResolver().openInputStream(mUri), file);
                        } catch (FileNotFoundException e) {
                            return mUri;
                        }
                    }
                    return Uri.parse("file://" + RcsMmsUtils.compressImage(file.getAbsolutePath(), 300 * 1024));
                } else {
                    return mUri;
                }
            }

            @Override
            protected void onPostExecute(Uri result) {
                mProgressBar.setVisibility(View.GONE);
                Intent intent = new Intent();
                intent.setData(result);
                setResult(RESULT_OK, intent);
                finish();
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
