package groupgroup.Regulator;

import org.json.simple.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class Regulator {
    long numberOfReleasedTokens;
    long numberOfUsedTokens;

    final int LWM;
    final int HWM;
    final int AM;

    int currentLevelOfQueue;

    public Regulator(int LWM, int HWM, int AM) {
        this.numberOfReleasedTokens = 0L;
        this.numberOfUsedTokens = 0L;
        this.LWM = LWM;
        this.HWM = HWM;
        this.AM = AM;
        this.currentLevelOfQueue = 0;
    }

    public synchronized JSONObject handleRequest(int numberOfRetries) {
        long accessToken = ++numberOfReleasedTokens;
        long returnTime = 0;

        JSONObject jc = new JSONObject();
        if(currentLevelOfQueue < AM) {
            jc.put("Type", "AccessService");
            jc.put("Token", accessToken);
        } else {
            jc.put("Type", "ScheduleMessage");
            jc.put("ReturnTime", returnTime);
        }
        return jc;
    }

    public synchronized void recievedUpdateFromApplicationServer(long numberOfUsedTokens) {
        this.numberOfUsedTokens = numberOfUsedTokens;
        currentLevelOfQueue = (int) (numberOfReleasedTokens - this.numberOfUsedTokens);
    }
}
