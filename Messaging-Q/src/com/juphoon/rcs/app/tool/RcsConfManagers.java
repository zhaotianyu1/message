package com.juphoon.rcs.app.tool;

import com.juphoon.cmcc.lemon.MtcCommonConstants;
import com.juphoon.cmcc.lemon.MtcImConf;

import java.util.ArrayList;
import java.util.List;

public class RcsConfManagers {

    private final static String TAG = "RcsConfManager";
    private static List<String> sListQuery = new ArrayList<String>();
    private static int sLastConfId = -1;

    public static void addConfQuery(String sessionIdentity) {
        synchronized (sListQuery) {
            if (!sListQuery.contains(sessionIdentity)) {
                sListQuery.add(sessionIdentity);
            }
        }
    }

    public static void clearQuery() {
        synchronized (sListQuery) {
            sListQuery.clear();
        }
    }

    public static void query(int confId) {
        if (confId != sLastConfId && confId != -1) {
            return;
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean succ = true;
                synchronized (sListQuery) {
                    if (sListQuery.size() > 0) {
                        String sessionIdentity = sListQuery.get(0);
                        sLastConfId = MtcImConf.Mtc_ImConfMSubsConf(sessionIdentity);
                        sListQuery.remove(0);
                        if (sLastConfId != MtcCommonConstants.INVALIDID) {
                        } else {
                            succ = false;
                        }
                    }
                }
                if (!succ) {
                    query(-1);
                }

            }
        }).start();

    }

}
