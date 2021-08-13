package com.juphoon.helper.mms.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.messaging.R;
import com.juphoon.helper.mms.RcsPublicMenu;
import com.juphoon.helper.mms.RcsPublicMenu.RcsPublicMenuItem;

public class RcsPublicMenuView extends LinearLayout {

    private final Context mContext;

    public RcsPublicMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void bind(RcsPublicMenu menu) {
        for (int i = getChildCount() - 1; i >= 2; i--) {
            removeViewAt(i);
        }

        for (int i = 0; i < menu.size(); i++) {
            RcsPublicMenuItem item = menu.getMenuItem(i);
            View menuView = inflate(mContext, R.layout.pubic_menu_item, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            menuView.setLayoutParams(params);
            Button button = (Button) menuView.findViewById(R.id.button_menu);
            button.setText(item.mTitle);
            button.setTag(item);
            button.setBackground(mContext.getDrawable(R.drawable.rcs_button_selectable));
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    doMenuItemClick(view);
                }
            });
            ImageView image = (ImageView) menuView.findViewById(R.id.imageview_more);
            if (menu.hasSubMenu(i)) {
                image.setVisibility(View.VISIBLE);
            } else {
                image.setVisibility(View.INVISIBLE);
            }
            addView(menuView);
        }
    }

    private void doMenuItemClick(View view) {
        final RcsPublicMenuItem item = (RcsPublicMenuItem) view.getTag();
        if (item.mSubMenuList.size() == 0) {
            doMenuAction(item);
        } else {
            PopupMenu popupMenu = new PopupMenu(mContext, view);
            Menu menu = popupMenu.getMenu();
            for (int i = 0; i < item.mSubMenuList.size(); i++) {
                menu.add(0, i, 0, item.mSubMenuList.get(i).mTitle);
            }
            popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int index = menuItem.getItemId();
                    doMenuAction(item.mSubMenuList.get(index));
                    return true;
                }
            });
            popupMenu.show();
        }
    }

    private void doMenuAction(RcsPublicMenuItem item) {
//        switch (item.mType) {
//            case RcsPaServiceConstants.EN_MTC_PA_MENU_CMD_LINK:
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setData(Uri.parse(item.mCommandId));
//                mContext.startActivity(intent);
//                break;
//            case RcsPaServiceConstants.EN_MTC_PA_MENU_CMD_SMLT_MSG:
//                if (RcsServiceManager.isLogined()) {
//                    RcsCallWrapper.rcsSendPublicPMsg(String.valueOf(0), item.mPaUuid, item.mCommandId);
//                }
//                break;
//            case RcsPaServiceConstants.EN_MTC_PA_MENU_CMD_APP_API:
//                break;
//            case RcsPaServiceConstants.EN_MTC_PA_MENU_CMD_DEV_API:
//                break;
//            default:
//                break;
//        }
    }
}
