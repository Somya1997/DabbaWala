package com.dabbawala.dabbawala;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dabbawala.dabbawala.Common.Common;
import com.dabbawala.dabbawala.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.kmenager.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.GoogleApiAvailabilityCache;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nineoldandroids.animation.ValueAnimator;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Welcome extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;
    //PLay Services
    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICES_RES_REQUEST = 7001;

    private LocationRequest mLocationRequest;

    private Location lastKnownLocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    GeoFire geoFire;
    SupportMapFragment mapFragment;
    Marker mCurrent;
    MaterialAnimatedSwitch location_switch;
    boolean first_time=true;


    //Car animation i,e route to destination
    private  List<LatLng> polyLineList;
    private Marker carMarker;
    private float v;
    private double lat,lng;
    private Handler handler;
    private LatLng startPosition,endPosition,currentPosition;
    private int index,next;
    private Button btnGo;
    private EditText edtPlace;
    private PolylineOptions polylineOptions, blackPolyLineOptions;
    private Polyline blackPolyLine,greyPolyLine;
    private  String destination;
    private IGoogleAPI mService;

    Runnable drawPathRunnable =new Runnable() {
        @Override
        public void run() {
            if(index<polyLineList.size()-1){
                index++;
                next=index+1;
            }
            if(index<polyLineList.size()-1){
                startPosition = polyLineList.get(index);
                endPosition = polyLineList.get(next);
            }
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v = valueAnimator.getAnimatedFraction();
                    lng = v*endPosition.longitude+(1-v)*startPosition.longitude;
                    lat = v*endPosition.latitude+ (1-v)*startPosition.latitude;
                    LatLng newPos = new LatLng(lat,lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f,0.5f);
                    carMarker.setRotation(getBearing(startPosition,newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(newPos)
                                .zoom(15.5f)
                                .build()
                    ));

                }
            });
            valueAnimator.start();
            handler.postDelayed(this,3000);

        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat = Math.abs(startPosition.latitude-endPosition.latitude);
        double lng = Math.abs(startPosition.longitude-endPosition.longitude);

        if(startPosition.latitude<endPosition.latitude && startPosition.longitude<endPosition.longitude){
            return (float)(Math.toDegrees(Math.atan(lng/lat)));
        }
        else if(startPosition.latitude>=endPosition.latitude && startPosition.longitude<endPosition.longitude){
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+90);
        }
        else if(startPosition.latitude>=endPosition.latitude && startPosition.longitude>=endPosition.longitude){
            return (float)(Math.toDegrees(Math.atan(lng/lat))+180);
        }
        else if(startPosition.latitude<endPosition.latitude && startPosition.longitude>=endPosition.longitude){
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
                displayLocation();

            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Init view
        location_switch = (MaterialAnimatedSwitch) findViewById(R.id.location_switch);
        btnGo =(Button)findViewById(R.id.btnGo);
        edtPlace =(EditText)findViewById(R.id.edtPlace);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
//                mMap.clear();
                lastKnownLocation = location;
                displayLocation();
            }
        };

        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isChecked) {
                if (isChecked) {
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are online", Snackbar.LENGTH_SHORT).show();
                } else {
                    stopLocationUpdates();
                    mCurrent.remove();
                    mMap.clear();
                    handler.removeCallbacks(drawPathRunnable);
                    Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        polyLineList = new ArrayList<>();
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destination = edtPlace.getText().toString();
                destination =destination.replace(" ","+");
                Log.d("Destination",destination);
                getDirection();
            }
        });

        drivers = FirebaseDatabase.getInstance().getReference("DriversLocation");
        geoFire = new GeoFire(drivers);
       mService = Common.getGoogleAPI();
       Log.i("Info",mService.toString());
    }

    private void getDirection() {
        currentPosition = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        String requestAPI = null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + destination + "&" +
                    "key=" + getResources().getString(R.string.google_direction_api);
            Log.d("Sid", requestAPI);
          mService.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for(int i=0; i<jsonArray.length();i++){
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyLine = poly.getString("points");
                                    polyLineList = decodePoly(polyLine);
                                }

                                //Adjusting Bounds
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for(LatLng latLng:polyLineList)
                                    builder.include(latLng);
                                LatLngBounds bounds = builder.build();
                                CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,2);
                                mMap.animateCamera(mCameraUpdate);
                                polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polyLineList);
                                greyPolyLine = mMap.addPolyline(polylineOptions);

                                blackPolyLineOptions = new PolylineOptions();
                                blackPolyLineOptions.color(Color.BLACK);
                                blackPolyLineOptions.width(5);
                                blackPolyLineOptions.startCap(new SquareCap());
                                blackPolyLineOptions.endCap(new SquareCap());
                                blackPolyLineOptions.jointType(JointType.ROUND);

                                blackPolyLine = mMap.addPolyline( blackPolyLineOptions);

                                mMap.addMarker(new MarkerOptions()
                                        .position(polyLineList.get(polyLineList.size()-1))
                                        .title("Pickup Location"));
                                //Animation
                                ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0,100);
                                polyLineAnimator.setDuration(2000);
                                polyLineAnimator.setInterpolator(new LinearInterpolator());
                                polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        List<LatLng> points = greyPolyLine.getPoints();
                                        int percentValue = (int)valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int) (size * (percentValue/100.0f));
                                        List<LatLng> p = points.subList(0,newPoints );
                                        blackPolyLine.setPoints(p);

                                    }
                                });
                                polyLineAnimator.start();
                                carMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                            .flat(true)
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                Handler handler = new Handler();
                                index =-1;
                                next =1;
                                handler.postDelayed(drawPathRunnable,3000);



                            }catch (JSONException e){
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(Welcome.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private ArrayList<LatLng> decodePoly(String encoded) {

        Log.i("Location", "String received: "+encoded);
        ArrayList<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((int) (((double) lat /1E5)* 1E6), (int) (((double) lng/1E5   * 1E6)));
            poly.add(p);
        }

        for(int i=0;i<poly.size();i++){
            Log.i("Location", "Point sent: Latitude: "+poly.get(i).latitude+" Longitude: "+poly.get(i).longitude);
        }
        return poly;
    }



    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            locationManager.removeUpdates(locationListener);

        }
    }

    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                if (location_switch.isChecked()) {
                    final double latitude = lastKnownLocation.getLatitude();
                    final double longitude = lastKnownLocation.getLongitude();

                    //Update to Firebase
                    geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            //Add Marker
                            if (mCurrent != null)
                                mCurrent.remove();
                            mCurrent = mMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                    .position(new LatLng(latitude, longitude))
                                    .title("Your Location"));
                            if(first_time) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16f));
                                first_time=false;
                            }

                            //Draw animation Rotation
                            rotateMarker(mCurrent, -480, mMap);
                        }
                    });
                }
            }
        }
    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mCurrent.getRotation();
        final long duration = 3500;

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rot = t * i + (1 - t) * startRotation;
                mCurrent.setRotation(-rot > 180 ? rot / 2 : rot);
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }

            }
        });

    }
}


