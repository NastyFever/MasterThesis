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
        LOGGER.info("Successfully started regulator.");
    }

    public JSONObject handleRequest(int numberOfRetries) {
        JSONObject jc = algorithm.runAlgorithm(numberOfFinishedJobs, numberOfRetries, LOGGER);
        return jc;
    }

    final double UPDATE_TASK_COMPLETION_RATE_THRESHOLD = 0.9; // Example, percent of c_c that need to be filled.
    final double C_C = 300; // Should be given as input.
    double averageTimePerClientMS = 125;
    boolean lastValue = false;
    long lastValueForFinishedJobs;
    final double THOUSAND_MS = 1000;

    private void onlineUpdateOfTaskCompletionRate(long numberOfFinishedJobs, long numberOfActiveClients) {

        if(UPDATE_TASK_COMPLETION_RATE_THRESHOLD * C_C < numberOfActiveClients) {
            if(lastValue) {
                long diff = lastValueForFinishedJobs - numberOfFinishedJobs;
                lastValueForFinishedJobs = numberOfFinishedJobs;
                averageTimePerClientMS = (averageTimePerClientMS + THOUSAND_MS/diff) /2; // Heavy weighted
                algorithm.updateEstimatedTaskCompletionRate(averageTimePerClientMS);
            } else {
                lastValue = true;
                lastValueForFinishedJobs = numberOfFinishedJobs;
            }
        } else {
            lastValue = false;
        }
    }

    public synchronized void receivedUpdateFromApplicationServer(long numberOfFinishedJobs) {
        this.numberOfFinishedJobs = numberOfFinishedJobs;
        long numberOfActiveClients = algorithm.getNumberOfReleasedTokens() - numberOfFinishedJobs;
        onlineUpdateOfTaskCompletionRate(numberOfFinishedJobs, numberOfActiveClients);
        LOGGER.info("Number of active tokens is: " + numberOfActiveClients);
        LOGGER.info("Number of finished jobs is: " +  numberOfFinishedJobs);
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
