package com.juphoon.helper.mms.ui;

import com.android.messaging.R;

import java.util.ArrayList;
import java.util.List;

public class RcsSingleCreateCroupActivity extends RcsCreateCroupActivity {

    @Override
    public void onContactsSelectedChanged(List<String> selectedContacts) {
        mListSelectPhones = (ArrayList<String>) selectedContacts;
        // 一对一扩展至群聊会传入exclude号码，这些号码和选择的号码都算是群成员
        for (String excludePhone : mListExcludePhones) {
            if (!mListSelectPhones.contains(excludePhone)) {
                mListSelectPhones.add(excludePhone);
            }
        }
        setTitle(String.format(getString(R.string.choose_contact_num), mTitle, mListSelectPhones.size()));
        invalidateOptionsMenu();
    }
}
