package com.juphoon.helper.mms.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.cursoradapter.widget.CursorAdapter;

import com.android.messaging.R;
import com.juphoon.helper.mms.RcsMessageForwardHelper;
import com.juphoon.helper.mms.RcsVCardHelper;
import com.juphoon.service.RmsDefine;
import com.juphoon.service.RmsDefine.Rms;

import java.util.ArrayList;
import java.util.List;

public class RcsForwardActivity extends Activity {

    public final static String FORWARD_MSG_ID = "forward_msg_id";
    public final static String FORWARD_ART_JSON = "forward_art_json";
    public final static String FORWARD_SMS = "forward_sms";

    private final static int REQUEST_CODE_CHOOSE_CONTACT = 100;
    private final static int REQUEST_CODE_CHOOSE_GROUP = 101;

    // 用去调起activity时设置显示的会话类型，不填表示所有，有内容则以','隔开，例如 1,2 表示显示一对一和一对多
    private static final Uri sAllThreadsUri = Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
    private static final String[] ALL_THREADS_PROJECTION = {Threads._ID, Threads.RECIPIENT_IDS, RmsDefine.Threads.RMS_THREAD_TYPE};
    protected static final int ID = 0;
    protected static final int RECIPIENT_IDS = 1;
    protected static final int RMS_THREAD_TYPE = 2;

    private long mMsgId;
    private String mSms;
    private Uri mVCard;
    private Uri mImageFile;
    private Uri mVideoFile;
    private String mCompainText;

    public static void startByMsgId(Context context, long msgId) {
        Intent intent = new Intent(context, RcsForwardActivity.class);
        intent.putExtra(FORWARD_MSG_ID, msgId);
        context.startActivity(intent);
    }

    public static void startByArtJson(Context context, String artJson) {
        Intent intent = new Intent(context, RcsForwardActivity.class);
        intent.putExtra(FORWARD_ART_JSON, artJson);
        context.startActivity(intent);
    }

    public static void startBySms(Context context, String sms) {
        Intent intent = new Intent(context, RcsForwardActivity.class);
        intent.putExtra(FORWARD_SMS, sms);
        context.startActivity(intent);
    }

    private class ConversationAdapter extends CursorAdapter {

