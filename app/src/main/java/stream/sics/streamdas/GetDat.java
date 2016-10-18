/*
* This java file handles the reading of the various files needed in order for the program to work.
* It also handles the creation of variables required for the calculations to function properly
*/
package stream.sics.streamdas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.androidplot.xy.XYPlot;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;


import cern.colt.matrix.impl.DenseObjectMatrix3D;


public class GetDat implements Runnable {

    /*The local TAG which helps keep track which java files prints messages in the logcat*/
    private static String TAG = "GetDat";

    /*A counter which is used to keep track of the time of the progressbar*/
    int counter = 0;


    /*Variables to hold the UI context and activity*/
    Context context;
    Activity activity;

    /*The size of a double variable in binary*/
    public static final int DOUBLE_SIZE = 8;

    /*This helps the class to receive the calling context and activity*/
    public GetDat(Activity activity){
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }

    /*First function to run in this class (also starts in a new thread when called)*/
    public void run() {

        getDatFile();
    }

    /*Function that sets the progressbar and decides which SIGNS to be read depending on the choice
    * in selection. It also sets the offset to the first SIGN and then calls main()*/
    void getDatFile() {



            Global.Uihandler.post(new Runnable() {
                @Override
                public void run() {

                    //Create a new progress dialog.
                    Global.progressDialog = new ProgressDialog(activity);
                    //Set the progress dialog to display a horizontal bar .
                    Global.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    //Set the dialog title to 'Loading...'.
                    Global.progressDialog.setTitle("Loading...");
                    //Set the dialog message to 'Loading application View, please wait...'.
                    Global.progressDialog.setMessage("Reading dat file, please wait...");
                    //This dialog can't be canceled by pressing the back key.
                    Global.progressDialog.setCancelable(false);
                    //This dialog isn't indeterminate.
                    Global.progressDialog.setIndeterminate(false);
                    //The maximum number of progress items is 100.
                    Global.progressDialog.setMax(100);
                    //Set the current progress to zero.
                    Global.progressDialog.setProgress(0);
                    //Display the progress dialog.
                    Global.progressDialog.show();
                }

            });

        if (Global.dat_Message.equals("Västerås, Kolbäck, AV1 20%") || Global.dat_Message.equals("Västerås, Kolbäck, AV2 40%")
                || Global.dat_Message.equals("Västerås, Kolbäck, AV3 60%") || Global.dat_Message.equals("Västerås, Kolbäck, AV4 80%")) {

                Global.kilom_Signs = readkmposts("vas-kol"); //Reads the corresponding SIGNS
                Global.stations = readstations("vas-kol");   //And stations

                /*Sets the lat-long for the second and third SIGN*/
                Global.temp_Signs.post.setLatitude(Global.kilom_Signs[1].lat);
                Global.temp_Signs.post.setLongitude(Global.kilom_Signs[1].lon);
                Global.temp_Signs.post2.setLatitude(Global.kilom_Signs[2].lat);
                Global.temp_Signs.post2.setLongitude(Global.kilom_Signs[2].lon);

                String temp_posts = String.valueOf(Global.kilom_Signs[0].id);
                int temp_post = Integer.parseInt(temp_posts.substring(temp_posts.length()-4))*1000;
                Global.offset = Math.abs(Global.stations[0].distance - temp_post);


                /*Does the same as the if, but with another file*/
        } else if (Global.dat_Message.equals("Kolbäck, Västerås, AV1 20%") || Global.dat_Message.equals("Kolbäck, Västerås, AV2 40%")
                || Global.dat_Message.equals("Kolbäck, Västerås, AV3 60%") || Global.dat_Message.equals("Kolbäck, Västerås, AV4 80%")) {
                Global.kilom_Signs = readkmposts("kol-vas");
                Global.stations = readstations("kol-vas");

                Global.temp_Signs.post.setLatitude(Global.kilom_Signs[1].lat);
                Global.temp_Signs.post.setLongitude(Global.kilom_Signs[1].lon);
                Global.temp_Signs.post2.setLatitude(Global.kilom_Signs[2].lat);
                Global.temp_Signs.post2.setLongitude(Global.kilom_Signs[2].lon);

                String temp_posts = String.valueOf(Global.kilom_Signs[0].id);
                int temp_post = Integer.parseInt(temp_posts.substring(temp_posts.length()-4))*1000;
                Global.offset = Math.abs(Global.stations[0].distance - temp_post);

            }

        main();
    }


