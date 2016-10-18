/*
* This java file contains the runnable thread which handles all the calculations and sends the data to the
* plotter when GPS data is received
*/

package stream.sics.streamdas;

import android.location.Location;

import java.io.FileWriter;
import java.io.IOException;

public class runDas implements Runnable {

    /*Logcat TAG which helps identify where the messages is coming from*/
    private static String TAG = "runDas";

    static double totdist;
    static double tottime;

    int disti;

    double curr_Speed;
    /*This decides how close to the kilometer signs, which is used in the distance correcting algorithm,
    * the locations must be to be applied in the correcting algorithm [m]*/
    int proximity = 50;

    int last_size = 100000;



    public void run(){

        totdist = 0;
        tottime = 0;

        if(!Global.testing) {
            try {
                Global.SEMAPHORE.acquire();

            /*Sets the first location to the station and sets the timestamp to the current time
            * [ms since january 1 1970]*/
                Location first = new Location("first");
                first.setTime(System.currentTimeMillis());
                first.setLatitude(Global.stations[0].lat);
                first.setLongitude(Global.stations[0].lon);
                first.setSpeed(0);

                String content = "Starting Location, " + first.getLatitude() + "," + first.getLongitude() + ",\tVel: " + first.getSpeed() * 3.6 + ",\tTime: " + first.getTime() + ",\tAcc: " + first.getAccuracy();

            /*Writes location to log*/
                FileWriter fw_loc;
                try {
                    fw_loc = new FileWriter(Global.Location_log, true);
                    fw_loc.write(content + "\n");

                    fw_loc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            /*Clear the location list and enters the first location*/
                Global.loc_List.clear();
                Global.loc_List.add(first);


            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                Global.SEMAPHORE.release();
            }

        }
        /*Variable which keeps track on how far we've travelled*/
        double odist = 0;

        Global.running = true;
        double Trip_Distance = Global.train.TDistance, Trip_Time = Global.train.TTime;


        while (Global.running) {

            final Global.trainOutput next_point;

            if (Global.loc_List.size() > 1) {

                distCalc();

                /*If we've travelled further or longer in time than the trip should be
                * show the complete graph and stop the program*/
                if (totdist >= Trip_Distance || tottime >= Trip_Time) {

                    if (totdist < Trip_Distance-100){
                        MainView.recalculate();
                    }
                    else {
                        Global.plot_Domain_Min = -50;
                        Global.plot_Domain_Max = Global.train.TDistance + 50;
                        Global.travelled_X.add(totdist);
                        Global.travelled_Y.add(0);
                        Global.position_X1 = totdist;
                        Global.position_X2 = totdist;
                        Global.running = false;

                        Plotter.updategraph(null, 0);

                        break;
                    }
                }

                /*If we have past the next timestep, either in time or distance, calculate a new one.
                * else check if we have travelled further than in the last iteration, if so
                * update the graph*/
                if (totdist > (Global.next_Dist - 1) || tottime > Global.next_Time) {

                    odist = totdist;

                    next_point = Calculation.onlineCalculations((int) Math.round(tottime), (int) Math.round(totdist), (int) curr_Speed);

                    Global.prev_Loc = next_point;

                    Global.travelled_X.add(totdist);
                    Global.travelled_Y.add(curr_Speed);

                    /*Find the next points distance*/
                    disti = 0;
                    while (next_point.X[disti] <= totdist && disti < next_point.X.length-1) {
                        disti++;
                    }

                    /*Set the next points distance and time and print it on the UI*/
                    Global.next_Dist = (int) next_point.X[disti];
                    Global.next_Time = (next_point.DT);

                    /*Global.Uihandler.post is needed to update the UI from another thread*/
                    Global.Uihandler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Global.ntime.setText(Math.round(Global.next_Dist - totdist) + " m      " + Math.round(Global.next_Time - tottime) + " sec");
                            if (Global.travelled_Y.size() != 0) {
                                Global.update.setText(Long.toString(Math.round(Global.travelled_Y.get(Global.travelled_Y.size() - 1).doubleValue())));
                            } else {
                                Global.update.setText("0");
                            }
                        }
                    });

                    Plotter.updategraph(next_point, totdist);

                } else {
                    if (odist < totdist) {

                        odist = totdist;

                        Global.travelled_X.add(totdist);
                        Global.travelled_Y.add(curr_Speed);

                        try {
                            Global.Uihandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Global.ntime.setText(Math.round((Global.next_Dist - totdist)) + " m      " + Math.round((Global.next_Time - tottime)) + " sec");
                                    if (Global.travelled_Y.size() != 0) {
                                        Global.update.setText(Long.toString(Math.round(Global.travelled_Y.get(Global.travelled_Y.size() - 1).doubleValue())));
                                    } else{
                                        Global.update.setText("0");
                                    }

                                }
                            });
                        }
                        catch(ArrayIndexOutOfBoundsException e){
                            e.printStackTrace();

                            return;
                        }

