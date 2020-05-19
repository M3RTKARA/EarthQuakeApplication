package com.example.androidproject2020.earthquakewatcher.Activities;

import android.Manifest;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidproject2020.earthquakewatcher.UI.CustomInfoWindow;
import com.example.androidproject2020.earthquakewatcher.Model.EarthQuake;
import com.example.androidproject2020.earthquakewatcher.Services.NotificationService;
import com.example.androidproject2020.earthquakewatcher.Util.Constants;
import com.example.androidproject2020.earthquakewatcher.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Scanner;
import java.io.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue queue;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private BitmapDescriptor[] iconColors;
    private Button showListBtn;
    private TextView textView;
    CircleOptions circleOptions;
    double[] savings;
    private Button btn;
    NotificationManager NM;
    Intent notificationIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        textView = (TextView) findViewById(R.id.textView);
        btn = (Button) findViewById(R.id.btn);

        savings = new double[4];

        NM=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        showListBtn = (Button) findViewById(R.id.showListBtn);


        showListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, QuakesListActivity.class));
            }
        });

        iconColors = new BitmapDescriptor[]{
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
        };


        queue = Volley.newRequestQueue(this);

        //START FETCHING EARTHQUAKES
        getEarthQuakes();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);


        //CHECK IF THERE IS ALREADY CIRCLE IN MAP

        String alltext="";
        try{
            Scanner scan = new Scanner(
                    openFileInput("out.txt")
            );

            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                alltext+=line;
                alltext+="\n";
            }
            if(alltext!="")
            {
                String[] lines = alltext.split(System.getProperty("line.separator"));
                savings[0] = Double.parseDouble(lines[0]);
                savings[1] = Double.parseDouble(lines[1]);
                savings[2] = Double.parseDouble(lines[2]);
                savings[3] = Double.parseDouble(lines[3]);

                textView.setText("Circle is created at \nlatitude:" + savings[0] + "\nlongtitude: " + savings[1]);
                btn.setVisibility(View.VISIBLE);



                createCircle(new LatLng(savings[0],savings[1]),savings[2],(int) savings[3]);
            }
            scan.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //CHECK THE SDK VERSION TO ARRANGE PERMISSON

        if (Build.VERSION.SDK_INT < 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Ask for permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);


            } else {

            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        }
    }


    //GET ALL THE EARTHQUAKES WITH PROGRESSBAR

    public void getEarthQuakes() {

        final ProgressDialog dialog = ProgressDialog.show(this, null, "Fetching Earthquakes");


        final EarthQuake earthQuake = new EarthQuake();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONArray features = response.getJSONArray("features");
                            for (int i = 0; i < features.length(); i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                                //Get geomety object
                                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");

                                //get coordinates array
                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double lon = coordinates.getDouble(0);
                                double lat = coordinates.getDouble(1);


                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setTime(properties.getLong("time"));
                                earthQuake.setLat(lat);
                                earthQuake.setLon(lon);
                                earthQuake.setMagnitude(properties.getDouble("mag"));
                                earthQuake.setDetailLink(properties.getString("detail"));

                                java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                                String formattedDate =  dateFormat.format(new Date(Long.valueOf(properties.getLong("time")))
                                .getTime());

                                MarkerOptions markerOptions = new MarkerOptions();

                                markerOptions.icon(iconColors[Constants.randomInt(iconColors.length, 0)]);
                                markerOptions.title(earthQuake.getPlace());
                                markerOptions.position(new LatLng(earthQuake.getLat(), earthQuake.getLon()));
                                markerOptions.snippet("Magnitude: "
                                        + earthQuake.getMagnitude() +
                                        "\n" + "Date: " + formattedDate);


                                //Add circle to markers that have mag > x
                                if (earthQuake.getMagnitude() >= 2.0 ) {
                                    CircleOptions circleOptions = new CircleOptions();
                                    circleOptions.center(new LatLng(earthQuake.getLat(),
                                            earthQuake.getLon()));
                                    circleOptions.radius(30000);
                                    circleOptions.strokeWidth(2.5f);
                                    circleOptions.fillColor(Color.RED);

                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                    mMap.addCircle(circleOptions);

                                }

                                Marker marker = mMap.addMarker(markerOptions);
                                marker.setTag(earthQuake.getDetailLink());

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }


        });


        queue.add(jsonObjectRequest);

        queue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<String>() {
            @Override
            public void onRequestFinished(Request<String> request) {
                if (dialog !=  null && dialog.isShowing())
                    dialog.dismiss();

            }
        });
    }

    //TO SHOW THE DETAILED INFORMATION

    @Override
    public void onInfoWindowClick(Marker marker) {

        getQuakeDetails(marker.getTag().toString());

    }

    //FETCH THE DETAILED INFORMATION WITH API

    private void getQuakeDetails(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String detailsUrl = "";

                try {
                    JSONObject properties = response.getJSONObject("properties");
                    JSONObject products = properties.getJSONObject("products");
                    JSONArray geoserve = products.getJSONArray("geoserve");

                    for (int i = 0; i < geoserve.length(); i++) {
                        JSONObject geoserveObj = geoserve.getJSONObject(i);

                        JSONObject contentObj = geoserveObj.getJSONObject("contents");
                        JSONObject geoJsonObj = contentObj.getJSONObject("geoserve.json");

                         detailsUrl = geoJsonObj.getString("url");


                    }

                    getMoreDetails(detailsUrl);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);


    }


    public void getMoreDetails(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                View view = getLayoutInflater().inflate(R.layout.popup, null);

                Button dismissButton = (Button) view.findViewById(R.id.dismissPop);
                Button dismissButtonTop = (Button) view.findViewById(R.id.desmissPopTop);
                TextView popList = (TextView) view.findViewById(R.id.popList);
                WebView htmlPop = (WebView) view.findViewById(R.id.htmlWebview);

                StringBuilder stringBuilder = new StringBuilder();


                try {

                    if (response.has("tectonicSummary") && response.getString("tectonicSummary") != null) {

                         JSONObject tectonic = response.getJSONObject("tectonicSummary");

                        if (tectonic.has("text") && tectonic.getString("text") != null) {

                            String text = tectonic.getString("text");

                            htmlPop.loadDataWithBaseURL(null, text, "text/html", "UTF-8", null);
                        }
                    }

                    JSONArray cities = response.getJSONArray("cities");

                    for (int i = 0; i < cities.length(); i++) {
                        JSONObject citiesObj = cities.getJSONObject(i);

                        stringBuilder.append("City: " + citiesObj.getString("name")
                          + "\n" + "Distance: " + citiesObj.getString("distance")
                          + "\n" + "Population: "
                          + citiesObj.getString("population"));

                        stringBuilder.append("\n\n");

                    }

                    popList.setText(stringBuilder);

                    dismissButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dismissButtonTop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialogBuilder.setView(view);
                    dialog = dialogBuilder.create();
                    dialog.show();

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonObjectRequest);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    //CREATE CIRCLE WITH LONG CLICK ON MAP AND CREATE CIRCLE ONLY ONCE

    @Override
    public void onMapLongClick(final LatLng latLng) {

        if(savings[2]!=0.0)
        {
            Toast.makeText(getApplicationContext(),"You can keep one filter area",Toast.LENGTH_LONG).show();
            return;
        }

        textView = (TextView) findViewById(R.id.textView);


        dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        View view = getLayoutInflater().inflate(R.layout.filter_area_popup, null);

        Button dismissButton = (Button) view.findViewById(R.id.dismissPop);
        Button dismissButtonTop = (Button) view.findViewById(R.id.desmissPopTop);
        final EditText editText = (EditText) view.findViewById(R.id.editText);
        final RadioButton radioButton1 = (RadioButton) view.findViewById(R.id.radioBtn1);
        final RadioButton radioButton2 = (RadioButton) view.findViewById(R.id.radioBtn2);
        final RadioButton radioButton3 = (RadioButton) view.findViewById(R.id.radioBtn3);
        Button applyButton = (Button) view.findViewById(R.id.applyFilter);


        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dismissButtonTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(editText.getText().toString().matches(""))
                    Toast.makeText(getApplicationContext(),"Enter the radius please!!",Toast.LENGTH_LONG).show();
                else
                {
                    //CREATE CIRCLE
                    if(radioButton1.isChecked())
                        createCircle(latLng, Double.parseDouble(editText.getText().toString()),1);
                    else if(radioButton2.isChecked())
                        createCircle(latLng, Double.parseDouble(editText.getText().toString()),2);
                    else if(radioButton3.isChecked())
                        createCircle(latLng, Double.parseDouble(editText.getText().toString()),3);


                    textView.setText("Circle is created at \nlatitude:" + latLng.latitude + "\nlongtitude: " + latLng.longitude);
                    dialog.dismiss();
                }


            }
        });

        dialogBuilder.setView(view);
        dialog = dialogBuilder.create();
        dialog.show();

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng , 2));

    }

    //CREATE CIRCLE WITH GIVEN INFORMATIONS SUCH AS RADIUS

    private void createCircle(LatLng latLng, double radius,int type) {

        circleOptions = new CircleOptions();

        circleOptions.center(latLng);
        circleOptions.clickable(true);

        circleOptions.radius(radius);
        circleOptions.strokeWidth(2.5f);
        circleOptions.fillColor(Color.BLUE);

        mMap.addCircle(circleOptions);

        //SAVE THE CIRCLE INFORMATION IN INTERNAL STORAGE

        if(savings[2]==0.0) {
            try {
                PrintStream output = new PrintStream(openFileOutput("out.txt", MODE_PRIVATE));
                output.println(latLng.latitude);
                output.println(latLng.longitude);
                output.println(radius);
                output.println(type);

                savings[0] = latLng.latitude;
                savings[1] = latLng.longitude;
                savings[2] = radius;
                savings[3] = type;
                output.close();
                btn.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

    }

    //REMOVE THE CIRCLE FROM MAP

    public void removeFilter(View view) {

        for(int i=0; i<savings.length;i++)
        {
            savings[i]=0.0;
        }

        File dir = getFilesDir();
        File file = new File(dir, "out.txt");
        file.delete();

        Intent i = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.CUPCAKE) {
            i = getBaseContext().getPackageManager().
                    getLaunchIntentForPackage(getBaseContext().getPackageName());
        }
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();

    }

    //EXECUTE THE NOTIFICATION SERVICE AFTER ONSTOP CALLED

    @Override
    protected void onStop() {


        String alltext="";
        try{
            Scanner scan = new Scanner(
                    openFileInput("out.txt")
            );

            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                alltext+=line;
                alltext+="\n";
            }
            if(alltext!="")
            {
                String[] lines = alltext.split(System.getProperty("line.separator"));
                savings[0] = Double.parseDouble(lines[0]);
                savings[1] = Double.parseDouble(lines[1]);
                savings[2] = Double.parseDouble(lines[2]);
                savings[3] = Double.parseDouble(lines[3]);


            }
            scan.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }


        if(savings[2]!=0.0 && notificationIntent==null)
        {
            //TURN ON BACKGROUND SERVICE
            notificationIntent = new Intent(this, NotificationService.class);

            notificationIntent.putExtra("savings",savings);

            startService(notificationIntent);

        }
        super.onStop();
    }

}

