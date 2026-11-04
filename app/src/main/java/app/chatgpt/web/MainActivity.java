package app.chatgpt.web;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

/**
 * Stable launcher for the official ChatGPT website.
 *
 * Authentication remains entirely in a real browser context. This Activity does
 * not embed WebView, inspect cookies/tokens, or intercept authentication traffic.
 */
public final class MainActivity extends Activity {
    private static final String TAG = "ChatGPTWeb";
    private static final Uri START_URI = Uri.parse("https://chatgpt.com/");

    private TextView statusView;
    private boolean browserWasOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        browserWasOpened = savedInstanceState != null
                && savedInstanceState.getBoolean("browserWasOpened", false);
        setContentView(buildContent());
        updateBrowserStatus(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusView != null && browserWasOpened) {
            statusView.setText("Браузер был открыт. Если вход завершён — нажмите «Открыть ChatGPT» снова. "
                    + "Если Huawei-браузер не принимает вход, используйте «Выбрать браузер».");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("browserWasOpened", browserWasOpened);
        super.onSaveInstanceState(outState);
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(18, 18, 18));
        window.setNavigationBarColor(Color.rgb(18, 18, 18));
        window.setBackgroundDrawableResource(android.R.color.transparent);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(28));
        root.setBackgroundColor(Color.rgb(18, 18, 18));
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("ChatGPT Web");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Официальный chatgpt.com в браузере устройства");
        subtitle.setTextColor(Color.rgb(180, 180, 180));
        subtitle.setTextSize(15f);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(8);
        root.addView(subtitle, subtitleParams);

        Space spacer = new Space(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(30)));

        Button openButton = makeButton("Открыть ChatGPT");
        openButton.setOnClickListener(v -> openPreferredBrowserSurface());
        root.addView(openButton, fullWidth());

        Button chooseButton = makeButton("Выбрать браузер");
        chooseButton.setOnClickListener(v -> openBrowserChooser());
        LinearLayout.LayoutParams chooseParams = fullWidth();
        chooseParams.topMargin = dp(12);
        root.addView(chooseButton, chooseParams);

        Button copyButton = makeButton("Скопировать ссылку");
        copyButton.setOnClickListener(v -> copyUrl());
        LinearLayout.LayoutParams copyParams = fullWidth();
        copyParams.topMargin = dp(12);
        root.addView(copyButton, copyParams);

        statusView = new TextView(this);
        statusView.setTextColor(Color.rgb(170, 170, 170));
        statusView.setTextSize(13f);
        statusView.setGravity(Gravity.CENTER);
        statusView.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(24);
        root.addView(statusView, statusParams);

        TextView privacy = new TextView(this);
        privacy.setText("Приложение не читает логины, cookies, токены и содержимое страницы.");
        privacy.setTextColor(Color.rgb(120, 120, 120));
        privacy.setTextSize(12f);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = matchWrap();
        privacyParams.topMargin = dp(18);
        root.addView(privacy, privacyParams);

        return root;
    }

    private void openPreferredBrowserSurface() {
        String customTabsPackage = null;
        try {
            customTabsPackage = CustomTabsClient.getPackageName(this, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not resolve a Custom Tabs provider", e);
        }

        if (customTabsPackage == null) {
            updateBrowserStatus(true);
            openBrowserChooser();
            return;
        }

        try {
            CustomTabsIntent customTab = new CustomTabsIntent.Builder()
                    .setShowTitle(false)
                    .setUrlBarHidingEnabled(false)
                    .build();
            customTab.intent.setPackage(customTabsPackage);
            customTab.intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            browserWasOpened = true;
            customTab.launchUrl(this, START_URI);
            statusView.setText("Открыто через браузер: " + customTabsPackage);
        } catch (RuntimeException e) {
            Log.w(TAG, "Custom Tab failed; showing browser chooser", e);
            updateBrowserStatus(true);
            openBrowserChooser();
        }
    }

    private void openBrowserChooser() {
        try {
            Intent browser = new Intent(Intent.ACTION_VIEW, START_URI);
            browser.addCategory(Intent.CATEGORY_BROWSABLE);
            browser.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            Intent chooser = Intent.createChooser(browser, "Открыть ChatGPT через");
            browserWasOpened = true;
            startActivity(chooser);
            statusView.setText("Выберите обычный браузер. На Huawei это основной резервный путь для входа.");
        } catch (RuntimeException e) {
            Log.e(TAG, "No browser can open ChatGPT", e);
            statusView.setText("Не найден браузер, который может открыть https://chatgpt.com/. "
                    + "Установите или включите браузер и повторите попытку.");
        }
    }

    private void copyUrl() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Буфер обмена недоступен", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("ChatGPT", START_URI.toString()));
        Toast.makeText(this, "Ссылка скопирована", Toast.LENGTH_SHORT).show();
    }

    private void updateBrowserStatus(boolean afterFailure) {
        String customTabsPackage = null;
        try {
            customTabsPackage = CustomTabsClient.getPackageName(this, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Custom Tabs provider lookup failed", e);
        }

        if (customTabsPackage != null) {
            statusView.setText("Браузерный вход доступен: " + customTabsPackage
                    + (afterFailure ? ". Custom Tab не открылся — используйте выбор браузера." : "."));
        } else {
            statusView.setText("Custom Tabs не обнаружены. Используйте «Выбрать браузер» — "
                    + "это штатный вариант без WebView.");
        }
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16f);
        button.setAllCaps(false);
        button.setMinHeight(dp(52));
        button.setPadding(dp(18), dp(10), dp(18), dp(10));
        button.setGravity(Gravity.CENTER);
        return button;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
