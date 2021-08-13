/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.mms.CarrierConfigValuesLoader;
import androidx.appcompat.mms.MmsManager;
import androidx.multidex.MultiDex;

import android.telephony.CarrierConfigManager;
import android.util.Log;

import com.android.messaging.datamodel.DataModel;
import com.android.messaging.receiver.SmsReceiver;
import com.android.messaging.sms.ApnDatabase;
import com.android.messaging.sms.BugleApnSettingsLoader;
import com.android.messaging.sms.BugleUserAgentInfoLoader;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.ui.ConversationDrawables;
import com.android.messaging.util.BugleGservices;
import com.android.messaging.util.BugleGservicesKeys;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.BuglePrefsKeys;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.Trace;
import com.google.common.annotations.VisibleForTesting;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.tcl.ff.component.animer.glow.view.utils.AnimerConfig;
import com.tcl.uicompat.util.TCLThemeUtils;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The application object
 */
public class BugleApplication extends Application implements UncaughtExceptionHandler {
    private static final String TAG = LogUtil.BUGLE_TAG;

    private UncaughtExceptionHandler sSystemUncaughtExceptionHandler;
    private static boolean sRunningTests = false;
    private String conversionid ;//当前对话人的id
    private String name;
    private boolean isMulit = false;
    private boolean isChose = false;
    private static Context mContext;

    private boolean isfinish;
    private int galleryCount = 0;

    private int touchCount = 1;

    private Cursor cursor;
    private Cursor cursorList;

    private long threadId;

    private Integer position;

    private Integer isFirst_count=0;

    private boolean ismap = false;
        private  int  isblank = 0;

        private boolean isPicture = false;
    private boolean isGong = false;
    @VisibleForTesting
    protected static void setTestsRunning() {
        sRunningTests = true;
    }
    Api api = new Api();
    /**
     * @return true if we're running unit tests.
     */
    public static boolean isRunningTests() {
        return sRunningTests;
    }
    @SuppressLint("StaticFieldLeak")
    private static BugleApplication myApplication;
    @Override
    public void onCreate() {
        Trace.beginSection("app.onCreate");
        super.onCreate();
        hookWebView();
        myApplication = this;
        Log.i("ooo","启动application");
        mContext = this;
        api.setContext(mContext);
        api.setBugleApplication(myApplication);
        AnimerConfig.init(this);
        AnimerConfig.setGlowAnimerSwitchValue(true);
        // Note onCreate is called in both test and real application environments
        if (!sRunningTests) {
            // Only create the factory if not running tests
            FactoryImpl.register(getApplicationContext(), this);
        } else {
            LogUtil.e(TAG, "BugleApplication.onCreate: FactoryImpl.register skipped for test run");
        }

        sSystemUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        Trace.endSection();

        TCLThemeUtils.setCustomTheme(this, R.style.UI_5_AppTheme);
        TCLThemeUtils.initRegisterActivity(this);
        /**
         * juphoon 初始话菊风 rcs 相关模块
         */
        RcsMmsInitHelper.init(this);
    }
    public static BugleApplication getInstance(){
        return myApplication;
    }

    public static Context getContext(){

        return mContext;
    }

    public boolean isPicture() {
        return isPicture;
    }

    public void setPicture(boolean picture) {
        isPicture = picture;
    }

    public int getIsblank() {
        return isblank;
    }

    public void setIsblank(int isblank) {
        this.isblank = isblank;
    }

    public boolean isIsmap() {
        return ismap;
    }

    public void setIsmap(boolean ismap) {
        this.ismap = ismap;
    }

    public boolean isIsfinish() {
        return isfinish;
    }

    public void setIsfinish(boolean isfinish) {
        this.isfinish = isfinish;
    }


    public Integer getIsFirst_count() {
        return isFirst_count;
    }

    public void setIsFirst_count(Integer isFirst_count) {
        this.isFirst_count = isFirst_count;
    }

    public int getGalleryCount() {
        return galleryCount;
    }

    public void setGalleryCount(int galleryCount) {
        this.galleryCount = galleryCount;
    }

    public int getTouchCount() {
        return touchCount;
    }

