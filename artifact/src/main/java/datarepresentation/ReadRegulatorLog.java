package datarepresentation;

import jxl.read.biff.BiffException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    static List<String> listOfTCRUpdateTimes = new ArrayList<>();
    static List<String> listOfTCR = new ArrayList<>();

    private static void parseFile() {
        String fileName = "C:\\Programmering\\Exjobb\\log.log";
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(line ->
                processLine(line)
            );
           ExcelBridge exl = new ExcelBridge();

            exl.writeList(listOfTimes, listOfQueueLevels, listOfFinishedJobs, listOfTCRUpdateTimes, listOfTCR);
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
        if(line.endsWith("tries")) {
            String word[] = line.split(" ");
            updateNumberRetries(Integer.parseInt(word[word.length-2]));
        } else if(line.contains("Number of active tokens is:")) {
            String word[] = line.split(" ");
            int subtractFromStringToGetInSeconds = 3;
            String timeAsString = word[0].substring(0, word[0].length() - subtractFromStringToGetInSeconds);
            if(!listOfTimes.contains(timeAsString)){
                listOfTimes.add(timeAsString);
                listOfQueueLevels.add(word[word.length-1]);
            } else {
                skip = true;
            }
        } else if(line.contains("Number of finished jobs is:")) {
            if(!skip) {
                String word[] = line.split(" ");
                listOfFinishedJobs.add(word[word.length-1]);
            } else {
                skip = false;
            }
        } else if(line.contains("Updated the estimated task completion rate")) {
            String word[] = line.split(" ");
            int subtractFromStringToGetInSeconds = 3;
            String timeAsString = word[0].substring(0, word[0].length() - subtractFromStringToGetInSeconds);
            if(!listOfTimes.contains(timeAsString)) {
                listOfTCRUpdateTimes.add(timeAsString);
                listOfTCR.add(Double.toString(round(Double.parseDouble(word[word.length - 6]), 5)));
            }
        }
    }

    private static void updateNumberRetries(int noTries) {
        if(noTries == 0)  {
            zeroTimes.incrementAndGet();
        } else if(noTries == 1){
            oneTime.incrementAndGet();
        } else if(noTries == 2) {
            twoTimes.incrementAndGet();
        }else if(noTries == 3) {
            threeTimes.incrementAndGet();
        }else if(noTries == 4) {
            fourTimes.incrementAndGet();
        }else if(noTries == 5) {
            fiveTimes.incrementAndGet();
        }else if(noTries == 6) {
            sixTimes.incrementAndGet();
        }else if(noTries == 7) {
            sevenTimes.incrementAndGet();
        }else if(noTries == 8) {
            eightTimes.incrementAndGet();
        } else if(noTries > 8 ) {
            nineTimesOrMore.incrementAndGet();
        } else {
            System.out.println("ERROR: noTries is less than zero OR not an integer.");
        }
    }
}