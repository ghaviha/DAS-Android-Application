/*
* This file handles the UI when the app has started to display the graph view. It also starts the
* necessary threads
*/

package stream.sics.streamdas;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

public class MainView extends AppCompatActivity {

    /*The local TAG which helps keep track which java files prints messages in the logcat*/
    private static String TAG = "MainView";

    static Context context;

    FragmentPagerAdapter adapterViewPager;

    /*Declaration of the threads*/
    Thread runDas_Thread = null, testing_Thread = null;

    static ViewPager vpPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment);

        /*Sets up the viewpager that handles the swipeviews*/
        vpPager = (ViewPager) findViewById(R.id.vpPager);
        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);

        vpPager.setCurrentItem(0);

        context = this;

        boolean invalid_dat = false;

        /*initialize different variables and classes*/
        Global.mainview_active = true;
        Global.position_X1 = 0;
        Global.position_X2 = 0;
        Global.plot_Domain_Min = -100;
        Global.plot_Domain_Max = 200;
        Global.train = new Global.trainData();
        Global.E_Total = 0;

        Global.timeStamp = (System.currentTimeMillis() / 1000);
        Global.onlineCalculation_log = getExternalFilesDir(null) + File.separator + "logs" + File.separator + Global.timeStamp + "_onlineCalculation_log.txt";
        Global.Location_log = getExternalFilesDir(null) + File.separator + "logs" + File.separator + Global.timeStamp + "_Location_log.txt";
        Global.Location_log2 = getExternalFilesDir(null) + File.separator + "logs" + File.separator + Global.timeStamp + "_Location_log_!started.txt";
        Global.Location_log3 = getExternalFilesDir(null) + File.separator + "logs" + File.separator + Global.timeStamp + "_Location_log_badacc.txt";

        Global.csv_iterator = 0;

        Global.loc_List.clear();

        Global.Uihandler = new Handler();

        Intent intent = getIntent();
        Global.dat_Message = intent.getStringExtra(Select.EXTRA_MESSAGE);

        /*Check which dat-file to read*/
        switch(Global.dat_Message) {
            case "Västerås, Kolbäck, AV1 20%":
                Global.filename = "120, 1500, 200 - 126T - AV1, v-k_Java";
                break;
            case "Västerås, Kolbäck, AV2 40%":
                Global.filename = "120, 1500, 200 - 132T - AV2, v-k_Java";
                break;
            case "Västerås, Kolbäck, AV3 60%":
                Global.filename = "120, 1500, 200 - 138T - AV3, v-k_Java";
                break;
            case "Västerås, Kolbäck, AV4 80%":
                Global.filename = "120, 1500, 200 - 142T - AV4, v-k_Java";
                break;
            case "Kolbäck, Västerås, AV1 20%":
                Global.filename = "132, 1500, 200 - 126T - AV1, k-v_Java";
                break;
            case "Kolbäck, Västerås, AV2 40%":
                Global.filename = "132, 1500, 200 - 132T - AV2, k-v_Java";
                break;
            case "Kolbäck, Västerås, AV3 60%":
                Global.filename = "132, 1500, 200 - 138T - AV3, k-v_Java";
                break;
            case "Kolbäck, Västerås, AV4 80%":
                Global.filename = "132, 1500, 200 - 142T - AV4, k-v_Java";
                break;
            default:
                Toast.makeText(MainView.this, "No valid dat file, try again", Toast.LENGTH_SHORT).show();
                invalid_dat = true;
                finish();
        }

        /*Check if the necessary services are installed and start the GPS */
        if (!invalid_dat) {
            /*Start the thread that gets the Dat-file*/
            new Thread(new GetDat(this)).start();
        }
    }

    /*This function is activated when the start button is pressed*/
    public void runDAS(View v) {

        Button start_button = (Button) findViewById(R.id.start_button);
        start_button.setVisibility(View.INVISIBLE);

        if (!Global.running) {
            runDas_Thread = new Thread(new runDas());
            runDas_Thread.start(); // declare and start the worker thread
            if (Global.testing){
                testing_Thread = new Thread(new Testing(this)); //If testing is true declare and start the testing thread
                testing_Thread.start();
            }
        }
    }

    /*Functions that are called when OS interrupts. Such as calls or other applications */
    @Override
    protected void onStart() {
        super.onStart();
        Global.mainview_active = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Global.mainview_active = true;
        if (!Global.Ggle.mGoogleApiClient.isConnected()) {
            Global.Ggle.mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Global.mainview_active = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Global.mainview_active = false;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Global.mainview_active = false;

        if (testing_Thread != null) {
            testing_Thread.interrupt();
        }
        if (runDas_Thread != null) {
            runDas_Thread.interrupt();
        }

        Global.running = false;
        if (Global.progressDialog != null) {
            Global.progressDialog.cancel();
        }

        Global.travelled = null;
        Global.travelled_X.clear();
        Global.travelled_Y.clear();
        Global.train = null;
        Global.loc_List.clear();

    }

    /*This callback function displays a permission request, if not already granted to the application*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GoogleApi.MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /*Function that decides what happens when the backbutton is pressed*/
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Aborting route")
                .setMessage("Are you sure you want to abort this route?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    /*The following two functions handles the toolbar and what it should display/do*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_recalculate:
                // User chose the "Settings" item, show the app settings UI...
                // get recalc_prompt.xml view
                LayoutInflater li = LayoutInflater.from(this);
                View promptsView = li.inflate(R.layout.recalc_prompt, null);

                ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.APDefacto_Dark);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);

                // set recalc_prompt.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        new Thread(new Runnable() {
                                            public void run()
                                            {
                                                recalculate();
                                            }
                                        }).start();                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /*Class which handles the functions for the pageradapter*/
    public static class MyPagerAdapter extends FragmentPagerAdapter {
        private static int NUM_ITEMS = 2;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return MainViewFragment.newInstance(0, "Page # 1");
                case 1: // Fragment # 0 - This will show FirstFragment different title
                    return EnergyFragment.newInstance(1, "Page # 2");
                default:
                    return null;
            }
        }

    }

    /*Function which handles the recalculate button function in the toolbar*/
    public static void recalculate() {

        try {
            Global.SEMAPHORE.acquire();

            if (Global.loc_List.size() > 1) {
                Location temp_loc = new Location("temp_loc");
                int closest_sign = 0;
                double shortest_dist = Double.MAX_VALUE;


                Global.posts[] temp_kilom_Signs = new Global.posts[Global.kilom_Signs.length+1];

                for (int i = 0; i < Global.kilom_Signs.length; i++) {
                    temp_kilom_Signs[i] = Global.kilom_Signs[i];
                }
                temp_kilom_Signs[temp_kilom_Signs.length-1] = new Global.posts(Global.stations[Global.stations.length-1].lat, Global.stations[Global.stations.length-1].lon, 0);

                for (int i = 0; i < temp_kilom_Signs.length; i++) {
                    temp_loc.setLatitude(temp_kilom_Signs[i].lat);
                    temp_loc.setLongitude(temp_kilom_Signs[i].lon);
                    if (Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc) < shortest_dist) {
                        shortest_dist = Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc);
                        closest_sign = i;
                    }
                }

                if (shortest_dist > Global.loc_List.get(Global.loc_List.size() - 1).getAccuracy()) {

                    if (closest_sign > 0 && closest_sign < temp_kilom_Signs.length) {
                        temp_loc.setLatitude(temp_kilom_Signs[closest_sign - 1].lat);
                        temp_loc.setLongitude(temp_kilom_Signs[closest_sign - 1].lon);
                        double dist1 = Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc);
                        double dist2;

                        try {
                            temp_loc.setLatitude(temp_kilom_Signs[closest_sign + 1].lat);
                            temp_loc.setLongitude(temp_kilom_Signs[closest_sign + 1].lon);
                            dist2 = Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc);
                        } catch (IndexOutOfBoundsException e) {
                            temp_loc.setLatitude(Global.stations[Global.stations.length - 1].lat);
                            temp_loc.setLongitude(Global.stations[Global.stations.length - 1].lon);
                            dist2 = Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc);
                        }


                        if (closest_sign == temp_kilom_Signs.length-1) {
                            runDas.totdist = Global.train.TDistance - Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc);


                        } else if (closest_sign == Global.kilom_Signs.length-1) {
                            Location temp_loc2 = new Location("temp");
                            temp_loc2.setLatitude(Global.kilom_Signs[closest_sign].lat);
                            temp_loc2.setLongitude(Global.kilom_Signs[closest_sign].lon);

                            if (Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc) > temp_loc2.distanceTo(temp_loc)){
                                runDas.totdist = Global.offset + (closest_sign * 1000) - shortest_dist;
                            } else {
                                runDas.totdist = Global.offset + (closest_sign * 1000) + shortest_dist;
                            }


                        } else if (dist1 < dist2) {
                            if (closest_sign == temp_kilom_Signs.length-1){
                                runDas.totdist = Global.train.TDistance - shortest_dist;
                            } else {
                                runDas.totdist = Global.offset + (closest_sign * 1000) - shortest_dist;
                            }

                        } else {
                            if (closest_sign == temp_kilom_Signs.length-1){
                                runDas.totdist = Global.train.TDistance;
                            } else {
                                runDas.totdist = Global.offset + (closest_sign * 1000) + shortest_dist;
                            }

                        }
                    } else if (closest_sign == 0) {
                        temp_loc.setLatitude(Global.stations[0].lat);
                        temp_loc.setLongitude(Global.stations[0].lon);

                        runDas.totdist = Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(temp_loc);


                    }
                    int index = 0;
                    double temp_min = Double.MAX_VALUE;
                    for (int i = 0; i < Global.first.X.length; i++) {
                        if (Math.abs(runDas.totdist - Global.first.X[i]) < temp_min) {
                            index = i;
                            temp_min = Math.abs(runDas.totdist - Global.first.X[i]);
                        }
                    }
                    long temp_time = Global.loc_List.get(Global.loc_List.size()-1).getTime() - (long) (Global.train.tstep * index)*1000;
                    Global.loc_List.get(0).setTime(temp_time);
                    Global.csv_iterator = index;
                }
            } else {
                Global.Uihandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Recalculation failed, no gps data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            Global.SEMAPHORE.release();
        }
    }
}


