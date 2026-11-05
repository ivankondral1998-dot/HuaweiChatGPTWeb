package app.chatgpt.web;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.webkit.PermissionRequest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.SafeBrowsingResponse;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public final class MainActivity extends Activity {
    private static final String TAG = "ChatGPTWeb";
    private static final String START_URL = "https://chatgpt.com/";
    private static final int SURFACE_COLOR = Color.rgb(33, 33, 33);
    private static final ExecutorService COOKIE_WRITER = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean COOKIE_WRITE_PENDING = new AtomicBoolean();

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private static final int MICROPHONE_PERMISSION = 2001;
    private PermissionRequest pendingMicrophone;
    private boolean microphoneDialogOpen;
    private boolean activityVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        webView = new WebView(this);
        webView.setBackgroundColor(SURFACE_COLOR);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setScrollbarFadingEnabled(true);
        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(SURFACE_COLOR);
        content.setFitsSystemWindows(true);
        content.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(content);

        configureCookies();
        configureWebView();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(START_URL);
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(SURFACE_COLOR);
        window.setNavigationBarColor(SURFACE_COLOR);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    private void configureCookies() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        settings.setSafeBrowsingEnabled(true);

        webView.setWebViewClient(new ChatGptWebViewClient());
        webView.setWebChromeClient(new ChatGptChromeClient());
    }

    private final class ChatGptWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            cancelMicrophoneRequest();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();

            if (!request.isForMainFrame()) {
                return false;
            }

            if (shouldStayInWebView(uri)) {
                return false;
            }

            openExternal(uri);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (shouldStayInWebView(uri)) {
                return false;
            }
            openExternal(uri);
            return true;
        }

        @Override
        public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error) {
            super.onReceivedError(view, request, error);

            if (request.isForMainFrame()) {
                Log.w(TAG, "Main-frame WebView error: " + error);
                Toast.makeText(
                        MainActivity.this,
                        "Не удалось загрузить ChatGPT. Проверьте интернет и повторите попытку.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }

        @Override
        public void onSafeBrowsingHit(
                WebView view,
                WebResourceRequest request,
                int threatType,
                SafeBrowsingResponse callback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                callback.backToSafety(true);
            }
        }
    }

    private final class ChatGptChromeClient extends WebChromeClient {
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            boolean wantsAudio = false;
            for (String resource : request.getResources()) {
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) wantsAudio = true;
            }
            if (!wantsAudio || microphoneDialogOpen || pendingMicrophone != null
                    || !isTrustedMicrophoneRequest(request)) {
                request.deny();
                return;
            }
            pendingMicrophone = request;
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                completeMicrophoneRequest();
            } else {
                microphoneDialogOpen = true;
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION);
            }
        }

        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            if (pendingMicrophone == request) pendingMicrophone = null;
        }

        @Override
        public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {

            if (fileChooserCallback != null) {
                fileChooserCallback.onReceiveValue(null);
            }
            fileChooserCallback = filePathCallback;

            Intent intent;
            try {
                intent = fileChooserParams.createIntent();
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to create file chooser intent", e);
                fileChooserCallback = null;
                return false;
            }

            try {
                startActivityForResult(intent, 1001);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "No file chooser available", e);
                fileChooserCallback = null;
                Toast.makeText(
                        MainActivity.this,
                        "На устройстве нет доступного выбора файлов.",
                        Toast.LENGTH_SHORT
                ).show();
                return false;
            }
        }

    }

    private boolean shouldStayInWebView(Uri uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private boolean isTrustedMicrophoneRequest(PermissionRequest request) {
        return activityVisible && !isFinishing() && !isDestroyed() && webView != null
                && MicrophoneOriginPolicy.isTrusted(request.getOrigin().toString())
                && MicrophoneOriginPolicy.isTrusted(webView.getUrl());
    }

    private void completeMicrophoneRequest() {
        PermissionRequest request = pendingMicrophone;
        pendingMicrophone = null;
        if (request == null) return;
        if (isTrustedMicrophoneRequest(request)
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Grant only audio, never blindly grant every resource requested by a page.
            request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
        } else {
            request.deny();
            Toast.makeText(this, R.string.microphone_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void cancelMicrophoneRequest() {
        PermissionRequest request = pendingMicrophone;
        pendingMicrophone = null;
        if (request != null) request.deny();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MICROPHONE_PERMISSION) {
            microphoneDialogOpen = false;
            completeMicrophoneRequest();
        }
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No browser for external URI: " + uri, e);
            Toast.makeText(this, "Не найдено приложение для открытия ссылки.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001 && fileChooserCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileChooserCallback.onReceiveValue(result);
            fileChooserCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        activityVisible = false;
        cancelMicrophoneRequest();
        // flush() performs synchronous disk I/O. Keep it off the UI thread and
        // coalesce repeated stops (picker, rotation, background) into one write.
        if (COOKIE_WRITE_PENDING.compareAndSet(false, true)) {
            COOKIE_WRITER.execute(() -> {
                try {
                    CookieManager.getInstance().flush();
                } finally {
                    COOKIE_WRITE_PENDING.set(false);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        cancelMicrophoneRequest();
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
