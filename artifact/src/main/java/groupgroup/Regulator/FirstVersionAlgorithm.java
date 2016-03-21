package groupgroup.Regulator;

import org.json.simple.JSONObject;

public class FirstVersionAlgorithm implements Algorithm {
    // The current level of the TCP backlog queue is the deff between numberOfReleasedTokens and numberOfFinishedJobs
    private long numberOfReleasedTokens = 0L;
    private double estimatedTaskCompletionRatePerMillis;
    private long virtualQueueEndTime = 0;

    int LWM;
    int HWM;
    int AM;

    public FirstVersionAlgorithm(int LWM, int HWM, int AM){
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        estimatedTaskCompletionRatePerMillis = 1/8000;
    }
    @Override
    public double getReturntime() {
        long currentTime = System.currentTimeMillis();
        if(currentTime < virtualQueueEndTime) {
            return 1/ estimatedTaskCompletionRatePerMillis;
        } else {
            double returnTime = virtualQueueEndTime - currentTime;
            returnTime =+ 1/ estimatedTaskCompletionRatePerMillis;
            return returnTime;
        }
    }

    @Override
    public synchronized JSONObject runAlgorithm(long numberOfFinishedJobs, int numberOfRetries) {
        JSONObject jc = new JSONObject();

        if(isQueueLevelLessThanAimedMark(numberOfFinishedJobs) || isQueueLevelOverThirdQuarterAndHasRetried(numberOfFinishedJobs, numberOfRetries)) {
            jc.put("Type", "AccessService");
            long accessToken = ++numberOfReleasedTokens;
            jc.put("Token", accessToken);
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
}
