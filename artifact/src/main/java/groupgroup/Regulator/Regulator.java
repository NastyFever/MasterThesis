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
    boolean fullyUtilized = false;
    long epokStartTime,
        epokStartNumberOfFinishedJobs;

    private void onlineUpdateOfTaskCompletionRate(long numberOfFinishedJobs, long numberOfActiveClients) {

        if(UPDATE_TASK_COMPLETION_RATE_THRESHOLD * C_C < numberOfActiveClients) {
            if(fullyUtilized) {
                long numberOfFinishedJobsDuringPeriod = numberOfFinishedJobs - epokStartNumberOfFinishedJobs;
                if(numberOfFinishedJobsDuringPeriod > 0) {
                    long fullyUtilizedPeriod = System.currentTimeMillis() - epokStartTime;
                    double clientFinishIntervall = 0.0 + fullyUtilizedPeriod / numberOfFinishedJobsDuringPeriod;
                    algorithm.updateEstimatedTaskCompletionRate(clientFinishIntervall, LOGGER);
                }
            } else {
                fullyUtilized = true;
                epokStartTime = System.currentTimeMillis();
                epokStartNumberOfFinishedJobs = numberOfFinishedJobs;
            }
        } else {
            fullyUtilized = false;
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
