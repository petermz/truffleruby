/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.tracepoint;

import org.truffleruby.RubyContext;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringOperations;
import org.truffleruby.language.yield.YieldNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.source.Source;

public class TraceBaseEventNode extends ExecutionEventNode {

    protected final RubyContext context;
    protected final EventContext eventContext;

    @CompilationFinal private RubyString file;
    @CompilationFinal private int line;

    @Child private YieldNode yieldNode;

    public TraceBaseEventNode(RubyContext context, EventContext eventContext) {
        this.context = context;
        this.eventContext = eventContext;
    }

    protected RubyString getFile() {
        if (file == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final Source source = eventContext.getInstrumentedSourceSection().getSource();
            file = StringOperations.createString(context, context.getPathToRopeCache().getCachedPath(source));
        }
        return file;
    }

    protected int getLine() {
        if (line == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            line = eventContext.getInstrumentedSourceSection().getStartLine();
        }
        return line;
    }

    protected Object yield(RubyProc block, Object... arguments) {
        if (yieldNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            yieldNode = insert(YieldNode.create());
        }

        return yieldNode.executeDispatch(block, arguments);
    }

}