    /*Function that calls for the creation of variables, sets the content view and then initializes
    * the plotter*/
    void main() {

        createVariables();

        //This works just like the onPostExecute method from the AsyncTask class
        Global.Uihandler.post(new Runnable() {
            @Override
            public void run() {
                //Close the progress dialog
                Global.progressDialog.dismiss();

                try {


                    Toolbar toolbar = (Toolbar) activity.findViewById(R.id.my_toolbar);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        activity.setActionBar(toolbar);
                    }

                    /*Finds the UI widgets by their respective ID's*/
                    Global.ntime = (TextView) activity.findViewById(R.id.ntime);
                    Global.update = (TextView) activity.findViewById(R.id.recV);
                    Global.update2 = (TextView) activity.findViewById(R.id.recT);
                    Global.update3 = (TextView) activity.findViewById(R.id.nrecV);
                    Global.update4 = (TextView) activity.findViewById(R.id.nrecT);
                    Global.batt = (TextView) activity.findViewById(R.id.Battery);
                    Global.plot = (XYPlot) (activity.findViewById(R.id.plot));



                } catch (Exception e) {
                    e.printStackTrace();
                }

                Plotter.initPlot();
                Plotter.setgraphparam();


            }
        });
    }

    /*Function that creates the necessary variables from the dat-file*/
    public void createVariables() {

        int SHORT_SIZE = 2;
        try {


            DataInputStream dis = new DataInputStream(context.getAssets().open("datfiles/"+Global.filename+".dat"));

            byte[] bytes = new byte[DOUBLE_SIZE];

            dis.read(bytes);
            Global.train.tS = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.xS = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.TTime = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.TDistance = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.Max_speedR = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.TMass = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.vS = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.minusT = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.plusT = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.Arr = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.Brr = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.Crr = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.ACMPower = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.MaxTrac = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.MaxBrake = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            dis.read(bytes);
            Global.train.BRPoint = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();

            Global.train.tstep = Global.train.TTime / Global.train.tS;
            Global.train.plusTstep = Math.round(Global.train.plusT/Global.train.tstep);
            Global.train.NoT = Global.train.tS + 1;
            Global.train.NoX = Global.train.xS + 1;
            Global.train.NoT2 = Global.train.NoT + Global.train.plusTstep;

            Global.train.SpeedL = new double[(int) Global.train.xS+1];
            for (int i = 0; i < Global.train.xS+1; i++){
                dis.read(bytes);
                Global.train.SpeedL[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            }

            Global.train.Elevations = new double[(int) Global.train.xS+1];
            for (int i = 0; i < Global.train.xS+1; i++){
                dis.read(bytes);
                Global.train.Elevations[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            }


            final int slices  = (int) Global.train.vS + 1;
            int rows    = (int) Global.train.NoT2;
            int columns = (int) Global.train.NoX;

            Global.train.Vop = new DenseObjectMatrix3D(slices, rows, columns);

            final long[] times = new long[slices];

            bytes = new byte[SHORT_SIZE * slices * columns * rows];
            dis.read(bytes);

            int short_counter = 0;
            try {
                for (int slice = 0; slice < slices; slice++) {
                    times[slice] = System.currentTimeMillis();
                    for (int column = 0; column < columns; column++) {
                        for (int row = 0; row < rows; row++) {

                            Global.train.Vop.set(slice, row, column, (short)((bytes[short_counter+1] & 0xff << 8) + (bytes[short_counter] & 0xff)));

                            short_counter += 2;
                        }
                    }
                    counter = slice;

                    //Global.update the changes to the UI thread
                    Global.Uihandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Set the current progress.
                            if (Global.progressDialog.getMax() != slices) {
                                Global.progressDialog.setMax(slices);
                            }

                            if (counter + 1 >= 15) {
                                double sec = 0.0;
                                for (int i = 0; i < counter - 1; i++) {
                                    sec += times[i + 1] / 1000.0 - times[i] / 1000.0;
                                }
                                sec = sec / (counter + 1);
                                double temp = Math.round((slices - (counter + 1)) * sec);
                                Global.progressDialog.setMessage("Reading dat file, please wait...\n" + (int) temp + " seconds remaining");
                            }

                            Global.progressDialog.setProgress(counter + 1);
                        }
                    });
                }

            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }


            dis.close();
        }
        catch (FileNotFoundException e) {

            Global.Uihandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    //Set the current progress.
                    Toast.makeText(context, "No .dat file found", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        } catch (IOException e) {
            Global.Uihandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    //Set the current progress.
                    Toast.makeText(context, "Reading .dat file Can not read file:", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        }
    }


    /*Function that reads the Kilom_Signs*/
    public Global.posts[] readkmposts(String filename) {

        BufferedReader bufferedReader;
        ArrayList<Global.posts> postses = new ArrayList<>();
        Global.posts[] postses1 = new Global.posts[1];


        double lon,lat;
        int id;

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open("kmposts"+ File.separator + filename+".csv")));
            String read;
            while ((read = bufferedReader.readLine()) != null)
            {
                String[] temp2;

                temp2 = read.split(",");
                lon = Double.parseDouble(temp2[1]);
                lat = Double.parseDouble(temp2[0]);
                id = Integer.parseInt(temp2[2]);

                postses.add(new Global.posts(lat,lon,id));
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        postses1 = postses.toArray(postses1);
        postses.clear();

        return postses1;
    }

    /*Function that reads the Stations*/
    public Global.station[] readstations(String route) {

        BufferedReader bufferedReader;

        ArrayList<Global.station> stations = new ArrayList<>();
        Global.station[] stations_r = new Global.station[1];


        try {
            bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open("stations" + File.separator + route+".csv")));
            String read;

            while((read = bufferedReader.readLine()) != null){

                String[] temp = read.split(",");
                String[] temp2 = temp[3].split("\\+");
                String[] temp3 = temp2[1].split(" ");
                int temp_dist = Integer.parseInt(temp2[0])*1000 + Integer.parseInt(temp3[temp3.length-1]);

                stations.add(new Global.station(Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), temp[2], temp_dist));
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        stations_r = stations.toArray(stations_r);
        stations.clear();

        return stations_r;
    }


}