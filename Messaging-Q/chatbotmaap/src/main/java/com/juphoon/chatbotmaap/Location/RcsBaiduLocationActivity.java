package com.juphoon.chatbotmaap.Location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.juphoon.chatbotmaap.R;
import com.juphoon.chatbotmaap.RcsChatbotDefine;
import com.juphoon.chatbotmaap.RcsChatbotUtils;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsServiceManager;
import com.juphoon.service.RcsJsonParamConstants;
import com.juphoon.service.RmsDefine;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;

public class RcsBaiduLocationActivity extends AppCompatActivity {

    private final String TAG = "RcsBaiduLocationActivity";

    public final static String LOCATION_FILE_PATH = "location_file_path";
    public final static String LOCATION_LATITUDE = "location_latutide";
    public final static String LOCATION_LONGITUDE = "location_longitude";
    public final static String CONVERSATIONID = "conversationId";

    private final static int MENU_SEND = 1;

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationService mLocationService;
    private String mLocationName;
    private GeoCoder mGeoSearch;
    private boolean mFirstLocate;
    private TextView mTextViewTip;
    private TextView mTextViewShowLocation;
    private boolean mLocateMode; // 区分是定位模式还是显示模式
    private final BitmapDescriptor mBitmapLocate = BitmapDescriptorFactory.fromResource(R.drawable.icon_location_l);

