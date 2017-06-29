package com.whb.hlyd.webrtcdemo;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.whb.hlyd.webrtcdemo.WebSocketChannelClient.WebSocketChannelEvents;
import com.whb.hlyd.webrtcdemo.WebSocketChannelClient.WebSocketConnectionState;
import com.whb.hlyd.webrtcdemo.util.AppRTCClient;
import com.whb.hlyd.webrtcdemo.util.Contents;
import com.whb.hlyd.webrtcdemo.util.SPUtils;
import com.whb.hlyd.webrtcdemo.util.SinglingReadyEvents;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.LinkedList;

/**
 * Negotiates signaling for chatting with https://appr.tc "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 * <p>
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
public class WebSocketRTCClient implements AppRTCClient, WebSocketChannelEvents {
    private static final String TAG = "WSRTCClient";
    private static final String ROOM_JOIN = "join";
    private static final String ROOM_MESSAGE = "message";
    private static final String ROOM_LEAVE = "leave";

    private enum ConnectionState {NEW, CONNECTED, CLOSED, ERROR}

    private enum MessageType {MESSAGE, LEAVE}

    private final Handler handler;
    private boolean initiator;
    private SignalingEvents events;
    private WebSocketChannelClient wsClient;
    private ConnectionState roomState;
    private RoomConnectionParameters connectionParameters;
    private String messageUrl;
    private String leaveUrl;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private LinkedList<IceCandidate> iceCandidates = new LinkedList<>();

    public WebSocketRTCClient(SignalingEvents events) {
        this.events = events;
        roomState = ConnectionState.NEW;
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    // --------------------------------------------------------------------
    // AppRTCClient interface implementation.
    // Asynchronously connect to an AppRTC room URL using supplied connection
    // parameters, retrieves room parameters and connect to WebSocket server.
    @Override
    public void connectToRoom(RoomConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        handler.post(new Runnable() {
            @Override
            public void run() {
                connectToRoomInternal();
            }
        });
    }

    @Override
    public void disconnectFromRoom() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                disconnectFromRoomInternal();
                handler.getLooper().quit();
            }
        });
    }

    // Connects to room - function runs on a local looper thread.
    private void connectToRoomInternal() {
        String connectionUrl = getConnectionUrl(connectionParameters);
        Log.d(TAG, "Connect to room: " + connectionUrl);
        roomState = ConnectionState.NEW;
        wsClient = new WebSocketChannelClient(handler, this);
//TODO

//        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
//        iceServers.add(new PeerConnection.IceServer("stun.l.google.com:19302"));
//        iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
//        iceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
//        iceServers.add(new PeerConnection.IceServer("stunserver.org"));
//        iceServers.add(new PeerConnection.IceServer("stun.voipstunt.com"));

        SignalingParameters signalingParameters = new SignalingParameters(iceServers, true,
                "clientid", connectionUrl, connectionParameters.roomId, null, iceCandidates);//TODO 暂时将posturl当作roomid

        SinglingReadyEvents events1=new SinglingReadyEvents() {
            @Override
            public void start(final SignalingParameters params) {
                WebSocketRTCClient.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        WebSocketRTCClient.this.signalingParametersReady(params);
                    }
                });
            }
        };
        events.onConnectedToRoom(signalingParameters,events1);
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        signalingParametersReady(signalingParameters);

//    RoomParametersFetcher.RoomParametersFetcherEvents callbacks = new RoomParametersFetcher.RoomParametersFetcherEvents() {
//      @Override
//      public void onSignalingParametersReady(final SignalingParameters params) {
//
//      }
//
//      @Override
//      public void onSignalingParametersError(String description) {
//        WebSocketRTCClient.this.reportError(description);
//      }
//    };
//
//    new RoomParametersFetcher(connectionUrl, null, callbacks).makeRequest();
    }

    // Disconnect from room and send bye messages - runs on a local looper thread.
    private void disconnectFromRoomInternal() {
        Log.d(TAG, "Disconnect. Room state: " + roomState);
        if (roomState == ConnectionState.CONNECTED) {
            Log.d(TAG, "Closing room.");
//      sendPostMessage(MessageType.LEAVE, leaveUrl, null);
        }
        roomState = ConnectionState.CLOSED;
        if (wsClient != null) {
            wsClient.disconnect(true);
        }
    }

    // Helper functions to get connection, post message and leave message URLs
    private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
        Log.e("魏红彬", connectionParameters.roomUrl + connectionParameters.roomId);
        return connectionParameters.roomUrl + connectionParameters.roomId;
    }

//  private String getMessageUrl(
//      RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
//    return connectionParameters.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters.roomId
//        + "/" + signalingParameters.clientId + getQueryString(connectionParameters);
//  }
//
//  private String getLeaveUrl(
//      RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
//    return connectionParameters.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters.roomId + "/"
//        + signalingParameters.clientId + getQueryString(connectionParameters);
//  }

//  private String getQueryString(RoomConnectionParameters connectionParameters) {
//    if (connectionParameters.urlParameters != null) {
//      return "?" + connectionParameters.urlParameters;
//    } else {
//      return "";
//    }
//  }

    // Callback issued when room parameters are extracted. Runs on local
    // looper thread.
    private void signalingParametersReady(final SignalingParameters signalingParameters) {
//    Log.d(TAG, "Room connection completed.");
//    if (connectionParameters.loopback
//        && (!signalingParameters.initiator || signalingParameters.offerSdp != null)) {
//      reportError("Loopback room is busy.");
//      return;
//    }
//    if (!connectionParameters.loopback && !signalingParameters.initiator
//        && signalingParameters.offerSdp == null) {
//      Log.w(TAG, "No offer SDP in room response.");
//    }
//    initiator = signalingParameters.initiator;
//    messageUrl = getMessageUrl(connectionParameters, signalingParameters);
//    leaveUrl = getLeaveUrl(connectionParameters, signalingParameters);
//    Log.d(TAG, "Message URL: " + messageUrl);
//    Log.d(TAG, "Leave URL: " + leaveUrl);
        roomState = ConnectionState.CONNECTED;

        // Fire connection and signaling parameters events.
        // Connect and register WebSocket client.
        wsClient.connect(signalingParameters.wssUrl, signalingParameters.wssPostUrl/*roomid*/);//
