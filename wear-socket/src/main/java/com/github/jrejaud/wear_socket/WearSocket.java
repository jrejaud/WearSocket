package com.github.jrejaud.wear_socket;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * Created by Jordan on 6/23/2015.
 */
public class WearSocket implements MessageApi.MessageListener, DataApi.DataListener {
    private static WearSocket ourInstance = new WearSocket();

    private GoogleApiClient googleApiClient = null;
    private final Semaphore nodeFound = new Semaphore(0,true);
    private String TAG = "WearSocket";
    private Context context;

    private String nodeID = null;
    private String receiverPath= null;
    private String dataPath = null;
    private String capability = null;

    private MessageListener messageReceived;
    private DataListener dataChanged;

    public static WearSocket getInstance() {
        return ourInstance;
    }

    private WearSocket() {
    }

    //********************************************************************
    //Setup and State Handling
    //********************************************************************

    public void setupAndConnect(Context context, final String capability) {
        this.context = context;
        this.capability = capability;
        Log.d(TAG, "Starting up Google Api Client");
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Google Api Client Connected, bundle: " + bundle);
                        findCapableNode(capability);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "onConnectedSuspended: " + i);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        showErrorAndCloseApp("Error, cannot connect", true);
                    }
                })
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
    }

    private void findCapableNode(final String capability) {
        Log.d(TAG,"Start looking for a capable node");
        new Thread(new Runnable() {
            @Override
            public void run() {
                CapabilityApi.GetCapabilityResult result =
                        Wearable.CapabilityApi.getCapability(googleApiClient,capability,CapabilityApi.FILTER_REACHABLE).await();
                CapabilityInfo capabilityInfo = result.getCapability();
                Set<Node> nodes = capabilityInfo.getNodes();
                String nodeID = findBestNodeId(nodes);
                Log.d(TAG,"Node found: "+nodeID);
                if (nodeID==null) {
                    String message = "Error, cannot find a connected device";
                    Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
                    new Throwable(message);
                    return;
                }
                WearSocket.this.nodeID = nodeID;
                nodeFound.release();
            }
        }).start();
    }

    private String findBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        for (Node node : nodes) {
            if (node.isNearby()) {
                bestNodeId = node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    @Deprecated
    private void findFirstNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start Node Search");
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi
                                .getConnectedNodes(googleApiClient).await();
                if (nodes.getNodes() == null) {
                    Log.d(TAG, "No nodes found");
                    showErrorAndCloseApp("Error, cannot find any nodes, make sure watch is paired to phone",true);
                }
                //TODO need to make this compatible with getting android wear connection over wifi instead of just bluetooth
                nodeID = nodes.getNodes().get(0).getId(); //This is causing errors
                Log.d(TAG,"Node found: "+ nodeID);
                nodeFound.release();
            }
        }).start();
    }


    //*************************************************************************************

    private void showErrorAndCloseApp(String message, boolean closeApp) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
        disconnect();
        if (closeApp) {
            ((Activity)context).finish();
        }
    }

    public void disconnect() {
        if (googleApiClient!=null) {
            googleApiClient.disconnect();
        }
        Wearable.MessageApi.removeListener(googleApiClient, this);
        Wearable.DataApi.removeListener(googleApiClient, this);
    }

    //********************************************************************
    //Send Message
    //********************************************************************

    public void sendMessage(String path, String message) {
        new sendMessageTask(path,message).execute();
    }

    private class sendMessageTask extends AsyncTask<Void, Void, Boolean> {

        private String path;
        private String message;

        public sendMessageTask(String path, String message) {
            this.path = path;
            this.message = message;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            while (nodeID ==null) {
                Log.d(TAG,"Node not found yet, waiting until one is found to send message");
                try {
                    nodeFound.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG,"Sending message to nodeID: "+ nodeID);

            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    googleApiClient, nodeID, path, message.getBytes()).await();

            if (!result.getStatus().isSuccess()) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean messageResponse) {
            if (messageResponse) {
                Log.d(TAG, "Message " + path + " : " + message + " sent successfully");
            }  else {
                showErrorAndCloseApp("Could not send message "+path+" : "+message,false);
            }

            super.onPostExecute(messageResponse);
        }
    }

    //********************************************************************
    //Receive Messages
    //********************************************************************

    public void startMessageListener(Context context, String path) {
        messageReceived = (MessageListener) context;
        this.receiverPath = path;
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    public interface MessageListener {
        void messageReceived(String path, String message);
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(receiverPath)) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String data = null;
                    try {
                        data = new String(messageEvent.getData(),"UTF-8");
                        messageReceived.messageReceived(messageEvent.getPath(), data);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    //********************************************************************
    //Update Data
    //********************************************************************

    public void updateDataItem(final String path, final String key, final Object object) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"Updating data item: "+key+" "+object.toString());
                Gson gson = new Gson();
                String jsonData = gson.toJson(object);
                if (!path.startsWith("/")) {
                    Log.e(TAG,"Path "+path+" must start with a /!");
                    return;
                }
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
                putDataMapRequest.getDataMap().putString(key, jsonData);
                PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
                DataApi.DataItemResult result = pendingResult.await();
                Log.d(TAG,result.toString());
                Log.d(TAG, "Update data item result: " + result.getStatus().getStatusMessage());
            }
        }).start();

    }

    //********************************************************************
    //Receive Data Changes and set keytypes
    //********************************************************************

    public void startDataListener(Context context, String path) {
        Log.d(TAG,"Starting data listener");
        dataChanged = (DataListener) context;
        dataPath = path;
        Wearable.DataApi.addListener(googleApiClient,this);
        keyTypes = new HashMap<>();

    }

    public interface DataListener {
        void dataChanged(String key, Object data);
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEventBuffer) {
        Log.d(TAG,"Data change event received");
        for (DataEvent dataEvent : dataEventBuffer) {
            DataItem dataItem = dataEvent.getDataItem();
            if (dataItem.getUri().getPath().compareTo(dataPath) == 0) {
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                Set<String> keys = dataMap.keySet();
                for (String key : keys) {
                    String data = dataMap.getString(key);
                    Gson gson = new Gson();
                    if (!keyTypes.containsKey(key)) {
                        Log.e(TAG,key+" key not associated to a datatype, please setKeyDataType");
                        return;
                    }
                    Object object = gson.fromJson(data, keyTypes.get(key));
                    dataChanged.dataChanged(key, object);
                }
            }
        }

    }

    private HashMap<String,Type> keyTypes;

    public void setKeyDataType(String key, Type type) {
        keyTypes.put(key, type);
    }
}