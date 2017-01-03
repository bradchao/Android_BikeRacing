package tw.brad.android.apps.bikeracing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class RacingWaitActivity extends AppCompatActivity {
    private Button go;
    private String my_id, room_id, id_creator, name_creator;
    private TextView textCreator, textWaitList;
    private Timer timer;
    private String strWaitList;
    private boolean isGo;
    private UIHandler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_racing_wait);


        // go full screen
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);

        go = (Button)findViewById(R.id.go);
        textCreator = (TextView)findViewById(R.id.creator);
        textWaitList = (TextView)findViewById(R.id.waitList);

        Intent it = getIntent();
        my_id = it.getStringExtra("my_id");
        room_id = it.getStringExtra("room_id");
        id_creator = it.getStringExtra("id_creator");
        name_creator = it.getStringExtra("name_creator");

        initView();

        uiHandler = new UIHandler();

    }
    private void initView(){
        textCreator.setText(name_creator);
        go.setVisibility(my_id.equals(id_creator)?View.VISIBLE:View.INVISIBLE);

        timer = new Timer();
        timer.schedule(new GetWaitInfo(),1000,1000);
    }

    private class GetWaitInfo extends TimerTask{
        @Override
        public void run() {
            try{
                URL url =
                        new URL(
                                "http://www.brad.tw/fem/getWaitList.php?room_id=" + room_id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()));
                String ret = br.readLine();
                parseWaitList(ret);
                br.close();
            }catch(Exception e){
                Log.v("brad", "Get Game List:" + e.toString());
            }

        }
    }
    private void parseWaitList(String json){
        try {
            strWaitList = "";
            JSONArray root = new JSONArray(json);
            for (int i=0; i<root.length(); i++){
                JSONObject row = root.getJSONObject(i);
                String status = row.getString("status");
                if (status.equals("1")){
                    isGo = true;
                    break;
                }
                String rname = row.getString("realname");
                strWaitList += rname + "\n";
            }
            if (isGo){
                gotoWorkout();
            }else{
                uiHandler.sendEmptyMessage(0);
            }

        }catch(Exception e){
            Log.v("brad", e.toString());
        }

    }

    public void go(View v){
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url =
                            new URL(
                                    "http://www.brad.tw/fem/startGame.php?room_id=" + room_id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    conn.getInputStream();
                }catch(Exception ee){}
            }
        }.start();
    }

    private void gotoWorkout(){
        Intent it = new Intent(this, WorkoutActivity.class);
        it.putExtra("my_id", my_id);
        it.putExtra("room_id", room_id);
        startActivity(it);
        finish();
    }

    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            textWaitList.setText(strWaitList);
        }
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
