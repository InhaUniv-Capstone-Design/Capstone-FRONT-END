package com.example.yakyeon.ui.graph;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.DrugRelationGraphRepository;
import com.example.yakyeon.data.mock.MockDurInteractionRepository;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.data.model.DrugGraphEdge;
import com.example.yakyeon.data.model.DrugRelationGraph;
import com.example.yakyeon.data.model.DurInteraction;
import com.example.yakyeon.data.model.Medication;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DrugRelationGraphActivity extends AppCompatActivity {

    private final DrugRelationGraphRepository graphRepository = new DrugRelationGraphRepository(
            MockMedicationRepository.getInstance(),
            MockDurInteractionRepository.getInstance()
    );

    private DrugRelationGraph graph = new DrugRelationGraph(Collections.emptyList(), Collections.emptyList());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drug_relation_graph);
        WindowSpacing.applySystemBars(findViewById(R.id.graph_root));
        BottomNav.bind(this, true);

        graph = graphRepository.load();
        bindRelations(graph);
        bindCanvas();
    }

    public DurInteraction findInteraction(String interactionId) {
        return graphRepository.findInteraction(interactionId);
    }

    private void showInteraction(String interactionId) {
        if (findInteraction(interactionId) == null) {
            return;
        }
        InteractionDetailDialog.open(getSupportFragmentManager(), interactionId);
    }

    private void bindRelations(DrugRelationGraph snapshot) {
        ((TextView) findViewById(R.id.graph_relation_title)).setText(
                getString(R.string.graph_relations_title, snapshot.getEdges().size())
        );
        Map<String, Medication> byId = new HashMap<>();
        for (Medication medication : snapshot.getMedications()) {
            byId.put(medication.getId(), medication);
        }
        LinearLayout list = findViewById(R.id.graph_relation_list);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DrugGraphEdge edge : snapshot.getEdges()) {
            Medication source = byId.get(edge.getSourceMedicationId());
            Medication target = byId.get(edge.getTargetMedicationId());
            if (source == null || target == null) {
                continue;
            }
            View item = inflater.inflate(R.layout.item_graph_relation, list, false);
            ((TextView) item.findViewById(R.id.graph_relation_source)).setText(source.getName());
            ((TextView) item.findViewById(R.id.graph_relation_target)).setText(target.getName());
            String interactionId = edge.getInteractionId();
            item.setOnClickListener(view -> showInteraction(interactionId));
            list.addView(item);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private void bindCanvas() {
        WebView canvas = findViewById(R.id.graph_canvas);
        WebSettings settings = canvas.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowContentAccess(false);
        canvas.setBackgroundColor(Color.WHITE);
        canvas.setVerticalScrollBarEnabled(false);
        canvas.setHorizontalScrollBarEnabled(false);
        canvas.setOverScrollMode(View.OVER_SCROLL_NEVER);
        canvas.setLongClickable(false);
        canvas.setHapticFeedbackEnabled(false);
        canvas.setOnLongClickListener(view -> true);
        canvas.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });
        canvas.addJavascriptInterface(new GraphBridge(), "YakyeonGraph");
        canvas.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("renderGraph(" + GraphCytoscapePayload.toJson(graph) + ")", null);
            }
        });
        canvas.loadUrl("file:///android_asset/graph/graph.html");
    }

    @Override
    protected void onDestroy() {
        WebView canvas = findViewById(R.id.graph_canvas);
        if (canvas != null) {
            canvas.removeJavascriptInterface("YakyeonGraph");
            if (canvas.getParent() instanceof ViewGroup) {
                ((ViewGroup) canvas.getParent()).removeView(canvas);
            }
            canvas.destroy();
        }
        super.onDestroy();
    }

    private final class GraphBridge {
        @JavascriptInterface
        public void onEdgeSelected(String interactionId) {
            runOnUiThread(() -> showInteraction(interactionId));
        }
    }
}
