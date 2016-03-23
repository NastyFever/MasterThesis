package datarepresentation;

import jxl.read.biff.BiffException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadRegulatorLog {

    public static void main(String[] args) {
        parseFile();
    }

    static AtomicInteger zeroTimes = new AtomicInteger(0),
        oneTime = new AtomicInteger(0),
        twoTimes = new AtomicInteger(0),
        threeTimesOrMore = new AtomicInteger(0);

    static List<String> listOfQueueLevels = new ArrayList<>();

    private static void parseFile() {
        String fileName = "C:\\Programmering\\Exjobb\\log.log";
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(line ->
                processLine(line)
            );
           ExcelBridge exl = new ExcelBridge();

            exl.writeList(listOfQueueLevels);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }

        System.out.println("Number of retries for accepted client request is as follow\n\t0: " +zeroTimes
                + "\n\t1: " + oneTime+ "\n\t2: " + twoTimes+ "\n\t3+: " + threeTimesOrMore);
    }

    private static void processLine(String line) {
        if(line.endsWith("tries")) {
            String word[] = line.split(" ");
            updateNumberRetries(Integer.parseInt(word[word.length-2]));
        } else if(line.contains("Number of active tokens is:")) {
            String word[] = line.split(" ");
            listOfQueueLevels.add(word[word.length-1]);
        }
    }

    private static void updateNumberRetries(int noTries) {
        if(noTries == 0)  {
            zeroTimes.incrementAndGet();
        } else if(noTries == 1){
            oneTime.incrementAndGet();
        } else if(noTries == 2) {
            twoTimes.incrementAndGet();
        } else if(noTries > 2 ) {
            threeTimesOrMore.incrementAndGet();
        } else {
            System.out.println("ERROR: noTries is less than zero OR not an integer.");
        }
    }
}