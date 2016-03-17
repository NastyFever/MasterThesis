package groupgroup.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class MockCentralForDebugging {

    public static void main(String[] args) throws Exception {
        testUpdate();
        testInit();
    }

    /**
     * Only tests the update rest command REST call in the same manner as central will.
     */
    private static void testUpdate(){
        try {
            URL url = new URL("http://" + "127.0.0.1:8081" + "/regulator/update?numberOfAcceptedJobs=5");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            int responseCode = connection.getResponseCode();
            System.out.println("Finished with response code " + responseCode);
            connection.disconnect();
            if(responseCode!=404) {
                System.out.println("Update ok!");
            } else {
                System.out.println("Fail, 404");
            }
        } catch (MalformedURLException e) {
            System.out.println("Bad URL for regulator updates. Error: {}." + e.getMessage());
        } catch (ProtocolException e) {
            System.out.println("Protocol exception for regulator updates. Error: {}." + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException for regulator updates. Error: {}." + e.getMessage());
        }
    }

    /**
     * Only tests the update init command REST call in the same manner as central will.
     */
    private static void testInit(){
        try {
            URL url = new URL("http://" + "127.0.0.1:8081" + "/regulator/init");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            int responseCode = connection.getResponseCode();
            System.out.println("Finished with response code " + responseCode);
            connection.disconnect();
            if(responseCode!=404) {
                System.out.println("Init ok!");
            } else {
                System.out.println("Fail, 404");
            }
        } catch (MalformedURLException e) {
            System.out.println("Bad URL for regulator updates. Error: {}." + e.getMessage());
        } catch (ProtocolException e) {
            System.out.println("Protocol exception for regulator updates. Error: {}." + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException for regulator updates. Error: {}." + e.getMessage());
        }
    }
}
