package com.qmatic.regulator;

import java.util.ArrayList;
import java.util.HashMap;

public class VectorAlgorithm extends SecondVersionAlgorithm {

    long[] queues = {0, 0, 0, 0, 0, 0, 0, 0};

    public HashMap<Integer, Integer> lookUpMap = new HashMap<>();

    public VectorAlgorithm(int LWM, int HWM, int AM, double initialTCR, boolean fairness){
        super(LWM, HWM, AM, initialTCR, fairness);
        buildLookUpMap();
    }

    private void buildLookUpMap() {
        for(int i = 0; i < queues.length; i++) {
            lookUpMap.put((int) Math.pow(2, i), i);
        }
    }

    @Override
    public double getReturntime() {
        ArrayList<Integer> queueIds = computeWhatQueuesToFill(estimatedTaskCompletionRatePerMillis);
        long relativeWaitTime = placeInVirtualQueueAndGetWaitTime(queueIds);
        return relativeWaitTime;
    }

    protected synchronized long placeInVirtualQueueAndGetWaitTime(ArrayList<Integer> queueIds) {
        long currentTime = getCurrentTimeInMillis();
        // First check if any of the queues is empty, if so return fastest queue
        for(int queueRate : queueIds) {
            long queueEndTime = queues[lookUpMap.get(queueRate)];
            if (queueEndTime < currentTime) {
                queues[lookUpMap.get(queueRate)] = currentTime
                        + convertRateFromPerSecondToIntervalInMillis(queueRate);
                return convertRateFromPerSecondToIntervalInMillis(queueRate);
            }
        }
        // Second, if non of the queues where empty, add to the shortest
        long compareTo = Long.MAX_VALUE;
        int choosedQueueRate = -1;
        for(int queueRate : queueIds) {
            long queueEndTime = queues[lookUpMap.get(queueRate)];
            if(compareTo > queueEndTime) {
                compareTo = queueEndTime;
                choosedQueueRate = queueRate;
            }
        }
        queues[lookUpMap.get(choosedQueueRate)] += convertRateFromPerSecondToIntervalInMillis(choosedQueueRate);
        return queues[lookUpMap.get(choosedQueueRate)] - currentTime;
    }

    private int convertRateFromPerSecondToIntervalInMillis(int choosedQueueRate) {
        return (int) (1000 * (1.0/choosedQueueRate));
    }

    protected long getCurrentTimeInMillis() {
        return System.currentTimeMillis();
    }

    protected ArrayList<Integer> computeWhatQueuesToFill(double wantedComebackRate) {
        ArrayList<Integer> whatQueuesToPick = new ArrayList<>();
        int rest = (int) Math.ceil(1000*wantedComebackRate); // aimed value for return rate

        for(int i = queues.length-1; i >= 0; i--){
            double quotient = rest / Math.pow(2, i);
            if (quotient >= 1){
               whatQueuesToPick.add((int) Math.pow(2, i));
                rest -= Math.pow(2,i);
            }
        }

        return whatQueuesToPick;
    }

}
