package com.example.androidproject2020.earthquakewatcher.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidproject2020.earthquakewatcher.Activities.MapsActivity;
import com.example.androidproject2020.earthquakewatcher.R;
import com.example.androidproject2020.earthquakewatcher.Util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends Service {

    String TAG = "earthquake";
    double[] savings;
    private RequestQueue queue;
    Timer timer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        //super.onStartCommand(intent, flags, startId);
        //get extras
        savings = intent.getDoubleArrayExtra("savings");

        queue = Volley.newRequestQueue(this);


        timer = new Timer();
        timer.schedule(new TimerTask() {  //her 60 sn de bir bildirimGonder(); metodu çağırılır.
            @Override
            public void run() {
                getEarthQuakesTime();
            }

        }, 0, 60000);

        return START_STICKY;
    }


    @Override
    public IBinder onBind (Intent arg0) {
        return null;
    }

    public void getEarthQuakesTime() {

        final Time time = new Time();   time.setToNow();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONArray features = response.getJSONArray("features");
                            for (int i = 0; i < features.length(); i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");

                                //get coordinates array
                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double lon = coordinates.getDouble(0);
                                double lat = coordinates.getDouble(1);


                                long tempTime = properties.getLong("time");
                                double tempMag = properties.getDouble("mag");


                                //Add circle to markers that have mag > x
                                if ((int) savings[3] == 1) {
                                    if(tempMag < 3.5 &&  (time.toMillis(false) - tempTime) < 10000 && (distance(savings[0],savings[1],lat,lon) < savings[2]) )
                                    {
                                        notifyExecute(lat,lon,tempMag);

                                    }

                                }
                                else if((int) savings[3] == 2)
                                {
                                    if(tempMag < 4.5  && (distance(savings[0],savings[1],lat,lon) < savings[2]) && (time.toMillis(false) - tempTime) < 10000 )
                                    {
                                        notifyExecute(lat,lon,tempMag);

                                    }
                                }
                                else
                                {
                                    if(tempMag > 4.5 && (time.toMillis(false) - tempTime) < 10000 && (distance(savings[0],savings[1],lat,lon) < savings[2]))
                                    {
                                        notifyExecute(lat,lon,tempMag);

                                    }
                                }

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

    }

    private void notifyExecute(double lat,double lon, double tempMag) {

        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0,intent,0);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder.setContentTitle("Earthquake Occured!").setContentText("EarthQuake Latitude: " + lat + "\nEarthQuake Longtitude: " + lon + "\nThe Magnitude: " + tempMag)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.logo2)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setStyle(new Notification.BigTextStyle(builder)
                        .bigText("EarthQuake Latitude: " + lat + "\nEarthQuake Longtitude: " + lon + "\nThe Magnitude: " + tempMag)
                        .setBigContentTitle("Earthquake Occured!")
                        .setSummaryText("The Magnitude: " + tempMag));


        Notification notification = builder.build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1,notification);

    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (int) (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

}
