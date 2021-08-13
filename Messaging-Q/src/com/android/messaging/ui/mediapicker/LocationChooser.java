package com.android.messaging.ui.mediapicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.messaging.BugleApplication;
import com.android.messaging.R;
import com.android.messaging.tcl.ui.fragment.ActConversationListFragment;
import com.android.messaging.ui.conversation.ConversationActivity;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
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
import com.juphoon.helper.mms.RcsMmsUtils;
import com.juphoon.helper.mms.ui.RcsBaiduLocationActivity;
import com.juphoon.rcs.tool.RcsCallWrapper;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.rcs.tool.RcsServiceManager;

import java.util.ArrayList;
import java.util.List;

public class LocationChooser extends MediaChooser {

    private ImageView button_my_position;
    private ImageView send_my_location;
    private final static String TAG = "LocationChooser";
    private Context mContext;
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
    private RcsLocationHelper.RcsLocationItem mRcsLocationItem;
    private String mGeoXml;
    public final static String LOCATION_FILE_PATH = "location_file_path";


    /**
     * Initializes a new instance of the Chooser class
     *
     * @param mediaPicker The media picker that the chooser is hosted in
     */
    LocationChooser(MediaPicker mediaPicker,Context context) {
        super(mediaPicker);
        this.mContext = context;
    }

    @Override
    public int getSupportedMediaTypes() {
        return MediaPicker.MEDIA_TYPE_LOCATION;
    }

    @Override
    public  int getIconResource() {
//        return R.drawable.ic_location;
        return R.drawable.act_local;
    }
    @Override
    int getIconFoucusResource() {
        return R.drawable.voice_focus;
    }
    @Override
    int getIconDescriptionResource() {
        return R.string.mediapicker_locations;
    }

    @Override
    int getActionBarTitleResId() {
        return R.string.mediapicker_locations;
    }

    @Override
    protected View createView(ViewGroup container) {
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(
                R.layout.act_baidu_location,
                container /* root */,
                false /* attachToRoot */);
        final LocationView conversationListFragment =
                (LocationView) getFragmentManager().findFragmentById(
                        R.id.location_fragment);

//        Log.i("mmm","打开地图");
//        init();
//        mFirstLocate = true;
//        send_my_location = view.findViewById(R.id.send_my_location);
//        button_my_position = view.findViewById(R.id.button_my_position);
//        mTextViewTip = (TextView) view.findViewById(R.id.textview_tip);
//        mTextViewShowLocation = (TextView) view.findViewById(R.id.textview_location_info);
//        mMapView = (MapView)view.findViewById(R.id.baidu_map_view);
//        mBaiduMap = mMapView.getMap();
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
//        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15));
//        mBaiduMap.setMyLocationEnabled(true);
//        mBaiduMap.setOnMapClickListener(mOnMapClickListener);
//        mLocationService = new LocationService(mContext);
//        LocationClientOption mOption = mLocationService.getDefaultLocationClientOption();
//        mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
//        mOption.setCoorType("bd09ll");
//        mOption.setScanSpan(1000);
//        mOption.setIsNeedAddress(true);
//        mOption.setOpenGps(true);
//
//        mLocationService.setLocationOption(mOption);
//        mLocationService.registerListener(listener);
//        mLocationService.start();
//
//        //mGeoXml = getIntent().getStringExtra(LOCATION_FILE_PATH);
//        if (TextUtils.isEmpty(mGeoXml)) {
//            mLocateMode = true;
//            mTextViewShowLocation.setVisibility(View.GONE);
//            mGeoSearch = GeoCoder.newInstance();
//            mGeoSearch.setOnGetGeoCodeResultListener(mOnGetGeoCoderResultListener);
//            mBaiduMap.setOnMapStatusChangeListener(mOnMapStatusChangeListener);
//        } else {
//            mLocateMode = false;
//            view.findViewById(R.id.imageview_mark).setVisibility(View.INVISIBLE);
//            mRcsLocationItem = RcsLocationHelper.parseLocationJson(RcsMmsUtils.geoFileToString(mGeoXml));
//            mLocationName = mRcsLocationItem.mAddress;
//            LatLng location = new LatLng(mRcsLocationItem.mLatitude, mRcsLocationItem.mLongitude);
//            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(location);
//            mBaiduMap.animateMapStatus(u);
//            updateOverlay(location);
//            mTextViewShowLocation.setText(mRcsLocationItem.mAddress);
//            if (TextUtils.isEmpty(mRcsLocationItem.mAddress)) {
//                mGeoSearch = GeoCoder.newInstance();
//                mGeoSearch.setOnGetGeoCodeResultListener(mOnGetGeoCoderResultListener);
//                searchAddress(location);
//            }
//        }
//        button_my_position.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!mFirstLocate) {
//                    MyLocationData data = mBaiduMap.getLocationData();
//                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(new LatLng(data.latitude, data.longitude));
//                    mBaiduMap.animateMapStatus(u);
//                }
//            }
//        });
//        send_my_location.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                LatLng location = mBaiduMap.getMapStatus().target;
//                Log.d(TAG, "send lat:" + location.latitude + " long:" + location.longitude);
//                String json = RcsLocationHelper.genLocationJson(location.latitude, location.longitude, 40.0f,
//                        mLocationName);
//                if (!TextUtils.isEmpty(json)) {
//                    Intent intent = new Intent();
//                    intent.putExtra(LOCATION_FILE_PATH, RcsFileUtils.saveGeoToFile(RcsServiceManager.getUserName(), json));
//
//                  // setResult(RESULT_OK, intent);
//
//                }
//
//            }
//        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        return view;
    }
    private final BaiduMap.OnMapClickListener mOnMapClickListener = new BaiduMap.OnMapClickListener() {

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
    private void updateOverlay(LatLng location) {
        MarkerOptions options = new MarkerOptions().anchor(23.0f, 0.0f).position(location).icon(mBitmapLocate);
        mBaiduMap.addOverlay(options);
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

    private void updateTip(boolean visiable) {
        if (visiable) {
            mTextViewTip.setText(mLocationName);
            mTextViewTip.setVisibility(View.VISIBLE);
        } else {
            mTextViewTip.setVisibility(View.INVISIBLE);
        }
        //invalidateOptionsMenu();
    }
    // 显示模式不会触发
    private final BaiduMap.OnMapStatusChangeListener mOnMapStatusChangeListener = new BaiduMap.OnMapStatusChangeListener() {

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

    private void searchAddress(LatLng location) {
        if (!mGeoSearch.reverseGeoCode(new ReverseGeoCodeOption().location(location))) {
            Log.d(TAG, "searchAddress fail");
        }
    }
    private void init() {

//        List<String> permissionList = new ArrayList<>();
//        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
//        }
//        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//            permissionList.add(Manifest.permission.READ_PHONE_STATE);
//        }
//        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        }
//        if (!permissionList.isEmpty()) {
//            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
//            ActivityCompat.requestPermissions(, permissions, 1);
//        }
//        ActivityCompat.requestPermissions(BugleApplication.getContext(),
//                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                1);
    }
}
