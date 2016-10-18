/*
* This java file contains all the calculations that gets its inputs from the gps.
*/
package stream.sics.streamdas;

import android.util.Log;

import java.io.FileWriter;

public class Calculation {

    public static Global.trainOutput onlineCalculations(int inputTime, int inputDistance, int inputVelocity) {

        /*Initialize the steps*/
        Global.train.tstep = Global.train.TTime / (Global.train.NoT - 1);
        Global.train.xstep = Global.train.TDistance/ (Global.train.NoX - 1);
        Global.train.vstep = Global.train.Max_speedR / Global.train.vS;

        /*Tag that lets the programmer know which part of the code the message is sent from*/
        String TAG = "Calculation";

        int plusTstep = (int) Math.round(Global.train.plusT / Global.train.tstep);

        double ACM_energy = (Global.train.ACMPower * Global.train.tstep / 3600.0)*3600000;     // [kW]*[h]*3600000 = [J]

        int Voltage = 750;      // [V] DC for now

        Global.train.NoT2 = Global.train.NoT + plusTstep;

        final double IT = Math.round(inputTime / Global.train.tstep); //Timestep
        double IX = Math.round(inputDistance / Global.train.xstep);    //Dist step
        double IV = Math.round(inputVelocity / Global.train.vstep);     //speed step

        double Power_Acc = Global.train.BRPoint * Global.train.MaxTrac;   //Power at brake point for acceleration
        double Power_Dec = Global.train.BRPoint * Global.train.MaxBrake;   //Power at brake point for deceleration

        double v_avg;
        double v1ms, v2ms, v1kmh, v2kmh;
        double AccEffort, DecEffort;
        double DrivingEnergy = 0;
        double BrakingEnergy = 0;
        double[] DistOp = new double[(int) Global.train.NoT2];
        double[] Eop = new double[(int) Global.train.NoT2];
        double[] Power = new double[(int) Global.train.NoT2];
        double[] Current = new double[(int) Global.train.NoT2];

        double MaxA = Global.train.MaxTrac/Global.train.TMass;                   //[m/s^2]
        double MaxD = Global.train.MaxBrake/Global.train.TMass;                   //[m/s^2]
        //max dv in one time step while decelerating[vstep]
        double Max_dV_d = Math.ceil(((MaxD * Global.train.tstep)*3.6)/Global.train.vstep);
        //max dv in one time step while accelerating[vstep]
        double Max_dV_a = Math.ceil(((MaxA * Global.train.tstep)*3.6)/Global.train.vstep);
        double NoV = Global.train.vS + 1;

        final double[] Effort_percentage = new double[(int) Global.train.NoT2];
        final double[] V = new double[(int) Global.train.NoT2];
        double[] X = new double[(int) Global.train.NoT2];
        double[] Elevation_F = new double[Global.train.Elevations.length];
        double[] FAccOp = new double[(int) Global.train.NoT2];
        double[] FROp = new double[(int) Global.train.NoT2];
        double[] Gop = new double[(int) Global.train.NoT2];
        final double[] EffortOp = new double[(int) Global.train.NoT2];

        double v1_1,v1_M,v1_Lim,delta_x,temp;

        double SoC = 100;
        double Battery_kWh = 800; //[kWh]
        double Battery_J = Battery_kWh * 3600000; // [J]



        V[(int) IT] = IV+1;
        X[(int) IT] = IX+1;



        /*The same loop as in MATlab with the ecxeption that it starts at value 0*/
        for (int t = (int)IT; t < Global.train.NoT2-1; t++) {
//            if(X[t] <= Global.train.NoX) {
            if(X[t] < Global.train.NoX) {
                v1_1 = Max_dV_a * t + 1;
                v1_M = Math.min(v1_1, NoV);
                v1_Lim = Math.min(Global.train.SpeedL[(int)X[t]], v1_M);

                if (V[t] <= v1_Lim) {
                    for (int i = 0; i < Elevation_F.length; i++) {
                        Elevation_F[i] = Global.train.TMass * 10.0 * Global.train.Elevations[i] / 1000.0;
                    }

                    try {
                        V[t + 1] = (short) Global.train.Vop.get((int) V[t] - 1, t, (int) X[t] - 1);

                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                        return Global.prev_Loc;
                    }

//                double constant = 10.0 / 36.0;
                    delta_x = (((V[t] + V[t + 1] - 2)) * Global.train.vstep * (10.0 / 36.0) * Global.train.tstep) / (2.0 * Global.train.xstep);

                    X[t + 1] = X[t] + Math.round(delta_x);

                    FAccOp[t] = Global.train.TMass * ((V[t + 1] - V[t]) * Global.train.vstep * 10.0) / (Global.train.tstep * 36.0);
                    FROp[t] = -(Global.train.Arr + Global.train.Brr * ((V[t] + V[t + 1] - 2) / 2.0) * Global.train.vstep + Global.train.Crr * Math.pow((((V[t] + V[t + 1] - 2) / 2.0) * Global.train.vstep), 2));

                    temp = 0;
                    int i = 0;

                    try {
                        for (i = (int) (X[t] - 1); i <= (int) X[t + 1] - 1; i++) {
                            temp += Elevation_F[i];
                        }
                        temp = temp / (i + 1 - ((int) X[t]));
                        Gop[t] = -(temp);
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        Log.e(TAG, "" + t);
                        temp = temp / (i - ((int) X[t]));
                        Gop[t] = -(temp);
                    }

                    v_avg = ((V[t] + V[t + 1] - 2) / 2.0) * Global.train.vstep;

                    EffortOp[t] = FAccOp[t] - FROp[t] - Gop[t];

                    v1kmh = (V[t] - 1) * Global.train.vstep;
                    if (v1kmh <= Global.train.BRPoint) {
                        AccEffort = Global.train.MaxTrac;
                        DecEffort = Global.train.MaxBrake;
                    } else {
                        AccEffort = Power_Acc / v1kmh;
                        DecEffort = Power_Dec / v1kmh;
                    }

                    if (EffortOp[t] < -(DecEffort)) {
                        EffortOp[t] = -(DecEffort);
                        v1kmh = (V[t] - 1) * Global.train.vstep;
                        v1ms = v1kmh * (10.0 / 36.0);
                        FROp[t] = -(Global.train.Arr + Global.train.Brr * v1kmh + Global.train.Crr * (Math.pow(v1kmh, 2)));
                        Gop[t] = -(Elevation_F[(int) X[t]]);
                        FAccOp[t] = EffortOp[t] + FROp[t] + Gop[t];
                        v2ms = v1ms + (FAccOp[t] / Global.train.TMass) * Global.train.tstep;
                        v2kmh = v2ms * 3.6;
                        V[t + 1] = Math.round(v2kmh / Global.train.vstep) + 1;
                        if (V[t + 1] < 1) {
                            V[t + 1] = 1;
                        }
                        delta_x = (((V[t] + V[t + 1] - 2)) * Global.train.vstep * (10.0 / 36.0) * Global.train.tstep) / (2.0 * Global.train.xstep);
                        X[t + 1] = X[t] + Math.round(delta_x);
                        DistOp[t + 1] = X[t] + delta_x;
                    }

                    if (EffortOp[t] > 0) {
                        Effort_percentage[t] = (EffortOp[t] / AccEffort) * 100.0;
                    } else {
                        Effort_percentage[t] = (EffortOp[t] / DecEffort) * 100.0;
                    }

                    v1ms = ((V[t] - 1) * Global.train.vstep) * (10.0 / 36.0);
                    v2ms = ((V[t + 1] - 1) * Global.train.vstep) * (10.0 / 36.0);

                    if (EffortOp[t] > 0) {
                        Eop[t] = 1.25 * EffortOp[t] * (((v1ms + v2ms) * Global.train.tstep) / 2.0) + ACM_energy;
                    } else {
                        Eop[t] = 0.8 * EffortOp[t] * (((v1ms + v2ms) * Global.train.tstep) / 2.0) + ACM_energy;
                    }
                    Power[t] = Eop[t] / Global.train.tstep;
                    Current[t] = Power[t] / Voltage;
                    if (EffortOp[t] >= 0) {
                        DrivingEnergy = DrivingEnergy + 1.25 * EffortOp[t] * (((v1ms + v2ms) * Global.train.tstep) / 2.0);
                    } else {
                        BrakingEnergy = BrakingEnergy + 0.8 * EffortOp[t] * (((v1ms + v2ms) * Global.train.tstep) / 2.0);
                    }
                } else {
                    v1kmh = (V[t] - 1) * Global.train.vstep;
                    v1ms = v1kmh * (10.0 / 36.0);
                    if (v1kmh <= Global.train.BRPoint) {
                        DecEffort = Global.train.MaxBrake;
                    } else {
                        DecEffort = Power_Dec / v1kmh;
                    }
                    EffortOp[t] = -(DecEffort);
                    Effort_percentage[t] = -100.0;
                    Gop[t] = -(Elevation_F[(int)X[t]]);
                    FROp[t] = -(Global.train.Arr + Global.train.Brr * v1kmh + Global.train.Crr * Math.pow(v1kmh, 2));
                    FAccOp[t] = EffortOp[t] + FROp[t] + Gop[t];
                    v2ms = v1ms + (FAccOp[t] / Global.train.TMass) * Global.train.tstep;
                    v2kmh = v2ms * 3.6;
                    V[t + 1] = Math.round(v2kmh / Global.train.vstep) + 1;
                    delta_x =(((V[t] + V[t + 1] - 2)) * Global.train.vstep * (10.0 / 36.0) * Global.train.tstep) / (2.0 * Global.train.xstep);
                    X[t+1] = X[t] + Math.round(delta_x);
                    DistOp[t+1] = X[t] + delta_x;
                    Eop[t] = 0.8 *  EffortOp[t] * (((v1ms + v2ms) * Global.train.tstep) / 2.0) + ACM_energy;
                    Power[t] = Eop[t] / Global.train.tstep;
                    Current[t] = Power[t] / Voltage;
                    BrakingEnergy = BrakingEnergy + 0.8 *  EffortOp[t] * (((v1ms + v2ms) * Global.train.tstep) / 2);
                }
            } else {
                V[t+1] = 1;
                X[t+1] = Global.train.NoX*Global.train.xstep;
            }
        }

        if(IT > 2) {

            double Battery_dV = inputVelocity - Global.travelled_Y.get(Global.travelled_Y.size() - 1).doubleValue();
            double Battery_dV_ms = Battery_dV / 3.6;
            double Battery_dX = inputDistance - Global.travelled_X.get(Global.travelled_X.size() - 1).doubleValue();
            double Fa = (Battery_dV_ms / Global.train.tstep) * Global.train.TMass;
            double frr = -(Global.train.Arr + Global.train.Brr * Battery_dV + Global.train.Crr * Math.pow(Battery_dV, 2));
            double fg;

            temp = 0.0;
            int j;

            for (j = Global.last_X; j <= (int) X[(int) IT] - 1; j++) {
                temp += Elevation_F[j];
            }
            temp = temp / (j + 1 - Global.last_X);
            fg = -(temp);
            double ft = Fa - frr - fg;
            double E;
            if(ft > Global.train.MaxTrac || ft < -(Global.train.MaxBrake)) {
                E = 0.0;
            } else {
                if (ft >= 0) {
                    E = 1.25 * ft * Battery_dX + ACM_energy;
                } else {
                    E = 0.8 * ft * Battery_dX + ACM_energy;
                }
            }
            Global.E_Total += E;

            SoC = 100 - ((Global.E_Total / Battery_J) * 100);
        }
        final double dSoC = SoC;

        Global.last_X = (int) Math.round(X[(int) IT] - 1);
        for (int i = 0; i < X.length; i++) {
            X[i] = X[i]-1;
            V[i] = V[i]-1;

            X[i] = Math.round(X[i]*Global.train.xstep);
            V[i] = Math.round(V[i]*Global.train.vstep);
        }

        Global.trainOutput output = new Global.trainOutput(V, X, (int) Math.round( (Global.train.tstep *(IT+1))));

        Global.plot_Domain_Max = V[(int)IT]*(60.0/Global.train.tstep);

        if (Global.plot_Domain_Max < 150){
            Global.plot_Domain_Max = 150;
        }
        else if (Global.plot_Domain_Max > 2500){
            Global.plot_Domain_Max = 2500;
        }

        /*This handler makes it possible to update the UI. It sets the recommended and next recommended tractive effort, As
        * well as the next speed*/
        Global.Uihandler.post(new Runnable()
        {
            @Override
            public void run()
            {

                Global.batt.setText(String.format("%d", Math.round(dSoC)));
                Global.batt.append(" %");
                Global.update2.setText(String.format("%d",  Math.round(Effort_percentage[(int) IT])));

                if( IT < Global.train.NoT2-1) {
                    Global.update3.setText(String.format("%d",  Math.round(V[(int) IT+1])));
                    Global.update4.setText(String.format("%d",  Math.round(Effort_percentage[(int) IT+1])));
                }
            }
        });

        try {
            String content = "InputTime: " + inputTime + "\tInputDistance: " + inputDistance + "\tInputVelocity: " + inputVelocity + "\ttime_step: " + (int)(IT);

            FileWriter fw_calc = new FileWriter(Global.onlineCalculation_log, true);
            fw_calc.write(content + "\n");
            fw_calc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
    }
}