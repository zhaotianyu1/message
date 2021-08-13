package com.juphoon.helper.mms.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.ui.UIIntents;
import com.juphoon.helper.RcsBroadcastHelper;
import com.juphoon.helper.RcsGroupHelper;
import com.juphoon.rcs.tool.RcsCheckUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RmsDefine;

public class RcsCreateCroupActivity extends RcsChooseActivity {

    private final static String DEFAULT_GROUP_NAME = "Group";
    private ProgressDialog mProgress;

    private interface IGroupNameListener {
        void onGroupName(String name);
    }

    @Override
    protected void doChooseGroupAction() {
        if (!TextUtils.isEmpty(mSelectGroupChatId)) {
            startNewConversation(mSelectGroupChatId);
            finish();
        }
    }

    @Override
    protected void doChooseContactsAction() {
        showGroupNameDialog(DEFAULT_GROUP_NAME, new IGroupNameListener() {

            @Override
            public void onGroupName(String name) {
                if (RcsServiceManager.isLogined()) {
                    showProgress();
                    RcsGroupHelper.createGroup(name, null,
                            TextUtils.join(";", mListSelectPhones.toArray(new String[mListSelectPhones.size()])),
                            new RcsBroadcastHelper.IGroupCreateListener() {

                                @Override
                                public void onGroupCreateResult(boolean succ, String groupChatId) {
                                    hideProgress();
                                    if (succ) {
                                        startNewConversation(groupChatId);
                                        finish();
                                    } else {
                                        Toast.makeText(RcsCreateCroupActivity.this, R.string.fail, Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(RcsCreateCroupActivity.this, R.string.not_login, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProgress() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(this);
            mProgress.setTitle(R.string.create_group_chat);
            mProgress.setCancelable(false);
        }
        mProgress.show();
    }

    private void hideProgress() {
        if (mProgress != null) {
            mProgress.dismiss();
        }
    }

    private void showGroupNameDialog(final String defaultName, final IGroupNameListener listener) {
        final EditText editText = new EditText(this);
        InputFilter[] filters = { new LengthFilter(90) };
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        linearLayout.setPadding(50, 0, 50, 0);
        editText.setFilters(filters);
        editText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        editText.setSingleLine();
        linearLayout.addView(editText);
        editText.setHint(defaultName);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // TODO 群名称检查函数要修改下
                String text = editable.toString();
                if (!RcsCheckUtils.checkGroupName(text)) {
                    if (text.length() > 0) {
                        String newStr = RcsCheckUtils.subMaxLenGroupName(text);
                        editText.setText(newStr);
                    }
                }
            }
        });

        new AlertDialog.Builder(this).setTitle(R.string.group_name).setView(linearLayout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int index) {
                        String text = editText.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            listener.onGroupName(text);
                        } else {
                            listener.onGroupName(defaultName);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).create().show();
    }

    private void startNewConversation(final String groupChatId) {

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... arg0) {
                long threadId = RmsDefine.Threads.getOrCreateThreadId(RcsCreateCroupActivity.this,
                        RmsDefine.RMS_GROUP_THREAD, groupChatId);
                DatabaseWrapper db = DataModel.get().getDatabase();
                String convId = BugleDatabaseOperations.getOrCreateConversationFromThreadId(db, threadId, false, -1);
                return convId;
            }

            protected void onPostExecute(String result) {
                if (!TextUtils.isEmpty(result)) {
                    UIIntents.get().launchConversationActivity(RcsCreateCroupActivity.this, result, null);
                }
            };
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
