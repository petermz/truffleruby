/*
 * Copyright (c) 2014, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.objects;

import org.truffleruby.RubyLanguage;
import org.truffleruby.extra.ffi.Pointer;
import org.truffleruby.language.RubyBaseNode;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.objects.shared.WriteBarrierNode;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;

@ReportPolymorphism
@GenerateUncached
public abstract class WriteObjectFieldNode extends RubyBaseNode {

    public static WriteObjectFieldNode create() {
        return WriteObjectFieldNodeGen.create();
    }

    public abstract void execute(RubyDynamicObject object, Object name, Object value);

    @Specialization(guards = "!objectLibrary.isShared(object)", limit = "getCacheLimit()")
    protected void writeLocal(RubyDynamicObject object, Object name, Object value,
            @CachedLibrary("object") DynamicObjectLibrary objectLibrary) {
        objectLibrary.put(object, name, value);
    }

    @Specialization(guards = "objectLibrary.isShared(object)", limit = "getCacheLimit()")
    protected void writeShared(RubyDynamicObject object, Object name, Object value,
            @CachedLibrary("object") DynamicObjectLibrary objectLibrary,
            @Cached WriteBarrierNode writeBarrierNode,
            @Cached BranchProfile shapeRaceProfile) {

        // Share `value` before it becomes reachable through `object`
        writeBarrierNode.executeWriteBarrier(value);

        /* We need a STORE_STORE memory barrier here, to ensure the value is seen as shared by all threads when
         * published below by writing the value to a field of the object. Otherwise, the compiler could theoretically
         * move the write barrier inside the synchronized block, and then the compiler or hardware could potentially
         * reorder the writes so that publication would happen before sharing. */
        Pointer.UNSAFE.storeFence();

        synchronized (object) {
            // Re-check the shape under the monitor as another thread might have changed it
            // by adding a field or upgrading an existing field to Object storage
            // (need to use the new storage)
            if (!objectLibrary.accepts(object)) {
                shapeRaceProfile.enter();
                DynamicObjectLibrary.getUncached().put(object, name, value);
                return;
            }
            objectLibrary.put(object, name, value);
        }
    }

    protected int getCacheLimit() {
        return RubyLanguage.getCurrentContext().getOptions().INSTANCE_VARIABLE_CACHE;
    }
}
