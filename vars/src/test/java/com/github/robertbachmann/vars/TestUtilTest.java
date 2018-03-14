package com.github.robertbachmann.vars;

import org.testng.annotations.Test;

@Test
public class TestUtilTest {
    @Test
    public void ensureTestUtilWorks() {
        Var<Integer> var = Var.valueOf(1);
        TestUtil.complete(var);
    }
}
