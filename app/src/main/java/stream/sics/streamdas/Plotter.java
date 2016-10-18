/*
* This Class contains all the function that the graph library uses to plot
*/
package stream.sics.streamdas;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.View;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.Arrays;

public class Plotter {

    /*The local TAG which helps keep track which java files prints messages in the logcat*/
    private static String TAG = "Plotter";

    /*Declare all the necessary variables and initializes the ones necessary*/
    static Number[] sep1 = {0,0};
    static Number[] sep2 = {-2500,0};
    static Number[] loca1;
    static Number[] loca2;

    static String[] cities;

    static double[] opt_V_first;
    static double[] opt_D_first;

    static Number[] first1;
    static Number[] first2;

    static Number[] posind1 = {-50,50};
    static Number[] posind2 = {0,0};

    static Number[] spdLimN1,spdLimN2;

    static double min;

    static public XYSeries Pos,firsts,speedlimits,elevation,sep,ncurve,loc;

    static public LineAndPointFormatter currentpos,spdlimf,optv,sepf,elevate,next,trav,local;

    static long otime = 0;


    /*This function makes the initial calculation and creates all the static values to be written
    * to the plot.*/
    public static void initPlot(){


        Global.first = Calculation.onlineCalculations(0, 0, 0);

        Global.prev_Loc = Global.first;
        Global.travelled_X.add(0);
        Global.travelled_Y.add(0);
        /************************HERE WE ARE GETTING THE STATIC VALUES FOR THE GRAPH**********************/
                            /*Getting first speed profile, spd limits and elevation*/

        opt_V_first = Global.first.V;
        opt_D_first = Global.first.X;

        first1 = new Number[opt_V_first.length];
        first2 = new Number[opt_V_first.length];

        spdLimN1 = new Number[Global.train.SpeedL.length];
        spdLimN2 = new Number[Global.train.SpeedL.length];

        for (int i = 0; i < Global.first.X.length; i++) {
            first1[i] = opt_V_first[i];
            first2[i] = opt_D_first[i];
        }
        for(int j = 0;j<Global.train.Elevations.length;j++){
            spdLimN1[j] = j*Global.train.xstep;
            spdLimN2[j] = Global.train.SpeedL[j];
        }
        firsts = new SimpleXYSeries(Arrays.asList(first2),
                Arrays.asList(first1), "Org Spd Prof");

        speedlimits = new SimpleXYSeries(Arrays.asList(spdLimN1),
                Arrays.asList(spdLimN2), "Spd Limits");

        Global.next_Dist = (int) Global.first.X[1];
        Global.next_Time = Global.first.DT;

        String next_Iter = Math.round(Global.next_Dist) + " m      " + Math.round(Global.next_Time) + " sec";
        Global.ntime.setText(next_Iter);
        Global.update.setText(Long.toString(otime));
    }

