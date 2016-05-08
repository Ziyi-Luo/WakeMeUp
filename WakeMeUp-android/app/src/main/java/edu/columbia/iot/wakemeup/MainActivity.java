package edu.columbia.iot.wakemeup;

import android.app.TimePickerDialog;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
    private TextView mTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setLock.set(true);
        bindViews();
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
                //get_http();
                post_http();
                break;
        }
    }

    private void get_http(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://192.168.231.136:5000/";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        mTextView.setText("Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText("That didn't work!");
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void post_http(){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String URL = "http://209.2.210.84:5000/trigger";
        // Post params to be sent to the server
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("item", "connect");

        JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            mTextView.setText("Response is: "+ response.toString());
                            // VolleyLog.v("Response:%n %s", response.toString(4));
                        }
                        catch (Exception e) {
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
}