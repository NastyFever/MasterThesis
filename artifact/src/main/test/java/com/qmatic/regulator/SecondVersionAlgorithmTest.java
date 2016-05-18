package com.qmatic.regulator;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class SecondVersionAlgorithmTest {

    @Test
    public void testGetReturntime() throws Exception {
        int HWM = 400, LWM = 0, AM = 300;
        double INITIAL_TCR = 8;

        SecondVersionAlgorithm alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, false));
        testAddWithoutTCRChange(alg);
        alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, false));
        testAddWhenTCRGoesFromSlowToFast(alg);
        alg = Mockito.spy(new SecondVersionAlgorithm(LWM, HWM, AM, INITIAL_TCR, false));
        testAddWhenTCRGoesFromSlowToFastToSlow(alg);
    }

    private void testAddWithoutTCRChange(SecondVersionAlgorithm alg) {

        Mockito.when(alg.getCurrentTimeInMillis()).thenReturn((long) 0);
        double expectedIntervall = 125.0;
        for (int i = 0; i < 10; i++) {
            assertThat(alg.getReturntime(), is((i+1) * expectedIntervall));
        }
    }

    private void testAddWhenTCRGoesFromSlowToFast(SecondVersionAlgorithm alg) {
        Mockito.when(alg.getCurrentTimeInMillis()).thenReturn((long) 0);
        double expectedIntervall = 125.0;
        for (int i = 0; i < 10; i++) {
            assertThat(alg.getReturntime(), is((i+1) * expectedIntervall));
        }
        alg.estimatedTaskCompletionRatePerMillis = 0.016;
        double queueMiddle = (10 * expectedIntervall) / 2;
        double newIntervall = 125.0 / 2;
        for(int i = 0; i < 10; i++) {
            assertThat(alg.getReturntime(), is((queueMiddle + (i + 1) * newIntervall)));
        }
    }

    private void testAddWhenTCRGoesFromSlowToFastToSlow(SecondVersionAlgorithm alg) {
        Mockito.when(alg.getCurrentTimeInMillis()).thenReturn((long) 0);
        double expectedIntervall = 125.0;
        for (int i = 0; i < 10; i++) {
            assertThat(alg.getReturntime(), is((i+1) * expectedIntervall));
        }
        alg.estimatedTaskCompletionRatePerMillis = 0.016;
        double queueMiddle = (10 * expectedIntervall) / 2;
        double newIntervall = 125.0 / 2;
        for(int i = 0; i < 10; i++) {
            assertThat(alg.getReturntime(), is((queueMiddle + (i + 1) * newIntervall)));
        }

        alg.estimatedTaskCompletionRatePerMillis = 0.008;
        for (int i = 0; i < 10; i++) {
            assertThat(alg.getReturntime(), is(10 * expectedIntervall + (i+1) * expectedIntervall));
        }
    }


    @Test
    public void testRunAlgorithm() throws Exception {

    }

    @Test
    public void testGetNumberOfReleasedTokens() throws Exception {

    }

    @Test
    public void testSetNumberOfReleasedTokens() throws Exception {

    }

    @Test
    public void testUpdateEstimatedTaskCompletionRate() throws Exception {

    }
}