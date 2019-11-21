package nl.gregnau.getij;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private AppCompatActivity activity;
    private GoogleApiClient googleClient;
    private TextView messageContainer,apiDate,apiHeight;
    private RelativeLayout mainContainer,overlay;
    Button apiButton;

    // On successful connection to Play-Services, add data listener
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(googleClient, this);
    }

    // On resuming activity, reconnect Play-Services
    public void onResume(){
        super.onResume();
        googleClient.connect();
    }

    // On suspended connection, remove Play-Services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    // Pause listener, disconnect Play-Services
    public void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }

    // On failed connection to Play-Services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.activity = this;

        // Set custom navigation bar color
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        // Set up our google Play-Services client
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Find all of our UI element
        messageContainer = (TextView) findViewById(R.id.messageContainer);
        mainContainer = (RelativeLayout) findViewById(R.id.mainContainer);
        overlay = (RelativeLayout) findViewById(R.id.overlay);
        apiButton = (Button) findViewById(R.id.apiButton);
        apiDate = (TextView) findViewById(R.id.apiDate);
        apiHeight = (TextView) findViewById(R.id.apiHeight);

        // Click action for button, connect to mobile
        apiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Bring the loading overlay to the front
                overlay.setVisibility(View.VISIBLE);
                overlay.bringToFront();

                // Populate API information, in preparation for our API call
                APIInformation apiInformation = setUpAPIInformation();


                // Execute Async-task
                APIAsyncTask asyncTask = new APIAsyncTask();
                asyncTask.execute(apiInformation);
            }
        });

        // Click listener for the overlay, touch to dismiss it in case the API fails or takes too long
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlay.setVisibility(View.INVISIBLE);
                mainContainer.bringToFront();
            }
        });
    }


    // Watches for data item
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event: dataEvents){

            // Data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){
                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                // Received initiation message, start the process!
                if(item.getUri().getPath().equals("/apiurl")){
                    String message = dataMapItem.getDataMap().getString("message");
                    messageContainer.setText(message);

                    // Populate API information, in preparation for our API call
                    APIInformation apiInformation = setUpAPIInformation();

                    // Execute Async-task
                    APIAsyncTask asyncTask = new APIAsyncTask();
                    asyncTask.execute(apiInformation);
                }
            }
        }
    }


    // Checks to see if we are online (and can access the internet)
    protected boolean isOnline(){
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        boolean connected = false;
        if((networkInfo != null) && (networkInfo.isConnected())){
            connected = true;
        }

        return connected;
    }

    // Populates information for our API
    protected APIInformation setUpAPIInformation(){
        APIInformation apiInformation = new APIInformation();

        apiInformation.setAPIEndpoint("https://www.worldtides.info/api");
        HashMap arguments = new HashMap<String, String>();

        arguments.put("key", "5752e2b7-1e22-40fe-a79a-51f16215c6c0");  // API-key
        arguments.put("heights",""); // We want the 'heights' only
        arguments.put("lat", "52.076670"); // Latitude (The Hague, Netherlands)
        arguments.put("lon", "4.298610"); // Longitude (The Hague, Netherlands)
        arguments.put("step", "60"); // 60 seconds
        arguments.put("length", "30"); // 30 seconds

        // Determine the next time period (after right now)
        Long currentTime = System.currentTimeMillis();
        String time = String.valueOf(currentTime / 1000L);
        arguments.put("start", time);

        apiInformation.setAPIArguments(arguments);
        apiInformation.setAPIUrl();

        return apiInformation;
    }

    // Main Async-task to connect to our API and collect a response
    public class APIAsyncTask extends AsyncTask<APIInformation, String, HashMap> {

        // Execute before we start
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Execute background task
        protected HashMap doInBackground(APIInformation... params) {
            APIInformation apiInformation = params[0];
            HashMap result;

            if(isOnline()){
                // Perform the HTTP request
                APIUrlConnection apiUrlConnection = new APIUrlConnection();

                // Get the result back and process
                result = apiUrlConnection.GetData(apiInformation.getAPIUrl());
            }else{
                // We are not online, flag the error
                result = new HashMap();
                result.put("type", "failure");
                result.put("data", "Not currrently online, can't connect to API");
            }

            return result;
        }

        // Update progress
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        // Execute once we are done
        protected void onPostExecute(HashMap result) {
            super.onPostExecute(result);

            // Build our message back to the wearable (either with data or a failure message)
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/responsemessage");
            putDataMapRequest.getDataMap().putLong("time", new Date().getTime());

            // Success (we collected our data from the API)
            if(result.get("type") == "success"){
                // Get the JSON response data string
                String data = (String) result.get("data");

                // Create a new JSON object
                try{
                    JSONObject jsonObject = new JSONObject(data);
                    if(jsonObject.has("heights")){
                        JSONArray heights = (JSONArray) jsonObject.get("heights");

                        // Loop through all 'heights' objects to get data
                        for(int i = 0; i < heights.length(); i++){
                            // Get the specific object from the set
                            JSONObject heightObject = heights.getJSONObject(i);
                            // Get our time and height values
                            Integer unixTime = Integer.parseInt(heightObject.getString("dt"));
                            String height = heightObject.getString("height");

                            // Process the values to make them readable.
                            String heightTrimmed = height.substring(0, 5);

                            // Convert date unix string to a human readable format
                            Date date = new Date(unixTime * 1000L);
                            DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                            String dateFormatted = format.format(date);

                            // Add our data to be passed back to the wearable
                            putDataMapRequest.getDataMap().putString("unixTime", dateFormatted);
                            putDataMapRequest.getDataMap().putString("height", heightTrimmed);

                            // Show off data in the mobile app also
                            messageContainer.setText("Huidige Getidjinformatie:");
                            messageContainer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.f);
                            apiDate.setText(dateFormatted);
                            apiHeight.setText(heightTrimmed + " m");

                            // Hide loading overlay
                            overlay.setVisibility(View.INVISIBLE);
                            mainContainer.bringToFront();
                        }
                    }else{
                        Log.d("error", "there was no height parm returned from the API");
                        putDataMapRequest.getDataMap().putString("error", "There was an issue processing the JSON object returned from API");
                    }
                }catch(Exception e){
                    // Could not create the JSON object
                    Log.d("error", "error creating the json object: " + e.getMessage());
                    putDataMapRequest.getDataMap().putString("error", "There was an issue processing the JSON object returned from API");
                }

            }
            // Failure (couldn't connect to the API or collect data)
            else if(result.get("type") == "failure"){
                Log.d("error", "There was an issue connecting to the API.");
                putDataMapRequest.getDataMap().putString("error", result.get("error").toString());
            }

            // Finalise our message and send it off (either success or failure)
            PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
            putDataRequest.setUrgent();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleClient, putDataRequest);
        }
    }
}
