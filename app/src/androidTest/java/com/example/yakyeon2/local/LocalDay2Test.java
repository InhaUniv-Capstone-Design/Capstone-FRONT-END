package com.example.yakyeon2.local;

import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.yakyeon2.MainActivity;
import com.example.yakyeon2.R;
import com.example.yakyeon2.data.AuthTokenStore;
import com.example.yakyeon2.ui.DrugGraphView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Screen checks for the logged-out local trials. A signed-in device is skipped
 * so the test does not clear or mix that session.
 */
@RunWith(AndroidJUnit4.class)
public class LocalDay2Test {

    @Before
    public void refuseSignedInDevice() {
        assumeFalse(
                "signed-in session is present; this device is left untouched",
                AuthTokenStore.hasSession(
                        InstrumentationRegistry.getInstrumentation().getTargetContext()
                )
        );
    }

    @Test
    public void it01EmptyLoginDoesNotOpenHome() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.button_login)).perform(click());
            onView(withText(R.string.auth_need_id)).check(matches(isDisplayed()));
            assertTrue(stillOnLogin(scenario));

            onView(allOf(
                    isDescendantOfA(withId(R.id.field_login_id)),
                    instanceOf(EditText.class)
            )).perform(typeText("day2id"), closeSoftKeyboard());
            onView(withId(R.id.button_login)).perform(click());
            onView(withText(R.string.auth_need_password)).check(matches(isDisplayed()));
            assertTrue(stillOnLogin(scenario));
        }
    }

    @Test
    public void it05ScenarioHomeShowsSixAndKeepsTheDevNote() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.auth_dev_home)).perform(click());
            onView(withText(R.string.home_dev_screen_note)).check(matches(isDisplayed()));
            onView(withId(R.id.text_home_greeting)).perform(longClick());
            onView(withText(R.string.home_dev_scenario_known)).perform(click());
            onView(withId(R.id.header_back)).perform(click());
            onView(withText("현재 6개 복용 중  ›")).check(matches(isDisplayed()));
            onView(withText(R.string.home_dev_screen_note)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void it08ScenarioGraphShowsNodesWithoutCallingThemRegistered() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.auth_dev_home)).perform(click());
            onView(withId(R.id.text_home_greeting)).perform(longClick());
            onView(withText(R.string.home_dev_scenario_known)).perform(click());
            onView(withId(R.id.header_back)).perform(click());
            onView(withId(R.id.tab_graph)).perform(click());
            onView(withText(R.string.graph_scenario_note)).check(matches(isDisplayed()));
            onView(withText("개발용 화면의 선 0개입니다. 등재 조회 결과가 아닙니다."))
                    .check(matches(isDisplayed()));
            String script = graphScript(scenario);
            assertTrue(script, script.contains("\"nodes\":6"));
            assertTrue(script, script.contains("\"edges\":0"));
            assertTrue(script, script.contains("약1"));
            assertFalse(script, script.contains("안전합니다"));
        }
    }

    private static boolean stillOnLogin(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Boolean> login = new AtomicReference<>(false);
        scenario.onActivity(activity -> login.set(
                activity.findViewById(R.id.button_login) != null
        ));
        return login.get();
    }

    private static String graphScript(ActivityScenario<MainActivity> scenario) {
        String last = "";
        for (int attempt = 0; attempt < 40; attempt++) {
            android.os.SystemClock.sleep(250);
            last = snapshot(scenario);
            String script = unwrap(scriptOf(last));
            if (script.contains("\"nodes\":6") && script.contains("\"edges\":0")
                    && script.contains("\"boxW\":") && !script.contains("\"boxW\":0")
                    && !script.contains("\"boxW\":-1")) {
                return script;
            }
        }
        fail(last);
        return last;
    }

    private static String snapshot(ActivityScenario<MainActivity> scenario) {
        AtomicReference<DrugGraphView> webRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            FrameLayout canvas = activity.findViewById(R.id.graph_canvas);
            if (canvas != null && canvas.getChildCount() > 0
                    && canvas.getChildAt(0) instanceof DrugGraphView) {
                webRef.set((DrugGraphView) canvas.getChildAt(0));
            }
        });
        DrugGraphView web = webRef.get();
        if (web == null) {
            return "no-webview";
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> script = new AtomicReference<>("pending");
        web.post(() -> web.evaluateJavascript(
                "(function(){var nodeCount=-1;var edgeCount=-1;var names=[];var boxW=-1;"
                        + "if(window.graph&&window.graph.nodes){"
                        + "nodeCount=window.graph.nodes().length;"
                        + "edgeCount=window.graph.edges().length;"
                        + "window.graph.nodes().forEach(function(n){names.push(n.data('name'));});"
                        + "if(nodeCount>0){boxW=Math.round(window.graph.nodes()[0].renderedBoundingBox().w);}"
                        + "}"
                        + "return JSON.stringify({nodes:nodeCount,edges:edgeCount,names:names.join(','),boxW:boxW});"
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
        return "script=" + script.get();
    }

    private static String scriptOf(String snapshot) {
        int marker = snapshot.indexOf("script=");
        return marker < 0 ? snapshot : snapshot.substring(marker + "script=".length());
    }

    private static String unwrap(String jsResult) {
        if (jsResult == null || jsResult.length() < 2 || jsResult.charAt(0) != '"') {
            return String.valueOf(jsResult);
        }
        return jsResult.substring(1, jsResult.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
