/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.array;

import org.truffleruby.core.array.library.ArrayStoreLibrary;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.language.RubyContextNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;

@ImportStatic(ArrayGuards.class)
@ReportPolymorphism
public abstract class ArrayEachIteratorNode extends RubyContextNode {

    public interface ArrayElementConsumerNode extends NodeInterface {
        void accept(RubyArray array, RubyProc block, Object element, int index);
    }

    @Child private ArrayEachIteratorNode recurseNode;

    public static ArrayEachIteratorNode create() {
        return ArrayEachIteratorNodeGen.create();
    }

    public abstract RubyArray execute(RubyArray array, RubyProc block, int startAt,
            ArrayElementConsumerNode consumerNode);

    @Specialization(
            guards = { "array.size == 1", "startAt == 0" },
            limit = "storageStrategyLimit()")
    protected RubyArray iterateOne(RubyArray array, RubyProc block, int startAt, ArrayElementConsumerNode consumerNode,
            @CachedLibrary("array.store") ArrayStoreLibrary arrays) {
        final Object store = array.store;

        consumerNode.accept(array, block, arrays.read(store, 0), 0);

        if (array.size > 1) {
            // Implicitly profiles through lazy node creation
            return getRecurseNode().execute(array, block, 1, consumerNode);
        }

        return array;
    }

    @Specialization(
            guards = { "array.size != 1" },
            limit = "storageStrategyLimit()")
    protected RubyArray iterateMany(RubyArray array, RubyProc block, int startAt, ArrayElementConsumerNode consumerNode,
            @CachedLibrary("array.store") ArrayStoreLibrary arrays,
            @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
            @Cached ConditionProfile strategyMatchProfile) {
        int i = startAt;
        loopProfile.profileCounted(array.size - startAt);
        try {
            for (; loopProfile.inject(i < array.size); i++) {
                if (strategyMatchProfile.profile(arrays.accepts(array.store))) {
                    final Object store = array.store;
                    consumerNode.accept(array, block, arrays.read(store, i), i);
                } else {
                    return getRecurseNode().execute(array, block, i, consumerNode);
                }
            }
        } finally {
            LoopNode.reportLoopCount(this, i - startAt);
        }

        return array;
    }

    private ArrayEachIteratorNode getRecurseNode() {
        if (recurseNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recurseNode = insert(ArrayEachIteratorNode.create());
        }
        return recurseNode;
    }
}
