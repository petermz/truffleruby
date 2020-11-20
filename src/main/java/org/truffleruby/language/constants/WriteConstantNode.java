/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.constants;

import org.truffleruby.core.constant.WarnAlreadyInitializedNode;
import org.truffleruby.core.module.RubyModule;
import org.truffleruby.language.RubyConstant;
import org.truffleruby.language.RubyContextSourceNode;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.control.RaiseException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;

public class WriteConstantNode extends RubyContextSourceNode {

    private final String name;

    @Child private RubyNode moduleNode;
    @Child private RubyNode valueNode;
    @Child private WarnAlreadyInitializedNode warnAlreadyInitializedNode;

    private final ConditionProfile moduleProfile = ConditionProfile.create();

    public WriteConstantNode(String name, RubyNode moduleNode, RubyNode valueNode) {
        this.name = name;
        this.moduleNode = moduleNode;
        this.valueNode = valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = valueNode.execute(frame);
        final Object moduleObject = moduleNode.execute(frame);

        if (!moduleProfile.profile(moduleObject instanceof RubyModule)) {
            throw new RaiseException(getContext(), coreExceptions().typeErrorIsNotAClassModule(moduleObject, this));
        }

        final RubyConstant previous = ((RubyModule) moduleObject).fields
                .setConstant(getContext(), this, name, value);
        if (previous != null && previous.hasValue()) {
            warnAlreadyInitializedConstant((RubyModule) moduleObject, name, previous.getSourceSection());
        }
        return value;
    }

    private void warnAlreadyInitializedConstant(RubyModule module, String name, SourceSection prevSourceSection) {
        if (warnAlreadyInitializedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            warnAlreadyInitializedNode = insert(new WarnAlreadyInitializedNode());
        }

        if (warnAlreadyInitializedNode.shouldWarn()) {
            warnAlreadyInitializedNode.warnAlreadyInitialized(module, name, getSourceSection(), prevSourceSection);
        }
    }

}
