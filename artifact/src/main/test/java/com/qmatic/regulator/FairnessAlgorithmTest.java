package com.qmatic.regulator;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;


public class FairnessAlgorithmTest {

    Logger LOGGER = LoggerFactory.getLogger(FairnessAlgorithmTest.class.getName());

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
        assertThat(alg.computeVirtualQueueAverage(), is(1.0));
        map.put(2, 5);
        assertThat(alg.computeVirtualQueueAverage(), is(1.5));
        map.put(3, 5);
        assertThat(alg.computeVirtualQueueAverage(), is(2.0));
    }

    @Test
    public void testIsOverAverage() throws Exception {
        int HWM = 400, LWM = 200, AM = 300;
        double INITIAL_TCR = 8;
        int numberOfFinishedJobs = 0;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        HashMap<Integer, Integer> map = alg.getNumberOfRequestsPerRetryInTheVirtualQueue();
        alg.setNumberOfReleasedTokens(300);
        map.put(1, 200);
        map.put(2, 50);
        map.put(3, 100);
        map.put(4, 1);
        addFiftyRequests(numberOfFinishedJobs, alg, true, 2);
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

        jc = alg.runAlgorithm(numberOfFinishedJobs, 1, LOGGER);
        assertThat(jc.toJSONString().contains("AccessService"), is(true));

        jc = alg.runAlgorithm(numberOfFinishedJobs, 1, LOGGER);
        assertThat(jc.toJSONString().contains("ScheduleMessage"), is(true));

        // Active clients is 2
        // Testing Prio 2, above average
        map.put(1, 5);
        map.put(2, 5);
        map.put(3, 2);

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

    @Test
    public void testIfFillsUpToHWM() throws Exception{
        int HWM = 400, LWM = 0, AM = 200;
        double INITIAL_TCR = 8;
        int numberOfFinishedJobs = 0;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        JSONObject jc;

        addHundredRequests(numberOfFinishedJobs, alg, true, 0);
        assertThat(alg.getNumberOfReleasedTokens(), is(100L));

        // Virtual queue level is 0
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        assertThat(alg.getNumberOfReleasedTokens(), is(100L));

        // Virtual queue level is 100
        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        assertThat(alg.getNumberOfReleasedTokens(), is(200L));

        // Virtual queue level is 0, 200 in
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        assertThat(alg.getNumberOfReleasedTokens(), is(200L));

        // Virtual queue level is 200
        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        assertThat(alg.getNumberOfReleasedTokens(), is(300L));

        // Virtual queue level is 100, 200 in
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        assertThat(alg.getNumberOfReleasedTokens(), is(300L));

        // Virtual queue level is 200, average is 1.5, 200 in
        assertThat(alg.isTopPrioritized(1), is(true));
        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        assertThat(alg.getNumberOfReleasedTokens(), is(400L));
    }

    @Test
    public void testCorrectPriorities() throws Exception{
        int HWM = 4, LWM = 0, AM = 2;
        double INITIAL_TCR = 8;
        int numberOfFinishedJobs = 0;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        addRequests(1, 0, alg, true, 0);
        addRequests(1, 0, alg, false, 0);
        addRequests(1, 0, alg, false, 0);
        addRequests(1, 0, alg, false, 0);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(1), is(3));
        addRequests(1, 0, alg, true, 1);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(1), is(2));
        addRequests(1, 0, alg, true, 1);
        assertThat(alg.getNumberOfRequestsPerRetryInTheVirtualQueue().get(1), is(1));
        assertThat(alg.getNumberOfReleasedTokens(), is(3L));
        addRequests(1, 0, alg, true, 1);
        assertThat(alg.getNumberOfReleasedTokens(), is(4L));

    }

    @Test
    public void testOnLargeScale() throws  Exception{
        int HWM = 400, LWM = 0, AM = 200;
        double INITIAL_TCR = 8;
        int numberOfFinishedJobs = 0;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        addHundredRequests(numberOfFinishedJobs, alg, true, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        addHundredRequests(numberOfFinishedJobs, alg, false, 1);
        numberOfFinishedJobs = 100;
        addHundredRequests(numberOfFinishedJobs, alg, false, 1);
        addHundredRequests(numberOfFinishedJobs, alg, true, 2);
    }

    @Test
    public void testOnLargeScaleOtherParameters() throws  Exception{
        int HWM = 400, LWM = 200, AM = 300;
        double INITIAL_TCR = 8;
        int numberOfFinishedJobs = 0;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, true));
        addHundredRequests(numberOfFinishedJobs, alg, true, 0);
        addHundredRequests(numberOfFinishedJobs, alg, true, 0);
        addFiftyRequests(numberOfFinishedJobs, alg, true, 0);
        // 250

        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addHundredRequests(numberOfFinishedJobs, alg, false, 0);
        addFiftyRequests(numberOfFinishedJobs, alg, true, 1);
        addFiftyRequests(numberOfFinishedJobs, alg, true, 1);
        addFiftyRequests(numberOfFinishedJobs, alg, true, 1);
        // 400
//        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
//        addHundredRequests(numberOfFinishedJobs, alg, true, 1);
        addHundredRequests(numberOfFinishedJobs, alg, false, 1);
        numberOfFinishedJobs = 100;
        addHundredRequests(numberOfFinishedJobs, alg, false, 1);
        addFiftyRequests(numberOfFinishedJobs, alg, true, 2);
    }

    private void addHundredRequests(int numberOfFinishedJobs, SecondVersionAlgorithm alg, boolean accessService, int numberOfTries) {
        addRequests(100, numberOfFinishedJobs, alg, accessService, numberOfTries);
    }

    private void addFiftyRequests(int numberOfFinishedJobs, SecondVersionAlgorithm alg, boolean accessService, int numberOfTries) {
        addRequests(50, numberOfFinishedJobs, alg, accessService, numberOfTries);
    }


    private void addRequests(int numberOfRequests, int numberOfFinishedJobs, SecondVersionAlgorithm alg, boolean accessService, int numberOfTries) {
        JSONObject jc;
        for (int i = 0; i < numberOfRequests; i++) {
            jc = alg.runAlgorithm(numberOfFinishedJobs, numberOfTries, LOGGER);
            if(accessService) {
                assertThat(jc.toJSONString().contains("AccessService"), is(true));
            }
            else {
                assertThat(jc.toJSONString().contains("ScheduleMessage"), is(true));
            }
        }
    }
}