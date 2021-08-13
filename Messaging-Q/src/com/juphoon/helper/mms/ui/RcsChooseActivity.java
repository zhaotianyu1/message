package com.juphoon.helper.mms.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.juphoon.helper.mms.ui.RcsChooseFragment.OnSelectedChangeListener;
import com.juphoon.rcs.tool.RcsNumberUtils;

import java.util.ArrayList;
import java.util.List;

public class RcsChooseActivity extends BugleActionBarActivity implements OnSelectedChangeListener {
    // 常量
    public static final String CHOOSE_MODE = "choose_mode";
    public static final int MODE_CONTACTS_ONLY = 0;
    public static final int MODE_CONTACTS_AND_GROUPS = MODE_CONTACTS_ONLY + 1;
    public static final int MODE_GROUP_ONLY = MODE_CONTACTS_AND_GROUPS + 1;
    public final static String TITLE = "title";
    public final static String LIMIT_MAX_SELECT = "limit_max_select";
    public final static String LIMIT_MIN_SELECT = "limit_min_select";
    public final static String EXCLUDE_PHONES = "exclude_phones";
    public final static String NOT_SHOW_PHONES = "not_show_phones";
    public final static String RESULT_PHONES = "result_phones";
    public final static String RESULT_GROUPCHAT_ID = "result_groupchat_id";
    public static final String SAVE_TITLE = "save_title";
    public static final String SAVE_MIN_SELECT = "save_min_select";
    public static final String SAVE_MAX_SELECT = "save_max_select";
    public static final String SAVE_SELECT_PHONES = "save_select_phones";
    public static final String SAVE_EXCLUDE_PHONES = "save_exclude_phones";
    public static final String SAVE_NOTSHOW_PHONES = "save_notshow_phones";

    // 变量
    protected int mChooseMode;
    protected int mLimitMinSelect;
    protected int mLimitMaxSelect;
    protected ArrayList<String> mListSelectPhones;
    protected ArrayList<String> mListExcludePhones;
    protected ArrayList<String> mListNotShowPhones;
    protected String mTitle;
    protected String mSelectGroupChatId;

