/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.mutex;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.dsl.CachedLanguage;
import org.truffleruby.RubyLanguage;
import org.truffleruby.SuppressFBWarnings;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.thread.GetCurrentRubyThreadNode;
import org.truffleruby.core.thread.RubyThread;
import org.truffleruby.core.thread.ThreadManager;
import org.truffleruby.core.thread.ThreadStatus;
import org.truffleruby.language.Nil;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.objects.AllocateHelperNode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;

@CoreModule(value = "ConditionVariable", isClass = true)
public abstract class ConditionVariableNodes {

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();

        @Specialization
        protected RubyConditionVariable allocate(RubyClass rubyClass,
                @CachedLanguage RubyLanguage language) {
            // condLock is only held for a short number of non-blocking instructions,
            // so there is no need to poll for safepoints while locking it.
            // It is an internal lock and so locking should be done with condLock.lock()
            // to avoid changing the Ruby Thread status and consume Java thread interrupts.
            final ReentrantLock condLock = MutexOperations.newReentrantLock();
            final Condition condition = MutexOperations.newCondition(condLock);

            final Shape shape = allocateNode.getCachedShape(rubyClass);
            final RubyConditionVariable instance = new RubyConditionVariable(rubyClass, shape, condLock, condition);
            allocateNode.trace(instance, this, language);
            return instance;
        }
    }

    @Primitive(name = "condition_variable_wait")
    public static abstract class WaitNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyConditionVariable noTimeout(RubyConditionVariable condVar, RubyMutex mutex, Nil timeout,
                @Cached GetCurrentRubyThreadNode getCurrentRubyThreadNode,
                @Cached BranchProfile errorProfile) {
            final RubyThread thread = getCurrentRubyThreadNode.execute();
            final ReentrantLock mutexLock = mutex.lock;

            MutexOperations.checkOwnedMutex(getContext(), mutexLock, this, errorProfile);
            waitInternal(condVar, mutexLock, thread, -1);
            return condVar;
        }

        @Specialization
        protected RubyConditionVariable withTimeout(RubyConditionVariable condVar, RubyMutex mutex, long timeout,
                @Cached GetCurrentRubyThreadNode getCurrentRubyThreadNode,
                @Cached BranchProfile errorProfile) {
            final RubyThread thread = getCurrentRubyThreadNode.execute();
            final ReentrantLock mutexLock = mutex.lock;

            MutexOperations.checkOwnedMutex(getContext(), mutexLock, this, errorProfile);
            waitInternal(condVar, mutexLock, thread, timeout);
            return condVar;
        }

        @TruffleBoundary
        private void waitInternal(RubyConditionVariable conditionVariable, ReentrantLock mutexLock,
                RubyThread thread, long durationInNanos) {
            final ReentrantLock condLock = conditionVariable.lock;
            final Condition condition = conditionVariable.condition;
            final long endNanoTime;
            if (durationInNanos >= 0) {
                endNanoTime = System.nanoTime() + durationInNanos;
            } else {
                endNanoTime = 0;
            }

            // Clear the wakeUp flag, following Ruby semantics:
            // it should only be considered if we are inside Mutex#sleep when Thread#{run,wakeup} is called.
            thread.wakeUp.set(false);

            // condLock must be locked before unlocking mutexLock, to avoid losing potential signals.
            // We must not change the Ruby Thread status and not consume a Java thread interrupt while locking condLock.
            // If there is an interrupt, it should be consumed by condition.await() and the Ruby Thread sleep status
            // must imply being ready to be interrupted by Thread#{run,wakeup}.
            condLock.lock();
            try {
                mutexLock.unlock();

                conditionVariable.waiters++;
                try {
                    awaitSignal(conditionVariable, thread, durationInNanos, condLock, condition, endNanoTime);
                } catch (Error | RuntimeException e) {
                    /* Consume a signal if one was waiting. We do this because the error may have occurred while we were
                     * waiting, or at some point after exiting a safepoint that throws an exception and another thread
                     * has attempted to signal us. It is valid for us to consume this signal because we are still marked
                     * as waiting for it. */
                    consumeSignal(conditionVariable);
                    throw e;
                } finally {
                    conditionVariable.waiters--;
                }
            } finally {
                condLock.unlock();
                MutexOperations.internalLockEvenWithException(getContext(), mutexLock, this);
            }
        }

        /** This duplicates {@link ThreadManager#runUntilResult} because it needs fine grained control when polling for
         * safepoints. */
        @SuppressFBWarnings(value = { "UL", "RV" })
        private void awaitSignal(RubyConditionVariable self, RubyThread thread, long durationInNanos,
                ReentrantLock condLock, Condition condition, long endNanoTime) {
            final ThreadStatus status = thread.status;
            while (true) {
                thread.status = ThreadStatus.SLEEP;
                try {
                    try {
                        /* We must not consumeSignal() here, as we should only consume a signal after being awaken by
                         * condition.signal() or condition.signalAll(). Otherwise, ConditionVariable#signal might
                         * condition.signal() a waiting thread, and then if the current thread calls
                         * ConditionVariable#wait before the waiting thread awakes, we might steal that waiting thread's
                         * signal with consumeSignal(). So, we must await() first.
                         * spec/ruby/library/conditionvariable/signal_spec.rb is a good spec for this (run with repeats
                         * = 10000). */
                        if (durationInNanos >= 0) {
                            final long currentTime = System.nanoTime();
                            if (currentTime >= endNanoTime) {
                                return;
                            }

                            condition.await(endNanoTime - currentTime, TimeUnit.NANOSECONDS);
                        } else {
                            condition.await();
                        }
                        if (consumeSignal(self)) {
                            return;
                        }
                    } finally {
                        thread.status = status;
                    }
                } catch (InterruptedException e) {
                    /* Working with ConditionVariables is tricky because of safepoints. To call await or signal on a
                     * condition variable we must hold the lock, and that lock is released when we start waiting.
                     * However if the wait is interrupted then the lock will be reacquired before control returns to us.
                     * If we are interrupted for a safepoint then we must release the lock so that all threads can enter
                     * the safepoint, and acquire it again before resuming waiting. */
                    condLock.unlock();
                    try {
                        getContext().getSafepointManager().pollFromBlockingCall(this);
                    } finally {
                        condLock.lock();
                    }

                    // Thread#{wakeup,run} might have woken us. In that a case, no signal is consumed.
                    if (thread.wakeUp.getAndSet(false)) {
                        return;
                    }

                    // Check if a signal are available now, since another thread might have used
                    // ConditionVariable#signal while we released condLock to check for safepoints.
                    if (consumeSignal(self)) {
                        return;
                    }
                }
            }
        }

        private boolean consumeSignal(RubyConditionVariable self) {
            if (self.signals > 0) {
                self.signals--;
                return true;
            }
            return false;
        }

    }

    @CoreMethod(names = "signal")
    public static abstract class SignalNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyConditionVariable signal(RubyConditionVariable self) {
            final ReentrantLock condLock = self.lock;
            final Condition condition = self.condition;

            condLock.lock();
            try {
                if (self.waiters > 0) {
                    self.signals++;
                    condition.signal();
                }
            } finally {
                condLock.unlock();
            }

            return self;
        }
    }

    @CoreMethod(names = "broadcast")
    public static abstract class BroadCastNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyConditionVariable broadcast(RubyConditionVariable self) {
            final ReentrantLock condLock = self.lock;
            final Condition condition = self.condition;

            condLock.lock();
            try {
                if (self.waiters > 0) {
                    self.signals += self.waiters;
                    condition.signalAll();
                }
            } finally {
                condLock.unlock();
            }

            return self;
        }
    }
}
