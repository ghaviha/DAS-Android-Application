/*
* This class handles the location inputs(as a thread) when the testing is active
*/

package stream.sics.streamdas;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Testing implements Runnable {

    int test_index = 0;

    Context context;

    /*This is the tag which helps the user to identify where the messages in the logcat is coming from*/
    private static String TAG = "Testing";

    /*This imports the context from the UI thread*/
    public Testing(Context context){
        this.context = context;
    }
    public void run() {


        BufferedReader bufferedReader;

        ArrayList<Double> lat = new ArrayList<>();
        ArrayList<Double> lon = new ArrayList<>();
        ArrayList<Float> vel = new ArrayList<>();
        ArrayList<Long> time = new ArrayList<>();
        InputStream file;
        try {
            /*This identifies and creates an inputstream to the corresponding simulation files*/
            switch (Global.dat_Message) {
                case "Västerås, Kolbäck, AV1 20%":
                    file = context.getAssets().open("simulation" + File.separator + "gpspunkter_v-k.csv");
                    break;
                case "Västerås, Kolbäck, AV2 40%":
                    file = context.getAssets().open("simulation" + File.separator + "gpspunkter_v-k2.csv");
                    break;
                case "Västerås, Kolbäck, AV3 60%":
                    file = context.getAssets().open("simulation" + File.separator + "gpspunkter_v-k3.csv");
                    break;
                case "Kolbäck, Västerås, AV1 20%":
                    file = context.getAssets().open("simulation" + File.separator + "gpspunkter_k-v.csv");
                    break;
                case "Kolbäck, Västerås, AV2 40%":
                    file = context.getAssets().open("simulation" + File.separator + "gpspunkter_k-v2.csv");
                    break;
                case "Kolbäck, Västerås, AV3 60%":
                    file = context.getAssets().open("simulation" + File.separator + "gpspunkter_k-v3.csv");
                    break;
                default:
                    Global.Uihandler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(context, "Kilom file not found", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
            }



            bufferedReader = new BufferedReader(new InputStreamReader(file));
            String read;

            /*Reads the gps coordinates from the file*/
            while((read = bufferedReader.readLine()) != null){

                String[] temp = read.split(",");

                lat.add(Double.parseDouble(temp[0]));
                lon.add(Double.parseDouble(temp[1]));
                vel.add(Float.parseFloat(temp[2])/(float)3.6);
                time.add(Long.parseLong(temp[3]));
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[] testlat = new double[lat.size()];
        double[] testlong = new double[lon.size()];
        float[] testvel = new float[vel.size()];
        long[] testtime = new long[time.size()];

        /*Puts the data in arrays*/
        for (int i = 0; i < lat.size(); i++) {
            testlat[i] = lat.get(i);
            testlong[i] = lon.get(i);
            testvel[i] = vel.get(i);
            testtime[i] = time.get(i);
        }

        while (Global.running) {

            Location testingloc = new Location("hej");

            /*Fills the list with the locations*/
            if (test_index < testtime.length) {

                testingloc.setLongitude(testlong[test_index]);
                testingloc.setLatitude(testlat[test_index]);
                testingloc.setTime(testtime[test_index]);
                testingloc.setSpeed(testvel[test_index]);

                try {
                    Global.SEMAPHORE.acquire();
                    Global.loc_List.add(testingloc);



                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Global.SEMAPHORE.release();
                }
                test_index++;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