    // Vew
    private RadioGroup mChooseRadioGroup;
    private ViewPager mChoosePager;
    private RadioButton mRadioBtnContacts;
    private RadioButton mRadioBtnGroups;
    private SearchView mSearchView;
    private RadioGroup mChooseTabLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_choose);
        mChooseMode = getIntent().getIntExtra(CHOOSE_MODE, MODE_CONTACTS_ONLY);
        // 去掉actionbar阴影
        getSupportActionBar().setElevation(0);
        // 返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init(savedInstanceState);
        initView();
    }

    private void init(Bundle bundle) {
        mListSelectPhones = new ArrayList<>();
        mListExcludePhones = new ArrayList<>();
        mListNotShowPhones = new ArrayList<>();
        if (bundle == null) {
            mTitle = getIntent().getStringExtra(TITLE);
            mLimitMinSelect = getIntent().getIntExtra(LIMIT_MIN_SELECT, 1);
            mLimitMaxSelect = getIntent().getIntExtra(LIMIT_MAX_SELECT, -1);
            String excludePhones = getIntent().getStringExtra(EXCLUDE_PHONES);
            if (!TextUtils.isEmpty(excludePhones)) {
                String[] phones = excludePhones.split(";");
                for (String phone : phones) {
                    mListExcludePhones.add(RcsNumberUtils.formatPhone86(phone));
                }
            }
            String notShowPhones = getIntent().getStringExtra(NOT_SHOW_PHONES);
            if (!TextUtils.isEmpty(notShowPhones)) {
                String[] phones = notShowPhones.split(";");
                for (String phone : phones) {
                    mListNotShowPhones.add(RcsNumberUtils.formatPhone86(phone));
                }
            }
        } else {
            mTitle = bundle.getString(SAVE_TITLE);
            mLimitMinSelect = bundle.getInt(SAVE_MIN_SELECT);
            mLimitMaxSelect = bundle.getInt(SAVE_MAX_SELECT);
            mListSelectPhones = bundle.getStringArrayList(SAVE_SELECT_PHONES);
            mListExcludePhones = bundle.getStringArrayList(SAVE_EXCLUDE_PHONES);
            mListNotShowPhones = bundle.getStringArrayList(SAVE_NOTSHOW_PHONES);
        }
        setTitle(String.format(getString(R.string.choose_contact_num), mTitle, String.valueOf(mListSelectPhones.size())));
    }

    private Bundle getContactsArguments() {
        Bundle bundle = new Bundle();
        bundle.putInt(SAVE_MIN_SELECT, mLimitMinSelect);
        bundle.putInt(SAVE_MAX_SELECT, mLimitMaxSelect);
        bundle.putStringArrayList(SAVE_EXCLUDE_PHONES, mListExcludePhones);
        bundle.putStringArrayList(SAVE_NOTSHOW_PHONES, mListNotShowPhones);
        bundle.putStringArrayList(SAVE_SELECT_PHONES, mListSelectPhones);
        return bundle;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rcs_choose, menu);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchMenu.getActionView();
        SearchView.SearchAutoComplete textView = (SearchView.SearchAutoComplete) mSearchView
                .findViewById(R.id.search_src_text);
        textView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            
            @Override
            public boolean onQueryTextSubmit(String arg0) {
                return false;
            }
            
            @Override
            public boolean onQueryTextChange(String string) {
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    ((RcsChooseFragment)fragment).onSerach(string);
                }
                return false;
            }
        });
        textView.setTextColor(Color.BLACK);
        textView.setHintTextColor(Color.GRAY);
        textView.setBackgroundColor(Color.WHITE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.id.action_search:
            return false;
        case R.id.action_sure:
            if (mChooseMode == MODE_CONTACTS_ONLY) {
                if (checkSelectedContacts()) {
                    doChooseContactsAction();
                }
            } else if (mChooseMode == MODE_GROUP_ONLY) {
                if (checkSelectedGroup()) {
                    doChooseGroupAction();
                }
            } else if (mChooseMode == MODE_CONTACTS_AND_GROUPS) {
                if (mRadioBtnContacts.isChecked()) {
                    if (checkSelectedContacts()) {
                        doChooseContactsAction();
                    }
                } else if (mRadioBtnGroups.isChecked()) {
                    if (checkSelectedGroup()) {
                        doChooseGroupAction();
                    }
                }
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean checkSelectedContacts() {
        if (mListSelectPhones == null || mListSelectPhones.size() == 0) {
            Toast.makeText(RcsChooseActivity.this,
                    String.format(getString(R.string.choose_contact_more_than), String.valueOf(0)),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mLimitMaxSelect != -1) {
            if (mListSelectPhones.size() > mLimitMaxSelect) {
                Toast.makeText(RcsChooseActivity.this,
                        String.format(getString(R.string.choose_contact_less_than), String.valueOf(mLimitMaxSelect)),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (mLimitMinSelect != -1) {
            if (mListSelectPhones.size() < mLimitMinSelect) {
                Toast.makeText(RcsChooseActivity.this,
                        String.format(getString(R.string.choose_contact_more_than), String.valueOf(mLimitMinSelect)),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private boolean checkSelectedGroup() {
        if (TextUtils.isEmpty(mSelectGroupChatId)) {
            Toast.makeText(RcsChooseActivity.this,
                    getString(R.string.please_select_a_group),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    protected void doChooseContactsAction() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(RESULT_PHONES, (ArrayList<String>) mListSelectPhones);
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void doChooseGroupAction() {
        Intent intent = new Intent();
        intent.putExtra(RESULT_GROUPCHAT_ID, mSelectGroupChatId);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initView() {
        mChoosePager = (ViewPager) findViewById(R.id.choose_view_pager);
        mChooseRadioGroup = (RadioGroup) findViewById(R.id.choose_radiogroup);
        mRadioBtnContacts = (RadioButton) mChooseRadioGroup.getChildAt(0);
        mRadioBtnGroups = (RadioButton) mChooseRadioGroup.getChildAt(1);
        mChooseTabLine = (RadioGroup) findViewById(R.id.choose_tab_line);

        List<RcsChooseFragment> fragmentLists = new ArrayList<RcsChooseFragment>();
        if (mChooseMode == MODE_CONTACTS_ONLY) {
            Bundle bundle = getContactsArguments();
            bundle.putInt(RcsChooseFragment.FRAGMENT_MODE, RcsChooseFragment.MODE_CONTACTS);
            RcsChooseFragment contactsFragment = RcsChooseFragment.newInstance(bundle);
            contactsFragment.setOnSelectedChangeListener(this);
            fragmentLists.add(contactsFragment);
            mChooseRadioGroup.setVisibility(View.GONE);
            mChooseTabLine.setVisibility(View.GONE);
        } else if (mChooseMode == MODE_CONTACTS_AND_GROUPS) {
            Bundle contactsBundle = getContactsArguments();
            contactsBundle.putInt(RcsChooseFragment.FRAGMENT_MODE, RcsChooseFragment.MODE_CONTACTS);
            RcsChooseFragment contactsFragment = RcsChooseFragment.newInstance(contactsBundle);
            contactsFragment.setArguments(getContactsArguments());
            contactsFragment.setOnSelectedChangeListener(this);
            fragmentLists.add(contactsFragment);
            Bundle groupBundle = new Bundle();
            groupBundle.putInt(RcsChooseFragment.FRAGMENT_MODE, RcsChooseFragment.MODE_GROUPS);
            RcsChooseFragment groupsFragment = RcsChooseFragment.newInstance(groupBundle);
            groupsFragment.setOnSelectedChangeListener(this);
            fragmentLists.add(groupsFragment);
        } else if (mChooseMode == MODE_GROUP_ONLY) {
            Bundle groupBundle = new Bundle();
            groupBundle.putInt(RcsChooseFragment.FRAGMENT_MODE, RcsChooseFragment.MODE_GROUPS);
            RcsChooseFragment groupsFragment = RcsChooseFragment.newInstance(groupBundle);
            groupsFragment.setOnSelectedChangeListener(this);
            fragmentLists.add(groupsFragment);
            mChooseRadioGroup.setVisibility(View.GONE);
            mChooseTabLine.setVisibility(View.GONE);
        }
        ChoosePagerAdapter pagerAdapter = new ChoosePagerAdapter(getSupportFragmentManager(), fragmentLists);
        mChoosePager.setAdapter(pagerAdapter);
        if (mChooseMode == MODE_CONTACTS_AND_GROUPS) {
            mChoosePager.addOnPageChangeListener(mChoosePagerChangeListener);
            mChooseRadioGroup.setOnCheckedChangeListener(mChooseCheckedChangeListener);
            ((RadioButton) mChooseRadioGroup.getChildAt(0)).setChecked(true);
            ((RadioButton) mChooseTabLine.getChildAt(0)).setChecked(true);
        }
    }

    RadioGroup.OnCheckedChangeListener mChooseCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            int indexOfChild = group.indexOfChild((RadioButton) findViewById(checkedId));
            mChoosePager.setCurrentItem(indexOfChild);
            if (indexOfChild == 0) {
                setTitle(String.format(getString(R.string.choose_contact_num), mTitle, String.valueOf(mListSelectPhones.size())));
            } else {
                setTitle(getString(R.string.choose_group));
            }
        }
    };

    ViewPager.OnPageChangeListener mChoosePagerChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            RadioButton btn = (RadioButton) mChooseRadioGroup.getChildAt(position);
            RadioButton line = (RadioButton) mChooseTabLine.getChildAt(position);
            btn.setChecked(true);
            line.setChecked(true);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    public class ChoosePagerAdapter extends FragmentStatePagerAdapter {
        private List<RcsChooseFragment> mFragments;

        public ChoosePagerAdapter(FragmentManager fm, List<RcsChooseFragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SAVE_TITLE, mTitle);
        outState.putInt(SAVE_MIN_SELECT, mLimitMinSelect);
        outState.putInt(SAVE_MAX_SELECT, mLimitMaxSelect);
        outState.putStringArrayList(SAVE_SELECT_PHONES, (ArrayList<String>) mListSelectPhones);
        outState.putStringArrayList(SAVE_EXCLUDE_PHONES, (ArrayList<String>) mListExcludePhones);
        outState.putStringArrayList(SAVE_NOTSHOW_PHONES, (ArrayList<String>) mListNotShowPhones);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onContactsSelectedChanged(List<String> selectedContacts) {
        mListSelectPhones = (ArrayList<String>) selectedContacts;
        setTitle(String.format(getString(R.string.choose_contact_num), mTitle, String.valueOf(mListSelectPhones.size())));
    }

    @Override
    public void onGroupSelectedChanged(String groupChatId) {
        mSelectGroupChatId = groupChatId;
        setTitle(getString(R.string.choose_group));
    }
}
