/**
 * 发送定位，每条消息显示的布局
 */
package com.android.messaging.ui;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.service.RcsJsonParamConstants;

import org.json.JSONException;
import org.json.JSONObject;

//juphoon
public class GeoAttachmentView extends FrameLayout {

    private TextView mTextViewAddress;
    private Context mContext;
    private Uri mGeoXml;

    public GeoAttachmentView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.geo_attachment_view, this, true);
        setContentDescription(context.getString(R.string.geo_attachment_content_description));
    }

    private void initView() {
        mTextViewAddress = (TextView) findViewById(R.id.geoattachmentview_adr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    public void bindMessagePartData(MessagePartData messagePartData, boolean showLocation) {
        Assert.isTrue(messagePartData == null || ContentType.isGeoType(messagePartData.getContentType()));
        mGeoXml = messagePartData.getContentUri();
        String geoJson = RcsMmsUtils.geoFileToString(RcsFileUtils.getFilePathByUri(mContext, mGeoXml));
        if (!TextUtils.isEmpty(geoJson)) {
            if (showLocation) {
                try {
                    JSONObject obj = new JSONObject(geoJson);
                    String locationName = obj.optString(RcsJsonParamConstants.RCS_JSON_GS_LOCATION_NAME);
                    if (TextUtils.isEmpty(locationName)) {
                        mTextViewAddress.setText(R.string.click_view_address);
                    } else {
                        mTextViewAddress.setText(locationName);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            mTextViewAddress.setText("");
        }
    }
}
