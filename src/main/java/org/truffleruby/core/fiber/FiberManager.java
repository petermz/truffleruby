/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.fiber;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.core.array.ArrayHelpers;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.basicobject.BasicObjectNodes.ObjectIDNode;
import org.truffleruby.core.basicobject.RubyBasicObject;
import org.truffleruby.core.exception.ExceptionOperations;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.proc.ProcOperations;
import org.truffleruby.core.thread.RubyThread;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.thread.ThreadManager;
import org.truffleruby.core.thread.ThreadManager.BlockingAction;
import org.truffleruby.language.control.BreakException;
import org.truffleruby.language.control.DynamicReturnException;
import org.truffleruby.language.control.ExitException;
import org.truffleruby.language.control.KillException;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.control.TerminationException;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;

/** Manages Ruby {@code Fiber} objects for a given Ruby thread. */
public class FiberManager {

    public static final String NAME_PREFIX = "Ruby Fiber";

    private final RubyContext context;
    private final RubyFiber rootFiber;
    private RubyFiber currentFiber;
    private final Set<RubyFiber> runningFibers = newFiberSet();

    public FiberManager(RubyLanguage language, RubyContext context, RubyThread rubyThread) {
        this.context = context;
        this.rootFiber = createRootFiber(language, context, rubyThread);
        this.currentFiber = rootFiber;
    }

    @TruffleBoundary
    private static Set<RubyFiber> newFiberSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public RubyFiber getRootFiber() {
        return rootFiber;
    }

    public RubyFiber getCurrentFiber() {
        assert context
                .getThreadManager()
                .getCurrentThread().fiberManager == this : "Trying to read the current Fiber of another Thread which is inherently racy";
        return currentFiber;
    }

    // If the currentFiber is read from another Ruby Thread,
    // there is no guarantee that fiber will remain the current one
    // as it could switch to another Fiber before the actual operation on the returned fiber.
    public RubyFiber getCurrentFiberRacy() {
        return currentFiber;
    }

    private void setCurrentFiber(RubyFiber fiber) {
        currentFiber = fiber;
    }

    private RubyFiber createRootFiber(RubyLanguage language, RubyContext context, RubyThread thread) {
        return createFiber(language, context, thread, context.getCoreLibrary().fiberClass, RubyLanguage.fiberShape);
    }

    public RubyFiber createFiber(RubyLanguage language, RubyContext context, RubyThread thread, RubyClass rubyClass,
            Shape shape) {
        CompilerAsserts.partialEvaluationConstant(language);
        final RubyBasicObject fiberLocals = new RubyBasicObject(
                context.getCoreLibrary().objectClass,
                language.basicObjectShape);
        final RubyArray catchTags = ArrayHelpers.createEmptyArray(context);

        return new RubyFiber(rubyClass, shape, fiberLocals, catchTags, thread);
    }

    public void initialize(RubyFiber fiber, RubyProc block, Node currentNode) {
        ThreadManager.FIBER_BEING_SPAWNED.set(fiber);
        try {
            context.getThreadManager().spawnFiber(() -> fiberMain(context, fiber, block, currentNode));
            waitForInitialization(context, fiber, currentNode);
        } finally {
            ThreadManager.FIBER_BEING_SPAWNED.remove();
        }
    }

    /** Wait for full initialization of the new fiber */
    public static void waitForInitialization(RubyContext context, RubyFiber fiber, Node currentNode) {
        final CountDownLatch initializedLatch = fiber.initializedLatch;

        context.getThreadManager().runUntilResultKeepStatus(currentNode, () -> {
            initializedLatch.await();
            return BlockingAction.SUCCESS;
        });

        final Throwable uncaughtException = fiber.uncaughtException;
        if (uncaughtException != null) {
            ExceptionOperations.rethrow(uncaughtException);
        }
    }

    private static final BranchProfile UNPROFILED = BranchProfile.create();

