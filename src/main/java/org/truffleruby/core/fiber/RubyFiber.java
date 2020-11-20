/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.fiber;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.basicobject.RubyBasicObject;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.thread.RubyThread;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.objects.ObjectGraphNode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;

public class RubyFiber extends RubyDynamicObject implements ObjectGraphNode {

    public final RubyBasicObject fiberLocals;
    public final RubyArray catchTags;
    public final CountDownLatch initializedLatch = new CountDownLatch(1);
    public CountDownLatch finishedLatch = new CountDownLatch(1);
    final BlockingQueue<FiberManager.FiberMessage> messageQueue = newMessageQueue();
    final RubyThread rubyThread;
    volatile RubyFiber lastResumedByFiber = null;
    public volatile boolean alive = true;
    public Thread thread = null;
    volatile boolean transferred = false;
    public volatile Throwable uncaughtException = null;

    public RubyFiber(
            RubyClass rubyClass,
            Shape shape,
            RubyBasicObject fiberLocals,
            RubyArray catchTags,
            RubyThread rubyThread) {
        super(rubyClass, shape);
        this.fiberLocals = fiberLocals;
        this.catchTags = catchTags;
        this.rubyThread = rubyThread;
    }

    @TruffleBoundary
    private static LinkedBlockingQueue<FiberManager.FiberMessage> newMessageQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Override
    public void getAdjacentObjects(Set<Object> reachable) {
        reachable.add(fiberLocals);
        reachable.add(catchTags);
        reachable.add(rubyThread);
    }

}
