package com.micontrollaboral.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoneyUtilsTest {
    @Test
    public void convertsBolivianosToCents() {
        assertEquals(10000, MoneyUtils.bolivianosToCents("100"));
        assertEquals(5050, MoneyUtils.bolivianosToCents("50.50"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMoreThanTwoDecimals() {
        MoneyUtils.bolivianosToCents("10.999");
    }
}