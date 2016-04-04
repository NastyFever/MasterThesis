package groupgroup.Regulator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

public class SecondVersionAlgorithm implements Algorithm {
    // The current level of the TCP backlog queue is the diff between numberOfReleasedTokens and numberOfFinishedJobs
    public long numberOfReleasedTokens = 0L;
    public double estimatedTaskCompletionRatePerMillis;
    private long numberOfClientsInTheVirtualQueue = 0L;

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
    public synchronized double getReturntime() {
        double waitDuration = 1/estimatedTaskCompletionRatePerMillis;
        return waitDuration*(1+numberOfClientsInTheVirtualQueue);
    }

    @Override
    public synchronized JSONObject runAlgorithm(long numberOfFinishedJobs, int numberOfRetries, Logger logger) {
        JSONObject jc = new JSONObject();

        if(isQueueLevelLessThanAimedMark(numberOfFinishedJobs) || isQueueLevelOverThirdQuarterAndHasRetried(numberOfFinishedJobs, numberOfRetries)) {
            if(numberOfRetries > 0) {
                --numberOfClientsInTheVirtualQueue;
            }
            jc.put("Type", "AccessService");
            long accessToken = ++numberOfReleasedTokens;
            jc.put("Token", accessToken);
            logger.info("Client was given access after " + numberOfRetries + " tries");
        } else {
            if(numberOfRetries == 0) {
                ++numberOfClientsInTheVirtualQueue;
            }
            jc.put("Type", "ScheduleMessage");
            jc.put("ReturnTime", getReturntime());
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
