package com.lattechiffon.hanium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static String SERVICE_CALLED_WEAR = "WearFallRecognition";

    private GoogleApiClient googleApiClient;
    private Vibrator vibrator;
    private Intent intentFalling;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private WatchViewStub stub;
    private TextView textView;

    private Node node;
    private boolean resolvingError = false;
    private int timerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, FallingCheckService.class);
        startService(intent);

        pref = getSharedPreferences("EmergencyData", Activity.MODE_PRIVATE);
        editor = pref.edit();

        intentFalling = getIntent();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                textView = (TextView) stub.findViewById(R.id.text);

                if (intentFalling.getBooleanExtra("falling", false)) {
                    stub.callOnClick();
                }
            }
        });
        stub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!pref.getBoolean("fall", false)) {
                    sendMessage("fall");

                    timerCount = 15;

                    long[] pattern = {0, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 3000};
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(pattern, -1);

                    delayTimer.sendEmptyMessage(0);

                    editor.putBoolean("fall", true);
                    editor.apply();
                } else {
                    sendMessage("stop");

                    if (vibrator != null) {
                        vibrator.cancel();
                    }

                    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    textView.setText("낙상 응급 알림이 중단되었습니다.");
                    textView.setTextSize(14);
                    textView.setTextColor(Color.rgb(160, 165, 167));
                    stub.setBackgroundColor(Color.rgb(35, 46, 51));

                    editor.putBoolean("fall", false);
                    editor.apply();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!resolvingError) {
            googleApiClient.connect();
        }
    }

    Handler delayTimer = new Handler() {
        public void handleMessage(Message msg) {
            if (pref.getBoolean("fall", false)) {
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                if (timerCount == 15 || timerCount == 14) {
                    textView.setText("낙상으로 인식되었습니다\n\n화면을 터치하면 알림이 중단됩니다");
                    textView.setTextSize(14);
                } else if (timerCount % 5 == 0 || timerCount % 5 == 4) {
                    textView.setText(timerCount + "초 남았습니다\n\n화면을 터치하면 알림이 중단됩니다");
                    textView.setTextSize(14);
                } else {
                    textView.setText(timerCount + "");
                    textView.setTextSize(150);
                }

                textView.setTextColor(Color.BLACK);
                stub.setBackgroundColor(Color.rgb(25, 204, 149));

                if (timerCount-- > 0) {
                    delayTimer.sendEmptyMessageDelayed(0, 1000);
                } else {
                    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    textView.setTextSize(14);
                    textView.setText("등록된 모든 보호자에게\n응급 푸쉬가 발송되었습니다");
                    textView.setTextColor(Color.rgb(0, 0, 0));
                    stub.setBackgroundColor(Color.rgb(250, 237, 125));

                    stub.setOnClickListener(null);

                    editor.putBoolean("fall", false);
                    editor.commit();
                }
            } else {
                if (!pref.getBoolean("fall", false)) {
                    editor.putBoolean("fall", true);
                    editor.commit();

                    stub.performClick();
                }
            }
        }
    };

    /**
     * Resolve the node = the connected device to send the message to
     */
    private void resolveNode() {
        Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            MainActivity.this.node = node;
                        }
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        resolveNode();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, FallingCheckService.class);
        stopService(intent);
    }

    /**
     * Send message to mobile handheld
     */
    private void sendMessage(String Key) {
        if (node != null && googleApiClient != null && googleApiClient.isConnected()) {
            Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), SERVICE_CALLED_WEAR + "--" + Key, null).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            /*if (!sendMessageResult.getStatus().isSuccess()) {
                                // error
                            }*/
                        }
                    }
            );
        }
    }
}
