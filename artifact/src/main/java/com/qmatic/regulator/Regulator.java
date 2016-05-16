package com.qmatic.regulator;

import com.qmatic.Configuration;
import com.qmatic.Main;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                this.algorithm = new SecondVersionAlgorithm(configuration.getLowWaterMark(), configuration.getHighWaterMark(), configuration.getAimedMark(), configuration.getInitialTCR(), configuration.isFairness());
                LOGGER.info("Using " + configuration.getRegulatorAlgorithm());
                break;
            case "VectorAlgorithm":
                this.algorithm = new VectorAlgorithm(configuration.getLowWaterMark(), configuration.getHighWaterMark(), configuration.getAimedMark(), configuration.getInitialTCR(), configuration.isFairness());
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
    int numberOfServerUpdates = 0;
    double sumOfJobTimes = 0;
    double sumOfSquaredJobTimes = 0;

    private synchronized void onlineUpdateOfTaskCompletionRate(double jobTime) {

        sumOfJobTimes += jobTime;
        sumOfSquaredJobTimes += jobTime*jobTime;
        double averageJobTime = sumOfJobTimes / numberOfServerUpdates;
        double variance = sumOfSquaredJobTimes / numberOfServerUpdates - averageJobTime*averageJobTime;
        double standardDeviation = Math.sqrt(variance);
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
        receivedUpdateFromApplicationServer(numberOfFinishedJobs);
        ++numberOfServerUpdates;
        if (tcrLiveUpdate) {
            onlineUpdateOfTaskCompletionRate(jobTime);
        }
    }

    public synchronized void receivedUpdateFromApplicationServer(long numberOfFinishedJobs) {
        this.numberOfFinishedJobs = numberOfFinishedJobs;
        LOGGER.info("Number of finished jobs is: " +  numberOfFinishedJobs);
        long numberOfActiveClients = algorithm.getNumberOfReleasedTokens() - numberOfFinishedJobs;
        LOGGER.info("Number of active tokens is: " + numberOfActiveClients);
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
