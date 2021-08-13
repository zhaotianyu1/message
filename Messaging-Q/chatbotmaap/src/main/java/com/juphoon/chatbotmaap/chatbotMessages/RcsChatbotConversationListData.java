package com.juphoon.chatbotmaap.chatbotMessages;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

/**
 * 同步表会话数据
 */

public class RcsChatbotConversationListData
        implements LoaderManager.LoaderCallbacks<Cursor> {


    public static final String AUTHORITY =
            "com.android.juphoon_messaging.datamodel.MessagingContentProvider";
    private static final String CONTENT_AUTHORITY = "content://" + AUTHORITY + '/';

    // Conversations query
    private static final String CONVERSATIONS_QUERY = "conversations";

    public static final Uri CONVERSATIONS_URI = Uri.parse(CONTENT_AUTHORITY + CONVERSATIONS_QUERY);
    static final Uri PARTS_URI = Uri.parse(CONTENT_AUTHORITY + "parts");

    public static final String[] PROJECTION = {
            "_id",
            "name",
            "icon",
            "snippet_text",
            "sort_timestamp",
            "read",
            "preview_uri",
            "preview_content_type",
            "participant_contact_id",
            "participant_lookup_key",
            "participant_normalized_destination",
            "participant_count",
            "current_self_id",
            "notification_enabled",
            "notification_sound_uri",
            "notification_vibration",
            "include_email_addr",
            "message_status",
            "show_draft",
            "draft_preview_uri",
            "draft_preview_content_type",
            "draft_snippet_text",
            "archive_status",
            "message_id",
            "subject_text",
            "subject_text",
            "raw_status",
            "snippet_sender_first_name",
            "snippet_sender_display_destination",
            "IS_ENTERPRISE",
            // juphoon
            "rms_thread_type",
            "priority", // juphoon
            "sms_thread_id", // juphoon
    };

    private static final String WHERE_ARCHIVED =
            "(" + "participant_normalized_destination" + " LIKE '%@%' )";

    //juphoon
    public static final String SORT_ORDER =
            "priority" + " DESC, " +
                    "sort_timestamp" + " DESC ";


    private static final String BINDING_ID = "bindingId";
    private static final int CONVERSATION_LIST_LOADER = 1;
    private static final int BLOCKED_PARTICIPANTS_AVAILABLE_LOADER = 2;
    private ChatbotConversationListDataListener mListener;
    private LoaderManager mLoaderManager;

    private final boolean mArchivedMode;
    private final Context mContext;


    public RcsChatbotConversationListData(final Context context, final ChatbotConversationListDataListener listener,
                                          final boolean archivedMode) {
        mListener = listener;
        mContext = context;
        mArchivedMode = archivedMode;
    }

    public interface ChatbotConversationListDataListener {
        public void onConversationListCursorUpdated(RcsChatbotConversationListData data, Cursor cursor);

        public void setBlockedParticipantsAvailable(boolean blockedAvailable);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Loader<Cursor> loader = null;
        // Check if data still bound to the requesting ui element
        switch (id) {
            case CONVERSATION_LIST_LOADER:
                loader = new CursorLoader(mContext,
                        CONVERSATIONS_URI,
                        PROJECTION,
                        WHERE_ARCHIVED,
                        null,       // selection args
                        SORT_ORDER);
                break;
            default:
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull androidx.loader.content.Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case CONVERSATION_LIST_LOADER:
                if (mListener != null) {
                    mListener.onConversationListCursorUpdated(this, data);
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void onLoaderReset(@NonNull androidx.loader.content.Loader<Cursor> loader) {

    }

    private Bundle mArgs;

    public void init(final LoaderManager loaderManager) {
        mArgs = new Bundle();
        mLoaderManager = loaderManager;
        mLoaderManager.initLoader(CONVERSATION_LIST_LOADER, mArgs, this);
    }

}
