package app.chatgpt.web;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiStallTrackerTest {
    @Test public void countsOnlyLargeSchedulingDelays() {
        UiStallTracker tracker = new UiStallTracker();
        tracker.start(1000);
        tracker.sample(1250);
        tracker.sample(1850); // expected 1500: delay 350
        tracker.sample(2100);
        assertEquals(3, tracker.samples());
        assertEquals(1, tracker.stalls());
        assertEquals(350, tracker.maxDelayMs());
    }
    @Test public void stoppedTrackerDoesNotCollect() {
        UiStallTracker tracker = new UiStallTracker();
        tracker.start(0);
        tracker.stop();
        tracker.sample(1000);
        assertEquals(0, tracker.samples());
    }
}
