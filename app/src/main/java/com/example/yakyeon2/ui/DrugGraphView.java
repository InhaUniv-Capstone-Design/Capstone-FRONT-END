package com.example.yakyeon2.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline Cytoscape.js graph. Scripts and pages are served from app assets only.
 */
public class DrugGraphView extends WebView {

    private static final String HOST = "yakyeon.graph";
    private static final String PAGE = "https://yakyeon.graph/graph.html";

    public interface Listener {
        void onNode(String medicineId);

        void onEdge(String edgeId);
    }

    private static final String TAG = "YakyeonGraph";

    private final List<String> trace = new ArrayList<>();
    private Listener listener;
    private String pendingJson;
    private boolean pageReady;

    public DrugGraphView(Context context) {
        this(context, null);
    }

    public DrugGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
        setClipToOutline(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDomStorageEnabled(false);
        settings.setGeolocationEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        addJavascriptInterface(new Bridge(), "AndroidGraph");
        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                note("console " + consoleMessage.messageLevel() + " " + consoleMessage.message());
                return true;
            }
        });
        setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return localAsset(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                note("page-finished " + url + " size=" + getWidth() + "x" + getHeight());
                pageReady = true;
                deliver();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                note("error " + request.getUrl() + " " + error.getErrorCode() + " " + error.getDescription());
            }
        });
        setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                ViewParent parent = view.getParent();
                while (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    parent = parent.getParent();
                }
            }
            return false;
        });
        loadUrl(PAGE);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void show(String json) {
        pendingJson = json == null ? "{\"nodes\":[],\"edges\":[]}" : json;
        note("show bytes=" + pendingJson.length() + " pageReady=" + pageReady + " size=" + getWidth() + "x" + getHeight());
        deliver();
    }

    public List<String> trace() {
        synchronized (trace) {
            return new ArrayList<>(trace);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && pageReady && (w != oldw || h != oldh)) {
            note("resize " + oldw + "x" + oldh + " -> " + w + "x" + h);
            evaluateJavascript(
                    "if (window.graph) {"
                            + " window.graph.resize();"
                            + " window.graph.fit(undefined, 48);"
                            + " if (window.graph.zoom() > 1.15) {"
                            + " window.graph.zoom(1.15); window.graph.center(); }"
                            + "}",
                    null
            );
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        listener = null;
        super.onDetachedFromWindow();
    }

    private void deliver() {
        if (!pageReady || pendingJson == null) {
            return;
        }
        note("deliver size=" + getWidth() + "x" + getHeight());
        evaluateJavascript("renderGraph(" + pendingJson + ")", value -> note("render-result " + value));
    }

    private void note(String message) {
        Log.i(TAG, message);
        synchronized (trace) {
            trace.add(message);
        }
    }

    private WebResourceResponse localAsset(Uri uri) {
        if (uri == null || !"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost())) {
            note("blocked " + uri);
            return empty();
        }
        String name = uri.getLastPathSegment();
        if (!"graph.html".equals(name) && !"cytoscape.min.js".equals(name)) {
            note("blocked " + uri);
            return empty();
        }
        try {
            InputStream stream = getContext().getAssets().open(name);
            note("asset " + name);
            String type = name.endsWith(".js") ? "application/javascript" : "text/html";
            return new WebResourceResponse(type, "UTF-8", stream);
        } catch (IOException exception) {
            note("asset-missing " + name + " " + exception.getMessage());
            return empty();
        }
    }

    private static WebResourceResponse empty() {
        return new WebResourceResponse(
                "text/plain",
                "UTF-8",
                404,
                "Not Found",
                Collections.emptyMap(),
                new ByteArrayInputStream(new byte[0])
        );
    }

    public class Bridge {
        @JavascriptInterface
        public void onNode(String medicineId) {
            post(() -> {
                if (listener != null) {
                    listener.onNode(medicineId);
                }
            });
        }

        @JavascriptInterface
        public void onEdge(String edgeId) {
            post(() -> {
                if (listener != null) {
                    listener.onEdge(edgeId);
                }
            });
        }
    }
}
