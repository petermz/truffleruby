/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.binding;

import java.util.LinkedHashSet;
import java.util.Set;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.GenerateUncached;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.builtins.UnaryCoreMethodNode;
import org.truffleruby.core.array.ArrayHelpers;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.cast.NameToJavaStringNode;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes.MakeStringNode;
import org.truffleruby.language.Nil;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.RubySourceNode;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.arguments.ReadCallerFrameNode;
import org.truffleruby.language.arguments.RubyArguments;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.locals.FindDeclarationVariableNodes;
import org.truffleruby.language.locals.FindDeclarationVariableNodes.FindAndReadDeclarationVariableNode;
import org.truffleruby.language.locals.FindDeclarationVariableNodes.FrameSlotAndDepth;
import org.truffleruby.language.locals.WriteFrameSlotNode;
import org.truffleruby.language.locals.WriteFrameSlotNodeGen;
import org.truffleruby.parser.TranslatorEnvironment;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@CoreModule(value = "Binding", isClass = true)
public abstract class BindingNodes {

    /** Creates a Binding without a SourceSection, only for Binding used internally and not exposed to the user. */
    public static RubyBinding createBinding(RubyContext context, MaterializedFrame frame) {
        return createBinding(context, frame, null);
    }

    public static RubyBinding createBinding(RubyContext context, MaterializedFrame frame, SourceSection sourceSection) {
        return new RubyBinding(
                context.getCoreLibrary().bindingClass,
                RubyLanguage.bindingShape,
                frame,
                sourceSection);
    }

    @TruffleBoundary
    public static FrameDescriptor newFrameDescriptor() {
        return new FrameDescriptor(Nil.INSTANCE);
    }

    @TruffleBoundary
    public static FrameDescriptor newFrameDescriptor(String name) {
        final FrameDescriptor frameDescriptor = new FrameDescriptor(Nil.INSTANCE);
        assert name != null && !name.isEmpty();
        frameDescriptor.addFrameSlot(name);
        return frameDescriptor;
    }

    public static FrameDescriptor getFrameDescriptor(RubyBinding binding) {
        return binding.getFrame().getFrameDescriptor();
    }

    public static MaterializedFrame newFrame(RubyBinding binding, FrameDescriptor frameDescriptor) {
        final MaterializedFrame frame = binding.getFrame();
        final MaterializedFrame newFrame = newFrame(frame, frameDescriptor);
        binding.setFrame(newFrame);
        return newFrame;
    }

    public static MaterializedFrame newFrame(MaterializedFrame parent) {
        final FrameDescriptor descriptor = newFrameDescriptor();
        return newFrame(parent, descriptor);
    }

    public static MaterializedFrame newFrame(MaterializedFrame parent, FrameDescriptor descriptor) {
        return Truffle.getRuntime().createVirtualFrame(
                RubyArguments.pack(
                        parent,
                        null,
                        null,
                        RubyArguments.getMethod(parent),
                        RubyArguments.getDeclarationContext(parent),
                        null,
                        RubyArguments.getSelf(parent),
                        RubyArguments.getBlock(parent),
                        RubyArguments.getArguments(parent)),
                descriptor).materialize();
    }

    public static void insertAncestorFrame(RubyBinding binding, MaterializedFrame ancestorFrame) {
        MaterializedFrame frame = binding.getFrame();
        while (RubyArguments.getDeclarationFrame(frame) != null) {
            frame = RubyArguments.getDeclarationFrame(frame);
        }
        RubyArguments.setDeclarationFrame(frame, ancestorFrame);

        // We need to invalidate caches depending on the top frame, so create a new empty frame
        newFrame(binding, newFrameDescriptor());
    }

    public static boolean isHiddenVariable(Object name) {
        if (name instanceof String) {
            return isHiddenVariable((String) name);
        } else {
            return true;
        }
    }

    private static boolean isHiddenVariable(String name) {
        assert !name.isEmpty();
        return name.charAt(0) == '$' || // Frame-local global variable
                name.charAt(0) == TranslatorEnvironment.TEMP_PREFIX;
    }

    @CoreMethod(names = { "dup", "clone" })
    public abstract static class DupNode extends UnaryCoreMethodNode {
        @Specialization
        protected RubyBinding dup(RubyBinding binding) {
            return new RubyBinding(
                    coreLibrary().bindingClass,
                    RubyLanguage.bindingShape,
                    binding.getFrame(),
                    binding.sourceSection);
        }
    }

    @ImportStatic({ BindingNodes.class, FindDeclarationVariableNodes.class })
    @GenerateUncached
    @GenerateNodeFactory
    @CoreMethod(names = "local_variable_defined?", required = 1)
    @NodeChild(value = "binding", type = RubyNode.class)
    @NodeChild(value = "name", type = RubyNode.class)
    public abstract static class LocalVariableDefinedNode extends RubySourceNode {

        public static LocalVariableDefinedNode create() {
            return BindingNodesFactory.LocalVariableDefinedNodeFactory.create(null, null);
        }

