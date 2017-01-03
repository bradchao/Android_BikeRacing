package tw.brad.android.apps.bikeracing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class RacingStartActivity extends AppCompatActivity {
    private Timer timer;
    private ListView racingList;
    private SimpleAdapter adapter;
    private LinkedList<HashMap<String,String>> data;
    private String[] from = {"name_creator"};
    private int[] to = {R.id.item_racing_creator};
    private UIHandler uiHandler;
    private String room_id, my_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_racing_start);

        uiHandler = new UIHandler();

        racingList = (ListView)findViewById(R.id.racingList);
        initRacingList();

        // go full screen
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);

        timer = new Timer();
        timer.schedule(new GetRoomList(),0,1000);

        my_id = "1";
    }

    private void initRacingList(){
        data = new LinkedList<>();
        adapter = new SimpleAdapter(this,data,R.layout.item_racing,from,to);
        racingList.setAdapter(adapter);
        racingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent it = new Intent(RacingStartActivity.this, RacingWaitActivity.class);
                room_id = data.get(i).get("room_id");
                addGame();
                it.putExtra("room_id", room_id);
                it.putExtra("id_creator", data.get(i).get("id_creator"));
                it.putExtra("name_creator", data.get(i).get("name_creator"));
                it.putExtra("my_id", my_id);
                startActivity(it);
                RacingStartActivity.this.finish();

            }
        });
    }

    private void addGame(){
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url =
                            new URL(
                                    "http://www.brad.tw/fem/addGame.php?room_id=" + room_id + "&id=" + my_id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    conn.getInputStream();
                }catch(Exception ee){

                }
            }
        }.start();
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

    public void createGame(View view){
        new Thread(){
            @Override
            public void run() {
                try{
                    URL url =
                            new URL(
                                    "http://www.brad.tw/fem/createGame.php?account=brad&id=1");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    BufferedReader br =
                            new BufferedReader(
                                    new InputStreamReader(
                                            conn.getInputStream()));
                    String ret = br.readLine();
                    br.close();
                    if (!ret.equals("xx")){
                        Log.v("brad", "room_id:" + ret);
                        Intent it = new Intent(RacingStartActivity.this, RacingWaitActivity.class);
                        it.putExtra("my_id", my_id);
                        it.putExtra("room_id", ret);
                        it.putExtra("id_creator", my_id);
                        it.putExtra("name_creator", "iii");
                        startActivity(it);
                        RacingStartActivity.this.finish();
                    }else{
                        Log.v("brad", "Create Game Fail");
                    }
                }catch(Exception e){
                    Log.v("brad", "Create Game:" + e.toString());
                }
            }
        }.start();
    }
    private class GetRoomList extends TimerTask {
        @Override
        public void run() {
            try{
                URL url =
                        new URL(
                                "http://www.brad.tw/fem/getGameList.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()));
                String ret = br.readLine();
                parseGameList(ret);
                br.close();
            }catch(Exception e){
                Log.v("brad", "Get Game List:" + e.toString());
            }
        }
    }
    private void parseGameList(String json){
        try {
            data.clear();
            JSONArray root = new JSONArray(json);
            for (int i=0; i<root.length(); i++){
                JSONObject row = root.getJSONObject(i);
                String id = row.getString("id"); // room_id
                String id_creator = row.getString("id_creator");
                String name_creator = row.getString("name_creator");
                HashMap<String,String> item = new HashMap<>();
                item.put(from[0],name_creator);
                item.put("room_id",id);
                item.put("id_creator",id_creator);
                data.add(item);
            }
            uiHandler.sendEmptyMessage(0);
        }catch(Exception e){
            Log.v("brad", e.toString());
        }

    }

    private class UIHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            adapter.notifyDataSetChanged();
        }
    }
}
