/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.format.convert;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.library.CachedLibrary;
import org.truffleruby.cext.CExtNodes;
import org.truffleruby.core.format.FormatFrameDescriptor;
import org.truffleruby.core.format.FormatNode;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.extra.ffi.Pointer;
import org.truffleruby.language.Nil;
import org.truffleruby.language.library.RubyLibrary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeChild("value")
public abstract class StringToPointerNode extends FormatNode {

    @Specialization
    protected long toPointer(Nil nil) {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Specialization(limit = "getRubyLibraryCacheLimit()")
    protected long toPointer(VirtualFrame frame, RubyString string,
            @Cached CExtNodes.StringToNativeNode stringToNativeNode,
            @CachedLibrary("string") RubyLibrary rubyLibrary) {
        rubyLibrary.taint(string);

        final Pointer pointer = stringToNativeNode.executeToNative(string).getNativePointer();

        List<Pointer> associated;

        try {
            associated = (List<Pointer>) frame.getObject(FormatFrameDescriptor.ASSOCIATED_SLOT);
        } catch (FrameSlotTypeException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }

        if (associated == null) {
            associated = new ArrayList<>();
            frame.setObject(FormatFrameDescriptor.ASSOCIATED_SLOT, associated);
        }

        add(associated, pointer);

        return pointer.getAddress();
    }

    @TruffleBoundary
    private static void add(List<Pointer> list, Pointer ptr) {
        list.add(ptr);
    }
}
