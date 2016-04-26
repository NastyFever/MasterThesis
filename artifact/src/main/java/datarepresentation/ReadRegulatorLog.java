package datarepresentation;

import jxl.read.biff.BiffException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ReadRegulatorLog {

    public static void main(String[] args) {
        parseFile();
    }

    static AtomicInteger zeroTimes = new AtomicInteger(0),
        oneTime = new AtomicInteger(0),
        twoTimes = new AtomicInteger(0),
        threeTimes = new AtomicInteger(0),
        fourTimes = new AtomicInteger(0),
        fiveTimes = new AtomicInteger(0),
        sixTimes = new AtomicInteger(0),
        sevenTimes = new AtomicInteger(0),
        eightTimes = new AtomicInteger(0),
        nineTimesOrMore = new AtomicInteger(0);

    static List<String> listOfQueueLevels = new ArrayList<>();
    static List<String> listOfFinishedJobs = new ArrayList<>();
    static List<String> listOfTimes = new ArrayList<>();
    static List<String> listOfTCR = new ArrayList<>();
    static List<String> listOfJobTimes = new ArrayList<>();

    private static void parseFile() {
        String fileName = "C:\\Programmering\\Exjobb\\log.log";
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(line ->
                buildEntry(line)
            );
           ExcelBridge exl = new ExcelBridge();

            exl.writeEntries(entries);
//            exl.writeList(listOfTimes, listOfQueueLevels, listOfFinishedJobs,
//                    listOfTCR, listOfJobTimes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }

        System.out.println("Number of retries for accepted client request is as follow"
                + "\n\tAVG: " + computeAverage() + "\n\t0: " +zeroTimes
                + "\n\t1: " + oneTime + "\n\t2: " + twoTimes + "\n\t3: " + threeTimes
                + "\n\t4: " + fourTimes + "\n\t5: " + fiveTimes + "\n\t6: " + sixTimes
                + "\n\t7: " + sevenTimes + "\n\t8: " + eightTimes + "\n\t9+: " + nineTimesOrMore);
    }

    private static double computeAverage() {
        double numberOfJobs = zeroTimes.get() + oneTime.get() + twoTimes.get() + threeTimes.get() + fourTimes.get()
                + fiveTimes.get() + sixTimes.get() + sevenTimes.get() + eightTimes.get() + nineTimesOrMore.get();
        double average = (oneTime.get() + 2 * twoTimes.get() + 3 * threeTimes.get() + 4 * fourTimes.get()
                + 5 * fiveTimes.get() + 6 * sixTimes.get() + 7 * sevenTimes.get() + 8 * eightTimes.get()
                + 9 * nineTimesOrMore.get()) / numberOfJobs;
        return round(average, 2);
    }

    private static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    static boolean skip = false;
    private static void processLine(String line) {
        String word[] = line.split(" ");
        if(isGivenAccessLine(line)) {
            updateNumberRetries(Integer.parseInt(word[word.length - 2]));
        } else if(isNewJobTimeLine(line)) {
            int subtractFromStringToGetInSeconds = 3;
            String timeAsString = word[0].substring(0, word[0].length() - subtractFromStringToGetInSeconds);
            if(!listOfTimes.contains(timeAsString)){
                skip=false;
                listOfTimes.add(timeAsString);
                listOfJobTimes.add(word[word.length - 1]);
            } else {
                skip = true;
            }
        } else if(!skip) {
            if(isActiveTokensLine(line)) {
                listOfQueueLevels.add(word[word.length - 1]);
            } else if(isNumberOfFinishedJobsLine(line)) {
                listOfFinishedJobs.add(word[word.length - 1]);
            } else if(isEstimatedTaskCompletionRateLine(line)) {
                listOfTCR.add(Double.toString(round(Double.parseDouble(word[word.length - 6]), 5)));
            }
        }
    }


    static ArrayList<Entry> entries = new ArrayList<>();
    static Entry workingEntry = new Entry();
    static HashMap<String, Integer> timeToArrayIdMap = new HashMap<>();
    static int initialNumberOfFinishedJobs = -1;

    private static void buildEntry(String line) {
        String word[] = line.split(" ");

        if(isGivenAccessLine(line)) {
            updateNumberRetries(Integer.parseInt(word[word.length - 2]));
        } else if(isNewJobTimeLine(line)) {
            workingEntry.jobTime = word[word.length - 1];
        } else if(isEstimatedTaskCompletionRateLine(line)) {
            workingEntry.comeBackRate = Double.toString(round(Double.parseDouble(word[word.length - 6]), 5));
        } else if(isNumberOfFinishedJobsLine(line)) {
            if (initialNumberOfFinishedJobs == -1) {
                initialNumberOfFinishedJobs = Integer.parseInt(word[word.length -1]);
            }
            workingEntry.finishedJobs = String.valueOf(Integer.parseInt(word[word.length - 1]) - initialNumberOfFinishedJobs);
        } else if(isActiveTokensLine(line)) {
            int subtractFromStringToGetInSeconds = 3;
            String timeAsString = word[0].substring(0, word[0].length() - subtractFromStringToGetInSeconds);
            workingEntry.activeChannels = word[word.length - 1];
            workingEntry.time = timeAsString;
            entries.add(workingEntry);
            workingEntry = new Entry();
        }

    }


    private static boolean isGivenAccessLine(String line) {
        return line.endsWith("tries");
    }

    private static boolean isNewJobTimeLine(String line) {
        return line.contains("New jobTime");
    }

    private static boolean isEstimatedTaskCompletionRateLine(String line) {
        return line.contains("Updated the estimated task completion rate");
    }

    private static boolean isNumberOfFinishedJobsLine(String line) {
        return line.contains("Number of finished jobs is:");
    }

    private static boolean isActiveTokensLine(String line) {
        return line.contains("Number of active tokens is:");
    }

    private static void updateNumberRetries(int noTries) {
        if(noTries == 0)  {
            zeroTimes.incrementAndGet();
        } else if(noTries == 1){
            oneTime.incrementAndGet();
        } else if(noTries == 2) {
            twoTimes.incrementAndGet();
        } else if(noTries == 3) {
            threeTimes.incrementAndGet();
        } else if(noTries == 4) {
            fourTimes.incrementAndGet();
        } else if(noTries == 5) {
            fiveTimes.incrementAndGet();
        } else if(noTries == 6) {
            sixTimes.incrementAndGet();
        } else if(noTries == 7) {
            sevenTimes.incrementAndGet();
        } else if(noTries == 8) {
            eightTimes.incrementAndGet();
        } else if(noTries > 8 ) {
            nineTimesOrMore.incrementAndGet();
        } else {
            System.out.println("ERROR: noTries is less than zero OR not an integer.");
        }
    }
}
