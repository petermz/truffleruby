/*
 * Copyright (c) 2016, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.tracepoint;

import com.oracle.truffle.api.dsl.CachedLanguage;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.builtins.UnaryCoreMethodNode;
import org.truffleruby.builtins.YieldingCoreMethodNode;
import org.truffleruby.core.array.ArrayToObjectArrayNode;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.binding.RubyBinding;
import org.truffleruby.core.kernel.TraceManager;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes.MakeStringNode;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.core.thread.GetCurrentRubyThreadNode;
import org.truffleruby.language.NotProvided;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.arguments.RubyArguments;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.methods.InternalMethod;
import org.truffleruby.language.objects.AllocateHelperNode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;

@CoreModule(value = "TracePoint", isClass = true)
public abstract class TracePointNodes {

    @TruffleBoundary
    public static boolean isEnabled(RubyTracePoint tracePoint) {
        return tracePoint.events[0].hasEventBinding();
    }

    @TruffleBoundary
    public static boolean createEventBindings(RubyContext context, RubyTracePoint tracePoint) {
        final TracePointEvent[] events = tracePoint.events;
        for (TracePointEvent event : events) {
            if (!event.setupEventBinding(context, tracePoint)) {
                return false;
            }
        }
        return true;
    }

    @TruffleBoundary
    public static boolean disposeEventBindings(RubyTracePoint tracePoint) {
        final TracePointEvent[] events = tracePoint.events;
        for (TracePointEvent event : events) {
            if (!event.diposeEventBinding()) {
                return false;
            }
        }
        return true;
    }

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();

        @Specialization
        protected RubyTracePoint allocate(RubyClass rubyClass,
                @CachedLanguage RubyLanguage language) {
            final Shape shape = allocateNode.getCachedShape(rubyClass);
            final RubyTracePoint instance = new RubyTracePoint(rubyClass, shape, null, null);
            allocateNode.trace(instance, this, language);
            return instance;
        }

    }

    @Primitive(name = "tracepoint_initialize")
    public abstract static class InitializeNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyTracePoint initialize(RubyTracePoint tracePoint, RubyArray eventsArray, RubyProc block,
                @Cached ArrayToObjectArrayNode arrayToObjectArrayNode,
                @CachedLanguage RubyLanguage language) {
            final Object[] eventSymbols = arrayToObjectArrayNode.executeToObjectArray(eventsArray);

            final TracePointEvent[] events = new TracePointEvent[eventSymbols.length];
            for (int i = 0; i < eventSymbols.length; i++) {
                events[i] = createEvents(language, (RubySymbol) eventSymbols[i]);
            }

            tracePoint.events = events;
            tracePoint.proc = block;
            return tracePoint;
        }

        @TruffleBoundary
        private TracePointEvent createEvents(RubyLanguage language, RubySymbol eventSymbol) {
            if (eventSymbol == language.coreSymbols.LINE) {
                return new TracePointEvent(TraceManager.LineTag.class, eventSymbol);
            } else if (eventSymbol == language.coreSymbols.CLASS) {
                return new TracePointEvent(TraceManager.ClassTag.class, eventSymbol);
            } else if (eventSymbol == language.coreSymbols.NEVER) {
                return new TracePointEvent(TraceManager.NeverTag.class, eventSymbol);
            } else {
                throw new UnsupportedOperationException(eventSymbol.getString());
            }
        }

    }

    @CoreMethod(names = "enable", needsBlock = true)
    public abstract static class EnableNode extends YieldingCoreMethodNode {

        @Specialization
        protected boolean enable(RubyTracePoint tracePoint, NotProvided block) {
            boolean setupDone = createEventBindings(getContext(), tracePoint);
            return !setupDone;
        }

        @Specialization
        protected Object enable(RubyTracePoint tracePoint, RubyProc block) {
            final boolean setupDone = createEventBindings(getContext(), tracePoint);
            try {
                return yield(block);
            } finally {
                if (setupDone) {
                    disposeEventBindings(tracePoint);
                }
            }
        }
    }

    @CoreMethod(names = "disable", needsBlock = true)
    public abstract static class DisableNode extends YieldingCoreMethodNode {

        @Specialization
        protected Object disable(RubyTracePoint tracePoint, NotProvided block) {
            return disposeEventBindings(tracePoint);
        }

        @Specialization
        protected Object disable(RubyTracePoint tracePoint, RubyProc block) {
            final boolean wasEnabled = disposeEventBindings(tracePoint);
            try {
                return yield(block);
            } finally {
                if (wasEnabled) {
                    createEventBindings(getContext(), tracePoint);
                }
            }
        }
    }

    @CoreMethod(names = "enabled?")
    public abstract static class EnabledNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected boolean enabled(RubyTracePoint tracePoint) {
            return isEnabled(tracePoint);
        }
    }

    private abstract static class TracePointCoreNode extends CoreMethodArrayArgumentsNode {

        @Child private GetCurrentRubyThreadNode getCurrentRubyThreadNode = GetCurrentRubyThreadNode.create();
        private final BranchProfile errorProfile = BranchProfile.create();

        protected TracePointState getTracePointState() {
            final TracePointState state = getCurrentRubyThreadNode.execute().tracePointState;
            if (!state.insideProc) {
                errorProfile.enter();
                throw new RaiseException(getContext(), coreExceptions().runtimeError("access from outside", this));
            }
            return state;
        }

    }

    @CoreMethod(names = "event")
    public abstract static class EventNode extends TracePointCoreNode {
        @Specialization
        protected RubySymbol event(RubyTracePoint tracePoint) {
            return getTracePointState().event;
        }
    }

    @CoreMethod(names = "path")
    public abstract static class PathNode extends TracePointCoreNode {
        @Specialization
        protected RubyString path(RubyTracePoint tracePoint) {
            return getTracePointState().path;
        }
    }

    @CoreMethod(names = "lineno")
    public abstract static class LineNode extends TracePointCoreNode {
        @Specialization
        protected int line(RubyTracePoint tracePoint) {
            return getTracePointState().line;
        }
    }

    @CoreMethod(names = "binding")
    public abstract static class BindingNode extends TracePointCoreNode {
        @Specialization
        protected RubyBinding binding(RubyTracePoint tracePoint) {
            return getTracePointState().binding;
        }
    }

    @CoreMethod(names = "method_id")
    public abstract static class MethodIDNode extends TracePointCoreNode {
        @Specialization
        protected RubyString methodId(RubyTracePoint tracePoint,
                @Cached MakeStringNode makeStringNode) {
            final RubyBinding binding = getTracePointState().binding;
            final InternalMethod method = RubyArguments.getMethod(binding.getFrame());
            return makeStringNode.executeMake(method.getName(), UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }
    }

    @CoreMethod(names = "self")
    public abstract static class SelfNode extends TracePointCoreNode {
        @Specialization
        protected Object self(RubyTracePoint tracePoint) {
            final RubyBinding binding = getTracePointState().binding;
            return RubyArguments.getSelf(binding.getFrame());
        }
    }

}