    private void fiberMain(RubyContext context, RubyFiber fiber, RubyProc block, Node currentNode) {
        assert fiber != rootFiber : "Root Fibers execute threadMain() and not fiberMain()";

        final Thread thread = Thread.currentThread();
        final SourceSection sourceSection = block.sharedMethodInfo.getSourceSection();
        final String oldName = thread.getName();
        thread.setName(NAME_PREFIX + " id=" + thread.getId() + " from " + RubyContext.fileLine(sourceSection));

        start(fiber, thread);
        try {

            final Object[] args = waitForResume(fiber);
            final Object result;
            try {
                result = ProcOperations.rootCall(block, args);
            } finally {
                // Make sure that other fibers notice we are dead before they gain control back
                fiber.alive = false;
            }
            resume(fiber, getReturnFiber(fiber, currentNode, UNPROFILED), FiberOperation.YIELD, result);

            // Handlers in the same order as in ThreadManager
        } catch (KillException | ExitException | RaiseException e) {
            // Propagate the exception until it reaches the root Fiber
            sendExceptionToParentFiber(fiber, e, currentNode);
        } catch (FiberShutdownException e) {
            // Ends execution of the Fiber
        } catch (BreakException e) {
            sendExceptionToParentFiber(
                    fiber,
                    new RaiseException(context, context.getCoreExceptions().breakFromProcClosure(currentNode)),
                    currentNode);
        } catch (DynamicReturnException e) {
            sendExceptionToParentFiber(
                    fiber,
                    new RaiseException(context, context.getCoreExceptions().unexpectedReturn(currentNode)),
                    currentNode);
        } finally {
            cleanup(fiber, thread);
            thread.setName(oldName);
        }
    }

    private void sendExceptionToParentFiber(RubyFiber fiber, RuntimeException exception, Node currentNode) {
        addToMessageQueue(getReturnFiber(fiber, currentNode, UNPROFILED), new FiberExceptionMessage(exception));
    }

    public RubyFiber getReturnFiber(RubyFiber currentFiber, Node currentNode, BranchProfile errorProfile) {
        assert currentFiber == this.currentFiber;

        if (currentFiber == rootFiber) {
            errorProfile.enter();
            throw new RaiseException(context, context.getCoreExceptions().yieldFromRootFiberError(currentNode));
        }

        final RubyFiber parentFiber = currentFiber.lastResumedByFiber;
        if (parentFiber != null) {
            currentFiber.lastResumedByFiber = null;
            return parentFiber;
        } else {
            return rootFiber;
        }
    }

    @TruffleBoundary
    private void addToMessageQueue(RubyFiber fiber, FiberMessage message) {
        fiber.messageQueue.add(message);
    }

