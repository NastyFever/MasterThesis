package com.qmatic.regulator;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class VectorAlgorithmTest {

    @Test
    public void testGetReturntime() throws Exception {

    }

    @Test
    public void testComputeQueues() throws Exception {
        int HWM = 400, LWM = 0, AM = 300;
        double INITIAL_TCR = 8;
        VectorAlgorithm alg = Mockito.spy(new VectorAlgorithm(LWM, HWM, AM, INITIAL_TCR));

        ArrayList<Object> expectedFirstAssertReturn = new ArrayList<>();
        expectedFirstAssertReturn.add(8);
        assertThat(alg.computeQueues(0.008), is(expectedFirstAssertReturn));
        ArrayList<Object> expectedSecondAssertReturn = new ArrayList<>();
        expectedSecondAssertReturn.add(4);
        expectedSecondAssertReturn.add(2);
        expectedSecondAssertReturn.add(1);
        assertThat(alg.computeQueues(0.007), is(expectedSecondAssertReturn));
        ArrayList<Object> expectedThirdAssertReturn = new ArrayList<>();
        expectedThirdAssertReturn.add(8);
        expectedThirdAssertReturn.add(1);
        assertThat(alg.computeQueues(0.009), is(expectedThirdAssertReturn));
    }
}