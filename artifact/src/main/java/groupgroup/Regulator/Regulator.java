package groupgroup.Regulator;

import groupgroup.Main;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Regulator {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private FileHandler handler;

    long numberOfFinishedJobs;
    Algorithm algorithm;

    public Regulator(int LWM, int HWM, int AM) {
        this.numberOfFinishedJobs = 0L;
        this.lastTimeForUpdate = System.currentTimeMillis();
        this.taskCompletionRate = 1/10000;
        this.algorithm = new FirstVersionAlgorithm(LWM, HWM, AM);
        init();
    }

    private void init() {
        try {
            handler = new FileHandler("log.txt", true);
        } catch (IOException e) {
            System.out.println("Failed to create file log.txt");
        }
        SimpleFormatter formatter = new SimpleFormatter();
        handler.setFormatter(formatter);
        LOGGER.addHandler(handler);
    }

    public JSONObject handleRequest(int numberOfRetries) {
        JSONObject jc = algorithm.runAlgorithm(numberOfFinishedJobs, numberOfRetries);
        return jc;
    }

    long lastTimeForUpdate;
    double taskCompletionRate;

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
