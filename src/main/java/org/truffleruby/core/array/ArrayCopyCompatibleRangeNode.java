/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.array;

import com.oracle.truffle.api.profiles.LoopConditionProfile;
import org.truffleruby.core.array.library.ArrayStoreLibrary;
import org.truffleruby.language.RubyBaseNode;
import org.truffleruby.language.objects.shared.IsSharedNode;
import org.truffleruby.language.objects.shared.WriteBarrierNode;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;


/** Copies a portion of an array to another array, whose store is known to have sufficient capacity, and to be
 * compatible with the source array's store.
 * <p>
 * This never checks the array's size, which may therefore be adjusted afterwards.
 * <p>
 * Also propagates sharing from the source array to destination array.
 * <p>
 * Typically only called after {@link ArrayPrepareForCopyNode} has been invoked on the destination. */
@ImportStatic(ArrayGuards.class)
@ReportPolymorphism
public abstract class ArrayCopyCompatibleRangeNode extends RubyBaseNode {

    public static ArrayCopyCompatibleRangeNode create() {
        return ArrayCopyCompatibleRangeNodeGen.create();
    }

    public abstract void execute(RubyArray dst, RubyArray src, int dstStart, int srcStart, int length);

    protected boolean noopGuard(RubyArray dst, RubyArray src, int dstStart, int srcStart, int length) {
        return length == 0 || dst == src && dstStart == srcStart;
    }

    @Specialization(guards = "noopGuard(dst, src, dstStart, srcStart, length)")
    protected void noop(RubyArray dst, RubyArray src, int dstStart, int srcStart, int length) {
    }

    @Specialization(guards = "!noopGuard(dst, src, dstStart, srcStart, length)", limit = "storageStrategyLimit()")
    protected void copy(RubyArray dst, RubyArray src, int dstStart, int srcStart, int length,
            @CachedLibrary("src.store") ArrayStoreLibrary stores,
            @Cached IsSharedNode isDstShared,
            @Cached IsSharedNode isSrcShared,
            @Cached WriteBarrierNode writeBarrierNode,
            @Cached ConditionProfile share,
            @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {

        final Object srcStore = src.store;
        stores.copyContents(srcStore, srcStart, dst.store, dstStart, length);

        if (share.profile(!stores.isPrimitive(srcStore) &&
                isDstShared.executeIsShared(dst) &&
                !isSrcShared.executeIsShared(src))) {
            loopProfile.profileCounted(length);
            for (int i = 0; loopProfile.inject(i < length); ++i) {
                writeBarrierNode.executeWriteBarrier(stores.read(srcStore, i));
            }
        }
    }
}
