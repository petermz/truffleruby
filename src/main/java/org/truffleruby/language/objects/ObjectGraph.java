/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.objects;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;

import org.truffleruby.RubyContext;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.arguments.RubyArguments;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Property;

public abstract class ObjectGraph {

    public static Set<Object> newObjectSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @TruffleBoundary
    public static Set<Object> stopAndGetAllObjects(String reason, RubyContext context, Node currentNode) {
        context.getMarkingService().queueMarking();
        final Set<Object> visited = newObjectSet();

        final Thread initiatingJavaThread = Thread.currentThread();

        context.getSafepointManager().pauseAllThreadsAndExecute(reason, currentNode, false, (thread, currentNode1) -> {
            synchronized (visited) {
                final Set<Object> reachable = newObjectSet();
                // Thread.current
                reachable.add(thread);
                // Fiber.current
                reachable.add(thread.fiberManager.getCurrentFiber());

                if (Thread.currentThread() == initiatingJavaThread) {
                    visitContextRoots(context, reachable);
                }

                Truffle.getRuntime().iterateFrames(frameInstance -> {
                    final Frame frame = frameInstance.getFrame(FrameAccess.READ_ONLY);
                    getObjectsInFrame(frame, reachable);
                    return null;
                });

                // NOTE: similar to SharedObjects.shareObjects()
                final Deque<Object> stack = new ArrayDeque<>(reachable);
                while (!stack.isEmpty()) {
                    final Object object = stack.pop();

                    if (visited.add(object)) {
                        if (object instanceof RubyDynamicObject) {
                            stack.addAll(ObjectGraph.getAdjacentObjects((RubyDynamicObject) object));
                        }
                    }
                }
            }
        });

        return visited;
    }

    @TruffleBoundary
    public static Set<Object> stopAndGetRootObjects(String reason, RubyContext context, Node currentNode) {
        final Set<Object> visited = newObjectSet();

        final Thread initiatingJavaThread = Thread.currentThread();

        context.getSafepointManager().pauseAllThreadsAndExecute(reason, currentNode, false, (thread, currentNode1) -> {
            synchronized (visited) {
                visited.add(thread);

                if (Thread.currentThread() == initiatingJavaThread) {
                    visitContextRoots(context, visited);
                }
            }
        });

        return visited;
    }

    public static void visitContextRoots(RubyContext context, Set<Object> roots) {
        // We do not want to expose the global object
        roots.addAll(context.getCoreLibrary().globalVariables.objectGraphValues());
        roots.addAll(context.getAtExitManager().getHandlers());
        context.getFinalizationService().collectRoots(roots);
    }

    public static boolean isSymbolOrDynamicObject(Object value) {
        return value instanceof RubyDynamicObject || value instanceof RubySymbol;
    }

    public static Set<Object> getAdjacentObjects(RubyDynamicObject object) {
        final Set<Object> reachable = newObjectSet();

        reachable.add(object.getLogicalClass());
        reachable.add(object.getMetaClass());

        if (object instanceof ObjectGraphNode) {
            ((ObjectGraphNode) object).getAdjacentObjects(reachable);
        }

        for (Property property : object.getShape().getPropertyListInternal(false)) {
            final Object value = property.get(object, object.getShape());
            addProperty(reachable, value);
        }

        return reachable;
    }

    public static void addProperty(Set<Object> reachable, Object value) {
        assert !(value instanceof Frame) : "Frame should be handled directly with ObjectGraphNode";
        assert !(value instanceof Collection) : "Collection should be handled directly with ObjectGraphNode";

        if (isSymbolOrDynamicObject(value)) {
            reachable.add(value);
        } else if (value instanceof Object[]) {
            for (Object element : (Object[]) value) {
                if (isSymbolOrDynamicObject(element)) {
                    reachable.add(element);
                }
            }
        } else if (value instanceof ObjectGraphNode) {
            ((ObjectGraphNode) value).getAdjacentObjects(reachable);
        }
    }

    public static void getObjectsInFrame(Frame frame, Set<Object> reachable) {
        final Frame lexicalParentFrame = RubyArguments.tryGetDeclarationFrame(frame);
        if (lexicalParentFrame != null) {
            getObjectsInFrame(lexicalParentFrame, reachable);
        }

        final Object self = RubyArguments.tryGetSelf(frame);
        if (isSymbolOrDynamicObject(self)) {
            reachable.add(self);
        }

        final RubyProc block = RubyArguments.tryGetBlock(frame);
        if (block != null) {
            reachable.add(block);
        }

        // Other frame arguments are either only internal or user arguments which appear in slots.

        for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
            final Object slotValue = frame.getValue(slot);

            if (isSymbolOrDynamicObject(slotValue)) {
                reachable.add(slotValue);
            }
        }
    }

}
