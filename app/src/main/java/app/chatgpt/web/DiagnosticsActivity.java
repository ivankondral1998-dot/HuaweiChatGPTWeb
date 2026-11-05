package app.chatgpt.web;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.media.AudioManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Device metadata only; no chat text, URLs, cookies or account information. */
public final class DiagnosticsActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PackageInfo provider = WebView.getCurrentWebViewPackage();
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            version = "unknown";
        }
        String info = "ChatGPT Web " + version
                + "\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
                + "\nWebView: " + (provider == null ? "unavailable" : provider.packageName + " " + provider.versionName)
                + "\nDisplay: " + getWindowManager().getDefaultDisplay().getRefreshRate() + " Hz"
                + "\nApp heap class: " + (manager == null ? "unknown" : manager.getMemoryClass() + " MB")
                + "\nMicrophone: " + (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED ? "allowed" : "not allowed")
                + audioDiagnostics()
                + performanceDiagnostics();

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        TextView details = new TextView(this);
        details.setText(info);
        details.setTextSize(17);
        details.setTextIsSelectable(true);
        content.addView(details);
        Button copy = new Button(this);
        copy.setText(R.string.copy_diagnostics);
        copy.setAllCaps(false);
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("WebView diagnostics", info));
                Toast.makeText(this, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show();
            }
        });
        content.addView(copy);
        scroll.addView(content);
        setContentView(scroll);
    }

    private String performanceDiagnostics() {
        android.content.SharedPreferences stats = getSharedPreferences("local_performance", MODE_PRIVATE);
        int samples = stats.getInt("samples", 0);
        return "\nUI heartbeat samples: " + samples
                + "\nUI scheduling stalls (>=350ms): " + stats.getInt("stalls", 0)
                + "\nMax UI scheduling delay: " + stats.getLong("max_ui_delay_ms", 0) + " ms"
                + "\nLast main-frame load: " + stats.getLong("last_navigation_ms", -1) + " ms"
                + "\nNote: heartbeat is not FPS; SPA transitions may not trigger page load.";
    }

    private String audioDiagnostics() {
        String info = "\nAudio settings: "
                + (checkSelfPermission(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
                   == android.content.pm.PackageManager.PERMISSION_GRANTED ? "allowed" : "not allowed");
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null) {
            info += "\nMicrophone muted: " + audio.isMicrophoneMute()
                    + "\nAudio mode: " + audio.getMode()
                    + "\nAudio inputs visible: " + audio.getDevices(AudioManager.GET_DEVICES_INPUTS).length;
        }
        return info;
    }
}
