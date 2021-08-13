package com.juphoon.chatbotmaap.tcl;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

public class ceshi {


    final File file = new File("./", "test.pcm");
    static   FileOutputStream os = null;
    public void init() throws IOException {

        int [] freqs= {1000,2000,3000,4000,6000,8000,250,500};// 频率
        int [] dbs = {0,5,10,15,20,25,30,35,40,45,50,55,60,65,70};// %95dB还在表数范围，100dB动态范围就超过了，声压级
        int M = freqs.length;
        int N = dbs.length;
        int fs = 48000;//采样率

        try {
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N ; j++) {
                int f = freqs[i];
                int dB = dbs[j];
                double [] y = sig_gen2(f,dB,fs);
                String outname = "sin_calibration"+String.valueOf(f)+"_"+String.valueOf(dB)+"dB_gen2_48000.wav";
                byte [] result = asByteArray(y);
                os.write(result);

//                File temp = File.createTempFile("KenTo", "wav", new File("./")); //生成临时文件
//                temp.deleteOnExit();
//                FileOutputStream fos = new FileOutputStream(temp);
//                fos.write(result);
//                fos.close();
            }
        }

    }

    public static byte[] asByteArray(double[] input){
        if(null == input ){
            return null;
        }
        return asByteBuffer(DoubleBuffer.wrap(input)).array();
    }
    public static ByteBuffer asByteBuffer(DoubleBuffer input){
        if(null == input ){
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(input.capacity()* (Double.SIZE/8));
        while(input.hasRemaining()){
            buffer.putDouble(input.get());
        }
        return buffer;
    }

    public static double [] sig_gen2(int f, int dB, int fs) {
        int [] freqs = {250,500,1000,2000,3000,4000,6000,8000};
        double [][] p ={{0.8394 , 12.8384},
                {0.8463  , 15.4576},
                {0.8476  ,17.8610},
                {1.0114   ,15.9365},
                {1.0038  ,  7.2996},{
                0.9459  ,  5.1563}};
        int ichannel = 1;
        for (int i = 0; i < 6; i++) {
            if (f ==freqs[i]){
                ichannel = i;
                break;
            }
        }
        double dB_origin = dB/p[ichannel][0] - p[ichannel][1]/p[ichannel][0];
        double time = 3.5;
      //  System.out.println("dB_origin----:"+dB_origin);
        int s = (int) (time*fs)+1;
        double []t =new double[s];
        double [] x = new double[s];
        double n = 0.0;
        double sum = 0.0;
        for (int i = 0; i < s; i++) {
            t[i] = n ;
            x [i] = Math.sin(t[i]* 2 * Math.PI * f *n);
            n = n + 1/(double)fs;
            sum = t[i] * t[i] + sum;
        }

        double p0 = 0.00002;
        int N = t.length;
        double [] x_power = new double[N];
        for (int i = 0; i < N; i++) {
            double re = 1/(double)N *sum;
            x_power[i] = Math.sqrt(re);

        }
        double [] r =new double[N];
        for (int i = 0; i <N ; i++) {
            r[i] = p0*Math.pow(10,dB_origin/20)/x_power[i];
        }
        double [] y = new double[N];
        for (int i = 0; i < N ; i++) {
            y[i] = r[i]*x[i];
        }
        return y;
    }

}
