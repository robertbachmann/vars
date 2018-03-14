package com.github.robertbachmann.vars;

import java.lang.reflect.Field;

public final class TestUtil {
    public static void complete(Var<?> var) {
        if (var == null) {
            throw new NullPointerException("var");
        }

        InternalPublisher internalPublisher;

        try {
            Field impl = Var.class.getDeclaredField("impl");
            impl.setAccessible(true);
            internalPublisher = (InternalPublisher) impl.get(var);
        } catch (Exception e) {
            throw new AssertionError("TestHelper reflection failed", e);
        }

        internalPublisher.complete();
    }

    private TestUtil() {
    }
}
