/**
 * Copyright (c) 2009, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.ui;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.provider.Telephony;
import android.provider.Telephony.MmsSms;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.mmslib.pdu.CharacterSets;
import com.android.messaging.ui.conversation.ConversationActivity;
import com.android.messaging.util.ContentType;
import com.google.gson.Gson;
import com.juphoon.chatbot.RcsChatbotCardBean;
import com.juphoon.service.RmsDefine;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Presents a List of search results.  Each item in the list represents a thread which
 * matches.  The item contains the contact (or phone number) as the "title" and a
 * snippet of what matches, below.  The snippet is taken from the most recent part of
 * the conversation that has a match.  Each match within the visible portion of the
 * snippet is highlighted.
 */
//juphoon
public class SearchActivity extends ListActivity
{
    private AsyncQueryHandler mQueryHandler;

    // Track which TextView's show which Contact objects so that we can update
    // appropriately when the Contact gets fully loaded.
//    private HashMap<Contact, TextView> mContactMap = new HashMap<Contact, TextView>();
    private static final String TAG = "SearchActivity";
    /* SPRD: Modify this for search function. @{ */
    private TextView mEmptyView;
    private Uri mUri;
    //juphoon search message add begin
    private String mConversationId;
    public static final String CONVERSATION_ID = "conversation_id";
    private AsyncSearchHandler mAsyncSearchHandler;
    private String mSearchStringParameter;
    //juphoon search message add end
    /* @}*/

    /*
     * Subclass of TextView which displays a snippet of text which matches the full text and
     * highlights the matches within the snippet.
     */
    public static class TextViewSnippet extends TextView {
        private static String sEllipsis = "\u2026";

        private static int sTypefaceHighlight = Typeface.BOLD;
        /* SPRD: Add this for bug 275907. @{ */
        //private int mTextColorForSearch;
        /* @} */

        private String mFullText;
        private String mTargetString;
        private Pattern mPattern;

        public TextViewSnippet(Context context, AttributeSet attrs) {
            super(context, attrs);
            /* SPRD: Add this for bug 275907. @{ */
            //mTextColorForSearch = context.getResources().getColor(R.color.subtitle_color_searchactivity);
            /* @} */
        }

        public TextViewSnippet(Context context) {
            super(context);
            /* SPRD: Add this for bug 275907. @{ */
            //mTextColorForSearch = context.getResources().getColor(R.color.subtitle_color_searchactivity);
            /* @} */
        }

        public TextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            /* SPRD: Add this for bug 275907. @{ */
            //mTextColorForSearch = context.getResources().getColor(R.color.subtitle_color_searchactivity);
            /* @} */
        }

        /**
         * We have to know our width before we can compute the snippet string.  Do that
         * here and then defer to super for whatever work is normally done.
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            /* SPRD: add for NullPointerException of mFullText. @{ */
            if (mFullText == null) {
                mFullText = "";
            }
            if(mTargetString ==null){
                return;
            }
            /* @} */
            String fullTextLower = mFullText.toLowerCase();
            String targetStringLower = mTargetString.toLowerCase();

            int startPos = 0;
            int searchStringLength = targetStringLower.length();
            int bodyLength = fullTextLower.length();

            Matcher m = mPattern.matcher(mFullText);
            if (m.find(0)) {
                startPos = m.start();
            }

            TextPaint tp = getPaint();

            float searchStringWidth = tp.measureText(mTargetString);
            float textFieldWidth = getWidth();

            float ellipsisWidth = tp.measureText(sEllipsis);
            textFieldWidth -= (2F * ellipsisWidth); // assume we'll need one on both ends

