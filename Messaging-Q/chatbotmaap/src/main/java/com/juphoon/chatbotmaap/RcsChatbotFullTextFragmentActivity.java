package com.juphoon.chatbotmaap;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;


public class RcsChatbotFullTextFragmentActivity extends FragmentActivity {

    public final static String TITLE = "title";
    public final static String CONTENT = "content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_fulltext);
        initViwe();
    }


    public void initViwe() {
        TextView title = (TextView) findViewById(R.id.chatbot_fulltext_title);
        TextView content = (TextView) findViewById(R.id.chatbot_fulltext_content);
        title.setText(getIntent().getStringExtra(TITLE));
        content.setText(getIntent().getStringExtra(CONTENT));
        content.setMovementMethod(ScrollingMovementMethod.getInstance());
    }
}