                        Plotter.updategraph(null, totdist);
                    }
                }
            }
            try {
                if (Global.testing) {
                    Thread.sleep(25);
                } else {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /*Function that handles the distance calculation as well as the correction utilizing the kilometer
    * signs from file*/
    public void distCalc(){

        double distancei = 0;
        double timei;

        Location end_station = new Location("stop");
        end_station.setLatitude(Global.stations[Global.stations.length-1].lat);
        end_station.setLongitude(Global.stations[Global.stations.length-1].lon);


        while(last_size == Global.loc_List.size()){
            try {
                if(Global.testing){
                    Thread.sleep(25);
                }
                else {
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            Global.SEMAPHORE.acquire();

            curr_Speed = Global.loc_List.get(Global.loc_List.size()-1).getSpeed()*3.6;
            /*Checks the distance and time between the last 2 locations*/
            if (Global.loc_List.size() > 1) {
                distancei = Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.loc_List.get(Global.loc_List.size() - 1));
            }
            timei = (Global.loc_List.get(Global.loc_List.size()-1).getTime() - Global.loc_List.get(0).getTime());

            timei = timei / 1000;

            /*If we have past the last kilometer sign, just update the distance and time*/
            if (Global.csv_iterator >= Global.kilom_Signs.length) {

                totdist += distancei;
                tottime = timei;

                if (Global.loc_List.get(Global.loc_List.size()-1).distanceTo(end_station) < (30-Global.loc_List.get(Global.loc_List.size()-1).getAccuracy())) {
                    totdist = Global.train.TDistance;
                }

            /*If we are closer to both the next kilometer sign as well as the subsequent sign just update the time and distance*/
            } else if (Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(Global.temp_Signs.post) < Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post) && Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(Global.temp_Signs.post2) < Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post2)) {

                totdist += distancei;
                tottime = timei;

                if (Global.loc_List.get(Global.loc_List.size()-1).distanceTo(end_station) < (30-Global.loc_List.get(Global.loc_List.size()-1).getAccuracy())) {
                    totdist = Global.train.TDistance;
                }


            /*If we are further away from the next kilometer sign as well as to the subsequent one, we are travelling the
            * wrong way, probably a bad location. Therefore we delete this location from the list*/
            } else if (Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(Global.temp_Signs.post) > Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post) && Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(Global.temp_Signs.post2) > Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post2)) {

                Global.loc_List.remove(Global.loc_List.size() - 1);


            /*If we are further away from the next kilometer sign but closer to the subsequent one. We have past it and apply our
            * distance correcting algorithm. After this we update the kilometer signs.*/
            } else if (Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(Global.temp_Signs.post) > Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post) && Global.loc_List.get(Global.loc_List.size() - 1).distanceTo(Global.temp_Signs.post2) < Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post2)) {

                totdist += distancei;
                tottime = timei;



                /*If the previous location is closer to the sign than our proximity variable. Set the location coordinates
                 * to the sign and correct the distance to be the distance from start to the sign + the distance to the
                  * newest location*/
                if (Global.loc_List.get(Global.loc_List.size() - 2).distanceTo(Global.temp_Signs.post) < proximity) {
                    Global.loc_List.get(Global.loc_List.size() - 2).setLatitude(Global.temp_Signs.post.getLatitude());
                    Global.loc_List.get(Global.loc_List.size() - 2).setLongitude(Global.temp_Signs.post.getLongitude());

                    totdist = Global.offset + Global.csv_iterator * 1000 + Global.temp_Signs.post.distanceTo(Global.loc_List.get(Global.loc_List.size() - 1));
                }

                Global.csv_iterator++;

                /*This if structure checks which kilometer sign we've past and updates to new one accordingly*/
                if (Global.csv_iterator < Global.kilom_Signs.length) {
                    Global.temp_Signs.post.setLatitude(Global.kilom_Signs[Global.csv_iterator].lat);
                    Global.temp_Signs.post.setLongitude(Global.kilom_Signs[Global.csv_iterator].lon);
                    if (Global.csv_iterator + 1 < Global.kilom_Signs.length) {
                        Global.temp_Signs.post2.setLatitude(Global.kilom_Signs[Global.csv_iterator + 1].lat);
                        Global.temp_Signs.post2.setLongitude(Global.kilom_Signs[Global.csv_iterator + 1].lon);
                    } else {
                        Global.temp_Signs.post2.setLatitude(Global.stations[Global.stations.length-1].lat);
                        Global.temp_Signs.post2.setLongitude(Global.stations[Global.stations.length-1].lon);
                    }
                /*If we've past the last kilometer sign we set them to be the destination station*/
                } else {
                    Global.temp_Signs.post.setLatitude(Global.stations[Global.stations.length-1].lat);
                    Global.temp_Signs.post.setLongitude(Global.stations[Global.stations.length-1].lon);
                    Global.temp_Signs.post2.setLatitude(Global.stations[Global.stations.length-1].lat);
                    Global.temp_Signs.post2.setLongitude(Global.stations[Global.stations.length-1].lon);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            Global.SEMAPHORE.release();
            last_size = Global.loc_List.size();

        }
    }
}