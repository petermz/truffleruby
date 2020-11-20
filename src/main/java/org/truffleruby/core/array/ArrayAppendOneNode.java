/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.array;

import static org.truffleruby.core.array.ArrayHelpers.setSize;
import static org.truffleruby.core.array.ArrayHelpers.setStoreAndSize;

import org.truffleruby.core.array.library.ArrayStoreLibrary;
import org.truffleruby.language.RubyContextSourceNode;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.objects.shared.PropagateSharingNode;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;

@NodeChild(value = "array", type = RubyNode.class)
@NodeChild(value = "value", type = RubyNode.class)
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendOneNode extends RubyContextSourceNode {

    @Child private PropagateSharingNode propagateSharingNode = PropagateSharingNode.create();

    public static ArrayAppendOneNode create() {
        return ArrayAppendOneNodeGen.create(null, null);
    }

    public abstract RubyArray executeAppendOne(RubyArray array, Object value);

    // Append of the correct type

    @Specialization(
            guards = { "stores.acceptsValue(array.store, value)" },
            limit = "storageStrategyLimit()")
    protected RubyArray appendOneSameType(RubyArray array, Object value,
            @CachedLibrary("array.store") ArrayStoreLibrary stores,
            @Cached("createCountingProfile()") ConditionProfile extendProfile) {
        final Object store = array.store;
        final int oldSize = array.size;
        final int newSize = oldSize + 1;
        final int length = stores.capacity(store);

        propagateSharingNode.executePropagate(array, value);

        if (extendProfile.profile(newSize > length)) {
            final int capacity = ArrayUtils.capacityForOneMore(getContext(), length);
            final Object newStore = stores.expand(store, capacity);
            stores.write(newStore, oldSize, value);
            setStoreAndSize(array, newStore, newSize);
        } else {
            stores.write(store, oldSize, value);
            setSize(array, newSize);
        }
        return array;
    }

    // Append forcing a generalization

    @Specialization(
            guards = "!currentStores.acceptsValue(array.store, value)",
            limit = "storageStrategyLimit()")
    protected RubyArray appendOneGeneralizeNonMutable(RubyArray array, Object value,
            @CachedLibrary("array.store") ArrayStoreLibrary currentStores,
            @CachedLibrary(limit = "storageStrategyLimit()") ArrayStoreLibrary newStores) {
        final int oldSize = array.size;
        final int newSize = oldSize + 1;
        final Object currentStore = array.store;
        final int oldCapacity = currentStores.capacity(currentStore);
        final int newCapacity = newSize > oldCapacity
                ? ArrayUtils.capacityForOneMore(getContext(), oldCapacity)
                : oldCapacity;
        final Object newStore = currentStores.allocateForNewValue(currentStore, value, newCapacity);
        currentStores.copyContents(currentStore, 0, newStore, 0, oldSize);
        propagateSharingNode.executePropagate(array, value);
        newStores.write(newStore, oldSize, value);
        setStoreAndSize(array, newStore, newSize);
        return array;
    }
}
