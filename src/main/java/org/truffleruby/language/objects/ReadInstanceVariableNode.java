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

import org.truffleruby.RubyContext;
import org.truffleruby.language.RubyContextSourceNode;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.WarningNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class ReadInstanceVariableNode extends RubyContextSourceNode {

    private final String name;

    @Child private RubyNode receiver;
    @Child private DynamicObjectLibrary objectLibrary;
    @Child private WarningNode warningNode;

    private final ConditionProfile objectProfile = ConditionProfile.create();

    private final boolean warnIfUndefined;

    public ReadInstanceVariableNode(String name, RubyNode receiver, boolean warnIfUndefined) {
        this.warnIfUndefined = warnIfUndefined;
        this.name = name;
        this.receiver = receiver;
    }

    public ReadInstanceVariableNode(String name, RubyNode receiver) {
        this(name, receiver, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (objectProfile.profile(receiverObject instanceof RubyDynamicObject)) {
            final DynamicObjectLibrary objectLibrary = getObjectLibrary();
            final RubyDynamicObject dynamicObject = (RubyDynamicObject) receiverObject;
            if (warnIfUndefined && !objectLibrary.containsKey(dynamicObject, name)) {
                warnNotInitialized();
            }
            return objectLibrary.getOrDefault(dynamicObject, name, nil);
        } else {
            return nil;
        }
    }

    @TruffleBoundary
    private String getWarningMessage() {
        return String.format("instance variable %s not initialized", name);
    }

    @Override
    public Object isDefined(VirtualFrame frame, RubyContext context) {
        final Object receiverObject = receiver.execute(frame);

        if (objectProfile.profile(receiverObject instanceof RubyDynamicObject)) {
            final DynamicObjectLibrary objectLibrary = getObjectLibrary();
            final RubyDynamicObject dynamicObject = (RubyDynamicObject) receiverObject;
            if (objectLibrary.containsKey(dynamicObject, name)) {
                return coreStrings().INSTANCE_VARIABLE.createInstance(context);
            } else {
                return nil;
            }
        } else {
            return false;
        }
    }

    private DynamicObjectLibrary getObjectLibrary() {
        if (objectLibrary == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            objectLibrary = insert(
                    DynamicObjectLibrary
                            .getFactory()
                            .createDispatched(getContext().getOptions().INSTANCE_VARIABLE_CACHE));
        }
        return objectLibrary;
    }

    private void warnNotInitialized() {
        if (warningNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            warningNode = insert(new WarningNode());
        }

        if (warningNode.shouldWarn()) {
            warningNode.warningMessage(getSourceSection(), getWarningMessage());
        }
    }

}
