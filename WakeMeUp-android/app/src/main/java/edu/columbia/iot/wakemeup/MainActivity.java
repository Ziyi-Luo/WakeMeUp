package edu.columbia.iot.wakemeup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import android.widget.Button;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btn_set;
    private Button btn_cancel;
    private Button btn_http_get;
    private AlarmManager alarmManager;
    private PendingIntent pi;
    private AtomicBoolean setLock = new AtomicBoolean();
    public TextView mTextView;
    private boolean startListening = false;
    private boolean end = false;
    private InetAddress serverIP;
    private MediaPlayer mediaPlayer;
    private Handler mainHandler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setLock.set(true);
        bindViews();
        mainHandler = new Handler(Looper.getMainLooper()){
          @Override
          public void handleMessage(Message msg) {
              Bundle info = msg.getData();
              if(info.containsKey("invoke")){
                  if(info.getInt("invoke") == 0)
                      startalarm();
              }
          }
          };
        try{
            serverIP = InetAddress.getByName("160.39.254.183");
        } catch (UnknownHostException e){
            Toast.makeText(MainActivity.this, "Server unreachable, please set server first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews() {
        btn_set = (Button) findViewById(R.id.btn_set);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        btn_http_get = (Button) findViewById(R.id.btn_http_get);
        mTextView = (TextView) findViewById(R.id.text);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(MainActivity.this, ClockActivity.class);
        pi = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);

        btn_set.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_http_get.setOnClickListener(this);

    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_set:
                Calendar currentTime = Calendar.getInstance();
                new TimePickerDialog(MainActivity.this, 0,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view,
                                                  int hourOfDay, int minute) {
                                if(!setLock.getAndSet(false))
                                    return;
                                //设置当前时间
                                Calendar c = Calendar.getInstance();
                                long currentTime = System.currentTimeMillis();
                                c.setTimeInMillis(currentTime);
                                // 根据用户选择的时间来设置Calendar对象
                                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                c.set(Calendar.MINUTE, minute);
                                if(c.getTimeInMillis()<currentTime)
                                    c.setTimeInMillis(c.getTimeInMillis()+86400000);
                                // ②设置AlarmManager在Calendar对应的时间启动Activity
                                alarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                                Log.e("Input time: ", hourOfDay + ":" + minute);
                                Log.e("Set time: ",c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE));   //这里的时间是一个unix时间戳
                                // 提示闹钟设置完毕:
                                Toast.makeText(MainActivity.this, "Set done, remaining time: "+ ((c.getTimeInMillis()-currentTime)/60000)+" minute(s)",
                                        Toast.LENGTH_SHORT).show();
                                setLock.set(true);
                            }
                        }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime
                        .get(Calendar.MINUTE), false).show();
                btn_cancel.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_cancel:
                alarmManager.cancel(pi);
                btn_cancel.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "闹钟已取消", Toast.LENGTH_SHORT)
                        .show();
                break;
            case R.id.btn_http_get:
                // get_http2();
                post_http(System.currentTimeMillis(), 900000);
                break;
        }
    }

    private void post_http(long waketime, long interval){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String URL = "http://" + serverIP.getHostAddress() + ":5000/trigger";
        // Post params to be sent to the server
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("type", "open");
        params.put("waketime", Long.toString(waketime));
        params.put("interval", Long.toString(interval));
        final PeriodGet myThread = new PeriodGet(serverIP.getHostAddress(),this);

        JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String info = response.getString("data");
                            if(info.equals("ok")) {
                                startListening = true;
                                myThread.start();
                                VolleyLog.e("INFO: ", "Server set done.");
                            }
                            else{
                                startListening = false;
                                VolleyLog.e("post_http(): Server failed to startup.");
                            }
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

// add the request object to the queue to be executed
        queue.add(req);
    }

    public void startalarm(){
        Intent intent = new Intent(this, ClockActivity.class);
        startActivity(intent);
    }

    class PeriodGet extends Thread {
        private String serverIP;
        private boolean end;
        public PeriodGet(String serverIP,MainActivity root){
            this.serverIP = serverIP;
            end=false;
        }

        public void run(){
            try {
                while (!end) {
                    Thread.sleep(5000);
                    Log.e("INFO: ", "Invoke get_http");
                    get_http();
                }
                Message message = new Message();
                Bundle b = new Bundle();
                b.putInt("invoke",0);
                message.setData(b);
                MainActivity.this.mainHandler.sendMessage(message);
            } catch (InterruptedException e){
                Log.e("Error: ",e.getMessage());
            }
            end=false;
        }

        private void get_http(){
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
            String url = new String();
            if(serverIP != null) {
                url = "http://" + serverIP + ":5000/wakeornot";
            }
            else {
                return;
            }

            // Request a string response from the provided URL.
            JsonObjectRequest req = new JsonObjectRequest(url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                boolean info = response.getBoolean("data");
                                Log.e("info message",Boolean.toString(info));
                                if(info) {
                                    end = true;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                }
            });

// add the request object to the queue to be executed
            queue.add(req);
        }

    };
}


