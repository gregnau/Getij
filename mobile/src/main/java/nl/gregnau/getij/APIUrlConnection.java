package nl.gregnau.getij;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/*takes an API request (url) and performs it*/
public class APIUrlConnection {

    //gets data based on URL, passed back something
    public HashMap GetData(String url){

        HashMap result = new HashMap<String, String>();
        BufferedReader bufferedReader = null;
        try{
            //use the URL to create a new connection and read content
            URL APIUrl = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) APIUrl.openConnection();

            StringBuilder stringBuilder = new StringBuilder();
            bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            int responseCode = httpURLConnection.getResponseCode();
            String responseMessage = httpURLConnection.getResponseMessage();

            String line;
            while ((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line);
            }

            //add back our result data
            result.put("type", "success");
            result.put("data", stringBuilder.toString());

        }catch(Exception e){

            e.printStackTrace();
            //return error
            result.put("type", "failure");
            result.put("data", "there was an error reading in data from the API: " + e.getMessage());

        }finally {
            //close input steam and finish
            if(bufferedReader != null){
                try{
                    bufferedReader.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
