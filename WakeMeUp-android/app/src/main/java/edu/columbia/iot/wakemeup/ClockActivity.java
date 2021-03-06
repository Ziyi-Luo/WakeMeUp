package edu.columbia.iot.wakemeup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by samluo on 5/4/16.
 */



public class ClockActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Calendar currentTime = Calendar.getInstance();
        currentTime.setTimeInMillis(System.currentTimeMillis());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);
        mediaPlayer = mediaPlayer.create(this,R.raw.por);
        mediaPlayer.start();
        //创建一个闹钟提醒的对话框,点击确定关闭铃声与页面
        new AlertDialog.Builder(ClockActivity.this).setTitle("Alarm")
                .setMessage("ALARM!\n Current time "+currentTime.get(Calendar.HOUR_OF_DAY)+":"+currentTime.get(Calendar.MINUTE))
                .setIcon(R.mipmap.alarm_icon)
                .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mediaPlayer.stop();
                        Intent intent = new Intent(Intent.ACTION_DEFAULT);
                        intent.putExtra("invoke",1);
                        sendBroadcast(intent);
                         // Log.e("Info ", "Sent a broadcast");
                        ClockActivity.this.finish();
                    }
                }).show();
    }
}
