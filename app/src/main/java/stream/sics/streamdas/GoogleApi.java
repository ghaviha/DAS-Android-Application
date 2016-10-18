/*
* Java file that contains the Google API that handles everything with the obtaining of locations
*/
package stream.sics.streamdas;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GoogleApi implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    String city;

    /*The local TAG which helps keep track which java files prints messages in the logcat*/
    private static String TAG = "GoogleApi";

    /*Variables to hold the UI context and activity*/
    protected Context context;
    protected Activity activity;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    /* Google client to interact with Google API*/
    public GoogleApiClient mGoogleApiClient;

    /*Request permission ID*/
    public static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    /*Variable to hold the Location request*/
    private LocationRequest mLocationRequest;

    /*Function that receives the UI activity and context*/
    public GoogleApi(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    /* Creating google api client object*/
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /*Creating location request object*/
    protected void createLocationRequest() {

        /*Vaues to pass to the location request. UPDATE_INTERVAL decides how often locations are requested
        * DISPLACEMENT decides how far we must have travelled before a new location is requested*/
        int UPDATE_INTERVAL = 5000; // 5 sec
        int DISPLACEMENT = 0; // 0 meters


        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /*Method to verify google play services on the device*/
    public boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(activity, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }

        return true;
    }

    /*Starting the location updates*/
    protected void startLocationUpdates() {

        /*Check if the application has permission to use location on the device*/
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);

        } else if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    /*Stopping location updates*/
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /*Google api callback methods*/
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {
        // Once connected with google api, start the location updates
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        /*ACCURACY decides how "bad" the location can be to be accepted*/
        int ACCURACY = 17; // < 17 meters


        /*This "if" will check if internet is available and if it is set your current city as
        * a preset to the selection screen*/
        if (Global.select_active && isOnline()) {


            Geocoder gcd = new Geocoder(context, Locale.getDefault());

            try {
                List<Address> addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    city = addresses.get(addresses.size()-1).getLocality();
                    Global.Uihandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (Select.onStartup) {
                                int index;
                                for (int i = 0; i < Select.train_start_list.getCount(); i++) {
                                    if (city.equals(Select.train_start_list.getItemAtPosition(i).toString())) {
                                        index = i;
                                        Select.train_start_list.setSelection(index);
                                        Select.onStartup = false;
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*structure that adds locations to the list if the testing variable is set to false
         * and the view is "mainview" */
        if (!Global.testing) {
            if (Global.mainview_active) {

                location.setTime(System.currentTimeMillis()); //Get the current time


                if (location.getAccuracy() < ACCURACY) { // Only accepts locations within the decided accuracy


                    if (!location.hasSpeed()) { // If the location has speed, use it. If not, calculate it by means of the last location

                        if (Global.loc_List.size() > 0) {
                            location.setSpeed(Global.loc_List.get(Global.loc_List.size()-1).getSpeed());
                        } else {
                            location.setSpeed(0);
                        }
                    }

                    try {
                        Global.SEMAPHORE.acquire(); //Claim the semaphore and add location to the list
                        Global.loc_List.add(location);

                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    } finally {
                        Global.SEMAPHORE.release();
                    }



                    /*Write the location to a log file*/
                    try {
                        String content;

                        content = location.getLatitude() + "," + location.getLongitude() + ",\tVel: " + location.getSpeed() * 3.6 + ",\tTime: " + location.getTime() + ",\tAcc: " + location.getAccuracy();

                        FileWriter fw_loc;
                        if (Global.running) {
                            fw_loc = new FileWriter(Global.Location_log, true);
                            fw_loc.write(content + "\n");
                        } else {
                            fw_loc = new FileWriter(Global.Location_log2, true);
                            fw_loc.write(content + "\n");
                        }

                        fw_loc.close();

                        fw_loc.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    String content = location.getLatitude() + "," + location.getLongitude() + ",\tVel: " + location.getSpeed() * 3.6 + ",\tTime: " + location.getTime() + ",\tAcc: " + location.getAccuracy();

                    try {
                        FileWriter fw_loc;
                        fw_loc = new FileWriter(Global.Location_log3, true);

                        fw_loc.write(content + "\n");
                        fw_loc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    Global.Uihandler.post(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(context, "Bad accuracy location", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    /*Function that checks if there is internet available*/
    public boolean isOnline() {

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
    }
}
