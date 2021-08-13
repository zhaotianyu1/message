package com.juphoon.chatbotmaap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbotmaap.chatbotcard.ChatbotCardCSSBean;
import com.juphoon.chatbotmaap.view.RoundLayoutView;
import com.juphoon.helper.RcsChatbotHelper;
import com.juphoon.helper.RcsFileDownloadHelper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.service.RmsDefine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RcsChatbotCardHelper {
    public final static String CARD_ORIENTATION_VERTICAL = "VERTICAL";
    public final static String CARD_ORIENTATION_HORIZONTAL = "HORIZONTAL";
    public final static String CARD_IMAGEALGINMENT_LEFT = "LEFT";
    public final static String CARD_IMAGEALGINMENT_RIGHT = "RIGHT";
    public final static String CARD_IMAGE_SHORT_HEIGHT = "SHORT_HEIGHT";
    public final static String CARD_IMAGE_TALL_HEIGHT = "TALL_HEIGHT";
    public final static String CARD_IMAGE_MEDIUM_HEIGHT = "MEDIUM_HEIGHT";
    private static HashMap<String, WeakReference<OnGetCSSCallBack>> sListGetCSSCallbacks = new HashMap<>();


    public interface OnGetCSSCallBack {
        void onCSSLoadOK(ChatbotCardCSSBean filePath);
    }

    public static void addGetCSSCallback(String mCookie, OnGetCSSCallBack callback) {
        sListGetCSSCallbacks.put(mCookie, new WeakReference<>(callback));
    }

    public static void removeGetCSSCallback(String mCookie) {
        sListGetCSSCallbacks.remove(mCookie);
    }

    public static String getCSSFilePath(String cssStyleUrl, String serviceId, final String cookie, OnGetCSSCallBack callback) {
        String cssFile = "";
        if (!TextUtils.isEmpty(cssStyleUrl)) {
            cssFile = RcsFileDownloadHelper.getPathFromFileInfo(
                    new RcsFileDownloadHelper.FileInfo(cssStyleUrl).setConTenType("css/css"),
                    RmsDefine.RMS_CHATBO_PATH);
        } else {
            //获取详情内CSS的地址
            RcsChatbotHelper.RcsChatbot mChatbotInfo = RcsChatbotHelper.getChatbotInfoByServiceId(serviceId);
            if (mChatbotInfo != null) {
                RcsChatbotInfoBean mRcsChatbotInfoBean = new Gson().fromJson(mChatbotInfo.json, RcsChatbotInfoBean.class);
                if (mRcsChatbotInfoBean != null) {
                    cssFile = RcsFileDownloadHelper.getPathFromFileInfo(new RcsFileDownloadHelper.FileInfo(mRcsChatbotInfoBean.genericCSStemplate).setConTenType("css/css"), RmsDefine.RMS_CHATBO_PATH);
                    if (!TextUtils.isEmpty(mRcsChatbotInfoBean.genericCSStemplate)) {
                        cssStyleUrl = mRcsChatbotInfoBean.genericCSStemplate;
                    }
                }
            }
        }
        if (TextUtils.isEmpty(cssFile) && !TextUtils.isEmpty(cssStyleUrl)) {
            RcsChatbotCardHelper.addGetCSSCallback(cookie, callback);
            RcsFileDownloadHelper.downloadFile(cookie,
                    new RcsFileDownloadHelper.FileInfo(cssStyleUrl).setConTenType("css/css"),
                    new RcsFileDownloadHelper.Callback() {
                        @Override
                        public void onDownloadResult(String downloadCookie, boolean succ, final String filePath) {
                            if (!succ) {
                                Log.d("CSS", "onDownloadResult: failed ");
                                return;
                            }
                            if (!TextUtils.equals(downloadCookie, cookie)) return;
                            if (sListGetCSSCallbacks.containsKey(downloadCookie)) {
                                sListGetCSSCallbacks.get(downloadCookie).get().onCSSLoadOK(RcsChatbotCardHelper.TransFileToCSSBean(filePath));
                            }
                            removeGetCSSCallback(downloadCookie);
                        }
                    }, null, RmsDefine.RMS_CHATBO_PATH);
        }
        return cssFile;
    }

    public static float getCardHeight(Context context, String cardHeight) {
        Resources resourece = context.getResources();
        float height = 0;
        if (!TextUtils.isEmpty(cardHeight)) {
            switch (cardHeight.toUpperCase()) {
                case RcsChatbotCardHelper.CARD_IMAGE_SHORT_HEIGHT:
                    height = resourece.getDimension(R.dimen.public_chatbot_single_height_short);
                    break;
                case RcsChatbotCardHelper.CARD_IMAGE_TALL_HEIGHT:
                    height = resourece.getDimension(R.dimen.public_chatbot_single_height_tall);
                    break;
                case RcsChatbotCardHelper.CARD_IMAGE_MEDIUM_HEIGHT:
                    height = resourece.getDimension(R.dimen.public_chatbot_single_height_medium);
                    break;
                default:
                    break;
            }
        }
        return height;
    }

    public static float getCardWeight(Context context) {
        return context.getResources().getDimension(R.dimen.public_chatbot_single_weight);
    }

    public static int getCardHeightType(String cardHeight) {
        int type = -1;
        if (!TextUtils.isEmpty(cardHeight)) {
            switch (cardHeight.toUpperCase()) {
                case RcsChatbotCardHelper.CARD_IMAGE_SHORT_HEIGHT:
                    type = 1;
                    break;
                case RcsChatbotCardHelper.CARD_IMAGE_TALL_HEIGHT:
                    type = 2;
                    break;
                case RcsChatbotCardHelper.CARD_IMAGE_MEDIUM_HEIGHT:
                    type = 3;
                    break;
                default:
                    break;
            }
        }
        return type;
    }


    public static ChatbotCardCSSBean TransFileToCSSBean(String cssFile) {
        String css = readCSSFile(cssFile);
        if (TextUtils.isEmpty(css)) return null;
        ChatbotCardCSSBean chatbotCardCSS = new ChatbotCardCSSBean();
        css.trim();
        try {
            String message;
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
                                chatbotCardCSS.messageBean.backgroudImage = url.contains("url") ? url.substring("url(\'".length(), url.length() - 2) : url;
                                break;
                        }
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            Log.d("CSS", "parseCSS Message: " + e.getMessage());
        }
        try {
            if (css.contains("message.content.title")) {
                String title;
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
        } catch (Exception e) {
            Log.d("CSS", "parseCSS title: " + e.getMessage());
        }
        try {
            if (css.contains("message.content.description")) {
                String description;
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
        } catch (Exception e) {
            Log.d("CSS", "parseCSS description: " + e.getMessage());
        }
        try {
            if (css.contains("message.content.suggestions")) {
                String suggestions;
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
        } catch (Exception e) {
            Log.d("CSS", "parseCSS suggestions: " + e.getMessage());
        }
        return chatbotCardCSS;
    }

    public static void setButtonTextFontStyle(ChatbotCardCSSBean chatbotCardCSSBean, Button button) {
        if (chatbotCardCSSBean == null || button == null) return;
        try {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("color", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.color) ? chatbotCardCSSBean.messageBean.color : chatbotCardCSSBean.suggestionsBean.color);
            hashMap.put("fontFamily", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.fontFamily) ? chatbotCardCSSBean.messageBean.fontFamily : chatbotCardCSSBean.suggestionsBean.fontFamily);
            hashMap.put("fontSize", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.fontSize) ? chatbotCardCSSBean.messageBean.fontSize : chatbotCardCSSBean.suggestionsBean.fontSize);
            hashMap.put("fontWeight", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.fontWeight) ? chatbotCardCSSBean.messageBean.fontWeight : chatbotCardCSSBean.suggestionsBean.fontWeight);
            hashMap.put("textAlign", TextUtils.isEmpty(chatbotCardCSSBean.suggestionsBean.textAlign) ? chatbotCardCSSBean.messageBean.textAlign : chatbotCardCSSBean.suggestionsBean.textAlign);
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
                        } catch (Exception e) {
                            Log.d("CSS", "setButtonTextFontStyle color can't parse " + e.getMessage());
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
                            button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getCssTextSize(value));
                        }
                        break;
                    case "fontWeight":
                        try {
                            if (TextUtils.equals(value, "bold") || Integer.valueOf(value) > 400) {
                                button.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                            }
                        } catch (Exception e) {
                            Log.d("CSS", "setButtonTextFontStyle fontWeight can't parse: " + e.getMessage());
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
            Log.d("CSS", "setButtonTextFontStyle: " + e.getMessage());
        }
    }

    private static String readCSSFile(String filePath) {
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

    private static int getCssTextSize(String testcss) {
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

    public static void setTextViewFlags(List<String> list, TextView textView) {
        if (list == null || textView == null) return;
        try {
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
            Log.d("CSS", "setTextViewFlags: " + e.getMessage());
        }
    }

    public static void setCardStyle(ChatbotCardCSSBean chatbotCardCSSBean, TextView contentView, TextView titleView, View view) {
        if (chatbotCardCSSBean == null) return;
        try {
            if (chatbotCardCSSBean.titleBean != null) {
                setTextViewFontStyle(chatbotCardCSSBean, titleView, true);
            }
            if (chatbotCardCSSBean.messageBean != null) {
                setCardViewBackground(chatbotCardCSSBean, view);
            }
            if (chatbotCardCSSBean.descriptionBean != null) {
                setTextViewFontStyle(chatbotCardCSSBean, contentView, false);
            }
        } catch (Exception e) {
            Log.d("CSS", "setCardStyle: " + e.getMessage());
        }
    }

    private static void setCardViewBackground(ChatbotCardCSSBean chatbotCardCSSBean, final View view) {
        GradientDrawable myGrad = (GradientDrawable) view.getBackground();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("color", chatbotCardCSSBean.messageBean.color);
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
                    myGrad.mutate();
                    try {
                        int backgroudColor = Color.parseColor(value);
                        myGrad.setColor(backgroudColor);
                    } catch (Exception e) {
                        Log.d("CSS", "setCardViewBackground color can't parse " + e.getMessage());
                    }
                    break;
                case "backgroudImage":
                    if (!TextUtils.isEmpty(value)) {
                        RcsFileDownloadHelper.FileInfo cssFileInfo = new RcsFileDownloadHelper.FileInfo(value).setConTenType(RcsFileUtils.getFileType(value));
                        String cssThump = RcsFileDownloadHelper.getPathFromFileInfo(cssFileInfo, RmsDefine.RMS_CHATBO_PATH);
                        if (TextUtils.isEmpty(cssThump)) {
                            RcsFileDownloadHelper.downloadFile("", cssFileInfo, new RcsFileDownloadHelper.Callback() {
                                @Override
                                public void onDownloadResult(String cookie, boolean succ, String filePath) {
                                    if (!succ) return;
                                    view.setBackground(new BitmapDrawable(BitmapFactory.decodeFile(filePath)));
                                }
                            }, null, RmsDefine.RMS_CHATBO_PATH);
                        } else {
                            View layout = view.findViewById(R.id.card_radius_layout);
                            if (layout != null) {
                                layout.setBackground(new BitmapDrawable(RcsChatbotImageUtil.getCssBackgroudBitmapFromPath(cssThump)));
                            }
                        }
                    } else {
                        continue;
                    }
                    break;
            }
        }
    }

    private static void setTextViewFontStyle(ChatbotCardCSSBean chatbotCardCSSBean, TextView textView, boolean isTitle) {
        if (chatbotCardCSSBean == null || textView == null) return;
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
                        textView.setTextColor(Color.parseColor(value));
                    } catch (Exception e) {
                        Log.d("CSS", "setTextViewFontStyle color can't parse " + e.getMessage());
                    }
                    break;
                case "fontFamily":
                    switch (value) {
                        case "sans":
                            textView.setTypeface(Typeface.SANS_SERIF);
                            break;
                        case "noraml":
                            textView.setTypeface(Typeface.DEFAULT);
                            break;
                        case "serif":
                            textView.setTypeface(Typeface.SERIF);
                            break;
                        case "monospace":
                            textView.setTypeface(Typeface.MONOSPACE);
                            break;
                        case "sans_serif":
                            textView.setTypeface(Typeface.SANS_SERIF);
                            break;
                        default:
                            textView.setTypeface(Typeface.DEFAULT);
                            break;
                    }
                    break;
                case "fontSize":
                    if (getCssTextSize(value) > 0) {
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getCssTextSize(value));
                    }
                    break;
                case "fontWeight":
                    //400为普通 700为粗体 Android 没有设置weight的方法。
                    try {
                        if (TextUtils.equals(value, "bold") || Integer.valueOf(value) > 400) {
                            textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                        }
                    } catch (Exception e) {
                        Log.d("CSS", "setTextViewFontStyle fontWeight can't parse: " + e.getMessage());
                    }
                    break;
                case "textAlign":
                    if (TextUtils.equals(value, "right")) {
                        textView.setGravity(Gravity.RIGHT);
                    } else if (TextUtils.equals(value, "left")) {
                        textView.setGravity(Gravity.LEFT);
                    } else {
                        textView.setGravity(Gravity.CENTER_HORIZONTAL);
                    }
                    break;
            }
        }
    }

}
