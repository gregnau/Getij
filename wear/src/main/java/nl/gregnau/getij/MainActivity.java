package nl.gregnau.getij;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
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

import java.util.Date;

public class MainActivity extends WearableActivity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient googleClient;

    private LinearLayout mainContainer,apiContainer;
    private TextView apiMessage;
    private TextView apiDate;
    private TextView apiHeight;
    private RelativeLayout overlay;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set up our google Play-Services client
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Find all of our UI element
        apiMessage = (TextView) findViewById(R.id.apiMessage);
        apiDate = (TextView) findViewById(R.id.apiDate);
        apiHeight = (TextView) findViewById(R.id.apiHeight);
        mainContainer = (LinearLayout) findViewById(R.id.mainContainer);
        overlay =  (RelativeLayout) findViewById(R.id.overlay);
        apiButton = (Button) findViewById(R.id.apiButton);
        apiContainer = (LinearLayout) findViewById(R.id.apiContainer);

        // Click action for button, connect to mobile
        apiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //bring the loading overlay to the front
                overlay.setVisibility(View.VISIBLE);
                overlay.bringToFront();

                //start API request to phone
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/apiurl");
                putDataMapRequest.getDataMap().putString("message", "Getijden wordt geladen, moment alstublieft...");
                putDataMapRequest.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                putDataRequest.setUrgent();

                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleClient, putDataRequest);
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


    // Function triggered every time there is a data change event
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event: dataEvents){
            if(event.getType() == DataEvent.TYPE_CHANGED){
                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                // Response back from mobile message
                if(item.getUri().getPath().equals("/responsemessage")){

                    // Turn overlay off and bring main content back to front
                    overlay.setVisibility(View.INVISIBLE);
                    mainContainer.bringToFront();

                    // Collect all of our info
                    String error = dataMapItem.getDataMap().getString("error");
                    String unixTime = dataMapItem.getDataMap().getString("unixTime");
                    String height = dataMapItem.getDataMap().getString("height");

                    // Success
                    if(error == null){
                        apiMessage.setText("Current Time Info");
                        apiDate.setText("Date|Time: " + unixTime);
                        apiHeight.setText("Height: "+ height + " Meters");
                    }
                    // Error
                    else {
                        apiDate.setText(error);
                    }

                    apiButton.setVisibility(View.GONE);
                    apiContainer.setVisibility(View.VISIBLE);

                }
            }
        }
    }

}

