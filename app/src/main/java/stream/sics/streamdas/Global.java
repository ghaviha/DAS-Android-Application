/*
* This java file contains all the global variables that needs to be accessed from different
* parts of the program. These should not be touch with the exception of testing which can be
* changed when the user wants to swap from testing mode to real mode. testing = true will
* activate the testing, while testing = false will let the application run its real time implementation
*/

package stream.sics.streamdas;

import android.app.ProgressDialog;
import android.location.Location;
import android.os.Handler;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import cern.colt.matrix.impl.DenseObjectMatrix3D;

public class Global {

    /*This is the semaphore that protects the location database*/
    final static public Semaphore SEMAPHORE = new Semaphore(1,true);

    /*These booleans states what view is active*/
    static boolean mainview_active = false;
    static boolean select_active = false;

    /*Boolean which is set to true if testing is desired and false if real runtime is desired*/
    static boolean testing = false;

    /*Declare a class which will handle the google API*/
    static GoogleApi Ggle;

    /*Variable that holds the firts onlinecalculation*/
    static trainOutput first;

    /*A class which holds the trainData*/
    public static class trainData {

        double tS, xS, TTime, TDistance, Max_speedR, TMass, vS, minusT, plusT, Arr, Brr, Crr, ACMPower, MaxTrac, MaxBrake, BRPoint, tstep, xstep, vstep, plusTstep, NoT, NoX, NoT2;

        double[] SpeedL, Elevations;

        DenseObjectMatrix3D Vop;
    }

    /*The global debugging TAG which shows where the error messages comes from*/
    static String debug_TAG = "StreamDAS,";

    /*Creation of a trainData class*/
    static trainData train;

    /*A class which holds the output from onlineCalculations*/
    public static class trainOutput {

        double[] V, X;
        int DT;

        public trainOutput(double[] v, double[] x, int dt) {
            V = v;
            X = x;
            DT = dt;
        }
    }

    /*A class which holds the data for the posts that is used to correct errors in distance*/
    public static class posts{
        double lat, lon;
        int id;

        public posts(double LAT, double LON, int ID) {
            this.lat = LAT;
            this.lon = LON;
            this.id = ID;
        }
    }

    /*A class which holds the coordinates and distances between stations on the route*/
    public static class station{
        double lat, lon;
        int distance;
        String id;

        public station(double LAT, double LON, String ID, int DISTANCE) {
            this.lat = LAT;
            this.lon = LON;
            this.id = ID;
            this.distance = DISTANCE;
        }
    }

    /*A variable to keep the previous location received*/
    public static trainOutput prev_Loc = null;

    /*A boolean which keeps the runDAS thread alive for the duration of the route*/
    public static boolean running = false;

    /*Creation of classes*/
    static posts[] kilom_Signs;
    static station[] stations;

    /*Declaration of variables that holds the distance and time to the next timestep*/
    static int next_Dist;
    static int next_Time;

    /*Variables that holds the limits in the plots domain x-axis*/
    static double plot_Domain_Min;
    static double plot_Domain_Max;

    /*Class that holds the coordinates to the posts used in the distance error correction*/
    public static class temp_Kilom_Sign_Loc {
        Location post, post2;

        public temp_Kilom_Sign_Loc(Location SIGN, Location SIGN2) {
            this.post = SIGN;
            this.post2 = SIGN2;
        }
    }

    /*String that holds the name of the .dat file to be used*/
    static String dat_Message;

    /*Variable that holds the timestamp of the log files*/
    static long timeStamp;

    /*Strings that holds the names of the logfiles that the data is written to*/
    static String onlineCalculation_log, Location_log, Location_log2, Location_log3;

    /*Creation of class*/
    static temp_Kilom_Sign_Loc temp_Signs = new temp_Kilom_Sign_Loc(new Location("mGoogleApiClient"),new Location("mGoogleApiClient"));

    //Arraylist of locations
    final static public ArrayList<Location> loc_List = new ArrayList<Location>();

    /*ArrayLists that holds the values of distance and velocity travelled*/
    final static public ArrayList<Number> travelled_X = new ArrayList<Number>();
    final static public ArrayList<Number> travelled_Y = new ArrayList<Number>();

    /*Declaration of variables that tells the position line on the plot where to begin*/
    static double position_X1;
    static double position_X2;

    /*Offset from the station to the first post*/
    static double offset = 0;

    /*Iterator that keeps track on which post is the next one*/
    static int csv_iterator;

    /*The textviews in the UI*/
    public static TextView ntime,update,update2,update3,update4,batt;

    /*Keeps track of the battery consumption*/
    public static double E_Total;

    /*Keep track of the last timestep's X*/
    public static int last_X;

    /*Declaration of the plot*/
    public static XYPlot plot;

    /*Declaration of the XYseries travelled used in the plot*/
    public static XYSeries travelled;

    /*Declaration of the handler connected to the UI thread*/
    public static Handler Uihandler;

    /*String the holds the filename of the dat-file to be read*/
    public static String filename;

    /*Declaration of the progressdialog that show the progress of reading the dat-file*/
    public static ProgressDialog progressDialog;
}