    public static void startWithLocation(Context context, Uri xmlUri) {
        String path = RcsFileUtils.getFilePathByUri(context, xmlUri);
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            Intent intent = new Intent(context, RcsBaiduLocationActivity.class);
            intent.putExtra(LOCATION_FILE_PATH, path);
            context.startActivity(intent);
        }
    }

    /***
     * 定位结果回调，在此方法中处理定位结果
     */
    private final BDLocationListener listener = new BDLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius()).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);
                if (mFirstLocate) {
                    mFirstLocate = false;
                    if (mLocateMode) {
                        mLocationName = location.getAddrStr();
                        updateTip(true);
                        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
                        mBaiduMap.animateMapStatus(u);
                    }
                }
            }
        }
    };

    private final OnMapClickListener mOnMapClickListener = new OnMapClickListener() {

        @Override
        public boolean onMapPoiClick(MapPoi location) {
            return false;
        }

        @Override
        public void onMapClick(LatLng location) {
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(location);
            mBaiduMap.animateMapStatus(u);
        }
    };

    // 显示模式不会触发
    private final OnMapStatusChangeListener mOnMapStatusChangeListener = new OnMapStatusChangeListener() {

        @Override
        public void onMapStatusChangeStart(MapStatus status) {
            if (!mLocateMode) {
                updateTip(false);
            }
        }

        @Override
        public void onMapStatusChangeFinish(MapStatus status) {
            searchAddress(status.target);
        }

        @Override
        public void onMapStatusChange(MapStatus status) {
        }
    };

    // 显示模式不会触发
    private final OnGetGeoCoderResultListener mOnGetGeoCoderResultListener = new OnGetGeoCoderResultListener() {

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
            if (result != null && result.error == SearchResult.ERRORNO.NO_ERROR) {
                mLocationName = result.getAddress();
                updateTip(true);
            } else {
            }
        }

        @Override
        public void onGetGeoCodeResult(GeoCodeResult arg0) {

        }
    };

    ImageView send_my_position ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.baidu_location_activity);

        mFirstLocate = true;
        mTextViewTip = (TextView) findViewById(R.id.textview_tip);
        mTextViewShowLocation = (TextView) findViewById(R.id.textview_location_info);
        mMapView = (MapView) findViewById(R.id.baidu_map_view);

        send_my_position = findViewById(R.id.send_my_position);
        send_my_position.setFocusable(true);
        send_my_position.setFocusableInTouchMode(true);
        send_my_position.requestFocus();

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15));
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setOnMapClickListener(mOnMapClickListener);
        mLocationService = new LocationService(this);
        LocationClientOption mOption = mLocationService.getDefaultLocationClientOption();
        mOption.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        mOption.setCoorType("bd09ll");
        mLocationService.setLocationOption(mOption);
        mLocationService.registerListener(listener);
        mLocationService.start();
        LatLng latLng = null;
        if (getIntent().getDoubleExtra(LOCATION_LONGITUDE, 0) != 0) {
            latLng = new LatLng(getIntent().getDoubleExtra(LOCATION_LATITUDE, 0), getIntent().getDoubleExtra(LOCATION_LONGITUDE, 0));
        }
        String xml = getIntent().getStringExtra(LOCATION_FILE_PATH);
        if (TextUtils.isEmpty(xml) && latLng == null) {
            mLocateMode = true;
            mTextViewShowLocation.setVisibility(View.GONE);
            mGeoSearch = GeoCoder.newInstance();
            mGeoSearch.setOnGetGeoCodeResultListener(mOnGetGeoCoderResultListener);
            mBaiduMap.setOnMapStatusChangeListener(mOnMapStatusChangeListener);
        } else if (latLng != null ) {
            mLocateMode = true;
            mLocateMode = false;
            findViewById(R.id.imageview_mark).setVisibility(View.INVISIBLE);
            LatLng location = new LatLng(latLng.latitude, latLng.longitude);
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(location);
            mBaiduMap.animateMapStatus(u);
            updateOverlay(location);
            mBaiduMap.getLocationData();
            mGeoSearch = GeoCoder.newInstance();
            mGeoSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                @Override
                public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                }

                @Override
                public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                    mTextViewShowLocation.setText(reverseGeoCodeResult.getAddress());
                }
            });
            mGeoSearch.reverseGeoCode(new ReverseGeoCodeOption()
                    .location(latLng));
        }
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().hide();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationService.unregisterListener(listener);
        mLocationService.stop();
        mMapView.onDestroy();
        mBitmapLocate.recycle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mLocateMode) {
            MenuItem item = menu.add(Menu.NONE, MENU_SEND, 0, "Send");
            item.setEnabled(mTextViewTip.getVisibility() == View.VISIBLE).setIcon(R.drawable.chatbot_send).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SEND:
            LatLng location = mBaiduMap.getMapStatus().target;
            String json = RcsLocationHelper.genLocationJson(location.latitude, location.longitude, 40.0f,
                    mLocationName);
            if (!TextUtils.isEmpty(json)) {
                Intent intent = new Intent();
                intent.putExtra(RcsChatbotDefine.KEY_FILEPATH, RcsFileUtils.saveGeoToFile(RcsServiceManager.getUserName(), json));
                intent.putExtra(RcsChatbotDefine.KEY_CONVERSATIONID, getIntent().getStringExtra(CONVERSATIONID));
                intent.setComponent(new ComponentName(RcsChatbotUtils.getMessageingPkg(), RcsChatbotUtils.getRcsChatBotHelperReceiverName()));
                intent.setAction(RcsChatbotDefine.ACTION_GEO);
                sendBroadcast(intent);
            }
            finish();
            break;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return true;
    }

    private void updateTip(boolean visiable) {
        if (visiable) {
            mTextViewTip.setText(mLocationName);
            mTextViewTip.setVisibility(View.VISIBLE);
        } else {
            mTextViewTip.setVisibility(View.INVISIBLE);
        }
        invalidateOptionsMenu();
    }

    private void updateOverlay(LatLng location) {
        MarkerOptions options = new MarkerOptions().anchor(23.0f, 0.0f).position(location).icon(mBitmapLocate);
        mBaiduMap.addOverlay(options);
    }

    private void searchAddress(LatLng location) {
        if (!mGeoSearch.reverseGeoCode(new ReverseGeoCodeOption().location(location))) {
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_my_position) {
            if (!mFirstLocate) {
                MyLocationData data = mBaiduMap.getLocationData();
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(new LatLng(data.latitude, data.longitude));
                mBaiduMap.animateMapStatus(u);
            }

        } else if(id == R.id.send_my_position) {
            LatLng location = mBaiduMap.getMapStatus().target;
            Log.d(TAG, "send lat:" + location.latitude + " long:" + location.longitude);
            String json = RcsLocationHelper.genLocationJson(location.latitude, location.longitude, 40.0f,
                    mLocationName);
            if (!TextUtils.isEmpty(json)) {
                Intent intent = new Intent();
                intent.putExtra(LOCATION_FILE_PATH, RcsFileUtils.saveGeoToFile(RcsServiceManager.getUserName(), json));
                setResult(RESULT_OK, intent);
            }
            finish();
        }else{
        }
    }

    public static String geoFileToString(String filePath) {
        String content = "";
        if (!TextUtils.isEmpty(filePath)) {
            content = readGeo(filePath);
            if (content != null) {
                //防止sdk没有start,xml改成由上层解析
                String geo = loadGeoXml(content);
                if (!TextUtils.isEmpty(geo)) {
                    String[] params = geo.split(";");
                    if (params.length >= 4) {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put(RcsJsonParamConstants.RCS_JSON_GS_LATITUDE, params[0]);
                            obj.put(RcsJsonParamConstants.RCS_JSON_GS_LONGTUDE, params[1]);
                            obj.put(RcsJsonParamConstants.RCS_JSON_GS_RADIUS, params[2]);
                            obj.put(RcsJsonParamConstants.RCS_JSON_GS_LOCATION_NAME, params[3]);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        content = obj.toString();
                    }
                }
            } else {
                Log.d("RcsBaiduLocation", "geo restore no content");
            }
        }
        return content;
    }

    private static String readGeo(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = fis.read(buf)) != -1) {
                stream.write(buf, 0, len);
            }
            fis.close();
            stream.close();
            return stream.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析地理位置xml
     * @param xmlMsg
     * @return
     */
    public static String loadGeoXml(String xmlMsg) {
        try {
            String label = null;
            String pos = null;
            String radius = null;
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xmlMsg));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("rcspushlocation")) {
                        label = parser.getAttributeValue(null, "label");
                    } else if (name.equals("pos")) {
                        pos = parser.nextText();
                    } else if (name.equals("radius")){
                        radius = parser.nextText();
                    }
                }
                eventType = parser.next();
            }
            if (!TextUtils.isEmpty(pos) && pos.split(" ").length == 2) {
                String[] position = pos.split(" ");
                StringBuilder builder = new StringBuilder();
                builder.append(position[0])
                        .append(";")
                        .append(position[1])
                        .append(";").append(TextUtils.isEmpty(radius) ? "0.0" : radius)
                        .append(";").append(label);
                return builder.toString();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
