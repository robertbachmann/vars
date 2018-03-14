package com.github.robertbachmann.vars.tck;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.TestUtil;
import com.github.robertbachmann.vars.Var;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test
public class SimpleVarPublisherVerificationTest extends PublisherVerification<Long> {
    private ExecutorService e;

    @BeforeClass
    void before() {
        e = Executors.newFixedThreadPool(4);
    }

    @AfterClass
    void after() {
        if (e != null) e.shutdown();
    }

    public SimpleVarPublisherVerificationTest() {
        super(new TestEnvironment());
    }

    public Publisher<Long> createPublisher(final long elements) {
        final SimpleVar<Long> var = Var.valueOf(0L);

        e.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                // ignore
            }
            TestUtil.complete(var);
        });

        return var;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }
}
