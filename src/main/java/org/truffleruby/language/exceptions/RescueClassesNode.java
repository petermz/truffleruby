/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.exceptions;

import org.truffleruby.RubyContext;
import org.truffleruby.core.exception.RubyException;
import org.truffleruby.language.RubyNode;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class RescueClassesNode extends RescueNode {

    @Children final RubyNode[] handlingClassNodes;

    public RescueClassesNode(RubyNode[] handlingClassNodes, RubyNode rescueBody) {
        super(rescueBody);
        this.handlingClassNodes = handlingClassNodes;
    }

    @ExplodeLoop
    @Override
    public boolean canHandle(VirtualFrame frame, RubyException exception) {
        for (RubyNode handlingClassNode : handlingClassNodes) {
            final Object handlingClass = handlingClassNode.execute(frame);
            if (matches(exception, handlingClass)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object isDefined(VirtualFrame frame, RubyContext context) {
        return RubyNode.defaultIsDefined(context, this);
    }

}
