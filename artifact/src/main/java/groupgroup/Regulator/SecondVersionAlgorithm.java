package groupgroup.Regulator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class SecondVersionAlgorithm implements Algorithm {
    // The current level of the TCP backlog queue is the diff between numberOfReleasedTokens and numberOfFinishedJobs
    public long numberOfReleasedTokens = 0L;
    public double estimatedTaskCompletionRatePerMillis;
    private AtomicLong numberOfClientsInTheVirtualQueue = new AtomicLong(0L);

    int LWM;
    int HWM;
    int AM;
    private final double OVERRATE;

    public SecondVersionAlgorithm(int LWM, int HWM, int AM, double tcrScalingFactor){
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        this.OVERRATE = tcrScalingFactor;
        estimatedTaskCompletionRatePerMillis = 8.0/1000;
    }
    @Override
    public double getReturntime() {
        double waitDuration = 1/estimatedTaskCompletionRatePerMillis;
        double returnTime = waitDuration * (1 + numberOfClientsInTheVirtualQueue.getAndIncrement());
        return returnTime;
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
    public void updateEstimatedTaskCompletionRate(double clientFinishInterval, Logger logger) {
        estimatedTaskCompletionRatePerMillis = OVERRATE * (1 / clientFinishInterval);
        logger.info("Updated the estimated task completion rate to: " + estimatedTaskCompletionRatePerMillis + " per ms with overrate " + OVERRATE);
    }
}
