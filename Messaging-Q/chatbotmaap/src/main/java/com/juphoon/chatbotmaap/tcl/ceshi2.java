package com.juphoon.chatbotmaap.tcl;

import android.util.Log;

public class ceshi2 {

    int [] dBs = {10,15,20,17,25,30};

    public void second(){
        int sum = 0;
        for (int i = 1; i <5 ; i++) {
            sum+=dBs[i];
        }
        int mean_hearing = sum/4;

        int dB = mean_hearing;

        if(dB <=25){
            Log.i("mean_hearing","听力正常");
        }else if(dB<=40){
            Log.i("mean_hearing","轻度听损");
        }else if(dB<=60) {
            Log.i("mean_hearing","中度听损");
        }else if(dB<=80) {
            Log.i("mean_hearing","重度听损");
        }else{
            Log.i("mean_hearing","极重度听损");
        }
    }

    public double[] thrid(){

        double [] dbs_age18 = {3.8,0.8,-1.5,-4.0,-3.8,1.4};
        double [] dbs_meadian =  new double[dbs_age18.length];

        for (int i = 0; i <dbs_age18.length ; i++) {
            dbs_meadian[i] = dbs_age18[i]+20;
        }
        double dB_dev_500_1000  = (dBs[0]-dbs_meadian[0]+dBs[1]-dbs_meadian[1])/2;
        double dB_dev_2000_3000   = (dBs[2]-dbs_meadian[2]+dBs[3]-dbs_meadian[3])/2;
        double dB_dev_4000_6000    = (dBs[4]-dbs_meadian[4]+dBs[5]-dbs_meadian[5])/2;

        double [] result = new double[3];
        result[0] = dB_dev_500_1000;
        result[1] = dB_dev_2000_3000;
        result[2] = dB_dev_4000_6000;
        return result;
    }
}
