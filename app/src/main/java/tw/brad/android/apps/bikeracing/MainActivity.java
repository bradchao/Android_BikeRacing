package tw.brad.android.apps.bikeracing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {
    private WebView webview;
    private boolean isMultiMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // go full screen
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);

        webview = (WebView)findViewById(R.id.webview);
        initWebView();
    }

    private void initWebView(){
        webview.setWebViewClient(new MyWebViewClient());
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new MyJS(), "android");
        webview.loadUrl("file:///android_asset/index.html");
    }

    private class MyWebViewClient extends WebViewClient {

    }

    public class MyJS{
        @JavascriptInterface
        public void selectMode(boolean isMulti){
            isMultiMode = isMulti;
            gotoWorkout();
        }
    }

    private void gotoWorkout(){
        if (isMultiMode){
            Intent it = new Intent(this, RacingStartActivity.class);
            startActivity(it);
        }else {
            Intent it = new Intent(this, WorkoutActivity.class);
            startActivity(it);
        }
    }
}
