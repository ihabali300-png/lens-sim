package com.ihab.visionlab;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import androidx.core.content.FileProvider;
import java.io.File;

/* Full-screen wrapper around the IHAB Vision Lab web app, with camera and photo picking */
public class MainActivity extends Activity {
    static final String URL = "https://ihabali300-png.github.io/lens-sim/";
    WebView web; PermissionRequest pending; ValueCallback<Uri[]> fileCb; Uri shot;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        setContentView(web);
        immersive();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); s.setAllowFileAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (u.getHost() != null && u.getHost().endsWith("github.io")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) {}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest r) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) r.grant(r.getResources());
                else { pending = r; requestPermissions(new String[]{Manifest.permission.CAMERA}, 1); }
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT); pick.addCategory(Intent.CATEGORY_OPENABLE); pick.setType("image/*");
                Intent cam = null;
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        File f = new File(getCacheDir(), "shot_" + System.currentTimeMillis() + ".jpg");
                        shot = FileProvider.getUriForFile(MainActivity.this, "com.ihab.visionlab.files", f);
                        cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); cam.putExtra(MediaStore.EXTRA_OUTPUT, shot);
                        cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception e) { cam = null; }
                } else requestPermissions(new String[]{Manifest.permission.CAMERA}, 2);
                Intent ch = Intent.createChooser(pick, "صورة");
                if (cam != null) ch.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
                try { startActivityForResult(ch, 7); } catch (Exception e) { fileCb.onReceiveValue(null); fileCb = null; return false; }
                return true;
            }
        });
        if (b != null) web.restoreState(b); else web.loadUrl(URL);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req != 7 || fileCb == null) return;
        Uri[] out = null;
        if (res == RESULT_OK) out = (data != null && data.getData() != null) ? new Uri[]{data.getData()} : (shot != null ? new Uri[]{shot} : null);
        fileCb.onReceiveValue(out); fileCb = null;
    }

    @Override public void onRequestPermissionsResult(int req, String[] p, int[] g) {
        if (pending != null) {
            if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) pending.grant(pending.getResources()); else pending.deny();
            pending = null;
        }
    }

    void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) immersive(); }
    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
