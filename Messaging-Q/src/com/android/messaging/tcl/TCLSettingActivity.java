package com.android.messaging.tcl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.android.messaging.R;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.util.BuglePrefs;
import com.google.common.annotations.VisibleForTesting;
import com.juphoon.helper.mms.RcsMmsInitHelper;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.rcs.JApplication;
import com.juphoon.ui.RcsConfigHelper;
import com.juphoon.ui.RcsRegisterActivity;
import com.tcl.ff.component.animer.glow.view.AllCellsGlowLayout;
import com.tcl.uicompat.TCLButton;
import com.tcl.uicompat.TCLItemLarge;
import com.tcl.uicompat.TCLListSwitch;

import java.util.Arrays;
import java.util.List;

public class TCLSettingActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {


    private AllCellsGlowLayout rcs;
  //  private AllCellsGlowLayout voices;

    private String mRcsloginEnablePreferenceKey;

    private TextView number;
    private TextView login_status;
    private LoaderManager loaderManager;
    private TCLButton buttadd2;
    @VisibleForTesting
    final Binding<ConversationListData> mListBinding = BindingBase.createBinding(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_setting);
        initdata();
        receiveRepeatLogin(getIntent());
        RcsServiceManager.addCallBack(mRcsServiceCallBack);

        loaderManager=getSupportLoaderManager();
        loaderManager.initLoader(1, null, this);

    }



    private void initdata() {
        buttadd2 = findViewById(R.id.buttadd2);
        buttadd2.setText("未登录");
        buttadd2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRcsLoginPreferencens();
            }
        });
       // login_status = findViewById(R.id.login_status);
        number = findViewById(R.id.number);
      //  rcs = findViewById(R.id.rcs);
//        rcs.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                updateRcsLoginPreferencens();
//            }
//        });
        mRcsloginEnablePreferenceKey = getString(R.string.rcs_login_pref_key);
        if (RcsCallWrapper.rcsIsLogined() && !TextUtils.isEmpty(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext))) {
           // mRcsloginEnablePreference.setSummary(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext));
            //login_status.setText("已登录");
            buttadd2.setText("已登录");
            number.setText(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext));
        }

//        voices = findViewById(R.id.voices);
//        voices.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });

    }

    private final RcsServiceManager.IServiceManagerCallback mRcsServiceCallBack = new RcsServiceManager.IServiceManagerCallback() {
        @Override
        public void onLoginStateChange(final boolean logined) {
            if (logined && !TextUtils.isEmpty(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext))) {
               // mRcsloginEnablePreference.setSummary(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext));
              //  login_status.setText("已登录");
                buttadd2.setText("已登录");
                number.setText(RcsConfigHelper.LastAccount.getAccount(JApplication.sContext));
            } else {
               // login_status.setText("未登录");
                buttadd2.setText("未登录");
                number.setText("");
               // mRcsloginEnablePreference.setSummary(getString(R.string.chatbot_not_login));
            }
        }
    };

    private void updateRcsLoginPreferencens() {
        boolean useRcs = RcsCallWrapper.rcsIsLogined();
        Log.i("mmm","useRcs---:"+useRcs);
        if (!useRcs) {
            Intent intent = new Intent(this, RcsRegisterActivity.class);
            startActivityForResult(intent, 1000);
        } else {
            RcsConfigHelper.LastAccount.saveAccount(this, "");
            RcsCallWrapper.rcsLogout();
         //   login_status.setText("未登录");
            buttadd2.setText("未登录");
            number.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RcsServiceManager.removeCallBack(mRcsServiceCallBack);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == 1) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "失败", Toast.LENGTH_SHORT).show();
            }
        }
    }


//    public void areturn(View view){
//        onBackPressed();
//    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveRepeatLogin(intent);
    }


    private void receiveRepeatLogin(Intent intent) {
        if (intent != null && TextUtils.equals(intent.getAction(), RcsJsonParamConstants.RCS_JSON_ACTION_REPEAT_LOGIN)) {
            SharedPreferences sharedPreferences = getSharedPreferences(BuglePrefs.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.rcs_login_pref_key), false);
            editor.commit();
            RcsCallWrapper.rcsLogout();
            RcsConfigHelper.LastAccount.saveAccount(RcsMmsInitHelper.getContext(), "");
            Toast.makeText(RcsMmsInitHelper.getContext(), R.string.user_kicked_offline, Toast.LENGTH_LONG).show();
        }
    }
    public static final String WHERE_NOT_ARCHIVED =
            "(" +  DatabaseHelper.ConversationColumns.ARCHIVE_STATUS + " = 0)";
    public static final String SORT_ORDER =
            DatabaseHelper.ConversationColumns.PRIORITY + " DESC, " +
                    DatabaseHelper.ConversationColumns.SORT_TIMESTAMP + " DESC ";
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader( this,
                MessagingContentProvider.CONVERSATIONS_URI,
                ConversationListItemData.PROJECTION,
                WHERE_NOT_ARCHIVED,
                null,       // selection args
                SORT_ORDER);
    }
    /* Display name for the conversation */
    public static final String NAME = "name";
    public static final String SNIPPET_TEXT = "snippet_text";

    public static final String _ID = "_id";
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        Log.i("ooo","cursor.get----:"+cursor.getCount());
        while (cursor.moveToNext()){
            Log.i("ooo","cursor.name----:"+cursor.getString(cursor.getColumnIndex(NAME)));
            Log.i("ooo","cursor.SNIPPET_TEXT----:"+cursor.getString(cursor.getColumnIndex(SNIPPET_TEXT)));
            Log.i("ooo","cursor.conversionId----:"+cursor.getString(cursor.getColumnIndex(_ID)));
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
}
