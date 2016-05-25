package com.qmatic.regulator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class SecondVersionAlgorithm implements Algorithm {
    public long numberOfReleasedTokens = 0L;
    public double estimatedTaskCompletionRatePerMillis;
    private double virtualQueueEndTime = 0;

    private AtomicLong numberOfClientsInTheVirtualQueue = new AtomicLong(0L);

    private int LWM;
    private int HWM;
    private int AM;

    private double virtualQueueAverage = 0.0;

    private HashMap<Integer, Integer> numberOfRequestsPerRetryInTheVirtualQueue = new HashMap<>();
    private boolean fairness;

    public SecondVersionAlgorithm(int LWM, int HWM, int AM, double initialTCR, boolean fairness){
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        this.fairness = fairness;
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
            virtualQueueEndTime = comeBackTime;
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
        if(fairness) {
            return passRequestThroughFairnessGate(numberOfFinishedJobs, numberOfRetries, logger);
        } else {
            return passRequestThroughDefaultGate(numberOfFinishedJobs, numberOfRetries, logger);
        }
    }

    private JSONObject passRequestThroughFairnessGate(long numberOfFinishedJobs, int numberOfRetries, Logger logger) {
        JSONObject jc = new JSONObject();

        try {
            checkThenSet.acquire(); // We have to protect the check.
            boolean go = false;
            long queueLevel = numberOfReleasedTokens - numberOfFinishedJobs;
            virtualQueueAverage = computeVirtualQueueAverage();
            if(isQueueLevelLessThanFirstQuarterThreshold(queueLevel)) { // Free go
                go = true;
            } else if (isQueueLevelLessThanSecondQuarterThreshold(queueLevel) && hasRetried(numberOfRetries)) { // Prio 3
                go = true;
            } else if (isQueueLevelLessThanThirdQuarterThreshold(queueLevel) && isOverAverage(numberOfRetries)) { // Prio 2
                go = true;
            } else if (isQueueLevelLessThanFourthQuarterThreshold(queueLevel) && isTopPrioritized(numberOfRetries)) { // Prio 1
                go = true;
            }
            if(hasRetried(numberOfRetries)) {
                decrementMapFor(numberOfRetries);
                numberOfClientsInTheVirtualQueue.decrementAndGet();
            }
            if(go) {
                long accessToken = ++numberOfReleasedTokens;
                checkThenSet.release();
                jc.put("Type", "AccessService");
                jc.put("Token", accessToken);
                logger.info("Client was given access after " + numberOfRetries + " tries");
            } else {
                incrementMapFor(numberOfRetries);
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

    protected double computeVirtualQueueAverage() {
        double average = 0.0;
        Set<Integer> keys = numberOfRequestsPerRetryInTheVirtualQueue.keySet();
        if(!keys.isEmpty()) {
            int numberOfRequestsInTheVirtualQueue = 0;
            for(int key : keys) {
                numberOfRequestsInTheVirtualQueue += numberOfRequestsPerRetryInTheVirtualQueue.get(key);
            }
            for(int key : keys) {
                average += 1.0 * key * getNumberOfRequestsPerRetryInTheVirtualQueue().get(key) / numberOfRequestsInTheVirtualQueue;
            }
        }
        return average;
    }

    protected void incrementMapFor(int numberOfRetries) {
        int updatedNumberOfRetries = numberOfRetries + 1;
        if(numberOfRequestsPerRetryInTheVirtualQueue.containsKey(updatedNumberOfRetries)) {
            numberOfRequestsPerRetryInTheVirtualQueue.put(updatedNumberOfRetries, numberOfRequestsPerRetryInTheVirtualQueue.get(updatedNumberOfRetries) + 1);
        } else {
            numberOfRequestsPerRetryInTheVirtualQueue.put(updatedNumberOfRetries, 1);
        }
    }

    protected void decrementMapFor(int numberOfRetries) {
        if(hasRetried(numberOfRetries)) {
            if(numberOfRequestsPerRetryInTheVirtualQueue.get(numberOfRetries) == 1) {
                numberOfRequestsPerRetryInTheVirtualQueue.remove(numberOfRetries);
            } else {
                numberOfRequestsPerRetryInTheVirtualQueue.put(numberOfRetries, numberOfRequestsPerRetryInTheVirtualQueue.get(numberOfRetries) - 1);
            }
        }
    }

    protected boolean isTopPrioritized(int numberOfRetries) {
        Set<Integer> keySet = numberOfRequestsPerRetryInTheVirtualQueue.keySet();
        if(!keySet.isEmpty()) {
            int lowestRetryLevelInTheHighestPrioritizedGroup = Collections.max(keySet);
            int numberOfTopPrioritized = (HWM - LWM) / 4;

            int sum = numberOfRequestsPerRetryInTheVirtualQueue.get(lowestRetryLevelInTheHighestPrioritizedGroup);
            while (sum < numberOfTopPrioritized && lowestRetryLevelInTheHighestPrioritizedGroup - 1 > 0) {
                if(numberOfRequestsPerRetryInTheVirtualQueue.containsKey(lowestRetryLevelInTheHighestPrioritizedGroup - 1)) {
                    int numberOfRequestsInThisRetryLevel = numberOfRequestsPerRetryInTheVirtualQueue.get(lowestRetryLevelInTheHighestPrioritizedGroup - 1);
                    if(numberOfRequestsInThisRetryLevel + sum < numberOfTopPrioritized) {
                        sum += numberOfRequestsInThisRetryLevel;
                        --lowestRetryLevelInTheHighestPrioritizedGroup;
                    } else {
                        if(lowestRetryLevelInTheHighestPrioritizedGroup > 1) {
                            --lowestRetryLevelInTheHighestPrioritizedGroup;
                        }
                        break;
                    }
                } else {
                    --lowestRetryLevelInTheHighestPrioritizedGroup;
                }
            }
            return numberOfRetries >= lowestRetryLevelInTheHighestPrioritizedGroup;
        } else {
            return false;
        }
    }

    protected boolean isQueueLevelLessThanFourthQuarterThreshold(long queueLevel) {
        return queueLevel < HWM;
    }

    protected boolean isOverAverage(int numberOfRetries) {
        return numberOfRetries > virtualQueueAverage;
    }

    private boolean isQueueLevelLessThanThirdQuarterThreshold(long queueLevel) {
        return queueLevel < LWM + 3 * (HWM - LWM) / 4.0;
    }

    private boolean isQueueLevelLessThanSecondQuarterThreshold(long queueLevel) {
        return queueLevel < LWM + (HWM - LWM) / 2.0;
    }

    private boolean hasRetried(int numberOfRetries) {
        return numberOfRetries > 0;
    }

    private boolean isQueueLevelLessThanFirstQuarterThreshold(long queueLevel) {
        return queueLevel < LWM + (HWM - LWM) / 4.0;
    }

    private JSONObject passRequestThroughDefaultGate(long numberOfFinishedJobs, int numberOfRetries, Logger logger) {
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

    public void setNumberOfClientsInTheVirtualQueue(AtomicLong numberOfClientsInTheVirtualQueue) {
        this.numberOfClientsInTheVirtualQueue = numberOfClientsInTheVirtualQueue;
    }

    protected HashMap<Integer, Integer> getNumberOfRequestsPerRetryInTheVirtualQueue() {
        return numberOfRequestsPerRetryInTheVirtualQueue;
    }

    protected double getVirtualQueueAverage() {
        return virtualQueueAverage;
    }
}
