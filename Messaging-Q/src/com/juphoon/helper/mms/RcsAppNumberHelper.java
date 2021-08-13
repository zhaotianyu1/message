package com.juphoon.helper.mms;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import com.juphoon.rcs.tool.RcsNumberUtils;
import com.juphoon.rcs.tool.RcsServiceManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by mofei on 2018/11/22.
 */

public class RcsAppNumberHelper {
    private static final String PROVIDER_NAME = "com.feinno.rongfly.provider";
    private static final String TABLE_NAME = "record";
    private static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/" + TABLE_NAME);
    private static final String MOBILE = "mobile";
    private static final String COUNT = "count";

    private static Context mContext;
    private static AppNumberChangeListener mListener;

    public interface AppNumberChangeListener {
        void notifyAppNumberChange(int unReadNum);
    }

    public static void init(Context context) {
        mContext = context;
        register();
    }

    //注册监听 
    public static void register() {
        try {
            if (mObserver != null) {
                mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, mObserver);
            }
        } catch (Exception e) {

        }
    }

    //取消注册监听
    public static void unRegister() {
        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    public static void addNumberChangeListener(AppNumberChangeListener listener) {
        mListener = listener;
    }

    public static void removeNumberChangeListener() {
        mListener = null;
    }

    private static void notifyAppNumberChange(int number) {
        if (mListener != null) {
            mListener.notifyAppNumberChange(number);
        }
    }

    /**
     * 根据手机号获取消息未读数
     *
     * @return 消息未读数
     */
    private static int queryUnreadCount() {
        String number = RcsServiceManager.getUserName();
        if (TextUtils.isEmpty(number)) {
            return 0;
        }
        number = RcsNumberUtils.formatPhoneNoCountryPrefix(number);
        int count = 0;
        try {
            ContentResolver contentResolver = mContext.getContentResolver();
            Cursor cursor = contentResolver.query(CONTENT_URI, new String[]{MOBILE, COUNT}, "mobile = ?", new String[]{number}, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(cursor.getColumnIndex(COUNT));
                cursor.close();
            }
        } catch (Exception e) {

        }
        return count;
    }

    public static void getUnreadCount() {
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... arg0) {
                return queryUnreadCount();
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                notifyAppNumberChange(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            getUnreadCount();
        }
    };

    public static void startAppNumber() {
        try {
            String number = RcsServiceManager.getUserName();
            if (!TextUtils.isEmpty(number)) {
                number = RcsNumberUtils.formatPhoneNoCountryPrefix(number);
//                SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext); juphoon
                final String simslot = String.valueOf(getDefaultDataPhoneId() + 1);
                ComponentName componentName = new ComponentName("com.nativeapp.rcsapp",
                        "com.nativeapp.rcsapp.MainActivity");
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("mobile", number);
                int simCount = RcsMmsUtils.getActiveSubCount(mContext);
                bundle.putString("simslot", simCount == 1 ? "1" : simslot);
                intent.putExtras(bundle);
                intent.setComponent(componentName);
                mContext.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //juphoon
    public static int getDefaultDataPhoneId() {
        try {
            Class<?> bookClass = Class.forName("android.telephony.SubscriptionManager");//完整类名
            int remainingMinutes = (int) ((double)Class
                    .forName("android.telephony.SubscriptionManager")
                    .getMethod("getDefaultDataPhoneId")
                    .invoke(bookClass));
            return remainingMinutes;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
