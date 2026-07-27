package com.example.imanicommunityapp.bookingSys;



import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.auth.Repository.TokenManager;

import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DriverSocketFragment extends Fragment {

    private WebSocket webSocket;
    private TextView socketStatus, liveMessage, seatCount;
    private String driverId;

    public DriverSocketFragment() {
        super(R.layout.driver_socket_fragment);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        socketStatus = view.findViewById(R.id.socketStatus);
        liveMessage = view.findViewById(R.id.liveMessage);
        seatCount = view.findViewById(R.id.seatsRemaining);

        TokenManager tm = new TokenManager(requireContext());
        driverId = tm.getUserRole();  // we saved role = driver_id

        startWebSocket();
    }

    private void startWebSocket() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("ws://10.0.2.2:8000/ws/driver/" + driverId + "/")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                requireActivity().runOnUiThread(() ->
                        socketStatus.setText("Connected to server"));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                requireActivity().runOnUiThread(() -> {
                    try {

                        JSONObject obj = new JSONObject(text);
                        liveMessage.setText(obj.getString("message"));

                        if (obj.has("remaining_seats")) {
                            seatCount.setText("Remaining Seats: " + obj.getInt("remaining_seats"));
                        }

                    } catch (Exception e) {
                        liveMessage.setText("Invalid message received");
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                requireActivity().runOnUiThread(() ->
                        socketStatus.setText("Connection failed: " + t.getMessage()));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocket != null) webSocket.close(1000, "Closing");
    }
}

