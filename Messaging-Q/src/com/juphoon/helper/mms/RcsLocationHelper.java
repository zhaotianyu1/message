package com.juphoon.helper.mms;

import com.juphoon.service.RcsJsonParamConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class RcsLocationHelper {

    public static class RcsLocationItem {
        public double mLatitude;
        public double mLongitude;
        public float mRadius;
        public String mAddress;
    }

    public static RcsLocationItem parseLocationJson(String json) {
        try {
            RcsLocationItem item = new RcsLocationItem();
            JSONObject obj = new JSONObject(json);
            item.mLatitude = Double.valueOf(obj.optString(RcsJsonParamConstants.RCS_JSON_GS_LATITUDE));
            item.mLongitude = Double.valueOf(obj.optString(RcsJsonParamConstants.RCS_JSON_GS_LONGTUDE));
            item.mRadius = Float.valueOf(obj.optString(RcsJsonParamConstants.RCS_JSON_GS_RADIUS));
            item.mAddress = obj.optString(RcsJsonParamConstants.RCS_JSON_GS_LOCATION_NAME);
            return item;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String genLocationJson(double latitude, double longitude, float radius, String address) {
        try {
            String json = new JSONStringer().object()
                    .key(RcsJsonParamConstants.RCS_JSON_GS_LATITUDE).value(String.valueOf(latitude))
                    .key(RcsJsonParamConstants.RCS_JSON_GS_LONGTUDE).value(String.valueOf(longitude))
                    .key(RcsJsonParamConstants.RCS_JSON_GS_RADIUS).value(String.valueOf(radius))
                    .key(RcsJsonParamConstants.RCS_JSON_GS_LOCATION_NAME).value(address)
                    .endObject()
                    .toString();
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
