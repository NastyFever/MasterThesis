package groupgroup.Regulator;

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
    private final double OVERRATE;

    public SecondVersionAlgorithm(int LWM, int HWM, int AM, double tcrScalingFactor, double initialTCR){
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        this.OVERRATE = tcrScalingFactor;
        estimatedTaskCompletionRatePerMillis = initialTCR / 1000;
    }
    @Override
    public synchronized double getReturntime() {
        double currentTime = System.currentTimeMillis();
        double waitDuration = 1 / estimatedTaskCompletionRatePerMillis;
        double suggestedRetryTime = waitDuration * (1 + numberOfClientsInTheVirtualQueue.get());

        if(!isVirtualQueueEndInFuture(currentTime)) {
            virtualQueueEndTime += currentTime + waitDuration;
            return waitDuration;
        }
        else if(isSuggestedRetryTimeWithinBound(waitDuration, suggestedRetryTime, currentTime)) {
            if(suggestedRetryTime + currentTime > virtualQueueEndTime) {
                virtualQueueEndTime = suggestedRetryTime;
            }
            return suggestedRetryTime;
        } else {
            virtualQueueEndTime += waitDuration;
            double retryTime = virtualQueueEndTime - currentTime + waitDuration;
            return retryTime;
        }
    }

    private boolean isSuggestedRetryTimeWithinBound(double waitDuration, double suggestedRetryTime, double currentTime) {
        return virtualQueueEndTime + waitDuration >= suggestedRetryTime + currentTime;
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
        estimatedTaskCompletionRatePerMillis = clientFinishInterval * overrate;
        logger.info("Updated the estimated task completion rate to: " + estimatedTaskCompletionRatePerMillis + " per ms with overrate " + OVERRATE);
    }
}