    /** Send the Java thread that represents this fiber to sleep until it receives a resume or exit message. */
    @TruffleBoundary
    private Object[] waitForResume(RubyFiber fiber) {
        final FiberMessage message = context.getThreadManager().runUntilResultKeepStatus(
                null,
                () -> fiber.messageQueue.take());

        setCurrentFiber(fiber);

        if (message instanceof FiberShutdownMessage) {
            throw new FiberShutdownException();
        } else if (message instanceof FiberExceptionMessage) {
            throw ((FiberExceptionMessage) message).getException();
        } else if (message instanceof FiberResumeMessage) {
            final FiberResumeMessage resumeMessage = (FiberResumeMessage) message;
            assert context.getThreadManager().getCurrentThread() == resumeMessage.getSendingFiber().rubyThread;
            if (resumeMessage.getOperation() == FiberOperation.RESUME) {
                fiber.lastResumedByFiber = resumeMessage.getSendingFiber();
            }
            return resumeMessage.getArgs();
        } else {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    /** Send a resume message to a fiber by posting into its message queue. Doesn't explicitly notify the Java thread
     * (although the queue implementation may) and doesn't wait for the message to be received. */
    private void resume(RubyFiber fromFiber, RubyFiber fiber, FiberOperation operation, Object... args) {
        addToMessageQueue(fiber, new FiberResumeMessage(operation, fromFiber, args));
    }

    public Object[] transferControlTo(RubyFiber fromFiber, RubyFiber fiber, FiberOperation operation, Object[] args) {
        resume(fromFiber, fiber, operation, args);
        return waitForResume(fromFiber);
    }

    public void start(RubyFiber fiber, Thread javaThread) {
        final ThreadManager threadManager = context.getThreadManager();

        if (Thread.currentThread() == javaThread) {
            context.getThreadManager().rubyFiber.set(fiber);
        }
        if (!threadManager.isRubyManagedThread(javaThread)) {
            context.getThreadManager().rubyFiberForeignMap.put(javaThread, fiber);
        }

        fiber.thread = javaThread;

        final RubyThread rubyThread = fiber.rubyThread;
        threadManager.initializeValuesForJavaThread(rubyThread, javaThread);

        runningFibers.add(fiber);

        if (threadManager.isRubyManagedThread(javaThread)) {
            context.getSafepointManager().enterThread();
        }

        // fully initialized
        fiber.initializedLatch.countDown();
    }

    public void cleanup(RubyFiber fiber, Thread javaThread) {
        fiber.alive = false;

        if (context.getThreadManager().isRubyManagedThread(javaThread)) {
            context.getSafepointManager().leaveThread();
        }

        context.getThreadManager().cleanupValuesForJavaThread(javaThread);

        runningFibers.remove(fiber);

        fiber.thread = null;

        if (Thread.currentThread() == javaThread) {
            context.getThreadManager().rubyFiber.remove();
        }
        context.getThreadManager().rubyFiberForeignMap.remove(javaThread);

        fiber.finishedLatch.countDown();
    }

    @TruffleBoundary
    public void killOtherFibers() {
        // All Fibers except the current one are in waitForResume(),
        // so sending a FiberShutdownMessage is enough to finish them.
        // This also avoids the performance cost of a safepoint.
        for (RubyFiber fiber : runningFibers) {
            if (fiber != rootFiber) {
                addToMessageQueue(fiber, new FiberShutdownMessage());

                // Wait for the Fiber to finish so we only run one Fiber at a time
                final CountDownLatch finishedLatch = fiber.finishedLatch;
                context.getThreadManager().runUntilResultKeepStatus(null, () -> {
                    finishedLatch.await();
                    return BlockingAction.SUCCESS;
                });
            }
        }
    }

    @TruffleBoundary
    public void shutdown(Thread javaThread) {
        killOtherFibers();
        cleanup(rootFiber, javaThread);
    }

    public String getFiberDebugInfo() {
        final StringBuilder builder = new StringBuilder();

        for (RubyFiber fiber : runningFibers) {
            builder.append("  fiber @");
            builder.append(ObjectIDNode.getUncached().execute(fiber));
            builder.append(" #");

            final Thread thread = fiber.thread;

            if (thread == null) {
                builder.append("(no Java thread)");
            } else {
                builder.append(thread.getId());
            }

            if (fiber == rootFiber) {
                builder.append(" (root)");
            }

            if (fiber == currentFiber) {
                builder.append(" (current)");
            }

            builder.append("\n");
        }

        if (builder.length() == 0) {
            return "  no fibers\n";
        } else {
            return builder.toString();
        }
    }

    public interface FiberMessage {
    }

    private static class FiberResumeMessage implements FiberMessage {

        private final FiberOperation operation;
        private final RubyFiber sendingFiber;
        private final Object[] args;

        public FiberResumeMessage(FiberOperation operation, RubyFiber sendingFiber, Object[] args) {
            this.operation = operation;
            this.sendingFiber = sendingFiber;
            this.args = args;
        }

        public FiberOperation getOperation() {
            return operation;
        }

        public RubyFiber getSendingFiber() {
            return sendingFiber;
        }

        public Object[] getArgs() {
            return args;
        }

    }

    /** Used to cleanup and terminate Fibers when the parent Thread dies. */
    private static class FiberShutdownException extends TerminationException {
        private static final long serialVersionUID = 1522270454305076317L;
    }

    private static class FiberShutdownMessage implements FiberMessage {
    }

    private static class FiberExceptionMessage implements FiberMessage {

        private final RuntimeException exception;

        public FiberExceptionMessage(RuntimeException exception) {
            this.exception = exception;
        }

        public RuntimeException getException() {
            return exception;
        }

    }

}
