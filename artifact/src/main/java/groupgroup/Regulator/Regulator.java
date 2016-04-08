package groupgroup.Regulator;

import groupgroup.Main;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Regulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());
    private final boolean tcrLiveUpdate;

    long numberOfFinishedJobs;
    Algorithm algorithm;

    public Regulator(String regulatorAlgorithm, int LWM, int HWM, int AM, double tcrScalingFactor, double initialTCR, boolean tcrLiveUpdate) {
        this.numberOfFinishedJobs = 0L;
        this.tcrLiveUpdate = tcrLiveUpdate;
        switch (regulatorAlgorithm){
            case "FirstVersionAlgorithm":
                this.algorithm = new FirstVersionAlgorithm(LWM, HWM, AM, tcrScalingFactor, initialTCR);
                LOGGER.info("Using " + regulatorAlgorithm);
                break;
            case "SecondVersionAlgorithm":
                this.algorithm = new SecondVersionAlgorithm(LWM, HWM, AM, tcrScalingFactor, initialTCR);
                LOGGER.info("Using " + regulatorAlgorithm);
                break;
            default:
                LOGGER.error("Invalid algorithm type");
                System.err.print("Invalid algorithm type");
                System.exit(-1);
        }
        LOGGER.info("Successfully started regulator.");
    }

    public JSONObject handleRequest(int numberOfRetries) {
        JSONObject jc = algorithm.runAlgorithm(numberOfFinishedJobs, numberOfRetries, LOGGER);
        return jc;
    }

    final double UPDATE_TASK_COMPLETION_RATE_THRESHOLD = 0.9; // Example, percent of c_c that need to be filled.
    final double C_C = 100; // Should be given as input.
    boolean fullyUtilized = false;
    long epokStartTime,
        epokStartNumberOfFinishedJobs;
    double averageJobTime = 125;
    double oldJobWeightFactor = 0.75;

    private synchronized void onlineUpdateOfTaskCompletionRate(double jobTime) {

        LOGGER.info("New jobTime: " + jobTime);
        if (averageJobTime > 0){
            averageJobTime = oldJobWeightFactor * averageJobTime + (1 - oldJobWeightFactor) * jobTime;
            LOGGER.info("Average jobtime is set to " + averageJobTime);
        }

        double clientFinishIntervall = averageJobTime / C_C;
        LOGGER.info("ClientFinishInterval set to " + clientFinishIntervall);
        algorithm.updateEstimatedTaskCompletionRate(clientFinishIntervall, LOGGER);
    }

    public synchronized void receivedUpdateFromApplicationServer(long numberOfFinishedJobs, double jobTime) {
        this.numberOfFinishedJobs = numberOfFinishedJobs;
        long numberOfActiveClients = algorithm.getNumberOfReleasedTokens() - numberOfFinishedJobs;
        if (tcrLiveUpdate) {
            onlineUpdateOfTaskCompletionRate(jobTime);
        }
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
