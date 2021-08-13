package com.juphoon.chatbotmaap.chatbotMessages;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class ConversationIntent {
    public void startLaunchConversationActivity(final Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        context.startActivity(intent);
    }

}
