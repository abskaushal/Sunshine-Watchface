package com.example.android.sunshine.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by Abhishek on 10-Sep-16.
 */
public class WeatherService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String HIGH_TEMP_KEY = "high_temp";
    private static final String LOW_TEMP_KEY = "low_temp";
    private static final String IMAGE_TEMP_KEY = "image_temp";
    private static final String TIME_STAMP_KEY = "time_stamp";

    private static final String START_WEATHER_SYNC_PATH = "/sync_weather";
    private static final String CURRENT_TEMP_PATH = "/current_temp";
    private static final String TIME_STAMP_PATH = "/time_stamp";
    private static final String MESSAGE_INTENT = "message_intent";

    private String mHighTemp;
    private String mLowTemp;
    private Bitmap mBitmap;
    private GoogleApiClient mGoogleApiClient;


    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        if (dataEventBuffer == null){
            return;
        }

        connectGoogleApiClient();
        for(DataEvent event : dataEventBuffer){
            String path = event.getDataItem().getUri().getPath();
            if(CURRENT_TEMP_PATH.equals(path)){
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                mHighTemp = dataMap.getString(HIGH_TEMP_KEY);
                mLowTemp = dataMap.getString(LOW_TEMP_KEY);
                new LoadBitmapAsyncTask().execute(dataMap.getAsset(IMAGE_TEMP_KEY));
            }else if(TIME_STAMP_PATH.equals(path)){
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                mHighTemp = dataMap.getString(HIGH_TEMP_KEY);
                mLowTemp = dataMap.getString(LOW_TEMP_KEY);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient,this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Wearable.DataApi.removeListener(mGoogleApiClient,this);
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals(START_WEATHER_SYNC_PATH)){
            connectGoogleApiClient();
            sendMessage(messageEvent.getSourceNodeId());
        }
    }

    private void sendMessage(String sourceNodeId) {

        Intent intent = new Intent(MESSAGE_INTENT);
        intent.putExtra(HIGH_TEMP_KEY,mHighTemp);
        intent.putExtra(LOW_TEMP_KEY,mLowTemp);

        try {
        if(mBitmap!=null){
            String filename = "image.png";

            FileOutputStream outputStream = openFileOutput(filename,MODE_PRIVATE);
            mBitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
            outputStream.close();

            intent.putExtra(IMAGE_TEMP_KEY,mBitmap);

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... params) {

            if(params.length > 0) {

                Asset asset = params[0];

                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        mGoogleApiClient, asset).await().getInputStream();

                if (assetInputStream == null) {
                    return null;
                }
                return BitmapFactory.decodeStream(assetInputStream);

            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {
                mBitmap = bitmap;
                sendMessage(null);
            }
        }
    }


    private void connectGoogleApiClient(){
        if(!mGoogleApiClient.isConnected()){
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(20, TimeUnit.SECONDS);

            if(!connectionResult.isSuccess()){
                return;
            }
        }
    }
}
