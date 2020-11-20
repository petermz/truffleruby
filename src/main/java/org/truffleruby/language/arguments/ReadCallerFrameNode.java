/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.arguments;

import org.truffleruby.language.RubyContextNode;
import org.truffleruby.language.FrameOrStorageSendingNode;
import org.truffleruby.language.NotOptimizedWarningNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class ReadCallerFrameNode extends RubyContextNode {

    private final ConditionProfile callerFrameProfile = ConditionProfile.create();
    @CompilationFinal private volatile boolean deoptWhenNotPassedCallerFrame = true;
    @Child private NotOptimizedWarningNode notOptimizedNode = null;

    public static ReadCallerFrameNode create() {
        return new ReadCallerFrameNode();
    }

    public MaterializedFrame execute(VirtualFrame frame) {
        final MaterializedFrame callerFrame = RubyArguments.getCallerFrame(frame);

        if (callerFrameProfile.profile(callerFrame != null)) {
            return callerFrame;
        } else {
            // Every time the caller of the method using ReadCallerFrameNode changes,
            // we need to notify the caller's CachedDispatchNode to pass us the frame next time.
            if (deoptWhenNotPassedCallerFrame) {
                // Invalidate because deoptWhenNotPassedCallerFrame might change and require recompilation
                CompilerDirectives.transferToInterpreterAndInvalidate();
            }
            return getCallerFrame();
        }
    }

    @TruffleBoundary
    private MaterializedFrame getCallerFrame() {
        if (!notifyCallerToSendFrame()) {
            // If we fail to notify the call node (e.g., because it is a UncachedDispatchNode which is not handled yet),
            // we don't want to deoptimize this CallTarget on every call.
            getNotOptimizedNode().warn("Unoptimized reading of caller frame.");
            deoptWhenNotPassedCallerFrame = false;
        }
        return getContext().getCallStack().getCallerFrameIgnoringSend(FrameAccess.MATERIALIZE).materialize();
    }

    private boolean notifyCallerToSendFrame() {
        final Node callerNode = getContext().getCallStack().getCallerNode(1, false);
        if (callerNode instanceof DirectCallNode || callerNode instanceof IndirectCallNode) {
            Node parent = callerNode.getParent();
            while (parent != null) {
                if (parent instanceof FrameOrStorageSendingNode) {
                    ((FrameOrStorageSendingNode) parent).startSendingOwnFrame();
                    return true;
                }
                if (parent instanceof RubyContextNode) {
                    return false;
                }
                parent = parent.getParent();
            }
        }

        return false;
    }

    public NotOptimizedWarningNode getNotOptimizedNode() {
        if (notOptimizedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            notOptimizedNode = insert(NotOptimizedWarningNode.create());
        }
        return notOptimizedNode;
    }
}