    public void setTouchCount(int touchCount) {
        this.touchCount = touchCount;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public Cursor getCursorList() {
        return cursorList;
    }

    public void setCursorList(Cursor cursorList) {
        this.cursorList = cursorList;
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public boolean isGong() {
        return isGong;
    }

    public void setGong(boolean gong) {
        isGong = gong;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getConversionid() {
        return conversionid;
    }

    public void setConversionid(String conversionid) {
        this.conversionid = conversionid;
    }

    public boolean isChose() {
        return isChose;
    }

    public void setChose(boolean chose) {
        isChose = chose;
    }

    public boolean isMulit() {
        return isMulit;
    }

    public void setMulit(boolean mulit) {
        isMulit = mulit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update conversation drawables when changing writing systems
        // (Right-To-Left / Left-To-Right)
        ConversationDrawables.get().updateDrawables();
    }

    public static void hookWebView(){
        int sdkInt = Build.VERSION.SDK_INT;
        try {
            Class<?> factoryClass = Class.forName("android.webkit.WebViewFactory");
            Field field = factoryClass.getDeclaredField("sProviderInstance");
            field.setAccessible(true);
            Object sProviderInstance = field.get(null);
            if (sProviderInstance != null) {
                Log.i("bug","sProviderInstance isn't null");
                return;
            }

            Method getProviderClassMethod;
            if (sdkInt > 22) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getProviderClass");
            } else if (sdkInt == 22) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getFactoryClass");
            } else {
                Log.i("bug","Don't need to Hook WebView");
                return;
            }
            getProviderClassMethod.setAccessible(true);
            Class<?> factoryProviderClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
            Class<?> delegateClass = Class.forName("android.webkit.WebViewDelegate");
            Constructor<?> delegateConstructor = delegateClass.getDeclaredConstructor();
            delegateConstructor.setAccessible(true);
            if(sdkInt < 26){//低于Android O版本
                Constructor<?> providerConstructor = factoryProviderClass.getConstructor(delegateClass);
                if (providerConstructor != null) {
                    providerConstructor.setAccessible(true);
                    sProviderInstance = providerConstructor.newInstance(delegateConstructor.newInstance());
                }
            } else {
                Field chromiumMethodName = factoryClass.getDeclaredField("CHROMIUM_WEBVIEW_FACTORY_METHOD");
                chromiumMethodName.setAccessible(true);
                String chromiumMethodNameStr = (String)chromiumMethodName.get(null);
                if (chromiumMethodNameStr == null) {
                    chromiumMethodNameStr = "create";
                }
                Method staticFactory = factoryProviderClass.getMethod(chromiumMethodNameStr, delegateClass);
                if (staticFactory!=null){
                    sProviderInstance = staticFactory.invoke(null, delegateConstructor.newInstance());
                }
            }

            if (sProviderInstance != null){
                field.set("sProviderInstance", sProviderInstance);
                Log.i("bug","Hook success!");
            } else {
                Log.i("bug","Hook failed!");
            }
        } catch (Throwable e) {
            Log.w("bug",e);
        }
    }
    // Called by the "real" factory from FactoryImpl.register() (i.e. not run in tests)
    public void initializeSync(final Factory factory) {
        Trace.beginSection("app.initializeSync");
        final Context context = factory.getApplicationContext();
        final BugleGservices bugleGservices = factory.getBugleGservices();
        final BuglePrefs buglePrefs = factory.getApplicationPrefs();
        final DataModel dataModel = factory.getDataModel();
        final CarrierConfigValuesLoader carrierConfigValuesLoader =
                factory.getCarrierConfigValuesLoader();

        maybeStartProfiling();

        BugleApplication.updateAppConfig(context);

        // Initialize MMS lib
        initMmsLib(context, bugleGservices, carrierConfigValuesLoader);
        // Initialize APN database
        ApnDatabase.initializeAppContext(context);
        // Fixup messages in flight if we crashed and send any pending
        dataModel.onApplicationCreated();
        // Register carrier config change receiver
        if (OsUtil.isAtLeastM()) {
            registerCarrierConfigChangeReceiver(context);
        }

        Trace.endSection();
    }

    private static void registerCarrierConfigChangeReceiver(final Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtil.i(TAG, "Carrier config changed. Reloading MMS config.");
                MmsConfig.loadAsync();
            }
        }, new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
    }

    private static void initMmsLib(final Context context, final BugleGservices bugleGservices,
            final CarrierConfigValuesLoader carrierConfigValuesLoader) {
        MmsManager.setApnSettingsLoader(new BugleApnSettingsLoader(context));
        MmsManager.setCarrierConfigValuesLoader(carrierConfigValuesLoader);
        MmsManager.setUserAgentInfoLoader(new BugleUserAgentInfoLoader(context));
        MmsManager.setUseWakeLock(true);
        // If Gservices is configured not to use mms api, force MmsManager to always use
        // legacy mms sending logic
        MmsManager.setForceLegacyMms(!bugleGservices.getBoolean(
                BugleGservicesKeys.USE_MMS_API_IF_PRESENT,
                BugleGservicesKeys.USE_MMS_API_IF_PRESENT_DEFAULT));
        bugleGservices.registerForChanges(new Runnable() {
            @Override
            public void run() {
                MmsManager.setForceLegacyMms(!bugleGservices.getBoolean(
                        BugleGservicesKeys.USE_MMS_API_IF_PRESENT,
                        BugleGservicesKeys.USE_MMS_API_IF_PRESENT_DEFAULT));
            }
        });
    }

    public static void updateAppConfig(final Context context) {
        // Make sure we set the correct state for the SMS/MMS receivers
        SmsReceiver.updateSmsReceiveHandler(context);
    }

    // Called from thread started in FactoryImpl.register() (i.e. not run in tests)
    public void initializeAsync(final Factory factory) {
        // Handle shared prefs upgrade & Load MMS Configuration
        Trace.beginSection("app.initializeAsync");
        maybeHandleSharedPrefsUpgrade(factory);
        MmsConfig.load();
        Trace.endSection();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
            LogUtil.d(TAG, "BugleApplication.onLowMemory");
        }
        Factory.get().reclaimMemory();
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        final boolean background = getMainLooper().getThread() != thread;
        if (background) {
            LogUtil.e(TAG, "Uncaught exception in background thread " + thread, ex);

            final Handler handler = new Handler(getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    sSystemUncaughtExceptionHandler.uncaughtException(thread, ex);
                }
            });
        } else {
            sSystemUncaughtExceptionHandler.uncaughtException(thread, ex);
        }
    }

    private void maybeStartProfiling() {
        // App startup profiling support. To use it:
        //  adb shell setprop log.tag.BugleProfile DEBUG
        //  #   Start the app, wait for a 30s, download trace file:
        //  adb pull /data/data/com.android.messaging/cache/startup.trace /tmp
        //  # Open trace file (using adt/tools/traceview)
        if (android.util.Log.isLoggable(LogUtil.PROFILE_TAG, android.util.Log.DEBUG)) {
            // Start method tracing with a big enough buffer and let it run for 30s.
            // Note we use a logging tag as we don't want to wait for gservices to start up.
            final File file = DebugUtils.getDebugFile("startup.trace", true);
            if (file != null) {
                android.os.Debug.startMethodTracing(file.getAbsolutePath(), 160 * 1024 * 1024);
                new Handler(Looper.getMainLooper()).postDelayed(
                       new Runnable() {
                            @Override
                            public void run() {
                                android.os.Debug.stopMethodTracing();
                                // Allow world to see trace file
                                DebugUtils.ensureReadable(file);
                                LogUtil.d(LogUtil.PROFILE_TAG, "Tracing complete - "
                                     + file.getAbsolutePath());
                            }
                        }, 30000);
            }
        }
    }

    private void maybeHandleSharedPrefsUpgrade(final Factory factory) {
        final int existingVersion = factory.getApplicationPrefs().getInt(
                BuglePrefsKeys.SHARED_PREFERENCES_VERSION,
                BuglePrefsKeys.SHARED_PREFERENCES_VERSION_DEFAULT);
        final int targetVersion = Integer.parseInt(getString(R.string.pref_version));
        if (targetVersion > existingVersion) {
            LogUtil.i(LogUtil.BUGLE_TAG, "Upgrading shared prefs from " + existingVersion +
                    " to " + targetVersion);
            try {
                // Perform upgrade on application-wide prefs.
                factory.getApplicationPrefs().onUpgrade(existingVersion, targetVersion);
                // Perform upgrade on each subscription's prefs.
                PhoneUtils.forEachActiveSubscription(new PhoneUtils.SubscriptionRunnable() {
                    @Override
                    public void runForSubscription(final int subId) {
                        factory.getSubscriptionPrefs(subId)
                                .onUpgrade(existingVersion, targetVersion);
                    }
                });
                factory.getApplicationPrefs().putInt(BuglePrefsKeys.SHARED_PREFERENCES_VERSION,
                        targetVersion);
            } catch (final Exception ex) {
                // Upgrade failed. Don't crash the app because we can always fall back to the
                // default settings.
                LogUtil.e(LogUtil.BUGLE_TAG, "Failed to upgrade shared prefs", ex);
            }
        } else if (targetVersion < existingVersion) {
            // We don't care about downgrade since real user shouldn't encounter this, so log it
            // and ignore any prefs migration.
            LogUtil.e(LogUtil.BUGLE_TAG, "Shared prefs downgrade requested and ignored. " +
                    "oldVersion = " + existingVersion + ", newVersion = " + targetVersion);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        /**
         * juphoon api 超过 65536 处理
         */
        MultiDex.install(base);
    }
}
