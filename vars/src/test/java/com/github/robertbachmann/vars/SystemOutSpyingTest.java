package com.github.robertbachmann.vars;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public abstract class SystemOutSpyingTest {
    protected final boolean assertionsEnabled;
    private static final PrintStream OUT = System.out;
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private PrintStream printStream;

    public SystemOutSpyingTest(boolean assertionsEnabled) {
        this.assertionsEnabled = assertionsEnabled;
    }

    @BeforeMethod
    public void setUp() {
        if (!assertionsEnabled) {
            System.setOut(System.err);
            printStream = null;
            return;
        }

        byteArrayOutputStream.reset();
        try {
            printStream = new PrintStream(byteArrayOutputStream, true, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        System.setOut(printStream);
    }

    public final void assertLines(String... expectedLines) {
        if (!assertionsEnabled) {
            return;
        }

        String actual;
        try {
            actual = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        List<String> actualLines = Arrays.asList(actual.split(System.lineSeparator()));
        Assert.assertEquals(actualLines, Arrays.asList(expectedLines));

        byteArrayOutputStream.reset();
        for (String line : actualLines) {
            OUT.println("*" + line);
        }
    }

    @AfterMethod
    public void close() {
        System.setOut(OUT);
        if (printStream != null) {
            printStream.close();
        }
    }
}
