package com.juphoon.chatbotmaap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.github.lzyzsd.jsbridge.BridgeWebView;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;

import static android.os.Environment.*;

public class RcsChatbotWebActivity extends AppCompatActivity {


    public final static String PARAMETERS = "parameters";
    public final static String VIEWMODE = "viewMode";
    public final static String URL = "url";
    private BridgeWebView mWebView;
    private String mUri;
    private String mViewMode;
    private String mParameters;
    private int REQUEST_CODE = 1000;
    private ValueCallback<Uri[]>  mUploadLoadUri;
    private Uri mTakePhotoImageUri;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.rcs_chatbot_webview);
        mUri = getIntent().getStringExtra(URL);
        mViewMode = getIntent().getStringExtra(VIEWMODE);
        mParameters = getIntent().getStringExtra(PARAMETERS);

        initToolbar();
        initWebView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            destroyWebView();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.chatbot_activity_webview_menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            if(mWebView.canGoBack()){
                mWebView.goBack();
            }else {
                destroyWebView();
                finish();
            }
            return true;
        } else if (i == R.id.action_share) {
            startShareActivity();
            return true;
        } else if (i == R.id.open_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mUri));
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initToolbar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initWebView() {

        mWebView = (BridgeWebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setWebChromeClient(new MyWebChromeClient());
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);  //支持js
        webSettings.setDomStorageEnabled(true);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);  //提高渲染的优先级
        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true);  //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        webSettings.setSupportZoom(true);  //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。
        //若上面是false，则该WebView不可缩放，这个不管设置什么都不能缩放。
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); //支持内容重新布局
        webSettings.supportMultipleWindows();  //多窗口
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);  //关闭webview中缓存
        webSettings.setAllowFileAccess(true);  //设置可以访问文件
        webSettings.setNeedInitialFocus(true); //当webview调用requestFocus时为webview设置节点
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true);  //支持自动加载图片
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        Uri uri = Uri.parse(mUri);
        if (uri.getScheme() == null) {
            mUri = Uri.parse("http://" + mUri).toString();
        }
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(url));
                startActivity(intent);

            }
        });
        mWebView.loadUrl(mUri);
    }


    private void startShareActivity() {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        if (mUri != null) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, mUri + "\r\n" + getString(R.string.chatbot_from_rcs));
            shareIntent.setType("text/plain");
        }
        final CharSequence title = getResources().getText(R.string.action_share);
        startActivity(Intent.createChooser(shareIntent, title));
    }
    class MyWebChromeClient extends  WebChromeClient{
        private final static String IMAGE_MIME_TYPE = "image/*";
        private final static String VIDEO_MIME_TYPE = "video/*";
        private final static String AUDIO_MIME_TYPE = "audio/*";

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            finish();
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadLoadUri = filePathCallback;
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            String mimeType = null;
            if (acceptTypes != null && acceptTypes.length > 0) {
                mimeType = acceptTypes[0];
            }
            Intent captureIntent = null;
            if (mimeType.equals(IMAGE_MIME_TYPE)) {
                String filePath = getExternalStorageDirectory() + File.separator
                        + DIRECTORY_PICTURES + File.separator;
                String fileName = "IMG_" + DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".jpg";
                File file = new File(filePath + fileName);
                mTakePhotoImageUri = RcsChatbotFileProvider.getUriForFile(RcsChatbotWebActivity.this, RcsChatbotFileProvider.AUTHORITY, file);
                captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTakePhotoImageUri);
            } else if (mimeType.equals(VIDEO_MIME_TYPE)) {
                captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            }
            Intent chooserIntent = Intent.createChooser(fileChooserParams.createIntent(), "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});
            startActivityForResult(chooserIntent, REQUEST_CODE);
            return true;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            Uri[] uri = parseResult(resultCode, data);
            if (uri == null && mTakePhotoImageUri != null) {
                uri = new Uri[1];
                uri[0] = mTakePhotoImageUri;
            }
            if (mUploadLoadUri != null) {
                mUploadLoadUri.onReceiveValue(uri);
            }
        }
    }

    private Uri[] parseResult(int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return null;
        }
        Uri result = intent == null || resultCode != Activity.RESULT_OK ? null
                : intent.getData();
        Uri[] uris = null;
        if (result != null) {
            uris = new Uri[1];
            uris[0] = result;
        }
        return uris;
    }

    class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    finish();
                    return true;
                }
            } catch (Exception e) {
                return true;
            }
            return false;
        }

        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage(getString(R.string.chatbot_webview_ssl_fail));
            builder.setPositiveButton(getString(R.string.chatbot_sure), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.proceed();
                }
            });

            builder.setNegativeButton(getString(R.string.chatbot_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.cancel();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request);
        }
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    private void destroyWebView() {
        if (mWebView != null) {
            mWebView.freeMemory();
            mWebView.destroy();
        }
    }
}
