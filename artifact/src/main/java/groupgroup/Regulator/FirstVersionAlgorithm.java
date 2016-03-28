package groupgroup.Regulator;

import org.json.simple.JSONObject;
import org.slf4j.Logger;

public class FirstVersionAlgorithm implements Algorithm {
    // The current level of the TCP backlog queue is the diff between numberOfReleasedTokens and numberOfFinishedJobs
    private long numberOfReleasedTokens = 0L;
    private double estimatedTaskCompletionRatePerMillis;
    private double virtualQueueEndTime = 0;

    int LWM;
    int HWM;
    int AM;

    public FirstVersionAlgorithm(int LWM, int HWM, int AM){
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        estimatedTaskCompletionRatePerMillis = 1.0/250;
    }
    @Override
    public synchronized double getReturntime() {
        double currentTime = System.currentTimeMillis();
        if(currentTime > virtualQueueEndTime) {
            double waitDuration = 1/estimatedTaskCompletionRatePerMillis;
            virtualQueueEndTime = currentTime + waitDuration;
            return waitDuration;
        } else {
            virtualQueueEndTime += 1/ estimatedTaskCompletionRatePerMillis;
            double waitDuration = virtualQueueEndTime - currentTime;
            return waitDuration;
        }
    }

    @Override
    public synchronized JSONObject runAlgorithm(long numberOfFinishedJobs, int numberOfRetries, Logger logger) {
        JSONObject jc = new JSONObject();

        if(isQueueLevelLessThanAimedMark(numberOfFinishedJobs) || isQueueLevelOverThirdQuarterAndHasRetried(numberOfFinishedJobs, numberOfRetries)) {
            jc.put("Type", "AccessService");
            long accessToken = ++numberOfReleasedTokens;
            jc.put("Token", accessToken);
            logger.info("Client was given access after " + numberOfRetries + " tries");
        } else {
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
    public void updateEstimatedTaskCompletionRate(double clientFinishInterval) {
        estimatedTaskCompletionRatePerMillis = clientFinishInterval / 1000;
    }
}
