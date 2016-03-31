package org.wso2.appserver.distributed.admin.clients;

import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import static org.testng.Assert.assertTrue;

/**
 * Created by dimuthu on 3/30/16.
 */
public class HttpClient {

    public String postRequest(URL url , String input) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        //String input = " ----- Your XML Array goes array";

        OutputStream os = conn.getOutputStream();
        os.write(input.getBytes());
        os.flush();

        /*assertTrue(conn.getResponseCode() == HttpURLConnection.HTTP_CREATED,
                "Response Code Mismatch. Expected 201 : Recived " + conn.getResponseCode());*/

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
        String response = br.readLine();
   return response;

    }


    public static HttpResponse sendGetRequest(String endpoint,
                                              String requestParameters, String contentType)
            throws IOException {
        if (endpoint.startsWith("http://")) {
            String urlStr = endpoint;
            if (requestParameters != null && requestParameters.length() > 0) {
                urlStr += "?" + requestParameters;
            }
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", contentType);
            conn.setRequestProperty("charset", "UTF-8");
            conn.setReadTimeout(10000);
            conn.connect();
            // Get the response
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.defaultCharset()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
            } finally {
                if (rd != null) {
                    rd.close();
                }
            }
            return new HttpResponse(sb.toString(), conn.getResponseCode());
        }
        return null;
    }
}
