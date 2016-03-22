package groupgroup.Regulator;

import groupgroup.Main;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Regulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());

    long numberOfFinishedJobs;
    Algorithm algorithm;

    public Regulator(int LWM, int HWM, int AM) {
        this.numberOfFinishedJobs = 0L;
        this.algorithm = new FirstVersionAlgorithm(LWM, HWM, AM);
        LOGGER.debug("Successfully started regulator.");
    }

    public JSONObject handleRequest(int numberOfRetries) {
        JSONObject jc = algorithm.runAlgorithm(numberOfFinishedJobs, numberOfRetries);
        return jc;
    }

    public synchronized void recievedUpdateFromApplicationServer(long numberOfFinishedJobs) {
        this.numberOfFinishedJobs = numberOfFinishedJobs;
    }

    public long getNumberOfReleasedTokens() {
        return algorithm.getNumberOfReleasedTokens();
    }

    public void setNumberOfReleasedTokens(long numberOfReleasedTokens) {
        algorithm.setNumberOfReleasedTokens(numberOfReleasedTokens);
    }

    public Long getNumberOfFinishedJobs() {
        return numberOfFinishedJobs;
    }
}