        public ConversationAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            String recipientIds = c.getString(RECIPIENT_IDS);
            int threadType = c.getInt(RMS_THREAD_TYPE);
            //TODO juphoon 为了编译通过,后续需要改动 
//            ContactList recipients = ContactList.getByIds(recipientIds, false);

//            ImageView avatarView = (ImageView) view.findViewById(R.id.imageview_avatar);
//            TextView nameView = (TextView) view.findViewById(R.id.textview_name);
//            if (threadType == RmsDefine.RMS_GROUP_THREAD) {
//                avatarView.setImageResource(R.drawable.ic_contact_picture);
//                nameView.setText(recipients.get(0).getName());
//            } else if (threadType == RmsDefine.BROADCAST_THREAD) {
//                avatarView.setImageResource(R.drawable.ic_contact_picture);
//                nameView.setText(recipients.formatNamesAndNumbers(";"));
//            } else if (threadType == RmsDefine.COMMON_THREAD) {
//                avatarView.setImageResource(R.drawable.ic_contact_picture);
//                if (recipients.get(0).hasAvatar()) {
//                    avatarView.setImageDrawable(recipients.get(0).getAvatar(context, getDrawable(R.drawable.ic_contact_picture)));
//                } else {
//                    String number = recipients.get(0).getNumber();
//                    RcsBitmapCache.getBitmapByPhone(avatarView, number, false);
//                }
//                nameView.setText(recipients.get(0).getName());
//            } else if (threadType == RmsDefine.RMS_PUBLIC_THREAD) {
//                avatarView.setImageResource(R.drawable.ic_contact_picture);
//                RcsPublicHelper.RcsPublicInfo info = RcsPublicHelper.getPublicAccountInfo(recipients.get(0).getNumber());
//                if (info != null) {
//                    RcsBitmapCache.getBitmapFromUrl(avatarView, info.mLogo, false);
//                }
//                nameView.setText(recipients.get(0).getName());
//            }
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            return LayoutInflater.from(mContext).inflate(R.layout.forward_conversation_item, parent, false);
        }

    }

    private class ConversationLoadTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... arg0) {
            Cursor cursor = getContentResolver().query(sAllThreadsUri, ALL_THREADS_PROJECTION,
                    RmsDefine.Threads.RMS_THREAD_TYPE + " in (" + buildThreadTypeWhrere(true, true, true, false) + ")",
                    null, "date desc");
            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mConversationAdapter.swapCursor(result);
            mConversationAdapter.notifyDataSetChanged();
        }

    }

    private ListView mListViewConversation;
    private ConversationAdapter mConversationAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.forward_activity);

        mMsgId = getIntent().getLongExtra(FORWARD_MSG_ID, -1);
        mSms = getIntent().getStringExtra(FORWARD_SMS);

        mListViewConversation = (ListView) findViewById(R.id.listview_conversations);
        mConversationAdapter = new ConversationAdapter(this, null);
        mListViewConversation.setAdapter(mConversationAdapter);
        mListViewConversation.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) mListViewConversation.getItemAtPosition(position);
                doChooseAction(cursor);
            }
        });

        setTitle(R.string.forward_message);

        new ConversationLoadTask().execute();

        String type = getIntent().getType();
        if (!TextUtils.isEmpty(type)) {
            if (TextUtils.equals(type, "text/x-vcard")) {
                mVCard = RcsVCardHelper.exportVcard(this, (Uri) getIntent().getExtras().getParcelable(Intent.EXTRA_STREAM));
            } else if (TextUtils.equals(type, "text/plain")) {
                mCompainText = getIntent().getExtras().getString(Intent.EXTRA_TEXT);
            } else if (type.startsWith("image/")) {
                mImageFile = getIntent().getExtras().getParcelable(Intent.EXTRA_STREAM);
            } else if (type.startsWith("video/")) {
                mVideoFile = getIntent().getExtras().getParcelable(Intent.EXTRA_STREAM);
            }
        }
    }

    private void doChooseAction(Cursor cursor) {
        int threadType = cursor.getInt(RMS_THREAD_TYPE);
        String recipientIds = cursor.getString(RECIPIENT_IDS);
        //TODO juphoon 为了编译通过,后续需要改动
//        ContactList recipients = ContactList.getByIds(recipientIds, false); // 关联原生代码
//        if (mMsgId != -1) {
//            RcsMessageForwardHelper.forwardMessage(threadType, mMsgId, recipients.formatNumbers(";"));
//        } else if (!TextUtils.isEmpty(mArtJson)) {
//            RcsMessageForwardHelper.forwardArtMessage(threadType, mArtJson, recipients.formatNumbers(";"));
//        } else if (!TextUtils.isEmpty(mSms)) {
//            RcsMessageForwardHelper.forwardSmsMessage(threadType, mSms, recipients.formatNumbers(";"));
//        } else if (mVCard != null) {
//            RcsMessageForwardHelper.forwardFileMessage(threadType, Rms.RMS_MESSAGE_TYPE_VCARD, mVCard, recipients.formatNumbers(";"));
//        } else if (mImageFile != null) {
//            RcsMessageForwardHelper.forwardFileMessage(threadType, Rms.RMS_MESSAGE_TYPE_IMAGE, mImageFile, recipients.formatNumbers(";"));
//        } else if (mVideoFile != null) {
//            RcsMessageForwardHelper.forwardFileMessage(threadType, Rms.RMS_MESSAGE_TYPE_VIDEO, mVideoFile, recipients.formatNumbers(";"));
//        } else if (mCompainText != null) {
//            RcsMessageForwardHelper.forwardSmsMessage(threadType, mCompainText, recipients.formatNumbers(";"));
//        }
        finish();
    }

    private String buildThreadTypeWhrere(boolean common, boolean broadcast, boolean groupChat, boolean publicChat) {
        List<String> l = new ArrayList<>();
        if (common) {
            l.add(String.valueOf(RmsDefine.COMMON_THREAD));
        }
        if (broadcast) {
            l.add(String.valueOf(RmsDefine.BROADCAST_THREAD));
        }
        if (groupChat) {
            l.add(String.valueOf(RmsDefine.RMS_GROUP_THREAD));
        }
        return TextUtils.join(",", l.toArray(new String[l.size()]));
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button_forward_contact: {
                Intent intent = new Intent(RcsForwardActivity.this, RcsChooseActivity.class);
                intent.putExtra(RcsChooseActivity.LIMIT_MIN_SELECT, 0);
                startActivityForResult(intent, REQUEST_CODE_CHOOSE_CONTACT);
            }
            break;
            case R.id.button_forward_group: {
                Intent intent = new Intent(RcsForwardActivity.this, RcsChooseActivity.class);
                intent.putExtra(RcsChooseActivity.CHOOSE_MODE, RcsChooseActivity.MODE_GROUP_ONLY);
                startActivityForResult(intent, REQUEST_CODE_CHOOSE_GROUP);
            }
            break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_CONTACT) {
                ArrayList<String> phones = data.getStringArrayListExtra(RcsChooseActivity.RESULT_PHONES);
                if (phones != null && phones.size() > 0) {
                    if (mMsgId != -1) {
                        if (phones.size() == 1) {
                            RcsMessageForwardHelper.forwardMessage(RmsDefine.COMMON_THREAD, mMsgId, TextUtils.join(";", phones.toArray()));
                        } else {
                            RcsMessageForwardHelper.forwardMessage(RmsDefine.BROADCAST_THREAD, mMsgId, TextUtils.join(";", phones.toArray()));
                        }
                    } else if (mVCard != null) {
                        if (phones.size() == 1) {
                            RcsMessageForwardHelper.forwardFileMessage(RmsDefine.COMMON_THREAD, Rms.RMS_MESSAGE_TYPE_VCARD, mVCard, TextUtils.join(";", phones.toArray()));
                        } else {
                            RcsMessageForwardHelper.forwardFileMessage(RmsDefine.BROADCAST_THREAD, Rms.RMS_MESSAGE_TYPE_VCARD, mVCard, TextUtils.join(";", phones.toArray()));
                        }
                    } else if (mImageFile != null) {
                        if (phones.size() == 1) {
                            RcsMessageForwardHelper.forwardFileMessage(RmsDefine.COMMON_THREAD, Rms.RMS_MESSAGE_TYPE_IMAGE, mImageFile, TextUtils.join(";", phones.toArray()));
                        } else {
                            RcsMessageForwardHelper.forwardFileMessage(RmsDefine.BROADCAST_THREAD, Rms.RMS_MESSAGE_TYPE_IMAGE, mImageFile, TextUtils.join(";", phones.toArray()));
                        }
                    } else if (mVideoFile != null) {
                        if (phones.size() == 1) {
                            RcsMessageForwardHelper.forwardFileMessage(RmsDefine.COMMON_THREAD, Rms.RMS_MESSAGE_TYPE_VIDEO, mVideoFile, TextUtils.join(";", phones.toArray()));
                        } else {
                            RcsMessageForwardHelper.forwardFileMessage(RmsDefine.BROADCAST_THREAD, Rms.RMS_MESSAGE_TYPE_VIDEO, mVideoFile, TextUtils.join(";", phones.toArray()));
                        }
                    } else if (TextUtils.isEmpty(mCompainText)) {
                        if (phones.size() == 1) {
                            RcsMessageForwardHelper.forwardSmsMessage(RmsDefine.COMMON_THREAD, mCompainText, TextUtils.join(";", phones.toArray()));
                        } else {
                            RcsMessageForwardHelper.forwardSmsMessage(RmsDefine.BROADCAST_THREAD, mCompainText, TextUtils.join(";", phones.toArray()));
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_CHOOSE_GROUP) {
                String groupChatId = data.getStringExtra(RcsChooseActivity.RESULT_GROUPCHAT_ID);
                if (!TextUtils.isEmpty(groupChatId)) {
                    if (mMsgId != -1) {
                        RcsMessageForwardHelper.forwardMessage(RmsDefine.RMS_GROUP_THREAD, mMsgId, groupChatId);
                    } else if (mVCard != null) {
                        RcsMessageForwardHelper.forwardFileMessage(RmsDefine.RMS_GROUP_THREAD, Rms.RMS_MESSAGE_TYPE_VCARD, mVCard, groupChatId);
                    } else if (mImageFile != null) {
                        RcsMessageForwardHelper.forwardFileMessage(RmsDefine.RMS_GROUP_THREAD, Rms.RMS_MESSAGE_TYPE_IMAGE, mImageFile, groupChatId);
                    } else if (mVideoFile != null) {
                        RcsMessageForwardHelper.forwardFileMessage(RmsDefine.RMS_GROUP_THREAD, Rms.RMS_MESSAGE_TYPE_VIDEO, mVideoFile, groupChatId);
                    } else if (TextUtils.isEmpty(mCompainText)) {
                        RcsMessageForwardHelper.forwardSmsMessage(RmsDefine.RMS_GROUP_THREAD, mCompainText, groupChatId);
                    }
                }
            }
            finish();
        }
    }

}
