package com.juphoon.chatbotmaap.bottomMenu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import androidx.appcompat.widget.ListPopupWindow;

import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotInfoBean;
import com.juphoon.chatbot.RcsChatbotReplyBean;
import com.juphoon.chatbot.RcsChatbotSuggestionsBean;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotCardView;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.chatbotmaap.tcl.TCLButtons;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.service.RcsImServiceConstants;
import com.tcl.uicompat.TCLButton;

import java.util.ArrayList;
import java.util.List;

public class RcsChatbotMenuView extends LinearLayout {

    private final Context mContext;
    private String mConversationId;
    private String mChatbotServiceId;
    private View line_view;

    private int [] photos = {R.drawable.button_pic,R.drawable.button_money,R.drawable.button_lifr};
//    private int [] focus_photo = {R.drawable.life_focus,R.drawable.money_focus,R.drawable.service_focus};
    public RcsChatbotMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void bind(RcsChatbotInfoBean.PersistentmenuBean menuBean, String conversation, String chatbotServiceId) {
        for (int i = getChildCount() - 1; i >= 1; i--) {
            removeViewAt(i);
        }

        if (getTapMenu(menuBean) == null) {
            return;
        }
        mConversationId = conversation;
        mChatbotServiceId = chatbotServiceId;

        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setPadding(60,0,0,0);

//        linearLayout.setClipChildren(false);
//        linearLayout.setClipToPadding(false);
        for (int i = 0; i < getTapMenu(menuBean).size(); i++) {
            RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries item = getTapMenu(menuBean).get(i);
            ViewGroup menuView = (ViewGroup) inflate(mContext, R.layout.chatbot_bottom_menu_item, null);
          //  LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0);
            params.setMargins(0,10,0,0);

//            menuView.setPadding(10,0,0,0);
            menuView.setLayoutParams(params);
            final ImageView line_view = menuView.findViewById(R.id.line_view);
            final Drawable drawable = getResources().getDrawable(photos[i]);
            drawable.setBounds(0,0,44,46);

//            if (item.menu == null) {
//                ImageView menuPic = menuView.findViewById(R.id.menu_pic);
//                menuPic.setVisibility(GONE);
//            }
            final TCLButtons button = (TCLButtons) menuView.findViewById(R.id.button_menu);
            button.setCompoundDrawables(drawable,null,null,null);
            button.setText(getDisplayText(item));
            button.setTag(item);
            //button.setBackground(mContext.getDrawable(R.drawable.rcs_button_selectable));
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    doMenuItemClick(view);
                }
            });
            if(i ==0){
                line_view.setVisibility(GONE);
            }else{
                button.setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if(hasFocus){
                            line_view.setVisibility(GONE);
                        }else{
                            line_view.setVisibility(VISIBLE);
                        }
                    }
                });
            }
