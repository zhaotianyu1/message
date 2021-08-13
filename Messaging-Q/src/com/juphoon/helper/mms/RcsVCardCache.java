package com.juphoon.helper.mms;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.messaging.R;
import com.juphoon.helper.mms.RcsVCardHelper.VCardItem;
import com.juphoon.helper.other.MD5Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RcsVCardCache {

    private final static int CACHE_SIZE = 10;

    private static Context sContext;

    private static class RcsVCardCacheItem {
        String mKey;
        VCardItem mItem;
    }

    public static void init(Context context) {
        sContext = context;
    }

    public static void showVCard(ImageView imageView, TextView textView, String path) {
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            VCardItem item = getItem(MD5Utils.md5(path));
            if (item != null) {
                showInfo(imageView, textView, item);
            } else {
                new LoadVcardTask(imageView, textView, path).execute();
            }
        }
    }

    private static class LoadVcardTask extends AsyncTask<Void, Void, VCardItem> {

        private final ImageView mImageView;
        private final TextView mTextView;
        private final String mPath;

        public LoadVcardTask(ImageView imageView, TextView textView, String path) {
            mImageView = imageView;
            mTextView = textView;
            mPath = path;
            mTextView.setTag(path);
        }

        @Override
        protected VCardItem doInBackground(Void... arg0) {
            return RcsVCardHelper.parseVCard(sContext, Uri.fromFile(new File(mPath)));
        }

        @Override
        protected void onPostExecute(VCardItem result) {
            super.onPostExecute(result);
            if (result != null) {
                addItem(MD5Utils.md5(mPath), result);
                if (TextUtils.equals(mPath, (String) mTextView.getTag())) {
                    showInfo(mImageView, mTextView, result);
                }
            }
        }

    }

    private final static List<RcsVCardCacheItem> sListVCard = new ArrayList<>();

    private static VCardItem getItem(String key) {
        for (RcsVCardCacheItem item : sListVCard) {
            if (TextUtils.equals(key, item.mKey)) {
                return item.mItem;
            }
        }
        return null;
    }

    private static void addItem(String key, VCardItem vcardItem) {
        RcsVCardCacheItem item = new RcsVCardCacheItem();
        item.mKey = key;
        item.mItem = vcardItem;
        sListVCard.add(item);
        if (sListVCard.size() > CACHE_SIZE) {
            sListVCard.remove(0);
        }
    }

    private static void showInfo(ImageView imageView, TextView textView, VCardItem item) {
        if (item.mAvatar != null) {
            imageView.setImageBitmap(item.mAvatar);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(sContext.getString(R.string.name)).append(":").append(item.mName).append("\n");
        int i = 1;
        for (String phone : item.mListPhone) {
            builder.append(sContext.getString(R.string.number)).append(i).append(":").append(phone).append("\n");
            i++;
        }
        textView.setText(builder.toString());
    }

}
