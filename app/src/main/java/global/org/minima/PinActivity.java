package global.org.minima;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.minima.utils.MinimaLogger;

public class PinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MinimaLogger.log("WEBVIEW");

        setContentView(R.layout.view_webview);

        WebView web = findViewById(R.id.minima_webview);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient());

        web.loadUrl("https://www.google.com");

        MinimaLogger.log("WEBVIEW");

    }
}
