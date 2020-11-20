/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 *
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.truffleruby.core.thread;

import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.dsl.CachedLanguage;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.NonStandard;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.builtins.UnaryCoreMethodNode;
import org.truffleruby.builtins.YieldingCoreMethodNode;
import org.truffleruby.collections.Memo;
import org.truffleruby.core.InterruptMode;
import org.truffleruby.core.VMPrimitiveNodes.VMRaiseExceptionNode;
import org.truffleruby.core.array.ArrayGuards;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.array.library.ArrayStoreLibrary;
import org.truffleruby.core.basicobject.RubyBasicObject;
import org.truffleruby.core.exception.GetBacktraceException;
import org.truffleruby.core.exception.RubyException;
import org.truffleruby.core.fiber.RubyFiber;
import org.truffleruby.core.hash.RubyHash;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.proc.ProcOperations;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.core.support.RubyRandomizer;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.core.thread.ThreadManager.UnblockingAction;
import org.truffleruby.core.thread.ThreadManager.UnblockingActionHolder;
import org.truffleruby.interop.InteropNodes;
import org.truffleruby.interop.TranslateInteropExceptionNode;
import org.truffleruby.language.Nil;
import org.truffleruby.language.NotProvided;
import org.truffleruby.language.SafepointAction;
import org.truffleruby.language.SafepointManager;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.backtrace.Backtrace;
import org.truffleruby.language.control.KillException;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.objects.AllocateHelperNode;
import org.truffleruby.language.objects.shared.SharedObjects;
import org.truffleruby.language.yield.YieldNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.source.SourceSection;

@CoreModule(value = "Thread", isClass = true)
public abstract class ThreadNodes {

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @Specialization
        protected Object allocate(RubyClass rubyClass) {
            throw new RaiseException(getContext(), coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @CoreMethod(names = "alive?")
    public abstract static class AliveNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected boolean alive(RubyThread thread) {
            final ThreadStatus status = thread.status;
            return status != ThreadStatus.DEAD;
        }

    }

    @Primitive(name = "thread_backtrace", lowerFixnum = { 1, 2 })
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object backtrace(RubyThread rubyThread, int omit, NotProvided length) {
            return backtrace(rubyThread, omit, Integer.MAX_VALUE);
        }

        @TruffleBoundary
        @Specialization
        protected Object backtrace(RubyThread rubyThread, int omit, int length) {
            final Memo<Backtrace> backtraceMemo = new Memo<>(null);

            getContext().getSafepointManager().pauseRubyThreadAndExecute(
                    "Thread#backtrace",
                    rubyThread,
                    this,
                    (thread, currentNode) -> {
                        final Backtrace backtrace = getContext().getCallStack().getBacktrace(currentNode, omit);
                        backtrace.getStackTrace(); // must be done on the thread
                        backtraceMemo.set(backtrace);
                    });

            final Backtrace backtrace = backtraceMemo.get();

            // If the thread is dead or aborting the SafepointAction will not run.
            // Must return nil if omitting more entries than available.
            if (backtrace == null || omit > backtrace.getTotalUnderlyingElements()) {
                return nil;
            }

            if (length < 0) {
                length = backtrace.getStackTrace().length + 1 + length;
            }

            return getContext().getUserBacktraceFormatter().formatBacktraceAsRubyStringArray(
                    null,
                    backtrace,
                    length);
        }
    }

    @Primitive(name = "thread_backtrace_locations", lowerFixnum = { 1, 2 })
    public abstract static class BacktraceLocationsNode extends PrimitiveArrayArgumentsNode {

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();

        @Specialization
        protected Object backtraceLocations(RubyThread rubyThread, int first, NotProvided second) {
            return backtraceLocationsInternal(rubyThread, first, GetBacktraceException.UNLIMITED);
        }

        @Specialization
        protected Object backtraceLocations(RubyThread rubyThread, int first, int second) {
            return backtraceLocationsInternal(rubyThread, first, second);
        }

        @TruffleBoundary
        private Object backtraceLocationsInternal(RubyThread rubyThread, int omit, int length) {
            final Memo<Object> backtraceLocationsMemo = new Memo<>(null);

            final SafepointAction safepointAction = (thread1, currentNode) -> {
                final Backtrace backtrace = getContext().getCallStack().getBacktrace(this, omit);
                backtraceLocationsMemo.set(backtrace.getBacktraceLocations(getContext(), allocateNode, length, this));
            };

            getContext()
                    .getSafepointManager()
                    .pauseRubyThreadAndExecute("Thread#backtrace_locations", rubyThread, this, safepointAction);

            // If the thread is dead or aborting the SafepointAction will not run.
            return backtraceLocationsMemo.get() == null
                    ? nil
                    : backtraceLocationsMemo.get();
        }
    }

