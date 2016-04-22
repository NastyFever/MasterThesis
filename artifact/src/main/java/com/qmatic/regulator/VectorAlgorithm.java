package com.qmatic.regulator;

import java.util.ArrayList;

public class VectorAlgorithm extends SecondVersionAlgorithm {

    int[] queues = {0, 0, 0, 0, 0, 0, 0, 0};


    public VectorAlgorithm(int LWM, int HWM, int AM, double initialTCR){
        super(LWM, HWM, AM, initialTCR);
    }

    @Override
    public double getReturntime() {
        ArrayList<Integer> queueIds = computeQueues(estimatedTaskCompletionRatePerMillis);
        return 0;
    }

    protected ArrayList<Integer> computeQueues(double wantedComebackRate) {
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
