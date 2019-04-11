package com.example.mapdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private LocationManager _locationManager;
    private Point _startPoint;
    private Point _endPoint;
    private JSONArray _route = null;
    private boolean routeOn = false;
    private ArrayList<JSONObject> directionQueue = new ArrayList();
    private Location _nextStop = new Location(LocationManager.GPS_PROVIDER);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mapView = findViewById(R.id.mapView);
        Button b = findViewById(R.id.route);
        mapView = findViewById(R.id.mapView);


        _locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        b.setOnClickListener(v -> initRoute(v));
        loadMap();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        _nextStop.set(_locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        startLocation();



    }



    //starts the location service

    private void startLocation() {
        _locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showError("Location Changed");
                TextView tv = findViewById(R.id.textView);

                String msg = String.format("Lat: %f  Long: %f \n Distance to next location: %f meters", location.getLatitude(), location.getLongitude(), _nextStop.distanceTo(location));
                tv.setText(msg);
                //5 meter buffer
                if (_nextStop.distanceTo(location) < 1) {
                    routeOn = true;
                }

                Viewpoint vp = new Viewpoint(location.getLatitude(), location.getLongitude(), 400);
                mapView.setViewpoint(vp);

                _startPoint = new Point(location.getLatitude(), location.getLongitude());

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
    }

    /*
     * Loads Tamu ADA layer data to the basemap
     * sets map into the MapView
     * */
    private void loadMap() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location location2 = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        _startPoint = new Point(location.getLatitude(), location.getLongitude());
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, location.getLatitude(), location.getLongitude(), 20);
        // ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 30, -96, 100);

        ArcGISMapImageLayer layer1 = new ArcGISMapImageLayer("http://gis.tamu.edu/arcgis/rest/services/FCOR/ADA_120717/MapServer");
        map.getBasemap().getBaseLayers().add(layer1);

        mapView.setMap(map);



    }

    @Override
    protected void onPause() {
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.dispose();
    }

    //Sets up Auth with Esri Service
    private void setupOAuthManager() {
        String clientId = getResources().getString(R.string.client_id);
        String redirectUrl = getResources().getString(R.string.redirect_url);

        try {
            OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com", clientId, redirectUrl);
            DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
            AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
            AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
        } catch (MalformedURLException e) {
            showError(e.getMessage());
        }
    }

    public void initRoute(android.view.View v) {
        if(_startPoint == null){
            return;
        }
        _endPoint = new Point(30.578172, -96.320299);
        //start thread to get route
      try {
         Thread t = new Thread(()-> {
             try {
                 getRoute();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         });
         t.start();
         t.join();
          // add directions to directionQueue
          for (int i = 0; i < _route.length(); i++) {
              JSONObject direction = null;
              try {
                  direction = _route.getJSONObject(i);
                  directionQueue.add(direction);
                  System.out.println(direction.toString());

              } catch (Exception e) {
                  showError(e.getMessage());
              }

          }
          System.out.println("directionQueue ready");

          new Thread(() -> {
              try {
                  startRoute();
              } catch (JSONException e) {
                  e.printStackTrace();
              }
          }).start();
      }catch (Exception e){
          showError(e.getMessage());
      }
        //start thread to track route.




    }
    private void parseDirections(JSONObject direction) throws JSONException {
        double  lat = direction.getJSONObject("startPoint").getDouble("lat");
        double lng = direction.getJSONObject("startPoint").getDouble("lng");

        int turnType = direction.getInt("turnType");


    }

    private void startRoute() throws JSONException {
        //startLocation();
        routeOn = true;
        while (!directionQueue.isEmpty()) {
            if (routeOn) {
                System.out.println("routeON");
                JSONObject direction = directionQueue.remove(0);
                int heading = direction.getInt("direction");
                double lat = direction.getJSONObject("startPoint").getDouble("lat");
                double lng = direction.getJSONObject("startPoint").getDouble("lng");
                _nextStop.setLongitude(lng);
                _nextStop.setLatitude(lat);
                int turnType = direction.getInt("turnType");


                runOnUiThread(() -> {

                    String msg = String.format("You are at (%f,%f) you need to turn %s heading %s ", lat, lng, getTurn(turnType), getHeading(heading));
                    EditText et = findViewById(R.id.latLong);
                    et.setText(msg);

                });
                routeOn = false;
                System.out.println("routeOFF");
            }


        }
    }

    private void getRoute() throws IOException {

        String url = getResources().getString(R.string.routing_url);
        url+=String.format("&from=%f,%f",_startPoint.getX(),_startPoint.getY());
        url += String.format("&to=%f,%f&routeType=pedestrian&unit=k", _endPoint.getX(), _endPoint.getY());

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        try {
            System.out.println("route retrieved");
            parseJson(response.toString());
        }catch (Exception e){
            showError(e.getMessage());
        }



    }

    private void parseJson(String response)throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONArray legs = json.getJSONObject("route").getJSONArray("legs");
        JSONArray maneuvers = legs.getJSONObject(0).getJSONArray("maneuvers");
        _route = maneuvers;

    }

    //Shows errors
    private void showError(String message) {
        Log.d("FindRoute", message);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private String getTurn(int headId) {
        switch (headId) {
            case 0:
                return "straight";
            case 1:
                return "slight right";
            case 2:
                return "right";
            case 3:
                return "sharp right";
            case 4:
                return "reverse";
            case 5:
                return "sharp left";
            case 6:
                return "left";
            case 7:
                return "slight left";
            case 8:
                return "right U-turn";
            case 9:
                return "left U-turn";
            case 10:
                return "right merge";
            case 11:
                return "left merge";
            case 16:
                return "right fork";
            case 17:
                return "left fork";
            case 18:
                return "straight fork";

            default:
                return "No Turn Id recognized";

        }
    }

    private String getHeading(int turnId) {
        switch (turnId){
            case 0:
                return "straight";
            case 1:
                return "north";
            case 2:
                return "northwest";
            case 3:
                return "northeast";
            case 4:
                return "south";
            case 5:
                return "southeast";
            case 6:
                return "southwest";
            case 7:
                return "west";
            case 8:
                return "east";
            default:
                return "No Head Id recognized";

        }
    }


}
/*0 - none
1 - north
2 - northwest
3 - northeast
4 - south
5 - southeast
6 - southwest
7 - west
8 - east*/

/*0 - straight
1 - slight right
2 - right
3 - sharp right
4 - reverse
5 - sharp left
6 - left
7 - slight left
8 - right u-turn
9 - left u-turn
10 - right merge
11 - left merge
12 - right on ramp
13 - left on ramp
14 - right off ramp
15 - left off ramp
16 - right fork
17 - left fork
18 - straight fork*/