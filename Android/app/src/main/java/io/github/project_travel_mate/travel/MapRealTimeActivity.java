package io.github.project_travel_mate.travel;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.project_travel_mate.R;
import objects.MapItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import utils.Constants;
import utils.GPSTracker;

/**
 * Show markers on map around user's current location
 */
public class MapRealTimeActivity extends AppCompatActivity implements OnMapReadyCallback {

    @BindView(R.id.data)
    ScrollView sc;

    private int index = 0;
    private Handler mHandler;

    private String curlat;
    private String curlon;

    private GoogleMap googleMap;

    private List<MapItem> mapItems =  new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_realtime);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mHandler = new Handler(Looper.getMainLooper());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String sorcelat = sharedPreferences.getString(Constants.SOURCE_CITY_LAT, Constants.DELHI_LAT);
        String sorcelon = sharedPreferences.getString(Constants.SOURCE_CITY_LON, Constants.DELHI_LON);
        String deslat = sharedPreferences.getString(Constants.DESTINATION_CITY_LAT, Constants.MUMBAI_LAT);
        String deslon = sharedPreferences.getString(Constants.DESTINATION_CITY_LON, Constants.MUMBAI_LON);
        String surce = sharedPreferences.getString(Constants.SOURCE_CITY, "Delhi");
        String dest = sharedPreferences.getString(Constants.DESTINATION_CITY, "Mumbai");

        sc.setVisibility(View.GONE);

        curlat = deslat;
        curlon = deslon;

        setTitle("Places");

        // Get user's current location
        GPSTracker tracker = new GPSTracker(this);
        if (!tracker.canGetLocation()) {
            tracker.showSettingsAlert();
            Log.e("cdsknvdsl ", curlat + "dsbjvdks" + curlon);
        } else {
            curlat = Double.toString(tracker.getLatitude());
            curlon = Double.toString(tracker.getLongitude());
            Log.e("cdsknvdsl", tracker.getLatitude() + " " + curlat + "dsbjvdks" + curlon);
            if (curlat.equals("0.0")) {
                curlat = "28.5952242";
                curlon = "77.1656782";
            }
            getMarkers(0, R.drawable.ic_local_pizza_black_24dp);
        }

        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Calls API to get nearby places
     *
     * @param mo mode; type of places;
     * @param ic marker icon
     */
    private void getMarkers(int mo, final int ic) {

        String uri = Constants.apilink + "places-api.php?mode=" + mo + "&lat=" + curlat + "&lng=" + curlon;
        Log.e("executing", uri + " ");

        //Set up client
        OkHttpClient client = new OkHttpClient();
        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Request Failed", "Message : " + e.getMessage());
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {

                final String res = Objects.requireNonNull(response.body()).string();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("YO", "Done");
                        try {
                            final JSONObject json = new JSONObject(res);
                            JSONArray routeArray = json.getJSONArray("results");

                            for (int i = 0; i < routeArray.length(); i++) {
                                String name = routeArray.getJSONObject(i).getString("name");
                                String web = routeArray.getJSONObject(i).getString("website");
                                String nums = routeArray.getJSONObject(i).getString("phone");
                                String addr = routeArray.getJSONObject(i).getString("address");
                                showMarker(Double.parseDouble(routeArray.getJSONObject(i).getString("lat")),
                                        Double.parseDouble(routeArray.getJSONObject(i).getString("lng")),
                                        routeArray.getJSONObject(i).getString("name"),
                                        ic);
                                mapItems.add(new MapItem(name, nums, web, addr));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("ERROR : ", e.getMessage() + " ");
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        if (item.getItemId() == R.id.action_sort) {

            new MaterialDialog.Builder(this)
                    .title(R.string.title)
                    .items(R.array.items)
                    .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {

                            googleMap.clear();
                            mapItems.clear();

                            for (int i = 0; i < which.length; i++) {
                                Log.e("selected", which[i] + " " + text[i]);
                                Integer icon;
                                switch (which[0]) {
                                    case 0:
                                        icon = R.drawable.ic_local_pizza_black_24dp;
                                        break;
                                    case 1:
                                        icon = R.drawable.ic_local_bar_black_24dp;
                                        break;
                                    case 2:
                                        icon = R.drawable.ic_camera_alt_black_24dp;
                                        break;
                                    case 3:
                                        icon = R.drawable.ic_directions_bus_black_24dp;
                                        break;
                                    case 4:
                                        icon = R.drawable.ic_local_mall_black_24dp;
                                        break;
                                    case 5:
                                        icon = R.drawable.ic_local_gas_station_black_24dp;
                                        break;
                                    case 6:
                                        icon = R.drawable.ic_local_atm_black_24dp;
                                        break;
                                    case 7:
                                        icon = R.drawable.ic_local_hospital_black_24dp;
                                        break;
                                    default:
                                        icon = R.drawable.ic_attach_money_black_24dp;
                                        break;
                                }
                                MapRealTimeActivity.this.getMarkers(which[0], icon);

                            }
                            return true;
                        }
                    })
                    .positiveText(R.string.choose)
                    .show();

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets marker at given location on map
     *
     * @param locationLat  latitude
     * @param locationLong longitude
     * @param locationName name of location
     * @param locationIcon icon
     */
    private void showMarker(Double locationLat, Double locationLong, String locationName, Integer locationIcon) {
        LatLng coord = new LatLng(locationLat, locationLong);

        if (ContextCompat.checkSelfPermission(MapRealTimeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 10));

                MarkerOptions abc = new MarkerOptions();
                MarkerOptions x = abc
                        .title(locationName)
                        .position(coord)
                        .icon(BitmapDescriptorFactory.fromResource(locationIcon));
                googleMap.addMarker(x);

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap map) {

        googleMap = map;

        // Zoom to current location
        LatLng coordinate = new LatLng(Double.parseDouble(curlat), Double.parseDouble(curlon));
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 10);
        map.animateCamera(yourLocation);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                sc.setVisibility(View.VISIBLE);
                for (int i = 0; i < mapItems.size(); i++) {
                    if (mapItems.get(i).getName().equals(marker.getTitle())) {
                        index = i;
                        break;
                    }
                }

                TextView title = MapRealTimeActivity.this.findViewById(R.id.VideoTitle);
                TextView description = MapRealTimeActivity.this.findViewById(R.id.VideoDescription);
                final Button calls, book;
                calls = MapRealTimeActivity.this.findViewById(R.id.call);
                book = MapRealTimeActivity.this.findViewById(R.id.book);

                title.setText(mapItems.get(index).getName());
                description.setText(mapItems.get(index).getAddress());
                calls.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + mapItems.get(index).getNumber()));
                        MapRealTimeActivity.this.startActivity(intent);

                    }
                });
                book.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent;
                        browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapItems.get(index).getAddress()));
                        MapRealTimeActivity.this.startActivity(browserIntent);
                    }
                });
                return false;
            }
        });

    }
}