            String snippetString = null;
            // SPRD: fix AOB bug309996
            if (searchStringWidth > textFieldWidth && bodyLength >= searchStringLength) {
                snippetString = mFullText.substring(startPos, startPos + searchStringLength);
            } else {

                int offset = -1;
                int start = -1;
                int end = -1;
                /* TODO: this code could be made more efficient by only measuring the additional
                 * characters as we widen the string rather than measuring the whole new
                 * string each time.
                 */
                while (true) {
                    offset += 1;

                    int newstart = Math.max(0, startPos - offset);
                    int newend = Math.min(bodyLength, startPos + searchStringLength + offset);

                    if (newstart == start && newend == end) {
                        // if we couldn't expand out any further then we're done
                        break;
                    }
                    start = newstart;
                    end = newend;

                    // pull the candidate string out of the full text rather than body
                    // because body has been toLower()'ed
                    String candidate = mFullText.substring(start, end);
                    if (tp.measureText(candidate) > textFieldWidth) {
                        // if the newly computed width would exceed our bounds then we're done
                        // do not use this "candidate"
                        break;
                    }

                    snippetString = String.format(
                            "%s%s%s",
                            start == 0 ? "" : sEllipsis,
                            candidate,
                            end == bodyLength ? "" : sEllipsis);
                }
            }

            //Add log for ANR of Bug 295111.
            /*if (MessageUtils.OS_DEBUG) {
                Log.d(TAG, " snippetString = " + snippetString + " mTargetString = " + mTargetString);
            }*/
            /* SPRD: Modify this for search by contacts' name. @{ */
            //If snippetString is null, give a null string keep it go on the work.
            if (snippetString == null) {
                snippetString = "";
            }
            /* @} */

            SpannableString spannable = new SpannableString(snippetString);
            int start = 0;
   //         Log.d(TAG, " spannable = " + spannable);
            m = mPattern.matcher(snippetString);
         // SPRD: modify by bug342863 of ANR
            if(!mTargetString.equals("")){
                while (m.find(start)) {
                    spannable.setSpan(new StyleSpan(sTypefaceHighlight), m.start(), m.end(), 0);
                    /* SPRD: Add this for bug 275907. @{ */
                    //spannable.setSpan(new ForegroundColorSpan(mTextColorForSearch), m.start(), m.end(), 0);
                    /* @} */
                    start = m.end();
                }
            }
            setText(spannable);

