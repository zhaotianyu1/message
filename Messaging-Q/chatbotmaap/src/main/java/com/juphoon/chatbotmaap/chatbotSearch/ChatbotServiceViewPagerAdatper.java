package com.juphoon.chatbotmaap.chatbotSearch;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.juphoon.chatbotmaap.chatbotMessages.RcsChatBotConversationListFragment;

public class ChatbotServiceViewPagerAdatper extends FragmentPagerAdapter {


    public ChatbotServiceViewPagerAdatper(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 1) {
            return new RcsChatBotConversationListFragment();
        } else {
            return new RcsChatbotSearchFragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        View root = ((Fragment) object).getView();
        for (Object v = view; v instanceof View; v = ((View) v).getParent()) {
            if (v == root) {
                return true;
            }
        }
        return false;
    }
}
