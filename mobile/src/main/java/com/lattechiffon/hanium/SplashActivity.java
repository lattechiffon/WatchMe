package com.lattechiffon.hanium;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SplashActivity extends Activity {
    public final int PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    SharedPreferences pref, settingPref;
    SharedPreferences.Editor editor;
    BackgroundTask task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        pref = getSharedPreferences("UserData", Activity.MODE_PRIVATE);
        settingPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        if (!networkConnection()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
            builder.setTitle("네트워크에 연결되지 않았습니다.");
            builder.setMessage("로그인 서버에 접근할 수 없습니다.\n기기의 네트워크 연결 상태를 확인하여 주십시오.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            FirebaseMessaging.getInstance().subscribeToTopic("notice");
            FirebaseMessaging.getInstance().subscribeToTopic("emergency");
            FirebaseMessaging.getInstance().subscribeToTopic("feedback");
            FirebaseInstanceId.getInstance().getToken();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
                    builder.setTitle(getString(R.string.permission_dialog_title_access_fine_location));
                    builder.setMessage(getString(R.string.permission_dialog_body_access_fine_location)).setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
                }
            } else {
                if (pref.getBoolean("deviceRegister", false)) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            task = new BackgroundTask();
                            task.execute();
                        }
                    }, 1000);
                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(getApplicationContext(), DeviceRegisterActivity.class));
                            SplashActivity.this.finish();
                        }
                    }, 1000);
                }
            }
        }
    }

    private class BackgroundTask extends AsyncTask<String, Integer, okhttp3.Response> {

        ProgressDialog progressDialog = new ProgressDialog(SplashActivity.this);
        String name;
        String phone;
        String push;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.getWindow().setGravity(Gravity.BOTTOM);
            progressDialog.setMessage("이용자 인증 처리 중입니다.");
            progressDialog.show();

            name = pref.getString("name", "");
            phone = pref.getString("phone", "");
            push = FirebaseInstanceId.getInstance().getToken();
        }

        @Override
        protected okhttp3.Response doInBackground(String... arg0) {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("name", name)
                    .add("phone", phone)
                    .add("token", push)
                    .build();

            Request request = new Request.Builder()
                    .url("http://www.lattechiffon.com/hanium/login.php")
                    .post(body)
                    .build();

            try {
                return client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(okhttp3.Response a) {
            super.onPostExecute(a);

            progressDialog.dismiss();

            try {
                JSONObject json = new JSONObject(a.body().string());

                if (json.getString("result").equals("Authorized")) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                } else {
                    editor.putBoolean("deviceRegister", false);
                    editor.commit();

                    Toast.makeText(SplashActivity.this, "로그인에 실패하였습니다.", Toast.LENGTH_LONG).show();

                    startActivity(new Intent(getApplicationContext(), DeviceRegisterActivity.class));
                    finish();
                }

            } catch (Exception e) {

            }
        }
    }

    private boolean networkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(SplashActivity.this, getString(R.string.permission_toast_allow_access_fine_location), Toast.LENGTH_LONG).show();

                    if (pref.getBoolean("deviceRegister", false)) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                task = new BackgroundTask();
                                task.execute();
                            }
                        }, 1000);
                    } else {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(getApplicationContext(), DeviceRegisterActivity.class));
                                SplashActivity.this.finish();
                            }
                        }, 1000);
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
                    builder.setTitle(getString(R.string.permission_dialog_title_deny));
                    builder.setMessage(getString(R.string.permission_dialog_body_access_fine_location)).setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return;
            }
        }
    }
}
