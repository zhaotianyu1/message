package com.juphoon.helper.mms.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.datamodel.data.ParticipantListItemData;
import com.android.messaging.datamodel.data.PeopleAndOptionsData;
import com.android.messaging.datamodel.data.PeopleAndOptionsData.PeopleAndOptionsDataListener;
import com.android.messaging.datamodel.data.PersonItemData;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.PersonItemView;
import com.android.messaging.ui.conversationsettings.CopyContactDetailDialog;
import com.android.messaging.util.Assert;
import com.juphoon.helper.mms.RcsDatabaseMessages.RmsMessage;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsNumberUtils;
import com.juphoon.service.RcsImServiceConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RcsPeopleActivity extends BugleActionBarActivity implements PeopleAndOptionsDataListener {

    public static final String CONVERSATION_ID = "conversation_id";
    public static final String MEASSAGE_ID = "message_id";

    private Context mContext = RcsPeopleActivity.this;
    private ListView mListView;
    private PeopleListAdapter mPeopleListAdapter;
    private final Binding<PeopleAndOptionsData> mBinding = BindingBase.createBinding(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcs_people);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final String conversationId = getIntent().getStringExtra(CONVERSATION_ID);
        final String messageId = getIntent().getStringExtra(MEASSAGE_ID);
        mListView = (ListView) findViewById(android.R.id.list);
        setConversationId(conversationId);
        initData(messageId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBinding.unbind();
    }

    private void initData(final String messageId) {
        new AsyncTask<Void, Void, RmsMessage>() {

            @Override
            protected RmsMessage doInBackground(Void... arg0) {
                DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
                MessageData readMessageData = BugleDatabaseOperations.readMessageData(dbWrapper, messageId);
                RmsMessage loadRms = RcsMmsUtils.loadRms(readMessageData.getSmsMessageUri());
                return loadRms;
            }

            protected void onPostExecute(RmsMessage result) {
                mPeopleListAdapter = new PeopleListAdapter(mContext, result.mImdnStatus);
                mListView.setAdapter(mPeopleListAdapter);
                mBinding.getData().init(getLoaderManager(), mBinding);
            };
        }.execute();
    }

    private void setConversationId(final String conversationId) {
        Assert.notNull(conversationId);
        mBinding.bind(DataModel.get().createPeopleAndOptionsData(conversationId, mContext, this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsCursorUpdated(PeopleAndOptionsData data, Cursor cursor) {
        Assert.isTrue(cursor == null || cursor.getCount() == 1);
        mBinding.ensureBound(data);
    }

    @Override
    public void onParticipantsListLoaded(PeopleAndOptionsData data, List<ParticipantData> participants) {
        mBinding.ensureBound(data);
        mPeopleListAdapter.updateParticipants(participants);
    }

    /**
     * An adapter that takes a list of ParticipantData and displays them as a list
     * of ParticipantListItemViews.
     */
    private class PeopleListAdapter extends ArrayAdapter<ParticipantData> {
        private String imdnStatus = null;

        class ViewHolder {
            PersonItemView personItem;
            TextView imdnStatusView;
        }

        public PeopleListAdapter(final Context context, String imdnStatus) {
            super(context, R.layout.rcs_people_item, new ArrayList<ParticipantData>());
            this.imdnStatus = imdnStatus;
        }

        public void updateParticipants(final List<ParticipantData> newList) {
            clear();
            addAll(newList);
            notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            ViewHolder holder;
            final ParticipantData item = getItem(position);
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = (FrameLayout) inflater.inflate(R.layout.rcs_people_item, parent, false);
                holder = new ViewHolder();
                holder.personItem = (PersonItemView) convertView.findViewById(R.id.people_item);
                holder.imdnStatusView = (TextView) convertView.findViewById(R.id.message_status);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final ParticipantListItemData itemData = DataModel.get().createParticipantListItemData(item);
            PersonItemView peopleItem = holder.personItem;
            peopleItem.bind(itemData);

            // Any click on the row should have the same effect as clicking the avatar icon
            final PersonItemView itemViewClosure = peopleItem;
            peopleItem.setListener(new PersonItemView.PersonItemViewListener() {
                @Override
                public void onPersonClicked(final PersonItemData data) {
                    itemViewClosure.performClickOnAvatar();
                }

                @Override
                public boolean onPersonLongClicked(final PersonItemData data) {
                    if (mBinding.isBound()) {
                        final CopyContactDetailDialog dialog = new CopyContactDetailDialog(getContext(),
                                data.getDetails());
                        dialog.show();
                        return true;
                    }
                    return false;
                }
            });
            if (!TextUtils.isEmpty(imdnStatus)) {
                HashMap<String, Integer> imdnStatusMap = getImdnStatusMap(imdnStatus);
                String formatPhone = RcsNumberUtils.formatPhone86(itemData.getNormalizedDestination());
                if (imdnStatusMap.containsKey(formatPhone)) {
                    int status = imdnStatusMap.get(formatPhone);
                    if (status == RcsImServiceConstants.EN_MTC_IMDN_STATE_DELIVERY) {
                        holder.imdnStatusView.setText(getString(R.string.delivered_status_content_description));
                    }
                }
            }
            return convertView;
        }
    }

    private HashMap<String, Integer> getImdnStatusMap(String statusStr) {
        HashMap<String, Integer> statusMap = new HashMap<String, Integer>();
        String[] statusArray = statusStr.split(";");
        for (String status : statusArray) {
            String[] keyValue = status.split(":");
            if (keyValue.length == 2)
                statusMap.put(keyValue[0], Integer.valueOf(keyValue[1]));
        }
        return statusMap;
    }
}
