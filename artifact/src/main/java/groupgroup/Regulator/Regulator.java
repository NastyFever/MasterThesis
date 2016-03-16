package groupgroup.Regulator;

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

    public synchronized boolean handleRequest(int numberOfRetries) {
        if(currentLevelOfQueue < AM) {
            return true;
        }
        return false;
    }

    public synchronized void recievedUpdateFromApplicationServer(long numberOfUsedTokens) {
        this.numberOfUsedTokens = numberOfUsedTokens;
        currentLevelOfQueue = (int) (numberOfReleasedTokens - this.numberOfUsedTokens);
    }
}
