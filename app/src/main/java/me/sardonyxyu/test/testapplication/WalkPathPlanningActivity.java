package me.sardonyxyu.test.testapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import java.util.ArrayList;
import java.util.List;
import me.sardonyxyu.test.testapplication.util.AMapUtil;
import me.sardonyxyu.test.testapplication.util.DrivingRouteOverlay;
import me.sardonyxyu.test.testapplication.util.SensorEventHelper;

public class WalkPathPlanningActivity extends Activity implements View.OnClickListener ,AMapLocationListener ,LocationSource{
    private Context ctx;
    private MapView mapView;
    private AMap aMap;
    private Button btn_navigation, btn_path;
    /**
     * 选择终点Aciton标志位
     */
    private NaviLatLng endNaviLatlng = new NaviLatLng(39.925041, 116.437901);
    private LatLonPoint endLatLon;
    private List<NaviLatLng> startList = new ArrayList<NaviLatLng>();
    /**
     * 地图对象
     */
    private Marker mStartMarker;
    private Marker mEndMarker;
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;

    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private boolean mFirstFix = false;
    private Marker mLocMarker;
    private SensorEventHelper mSensorHelper;
    private Circle mCircle;
    public static final String LOCATION_MARKER_FLAG = "mylocation";
    private LatLng locationLatLng;
    private UiSettings mUiSettings;//定义一个UiSettings对象
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_planning);
        ctx = this;
        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {
        btn_navigation = (Button) findViewById(R.id.btn_navigation);
        btn_navigation.setOnClickListener(this);
        btn_path = (Button) findViewById(R.id.btn_path);
        btn_path.setOnClickListener(this);


        mapView = (MapView) WalkPathPlanningActivity.this.findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);
        //初始化地图控制器对象

        aMap = mapView.getMap();
        setUpMap();
        mSensorHelper = new SensorEventHelper(this);
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
        setTerminal();
        mapRoute();
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        mUiSettings = aMap.getUiSettings();//实例化UiSettings类对象
        mUiSettings.setScaleControlsEnabled(true);//控制比例尺控件是否显示
    }

    /**
     * 选取终点
     */
    private void setTerminal(){
        //选取终点，路线规划
        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //控制选起点
                endNaviLatlng = new NaviLatLng(latLng.latitude, latLng.longitude);
                endLatLon = new LatLonPoint(latLng.latitude, latLng.longitude);
                mEndMarker.setPosition(latLng);
                endList.clear();
                endList.add(endNaviLatlng);
            }
        });
        // 初始化Marker添加到地图
        mStartMarker = aMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.start))));
        mEndMarker = aMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.end))));

    }

    /**
     * 规划路线
     */
    private void mapRoute(){
        //搜索路线规划
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
                aMap.clear();// 清理地图上的所有覆盖物
                if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                    if (result != null && result.getPaths() != null) {
                        if (result.getPaths().size() > 0) {
                            mDriveRouteResult = result;
                            final DrivePath drivePath = mDriveRouteResult.getPaths()
                                    .get(0);
                            DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                                    ctx, aMap, drivePath,
                                    mDriveRouteResult.getStartPos(),
                                    mDriveRouteResult.getTargetPos(), null);

                            drivingRouteOverlay.setNodeIconVisibility(false);// 设置节点marker是否显示
                            drivingRouteOverlay.setIsColorfulline(false);// 是否用颜色展示交通拥堵情况，默认true
                            drivingRouteOverlay.removeFromMap();
                            drivingRouteOverlay.addToMap();
                            drivingRouteOverlay.zoomToSpan();
                            int dis = (int) drivePath.getDistance();
                            int dur = (int) drivePath.getDuration();
                            String des = AMapUtil.getFriendlyTime(dur) + "("
                                    + AMapUtil.getFriendlyLength(dis) + ")";
                            // mRotueTimeDes.setText(des); 时间
                            // mRouteDetailDes.setVisibility(View.VISIBLE);
                            int taxiCost = (int) mDriveRouteResult.getTaxiCost();
                            // mRouteDetailDes.setText("打车约"+taxiCost+"元");
                            // mBottomLayout.setOnClickListener(new OnClickListener() {
                            // @Override
                            // public void onClick(View v) {
                            // Intent intent = new Intent(mContext,
                            // DriveRouteDetailActivity.class);
                            // intent.putExtra("drive_path", drivePath);
                            // intent.putExtra("drive_result",
                            // mDriveRouteResult);
                            // startActivity(intent);
                            // }
                            // });

//                            if (latLonPoints.size() > 0) {
//
//                                for (int i = 0; i < latLonPoints.size(); i++) {
//                                    aMap.addMarker(new MarkerOptions()
//                                            .position(
//                                                    AMapUtil.convertToLatLng(latLonPoints
//                                                            .get(i)))
//                                            .icon(BitmapDescriptorFactory
//                                                    .fromResource(R.drawable.amap_through))
//                                            .perspective(true).draggable(true));
//                                }
//
//                            }

                        } else if (result != null && result.getPaths() == null) {
                            Toast.makeText(ctx,
                                    "对不起，没有搜索到相关数据！", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(ctx, "对不起，没有搜索到相关数据！", Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
//                    Log.i("MyDingDanMapActivity", "" + errorCode);
                }
            }

            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

            }

            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

            }
        });
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                LatLng location = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                locationLatLng = location;
                if (!mFirstFix) {
                    mFirstFix = true;
                    addCircle(location, amapLocation.getAccuracy());//添加定位精度圆
                    addMarker(location);//添加定位图标
                    mSensorHelper.setCurrentMarker(mLocMarker);//定位图标旋转
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,16));
                } else {
                    mCircle.setCenter(location);
                    mCircle.setRadius(amapLocation.getAccuracy());
                    mLocMarker.setPosition(location);
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(location));
                }
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }


    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    private void addCircle(LatLng latlng, double radius) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        mCircle = aMap.addCircle(options);
    }

    private void addMarker(LatLng latlng) {
        if (mLocMarker != null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked)));
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker = aMap.addMarker(options);
        mLocMarker.setTitle(LOCATION_MARKER_FLAG);
    }

    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteResult(int mode) {
        if (endLatLon == null) {
            Toast.makeText(ctx, "终点未设置", Toast.LENGTH_SHORT).show();
        } else {
            LatLonPoint startNaviLatLng = new LatLonPoint(locationLatLng.latitude, locationLatLng.longitude);
            RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                    startNaviLatLng, endLatLon);
            RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, mode,
                    null, null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
            mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_navigation:
                Intent intent = new Intent(ctx, WalkNavigationActivity.class);
                intent.putExtra("latitude", aMap.getMyLocation().getLatitude());
                intent.putExtra("longitude", aMap.getMyLocation().getLongitude());
                startActivity(intent);
                break;
            case R.id.btn_path:
                //路线规划
                searchRouteResult(RouteSearch.DrivingDefault);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
        startList.clear();
        wayList.clear();
        endList.clear();
        if (mLocMarker != null) {
            mLocMarker.destroy();
        }
        mapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
        if (mSensorHelper != null) {
            mSensorHelper.unRegisterSensorListener();
            mSensorHelper.setCurrentMarker(null);
            mSensorHelper = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

}
