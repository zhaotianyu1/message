package com.juphoon.helper.mms.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
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
import com.juphoon.helper.mms.LocationService;
import com.juphoon.helper.mms.RcsLocationHelper;
import com.juphoon.helper.mms.RcsLocationHelper.RcsLocationItem;
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsServiceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RcsBaiduLocationActivity extends AppCompatActivity {

    private final static String TAG = "RcsBaiduLocationActivity";

    public final static String LOCATION_FILE_PATH = "location_file_path";

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
    private RcsLocationItem mRcsLocationItem;
    private String mGeoXml;

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
            Log.d(TAG, "onReceiveLocation: " + location.getLocType());
            double latitude = location.getLatitude();    //获取纬度信息
            double longitude = location.getLongitude();    //获取经度信息
            Log.d(TAG, "latitude: " + latitude);
            Log.d(TAG, "longitude: " + longitude);
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius()).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);
                if (mFirstLocate) {
                    mFirstLocate = false;
                    if (mLocateMode) {
                        mLocationName = location.getAddrStr();
                        Log.d(TAG, "mLocationName: " + mLocationName);
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
            Log.d(TAG, "onMapStatusChangeFinish");
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
            Log.d(TAG, "onGetReverseGeoCodeResult" + result.error);

            if (result != null && result.error == SearchResult.ERRORNO.NO_ERROR) {
                mLocationName = result.getAddress();
                if (mLocateMode) {
                    updateTip(true);
                } else {
                    mRcsLocationItem.mAddress = mLocationName;
                    mTextViewShowLocation.setText(mLocationName);
                    RcsCallWrapper.rcsSetGeoToFile("", mRcsLocationItem.mLatitude, mRcsLocationItem.mLongitude, mRcsLocationItem.mRadius, mRcsLocationItem.mAddress, mGeoXml);
                }
            } else {
                Log.d(TAG, "geosearch error");
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
        Log.i("mmm","打开地图");
        BugleApplication.getInstance().setIsmap(true);
        init();
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
        mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        mOption.setCoorType("bd09ll");
        mOption.setScanSpan(1000);
        mOption.setIsNeedAddress(true);
        mOption.setOpenGps(true);

        mLocationService.setLocationOption(mOption);
        mLocationService.registerListener(listener);
        mLocationService.start();

        mGeoXml = getIntent().getStringExtra(LOCATION_FILE_PATH);
        if (TextUtils.isEmpty(mGeoXml)) {
            mLocateMode = true;
            mTextViewShowLocation.setVisibility(View.GONE);
            mGeoSearch = GeoCoder.newInstance();
            mGeoSearch.setOnGetGeoCodeResultListener(mOnGetGeoCoderResultListener);
            mBaiduMap.setOnMapStatusChangeListener(mOnMapStatusChangeListener);
        } else {
            mLocateMode = false;
            findViewById(R.id.imageview_mark).setVisibility(View.INVISIBLE);
            mRcsLocationItem = RcsLocationHelper.parseLocationJson(RcsMmsUtils.geoFileToString(mGeoXml));
            mLocationName = mRcsLocationItem.mAddress;
            LatLng location = new LatLng(mRcsLocationItem.mLatitude, mRcsLocationItem.mLongitude);
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(location);
            mBaiduMap.animateMapStatus(u);
            updateOverlay(location);
            mTextViewShowLocation.setText(mRcsLocationItem.mAddress);
            if (TextUtils.isEmpty(mRcsLocationItem.mAddress)) {
                mGeoSearch = GeoCoder.newInstance();
                mGeoSearch.setOnGetGeoCodeResultListener(mOnGetGeoCoderResultListener);
                searchAddress(location);
            }
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().hide();
//
//                Runnable run2 = new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    View rootview = RcsBaiduLocationActivity.this.getWindow().getDecorView();
//                    View aaa = rootview.findFocus();
//                    Log.i("xxx", aaa.getId()+"");
//                    Log.i("xxx","id = 0x"+Integer.toHexString(aaa.getId()));
//                    Log.i("xxx", aaa.toString());
//                }
//            }
//        };
//        new Thread(run2).start();
    }

    private void init() {

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(RcsBaiduLocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(RcsBaiduLocationActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(RcsBaiduLocationActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(RcsBaiduLocationActivity.this, permissions, 1);
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
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
            MenuItem item = menu.add(Menu.NONE, MENU_SEND, 0, R.string.send);
            item.setEnabled(mTextViewTip.getVisibility() == View.VISIBLE).setIcon(R.drawable.chatbot_send).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SEND:
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
            Log.d(TAG, "searchAddress fail");
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button_my_position:
                if (!mFirstLocate) {
                    MyLocationData data = mBaiduMap.getLocationData();
                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(new LatLng(data.latitude, data.longitude));
                    mBaiduMap.animateMapStatus(u);

                }
                break;
            case R.id.send_my_position:
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
                break;
            default:
                break;
        }
    }

}
