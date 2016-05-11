package edu.columbia.iot.wakemeup;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.util.Log;

import android.view.WindowManager;
import android.widget.Button;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_set_ip;
    private Button btn_start_alarm;
    private TextView alarmTime;
    private RadioGroup alarmmode;
    private TextView txt_interval;
    private TextView interval;
    private AlarmManager alarmManager;
    private PendingIntent pi;
    public TextView mTextView;
    private InetAddress serverIP;
    private Handler mainHandler = new Handler();
    private boolean isWakeMeUp = true;
    private long alarm_time;
    private int alarm_interval;
    private File configFile;
    private String configName = "load.config";


    //Alarm runtime arguments
    private volatile boolean alarmStart = false;
    public volatile boolean running;
    private boolean isWakeMeUp_rt;
    private long alarm_time_rt;
    private int alarm_interval_rt;

    //Final arguments
    final static long QUERY_INTERVAL    = 30000;
    final static long INTERVAL_MINUTE   = 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        alarmmode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                switch (checkedId) {
                    case R.id.rb_normal:
                        txt_interval.setVisibility(View.INVISIBLE);
                        interval.setVisibility(View.INVISIBLE);
                        isWakeMeUp = false;
                        break;
                    case R.id.rb_wakemeup:
                        txt_interval.setVisibility(View.VISIBLE);
                        interval.setVisibility(View.VISIBLE);
                        isWakeMeUp = true;
                        break;
                }
            }
        });
        configFile = new File(this.getFilesDir(), configName);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
            String line = null;
            while ((line = br.readLine()) != null) {
                if(line.charAt(line.length()-1)=='\n')
                    line = line.substring(0,line.length()-1);
                Log.e("info ",line);
                String[] info = line.split("=");
                switch (info[0]){
                    case "time":
                        String[] hm = info[1].split(":");
                        int hour = Integer.parseInt(hm[0]);
                        int min = Integer.parseInt(hm[1]);
                        if(hour < 12)
                            alarmTime.setText(String.format("%02d:%02d AM",hour,min));
                        else
                            alarmTime.setText(String.format("%02d:%02d PM",hour-12,min));
                        alarm_time = setCalendartime(hour, min);
                        break;
                    case "interval":
                        alarm_interval = Integer.parseInt(info[1]);
                        interval.setText(String.format("%2d mins",alarm_interval));
                        break;
                    case "server":
                        serverIP = InetAddress.getByName(info[1]);
                        break;
                }
            }
        }catch(Exception e){
            Log.e("Failed","Log file failed to load");
            alarm_time = setCalendartime(7, 0);
            alarm_interval = 20;
            try {
                serverIP = InetAddress.getByName("160.39.254.56");
                updateLogfile();
            } catch (UnknownHostException e1) {
                Toast.makeText(MainActivity.this, "Server unreachable, please set server first.", Toast.LENGTH_SHORT).show();
            }
        }finally {
            if (br != null) {
                try {
                    br.close();
                }catch(IOException e){}
            }
        }

        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Bundle info = msg.getData();
                if (info.containsKey("invoke")) {
                    switch(info.getInt("invoke")){
                        case 0:
                            startalarm();
                            running = false;
                            alarmStart = false;
                            btn_start_alarm.setText("START");
                            break;
                    }

                }
            }
        };

    }

    private long setCalendartime(int hour_of_day, int min) {
        long alarm_time = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(alarm_time);
        c.set(Calendar.HOUR_OF_DAY, hour_of_day);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() < alarm_time)
            alarm_time = c.getTimeInMillis() + AlarmManager.INTERVAL_DAY;
        else
            alarm_time = c.getTimeInMillis();
        return alarm_time;
    }

    private void bindViews() {
        btn_set_ip = (Button) findViewById(R.id.btn_set_ip);
        mTextView = (TextView) findViewById(R.id.text);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        btn_start_alarm = (Button) findViewById(R.id.btn_start_alarm);
        alarmTime = (TextView) findViewById(R.id.alarmTime);
        txt_interval = (TextView) findViewById(R.id.txt_interval);
        interval = (TextView) findViewById(R.id.interval);
        alarmmode = (RadioGroup) findViewById(R.id.alarmmode);

        Intent intent = new Intent(MainActivity.this, ClockActivity.class);
        pi = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);

        btn_set_ip.setOnClickListener(this);
        alarmTime.setOnClickListener(this);
        interval.setOnClickListener(this);
        btn_start_alarm.setOnClickListener(this);

    }

    private void updateLogfile(){
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter((new FileOutputStream(configFile))));
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(alarm_time);
            bw.write(String.format("time=%02d:%02d\n", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
            bw.write("interval="+Integer.toString(alarm_interval)+"\n");
            bw.write("server="+serverIP.getHostAddress()+"\n");

        } catch (Exception e){
            Log.e("Failed","Unable to write the config file");
        } finally {
            if(bw != null)
                try {
                    bw.close();
                }catch (IOException e1){
                    Log.e("Error","Unknown IOException in updateLogfile()");
                }
        }
    }
    @Override
    public void onClick(View v) {
        AlertDialog dialog;
        AlertDialog.Builder builder;
        switch (v.getId()) {
            case R.id.alarmTime:
                Calendar currentTime = Calendar.getInstance();
                currentTime.setTimeInMillis(alarm_time);
                new TimePickerDialog(MainActivity.this, 0,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view,
                                                  int hourOfDay, int minute) {
                                alarm_time = setCalendartime(hourOfDay, minute);
                                updateLogfile();
                                String str;
                                if (hourOfDay < 12)
                                    str = String.format("%02d:%02d AM", hourOfDay, minute);
                                else
                                    str = String.format("%02d:%02d PM", hourOfDay - 12, minute);
                                alarmTime.setText(str);
                            }
                        }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime
                        .get(Calendar.MINUTE), false).show();
                break;
            case R.id.interval:
                builder = new AlertDialog.Builder(this);
                builder.setTitle("Set flexible time interval");
                // Set up the input
                final EditText input = new EditText(this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setFilters(new InputFilter[]{
                        // Maximum 2 characters.
                        new InputFilter.LengthFilter(2),
                        // Digits only.
                        DigitsKeyListener.getInstance(),  // Not strictly needed, IMHO.
                });
                input.setKeyListener(DigitsKeyListener.getInstance());

                input.setText(interval.getText().subSequence(0, 2));
                builder.setView(input);
                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int interv = Integer.parseInt(input.getText().toString());
                        if (interv <= 0) {
                            Toast.makeText(MainActivity.this, "Invalid: must larger than 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        alarm_interval = interv;
                        updateLogfile();
                        String info = input.getText() + " mins";
                        interval.setText(info);

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
                break;
            case R.id.btn_start_alarm:
                while (alarm_time < System.currentTimeMillis())
                    alarm_time += AlarmManager.INTERVAL_DAY;
                if (!alarmStart) {
                    alarm_time_rt = alarm_time;
                    isWakeMeUp_rt = isWakeMeUp;
                    if (isWakeMeUp_rt) {
                        alarm_interval_rt = alarm_interval;
                        if ((alarm_time_rt - alarm_interval_rt * INTERVAL_MINUTE) < System.currentTimeMillis()) {
                            Toast.makeText(MainActivity.this, "Sleeping time too short, please use regular alarm.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        running = true;
                        WaitUntil wait = new WaitUntil(serverIP.getHostAddress());
                        wait.start();
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarm_time_rt, pi);
                        Toast.makeText(MainActivity.this, "Remaining time: "
                                        + (long) Math.ceil((double) (alarm_time_rt - System.currentTimeMillis()) / INTERVAL_MINUTE)
                                        + " minute(s)",
                                Toast.LENGTH_SHORT).show();
                    }
                    alarmStart = true;
                    btn_start_alarm.setText("STOP");
                } else {
                    if (isWakeMeUp_rt) {
                        running = false;
                    } else {
                        alarmManager.cancel(pi);
                    }
                    Toast.makeText(MainActivity.this, "The alarm has been cancelled", Toast.LENGTH_SHORT)
                            .show();
                    alarmStart = false;
                    btn_start_alarm.setText("START!");
                }
                break;
            case R.id.btn_set_ip:
                builder = new AlertDialog.Builder(this);
                builder.setTitle("Set server IP");
                final EditText input_ip = new EditText(this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input_ip.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                input_ip.setFilters(new InputFilter[]{
                        // Maximum 2 characters.
                        new InputFilter.LengthFilter(39)
                });
                // input_ip.setKeyListener(DigitsKeyListener.getInstance());

                input_ip.setText(serverIP.getHostAddress());
                builder.setView(input_ip);
                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            serverIP = InetAddress.getByName(input_ip.getText().toString());
                            updateLogfile();
                        }catch(Exception e){
                            Toast.makeText(MainActivity.this, "Failed to set IP: invalid ip", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
                break;


        }
    }


    public void startalarm() {
        Intent intent = new Intent(this, ClockActivity.class);
        startActivity(intent);
    }


    class WaitUntil extends Thread {
        private String serverIP;
        private long start_sending_time;

        public WaitUntil(String serverIP) {
            this.serverIP = serverIP;
            long half_an_hour = 30 * INTERVAL_MINUTE;
            start_sending_time = alarm_time_rt - alarm_interval_rt * INTERVAL_MINUTE - half_an_hour;

        }

        public void run() {
            try {
                while (running) {
                    if (System.currentTimeMillis() < start_sending_time)
                        Thread.sleep(INTERVAL_MINUTE/2);
                    else
                        break;
                }
                if (running) {
                    post_http(alarm_time_rt, alarm_interval_rt);
                }
            } catch (InterruptedException e) {
                Log.e("Error: ", e.getMessage());
            }
        }

        private void post_http(long waketime, int interval) {
            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
            final String URL = "http://" + serverIP + ":5000/trigger";
            // Post params to be sent to the server
            HashMap<String, String> params = new HashMap<String, String>();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(waketime);
            params.put("type", "open");
            params.put("waketime", Integer.toString(c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE)));
            params.put("day", Integer.toString(c.get(Calendar.DAY_OF_MONTH)));
            params.put("interval", Integer.toString(interval));
            final PeriodGet myThread = new PeriodGet(serverIP);

            JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String info = response.getString("data");
                                if (info.equals("ok")) {
                                    myThread.start();
                                    VolleyLog.e("INFO: ", "Server set done.");
                                } else {
                                    VolleyLog.e("post_http(): Server failed to startup.");
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

    }

    ;

    class PeriodGet extends Thread {
        private String serverIP;
        private boolean end;

        public PeriodGet(String serverIP) {
            this.serverIP = serverIP;
            end = false;
        }

        public void run() {
            try {
                while (!end && running) {
                    Thread.sleep(QUERY_INTERVAL);
                    Log.e("INFO: ", "Invoke get_http");
                    get_http();
                }
                if (running) {
                    Message message = new Message();
                    Bundle b = new Bundle();
                    b.putInt("invoke", 0);
                    message.setData(b);
                    MainActivity.this.mainHandler.sendMessage(message);
                }
            } catch (InterruptedException e) {
                Log.e("Error: ", e.getMessage());
            }
            end = false;
        }

        private void get_http() {
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
            String url;
            if (serverIP != null) {
                url = "http://" + serverIP + ":5000/wakeornot";
            } else {
                return;
            }

            // Request a string response from the provided URL.
            JsonObjectRequest req = new JsonObjectRequest(url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                boolean info = response.getBoolean("data");
                                Log.e("info message", Boolean.toString(info));
                                if (info) {
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

    }
}