    @CoreMethod(names = "current", onSingleton = true)
    public abstract static class CurrentNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyThread current(
                @Cached GetCurrentRubyThreadNode getCurrentRubyThreadNode) {
            return getCurrentRubyThreadNode.execute();
        }

    }

    @CoreMethod(names = "group")
    public abstract static class GroupNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object group(RubyThread thread) {
            return thread.threadGroup;
        }

    }

    @CoreMethod(names = { "kill", "exit", "terminate" })
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyThread kill(RubyThread rubyThread) {
            final ThreadManager threadManager = getContext().getThreadManager();
            final RubyThread rootThread = threadManager.getRootThread();

            getContext().getSafepointManager().pauseRubyThreadAndExecute(
                    "Thread#kill",
                    rubyThread,
                    this,
                    (thread, currentNode) -> {
                        if (thread == rootThread) {
                            throw new RaiseException(getContext(), coreExceptions().systemExit(0, currentNode));
                        } else {
                            thread.status = ThreadStatus.ABORTING;
                            throw new KillException();
                        }
                    });

            return rubyThread;
        }

    }

    @CoreMethod(names = "handle_interrupt", required = 2, needsBlock = true, visibility = Visibility.PRIVATE)
    public abstract static class HandleInterruptNode extends YieldingCoreMethodNode {

        @CompilationFinal private RubySymbol immediateSymbol;
        @CompilationFinal private RubySymbol onBlockingSymbol;
        @CompilationFinal private RubySymbol neverSymbol;

        private final BranchProfile errorProfile = BranchProfile.create();

        @Specialization
        protected Object handleInterrupt(RubyThread self, RubyClass exceptionClass, RubySymbol timing, RubyProc block,
                @CachedLanguage RubyLanguage language) {
            // TODO (eregon, 12 July 2015): should we consider exceptionClass?
            final InterruptMode newInterruptMode = symbolToInterruptMode(language, timing);

            final InterruptMode oldInterruptMode = self.interruptMode;
            self.interruptMode = newInterruptMode;
            try {
                return yield(block);
            } finally {
                self.interruptMode = oldInterruptMode;
            }
        }

        private InterruptMode symbolToInterruptMode(RubyLanguage language, RubySymbol symbol) {
            if (symbol == language.coreSymbols.IMMEDIATE) {
                return InterruptMode.IMMEDIATE;
            } else if (symbol == language.coreSymbols.ON_BLOCKING) {
                return InterruptMode.ON_BLOCKING;
            } else if (symbol == language.coreSymbols.NEVER) {
                return InterruptMode.NEVER;
            } else {
                errorProfile.enter();
                throw new RaiseException(getContext(), coreExceptions().argumentError("invalid timing symbol", this));
            }
        }

    }

    @Primitive(name = "thread_allocate")
    public abstract static class ThreadAllocateNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyThread allocate(RubyClass rubyClass,
                @Cached AllocateHelperNode allocateNode,
                @CachedLanguage RubyLanguage language) {
            final Shape shape = allocateNode.getCachedShape(rubyClass);
            final RubyThread instance = getContext().getThreadManager().createThread(rubyClass, shape, language);
            allocateNode.trace(instance, this, language);
            return instance;
        }

    }

    @Primitive(name = "thread_initialized?")
    public abstract static class ThreadIsInitializedNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected boolean isInitialized(RubyThread thread) {
            final RubyFiber rootFiber = thread.fiberManager.getRootFiber();
            return rootFiber.initializedLatch.getCount() == 0;
        }

    }

    @Primitive(name = "thread_initialize")
    @ImportStatic(ArrayGuards.class)
    public abstract static class ThreadInitializeNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(limit = "storageStrategyLimit()")
        protected Object initialize(RubyThread thread, RubyArray arguments, RubyProc block,
                @CachedLibrary("arguments.store") ArrayStoreLibrary stores) {
            final SourceSection sourceSection = block.sharedMethodInfo.getSourceSection();
            final String info = RubyContext.fileLine(sourceSection);
            final int argSize = arguments.size;
            final Object[] args = stores.boxedCopyOfRange(arguments.store, 0, argSize);
            final String sharingReason = "creating Ruby Thread " + info;

            if (getContext().getOptions().SHARED_OBJECTS_ENABLED) {
                getContext().getThreadManager().startSharing(thread, sharingReason);
                SharedObjects.shareDeclarationFrame(getContext(), block);
            }

            getContext().getThreadManager().initialize(
                    thread,
                    this,
                    info,
                    sharingReason,
                    () -> ProcOperations.rootCall(block, args));
            return nil;
        }

    }

    @CoreMethod(names = "join", optional = 1, lowerFixnum = 1)
    public abstract static class JoinNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyThread join(RubyThread thread, NotProvided timeout) {
            doJoin(getContext(), this, thread);
            return thread;
        }

        @Specialization
        protected RubyThread join(RubyThread thread, Nil timeout) {
            return join(thread, NotProvided.INSTANCE);
        }

        @Specialization
        protected Object join(RubyThread thread, int timeout) {
            return joinMillis(thread, timeout * 1000);
        }

        @Specialization
        protected Object join(RubyThread thread, double timeout) {
            return joinMillis(thread, (int) (timeout * 1000.0));
        }

        private Object joinMillis(RubyThread self, int timeoutInMillis) {
            if (doJoinMillis(self, timeoutInMillis)) {
                return self;
            } else {
                return nil;
            }
        }

        @TruffleBoundary
        static void doJoin(RubyContext context, Node currentNode, RubyThread thread) {
            context.getThreadManager().runUntilResult(currentNode, () -> {
                thread.finishedLatch.await();
                return ThreadManager.BlockingAction.SUCCESS;
            });

            final RubyException exception = thread.exception;
            if (exception != null) {
                context.getCoreExceptions().showExceptionIfDebug(exception);
                VMRaiseExceptionNode.reRaiseException(context, exception);
            }
        }

        @TruffleBoundary
        private boolean doJoinMillis(RubyThread thread, int timeoutInMillis) {
            final long start = System.currentTimeMillis();

            final boolean joined = getContext().getThreadManager().runUntilResult(this, () -> {
                long now = System.currentTimeMillis();
                long waited = now - start;
                if (waited >= timeoutInMillis) {
                    // We need to know whether countDown() was called and we do not want to block.
                    return thread.finishedLatch.getCount() == 0;
                }
                return thread.finishedLatch.await(timeoutInMillis - waited, TimeUnit.MILLISECONDS);
            });

            if (joined) {
                final RubyException exception = thread.exception;
                if (exception != null) {
                    getContext().getCoreExceptions().showExceptionIfDebug(exception);
                    VMRaiseExceptionNode.reRaiseException(getContext(), exception);
                }
            }

            return joined;
        }

    }

    @CoreMethod(names = "main", onSingleton = true)
    public abstract static class MainNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyThread main() {
            return getContext().getThreadManager().getRootThread();
        }

    }

    @CoreMethod(names = "pass", onSingleton = true)
    public abstract static class PassNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object pass() {
            Thread.yield();
            return nil;
        }

    }

    @CoreMethod(names = "status")
    public abstract static class StatusNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @Specialization
        protected Object status(RubyThread self) {
            // TODO: slightly hackish
            final ThreadStatus status = self.status;
            if (status == ThreadStatus.DEAD) {
                if (self.exception != null) {
                    return nil;
                } else {
                    return false;
                }
            }
            return makeStringNode
                    .executeMake(StringUtils.toLowerCase(status.name()), USASCIIEncoding.INSTANCE, CodeRange.CR_7BIT);
        }

    }

    @CoreMethod(names = "stop?")
    public abstract static class StopNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected boolean stop(RubyThread self) {
            final ThreadStatus status = self.status;
            return status == ThreadStatus.DEAD || status == ThreadStatus.SLEEP;
        }

    }

    @CoreMethod(names = "value")
    public abstract static class ValueNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object value(RubyThread self) {
            JoinNode.doJoin(getContext(), this, self);
            final Object value = self.value;
            assert value != null;
            return value;
        }

    }

    @CoreMethod(names = { "wakeup", "run" })
    public abstract static class WakeupNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyThread wakeup(RubyThread rubyThread) {
            final RubyFiber currentFiber = rubyThread.fiberManager.getCurrentFiberRacy();
            final Thread thread = currentFiber.thread;
            if (!currentFiber.alive || thread == null) {
                throw new RaiseException(getContext(), coreExceptions().threadErrorKilledThread(this));
            }

            // This only interrupts Kernel#sleep, Mutex#sleep and ConditionVariable#wait by having those check for the
            // wakeup flag. Other operations just retry when interrupted.
            rubyThread.wakeUp.set(true);
            getContext().getThreadManager().interrupt(thread);

            return rubyThread;
        }

    }

    @NonStandard
    @Primitive(name = "call_with_unblocking_function")
    public abstract static class CallWithUnblockingFunctionNode extends PrimitiveArrayArgumentsNode {

        @Specialization(limit = "getCacheLimit()")
        protected Object call(RubyThread thread, Object function, Object arg, Object unblocker, Object unblockerArg,
                @CachedLibrary("function") InteropLibrary receivers,
                @Cached TranslateInteropExceptionNode translateInteropExceptionNode) {
            final ThreadManager threadManager = getContext().getThreadManager();
            final UnblockingAction unblockingAction;
            if (unblocker == nil) {
                unblockingAction = threadManager.getNativeCallUnblockingAction();
            } else {
                unblockingAction = makeUnblockingAction(unblocker, unblockerArg);
            }

            final UnblockingActionHolder actionHolder = threadManager.getActionHolder(Thread.currentThread());
            final UnblockingAction oldAction = actionHolder.changeTo(unblockingAction);
            final ThreadStatus status = thread.status;
            thread.status = ThreadStatus.SLEEP;
            try {
                return InteropNodes.execute(function, new Object[]{ arg }, receivers, translateInteropExceptionNode);
            } finally {
                thread.status = status;
                actionHolder.restore(oldAction);
            }
        }

        @TruffleBoundary
        private UnblockingAction makeUnblockingAction(Object function, Object argument) {
            assert InteropLibrary.getUncached().isExecutable(function);
            return () -> {
                try {
                    InteropLibrary.getUncached().execute(function, argument);
                } catch (InteropException e) {
                    throw CompilerDirectives.shouldNotReachHere(e);
                }
            };
        }

        protected int getCacheLimit() {
            return getContext().getOptions().DISPATCH_CACHE;
        }
    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyArray list() {
            return createArray(getContext().getThreadManager().getThreadList());
        }
    }

    @Primitive(name = "thread_local_variables")
    public static abstract class ThreadLocalVariablesPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyHash threadLocalVariables(RubyThread thread) {
            return thread.threadLocalVariables;
        }

    }

    @Primitive(name = "thread_randomizer")
    public static abstract class ThreadRandomizerPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyRandomizer randomizer(
                @Cached GetCurrentRubyThreadNode getCurrentRubyThreadNode) {
            return getCurrentRubyThreadNode.execute().randomizer;
        }

    }

    @Primitive(name = "thread_recursive_objects")
    public static abstract class ThreadRecursiveObjectsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyHash recursiveObjects(
                @Cached GetCurrentRubyThreadNode getCurrentRubyThreadNode) {
            return getCurrentRubyThreadNode.execute().recursiveObjects;
        }

    }

    @Primitive(name = "thread_get_report_on_exception")
    public static abstract class ThreadGetReportOnExceptionPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected boolean getReportOnException(RubyThread thread) {
            return thread.reportOnException;
        }

    }

    @Primitive(name = "thread_set_report_on_exception")
    public static abstract class ThreadSetReportOnExceptionPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyThread setReportOnException(RubyThread thread, boolean value) {
            thread.reportOnException = value;
            return thread;
        }

    }

    @Primitive(name = "thread_get_abort_on_exception")
    public static abstract class ThreadGetAbortOnExceptionPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected boolean getAbortOnException(RubyThread thread) {
            return thread.abortOnException;
        }

    }

    @Primitive(name = "thread_set_abort_on_exception")
    public static abstract class ThreadSetAbortOnExceptionPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyThread setAbortOnException(RubyThread thread, boolean value) {
            thread.abortOnException = value;
            return thread;
        }

    }

    @Primitive(name = "thread_raise")
    public static abstract class ThreadRaisePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object raise(RubyThread thread, RubyException exception) {
            raiseInThread(getContext(), thread, exception, this);
            return nil;
        }

        @TruffleBoundary
        public static void raiseInThread(RubyContext context, RubyThread rubyThread, RubyException exception,
                Node currentNode) {
            // The exception will be shared with another thread
            SharedObjects.writeBarrier(context, exception);

            context.getSafepointManager().pauseRubyThreadAndExecute(
                    "Thread#raise",
                    rubyThread,
                    currentNode,
                    (currentThread, currentNode1) -> {
                        if (exception.backtrace == null) {
                            exception.backtrace = context.getCallStack().getBacktrace(currentNode1);
                        }

                        VMRaiseExceptionNode.reRaiseException(context, exception);
                    });
        }

    }

    @Primitive(name = "thread_source_location")
    public static abstract class ThreadSourceLocationNode extends PrimitiveArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @Specialization
        protected RubyString sourceLocation(RubyThread thread) {
            return makeStringNode
                    .executeMake(thread.sourceLocation, UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }
    }

    @CoreMethod(names = "name")
    public static abstract class ThreadNameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object getName(RubyThread thread) {
            return thread.name;
        }
    }

    @Primitive(name = "thread_set_name")
    public static abstract class ThreadSetNamePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object setName(RubyThread thread, Object name) {
            thread.name = name;
            return name;
        }
    }

    @Primitive(name = "thread_get_priority")
    public static abstract class ThreadGetPriorityPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected int getPriority(RubyThread thread) {
            final Thread javaThread = thread.thread;
            if (javaThread != null) {
                return javaThread.getPriority();
            } else {
                return thread.priority;
            }
        }

    }

    @Primitive(name = "thread_set_priority", lowerFixnum = 1)
    public static abstract class ThreadSetPriorityPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected int getPriority(RubyThread thread, int javaPriority) {
            final Thread javaThread = thread.thread;
            if (javaThread != null) {
                javaThread.setPriority(javaPriority);
            }
            thread.priority = javaPriority;
            return javaPriority;
        }

    }

    @Primitive(name = "thread_set_group")
    public static abstract class ThreadSetGroupPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object setGroup(RubyThread thread, Object threadGroup) {
            thread.threadGroup = threadGroup;
            return threadGroup;
        }
    }

    @Primitive(name = "thread_get_exception")
    public static abstract class ThreadGetExceptionNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object getException(
                @Cached GetCurrentRubyThreadNode getThreadNode) {
            return getLastException(getThreadNode.execute());
        }

        private static Object getLastException(RubyThread currentThread) {
            return currentThread.threadLocalGlobals.exception;
        }

        public static Object getLastException(RubyContext context) {
            return getLastException(context.getThreadManager().getCurrentThread());
        }

    }

    @Primitive(name = "thread_get_return_code")
    public static abstract class ThreadGetReturnCodeNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object getExitCode(
                @Cached GetCurrentRubyThreadNode getThreadNode) {
            return getThreadNode.execute().threadLocalGlobals.processStatus;
        }
    }

    @Primitive(name = "thread_set_exception")
    public static abstract class SetThreadLocalExceptionNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object setException(Object exception,
                @Cached GetCurrentRubyThreadNode getThreadNode) {
            return getThreadNode.execute().threadLocalGlobals.exception = exception;
        }
    }

    @Primitive(name = "thread_set_return_code")
    public static abstract class SetThreadLocalReturnCodeNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object getException(Object processStatus,
                @Cached GetCurrentRubyThreadNode getThreadNode) {
            return getThreadNode.execute().threadLocalGlobals.processStatus = processStatus;
        }
    }

    @Primitive(name = "thread_get_fiber_locals")
    public static abstract class ThreadGetFiberLocalsNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyBasicObject getFiberLocals(RubyThread thread) {
            final RubyFiber fiber = thread.fiberManager.getCurrentFiberRacy();
            return fiber.fiberLocals;
        }
    }

    /** Similar to {@link ThreadManager#runUntilResult(Node, ThreadManager.BlockingAction)} but purposed for blocking
     * native calls. If the {@link SafepointManager} needs to interrupt the thread, it will send a SIGVTALRM to abort
     * the blocking syscall and the action will return NotProvided if the syscall fails with errno=EINTR, meaning it was
     * interrupted. */
    @Primitive(name = "thread_run_blocking_nfi_system_call")
    public static abstract class ThreadRunBlockingSystemCallNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object runBlockingSystemCall(RubyProc block,
                @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                @Cached YieldNode yieldNode) {
            final ThreadManager threadManager = getContext().getThreadManager();
            final UnblockingAction unblockingAction = threadManager.getNativeCallUnblockingAction();
            final RubyThread thread = threadManager.getCurrentThread();
            final UnblockingActionHolder actionHolder = threadManager.getActionHolder(Thread.currentThread());

            final UnblockingAction oldAction = actionHolder.changeTo(unblockingAction);
            try {
                Object result;
                do {
                    final ThreadStatus status = thread.status;
                    thread.status = ThreadStatus.SLEEP;

                    try {
                        result = yieldNode.executeDispatch(block);
                    } finally {
                        thread.status = status;
                    }
                } while (loopProfile.profile(result == NotProvided.INSTANCE));

                return result;
            } finally {
                actionHolder.restore(oldAction);
            }
        }
    }

}
