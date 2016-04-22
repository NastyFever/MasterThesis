package com.qmatic.regulator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

public interface Algorithm {

    /**
     * Computes for how long the client should wait until it retries.
     * @return
     */
    double getReturntime();

    /**
     * Return a JSON object to be sent to the clients.
     * The JSON will either tell the client to go forth, or return at a later time.
     * @return
     * @param numberOfFinishedJobs
     * @param numberOfRetries
     * @param logger
     */
    JSONObject runAlgorithm(long numberOfFinishedJobs, int numberOfRetries, Logger logger);

    long getNumberOfReleasedTokens();

    void setNumberOfReleasedTokens(long numberOfReleasedTokens);

    void updateEstimatedTaskCompletionRate(double clientFinishInterval, Logger logger, double overrate);
}
