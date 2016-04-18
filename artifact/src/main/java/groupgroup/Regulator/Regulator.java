package groupgroup.Regulator;

import groupgroup.Main;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groupgroup.Configuration;

public class Regulator {

    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());
    private final boolean tcrLiveUpdate;

    long numberOfFinishedJobs;
    Algorithm algorithm;

    public Regulator(Configuration configuration) {
        this.numberOfFinishedJobs = 0L;
        this.tcrLiveUpdate = configuration.isTCRLiveUpdate();
        this.C_C = configuration.getCC();
        switch (configuration.getRegulatorAlgorithm()){
            case "FirstVersionAlgorithm":
                this.algorithm = new FirstVersionAlgorithm(configuration.getLowWaterMark(), configuration.getHighWaterMark(), configuration.getAimedMark(), configuration.getInitialTCR());
                LOGGER.info("Using " + configuration.getRegulatorAlgorithm());
                break;
            case "SecondVersionAlgorithm":
                this.algorithm = new SecondVersionAlgorithm(configuration.getLowWaterMark(), configuration.getHighWaterMark(), configuration.getAimedMark(), configuration.getInitialTCR());
                LOGGER.info("Using " + configuration.getRegulatorAlgorithm());
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

    final double C_C;
    double averageJobTime = 0;
    int numberOfServerUpdates = 0;
    double variance;
    double standardDeviation;
    double sumOfJobTimes = 0;
    double sumOfSquaredJobTimes = 0;

    private synchronized void onlineUpdateOfTaskCompletionRate(double jobTime) {

        sumOfJobTimes += jobTime;
        sumOfSquaredJobTimes += jobTime*jobTime;
        averageJobTime = sumOfJobTimes / numberOfServerUpdates;
        variance = sumOfSquaredJobTimes / numberOfServerUpdates - averageJobTime*averageJobTime;
        standardDeviation = Math.sqrt(variance);
        LOGGER.info("Standard deviation: " + standardDeviation);

        LOGGER.info("New jobTime: " + jobTime);
        LOGGER.info("Average jobtime is set to " + averageJobTime);

        if ( numberOfServerUpdates > 1) {
            double overrate = 1 + standardDeviation / averageJobTime;
            double clientFinishIntervall = averageJobTime / C_C;
            LOGGER.info("ClientFinishInterval set to " + clientFinishIntervall);
            LOGGER.info("eTCR : " + 1 / clientFinishIntervall);
            algorithm.updateEstimatedTaskCompletionRate(clientFinishIntervall, LOGGER, overrate);
        }
    }

    public synchronized void receivedUpdateFromApplicationServer(long numberOfFinishedJobs, double jobTime) {
        this.numberOfFinishedJobs = numberOfFinishedJobs;
        long numberOfActiveClients = algorithm.getNumberOfReleasedTokens() - numberOfFinishedJobs;
        ++numberOfServerUpdates;
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