//        wsClient.register(connectionParameters.roomId, signalingParameters.clientId);
    }

    // Send local offer SDP to the other participant.
    @Override
    public void sendOfferSdp(final SessionDescription sdp) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (roomState != ConnectionState.CONNECTED) {
                    reportError("Sending offer SDP in non connected state.");
                    return;
                }
                String socketId = (String) SPUtils.get(BaseApplication.getAppContext(), Contents.FRIENDSID, "123");
                JSONObject jsdp = new JSONObject();
                jsonPut(jsdp, "sdp", sdp.description);
                jsonPut(jsdp, "type", "offer");
                JSONObject jData = new JSONObject();
                jsonPut(jData, "sdp", jsdp);
                jsonPut(jData, "socketId", socketId);
                JSONObject json = new JSONObject();
                jsonPut(json, "eventName", "__offer");
                jsonPut(json, "data", jData);
                Log.e("魏紅彬","发送"+json.toString());
                wsClient.send(json.toString());
            }
        });
    }

    // Send local answer SDP to the other participant.
    @Override
    public void sendAnswerSdp(final SessionDescription sdp) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (connectionParameters.loopback) {
                    Log.e(TAG, "Sending answer in loopback mode.");
                    return;
                }
                String socketId = (String) SPUtils.get(BaseApplication.getAppContext(), Contents.FRIENDSID, "123");
                JSONObject jsdp = new JSONObject();
                jsonPut(jsdp, "sdp", sdp.description);
                jsonPut(jsdp, "type", "answer");
                JSONObject jData = new JSONObject();
                jsonPut(jData, "sdp", jsdp);
                jsonPut(jData, "socketId", socketId);
                JSONObject json = new JSONObject();
                jsonPut(json, "eventName", "__answer");
                jsonPut(json, "data", jData);
                wsClient.send(json.toString());
            }
        });
    }

    // Send Ice candidate to the other participant.
    @Override
    public void sendLocalIceCandidate(final IceCandidate candidate) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                String socketId = (String) SPUtils.get(BaseApplication.getAppContext(), Contents.FRIENDSID, "123");
                JSONObject jData = new JSONObject();
                jsonPut(jData, "label", candidate.sdpMLineIndex);
                jsonPut(jData, "socketId", /*candidate.sdpMid*/socketId);
                jsonPut(jData, "id", candidate.sdpMid);
                jsonPut(jData, "candidate", candidate.sdp);
                JSONObject json = new JSONObject();
                jsonPut(json, "eventName", "__ice_candidate");
                jsonPut(json, "data", jData);
                Log.e("魏红彬", "发送__ice_candidate:" + json.toString());
                wsClient.send(json.toString());
                if (initiator) {
                    // Call initiator sends ice candidates to GAE server.
                    if (roomState != ConnectionState.CONNECTED) {
                        reportError("Sending ICE candidate in non connected state.");
                        return;
                    }
                    wsClient.send(json.toString());
//                 sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
                    if (connectionParameters.loopback) {
                        events.onRemoteIceCandidate(candidate);
                    }
                } else {
                    // Call receiver sends ice candidates to websocket server.
                    wsClient.send(json.toString());
                }
            }
        });
    }

    // Send removed Ice candidates to the other participant.
    @Override
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "remove-candidates");
                JSONArray jsonArray = new JSONArray();
                for (final IceCandidate candidate : candidates) {
                    jsonArray.put(toJsonCandidate(candidate));
                }
                jsonPut(json, "candidates", jsonArray);
                if (initiator) {
                    // Call initiator sends ice candidates to GAE server.
                    if (roomState != ConnectionState.CONNECTED) {
                        reportError("Sending ICE candidate removals in non connected state.");
                        return;
                    }
                    wsClient.send(json.toString());
//          sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
                    if (connectionParameters.loopback) {
                        events.onRemoteIceCandidatesRemoved(candidates);
                    }
                } else {
                    // Call receiver sends ice candidates to websocket server.
                    wsClient.send(json.toString());
                }
            }
        });
    }

    // --------------------------------------------------------------------
    // WebSocketChannelEvents interface implementation.
    // All events are called by WebSocketChannelClient on a local looper thread
    // (passed to WebSocket client constructor).
    @Override
    public void onWebSocketMessage(final String msg) {
        if (wsClient.getState() != WebSocketConnectionState.REGISTERED) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(msg);
            String eventName = (String) jsonObject.get("eventName");
            dealWithResult(eventName, jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void dealWithResult(String eventName, JSONObject jsonObject) {
        JSONObject jData = null;
        switch (eventName) {
            case "_peers":
                try {
                    jData = jsonObject.getJSONObject("data");
                    JSONArray cons = jData.getJSONArray("connections");;
                    String youId = jData.getString("you");
                    if(cons.length()==0){
                    }else{
                        String ids=cons.getString(cons.length()-1);
                        SPUtils.put(BaseApplication.getAppContext(), Contents.FRIENDSID, ids);
                        events.onCreateOffer();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "_new_peer":
                try {
                    jData = jsonObject.getJSONObject("data");
                    String socketId = jData.getString("socketId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "_remove_peer":
                try {
                    jData = jsonObject.getJSONObject("data");
                    String socketId = jData.getString("socketId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "_offer"://收到offer
                try {
                    jData = jsonObject.getJSONObject("data");
                    String socketId = jData.getString("socketId");
                    JSONObject jsdp = jData.getJSONObject("sdp");
                    String type=jsdp.getString("type");
                    String sdp=jsdp.getString("sdp");
                    SPUtils.put(BaseApplication.getAppContext(), Contents.FRIENDSID, socketId);
                    //去创建并发送answer
                    events.onCreateAnwser(sdp);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "_answer"://收到answer
                try {
                    jData = jsonObject.getJSONObject("data");
                    String socketId = jData.getString("socketId");
                    JSONObject jsdp = jData.getJSONObject("sdp");
                    String type=jsdp.getString("type");
                    String sdp=jsdp.getString("sdp");
                    SPUtils.put(BaseApplication.getAppContext(), Contents.FRIENDSID, socketId);
                    events.onRemoteDescription(new SessionDescription(SessionDescription.Type.fromCanonicalForm("ANSWER"), sdp));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "_ice_candidate"://收到_ice_candidate
                try {
                    jData = jsonObject.getJSONObject("data");
                    String label = jData.getString("label");
                    String candt = jData.getString("candidate");
                    String socketId = jData.getString("socketId");
                    String id=jData.getString("id");
                    events.onRemoteIceCandidate(new IceCandidate(id, Integer.parseInt(label), candt));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

        }
    }

    @Override
    public void onWebSocketClose() {
        events.onChannelClose();
    }

    @Override
    public void onWebSocketError(String description) {
        reportError("WebSocket error: " + description);
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (roomState != ConnectionState.ERROR) {
                    roomState = ConnectionState.ERROR;
                    events.onChannelError(errorMessage);
                }
            }
        });
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    // Converts a Java candidate to a JSONObject.
    private JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

}
