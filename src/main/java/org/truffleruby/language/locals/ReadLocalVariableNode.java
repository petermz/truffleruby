/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.locals;

import org.truffleruby.language.RubyNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ReadLocalVariableNode extends ReadLocalNode {

    public ReadLocalVariableNode(LocalVariableType type, FrameSlot frameSlot) {
        super(frameSlot, type);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return readFrameSlot(frame);
    }

    @Override
    protected Object readFrameSlot(VirtualFrame frame) {
        if (readFrameSlotNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readFrameSlotNode = insert(ReadFrameSlotNodeGen.create(frameSlot));
        }

        return readFrameSlotNode.executeRead(frame);
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return new WriteLocalVariableNode(frameSlot, rhs);
    }

}
