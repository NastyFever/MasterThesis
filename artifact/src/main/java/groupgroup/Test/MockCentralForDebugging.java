package groupgroup.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class MockCentralForDebugging {

    public static void main(String[] args) throws Exception {

        try {
            URL url = new URL("http://" + "127.0.0.1:8081" + "/regulator/update?numberOfAcceptedJobs=5");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            int responseCode = connection.getResponseCode();
            System.out.println("Finished with response code " + responseCode);
            connection.disconnect();
        } catch (MalformedURLException e) {
            System.out.println("Bad URL for regulator updates. Error: {}." + e.getMessage());
        } catch (ProtocolException e) {
            System.out.println("Protocol exception for regulator updates. Error: {}." + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException for regulator updates. Error: {}." + e.getMessage());
        }
    }
}
