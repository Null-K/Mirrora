package com.puddingkc.Mirrora.util;

import java.util.concurrent.atomic.AtomicInteger;


public final class FakeEntityIdAllocator {

    private static final AtomicInteger COUNTER = new AtomicInteger(Integer.MIN_VALUE);

    private FakeEntityIdAllocator() { }

    public static int next() {
        return COUNTER.getAndIncrement();
    }
}
