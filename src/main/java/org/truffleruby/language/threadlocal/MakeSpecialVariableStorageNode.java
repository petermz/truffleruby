/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.threadlocal;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import org.truffleruby.Layouts;
import org.truffleruby.language.RubyContextSourceNode;

public class MakeSpecialVariableStorageNode extends RubyContextSourceNode {

    @CompilationFinal protected FrameSlot storageSlot;
    @CompilationFinal protected Assumption frameAssumption;

    @Override
    public Object execute(VirtualFrame frame) {
        if (frameAssumption == null || !frameAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final FrameDescriptor descriptor = frame.getFrameDescriptor();
            frameAssumption = descriptor.getVersion();
            storageSlot = descriptor.findFrameSlot(Layouts.SPECIAL_VARIABLES_STORAGE);
        }

        if (storageSlot != null) {
            frame.setObject(storageSlot, new SpecialVariableStorage());
        }

        return nil;
    }

}
