package com.qmatic.regulator;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;


public class FairnessAlgorithmTest {

    @Test
    public void testDecrementMapFor() throws Exception {
        int HWM = 400, LWM = 200, AM = 300;
        double INITIAL_TCR = 8;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        HashMap<Integer, Integer> map = alg.getNumberOfRequestsPerRetryInTheVirtualQueue();
        map.put(1, 2);
        map.put(2, 3);
        map.put(3, 4);
        alg.decrementMapFor(2);
        assertThat(map.get(2), is(2));
        alg.decrementMapFor(2);
        assertThat(map.get(2), is(1));
        alg.decrementMapFor(2);
        assertThat(map.containsKey(2), is(false));
    }

    @Test
    public void testComputeVirtualQueueAverage() throws Exception {
        int HWM = 400, LWM = 200, AM = 300;
        double INITIAL_TCR = 8;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        HashMap<Integer, Integer> map = alg.getNumberOfRequestsPerRetryInTheVirtualQueue();
        map.put(1, 5);
        alg.setNumberOfClientsInTheVirtualQueue(new AtomicLong(5));
        assertThat(alg.computeVirtualQueueAverage(), is(1.0));
        map.put(2, 5);
        alg.setNumberOfClientsInTheVirtualQueue(new AtomicLong(10));
        assertThat(alg.computeVirtualQueueAverage(), is(1.5));
        map.put(3, 5);
        alg.setNumberOfClientsInTheVirtualQueue(new AtomicLong(15));
        assertThat(alg.computeVirtualQueueAverage(), is(2.0));
    }

    @Test
    public void testIncrementMapFor() throws Exception {
        int HWM = 400, LWM = 200, AM = 300;
        double INITIAL_TCR = 8;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        alg.incrementMapFor(1);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(2), is(1));
        alg.incrementMapFor(1);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(2), is(2));
        alg.incrementMapFor(1);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(1 + 1), is(3));
        alg.incrementMapFor(1);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(1 + 1), is(4));
    }

    @Test
    public void testIncrementAndDecrement() throws Exception {
        int HWM = 400, LWM = 200, AM = 300;
        double INITIAL_TCR = 8;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        alg.incrementMapFor(1);
        alg.incrementMapFor(1);
        alg.incrementMapFor(1);
        alg.incrementMapFor(7);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(8), is(1));
        alg.decrementMapFor(8);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().containsKey(8), is(false));
    }

    @Test
    public void testRunAlgorithm() throws Exception {
        int HWM = 4, LWM = 0, AM = 2;
        double INITIAL_TCR = 8;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));

        Logger LOGGER = LoggerFactory.getLogger(FairnessAlgorithmTest.class.getName());
        long numberOfFinishedJobs = 0;
        HashMap<Integer, Integer> map = alg.getNumberOfRequestsPerRetryInTheVirtualQueue();
        map.put(1, 5);
        map.put(2, 5);
        map.put(3, 5);
        alg.setNumberOfClientsInTheVirtualQueue(new AtomicLong(15));


        // Average is 2.0, active clients is 0
        // Testing Free go
        JSONObject jc = alg.runAlgorithm(numberOfFinishedJobs, 0, LOGGER);
        assertThat(jc.toJSONString().contains("AccessService"), is(true));

        jc = alg.runAlgorithm(numberOfFinishedJobs, 0, LOGGER);
        assertThat(jc.toJSONString().contains("ScheduleMessage"), is(true));

        // Active clients is 1
        // Testing Prio 3
        map.put(1, 5);
        map.put(2, 5);
        map.put(3, 5);
        alg.setNumberOfClientsInTheVirtualQueue(new AtomicLong(15));

        jc = alg.runAlgorithm(numberOfFinishedJobs, 1, LOGGER);
        assertThat(jc.toJSONString().contains("AccessService"), is(true));

        jc = alg.runAlgorithm(numberOfFinishedJobs, 1, LOGGER);
        assertThat(jc.toJSONString().contains("ScheduleMessage"), is(true));

        // Active clients is 2
        // Testing Prio 2, above average
        map.put(1, 5);
        map.put(2, 5);
        map.put(3, 2);

        alg.setNumberOfClientsInTheVirtualQueue(new AtomicLong(15));
        jc = alg.runAlgorithm(numberOfFinishedJobs, 2, LOGGER);
        assertThat(jc.toJSONString().contains("AccessService"), is(true));

        jc = alg.runAlgorithm(numberOfFinishedJobs, 2, LOGGER);
        assertThat(jc.toJSONString().contains("ScheduleMessage"), is(true));

        // Active clients is 3
        // Testing Prio 1
        jc = alg.runAlgorithm(numberOfFinishedJobs, 3, LOGGER);
        assertThat(jc.toJSONString().contains("AccessService"), is(true));

        // Active clients is 4
        // Test that we don't surpass HWM
        jc = alg.runAlgorithm(numberOfFinishedJobs, 3, LOGGER);
        assertThat(jc.toJSONString().contains("ScheduleMessage"), is(true));
    }
}