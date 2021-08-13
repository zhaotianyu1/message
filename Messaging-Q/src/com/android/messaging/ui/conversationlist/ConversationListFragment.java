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
package com.android.messaging.ui.conversationlist;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;

import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.annotation.VisibleForAnimation;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListData.ConversationListDataListener;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.tcl.SimpleItemDecoration;
import com.android.messaging.ui.BugleAnimationTags;
import com.android.messaging.ui.ListEmptyView;
import com.android.messaging.ui.SnackBarInteraction;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.AccessibilityUtil;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ImeUtil;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.UiUtils;
import com.google.common.annotations.VisibleForTesting;
import com.juphoon.chatbotmaap.RcsLocalChatbotActivity;
import com.juphoon.chatbotmaap.chatbotSearch.RcsChatbotServiceActivity;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.tcl.uicompat.TCLButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of conversations.
 */
public class ConversationListFragment extends Fragment implements ConversationListDataListener,
        ConversationListItemView.HostInterface {
    private static final String BUNDLE_ARCHIVED_MODE = "archived_mode";
    private static final String BUNDLE_FORWARD_MESSAGE_MODE = "forward_message_mode";
    private static final boolean VERBOSE = false;

    private MenuItem mShowBlockedMenuItem;
    private boolean mArchiveMode;
    private boolean mBlockedAvailable;
    private boolean mForwardMessageMode;

    public interface ConversationListFragmentHost {
        public void onConversationClick(final ConversationListData listData,
                                        final ConversationListItemData conversationListItemData,
                                        final boolean isLongClick,
                                        final ConversationListItemView conversationView);
        public void onCreateConversationClick();
        public boolean isConversationSelected(final String conversationId);
        public boolean isSwipeAnimatable();
        public boolean isSelectionMode();
        public boolean hasWindowFocus();
    }

    private ConversationListFragmentHost mHost;
    private RecyclerView mRecyclerView;
    private TCLButton mStartNewConversationButton;
    private TCLButton start_new_chengxu_button;
    private ListEmptyView mEmptyListMessageView;
    private ConversationListAdapter mAdapter;

    // Saved Instance State Data - only for temporal data which is nice to maintain but not
    // critical for correctness.
    private static final String SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY =
            "conversationListViewState";
    private Parcelable mListState;

    @VisibleForTesting
    final Binding<ConversationListData> mListBinding = BindingBase.createBinding(this);

    public static ConversationListFragment createArchivedConversationListFragment() {
        return createConversationListFragment(BUNDLE_ARCHIVED_MODE);
    }

    public static ConversationListFragment createForwardMessageConversationListFragment() {
        return createConversationListFragment(BUNDLE_FORWARD_MESSAGE_MODE);
    }

    public static ConversationListFragment createConversationListFragment(String modeKeyName) {
        final ConversationListFragment fragment = new ConversationListFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean(modeKeyName, true);
        fragment.setArguments(bundle);
        return fragment;
    }
    // juphoon 监听融合通信登录状态
    private final RcsServiceManager.IServiceManagerCallback mRcsServiceCallBack = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(final boolean logined) {
            start_new_chengxu_button.setVisibility(logined ? View.VISIBLE : View.GONE);
        }
    };

    RcsMmsInitHelper.RcsInitCallback mRcsInitCallback = new RcsMmsInitHelper.RcsInitCallback() {

        @Override
        public void onInited(boolean inited) {
            // juphoon 添加是否使用融合通信的监听回调
            RcsServiceManager.addCallBack(mRcsServiceCallBack);
        }
    };
    private TextView signal_data;
    private TextView signal;
    TextView sign_id;
    private ImageView iv_sign;
    TelephonyManager Tel;
    private Button setting;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Tel = (TelephonyManager) BugleApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE);

            String operator = Tel.getSimOperator();

            if (!operator.isEmpty()) {
                //中国移动
                if (operator.equals("46000") || operator.equals("46002") ||operator.equals("46004")||operator.equals("46008")|| operator.equals("46007")||operator.equals("46020")){
                    sign_id.setText("中国移动");
                } else if (operator.equals("46001")|| operator.equals("46006")||operator.equals("46009")||operator.equals("46010")) {//中国联通{//中国联通
                    sign_id.setText("中国联通");
                } else if (operator.equals("46003") || operator.equals("46011")||operator.equals("46005")||operator.equals("46012")) {//中国电信
                    sign_id.setText("中国电信");
                }
            } else {
                sign_id.setText("");
            }
            //监听网络状态
            internet();
            // 每5秒执行一次
            handler.postDelayed(runnable, 4000);
        }
    };
    MyPhoneStateListener MyListener;
    /**
     * {@inheritDoc} from Fragment
     */
    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
     //   RcsMmsInitHelper.setRcsInitCallback(mRcsInitCallback);
        mListBinding.getData().init(getLoaderManager(), mListBinding);
        mAdapter = new ConversationListAdapter(getActivity(), null, this);
        //监听SIM卡手机号
        MyListener = new MyPhoneStateListener();
        handler.post(runnable); //发送消息，启动线程监听运行

    }

    @Override
    public void onResume() {
        super.onResume();

        Assert.notNull(mHost);
        setScrolledToNewestConversationIfNeeded();

        updateUi();
    }

    public void setScrolledToNewestConversationIfNeeded() {
        if (!mArchiveMode
                && !mForwardMessageMode
                && isScrolledToFirstConversation()
                && mHost.hasWindowFocus()) {
            mListBinding.getData().setScrolledToNewestConversation(true);
        }
    }

    private boolean isScrolledToFirstConversation() {
        int firstItemPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        return firstItemPosition == 0;
    }

    /**
     * {@inheritDoc} from Fragment
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mListBinding.unbind();
        mHost = null;
    }

    /**
     * {@inheritDoc} from Fragment
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.conversation_list_fragment,
                container, false);
        //初始化图标
        iv_sign = (ImageView) rootView.findViewById(R.id.Iv_sign);
        signal = (TextView) rootView.findViewById(R.id.signal);
        signal_data = rootView.findViewById(R.id.signal_data);
        sign_id = rootView.findViewById(R.id.sign_id);
        setting = rootView.findViewById(R.id.setting);
        mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mEmptyListMessageView = (ListEmptyView) rootView.findViewById(R.id.no_conversations_view);
        mEmptyListMessageView.setImageHint(R.drawable.ic_oobe_conv_list);
        // The default behavior for default layout param generation by LinearLayoutManager is to
        // provide width and height of WRAP_CONTENT, but this is not desirable for
        // ConversationListFragment; the view in each row should be a width of MATCH_PARENT so that
        // the entire row is tappable.
        final Activity activity = getActivity();
        final LinearLayoutManager manager = new LinearLayoutManager(activity) {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        };
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addItemDecoration(new SimpleItemDecoration());

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            int mCurrentState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

            @Override
            public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
                if (mCurrentState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                        || mCurrentState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    ImeUtil.get().hideImeKeyboard(getActivity(), mRecyclerView);
                }

                if (isScrolledToFirstConversation()) {
                    setScrolledToNewestConversationIfNeeded();
                } else {
                    mListBinding.getData().setScrolledToNewestConversation(false);
                }
            }

            @Override
            public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
                mCurrentState = newState;
            }
        });
        mRecyclerView.addOnItemTouchListener(new ConversationListSwipeHelper(mRecyclerView));

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY);
        }

        start_new_chengxu_button = (TCLButton) rootView.findViewById(R.id.start_new_chengxu_button);

        start_new_chengxu_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), RcsChatbotServiceActivity.class);
                startActivity(intent);
            }
        });

        mStartNewConversationButton = (TCLButton) rootView.findViewById(
                R.id.start_new_conversation_button);
        if (mArchiveMode) {
            mStartNewConversationButton.setVisibility(View.GONE);
        } else {
            mStartNewConversationButton.setVisibility(View.VISIBLE);
            mStartNewConversationButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View clickView) {
                    mHost.onCreateConversationClick();
                }
            });
        }
        ViewCompat.setTransitionName(mStartNewConversationButton, BugleAnimationTags.TAG_FABICON);


        if (mForwardMessageMode) {
            mStartNewConversationButton.setVisibility(View.GONE);
        }

        // The root view has a non-null background, which by default is deemed by the framework
        // to be a "transition group," where all child views are animated together during an
        // activity transition. However, we want each individual items in the recycler view to
        // show explode animation themselves, so we explicitly tag the root view to be a non-group.
        ViewGroupCompat.setTransitionGroup(rootView, false);

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (VERBOSE) {
            LogUtil.v(LogUtil.BUGLE_TAG, "Attaching List");
        }
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mArchiveMode = arguments.getBoolean(BUNDLE_ARCHIVED_MODE, false);
            mForwardMessageMode = arguments.getBoolean(BUNDLE_FORWARD_MESSAGE_MODE, false);
        }
        mListBinding.bind(DataModel.get().createConversationListData(activity, this, mArchiveMode));
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListState != null) {
            outState.putParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY, mListState);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mListState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        mListBinding.getData().setScrolledToNewestConversation(false);
    }

    /**
     * Call this immediately after attaching the fragment
     */
    public void setHost(final ConversationListFragmentHost host) {
        Assert.isNull(mHost);
        mHost = host;
    }

    @Override
    public void onConversationListCursorUpdated(final ConversationListData data,
            final Cursor cursor) {
        mListBinding.ensureBound(data);
        final Cursor oldCursor = mAdapter.swapCursor(cursor);
        updateEmptyListUi(cursor == null || cursor.getCount() == 0);
        if (mListState != null && cursor != null && oldCursor == null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
        }
    }

    @Override
    public void setBlockedParticipantsAvailable(final boolean blockedAvailable) {
        mBlockedAvailable = blockedAvailable;
        if (mShowBlockedMenuItem != null) {
            mShowBlockedMenuItem.setVisible(blockedAvailable);
        }
    }

    public void updateUi() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final MenuItem startNewConversationMenuItem =
                menu.findItem(R.id.action_start_new_conversation);
        if (startNewConversationMenuItem != null) {
            // It is recommended for the Floating Action button functionality to be duplicated as a
            // menu
            AccessibilityManager accessibilityManager = (AccessibilityManager)
                    getActivity().getSystemService(Context.ACCESSIBILITY_SERVICE);
            startNewConversationMenuItem.setVisible(accessibilityManager
                    .isTouchExplorationEnabled());
        }

        final MenuItem archive = menu.findItem(R.id.action_show_archived);
        if (archive != null) {
            archive.setVisible(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        if (!isAdded()) {
            // Guard against being called before we're added to the activity
            return;
        }

        mShowBlockedMenuItem = menu.findItem(R.id.action_show_blocked_contacts);
        if (mShowBlockedMenuItem != null) {
            mShowBlockedMenuItem.setVisible(mBlockedAvailable);
        }
    }

    /**
     * {@inheritDoc} from ConversationListItemView.HostInterface
     */
    @Override
    public void onConversationClicked(final ConversationListItemData conversationListItemData,
            final boolean isLongClick, final ConversationListItemView conversationView) {
        final ConversationListData listData = mListBinding.getData();
        mHost.onConversationClick(listData, conversationListItemData, isLongClick,
                conversationView);
    }

    /**
     * {@inheritDoc} from ConversationListItemView.HostInterface
     */
    @Override
    public boolean isConversationSelected(final String conversationId) {
        return mHost.isConversationSelected(conversationId);
    }

    @Override
    public boolean isSwipeAnimatable() {
        return mHost.isSwipeAnimatable();
    }

    // Show and hide empty list UI as needed with appropriate text based on view specifics
    private void updateEmptyListUi(final boolean isEmpty) {
        if (isEmpty) {
            int emptyListText;
            if (!mListBinding.getData().getHasFirstSyncCompleted()) {
                emptyListText = R.string.conversation_list_first_sync_text;
            } else if (mArchiveMode) {
                emptyListText = R.string.archived_conversation_list_empty_text;
            } else {
                emptyListText = R.string.conversation_list_empty_text;
            }
            mEmptyListMessageView.setTextHint(emptyListText);
            mEmptyListMessageView.setVisibility(View.VISIBLE);
            mEmptyListMessageView.setIsImageVisible(true);
            mEmptyListMessageView.setIsVerticallyCentered(true);
        } else {
            mEmptyListMessageView.setVisibility(View.GONE);
        }
    }

    @Override
    public List<SnackBarInteraction> getSnackBarInteractions() {
        final List<SnackBarInteraction> interactions = new ArrayList<SnackBarInteraction>(1);
        final SnackBarInteraction fabInteraction =
                new SnackBarInteraction.BasicSnackBarInteraction(mStartNewConversationButton);
        interactions.add(fabInteraction);
        return interactions;
    }

    private ViewPropertyAnimator getNormalizedFabAnimator() {
        return mStartNewConversationButton.animate()
                .setInterpolator(UiUtils.DEFAULT_INTERPOLATOR)
                .setDuration(getActivity().getResources().getInteger(
                        R.integer.fab_animation_duration_ms));
    }

    public ViewPropertyAnimator dismissFab() {
        // To prevent clicking while animating.
        mStartNewConversationButton.setEnabled(false);
        final MarginLayoutParams lp =
                (MarginLayoutParams) mStartNewConversationButton.getLayoutParams();
        final float fabWidthWithLeftRightMargin = mStartNewConversationButton.getWidth()
                + lp.leftMargin + lp.rightMargin;
        final int direction = AccessibilityUtil.isLayoutRtl(mStartNewConversationButton) ? -1 : 1;
        return getNormalizedFabAnimator().translationX(direction * fabWidthWithLeftRightMargin);
    }

    public ViewPropertyAnimator showFab() {
        return getNormalizedFabAnimator().translationX(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                // Re-enable clicks after the animation.
                mStartNewConversationButton.setEnabled(true);
            }
        });
    }

    public View getHeroElementForTransition() {
        return mArchiveMode ? null : mStartNewConversationButton;
    }

    @VisibleForAnimation
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public void startFullScreenPhotoViewer(
            final Uri initialPhoto, final Rect initialPhotoBounds, final Uri photosUri) {
        UIIntents.get().launchFullScreenPhotoViewer(
                getActivity(), initialPhoto, initialPhotoBounds, photosUri);
    }

    @Override
    public void startFullScreenVideoViewer(final Uri videoUri) {
        UIIntents.get().launchFullScreenVideoViewer(getActivity(), videoUri);
    }

    @Override
    public boolean isSelectionMode() {
        return mHost != null && mHost.isSelectionMode();
    }





    /**
     * 监听SIM卡的状态
     */
    class MyPhoneStateListener extends PhoneStateListener {


        private static final String TAG = "ZTY_MyPhoneStateListener";
        //判断当前网络是否有wifi
        int disConnect;

        /**
         * 监听当前SIM卡信号强度
         *
         * @param signalStrength
         */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);



            //获取基站的信号强度asu值
            if (signalStrength.getGsmSignalStrength() >= 30) {
                iv_sign.setBackgroundResource(R.drawable.ic_status_bar_signal4);
            } else if (signalStrength.getGsmSignalStrength() < 30 && signalStrength.getGsmSignalStrength() >= 20) {
                iv_sign.setBackgroundResource(R.drawable.ic_status_bar_signal3);
            } else if (signalStrength.getGsmSignalStrength() < 20 && signalStrength.getGsmSignalStrength() >= 10) {
                iv_sign.setBackgroundResource(R.drawable.ic_status_bar_signal2);
            } else if (signalStrength.getGsmSignalStrength() < 10) {
                iv_sign.setBackgroundResource(R.drawable.ic_status_bar_signal1);

            }


        }

        /**
         * 监听网络是否断开
         *
         * @param state
         */
        @Override
        public void onDataConnectionStateChanged(int state) {

            switch (state) {
                case TelephonyManager.DATA_DISCONNECTED://网络断开
                    signal.setBackgroundResource(0);
                    signal_data.setBackgroundResource(0);
                    disConnect = 0;
                    break;
                case TelephonyManager.DATA_CONNECTING://网络正在连接
                    break;
                case TelephonyManager.DATA_CONNECTED://网络连接上
                    disConnect = 2;
                    //判断当前网络是5G还是4G
                    TelephonyManager telephonyManager = (TelephonyManager) BugleApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE);

                    //获取网络类型
                    int networkType = telephonyManager.getNetworkType();
                    if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {//4G网络

                        signal.setBackgroundResource(R.drawable.ic_status_bar_4g);

                    } else {//5G网络

                        signal.setBackgroundResource(R.drawable.ic_status_bar_5g);
                    }
                    break;
            }

