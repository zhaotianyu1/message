/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.ui;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.ui.conversation.ConversationMessageAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy of CursorAdapter suited for RecyclerView.
 *
 * TODO: BUG 16327984. Replace this with a framework supported CursorAdapter for
 * RecyclerView when one is available.
 */
public abstract class CursorRecyclerAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected boolean mDataValid;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected boolean mAutoRequery;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected Cursor mCursor;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected Context mContext;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected int mRowIDColumn;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected ChangeObserver mChangeObserver;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected DataSetObserver mDataSetObserver;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected FilterQueryProvider mFilterQueryProvider;

    /**
     * If set the adapter will call requery() on the cursor whenever a content change
     * notification is delivered. Implies {@link #FLAG_REGISTER_CONTENT_OBSERVER}.
     *
     * @deprecated This option is discouraged, as it results in Cursor queries
     * being performed on the application's UI thread and thus can cause poor
     * responsiveness or even Application Not Responding errors.  As an alternative,
     * use {@link android.app.LoaderManager} with a {@link android.content.CursorLoader}.
     */
    @Deprecated
    public static final int FLAG_AUTO_REQUERY = 0x01;

    /**
     * If set the adapter will register a content observer on the cursor and will call
     * {@link #onContentChanged()} when a notification comes in.  Be careful when
     * using this flag: you will need to unset the current Cursor from the adapter
     * to avoid leaks due to its registered observers.  This flag is not needed
     * when using a CursorAdapter with a
     * {@link android.content.CursorLoader}.
     */
    public static final int FLAG_REGISTER_CONTENT_OBSERVER = 0x02;

    /**
     * Recommended constructor.
     *
     * @param c The cursor from which to get the data.
     * @param context The context
     * @param flags Flags used to determine the behavior of the adapter; may
     * be any combination of {@link #FLAG_AUTO_REQUERY} and
     * {@link #FLAG_REGISTER_CONTENT_OBSERVER}.
     */
    public CursorRecyclerAdapter(final Context context, final Cursor c, final int flags) {
        init(context, c, flags);
    }

    void init(final Context context, final Cursor c, int flags) {
        if ((flags & FLAG_AUTO_REQUERY) == FLAG_AUTO_REQUERY) {
            flags |= FLAG_REGISTER_CONTENT_OBSERVER;
            mAutoRequery = true;
        } else {
            mAutoRequery = false;
        }
        final boolean cursorPresent = c != null;
        mCursor = c;
        mDataValid = cursorPresent;
        mContext = context;
        mRowIDColumn = cursorPresent ? c.getColumnIndexOrThrow("_id") : -1;
        if ((flags & FLAG_REGISTER_CONTENT_OBSERVER) == FLAG_REGISTER_CONTENT_OBSERVER) {
            mChangeObserver = new ChangeObserver();
            mDataSetObserver = new MyDataSetObserver();
        } else {
            mChangeObserver = null;
            mDataSetObserver = null;
        }

        if (cursorPresent) {
            if (mChangeObserver != null) {
                c.registerContentObserver(mChangeObserver);
            }
            if (mDataSetObserver != null) {
                c.registerDataSetObserver(mDataSetObserver);
            }
        }
    }

    /**
     * Returns the cursor.
     * @return the cursor.
     */
    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            Log.i("zxc","getItemCount--:start");
            if(conversationListItemDataList.size()>0){
                Log.i("zxc","getItemCount--:123");
                return mCursor.getCount();
            }
            return mCursor.getCount()+1;
        } else {
            return 0;
        }
    }

    /**
     * @see
     */
    public Object getItem(final int position) {
        if (mDataValid && mCursor != null) {
            mCursor.moveToPosition(position);
            return mCursor;
        } else {
            return null;
        }
    }

    /**
     * @see androidx.recyclerview.widget.RecyclerView.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(final int position) {
        if (mDataValid && mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getLong(mRowIDColumn);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public VH onCreateViewHolder(final ViewGroup parent, final int viewType) {

        return createViewHolder(mContext, parent, viewType);
    }

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
         //   throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        if(conversationListItemDataList.size()>0){
            bindViewHolder_list(holder, mContext, mCursor,position,conversationListItemDataList);
            return;
        }
//        else if(conversationMessageDataList.size()>0){
//            Log.i("zxc","position--:"+position);
//            bindViewHolder_message(holder, mContext, mCursor,position,conversationMessageDataList);
//            return;
//        }


        bindViewHolder(holder, mContext, mCursor, position);

    }

    List<ConversationListItemData> conversationListItemDataList =new ArrayList<>();
    /**
     * Bind an existing view to the data pointed to by cursor
     * @param context Interface to application's global information
     * @param cursor The cursor from which to get the data. The cursor is already
     * moved to the correct position.
     */
    public abstract void bindViewHolder(VH holder, Context context, Cursor cursor,int position);
    public abstract void bindViewHolder_message(VH holder, Context context, Cursor cursor,int position,List<ConversationMessageData> conversationListItemDataList);
    public abstract void bindViewHolder_list(VH holder, Context context, Cursor cursor,int position, List<ConversationListItemData> conversationListItemDataList);
    /**
     * @se
     */
    public abstract VH createViewHolder(Context context, ViewGroup parent, int viewType);

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    public void changeCursor(final Cursor cursor) {
        final Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    ConversationListItemData conversationListItemData;
    ConversationMessageData mData;
    List<ConversationMessageData> conversationMessageDataList =new ArrayList<>();
    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    public Cursor swapCursor(final Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null) {
            if (mChangeObserver != null) {
                oldCursor.unregisterContentObserver(mChangeObserver);
            }
            if (mDataSetObserver != null) {
                oldCursor.unregisterDataSetObserver(mDataSetObserver);
            }
        }
        mCursor = newCursor;
        if (newCursor != null) {
            if (mChangeObserver != null) {
                newCursor.registerContentObserver(mChangeObserver);
            }
            if (mDataSetObserver != null) {
                newCursor.registerDataSetObserver(mDataSetObserver);
            }
            mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
            // notify the observers about the new cursor
            notifyDataSetChanged();
        } else {
            mRowIDColumn = -1;
            mDataValid = false;
            // notify the observers about the lack of a data set
            notifyDataSetChanged();
        }
        Log.i("ooo","oldCursor");
//        if(mCursor.moveToFirst() && mCursor != null){
//            Log.i("ooo","cursor.name----:"+mCursor.getString(mCursor.getColumnIndex("name")));
//            Log.i("ooo","mCursor--conversionid:"+mCursor.getString(mCursor.getColumnIndex("_id")));
//            Log.i("ooo","cursor.SNIPPET_TEXT----:"+mCursor.getString(mCursor.getColumnIndex("snippet_text")));
//
//        }
        if(mCursor != null) {
            int i = 0;
            BugleApplication.getInstance().setCursorList(mCursor);
            while (mCursor.moveToNext()) {
                conversationListItemData = new ConversationListItemData();
                conversationListItemData.bind(mCursor);
                conversationListItemDataList.add(i, conversationListItemData);
                Log.i("ccc", "conversationListItemData.get(i).getName():" + conversationListItemDataList.get(i).getName());
                ++i;
            }
        }else{
            Log.i("ccc", "mCursor is null");
        }
        return oldCursor;
    }


    public Cursor swapCursors(final Cursor newCursor) {
        Log.i("zxc", "swapCursors---start");
        if (newCursor == mCursor) {
            Log.i("zxc", "swapCursors---start1");
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null) {
            Log.i("zxc", "swapCursors---start2");
            if (mChangeObserver != null) {
                oldCursor.unregisterContentObserver(mChangeObserver);
            }
            if (mDataSetObserver != null) {
                Log.i("zxc", "swapCursors---start3");
                oldCursor.unregisterDataSetObserver(mDataSetObserver);
            }
        }
        Log.i("zxc", "swapCursors---start4");
        mCursor = newCursor;
        if(mCursor != null) {
            Log.i("zxc", "mCursor:" + mCursor.getCount());
            BugleApplication.getInstance().setCursor(mCursor);
            Log.i("zxc", "mCursor12321---:" + BugleApplication.getInstance().getCursor().getCount());
            mCursor.moveToFirst();
            int i = 0;
            while (mCursor.moveToNext()) {
                Log.i("zxc", "循环");
                mData = new ConversationMessageData();
                mData.bind(mCursor);
                conversationMessageDataList.add(i, mData);
                Log.i("zxc", "conversationListItemData.get(i).getName():" + conversationMessageDataList.get(i).getText());
                ++i;
            }
            Log.i("zxc", "conversationMessageDataList.size()" + conversationMessageDataList.size());
        }else{
            Log.i("zxc", "mCursor is null");
        }
        if (newCursor != null) {
            Log.i("zxc", "swapCursors---start5");
            if (mChangeObserver != null) {
                newCursor.registerContentObserver(mChangeObserver);
            }
            if (mDataSetObserver != null) {
                newCursor.registerDataSetObserver(mDataSetObserver);
            }
            mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
            // notify the observers about the new cursor
            notifyDataSetChanged();
        } else {
            Log.i("zxc", "swapCursors---start6");
            mRowIDColumn = -1;
            mDataValid = false;
            // notify the observers about the lack of a data set
            notifyDataSetChanged();
        }

        return oldCursor;
    }
    /**
     * <p>Converts the cursor into a CharSequence. Subclasses should override this
     * method to convert their results. The default implementation returns an
     * empty String for null values or the default String representation of
     * the value.</p>
     *
     * @param cursor the cursor to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    public CharSequence convertToString(final Cursor cursor) {
        return cursor == null ? "" : cursor.toString();
    }

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     *
     * @see ContentObserver#onChange(boolean)
     */
    protected void onContentChanged() {
        if (mAutoRequery && mCursor != null && !mCursor.isClosed()) {
            if (false) {
                Log.v("Cursor", "Auto requerying " + mCursor + " due to update");
            }
            mDataValid = mCursor.requery();
        }
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(final boolean selfChange) {
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            mDataValid = false;
            notifyDataSetChanged();
        }
    }

}