        public abstract boolean execute(RubyBinding binding, String name);

        @CreateCast("name")
        protected RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNode.create(name);
        }

        @Specialization(
                guards = {
                        "name == cachedName",
                        "!isHiddenVariable(cachedName)",
                        "getFrameDescriptor(binding) == descriptor" },
                limit = "getCacheLimit()")
        protected boolean localVariableDefinedCached(RubyBinding binding, String name,
                @Cached("name") String cachedName,
                @Cached("getFrameDescriptor(binding)") FrameDescriptor descriptor,
                @Cached("findFrameSlotOrNull(name, binding.getFrame())") FrameSlotAndDepth cachedFrameSlot) {
            return cachedFrameSlot != null;
        }

        @TruffleBoundary
        @Specialization(guards = "!isHiddenVariable(name)", replaces = "localVariableDefinedCached")
        protected boolean localVariableDefinedUncached(RubyBinding binding, String name) {
            return FindDeclarationVariableNodes.findFrameSlotOrNull(name, binding.getFrame()) != null;
        }

        @TruffleBoundary
        @Specialization(guards = "isHiddenVariable(name)")
        protected Object localVariableDefinedLastLine(RubyBinding binding, String name,
                @CachedContext(RubyLanguage.class) RubyContext context) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().nameError("Bad local variable name", binding, name, this));
        }

        protected int getCacheLimit() {
            return RubyLanguage.getCurrentContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }

    }

    @GenerateUncached
    @GenerateNodeFactory
    @NodeChild(value = "binding", type = RubyNode.class)
    @NodeChild(value = "name", type = RubyNode.class)
    @ImportStatic(BindingNodes.class)
    @CoreMethod(names = "local_variable_get", required = 1)
    public abstract static class LocalVariableGetNode extends RubySourceNode {

        public abstract Object execute(RubyBinding binding, String name);

        public static LocalVariableGetNode create() {
            return BindingNodesFactory.LocalVariableGetNodeFactory.create(null, null);
        }

        @CreateCast("name")
        protected RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNode.create(name);
        }

        @Specialization(guards = "!isHiddenVariable(name)")
        protected Object localVariableGet(RubyBinding binding, String name,
                @Cached FindAndReadDeclarationVariableNode readNode,
                @CachedContext(RubyLanguage.class) RubyContext context) {
            MaterializedFrame frame = binding.getFrame();
            Object result = readNode.execute(frame, name, null);
            if (result == null) {
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().nameErrorLocalVariableNotDefined(name, binding, this));
            }
            return result;
        }

        @TruffleBoundary
        @Specialization(guards = "isHiddenVariable(name)")
        protected Object localVariableGetLastLine(RubyBinding binding, String name,
                @CachedContext(RubyLanguage.class) RubyContext context) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().nameError("Bad local variable name", binding, name, this));
        }

        protected int getCacheLimit() {
            return RubyLanguage.getCurrentContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }

    }

    @ReportPolymorphism
    @GenerateUncached
    @GenerateNodeFactory
    @CoreMethod(names = "local_variable_set", required = 2)
    @NodeChild(value = "binding", type = RubyNode.class)
    @NodeChild(value = "name", type = RubyNode.class)
    @NodeChild(value = "value", type = RubyNode.class)
    @ImportStatic({ BindingNodes.class, FindDeclarationVariableNodes.class })
    public abstract static class LocalVariableSetNode extends RubySourceNode {

        public static LocalVariableSetNode create() {
            return BindingNodesFactory.LocalVariableSetNodeFactory.create(null, null, null);
        }

        public abstract Object execute(RubyBinding binding, String name, Object value);

        @CreateCast("name")
        protected RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNode.create(name);
        }

        @Specialization(
                guards = {
                        "name == cachedName",
                        "!isHiddenVariable(cachedName)",
                        "getFrameDescriptor(binding) == cachedFrameDescriptor",
                        "cachedFrameSlot != null" },
                assumptions = "cachedFrameDescriptor.getVersion()",
                limit = "getCacheLimit()")
        protected Object localVariableSetCached(RubyBinding binding, String name, Object value,
                @Cached("name") String cachedName,
                @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                @Cached("findFrameSlotOrNull(name, binding.getFrame())") FrameSlotAndDepth cachedFrameSlot,
                @Cached("createWriteNode(cachedFrameSlot)") WriteFrameSlotNode writeLocalVariableNode) {
            final MaterializedFrame frame = RubyArguments
                    .getDeclarationFrame(binding.getFrame(), cachedFrameSlot.depth);
            return writeLocalVariableNode.executeWrite(frame, value);
        }

        @Specialization(
                guards = {
                        "name == cachedName",
                        "!isHiddenVariable(cachedName)",
                        "getFrameDescriptor(binding) == cachedFrameDescriptor",
                        "cachedFrameSlot == null" },
                assumptions = "cachedFrameDescriptor.getVersion()",
                limit = "getCacheLimit()")
        protected Object localVariableSetNewCached(RubyBinding binding, String name, Object value,
                @Cached("name") String cachedName,
                @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                @Cached("findFrameSlotOrNull(name, binding.getFrame())") FrameSlotAndDepth cachedFrameSlot,
                @Cached("newFrameDescriptor(name)") FrameDescriptor newDescriptor,
                @Cached("findFrameSlot(name, newDescriptor)") FrameSlotAndDepth newFrameSlot,
                @Cached("createWriteNode(newFrameSlot)") WriteFrameSlotNode writeLocalVariableNode) {
            final MaterializedFrame frame = newFrame(binding, newDescriptor);
            return writeLocalVariableNode.executeWrite(frame, value);
        }

        @TruffleBoundary
        @Specialization(
                guards = "!isHiddenVariable(name)",
                replaces = { "localVariableSetCached", "localVariableSetNewCached" })
        protected Object localVariableSetUncached(RubyBinding binding, String name, Object value) {
            MaterializedFrame frame = binding.getFrame();
            final FrameSlotAndDepth frameSlot = FindDeclarationVariableNodes.findFrameSlotOrNull(name, frame);
            final FrameSlot slot;
            if (frameSlot != null) {
                frame = RubyArguments.getDeclarationFrame(frame, frameSlot.depth);
                slot = frameSlot.slot;
            } else {
                frame = newFrame(binding, newFrameDescriptor(name));
                slot = frame.getFrameDescriptor().findFrameSlot(name);
            }
            frame.setObject(slot, value);
            return value;
        }

        @TruffleBoundary
        @Specialization(guards = "isHiddenVariable(name)")
        protected Object localVariableSetLastLine(RubyBinding binding, String name, Object value,
                @CachedContext(RubyLanguage.class) RubyContext context) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().nameError("Bad local variable name", binding, name, this));
        }

        protected WriteFrameSlotNode createWriteNode(FrameSlotAndDepth frameSlot) {
            return WriteFrameSlotNodeGen.create(frameSlot.slot);
        }

        protected int getCacheLimit() {
            return RubyLanguage.getCurrentContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }
    }

    @Primitive(name = "local_variable_names")
    @ImportStatic(BindingNodes.class)
    public abstract static class LocalVariablesNode extends PrimitiveArrayArgumentsNode {

        @Specialization(
                guards = "getFrameDescriptor(binding) == cachedFrameDescriptor",
                assumptions = "cachedFrameDescriptor.getVersion()",
                limit = "getCacheLimit()")
        protected RubyArray localVariablesCached(RubyBinding binding,
                @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                @Cached("listLocalVariables(getContext(), binding.getFrame())") RubyArray names) {
            return names;
        }

        @Specialization(replaces = "localVariablesCached")
        protected RubyArray localVariables(RubyBinding binding) {
            return listLocalVariables(getContext(), binding.getFrame());
        }

        @TruffleBoundary
        public static RubyArray listLocalVariables(RubyContext context, MaterializedFrame frame) {
            final Set<Object> names = new LinkedHashSet<>();
            while (frame != null) {
                addNamesFromFrame(context, frame, names);

                frame = RubyArguments.getDeclarationFrame(frame);
            }
            return ArrayHelpers.createArray(context, names.toArray());
        }

        private static void addNamesFromFrame(RubyContext context, Frame frame, final Set<Object> names) {
            for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                if (!isHiddenVariable(slot.getIdentifier())) {
                    names.add(context.getSymbol((String) slot.getIdentifier()));
                }
            }
        }

        protected int getCacheLimit() {
            return getContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }
    }

    @CoreMethod(names = "receiver")
    public abstract static class ReceiverNode extends UnaryCoreMethodNode {

        @Specialization
        protected Object receiver(RubyBinding binding) {
            return RubyArguments.getSelf(binding.getFrame());
        }
    }

    // NOTE: Introduced in Ruby 2.6, but already useful for Binding#eval
    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        protected Object sourceLocation(RubyBinding binding,
                @Cached MakeStringNode makeStringNode) {
            final SourceSection sourceSection = binding.sourceSection;

            if (sourceSection == null) {
                return nil;
            } else {
                final RubyString file = makeStringNode.executeMake(
                        getContext().getSourcePath(sourceSection.getSource()),
                        UTF8Encoding.INSTANCE,
                        CodeRange.CR_UNKNOWN);
                return createArray(new Object[]{ file, sourceSection.getStartLine() });
            }
        }
    }

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @Specialization
        protected Object allocate(RubyClass rubyClass) {
            throw new RaiseException(getContext(), coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @Primitive(name = "caller_binding")
    public abstract static class CallerBindingNode extends PrimitiveArrayArgumentsNode {

        @Child ReadCallerFrameNode callerFrameNode = new ReadCallerFrameNode();

        @Specialization
        protected RubyBinding binding(VirtualFrame frame) {
            final MaterializedFrame callerFrame = callerFrameNode.execute(frame);

            return BindingNodes.createBinding(getContext(), callerFrame);
        }
    }
}
