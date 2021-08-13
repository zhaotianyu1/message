package com.android.messaging.tcl;

import android.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.action.ActionMonitor;
import com.android.messaging.datamodel.action.GetOrCreateConversationAction;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.ConversationData;
import com.android.messaging.ui.contact.ContactPickerFragment;
import com.android.messaging.ui.conversation.ComposeMessageView;
import com.android.messaging.util.Assert;
import com.google.common.annotations.VisibleForTesting;
import com.tcl.uicompat.TCLButton;
import com.tcl.uicompat.TCLEditText;

public class AddMessageFragement extends Fragment implements GetOrCreateConversationAction.GetOrCreateConversationActionListener {

    private TCLEditText text_input;
    private TCLButton send_message_body_button;
    private TCLButton add_message_return_button;
    @VisibleForTesting
    final Binding<ConversationData> mBinding = BindingBase.createBinding(this);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onGetOrCreateConversationSucceeded(ActionMonitor monitor, Object data, String conversationId) {

        Log.i("cccc","onGetOrCreateNewConversation-----");
        mHost.onGetOrCreateNewConversation(conversationId);

    }

    @Override
    public void onGetOrCreateConversationFailed(ActionMonitor monitor, Object data) {

    }

    public interface AddMessageFragementHost {
        void onGetOrCreateNewConversation(String conversationId);
        void onBackButtonPressed();
        void onInitiateAddMoreParticipants();
        void onParticipantCountChanged(boolean canAddMoreParticipants);
        void invalidateActionBar();
    }
    public void setHost(final AddMessageFragementHost host) {
        mHost = host;
    }
    private AddMessageFragementHost mHost;
    /**
     * {@inheritDoc} from Fragment
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_add_message, container, false);
        text_input = view.findViewById(R.id.text_input);
        send_message_body_button = view.findViewById(R.id.send_message_body_button);
        add_message_return_button = view.findViewById(R.id.add_message_return_button);
        send_message_body_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mHost.onGetOrCreateNewConversation(conversationId);
            }
        });
        return view;
    }



}
