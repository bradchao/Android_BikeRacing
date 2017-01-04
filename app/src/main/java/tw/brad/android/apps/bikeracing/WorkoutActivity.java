package tw.brad.android.apps.bikeracing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/*
相關計算參考資料: http://xstarcd.github.io/wiki/Bike/gear_cadence_speed.html
 */
public class WorkoutActivity extends AppCompatActivity {
    private Timer timer;
    private int workoutTimer;
    private double workoutRPM;
    private double workoutSpeed;    // km/h
    private double workoutDistance; // km

    private TextView textClock, textSpeed, textDistance;

    private static final int UI_TIMER_UPDATE = 1;
    private static final int UI_GAMEINFO_UPDATE = 2;

    private UIHandler uiHandler;

    private boolean isRacing;
    private String room_id, my_id;
    private String strRacingInfo;
    private TextView textGameInfo;

    private VideoView videoView;
    private View view;
    private long lastClickTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // 產生全螢幕
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);

        init();
    }

    private void init(){
        view = findViewById(R.id.activity_workout);
        textClock = (TextView)findViewById(R.id.clock);
        textSpeed = (TextView)findViewById(R.id.speed);
        textDistance = (TextView)findViewById(R.id.distance);
        textGameInfo = (TextView)findViewById(R.id.racingInfo);

        videoView = (VideoView)findViewById(R.id.video);

        uiHandler = new UIHandler();

        timer = new Timer();
        workoutTimer = 0;
        timer.schedule(new ClockTask(),1000, 1000);
        workoutRPM = 0;

        Intent it = getIntent();
        room_id = it.getStringExtra("room_id");
        my_id = it.getStringExtra("my_id");
        if (room_id != null){
            isRacing = true;
            timer.schedule(new RacingTask(),0, 1000);
        }
        playLocalVideo();

        lastClickTime = System.currentTimeMillis();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long now = System.currentTimeMillis();
                long dt = now - lastClickTime;
                lastClickTime = now;
                workoutRPM = 60f / (dt / 1000f);
                Log.v("brad", "rpm:" + workoutRPM);
            }
        });
    }


    public void playLocalVideo() {
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setKeepScreenOn(true);
        videoView.setClickable(false);
        videoView.setMediaController(null);
        videoView.setVideoPath("android.resource://" + getPackageName() + "/" + R.raw.videoplayback);
        videoView.seekTo(12*1000);
        videoView.start();
        videoView.requestFocus();
    }

    private class ClockTask extends TimerTask {
        @Override
        public void run() {
            workoutTimer ++;
            calSpeed();
            uiHandler.sendEmptyMessage(UI_TIMER_UPDATE);
        }
    }

    private void calSpeed(){
        // unit: km/h
        // 時速＝踏頻×60分鐘×齒輪比×車輪周長/1000
        workoutSpeed = workoutRPM * 60 * 5.09 * 1516 / 1000 / 1000;
        calDistance();
    }
    private void calDistance(){
        // 因為每間隔一秒計算一次, 所以是以目前的距離
        // 加上一秒踏頻速度所移動之距離
        // 1km/h => 1/60/60 m/s
        workoutDistance += workoutSpeed / (60*60);
    }

    private class RacingTask extends TimerTask {
        @Override
        public void run() {
            try{
                URL url =
                        new URL(
                                "http://www.brad.tw/fem/getRacingInfo.php?room_id=" + room_id + "&my_id=" + my_id + "&distance=" + workoutDistance);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()));
                String ret = br.readLine();
                parseGameInfo(ret);
                br.close();
            }catch(Exception e){
                Log.v("brad", "Get Game List:" + e.toString());
            }

        }
    }

    private void parseGameInfo(String json){
        try {
            strRacingInfo = "";
            JSONArray root = new JSONArray(json);
            for (int i=0; i<root.length(); i++){
                JSONObject row = root.getJSONObject(i);
                String status = row.getString("status");
                String rname = row.getString("realname");
                String distance = row.getString("distance");
                strRacingInfo += rname + " : " +
                        String.format("%4.2f",Double.valueOf(distance))+"\n";
            }
            uiHandler.sendEmptyMessage(UI_GAMEINFO_UPDATE);
        }catch(Exception e){
            Log.v("brad", e.toString());
        }

    }


    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UI_TIMER_UPDATE:
                    updateHeaderInfo();
                    break;
                case UI_GAMEINFO_UPDATE:
                    updateGameInfo();
                    break;
            }
        }
    }
    private void updateHeaderInfo(){
        textClock.setText(toColok(workoutTimer));
        textSpeed.setText(String.format("%4.2f",workoutSpeed));
        textDistance.setText(String.format("%4.2f",workoutDistance));
    }
    private void updateGameInfo(){
        textGameInfo.setText(strRacingInfo);
    }

    private String toColok(int sec){
        int ss = workoutTimer % 60;
        int mm = workoutTimer / 60;
        return (mm<10?"0":"")+ mm + ":"+(ss<10?"0":"")+ ss;
    }

    @Override
    public void finish() {
        if (timer != null){
            timer.cancel();
            timer.purge();
            timer = null;
        }
        super.finish();
    }
}
