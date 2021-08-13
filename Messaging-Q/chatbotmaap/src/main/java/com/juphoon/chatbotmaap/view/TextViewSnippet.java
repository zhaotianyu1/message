package com.juphoon.chatbotmaap.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatTextView;

import com.juphoon.chatbotmaap.R;

/**
 * 搜索字体高亮控件
 */

public class TextViewSnippet extends AppCompatTextView {
    private static final String TAG = TextViewSnippet.class.getSimpleName();
    private static final boolean DEBUG_FLAG = false;
    private static String sEllipsis = "\u2026";
    private String mFullText;
    private String mSearchString;
    private Context mContext;
    private int mHighlightColor;

    public TextViewSnippet(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mHighlightColor = context.getResources().getColor(R.color.primary_color);

    }

    public TextViewSnippet(Context context) {
        super(context);
        mContext = context;

        mHighlightColor = context.getResources().getColor(R.color.primary_color);

    }

    public TextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        mHighlightColor = context.getResources().getColor(R.color.primary_color);

    }

    /**
     * We have to know our width before we can compute the snippet string.  Do that
     * here and then defer to super for whatever work is normally done.
     */
    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG_FLAG) {
            Log.d(TAG, "onLayout --------------------------------");
        }

        if (mFullText == null) {
            if (DEBUG_FLAG) {
                Log.d(TAG, "mSearchString == null");
            }

            return;
        }

        String fullTextLower = mFullText.toLowerCase();
        String searchStringLower = mSearchString.toLowerCase();

        int startPos = 0;
        int searchStringLength = searchStringLower.length();
        int bodyLength = fullTextLower.length();

        startPos = fullTextLower.indexOf(searchStringLower);

        TextPaint tp = getPaint();
        float searchStringWidth = tp.measureText(mSearchString);
        float textFieldWidth = getWidth();
        String snippetString = null;

        if (searchStringWidth > textFieldWidth) {
            if (-1 == startPos) {
                return;
            }

            snippetString = mFullText.substring(startPos, startPos + searchStringLength);
        } else {
            if (DEBUG_FLAG) {
                Log.v(TAG, "searchStringWidth <= textFieldWidth");
            }

            float ellipsisWidth = tp.measureText(sEllipsis);
            textFieldWidth -= (1F * ellipsisWidth); // assume we'll need one on both ends

            int offset = -1;
            int start = -1;
            int end = -1;

            while (true) {
                offset += 1;

                int newstart = Math.max(0, startPos - offset);
                int newend = Math.min(bodyLength, startPos + searchStringLength + offset);

                if (newstart == start && newend == end) {
                    break;
                }

                start = newstart;
                end = newend;

                String candidate = mFullText.substring(start, end);
                snippetString = String.format("%s%s%s", start == 0 ? "" : sEllipsis, candidate,
                        end == bodyLength ? "" : sEllipsis);

                if (tp.measureText(candidate) > textFieldWidth) {
                    break;
                }
            }
        }

        String snippetStringLower = snippetString.toLowerCase();
        SpannableString spannable = new SpannableString(snippetString);
        int start = 0;
        while (true) {
            int index = snippetStringLower.indexOf(searchStringLower, start);

            if (index == -1) {
                break;
            }

            spannable.setSpan(new ForegroundColorSpan(mHighlightColor),
                    index, index + searchStringLength, 0);

            start = index + searchStringLength;
        }

        setText(spannable);

        if (DEBUG_FLAG) {
            Log.d(TAG, "onlayout end");
        }

        super.onLayout(changed, left, top, right, bottom);
    }

    public void setText(String fullText, String searchText) {
        if ((fullText == null) || (searchText == null)) {
            return;
        }
        mFullText = fullText;
        mSearchString = searchText;
        requestLayout();
    }
}
