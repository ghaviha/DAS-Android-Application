/*
* This java file handles the UI during the selection of train, departure - arrival cities, weight and time.
* It checks for GPS and network availability and starts the GPS.  */

package stream.sics.streamdas;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toolbar;

import java.util.ArrayList;
import java.util.Arrays;

public class Select extends AppCompatActivity {

    static boolean onStartup = true;

    Spinner train_list, train_mass_list, train_stop_list, train_time_list;
    static Spinner train_start_list;

    RadioButton demo;

    ArrayAdapter<String> adapter4;

    String[][] start_stop_locations = new String[3][3];

    boolean play_flag = false;

    public final static String EXTRA_MESSAGE = "stream.sics.streamdas.MESSAGE";

    /* LogCat tag that shows from which part of the code the messages comes from */
    private static final String TAG = "Select";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select);

        /*This initializes the toolbar and sets the content view*/
        Toolbar myToolbar = (Toolbar) findViewById(R.id.select_toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setActionBar(myToolbar);
        }
        demo = (RadioButton) findViewById(R.id.demo_mode);


        /*Gets the context*/
        final Context context = getApplicationContext();


        boolean gps_enabled = false ,network_enabled = false;

        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        try{
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        try{
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){
            ex.printStackTrace();
        }

        /*Checks if the GPS and the network is enabled, if not open options for the user to do so*/
        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(this.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(this.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();

        }

        Global.Uihandler = new Handler();

        /*Initialize the choices in the spinners*/
        start_stop_locations[0][0] = "Stockholm"; // Starting Location
        start_stop_locations[0][1] = "Västerås";
        start_stop_locations[0][2] = "Göteborg";

        start_stop_locations[1][0] = "Västerås"; // Starting Location
        start_stop_locations[1][1] = "Stockholm";
        start_stop_locations[1][2] = "Kolbäck";

        start_stop_locations[2][0] = "Kolbäck"; // Starting Location
        start_stop_locations[2][1] = "Stockholm";
        start_stop_locations[2][2] = "Västerås";

        initSpinners();

        Global.Ggle = new GoogleApi(this);
        gps();
    }

    /*Initialize the spinners, This function needs to be catered to the choices wanted.
    * Later on this should be remade for a server-client implementation*/
    public void initSpinners() {

        /*Sets the spinners bu their respective id's*/
        train_list = (Spinner)findViewById(R.id.train);
        train_mass_list = (Spinner)findViewById(R.id.mass);
        train_start_list = (Spinner)findViewById(R.id.start);
        train_stop_list = (Spinner)findViewById(R.id.stop);
        train_time_list = (Spinner)findViewById(R.id.time_table);

        String[] trains = new String[]{"X2000", "SJ Regional"};
        String[] masses = new String[]{"AV1 20%", "AV2 40%", "AV3 60%", "AV4 80%"};
        String[] start_locations = new String[] {"Stockholm", "Västerås", "Kolbäck"};

        String[] timetables = new String[]{"12:00 - 13:05 (65 min)", "15:00 - 17:00 (120 min)"};

        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, trains);
        train_list.setAdapter(adapter1);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, masses);
        train_mass_list.setAdapter(adapter2);
        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, start_locations);
        train_start_list.setAdapter(adapter3);

        ArrayList<String> lst = new ArrayList<String>(Arrays.asList( new String[]{""}));

        ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, timetables);
        train_time_list.setAdapter(adapter5);

        adapter4 = new ArrayAdapter<String>(Select.this, android.R.layout.simple_spinner_dropdown_item, lst);
        train_stop_list.setAdapter(adapter4);

        train_start_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStop(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /*Stop updating the spinners when a choice has been made*/
    public void updateStop(int index) {

        String[] stop_locations = new String[start_stop_locations[index].length-1];

        if (start_stop_locations[index][0].equals(train_start_list.getSelectedItem().toString())) {
            for(int j = 1; j < start_stop_locations[index].length; j++) {
                stop_locations[j-1] = start_stop_locations[index][j];
            }
        }
        ArrayList<String> lst = new ArrayList<String>(Arrays.asList(stop_locations));

        adapter4.clear();
        adapter4.addAll(lst);
    }

    /*When done, start the next activity*/
    public void nextactivity(View v) {

        if (play_flag) {
            Global.Ggle.buildGoogleApiClient();
            Global.Ggle.createLocationRequest();
        }

        Intent intent = new Intent(this, MainView.class);
        Spinner train_list, train_mass_list, train_start_list, train_stop_list, train_time_list;

        train_list = (Spinner)findViewById(R.id.train);
        train_mass_list = (Spinner)findViewById(R.id.mass);
        train_start_list = (Spinner)findViewById(R.id.start);
        train_stop_list = (Spinner)findViewById(R.id.stop);
        train_time_list = (Spinner)findViewById(R.id.time_table);

//        String dat_Message = train_list.getSelectedItem().toString() + "_" + train_start_list.getSelectedItem().toString() +
//                "_" + train_stop_list.getSelectedItem().toString() + "_" + train_mass_list.getSelectedItem().toString() +
//                "_" + train_time_list.getSelectedItem().toString();

        String message = train_start_list.getSelectedItem().toString() + ", " + train_stop_list.getSelectedItem().toString() + ", " + train_mass_list.getSelectedItem().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    /*This initializes and starts the GPS collection*/
    public void gps() {
        // First we need to check availability of play services
        if (Global.Ggle.checkPlayServices()) {

            // Building the GoogleApi client
            Global.Ggle.buildGoogleApiClient();
            Global.Ggle.createLocationRequest();
        } else {
            play_flag = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Global.select_active = true;

        if (Global.Ggle.mGoogleApiClient != null) {
            Global.Ggle.mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Global.select_active = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Global.select_active = false;

        if (Global.Ggle.mGoogleApiClient != null){
            if (Global.Ggle.mGoogleApiClient.isConnected()) {
                Global.Ggle.stopLocationUpdates();
                Global.Ggle.mGoogleApiClient.disconnect();
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Global.select_active = false;


    }

    @Override
    protected void onPause() {
        super.onPause();
        Global.select_active = false;
    }

    /*This is the callback which handles the permission request in GoogleApi*/
    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
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

    /*Decides what happens if the back button is pressed*/
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Activity")
                .setMessage("Are you sure you want to close this application?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    /*The following two functions handles the toolbar and what it should display/do*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.select_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_demo:
                // User chose the "Settings" item, show the app settings UI...
                // get demo_prompt.xml view
                LayoutInflater li = LayoutInflater.from(this);
                View promptsView = li.inflate(R.layout.demo_prompt, null);

                ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.APDefacto_Dark);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);

                // set demo_prompt.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        Global.testing = !Global.testing;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //Do something on UiThread

                                                if(Global.testing) {
                                                    demo.setVisibility(View.VISIBLE);
                                                } else {
                                                    demo.setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        });

                                    }
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

}
