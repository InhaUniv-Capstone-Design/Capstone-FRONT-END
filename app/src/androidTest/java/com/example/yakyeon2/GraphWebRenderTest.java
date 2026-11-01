package com.example.yakyeon2;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.yakyeon2.data.DoseAmount;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseTiming;
import com.example.yakyeon2.data.DrugRelations;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.data.MedicineDraft;
import com.example.yakyeon2.data.MedicationRules;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.YakyeonRepositoryProvider;
import com.example.yakyeon2.main.MainScreens;
import com.example.yakyeon2.ui.DrugGraphView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class GraphWebRenderTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    @Test
    public void committedMedicinesReachCytoscapeNodesAndEdges() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<String> emptyReport = new AtomicReference<>("");
            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                MedicationStore store = YakyeonRepositoryProvider.get().medications();
                store.clearGraphChecks();
                store.setDevGraphRelationScenario(false);
                activity.openGraph();
                TextView empty = activity.findViewById(R.id.text_graph_empty);
                int taking = DrugRelations.takingNodes(store).size();
                emptyReport.set("taking=" + taking + " emptyVisible="
                        + (empty != null && empty.getVisibility() == TextView.VISIBLE)
                        + " emptyText=" + (empty == null ? "" : empty.getText()));
            });
            saveShot("graph-empty.png");
            assertTrue(emptyReport.get(), emptyReport.get().contains("taking=0")
                    && emptyReport.get().contains("emptyVisible=true"));

            scenario.onActivity(activity -> {
                MedicationStore store = YakyeonRepositoryProvider.get().medications();
                commit(store, "약하나");
                activity.openGraph();
            });
            String one = waitForGraph(scenario, 1, 0);
            saveShot("graph-one.png");
            assertTrue(one, unwrap(scriptOf(one)).contains("약하나"));

            scenario.onActivity(activity -> {
                MedicationStore store = YakyeonRepositoryProvider.get().medications();
                commit(store, "약둘");
                commit(store, "약셋");
                store.setDevGraphRelationScenario(false);
                activity.openGraph();
            });
            String three = waitForGraph(scenario, 3, 0);
            saveShot("graph-three.png");
            assertTrue(three, unwrap(scriptOf(three)).contains("\"edges\":0"));

            scenario.onActivity(activity -> {
                YakyeonRepositoryProvider.get().medications().setDevGraphRelationScenario(true);
                activity.openGraph();
            });
            String linked = waitForGraph(scenario, 3, 1);
            saveShot("graph-edge.png");
            assertTrue(linked, unwrap(scriptOf(linked)).contains("\"edges\":1"));

            scenario.onActivity(activity -> {
                YakyeonRepositoryProvider.get().medications().setDevGraphRelationScenario(false);
                activity.openGraph();
            });
            String cleared = waitForGraph(scenario, 3, 0);
            saveShot("graph-cleared.png");
            String clearedScript = unwrap(scriptOf(cleared));
            assertTrue(cleared, clearedScript.contains("\"edges\":0"));
            assertEquals(3, namesFrom(clearedScript).split(",").length);
        }
    }

    @Test
    public void homeGraphChecksReachTheGraphTab() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                activity.findViewById(R.id.button_dev_graph_clear).performClick();
            });
            int base = takingCount(scenario);
            String clearedHome = graphReport(scenario);
            if (base == 0) {
                assertTrue(clearedHome, clearedHome.contains("emptyVisible=true")
                        && clearedHome.contains("현재 복용 중인 약이 없습니다."));
                SystemClock.sleep(1200);
            }

            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                activity.findViewById(R.id.button_dev_envelopes).performClick();
                activity.openGraph();
            });
            assertEquals(base, takingCount(scenario));
            if (base == 0) {
                String fixtures = graphReport(scenario);
                assertTrue(fixtures, fixtures.contains("emptyVisible=true"));
                assertTrue(fixtures, fixtures.contains("sampleVisible=true"));
                assertTrue(fixtures, fixtures.contains("개발용 예시 약은 그래프에 넣지 않습니다."));
                SystemClock.sleep(1200);
            }

            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                activity.findViewById(R.id.button_dev_graph_one).performClick();
            });
            String one = waitForGraph(scenario, base + 1, 0);
            assertTrue(one, unwrap(scriptOf(one)).contains("그래프 검증 1"));
            assertFalse(one, unwrap(scriptOf(one)).contains("개발용 예시"));
            SystemClock.sleep(1200);

            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                activity.findViewById(R.id.button_dev_graph_three).performClick();
            });
            String three = waitForGraph(scenario, base + 3, 0);
            String threeScript = unwrap(scriptOf(three));
            assertTrue(three, threeScript.contains("그래프 검증 1"));
            assertTrue(three, threeScript.contains("그래프 검증 2"));
            assertTrue(three, threeScript.contains("그래프 검증 3"));
            assertTrue(three, threeScript.contains("\"edges\":0"));
            SystemClock.sleep(1500);

            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                activity.findViewById(R.id.button_dev_graph).performClick();
            });
            String linked = waitForGraph(scenario, base + 3, 1);
            String linkedScript = unwrap(scriptOf(linked));
            assertTrue(linked, linkedScript.contains("\"edges\":1"));
            assertTrue(linked, linkedScript.contains("그래프 검증 3"));
            SystemClock.sleep(1500);

            scenario.onActivity(activity -> {
                activity.onDevHome();
                activity.openDevTools();
                activity.findViewById(R.id.button_dev_graph).performClick();
            });
            String cleared = waitForGraph(scenario, base + 3, 0);
            String clearedScript = unwrap(scriptOf(cleared));
            assertTrue(cleared, clearedScript.contains("\"edges\":0"));
            assertEquals(base + 3, namesFrom(clearedScript).split(",").length);

            scenario.onActivity(activity -> activity.openGraph());
            String again = waitForGraph(scenario, base + 3, 0);
            assertTrue(again, unwrap(scriptOf(again)).contains("그래프 검증 1"));
            assertTrue(again, unwrap(scriptOf(again)).contains("\"edges\":0"));
        }
    }

    private static int takingCount(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Integer> count = new AtomicReference<>(-1);
        scenario.onActivity(activity -> count.set(
                DrugRelations.takingNodes(YakyeonRepositoryProvider.get().medications()).size()
        ));
        return count.get();
    }

    private static String graphReport(ActivityScenario<MainActivity> scenario) {
        AtomicReference<String> report = new AtomicReference<>("");
        scenario.onActivity(activity -> {
            MedicationStore store = YakyeonRepositoryProvider.get().medications();
            TextView empty = activity.findViewById(R.id.text_graph_empty);
            TextView sample = activity.findViewById(R.id.text_graph_sample_note);
            report.set("taking=" + DrugRelations.takingNodes(store).size()
                    + " emptyVisible=" + (empty != null && empty.getVisibility() == TextView.VISIBLE)
                    + " emptyText=" + (empty == null ? "" : empty.getText())
                    + " sampleVisible=" + (sample != null && sample.getVisibility() == TextView.VISIBLE)
                    + " sampleText=" + (sample == null ? "" : sample.getText()));
        });
        return report.get();
    }

    private static void saveShot(String name) {
        SystemClock.sleep(400);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String waitForGraph(ActivityScenario<MainActivity> scenario, int nodes, int edges) {
        String last = "";
        for (int attempt = 0; attempt < 40; attempt++) {
            SystemClock.sleep(250);
            last = snapshot(scenario);
            String script = unwrap(scriptOf(last));
            if (script.contains("\"nodes\":" + nodes)
                    && script.contains("\"edges\":" + edges)
                    && script.contains("\"boxW\":")
                    && !script.contains("\"boxW\":0")
                    && !script.contains("\"boxW\":-1")) {
                return last;
            }
        }
        fail(last);
        return last;
    }

    private static String snapshot(ActivityScenario<MainActivity> scenario) {
        AtomicReference<DrugGraphView> webRef = new AtomicReference<>();
        AtomicReference<String> javaSide = new AtomicReference<>("");
        scenario.onActivity(activity -> {
            FrameLayout canvas = activity.findViewById(R.id.graph_canvas);
            if (canvas == null || canvas.getChildCount() == 0 || !(canvas.getChildAt(0) instanceof DrugGraphView)) {
                TextView empty = activity.findViewById(R.id.text_graph_empty);
                javaSide.set("no-webview empty=" + (empty == null ? "missing" : empty.getText()));
                return;
            }
            DrugGraphView web = (DrugGraphView) canvas.getChildAt(0);
            webRef.set(web);
            StringBuilder names = new StringBuilder();
            for (DrugRelations.Node node : DrugRelations.takingNodes(
                    YakyeonRepositoryProvider.get().medications()
            )) {
                if (names.length() > 0) {
                    names.append(',');
                }
                names.append(node.getName());
            }
            javaSide.set("javaNames=" + names + " view=" + web.getWidth() + "x" + web.getHeight());
        });
        DrugGraphView web = webRef.get();
        if (web == null) {
            return javaSide.get();
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> script = new AtomicReference<>("pending");
        web.post(() -> web.evaluateJavascript(
                "(function(){"
                        + "var el=document.getElementById('graph');"
                        + "var names=[];"
                        + "var nodeCount=-1;"
                        + "var edgeCount=-1;"
                        + "if(window.graph&&window.graph.nodes){"
                        + "nodeCount=window.graph.nodes().length;"
                        + "edgeCount=window.graph.edges().length;"
                        + "window.graph.nodes().forEach(function(n){names.push(n.data('name'));});"
                        + "}"
                        + "var box={w:-1,h:-1,x:-1,y:-1};"
                        + "if(nodeCount>0){"
                        + "var b=window.graph.nodes()[0].renderedBoundingBox();"
                        + "box={w:Math.round(b.w),h:Math.round(b.h),x:Math.round(b.x1),y:Math.round(b.y1)};"
                        + "}"
                        + "return JSON.stringify({"
                        + "href:String(location.href),"
                        + "cy:typeof cytoscape,"
                        + "render:typeof renderGraph,"
                        + "width:el?el.clientWidth:-1,"
                        + "height:el?el.clientHeight:-1,"
                        + "nodes:nodeCount,"
                        + "edges:edgeCount,"
                        + "names:names.join(','),"
                        + "boxW:box.w,boxH:box.h,boxX:box.x,boxY:box.y"
                        + "});"
                        + "})()",
                value -> {
                    script.set(value);
                    latch.countDown();
                }
        ));
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return javaSide.get() + " script=" + script.get() + " trace=" + web.trace();
    }

    private static String scriptOf(String snapshot) {
        int marker = snapshot.indexOf(" script=");
        if (marker < 0) {
            return snapshot;
        }
        int start = marker + " script=".length();
        int end = snapshot.indexOf(" trace=", start);
        return end < 0 ? snapshot.substring(start) : snapshot.substring(start, end);
    }

    private static String unwrap(String jsResult) {
        if (jsResult == null || jsResult.length() < 2 || jsResult.charAt(0) != '"') {
            return String.valueOf(jsResult);
        }
        return jsResult.substring(1, jsResult.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String namesFrom(String snapshot) {
        int marker = snapshot.indexOf("\"names\":\"");
        if (marker < 0) {
            return "";
        }
        int start = marker + "\"names\":\"".length();
        int end = snapshot.indexOf('"', start);
        return end < 0 ? "" : snapshot.substring(start, end);
    }

    private static void commit(MedicationStore store, String name) {
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName("hospital");
        MedicineDraft medicine = store.addMedicine(DAY);
        medicine.setName(name);
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(DAY);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        draft.alarmMinutes().put(DoseRound.LUNCH, 13 * 60);
        draft.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        assertEquals(
                MedicationRules.SaveBlock.OK,
                store.commit(ZONE, DAY.atStartOfDay(ZONE).toInstant().toEpochMilli())
        );
    }
}
