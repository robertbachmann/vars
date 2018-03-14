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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("Duplicates")
@Test
public class VarPublisherVerificationTest extends PublisherVerification<Long> {
    private ExecutorService e;

    @BeforeClass
    void before() {
        e = Executors.newFixedThreadPool(4);
    }

    @AfterClass
    void after() {
        if (e != null) e.shutdown();
    }

    public VarPublisherVerificationTest() {
        super(new TestEnvironment());
    }

    public Publisher<Long> createPublisher(long n) {
        final AtomicLong cnt = new AtomicLong(n);
        final SimpleVar<Long> var = Var.valueOf(n);
        final Var<Long> derivedVar = Var.map(var, (v) -> v);

        // generate values
        final Future<?> future = e.submit(() -> {
            long dummy = 0;
            while (!Thread.interrupted()) {
                dummy = (dummy == 123) ? 456 : 123;

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e1) {
                    break;
                }

                if (cnt.get() <= 0) {
                    break;
                }
                var.setValue(dummy);
            }
            TestUtil.complete(var);
        });

        //noinspection Convert2Diamond
        return new HookProcessor<Long>(derivedVar) {
            @Override
            protected void afterNext(Long aLong) {
                cnt.decrementAndGet();
            }

            @Override
            protected void afterCancel() {
                future.cancel(true);
            }

            @Override
            protected void afterComplete() {
                future.cancel(true);
            }

            @Override
            protected void afterError(Throwable t) {
                future.cancel(true);
            }
        };
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        Var<Integer> integerVar = Var.valueOf(1);
        return Var.map(integerVar, (i) -> i / 0L);
    }
}
