/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.hash;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.core.numeric.BigIntegerOps;
import org.truffleruby.core.numeric.RubyBignum;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.language.Nil;
import org.truffleruby.language.RubyBaseNode;
import org.truffleruby.language.objects.shared.SharedObjects;

public abstract class HashOperations {

    public static RubyHash newEmptyHash(RubyContext context) {
        final Object nil = Nil.INSTANCE;
        return new RubyHash(
                context.getCoreLibrary().hashClass,
                RubyLanguage.hashShape,
                context,
                null,
                0,
                null,
                null,
                nil,
                nil,
                false);
    }

    @TruffleBoundary
    public static boolean verifyStore(RubyContext context, RubyHash hash) {
        final Object store = hash.store;
        final int size = hash.size;
        final Entry firstInSequence = hash.firstInSequence;
        final Entry lastInSequence = hash.lastInSequence;

        assert store == null || store.getClass() == Object[].class || store instanceof Entry[];

        if (store == null) {
            assert size == 0;
            assert firstInSequence == null;
            assert lastInSequence == null;
        } else if (store instanceof Entry[]) {
            assert lastInSequence == null || lastInSequence.getNextInSequence() == null;

            final Entry[] entryStore = (Entry[]) store;

            Entry foundFirst = null;
            Entry foundLast = null;
            int foundSizeBuckets = 0;

            for (int n = 0; n < entryStore.length; n++) {
                Entry entry = entryStore[n];

                while (entry != null) {
                    assert SharedObjects.assertPropagateSharing(
                            hash,
                            entry.getKey()) : "unshared key in shared Hash: " + entry.getKey();
                    assert SharedObjects.assertPropagateSharing(
                            hash,
                            entry.getValue()) : "unshared value in shared Hash: " + entry.getValue();

                    foundSizeBuckets++;

                    if (entry == firstInSequence) {
                        assert foundFirst == null;
                        foundFirst = entry;
                    }

                    if (entry == lastInSequence) {
                        assert foundLast == null;
                        foundLast = entry;
                    }

                    entry = entry.getNextInLookup();
                }
            }

            assert foundSizeBuckets == size;
            assert firstInSequence == foundFirst;
            assert lastInSequence == foundLast;

            int foundSizeSequence = 0;
            Entry entry = firstInSequence;

            while (entry != null) {
                foundSizeSequence++;

                if (entry.getNextInSequence() == null) {
                    assert entry == lastInSequence;
                } else {
                    assert entry.getNextInSequence().getPreviousInSequence() == entry;
                }

                entry = entry.getNextInSequence();

                assert entry != firstInSequence;
            }

            assert foundSizeSequence == size : StringUtils.format("%d %d", foundSizeSequence, size);
        } else if (store.getClass() == Object[].class) {
            assert ((Object[]) store).length == context.getLanguageSlow().options.HASH_PACKED_ARRAY_MAX *
                    PackedArrayStrategy.ELEMENTS_PER_ENTRY : ((Object[]) store).length;

            final Object[] packedStore = (Object[]) store;

            for (int i = 0; i < size * PackedArrayStrategy.ELEMENTS_PER_ENTRY; i++) {
                assert packedStore[i] != null;
            }

            for (int n = 0; n < size; n++) {
                final Object key = PackedArrayStrategy.getKey(packedStore, n);
                final Object value = PackedArrayStrategy.getValue(packedStore, n);

                assert SharedObjects.assertPropagateSharing(hash, key) : "unshared key in shared Hash: " + key;
                assert SharedObjects.assertPropagateSharing(hash, value) : "unshared value in shared Hash: " +
                        value;
            }

            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        return true;
    }

    // random number, stops hashes for similar values but different classes being the same, static because we want deterministic hashes
    public static final int BOOLEAN_CLASS_SALT = 55927484;
    public static final int INTEGER_CLASS_SALT = 1028093337;
    public static final int DOUBLE_CLASS_SALT = -1611229937;

    public static long hashBoolean(boolean value, RubyContext context, RubyBaseNode node) {
        return context.getHashing(node).hash(BOOLEAN_CLASS_SALT, Boolean.hashCode(value));
    }

    public static long hashLong(long value, RubyContext context, RubyBaseNode node) {
        return context.getHashing(node).hash(INTEGER_CLASS_SALT, value);
    }

    public static long hashDouble(double value, RubyContext context, RubyBaseNode node) {
        return context.getHashing(node).hash(DOUBLE_CLASS_SALT, Double.doubleToRawLongBits(value));
    }

    public static long hashBignum(RubyBignum value, RubyContext context, RubyBaseNode node) {
        return context.getHashing(node).hash(INTEGER_CLASS_SALT, BigIntegerOps.hashCode(value));
    }

}
