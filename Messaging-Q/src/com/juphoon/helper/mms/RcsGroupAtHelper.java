package com.juphoon.helper.mms;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RcsGroupAtHelper {

    public interface IRcsGroupAtHelperListener {
        public void onChooseAt();
    }

    private class AtItem {
        String mPhone;
        String mOrigContent;
        String mNowContent;
        int mStart;
        int mEnd;

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("phone=").append(mPhone).append(" ")
                    .append("mOrigContent=").append(mOrigContent).append(" ")
                    .append("mNowContent=").append(mNowContent).append(" ")
                    .append("mStart=").append(mStart).append(" ")
                    .append("mEnd=").append(mEnd);
            return builder.toString();
        }
    }

    private List<AtItem> mListAtItems = new ArrayList<>();
    private List<AtItem> mListInValidAtItems = new ArrayList<>();
    private String mBeforeText;
    private AtItem mRemoveItem;
    private int mStart, mBefore, mCount; // 记录变化
    private IRcsGroupAtHelperListener mIRcsGroupAtHelperListener;
    private static Map<String, String> sMapAtPhones = new HashMap<>(); // 保存全局的At队列，key表示某条消息的唯一标示

    public void setListener(IRcsGroupAtHelperListener listener) {
        mIRcsGroupAtHelperListener = listener;
    }

    // start为@位置 end为空格+1位置
    public void addItem(String phone, String origcontent, int start) {
        // 在某个@对象之内就不添加节点
        for (AtItem item : mListAtItems) {
            if (item.mStart <= start && item.mEnd >= start + origcontent.length()) {
                return;
            }
        }
        AtItem item = new AtItem();
        item.mPhone = phone;
        item.mOrigContent = origcontent;
        item.mStart = -1;
        item.mEnd = -1;
        mListInValidAtItems.add(item);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mBeforeText = s.toString();
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mStart = start;
        mBefore = before;
        mCount = count;
        // @通知上层可以选@对象
        if (before == 0 && count == 1) {
            if (s.charAt(start) == '@') {
                if (start != 0) {
                    // @前要非数字字母
                    if (!isNumberOrLetter(s.charAt(start - 1))) {
                        notifyChooseAt();
                    }
                } else {
                    notifyChooseAt();
                }
            }
        }
        // 删除空格
        if (before == 1 && count == 0) {
            if (mBeforeText.charAt(start) == ' ') {
                // 寻找是否是某个@对象的空格
                for (int i = 0; i < mListAtItems.size(); i++) {
                    AtItem item = mListAtItems.get(i);
                    if (item.mEnd == start + 1) {
                        mRemoveItem = item;
                        mListAtItems.remove(i);
                    }
                }
            }
        }
    }

    public void afterTextChanged(Editable s) {
        // 对空格删除整个@对象的处理
        if (mRemoveItem != null) {
            mStart = mRemoveItem.mStart;
            mBefore = mRemoveItem.mNowContent.length();
            mRemoveItem = null;
            s.delete(mStart, mStart + mBefore - 1); // 空格已被删除
        }
        updateAtList(s.toString(), mStart, mBefore, mCount);
    }

    // ; 隔开
    public String getAtListAndClear() {
        HashSet<String> setPhones = new HashSet<>();
        StringBuilder builder = new StringBuilder();
        for (AtItem item : mListAtItems) {
            if (TextUtils.equals(item.mNowContent, item.mOrigContent)) {
                setPhones.add(item.mPhone);
            }
        }
        mListAtItems.clear();
        mListInValidAtItems.clear();
        for (String phone : setPhones) {
            builder.append(phone).append(";");
        }
        return builder.toString();
    }

    public void saveAtListToGlobal(String key, String atList) {
        synchronized (sMapAtPhones) {
            sMapAtPhones.put(key, atList);
        }
    }

    public static String getAtListFromGlobalAndRemove(String key) {
        synchronized (sMapAtPhones) {
            if (sMapAtPhones.containsKey(key)) {
                String atList = sMapAtPhones.get(key);
                sMapAtPhones.remove(key);
                return atList;
            }
        }
        return null;
    }

    private void notifyChooseAt() {
        if (mIRcsGroupAtHelperListener != null) {
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    mIRcsGroupAtHelperListener.onChooseAt();
                }
            });
        }
    }

    private void updateAtList(String s, int start, int before, int count) {
        boolean needSearch = true;
        int searchStart = 0, searchEnd = s.length(); // 用于无效的At节点搜索匹配范围
        Iterator<AtItem> iter = mListAtItems.iterator();
        while (iter.hasNext()) {
            AtItem item = iter.next();
            if (item.mEnd <= start) { // 在节点外不影响
                // 搜索起点为改节点end后
                if (item.mEnd > searchStart) {
                    searchStart = item.mEnd;
                }
            } else if (item.mStart >= start + before) { // 在节点外只更新位置
                item.mStart = item.mStart - before + count;
                item.mEnd = item.mEnd - before + count;
                if (item.mStart < searchEnd) {
                    searchEnd = item.mStart;
                }
            } else if (item.mStart <= start && item.mEnd >= start + before) { // 在节点里更新end和内容
                item.mEnd = item.mEnd - before + count;
                item.mNowContent = s.substring(item.mStart, item.mEnd);
                if (item.mNowContent.isEmpty()) {
                    mListInValidAtItems.add(item);
                    iter.remove();
                }
                needSearch = false; // 在节点里则不需要搜索
            } else { // 跨节点
                mListInValidAtItems.add(item);
                iter.remove();
            }
        }

        // 黏贴后整个字符串寻找被置无效的@节点
        if (needSearch && mListInValidAtItems.size() > 0) {
            String regex = "@[^ ]* ";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(s.substring(searchStart, searchEnd));
            while (matcher.find()) {
                String matchText = matcher.group();
                int matchStart = matcher.start();
                int matchEnd = matcher.end();
                for (int i=0; i<mListInValidAtItems.size(); i++) {
                    AtItem item = mListInValidAtItems.get(i);
                    if (TextUtils.equals(matchText, item.mOrigContent)) {
                        item.mStart = searchStart + matchStart;
                        item.mEnd = searchStart + matchEnd;
                        item.mNowContent = item.mOrigContent;
                        mListAtItems.add(item);
                        mListInValidAtItems.remove(i);
                        break;
                    }
                }
            }
        }
    }

    private boolean isNumberOrLetter(char c) {
        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return true;
        }
        return false;
    }
}