    /*This function sets all the parameters for the graph*/
    public static void setgraphparam() {


        Global.Uihandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if(Global.plot.getLayerType() == View.LAYER_TYPE_NONE){
                    Global.plot.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                } else {
                    Global.plot.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
                }
            }
        });


        Number[] elev;

        Global.plot.getGraphWidget().setMarginTop(0);
        Global.plot.getGraphWidget().setMarginRight(0);
        Global.plot.getGraphWidget().setMarginLeft(0);
        Global.plot.getGraphWidget().setMarginBottom(0);
        Global.plot.setBorderStyle(com.androidplot.Plot.BorderStyle.SQUARE, null, null);

        Global.plot.getGraphWidget().getDomainGridLinePaint().setColor(Color.DKGRAY);
        Global.plot.getGraphWidget().getRangeGridLinePaint().setColor(Color.DKGRAY);

        Global.plot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        Global.plot.getGraphWidget().setRangeValueFormat(
                new DecimalFormat("#"));
        Global.plot.getGraphWidget().setDomainValueFormat(
                new DecimalFormat("#"));

        Global.plot.setUserDomainOrigin(0);
        Global.plot.setUserRangeOrigin(0);

        Global.plot.getLegendWidget().setTableModel(new DynamicTableModel(4, 2));
        Global.plot.getLegendWidget().setSize(new Size(105, SizeLayoutType.ABSOLUTE, 1800, SizeLayoutType.ABSOLUTE));
        Global.plot.getLegendWidget().position(40, XLayoutStyle.ABSOLUTE_FROM_RIGHT,3, YLayoutStyle.ABSOLUTE_FROM_BOTTOM, AnchorPosition.RIGHT_BOTTOM);

        Global.plot.getLegendWidget().refreshLayout();

        Global.plot.setDomainStep(XYStepMode.SUBDIVIDE, 4);

        Paint dashPaint = new Paint();
        dashPaint.setColor(Color.rgb(0,255,255));
        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setStrokeWidth(3);
        dashPaint.setPathEffect(new DashPathEffect(new float[] {10,5}, 0));

        sep2[1] = Global.train.TDistance;

        double max = 0;

        for(int i = 0;i<Global.train.SpeedL.length;i++){
            if(Global.train.SpeedL[i] > max){
                max = Global.train.SpeedL[i];
            }
        }

        max = max/10;
        max = Math.ceil(max);
        max = max*10;


        loca1 = new Number[Global.stations.length];
        loca2 = new Number[Global.stations.length];
        cities = new String[Global.stations.length];

        loca2[0] = 0;
        loca1[0] = max;
        cities[0] = Global.stations[0].id;

        for (int i = 1; i < Global.stations.length-1; i++) {
            loca1[i] = max;
            loca2[i] = Math.abs(Global.stations[i].distance-Global.stations[0].distance);
            cities[i] = Global.stations[i].id;
        }

        loca1[loca1.length-1] = max;
        loca2[loca2.length-1] = Global.train.TDistance;
        cities[cities.length-1] = Global.stations[Global.stations.length-1].id;

        min = max/3;
        Global.plot.setRangeBoundaries(0-min,max, BoundaryMode.FIXED);
        Global.plot.setDomainBoundaries(Global.plot_Domain_Min,Global.plot_Domain_Max,BoundaryMode.FIXED);

        Global.plot.setDomainStep(XYStepMode.SUBDIVIDE, 4);
        elev = eleva();

        elevation = new SimpleXYSeries(Arrays.asList(spdLimN1),Arrays.asList(elev),"Elevation");

        posind1[1] = max;
        posind1[0] = 0-min;
        posind2[0] = Global.position_X1;
        posind2[1] = Global.position_X2;

        Pos = new SimpleXYSeries(Arrays.asList(posind2),Arrays.asList(posind1), "Current Pos");
        sep = new SimpleXYSeries(Arrays.asList(sep2),Arrays.asList(sep1),"");

        /*CHANGE*/
        loc = new SimpleXYSeries(Arrays.asList(loca2),Arrays.asList(loca1),"");
        /**/

        sepf = new LineAndPointFormatter(Color.DKGRAY,null,Color.BLACK,null);
        currentpos = new LineAndPointFormatter(Color.RED,null, null, null);
        spdlimf = new LineAndPointFormatter(Color.rgb(255,171,0),null, null, null);
        optv = new LineAndPointFormatter(Color.YELLOW,null, null, null);
        elevate = new LineAndPointFormatter(Color.DKGRAY,null,Color.rgb(15,112,4),null);

        local = new LineAndPointFormatter(null,Color.WHITE,null,new PointLabelFormatter(Color.WHITE));
        local.setPointLabeler(new PointLabeler() {

            @Override
            public String getLabel(XYSeries series, int index) {

                return cities[index];
            }
        });
        next = new LineAndPointFormatter(dashPaint.getColor(),Color.WHITE,null,null);
        trav = new LineAndPointFormatter(Color.rgb(13,182,171),null,null,null);
        next.setLinePaint(dashPaint);

        Global.plot.addSeries(sepf,sep);
        Global.plot.addSeries(firsts, optv);
        Global.plot.addSeries(speedlimits,spdlimf);

        Global.plot.addSeries(elevation,elevate);
        Global.plot.addSeries(loc,local);
        Global.plot.addSeries(currentpos,Pos);

    }

    /*This function is called everytime the plot needs to be updated, except for the first time*/
    public static void updategraph(Global.trainOutput trOut, double dist ){


        posind2[0] = Global.position_X1 + dist;
        posind2[1] = Global.position_X2 + dist;

        Number[] travX = new Number[Global.travelled_X.size()];
        Number[] travY = new Number[Global.travelled_X.size()];

        for(int i = 0; i<Global.travelled_X.size(); i++) {
            travX[i] = Global.travelled_X.get(i);
            travY[i] = Global.travelled_Y.get(i);
        }

        Global.plot.removeSeries(Pos);
        Global.plot.removeSeries(Global.travelled);
        Global.plot.setDomainBoundaries(Global.plot_Domain_Min + dist,Global.plot_Domain_Max +dist, BoundaryMode.FIXED);
        Pos = new SimpleXYSeries(Arrays.asList(posind2),Arrays.asList(posind1), "Current Pos");
        Global.travelled = new SimpleXYSeries(Arrays.asList(travX),Arrays.asList(travY),"Trvld Pos");

        Global.Uihandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if(Global.plot.getLayerType() == View.LAYER_TYPE_NONE){
                    Global.plot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                } else {
                    Global.plot.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
                }
            }
        });


        /*This is only performed when we get a new point calculated*/
        if (trOut != null){
            int j = 0;
            int iter = 0;

            while(j<trOut.V.length-1){
                if(trOut.X[j] >dist){
                    iter++;
                }
                j++;
            }

            Number[] tro = new Number[iter+1];
            Number[] tro2 = new Number[iter+1];
            tro2[0] = dist;
            tro[0] = travY[travY.length-1];

            for(int i = 0;i<iter;i++) {
                tro[i+1] = trOut.V[j-iter+i];
                tro2[i+1] = trOut.X[j-iter+i];
            }

            Global.plot.removeSeries(ncurve);

            if(tro2[tro2.length-1].intValue() != 0) {
                ncurve = new SimpleXYSeries(Arrays.asList(tro2), Arrays.asList(tro), "New Spd prof");
            }
        }
        if (ncurve!= null && Global.running) {
            Global.plot.addSeries(ncurve, next);
        }
        Global.plot.addSeries(currentpos,Pos);
        Global.plot.addSeries(Global.travelled,trav);
        if (!Global.running){
            Global.plot.removeSeries(ncurve);
            Global.plot.removeSeries(loc);
        }
    }

    /*This function calculates the elevation in all the points*/
    static Number[] eleva() {
        double[] elevation_height = new double[Global.train.Elevations.length];
        Number[] nelevation_height = new Number[Global.train.Elevations.length];

        double a;

        elevation_height[0] = 0;
        for (int i = 0; i < Global.train.Elevations.length; i++) {
            if (i != 0) {

                a = Math.atan(Global.train.Elevations[i]/1000);
                elevation_height[i] = elevation_height[i - 1] + Math.tan(a)*Global.train.xstep;
            }
        }

        double[] temp_arr = elevation_height.clone();
        Arrays.sort(temp_arr);

        double temp_max = temp_arr[temp_arr.length-1];
        double temp_min = temp_arr[0];

        double upperbound = (min/2.0)/temp_max;
        double lowerbound = (min/2.0)/Math.abs(temp_min);

        for (int i = 0; i < nelevation_height.length; i++) {
            nelevation_height[i] = (elevation_height[i]*Math.min(Math.min(lowerbound, upperbound),1.5))-(min/2.0);
        }


        return nelevation_height;
    }

}