            // do this after the call to setText() above
            super.onLayout(changed, left, top, right, bottom);
        }

        public void setText(String fullText, String target) {
            // Use a regular expression to locate the target string
            // within the full text.  The target string must be
            // found as a word start so we use \b which matches
            // word boundaries.
            // SPRD: Modify this for search by contacts' name.
            String patternString = /* "\\b" + */Pattern.quote(target);
            mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

            /* SPRD: Modify this for search by contacts' name. @{*/
            // mFullText = fullText;
            // mTargetString = target;
            mFullText = (fullText == null) ? "" : fullText;
            mTargetString = (target == null) ? "" : target;
            /* @} */
            requestLayout();
        }
    }

    /*Contact.UpdateListener mContactListener = new Contact.UpdateListener() {
        public void onUpdate(Contact updated) {
            TextView tv = mContactMap.get(updated);
            if (tv != null) {
                tv.setText(updated.getNameAndNumber());
            }
        }
    };*/

    @Override
    public void onStop() {
        super.onStop();
        //Contact.removeListener(mContactListener);
        //bug 438942 anr : because too many composemessageactivity is launched and then start this activity on the flags of 0x14000000(clear_top and new_task)
        //so that the task must clear all the above composemessageactivity  costs too long and then causes anr.
        //the resulation is : finish on the onStop() and do not need to clear other activities when starting.
        finish();
       //bug 438942 end
    }

    private long getThreadId(long sourceId, long which) {
        Uri.Builder b = Uri.parse("content://mms-sms/messageIdToThread").buildUpon();
        b = b.appendQueryParameter("row_id", String.valueOf(sourceId));
        b = b.appendQueryParameter("table_to_use", String.valueOf(which));
        String s = b.build().toString();

        Cursor c = getContentResolver().query(
                Uri.parse(s),
                null,
                null,
                null,
                null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return c.getLong(c.getColumnIndex("thread_id"));
                }
            } finally {
                c.close();
            }
        }
        return -1;
    }

    private String matchSearchString(String textString, String searchString) {
        try {
            JSONObject textJsonObj = new JSONObject(textString);
            String bodyStr = textJsonObj.optString(RmsDefine.Rms.BODY);
            if (bodyStr.contains(searchString)) {
                return bodyStr;
            }
            String extraStr = textJsonObj.optString(RmsDefine.Rms.RMS_EXTRA);
            RcsChatbotCardBean rcsChatbotCardBean = new Gson().fromJson(extraStr, RcsChatbotCardBean.class);
            if (rcsChatbotCardBean != null && rcsChatbotCardBean.message != null && rcsChatbotCardBean.message.generalPurposeCardCarousel != null && rcsChatbotCardBean.message.generalPurposeCardCarousel.content != null) {
                for (RcsChatbotCardBean.MessageBean.ContentBean bean : rcsChatbotCardBean.message.generalPurposeCardCarousel.content) {
                    if (bean.title != null && bean.title.contains(searchString)) {
                        return bean.title;
                    } else if (bean.description != null && bean.description.contains(searchString)) {
                        return bean.description;
                    }
                }
            }
            if (rcsChatbotCardBean != null && rcsChatbotCardBean.message != null && rcsChatbotCardBean.message.generalPurposeCard != null && rcsChatbotCardBean.message.generalPurposeCard.content != null) {
                RcsChatbotCardBean.MessageBean.ContentBean bean = rcsChatbotCardBean.message.generalPurposeCard.content;
                if (bean.title != null && bean.title.contains(searchString)) {
                    return bean.title;
                } else if (bean.description != null && bean.description.contains(searchString)) {
                    return bean.description;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mSearchStringParameter = getIntent().getStringExtra(SearchManager.QUERY);
        if (mSearchStringParameter == null) {
            mSearchStringParameter = getIntent().getStringExtra("intent_extra_data_key" /*SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA*/);
        }
        mConversationId = getIntent().getStringExtra(SearchActivity.CONVERSATION_ID);
        final String searchString =
                mSearchStringParameter != null ? mSearchStringParameter.trim() : mSearchStringParameter;
        System.out.println("jordan,searchString = " + searchString);
        // If we're being launched with a source_id then just go to that particular thread.
        // Work around the fact that suggestions can only launch the search activity, not some
        // arbitrary activity (such as ComposeMessageActivity).
        final Uri u = getIntent().getData();
        if (u != null && u.getQueryParameter("source_id") != null) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        long sourceId = Long.parseLong(u.getQueryParameter("source_id"));
                        long whichTable = Long.parseLong(u.getQueryParameter("which_table"));
                        long threadId = getThreadId(sourceId, whichTable);
                        System.out.println("sourceId = " + sourceId + ", whichTable = " + whichTable + ", threadId = " + threadId);
                        final Intent onClickIntent = new Intent(SearchActivity.this, ConversationActivity.class);
                        onClickIntent.putExtra("highlight", searchString);
                        onClickIntent.putExtra("select_id", sourceId);
                        onClickIntent.putExtra("thread_id", threadId);
                        startActivity(onClickIntent);
                        finish();
                        return;
                    } catch (NumberFormatException ex) {
                        // ok, we do not have a thread id so continue
                    }
                }
            }, "Search thread");
            t.start();
            return;
        }

        setContentView(R.layout.search_activity);
//        ContentResolver cr = getContentResolver();
        /*
         * SPRD:Bug#255558,the searched item is disappeared after edit it.
         *
         * @{
         */
        mContentResolver = getContentResolver();
        registerConversationListChangeObserver();
        /*
         * @}
         */
