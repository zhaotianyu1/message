package com.juphoon.helper.mms;

import java.util.ArrayList;
import java.util.List;

public class RcsPublicMenu {

    public static class RcsPublicMenuItem {
        public long mId;
        public String mCommandId;
        public String mPaUuid;
        public long mParentMenuId;
        public int mPriority;
        public String mTitle;
        public int mType;
        public List<RcsPublicMenuItem> mSubMenuList;
    }

    private final List<RcsPublicMenuItem> mMenuList = new ArrayList<>();

    public int size() {
        return mMenuList.size();
    }

    public boolean hasSubMenu(int index) {
        return mMenuList.get(index).mSubMenuList.size() > 0;
    }

    public RcsPublicMenuItem getMenuItem(int index) {
        if (index < 0 || index >= mMenuList.size()) {
            return null;
        }
        return mMenuList.get(index);
    }

    public List<RcsPublicMenuItem> getSubMenuItem(int index) {
        RcsPublicMenuItem item = getMenuItem(index);
        if (item != null) {
            return item.mSubMenuList;
        }
        return null;
    }

}
