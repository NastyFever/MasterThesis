package groupgroup.Regulator;

import org.json.simple.JSONObject;

public class Regulator {
    long numberOfReleasedTokens;
    long numberOfUsedTokens;

    final int LWM;
    final int HWM;
    final int AM;

    long currentLevelOfQueue;

    public Regulator(int LWM, int HWM, int AM) {
        this.numberOfReleasedTokens = 0L;
        this.numberOfUsedTokens = 0L;
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        this.currentLevelOfQueue = 0;
        this.lastTimeForUpdate = System.currentTimeMillis();
        this.taskCompletionRate = -1;
    }

    public synchronized JSONObject handleRequest(int numberOfRetries) {
        JSONObject jc = new JSONObject();
        if(currentLevelOfQueue < AM) {
            jc.put("Type", "AccessService");
            long accessToken = ++numberOfReleasedTokens;
            jc.put("Token", accessToken);
        } else {
            jc.put("Type", "ScheduleMessage");
            double returnTime = System.currentTimeMillis() + 1/taskCompletionRate;
            jc.put("ReturnTime", returnTime);
        }
        return jc;
    }

    long lastTimeForUpdate;
    double taskCompletionRate;

    public synchronized void recievedUpdateFromApplicationServer(long numberOfUsedTokens) {
        long currentTime = System.currentTimeMillis();
        if(numberOfUsedTokens > this.numberOfUsedTokens) {
            long newFinishedJobs = numberOfUsedTokens - this.numberOfUsedTokens;
            taskCompletionRate = newFinishedJobs / (currentTime- lastTimeForUpdate);
            System.out.println("TCR is updated to: " + taskCompletionRate);
        }
        this.numberOfUsedTokens = numberOfUsedTokens;
        currentLevelOfQueue = numberOfReleasedTokens - this.numberOfUsedTokens;
    }
}
