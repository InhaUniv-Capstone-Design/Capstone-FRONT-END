package com.example.yakyeon2.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MealTimingTextTest {

    @Test
    public void afterMealPhrasesDisplayAsAfterWithoutChangingOtherPhrases() {
        assertEquals("식후", MealTimingText.displayPhrase("식후 30분"));
        assertEquals("식후", MealTimingText.displayPhrase("식후30분"));
        assertEquals("식후", MealTimingText.displayPhrase("식후"));
        assertEquals("식후", MealTimingText.displayPhrase("식사 직후"));
        assertEquals("식후", MealTimingText.displayPhrase("식사 후"));
        assertEquals("식후", MealTimingText.displayPhrase("AFTER_MEAL"));
        assertEquals("식전 30분", MealTimingText.displayPhrase("식전 30분"));
        assertEquals("식전", MealTimingText.displayPhrase("식전"));
        assertEquals("BEFORE_MEAL", MealTimingText.displayPhrase("BEFORE_MEAL"));
        assertFalse(MealTimingText.displaysAsAfterMeal("식전 30분"));
        assertTrue(MealTimingText.displaysAsAfterMeal("식후 30분"));
    }
}