//            button.setOnFocusChangeListener(new OnFocusChangeListener() {
//                @Override
//                public void onFocusChange(View v, boolean hasFocus) {
//                    if(hasFocus){
//                        button.setCompoundDrawables(drawable_focus,null,null,null);
//                    }else{
//                        button.setCompoundDrawables(drawable,null,null,null);
//                    }
//                }
//            });
            linearLayout.addView(menuView);
        }
        addView(linearLayout);
    }
    private ListViewDialog dialog;


    private void doMenuItemClick(final View view) {
        final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries item = (RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries) view.getTag();
        if (item.menu == null) {
            doMenuAction(item);
        } else {
            dialog = new ListViewDialog(getContext(),item.menu.entries,mConversationId,mChatbotServiceId);

            dialog.show();
            final ListPopupWindow mListPop;

//            final MenuItemAdapter menuItemAdapter = new MenuItemAdapter(item.menu.entries);
        //    mListPop = new ListPopupWindow(mContext);
//            mListPop.setAdapter(menuItemAdapter);
      //         mListPop.setWidth(350);
//            mListPop.setOnItemSelectedListener();
//            mListPop.setHeight(LayoutParams.WRAP_CONTENT);
//            mListPop.setAnchorView(view);//设置ListPopupWindow的锚点，即关联PopupWindow的显示位置和这个锚点
//            mListPop.setDropDownGravity(Gravity.TOP);
//            mListPop.setModal(true);//设置是否是模式
//            mListPop.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    menuItemAdapter.onClickMenuItem(position);
//                    mListPop.dismiss();
//                }
//            });
//            mListPop.show();
        }
    }

    private void doSubMenuItemClick(final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries entry, View view) {
        PopupMenu popupMenu = new PopupMenu(mContext, view);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < entry.menu.entries.size(); i++) {
            menu.add(0, i, 0, getDisplayText(entry.menu.entries.get(i)));
        }

        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int index = menuItem.getItemId();
                RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries subExtra = entry.menu.entries.get(index);
                doMenuAction(subExtra);
                return true;
            }
        });
        popupMenu.show();
    }

    private void doMenuAction(RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries item) {
        if (item.action != null) {
            RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
            replyBean.response = new RcsChatbotReplyBean.ResponseBean();
            replyBean.response.action = new RcsChatbotSuggestionsBean.SuggestionsBean.ActionBean();
            replyBean.response.action.displayText = item.action.displayText;
            replyBean.response.action.postback = item.action.postback;
            RcsCallWrapper.rcsSendMessage1To1("", mChatbotServiceId, new Gson().toJson(replyBean), RcsImServiceConstants.RCS_MESSAGE_TYPE_CHATBOT_SUGGESTION, RcsImServiceConstants.RCS_MESSAGE_SERVICE_TYPE_CHATBOT, null, null, null);
            RcsChatbotCardView.dealSugesstion(item.action, mContext, mConversationId, mChatbotServiceId, null, null, null);
        } else if (item.reply != null) {
            RcsChatbotReplyBean replyBean = new RcsChatbotReplyBean();
            replyBean.response = new RcsChatbotReplyBean.ResponseBean();
            replyBean.response.reply = new RcsChatbotSuggestionsBean.SuggestionsBean.ReplyBean();
            replyBean.response.reply.displayText = item.reply.displayText;
            replyBean.response.reply.postback = item.reply.postback;
            Intent intent = new Intent();
            intent.putExtra(RcsChatbotDefine.KEY_REPLYJSON, new Gson().toJson(replyBean));
            intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, mConversationId);
            intent.setAction(RcsChatbotDefine.ACTION_MENU_REPLY);
            intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
            getContext().sendBroadcast(intent);
        }
    }

    public static RcsChatbotInfoBean.PersistentmenuBean pauseFromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        return new Gson().fromJson(json, RcsChatbotInfoBean.PersistentmenuBean.class);
    }

    public List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> getTapMenu(RcsChatbotInfoBean.PersistentmenuBean persistentmenuBean) {
        if (persistentmenuBean != null) {
            if (persistentmenuBean.menu == null) {
                return persistentmenuBean.entries;
            }
            return persistentmenuBean.menu.entries;
        }
        return null;
    }

    public static String getDisplayText(RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries entrie) {
        if (entrie.action != null) {
            return entrie.action.displayText;
        } else if (entrie.menu != null) {
            return entrie.menu.displayText;
        } else if (entrie.reply != null) {
            return entrie.reply.displayText;
        }
        return "";
    }

    class MenuItemAdapter extends BaseAdapter {
        List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> mMenuEntriesList;

        public MenuItemAdapter(List<RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries> entries) {
            mMenuEntriesList = entries;
        }

        @Override
        public int getCount() {
            return mMenuEntriesList.size();
        }

        @Override
        public Object getItem(int position) {

            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries currentItemEntries = mMenuEntriesList.get(position);
            String text = getDisplayText(currentItemEntries);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View view = inflater.inflate(R.layout.chatbot_menu_item_layout, parent, false);
           // view.setBackgroundResource(R.color.biaoti);
            TextView textView = view.findViewById(R.id.menu_text);
            textView.setText(text);
            return view;
        }

        public void onClickMenuItem(int position) {
            final RcsChatbotInfoBean.PersistentmenuBean.RcsChatbotMenuEntries currentItemEntries = mMenuEntriesList.get(position);
            if (currentItemEntries.menu == null) {
                doMenuAction(currentItemEntries);
            } else {
                //不支持二级菜单
                //  doSubMenuItemClick(currentItemEntries, view);
            }
        }
    }

    //确定子布局宽度
    private int measureContentWidth(BaseAdapter listAdapter) {
        ViewGroup mMeasureParent = null;
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final BaseAdapter adapter = listAdapter;
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new LinearLayout(mContext);
            }

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }
}
