/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.kernel;

import java.util.ArrayList;
import java.util.Collection;

import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.core.binding.BindingNodes;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.tracepoint.TraceBaseEventNode;
import org.truffleruby.language.Nil;
import org.truffleruby.language.arguments.RubyArguments;
import org.truffleruby.language.objects.LogicalClassNode;
import org.truffleruby.shared.TruffleRuby;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class TraceManager {
    public static class LineTag extends Tag {
    }

    public static class CallTag extends Tag {
    }

    public static class ClassTag extends Tag {
    }

    /** A tag which applies to no Node, but is useful to handle not-yet-implemented TracePoint events. */
    public static class NeverTag extends Tag {
    }

    private final RubyContext context;
    private final RubyLanguage language;
    private final Instrumenter instrumenter;

    private Collection<EventBinding<?>> instruments;
    private boolean isInTraceFunc = false;

    public TraceManager(RubyLanguage language, RubyContext context, Instrumenter instrumenter) {
        this.language = language;
        this.context = context;
        this.instrumenter = instrumenter;
    }

    @TruffleBoundary
    public synchronized void setTraceFunc(RubyProc traceFunc) {

        if (instruments != null) {
            for (EventBinding<?> instrument : instruments) {
                instrument.dispose();
            }
        }

        if (traceFunc == null) {
            // Update to a new valid assumption
            language.traceFuncUnusedAssumption.invalidate();
            instruments = null;
            return;
        }

        // Invalidate current assumption
        language.traceFuncUnusedAssumption.getAssumption().invalidate();

        instruments = new ArrayList<>();

        instruments.add(
                instrumenter.attachExecutionEventFactory(
                        SourceSectionFilter.newBuilder().mimeTypeIs(TruffleRuby.MIME_TYPE).tagIs(LineTag.class).build(),
                        eventContext -> new BaseEventEventNode(
                                context,
                                eventContext,
                                traceFunc,
                                context.getCoreStrings().LINE.createInstance(context))));

        instruments.add(
                instrumenter.attachExecutionEventFactory(
                        SourceSectionFilter
                                .newBuilder()
                                .mimeTypeIs(TruffleRuby.MIME_TYPE)
                                .tagIs(ClassTag.class)
                                .build(),
                        eventContext -> new BaseEventEventNode(
                                context,
                                eventContext,
                                traceFunc,
                                context.getCoreStrings().CLASS.createInstance(context))));

        if (context.getOptions().TRACE_CALLS) {
            instruments.add(
                    instrumenter.attachExecutionEventFactory(
                            SourceSectionFilter
                                    .newBuilder()
                                    .mimeTypeIs(TruffleRuby.MIME_TYPE)
                                    .tagIs(CallTag.class)
                                    .includeInternal(false)
                                    .build(),
                            eventContext -> new CallEventEventNode(
                                    context,
                                    eventContext,
                                    traceFunc,
                                    context.getCoreStrings().CALL.createInstance(context))));
        }
    }

    private class BaseEventEventNode extends TraceBaseEventNode {

        protected final ConditionProfile inTraceFuncProfile = ConditionProfile.create();

        protected final RubyProc traceFunc;
        protected final Object event;

        public BaseEventEventNode(
                RubyContext context,
                EventContext eventContext,
                RubyProc traceFunc,
                Object event) {
            super(context, eventContext);
            this.traceFunc = traceFunc;
            this.event = event;
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            if (inTraceFuncProfile.profile(isInTraceFunc)) {
                return;
            }

            isInTraceFunc = true;
            try {
                yield(
                        traceFunc,
                        event,
                        getFile(),
                        getLine(),
                        Nil.INSTANCE,
                        BindingNodes.createBinding(
                                context,
                                frame.materialize(),
                                eventContext.getInstrumentedSourceSection()),
                        Nil.INSTANCE);
            } finally {
                isInTraceFunc = false;
            }
        }

    }

    private class CallEventEventNode extends BaseEventEventNode {

        @Child private LogicalClassNode logicalClassNode;

        public CallEventEventNode(
                RubyContext context,
                EventContext eventContext,
                RubyProc traceFunc,
                Object event) {
            super(context, eventContext, traceFunc, event);
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            if (inTraceFuncProfile.profile(isInTraceFunc)) {
                return;
            }

            isInTraceFunc = true;
            try {
                yield(
                        traceFunc,
                        event,
                        getFile(),
                        getLine(),
                        context.getSymbol(RubyArguments.getMethod(frame).getName()),
                        BindingNodes.createBinding(
                                context,
                                frame.materialize(),
                                eventContext.getInstrumentedSourceSection()),
                        getLogicalClass(RubyArguments.getSelf(frame)));
            } finally {
                isInTraceFunc = false;
            }
        }

        private RubyClass getLogicalClass(Object object) {
            if (logicalClassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                logicalClassNode = insert(LogicalClassNode.create());
            }

            return logicalClassNode.executeLogicalClass(object);
        }

    }

}
