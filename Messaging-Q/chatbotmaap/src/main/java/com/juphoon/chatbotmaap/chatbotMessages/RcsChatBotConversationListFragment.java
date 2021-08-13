package com.juphoon.chatbotmaap.chatbotMessages;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.tcl.SimpleItemDecoration;

public class RcsChatBotConversationListFragment extends Fragment implements
        RcsChatbotConversationListItemView.HostInterface {

    public interface RcsChatBotConversationListFragmentHost {
        public void onConversationClick();
        public boolean isSelectionMode();
    }

    private SearchView mSearchView;
    private RecyclerView mConversationListView;
    private RcsChatBotConversationListAdapter mConversationListAdapter;
    private Activity mHost;

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return super.getLifecycle();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.chatbot_conversation_list_fragment, container, false);
        mHost = getActivity();
        initSearchView(view);
        initConversationListView(view);
        refreshConversationListData();
        return view;
    }

    private void refreshConversationListData() {
        RcsChatbotConversationListData chatbotConversationListData = new RcsChatbotConversationListData(getContext(), new RcsChatbotConversationListData.ChatbotConversationListDataListener() {
            @Override
            public void onConversationListCursorUpdated(RcsChatbotConversationListData data, Cursor cursor) {
                mConversationListAdapter.swapCursor(cursor);
            }

            @Override
            public void setBlockedParticipantsAvailable(boolean blockedAvailable) {

            }
        }, false);

        chatbotConversationListData.init(LoaderManager.getInstance(this));
    }

    private void initConversationListView(View view) {
        mConversationListView = view.findViewById(R.id.messages_recyclerView);
        mConversationListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mConversationListAdapter = new RcsChatBotConversationListAdapter(getContext(), null, this);

        mConversationListView.setAdapter(mConversationListAdapter);
        mConversationListView.addItemDecoration(new SimpleItemDecoration());
    }

    private void initSearchView(View view) {
        mSearchView = view.findViewById(R.id.message_search_view);
        mSearchView.setIconified(false);
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

            }
        });

        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return true;
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query)) {
                    Toast.makeText(mHost, R.string.chatbot_enter_search_key, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                }
                return true;
            }
        });
    }

    @Override
    public boolean isConversationSelected(String conversationId) {
        return false;
    }

    @Override
    public void onConversationClicked(RcsChatBotConversationListItemData conversationListItemData, boolean isLongClick) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = "sms:" + conversationListItemData.getOtherParticipantNormalizedDestination();
        intent.setData(Uri.parse(url));
        getContext().startActivity(intent);
    }

    @Override
    public boolean isSwipeAnimatable() {
        return false;
    }
}
