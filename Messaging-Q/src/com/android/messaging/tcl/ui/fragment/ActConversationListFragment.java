package com.android.messaging.tcl.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.tcl.SimpleItemDecoration;
import com.android.messaging.tcl.ui.adapter.MessageAdapter;
import com.android.messaging.ui.conversationlist.ConversationListItemView;
import com.google.common.annotations.VisibleForTesting;
import com.juphoon.chatbotmaap.chatbotSearch.RcsChatbotServiceActivity;
import com.tcl.uicompat.TCLButton;


import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class ActConversationListFragment extends Fragment implements
        ConversationListData.ConversationListDataListener, MessageAdapter.HostInterface {

    private static final String BUNDLE_ARCHIVED_MODE = "archived_mode";
    @VisibleForTesting
    final Binding<ConversationListData> mListBinding = BindingBase.createBinding(this);
    private MessageAdapter mAdapter;
    private RecyclerView recyclerView_message;//短信的RecyclerView
    private Button add_mess;
    private boolean mArchiveMode;

    private ImageButton setting;

    private Button chengxu;
    private ImageView mask;

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        mListBinding.getData().init(getLoaderManager(), mListBinding);
        Log.i("ccc","=====onCreate.bind");
        try {
            mAdapter = new MessageAdapter(getActivity(), null,this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mArchiveMode = arguments.getBoolean(BUNDLE_ARCHIVED_MODE, false);
           // mForwardMessageMode = arguments.getBoolean(BUNDLE_FORWARD_MESSAGE_MODE, false);
        }
        Log.i("ddd"," mListBinding.bind----------:");
        mListBinding.bind(DataModel.get().createConversationListData(activity, this, mArchiveMode));
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.i("ddd"," 123456---------:");
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.act_conversitionlist_fragment,
                container, false);
        //初始化按钮
        add_mess=rootView.findViewById(R.id.add_message);
        //聚焦到添加信息
        add_mess.setFocusable(true);
        add_mess.requestFocus();
        add_mess.setFocusableInTouchMode(true);
        add_mess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHost.onCreateConversationClick();
            }
        });
        setting = rootView.findViewById(R.id.setting);
//        setting.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if(hasFocus){
//                    setting.setBackgroundResource(R.drawable.more_focus);
//                }else{
//                    setting.setBackgroundResource(R.drawable.more_normal);
//                }
//            }
//        });

        //小程序入口
        chengxu = rootView.findViewById(R.id.chengxu);
        chengxu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RcsChatbotServiceActivity.class);
                startActivity(intent);
            }
        });
        mask = rootView.findViewById(R.id.record_mask);
        //短信的列表
        recyclerView_message = rootView.findViewById(R.id.recycler_message);
        //设置RecyclerView短信布局管理器
        LinearLayoutManager linearLayoutManager_con = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView_message.setLayoutManager(linearLayoutManager_con);

        recyclerView_message.setAdapter(mAdapter);
        recyclerView_message.addItemDecoration(new SimpleItemDecoration());
     //   mHost.onCreateConversationClick();


        int firstItemPosition = linearLayoutManager_con.findFirstVisibleItemPosition();
        if (firstItemPosition > 0) {
            mask.setVisibility(View.VISIBLE);
        } else {
            mask.setVisibility(View.GONE);
        }

        //对message列表的进行监听
        mAdapter.setOnItemListener(new MessageAdapter.OnItemClickListener(){

            @Override
            public void setOnItemClick(int position, boolean isCheck) {

            }

            @Override
            public boolean setOnItemLongClick(int position) {
                return false;
            }

            @Override
            public void setOnItemCheckedChanged(int position, boolean isCheck) {

            }

            @Override
            public void setOnClick(List<ConversationListItemData> conversationListItemData , int position) {
                onConversationClicked(conversationListItemData.get(position));
            }

            @Override
            public void onItemRightClick(int keyCode, View view, int position) {
                //右键删除信息
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                }
            }
        });
        return rootView;

    }



    public interface ActConversationListFragmentHost {
        public void onConversationClick(final ConversationListData listData,
                                        final ConversationListItemData conversationListItemData);
        public void onCreateConversationClick();
        public boolean isConversationSelected(final String conversationId);
        public boolean isSwipeAnimatable();
        public boolean isSelectionMode();
        public boolean hasWindowFocus();
    }

    private ActConversationListFragmentHost mHost;

    public void onConversationClicked(final ConversationListItemData conversationListItemData) {
        Log.i("ddd", "mListBinding:getData-----："+mListBinding.getData().toString());
        final ConversationListData listData = mListBinding.getData();
        mHost.onConversationClick(listData, conversationListItemData);
    }

    private static final String BUNDLE_FORWARD_MESSAGE_MODE = "forward_message_mode";
    public static ActConversationListFragment createForwardMessageConversationListFragment() {
        return createConversationListFragment(BUNDLE_FORWARD_MESSAGE_MODE);
    }
    public static ActConversationListFragment createConversationListFragment(String modeKeyName) {
        final ActConversationListFragment fragment = new ActConversationListFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean(modeKeyName, true);
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public void onConversationListCursorUpdated(ConversationListData data, Cursor cursor) {
        mListBinding.ensureBound(data);
        final Cursor oldCursor = mAdapter.swapCursor(cursor);
        Log.i("ccc","=====oldCursor.bind");
//        updateEmptyListUi(cursor == null || cursor.getCount() == 0);
//        if (mListState != null && cursor != null && oldCursor == null) {
//            mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
//        }
    }

    @Override
    public void setBlockedParticipantsAvailable(boolean blockedAvailable) {

    }

    public void setScrolledToNewestConversationIfNeeded() {

            mListBinding.getData().setScrolledToNewestConversation(true);

    }

    /**
     * Call this immediately after attaching the fragment
     */
    public void setHost(final ActConversationListFragmentHost host) {
//        Assert.isNull(mHost);
        Log.i("ccc","host-----:"+host.toString());
        mHost = host;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListBinding.unbind();
        mHost = null;
    }
    @Override
    public boolean isConversationSelected(final String conversationId) {
        return mHost.isConversationSelected(conversationId);
    }

    @Override
    public void onConversationClicked(ConversationListItemData conversationListItemData, boolean isLongClick, ConversationListItemView conversationView) {

    }

    @Override
    public boolean isSwipeAnimatable() {
        return mHost.isSwipeAnimatable();
    }

    @Override
    public void startFullScreenPhotoViewer(Uri initialPhoto, Rect initialPhotoBounds, Uri photosUri) {

    }

    @Override
    public void startFullScreenVideoViewer(Uri videoUri) {

    }

    @Override
    public boolean isSelectionMode() {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

//        Assert.notNull(mHost);
        setScrolledToNewestConversationIfNeeded();

        updateUi();
    }

    public void updateUi() {
        mAdapter.notifyDataSetChanged();
    }

}