//        searchStringParameter = searchStringParameter.trim();
        final ListView listView = getListView();
        listView.setItemsCanFocus(true);
        listView.setFocusable(true);
        listView.setClickable(true);
        // SPRD: Modify this for search function.
        mEmptyView = (TextView)findViewById(android.R.id.empty);

        // I considered something like "searching..." but typically it will
        // flash on the screen briefly which I found to be more distracting
        // than beneficial.
        // This gets updated when the query completes.
        setTitle("");

        //Contact.addListener(mContactListener);

        //juphoon 替换搜索方式为同步表内搜索
        mAsyncSearchHandler = new AsyncSearchHandler() {
            @Override
            public void onQueryComplete(final Cursor c) {
                if (c == null) {
                    setTitle(getResources().getQuantityString(
                            R.plurals.search_results_title,0, 0,searchString));
                    System.out.println("enter SearchActivity, Query result cursor is null, return");
                    return;
                }
                System.out.println("jordan,in SearchActivity cursor size = " + c.getCount());
                int cursorCount = c.getCount();
                new AsyncTask<Void, Void, ArrayList<SearchViewBean>>() {
                    @Override
                    protected ArrayList<SearchViewBean> doInBackground(Void... arg0) {
                        ArrayList<SearchViewBean> searchList = changeCursorToList(c, searchString);
                        return searchList;
                    }

                    protected void onPostExecute(final ArrayList<SearchViewBean> searchList) {
                        setTitle(getResources().getQuantityString(
                                R.plurals.search_results_title,
                                searchList.size(),
                                searchList.size(),
                                searchString));
                        BaseAdapter adapter = new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return searchList.size();
                            }

                            @Override
                            public Object getItem(int position) {
                                return null;
                            }

                            @Override
                            public long getItemId(int position) {
                                return 0;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                final SearchViewBean bean = searchList.get(position);
                                LayoutInflater inflater = LayoutInflater.from(SearchActivity.this);
                                View view = inflater.inflate(R.layout.search_item, parent, false);
                                TextView title = (TextView) (view.findViewById(R.id.title));
                                TextViewSnippet snippet = (TextViewSnippet) (view.findViewById(R.id.subtitle));
                                snippet.setText(bean.snipText, searchString);
                                title.setText(bean.conversationName);
                                view.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                final DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
                                                dbWrapper.beginTransaction();
                                                Cursor cursor = BugleDatabaseOperations.queryMessage(dbWrapper, bean.conversationId, "");
                                                dbWrapper.endTransaction();
                                                int index = 0;
                                                if (cursor != null) {
                                                    while (cursor.moveToNext()) {
                                                        if (bean.messageId == cursor.getInt(cursor.getColumnIndex(DatabaseHelper.PartColumns.MESSAGE_ID))) {
                                                            break;
                                                        }
                                                        index++;
                                                    }
                                                    cursor.close();
                                                }
                                                UIIntents.get().launchConversationActivityWithSearch(SearchActivity.this, bean.conversationId, index);
                                            }
                                        }).start();
                                    }
                                });
                                return view;
                            }
                        };
                        setListAdapter(adapter);
                    }
                }.execute();
                listView.setFocusable(true);
                listView.setFocusableInTouchMode(true);
                listView.requestFocus();
                if (cursorCount > 0) {
                    //add by jordan
                    SearchRecentSuggestions recent = null;
                    //SearchRecentSuggestions recent = ((MmsApp)getApplication()).getRecentSuggestions();
                    if (recent != null) {
                        recent.saveRecentQuery(
                                searchString,
                                getString(R.string.search_history,
                                        cursorCount, searchString));
                    }
                } else {
                    /* SPRD: Modify this for search function. @{ */
                    mEmptyView.setVisibility(View.VISIBLE);
                    mEmptyView.setText(R.string.search_empty);
                    /* @} */
                }
            }
        };
        // When the query completes cons up a new adapter and set our list adapter to that.
        mQueryHandler = new AsyncQueryHandler(mContentResolver) {
            protected void onQueryComplete(int token, Object cookie, final Cursor c) {
                if (c == null) {
                    setTitle(getResources().getQuantityString(
                            R.plurals.search_results_title,
                            0,
                            0,
                            searchString));
                    System.out.println("enter SearchActivity, Query result cursor is null, return");
                    return;
                }
                System.out.println("jordan,in SearchActivity cursor size = " + c.getCount());
                final int address = c.getColumnIndex("address");
                final int threadIdPos = c.getColumnIndex("thread_id");
                final int bodyPos     = c.getColumnIndex("body");
                int cursorCount = c.getCount();
                setTitle(getResources().getQuantityString(
                        R.plurals.search_results_title,
                        cursorCount,
                        cursorCount,
                        searchString));

                new AsyncTask<Void, Void, HashMap<Long, String[]>>(){

                    @Override
                    protected HashMap<Long, String[]> doInBackground(Void... arg0) {
                        HashMap<Long, String[]> convIdAndNames = new HashMap<Long, String[]>();
                        if (c != null) {
                            while (c.moveToNext()) {
                                long threadId = c.getLong(threadIdPos);
                                DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
                                String convId = BugleDatabaseOperations.getExistingConversation(dbWrapper, threadId, false);
                                String convName = BugleDatabaseOperations.getExistingConversationName(dbWrapper, threadId);
                                convIdAndNames.put(threadId, new String[] {convId, convName});
                            }
                        }
                        return convIdAndNames;
                    }

                    protected void onPostExecute(final HashMap<Long, String[]> result) {
                        // Note that we're telling the CursorAdapter not to do auto-requeries. If we
                        // want to dynamically respond to changes in the search results,
                        // we'll have have to add a setOnDataSetChangedListener().
                        setListAdapter(new CursorAdapter(SearchActivity.this,
                                c, false /* no auto-requery */) {
                            @Override
                            public void bindView(View view, final Context context, Cursor cursor) {
                                final TextView title = (TextView)(view.findViewById(R.id.title));
                                final TextViewSnippet snippet = (TextViewSnippet)(view.findViewById(R.id.subtitle));
                               
                                long threadId = cursor.getLong(threadIdPos);
                                String[] convIdAndName = result.get(threadId);
                                final String convId = convIdAndName[0];
                                String convName = convIdAndName[1];
                                final String addressStr = cursor.getString(address);
                                final String bodyStr = cursor.getString(bodyPos);
                                title.setText(convName.isEmpty() ? addressStr : convName);
                                snippet.setText(bodyStr, searchString);

                                view.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        UIIntents.get().launchConversationActivity(context, convId, null, null, false);
                                    }
                                });
                            }

                            @Override
                            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                                LayoutInflater inflater = LayoutInflater.from(context);
                                View v = inflater.inflate(R.layout.search_item, parent, false);
                                return v;
                            }

                        });
                    };
                }.execute();

                // ListView seems to want to reject the setFocusable until such time
                // as the list is not empty.  Set it here and request focus.  Without
                // this the arrow keys (and trackball) fail to move the selection.
                listView.setFocusable(true);
                listView.setFocusableInTouchMode(true);
                listView.requestFocus();

                // Remember the query if there are actual results
                if (cursorCount > 0) {
                    //add by jordan
                    SearchRecentSuggestions recent = null;
                    //SearchRecentSuggestions recent = ((MmsApp)getApplication()).getRecentSuggestions();
                    if (recent != null) {
                        recent.saveRecentQuery(
                                searchString,
                                getString(R.string.search_history,
                                        cursorCount, searchString));
                    }
                } else {
                    /* SPRD: Modify this for search function. @{ */
                    mEmptyView.setVisibility(View.VISIBLE);
                    mEmptyView.setText(R.string.search_empty);
                    /* @} */
                }
            }
        };

        // don't pass a projection since the search uri ignores it
        mUri = Telephony.MmsSms.SEARCH_URI.buildUpon()
                    .appendQueryParameter("pattern", searchString).build();

        // kick off a query for the threads which match the search string
        // SPRD:Remove this for search function.
        // mQueryHandler.startQuery(0, null, uri, null, null, null, null);

        ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE
                | ActionBar.DISPLAY_HOME_AS_UP);
    }

    class SearchViewBean {
        String conversationId;
        long threadId;
        String snipText;
        String conversationName;
        int messageId;
    }

    private ArrayList<SearchViewBean> changeCursorToList(Cursor cursor, String searchString) {
        ArrayList<SearchViewBean> arrayList = new ArrayList<>();
        if (cursor != null) {
            DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
            while (cursor.moveToNext()) {
                SearchViewBean searchViewBean = new SearchViewBean();
                long conversationId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.PartColumns.CONVERSATION_ID));
                long threadId = BugleDatabaseOperations.getThreadId(dbWrapper, String.valueOf(conversationId));
                String textStr = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PartColumns.TEXT));
                String contentType = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PartColumns.CONTENT_TYPE));
                int messageId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.PartColumns.MESSAGE_ID));
                searchViewBean.conversationName = BugleDatabaseOperations.getExistingConversationName(dbWrapper, threadId);
                searchViewBean.threadId = threadId;
                searchViewBean.conversationId = String.valueOf(conversationId);
                searchViewBean.messageId = messageId;
                //deal chatbotMessage  when SearchKey is not Match skip this message
                if (ContentType.isChatbotCard(contentType)) {
                    String searchText = matchSearchString(textStr, searchString);
                    if (!TextUtils.isEmpty(searchText)) {
                        searchViewBean.snipText = searchText;
                    } else {
                        continue;
                    }
                } else {
                    searchViewBean.snipText = textStr;
                }
                if (searchViewBean.snipText.contains(searchString)) {
                    arrayList.add(searchViewBean);
                }
            }
            cursor.close();
        }
        return arrayList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    /**
     * SPRD: Add this method for search function. @{
     */
    @Override
    protected void onStart() {
      // TODO Auto-generated method stub
        super.onStart();
        System.out.println("jordan,enter onStart(), the mUri = " + mUri.toString());
        //mQueryHandler.startQuery(0, null, mUri, null, null, null, null);
        mAsyncSearchHandler.startSearch(mConversationId,mSearchStringParameter.trim());
    }

    /* @} */

    /*
     * SPRD: Bug#255558,the searched item is disappeared after edit it.
     *
     * @{
     */
    private ContentResolver mContentResolver;
    private final ContentObserver mChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfUpdate) {
            if (!selfUpdate && mUri != null && mQueryHandler != null) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                     //   mQueryHandler.startQuery(0, null, mUri, null, null, null, null);
                    }
                }, 1500);
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mContentResolver != null) {
            mContentResolver.unregisterContentObserver(mChangeObserver);
        }
    }

    private void registerConversationListChangeObserver() {
        if (mContentResolver != null) {
            mContentResolver.registerContentObserver(MmsSms.CONTENT_CONVERSATIONS_URI, true, mChangeObserver);
        }
    }

    /*
     * @}
     */
    public static byte[] getBytesUsingUtf8(String data) {
        try {
            return data.getBytes(CharacterSets.DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            // Impossible to reach here!
            return new byte[0];
        }
    }

    public static abstract class AsyncSearchHandler extends Handler {
        public abstract void onQueryComplete(Cursor v);

        private static final int EVENT_ARG_QUERY = 1;

        public void startSearch(final String conversationId, final String key) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Cursor cursor = startQuery(conversationId, key);
                    Message message = new Message();
                    message.what = EVENT_ARG_QUERY;
                    message.obj = cursor;
                    AsyncSearchHandler.this.sendMessage(message);
                }
            }).start();
        }

        private Cursor startQuery(String conversationId, String searchString) {
            final DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
            dbWrapper.beginTransaction();
            searchString = searchString.replace("\\","\\\\");
            searchString = searchString.replace("%", "\\%");
            searchString = searchString.replace("_", "\\_");
            searchString = searchString.replace("'", "\'\'");
            Cursor cursor = BugleDatabaseOperations.queryMessage(dbWrapper, conversationId, "");
            if (cursor != null) {
                dbWrapper.setTransactionSuccessful();
            }
            dbWrapper.endTransaction();
            return cursor;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_ARG_QUERY:
                    onQueryComplete((Cursor) msg.obj);
                    break;
            }
        }
    }
}
