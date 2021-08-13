package com.android.messaging.datamodel.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

import com.android.messaging.datamodel.BoundCursorLoader;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.binding.BindableData;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.util.Assert;
import com.android.messaging.util.LogUtil;

public class ChatbotConversationListData extends BindableData
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;
    private static final String BINDING_ID = "bindingId";
    private static final int CONVERSATION_LIST_LOADER = 1;
    private static final int BLOCKED_PARTICIPANTS_AVAILABLE_LOADER = 2;
    private ChatbotConversationListDataListener mListener;
    private LoaderManager mLoaderManager;

    private static final String WHERE_ARCHIVED =
            "(" + ConversationListItemData.ConversationListViewColumns.ARCHIVE_STATUS + " = 1)";
    public static final String WHERE_NOT_ARCHIVED =
            "(" + ConversationListItemData.ConversationListViewColumns.ARCHIVE_STATUS + " = 0)";

    public static final String SORT_ORDER =
            ConversationListItemData.ConversationListViewColumns.PRIORITY + " DESC, " +
                    ConversationListItemData.ConversationListViewColumns.SORT_TIMESTAMP + " DESC ";

    private final boolean mArchivedMode;
    private final Context mContext;


    public ChatbotConversationListData(final Context context, final ChatbotConversationListDataListener listener,
                                       final boolean archivedMode) {
        mListener = listener;
        mContext = context;
        mArchivedMode = archivedMode;
    }


    public interface ChatbotConversationListDataListener {
        public void onConversationListCursorUpdated(ChatbotConversationListData data, Cursor cursor);

        public void setBlockedParticipantsAvailable(boolean blockedAvailable);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String bindingId = args.getString(BINDING_ID);
        android.content.Loader<Cursor> loader = null;
        // Check if data still bound to the requesting ui element
        if (isBound(bindingId)) {
            switch (id) {
                case CONVERSATION_LIST_LOADER:
                    loader = new BoundCursorLoader(bindingId, mContext,
                            MessagingContentProvider.CONVERSATIONS_URI,
                            ConversationListItemData.PROJECTION,
                            mArchivedMode ? WHERE_ARCHIVED : WHERE_NOT_ARCHIVED,
                            null,       // selection args
                            SORT_ORDER);
                    break;
                default:
                    Assert.fail("Unknown loader id");
                    break;
            }
        } else {
            LogUtil.w(TAG, "Creating loader after unbinding list");
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> generic, Cursor data) {
        final BoundCursorLoader loader = (BoundCursorLoader) generic;
        if (isBound(loader.getBindingId())) {
            switch (loader.getId()) {
                case CONVERSATION_LIST_LOADER:
                    mListener.onConversationListCursorUpdated(this, data);
                    break;
                default:
                    Assert.fail("Unknown loader id");
                    break;
            }
        } else {
            LogUtil.w(TAG, "Loader finished after unbinding list");
        }
    }

    private Bundle mArgs;

    public void init(final LoaderManager loaderManager,
                     final BindingBase<ChatbotConversationListData> binding) {
        mArgs = new Bundle();
        mArgs.putString(BINDING_ID, binding.getBindingId());
        mLoaderManager = loaderManager;
        mLoaderManager.initLoader(CONVERSATION_LIST_LOADER, mArgs, this);
        mLoaderManager.initLoader(BLOCKED_PARTICIPANTS_AVAILABLE_LOADER, mArgs, this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    protected void unregisterListeners() {

    }
}