//                }

        }

        /**
         * 监听上下行网络
         *
         * @param direction
         */
        @Override
        public void onDataActivity(int direction) {

            //数据网络的状态
            switch (direction) {
                case TelephonyManager.DATA_ACTIVITY_NONE:
                    if (disConnect == 0) {
                        signal.setBackgroundResource(0);
                        signal_data.setBackgroundResource(0);
                    } else {
                        signal_data.setBackgroundResource(R.drawable.ic_status_bar_none);
                    }
                    break;
                case TelephonyManager.DATA_ACTIVITY_IN:
                    signal_data.setBackgroundResource(R.drawable.ic_status_bar_up);
                    break;
                case TelephonyManager.DATA_ACTIVITY_OUT:
                    signal_data.setBackgroundResource(R.drawable.ic_status_bar_down);
                    break;
                case TelephonyManager.DATA_ACTIVITY_INOUT:
                    signal_data.setBackgroundResource(R.drawable.ic_status_bar_all);
                    break;
                case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    break;
            }
        }
//        }
    }
    private void internet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) BugleApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        TelephonyManager tm = (TelephonyManager)  BugleApplication.getContext().getSystemService(Service.TELEPHONY_SERVICE);
        int state=tm.getSimState();
        if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
            boolean is = isPluggedIn();
            Log.i(TAG, "--------is----"+is);
            if(is){
                Log.i(TAG, "--------仅有线网络可用----");
                iv_sign.setBackgroundResource(R.drawable.ic_network);
                signal_data.setBackgroundResource(0);
                signal.setBackgroundResource(0);
            }else if(Tel.getSimState() == TelephonyManager.SIM_STATE_READY){
                Log.i(TAG, "--------无数据网络可用----");
                Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                        PhoneStateListener.LISTEN_DATA_ACTIVITY |
                        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            }else if(Tel.getSimState() != TelephonyManager.SIM_STATE_READY){
                Log.i(TAG, "-------无SIM卡---");
                iv_sign.setBackgroundResource(0);
                signal_data.setBackgroundResource(0);
                signal.setBackgroundResource(0);
            }
        } else if (wifiNetInfo.isConnected()) {
            Log.i(TAG, "-------仅Wifi网络可用---");
            signal_data.setBackgroundResource(0);
            signal.setBackgroundResource(0);
            checkWifiState();
        }else if(mobNetInfo.isConnected()) {

            Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                    PhoneStateListener.LISTEN_DATA_ACTIVITY |
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        }else {
            Log.i(TAG, "-------无SIM卡---");
            iv_sign.setBackgroundResource(0);
            signal_data.setBackgroundResource(0);
            signal.setBackgroundResource(0);
        }

    }

    /**
     * 检查无线信号强度
     */
    public void checkWifiState() {

        WifiManager mWifiManager = (WifiManager)BugleApplication.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int wifi = mWifiInfo.getRssi();//获取wifi信号强度
        if (wifi > -50 && wifi < 0) {//最强

            iv_sign.setBackgroundResource(R.drawable.ic_wifi_3);
        } else if (wifi > -70 && wifi < -50) {//较强

            iv_sign.setBackgroundResource(R.drawable.ic_wifi_2);
        } else if (wifi > -80 && wifi < -70) {//较弱

            iv_sign.setBackgroundResource(R.drawable.ic_wifi_1);
        } else if (wifi > -100 && wifi < -80) {//微弱
            iv_sign.setBackgroundResource(R.drawable.ic_wifi_0);

        }

    }
    /**
     * 网线的返回
     *
     * @return
     */
    public boolean isPluggedIn() {
        Context context = BugleApplication.getContext();

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
        for (int i = 0; i < networkInfo.length; i++) {
            if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                Log.i(TAG, "有网络-------------");
                return true;
            }
        }
        return false;
    }
    private static final String TAG = "ZTY_MainActivity";
}
