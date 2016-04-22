package com.qmatic.regulator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class SecondVersionAlgorithm implements Algorithm {
    public long numberOfReleasedTokens = 0L;
    public double estimatedTaskCompletionRatePerMillis;
    private double virtualQueueEndTime = 0;
    private AtomicLong numberOfClientsInTheVirtualQueue = new AtomicLong(0L);

    int LWM;
    int HWM;
    int AM;

    public SecondVersionAlgorithm(int LWM, int HWM, int AM, double initialTCR){
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        estimatedTaskCompletionRatePerMillis = initialTCR / 1000;
    }
    @Override
    public synchronized double getReturntime() {
        double currentTime = getCurrentTimeInMillis();
        double wantedWaitIntervall = 1 / estimatedTaskCompletionRatePerMillis;
        double dynamicInsertBasedOnCounterRelativeTime = wantedWaitIntervall * (1 + numberOfClientsInTheVirtualQueue.getAndIncrement());

        if(!isVirtualQueueEndInFuture(currentTime)) { // Special case, queue empty, place first
            virtualQueueEndTime = currentTime + wantedWaitIntervall;
            return wantedWaitIntervall;
        }
        else if(isSuggestedRetryTimeWithinBound(wantedWaitIntervall,
                dynamicInsertBasedOnCounterRelativeTime, currentTime)) { // In bound
            double comeBackTime = dynamicInsertBasedOnCounterRelativeTime + currentTime;
            if(isAfterVirtualQueueEnd(comeBackTime)) {
                virtualQueueEndTime = comeBackTime;
            }
            return dynamicInsertBasedOnCounterRelativeTime;
        } else { // Queue not empty, not in bound
            double comeBackTime = virtualQueueEndTime + wantedWaitIntervall;
            double retryRelativeTime = comeBackTime - currentTime;
            virtualQueueEndTime += comeBackTime;
            return retryRelativeTime;
        }
    }

    protected long getCurrentTimeInMillis() {
        return System.currentTimeMillis();
    }

    private boolean isAfterVirtualQueueEnd(double comeBackTime) {
        return comeBackTime > virtualQueueEndTime;
    }

    private boolean isSuggestedRetryTimeWithinBound(double wantedWaitIntervall, double dynamicInsertBasedOnCounterRelativeTime, double currentTime) {
        return virtualQueueEndTime + wantedWaitIntervall >= currentTime + dynamicInsertBasedOnCounterRelativeTime;
    }

    private boolean isVirtualQueueEndInFuture(double currentTime) {
        return virtualQueueEndTime > currentTime;
    }

    Semaphore checkThenSet = new Semaphore(1);

    @Override
    public JSONObject runAlgorithm(long numberOfFinishedJobs, int numberOfRetries, Logger logger) {
        JSONObject jc = new JSONObject();
        if(numberOfRetries > 0) {
            numberOfClientsInTheVirtualQueue.decrementAndGet();
        }
        try {
            checkThenSet.acquire(); // We have to protect the check.
            if(isQueueLevelLessThanAimedMark(numberOfFinishedJobs) ||
                    isQueueLevelOverThirdQuarterAndHasRetried(numberOfFinishedJobs, numberOfRetries)) {
                long accessToken = ++numberOfReleasedTokens;
                checkThenSet.release();
                jc.put("Type", "AccessService");
                jc.put("Token", accessToken);
                logger.info("Client was given access after " + numberOfRetries + " tries");
            } else {
                checkThenSet.release();
                jc.put("Type", "ScheduleMessage");
                jc.put("ReturnTime", getReturntime());
            }
        } catch (InterruptedException e) {
            logger.info("Got interupted when checking if the regulator should release token: " + e.getMessage());
            checkThenSet.release();
        }

        return jc;
    }

    private boolean isQueueLevelOverThirdQuarterAndHasRetried(long numberOfFinishedJobs, int numberOfRetries) {
        return numberOfRetries > 0 && (numberOfReleasedTokens - numberOfFinishedJobs) < (HWM + AM)/2;
    }

    private boolean isQueueLevelLessThanAimedMark(long numberOfFinishedJobs) {
        return numberOfReleasedTokens-numberOfFinishedJobs < AM;
    }

    @Override
    public long getNumberOfReleasedTokens() {
        return numberOfReleasedTokens;
    }

    @Override
    public void setNumberOfReleasedTokens(long numberOfReleasedTokens) {
        this.numberOfReleasedTokens = numberOfReleasedTokens;
    }


    @Override
    public void updateEstimatedTaskCompletionRate(double clientFinishInterval, Logger logger, double overrate) {
        estimatedTaskCompletionRatePerMillis = ( 1 / clientFinishInterval) * overrate;
        logger.info("Updated the estimated task completion rate to: " + estimatedTaskCompletionRatePerMillis + " per ms with overrate " + overrate);
    }
}
