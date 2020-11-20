/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.debug;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.Layouts;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.SuppressFBWarnings;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreMethodNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.core.RubyHandle;
import org.truffleruby.core.array.ArrayGuards;
import org.truffleruby.core.array.ArrayHelpers;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.array.library.ArrayStoreLibrary;
import org.truffleruby.core.binding.BindingNodes;
import org.truffleruby.core.hash.RubyHash;
import org.truffleruby.core.method.RubyMethod;
import org.truffleruby.core.method.RubyUnboundMethod;
import org.truffleruby.core.numeric.BigIntegerOps;
import org.truffleruby.core.numeric.RubyBignum;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.extra.ffi.Pointer;
import org.truffleruby.interop.BoxedValue;
import org.truffleruby.interop.ToJavaStringNode;
import org.truffleruby.language.ImmutableRubyObject;
import org.truffleruby.language.NotProvided;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.arguments.RubyArguments;
import org.truffleruby.language.methods.DeclarationContext;
import org.truffleruby.language.methods.InternalMethod;
import org.truffleruby.language.objects.AllocateHelperNode;
import org.truffleruby.language.objects.shared.SharedObjects;
import org.truffleruby.language.yield.YieldNode;
import org.truffleruby.shared.TruffleRuby;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.utilities.TriState;

@CoreModule("Truffle::Debug")
public abstract class TruffleDebugNodes {

    @CoreMethod(names = "print", onSingleton = true, required = 1)
    public abstract static class DebugPrintNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object debugPrint(Object string) {
            System.err.println(string.toString());
            return nil;
        }

    }

    @CoreMethod(names = "break_handle", onSingleton = true, required = 2, needsBlock = true, lowerFixnum = 2)
    public abstract static class BreakNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyHandle setBreak(RubyString file, int line, RubyProc block,
                @CachedLanguage RubyLanguage language) {
            final String fileString = file.getJavaString();

            final SourceSectionFilter filter = SourceSectionFilter
                    .newBuilder()
                    .mimeTypeIs(TruffleRuby.MIME_TYPE)
                    .sourceIs(source -> source != null && getContext().getSourcePath(source).equals(fileString))
                    .lineIs(line)
                    .tagIs(StandardTags.StatementTag.class)
                    .build();

            final EventBinding<?> breakpoint = getContext().getInstrumenter().attachExecutionEventFactory(
                    filter,
                    eventContext -> new ExecutionEventNode() {

                        @Child private YieldNode yieldNode = YieldNode.create();

                        @Override
                        protected void onEnter(VirtualFrame frame) {
                            yieldNode.executeDispatch(
                                    block,
                                    BindingNodes.createBinding(
                                            getContext(),
                                            frame.materialize(),
                                            eventContext.getInstrumentedSourceSection()));
                        }

                    });

            final RubyHandle instance = new RubyHandle(
                    coreLibrary().handleClass,
                    RubyLanguage.handleShape,
                    breakpoint);
            allocateNode.trace(instance, this, language);
            return instance;
        }

    }

    @CoreMethod(names = "remove_handle", onSingleton = true, required = 1)
    public abstract static class RemoveNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object remove(RubyHandle handle) {
            EventBinding.class.cast(handle.object).dispose();
            return nil;
        }

    }

    @CoreMethod(names = "java_class_of", onSingleton = true, required = 1)
    public abstract static class JavaClassOfNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyString javaClassOf(Object value) {
            return makeStringNode
                    .executeMake(value.getClass().getSimpleName(), UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }

    }

    @CoreMethod(names = "print_backtrace", onSingleton = true)
    public abstract static class PrintBacktraceNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        protected Object printBacktrace() {
            getContext().getDefaultBacktraceFormatter().printBacktraceOnEnvStderr(this);
            return nil;
        }

    }

    @CoreMethod(
            names = "ast",
            onSingleton = true,
            optional = 1,
            needsBlock = true,
            argumentNames = { "method_or_proc", "block" })
    public abstract static class ASTNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object astMethod(RubyMethod method, NotProvided block) {
            ast(method.method);
            return nil;
        }

        @TruffleBoundary
        @Specialization
        protected Object astUnboundMethod(RubyUnboundMethod method, NotProvided block) {
            ast(method.method);
            return nil;
        }

        @TruffleBoundary
        @Specialization
        protected Object astProc(RubyProc proc, NotProvided block) {
            ast(proc.callTargetForType);
            return nil;
        }

        @TruffleBoundary
        @Specialization
        protected Object astBlock(NotProvided proc, RubyProc block) {
            ast(block.callTargetForType);
            return nil;
        }

        private Object ast(InternalMethod method) {
            return ast(method.getCallTarget());
        }

        private Object ast(RootCallTarget rootCallTarget) {
            return ast(rootCallTarget.getRootNode());
        }

        private Object ast(Node node) {
            if (node == null) {
                return nil;
            }

            final List<Object> array = new ArrayList<>();

            array.add(getSymbol(node.getClass().getSimpleName()));

            for (Node child : node.getChildren()) {
                array.add(ast(child));
            }

            return createArray(array.toArray());
        }

    }

    @CoreMethod(
            names = "print_ast",
            onSingleton = true,
            optional = 1,
            needsBlock = true,
            argumentNames = { "method_or_proc", "block" })
    public abstract static class PrintASTNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object astMethod(RubyMethod method, NotProvided block) {
            printAst(method.method);
            return nil;
        }

        @TruffleBoundary
        @Specialization
        protected Object astUnboundMethod(RubyUnboundMethod method, NotProvided block) {
            printAst(method.method);
            return nil;
        }

        @TruffleBoundary
        @Specialization
        protected Object astProc(RubyProc proc, NotProvided block) {
            printAst(proc.callTargetForType);
            return nil;
        }

        @TruffleBoundary
        @Specialization
        protected Object astBlock(NotProvided proc, RubyProc block) {
            printAst(block.callTargetForType);
            return nil;
        }

        public static void printAst(InternalMethod method) {
            NodeUtil.printCompactTree(System.err, method.getCallTarget().getRootNode());
        }

        private void printAst(RootCallTarget callTarget) {
            NodeUtil.printCompactTree(System.err, callTarget.getRootNode());
        }

    }

    @CoreMethod(names = "object_type_of", onSingleton = true, required = 1)
    public abstract static class ObjectTypeOfNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubySymbol objectTypeOf(RubyDynamicObject value) {
            return getSymbol(value.getShape().getObjectType().getClass().getSimpleName());
        }
    }

    @CoreMethod(names = "shape", onSingleton = true, required = 1)
    public abstract static class ShapeNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyString shape(RubyDynamicObject object) {
            return makeStringNode
                    .executeMake(object.getShape().toString(), UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }

    }

    @CoreMethod(names = "array_storage", onSingleton = true, required = 1)
    public abstract static class ArrayStorageNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyString arrayStorage(RubyArray array) {
            String storage = ArrayStoreLibrary.getFactory().getUncached().toString(array.store);
            return makeStringNode.executeMake(storage, USASCIIEncoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }

    }

    @CoreMethod(names = "array_capacity", onSingleton = true, required = 1)
    @ImportStatic(ArrayGuards.class)
    public abstract static class ArrayCapacityNode extends CoreMethodArrayArgumentsNode {

        @Specialization(limit = "storageStrategyLimit()")
        protected long arrayStorage(RubyArray array,
                @CachedLibrary("array.store") ArrayStoreLibrary stores) {
            return stores.capacity(array.store);
        }

    }

    @CoreMethod(names = "hash_storage", onSingleton = true, required = 1)
    public abstract static class HashStorageNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyString hashStorage(RubyHash hash) {
            Object store = hash.store;
            String storage = store == null ? "null" : store.getClass().toString();
            return makeStringNode.executeMake(storage, USASCIIEncoding.INSTANCE, CodeRange.CR_7BIT);
        }

    }

    @CoreMethod(names = "shared?", onSingleton = true, required = 1)
    @ImportStatic(SharedObjects.class)
    public abstract static class IsSharedNode extends CoreMethodArrayArgumentsNode {

        @Specialization(
                guards = "object.getShape() == cachedShape",
                assumptions = "cachedShape.getValidAssumption()",
                limit = "getCacheLimit()")
        protected boolean isSharedCached(RubyDynamicObject object,
                @Cached("object.getShape()") Shape cachedShape,
                @Cached("cachedShape.isShared()") boolean shared) {
            return shared;
        }

        @Specialization(replaces = "isSharedCached")
        protected boolean isShared(RubyDynamicObject object) {
            return SharedObjects.isShared(object);
        }

        @Specialization
        protected boolean isSharedImmutable(ImmutableRubyObject object) {
            return true;
        }

        protected int getCacheLimit() {
            return getContext().getOptions().INSTANCE_VARIABLE_CACHE;
        }

    }

    @CoreMethod(names = "log_config", onSingleton = true, required = 1)
    public abstract static class LogConfigNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object logConfig(Object value,
                @Cached ToJavaStringNode toJavaStringNode) {
            config(toJavaStringNode.executeToJavaString(value));
            return nil;
        }

        @TruffleBoundary
        static void config(String message) {
            RubyLanguage.LOGGER.config(message);
        }

    }

    @CoreMethod(names = "log_warning", onSingleton = true, required = 1)
    public abstract static class LogWarningNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object logWarning(Object value,
                @Cached ToJavaStringNode toJavaStringNode) {
            warning(toJavaStringNode.executeToJavaString(value));
            return nil;
        }

        @TruffleBoundary
        static void warning(String message) {
            RubyLanguage.LOGGER.warning(message);
        }

    }

    @CoreMethod(names = "throw_java_exception", onSingleton = true, required = 1)
    public abstract static class ThrowJavaExceptionNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object throwJavaException(RubyString message) {
            callingMethod(message.getJavaString());
            return nil;
        }

        // These two named methods makes it easy to test that the backtrace for a Java exception is what we expect

        private static void callingMethod(String message) {
            throwingMethod(message);
        }

        private static void throwingMethod(String message) {
            throw new RuntimeException(message);
        }

    }

    @CoreMethod(names = "throw_java_exception_with_cause", onSingleton = true, required = 1)
    public abstract static class ThrowJavaExceptionWithCauseNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object throwJavaExceptionWithCause(RubyString message) {
            throw new RuntimeException(
                    message.getJavaString(),
                    new RuntimeException("cause 1", new RuntimeException("cause 2")));
        }

    }

    @CoreMethod(names = "throw_assertion_error", onSingleton = true, required = 1)
    public abstract static class ThrowAssertionErrorNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object throwAssertionError(RubyString message) {
            throw new AssertionError(message.getJavaString());
        }

    }

    @CoreMethod(names = "assert", onSingleton = true, required = 1)
    public abstract static class AssertNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object doAssert(boolean condition) {
            assert condition;
            return nil;
        }

    }

    @CoreMethod(names = "java_class", onSingleton = true)
    public abstract static class JavaClassNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object javaObject() {
            return getContext().getEnv().asGuestValue(BigInteger.class);
        }

    }

    @CoreMethod(names = "java_object", onSingleton = true)
    public abstract static class JavaObjectNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object javaObject() {
            return getContext().getEnv().asGuestValue(new BigInteger("14"));
        }

    }

    @CoreMethod(names = "java_null", onSingleton = true)
    public abstract static class JavaNullNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object javaNull() {
            return getContext().getEnv().asGuestValue(null);
        }

    }

    @CoreMethod(names = "foreign_null", onSingleton = true)
    public abstract static class ForeignNullNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignNull implements TruffleObject {

            @ExportMessage
            protected boolean isNull() {
                return true;
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign null]";
            }
        }

        @TruffleBoundary
        @Specialization
        protected Object foreignNull() {
            return new ForeignNull();
        }

    }

    @CoreMethod(names = "foreign_pointer", required = 1, onSingleton = true)
    public abstract static class ForeignPointerNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignPointer implements TruffleObject {

            private final long address;

            public ForeignPointer(long address) {
                this.address = address;
            }

            @ExportMessage
            protected boolean isPointer() {
                return true;
            }

            @ExportMessage
            protected long asPointer() {
                return address;
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign pointer]";
            }
        }

        @TruffleBoundary
        @Specialization
        protected Object foreignPointer(long address) {
            return new ForeignPointer(address);
        }

    }

    @CoreMethod(names = "foreign_object", onSingleton = true)
    public abstract static class ForeignObjectNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignObject implements TruffleObject {
            @TruffleBoundary
            @ExportMessage
            protected TriState isIdenticalOrUndefined(Object other) {
                return other instanceof ForeignObject ? TriState.valueOf(this == other) : TriState.UNDEFINED;
            }

            @TruffleBoundary
            @ExportMessage
            protected int identityHashCode() {
                return System.identityHashCode(this);
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign object]";
            }
        }

        @TruffleBoundary
        @Specialization
        protected Object foreignObject() {
            return new ForeignObject();
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CoreMethod(names = "foreign_object_from_map", required = 1, onSingleton = true)
    public abstract static class ForeignObjectFromMapNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignObjectFromMap implements TruffleObject {

            private final Map map;

            public ForeignObjectFromMap(Map map) {
                this.map = map;
            }

            @ExportMessage
            protected boolean hasMembers() {
                return true;
            }

            @ExportMessage
            @TruffleBoundary
            protected Object getMembers(boolean includeInternal,
                    @CachedContext(RubyLanguage.class) RubyContext context) {
                return context.getEnv().asGuestValue(map.keySet().toArray());
            }

            @TruffleBoundary
            @ExportMessage
            protected boolean isMemberReadable(String member) {
                return map.containsKey(member);
            }

            @TruffleBoundary
            @ExportMessage
            protected Object readMember(String key) throws UnknownIdentifierException {
                final Object value = map.get(key);
                if (value == null) {
                    throw UnknownIdentifierException.create(key);
                }
                return value;
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign object with members]";
            }
        }

        @TruffleBoundary
        @Specialization
        protected Object foreignObjectFromMap(Object map) {
            return new ForeignObjectFromMap((Map) getContext().getEnv().asHostObject(map));
        }

    }

    @CoreMethod(names = "foreign_array_from_java", required = 1, onSingleton = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class ForeignArrayFromJavaNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignArrayFromJava implements TruffleObject {

            private final Object[] array;

            public ForeignArrayFromJava(Object[] array) {
                this.array = array;
            }

            @ExportMessage
            protected boolean hasArrayElements() {
                return true;
            }

            @ExportMessage(name = "isArrayElementReadable")
            @ExportMessage(name = "isArrayElementModifiable")
            protected boolean isArrayElement(long index) {
                return 0 >= index && index < array.length;
            }

            @TruffleBoundary
            @ExportMessage
            protected Object readArrayElement(long index) throws InvalidArrayIndexException {
                try {
                    return array[(int) index];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw InvalidArrayIndexException.create(index);
                }
            }

            @TruffleBoundary
            @ExportMessage
            protected void writeArrayElement(long index, Object value) throws InvalidArrayIndexException {
                try {
                    array[(int) index] = value;
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw InvalidArrayIndexException.create(index);
                }
            }

            @ExportMessage
            protected final boolean isArrayElementInsertable(long index) {
                return false;
            }

            @ExportMessage
            protected long getArraySize() {
                return array.length;
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign array]";
            }
        }

        @TruffleBoundary
        @Specialization(limit = "storageStrategyLimit()")
        protected Object foreignArrayFromJava(Object array,
                @CachedLibrary("hostObject(array)") ArrayStoreLibrary hostObjects) {
            final Object hostObject = hostObject(array);
            final int size = hostObjects.capacity(hostObject);
            final Object[] boxedArray = hostObjects.boxedCopyOfRange(hostObject, 0, size);
            return new ForeignArrayFromJava(boxedArray);
        }

        protected Object hostObject(Object array) {
            return getContext().getEnv().asHostObject(array);
        }
    }

    @CoreMethod(names = "foreign_pointer_array_from_java", required = 1, onSingleton = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class ForeignPointerArrayFromJavaNode extends ForeignArrayFromJavaNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignPointerArrayFromJava extends ForeignArrayFromJava {

            public ForeignPointerArrayFromJava(Object[] array) {
                super(array);
            }

            @ExportMessage
            protected boolean isPointer() {
                return true;
            }

            @ExportMessage
            protected long asPointer() {
                return 0; // shouldn't be used
            }

            @Override
            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign pointer array]";
            }
        }

        @Override
        @TruffleBoundary
        @Specialization
        protected Object foreignArrayFromJava(Object array,
                @CachedLibrary(limit = "storageStrategyLimit()") ArrayStoreLibrary stores) {
            final Object hostObject = getContext().getEnv().asHostObject(array);
            final int size = stores.capacity(hostObject);
            return new ForeignPointerArrayFromJava(stores.boxedCopyOfRange(hostObject, 0, size));
        }
    }

    @CoreMethod(names = "foreign_executable", required = 1, onSingleton = true)
    public abstract static class ForeignExecutableNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignExecutable implements TruffleObject {

            private final Object value;

            public ForeignExecutable(Object value) {
                this.value = value;
            }

            @ExportMessage
            protected boolean isExecutable() {
                return true;
            }

            @ExportMessage
            protected Object execute(Object... arguments) {
                return value;
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign executable]";
            }
        }

        @TruffleBoundary
        @Specialization
        protected Object foreignExecutable(Object value) {
            return new ForeignExecutable(value);
        }

    }

    @CoreMethod(names = "foreign_string", onSingleton = true, required = 1)
    public abstract static class ForeignStringNode extends CoreMethodArrayArgumentsNode {

        @ExportLibrary(InteropLibrary.class)
        public static class ForeignString implements TruffleObject {

            private final String string;

            public ForeignString(String string) {
                this.string = string;
            }

            @ExportMessage
            protected boolean isString() {
                return true;
            }

            @ExportMessage
            protected String asString() {
                return string;
            }

            @ExportMessage
            protected String toDisplayString(boolean allowSideEffects) {
                return "[foreign string]";
            }
        }

        @TruffleBoundary
        @Specialization
        protected Object foreignString(RubyString string) {
            return new ForeignString(string.getJavaString());
        }

    }

    @CoreMethod(names = "foreign_boxed_value", onSingleton = true, required = 1)
    public abstract static class ForeignBoxedNumberNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object foreignBoxedNumber(Object number) {
            return new BoxedValue(number);
        }

    }

    @CoreMethod(names = "float", onSingleton = true, required = 1)
    public abstract static class FloatNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected float foreignBoxedNumber(long value) {
            return value;
        }

        @Specialization
        protected float foreignBoxedNumber(RubyBignum value) {
            return (float) BigIntegerOps.doubleValue(value);
        }

        @Specialization
        protected float foreignBoxedNumber(double value) {
            return (float) value;
        }

    }

    @CoreMethod(names = "thread_info", onSingleton = true)
    public abstract static class ThreadInfoNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @Specialization
        protected RubyString threadInfo() {
            return makeStringNode.executeMake(getThreadDebugInfo(), UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }

        @TruffleBoundary
        private String getThreadDebugInfo() {
            return getContext().getThreadManager().getThreadDebugInfo() +
                    getContext().getSafepointManager().getSafepointDebugInfo() + "\n";
        }

    }

    @CoreMethod(names = "dead_block", onSingleton = true)
    public abstract static class DeadBlockNode extends CoreMethodArrayArgumentsNode {

        @SuppressFBWarnings("UW")
        @TruffleBoundary
        @Specialization
        protected Object deadBlock() {
            RubyLanguage.LOGGER.severe("Truffle::Debug.dead_block is being called - will lock up the interpreter");

            final Object monitor = new Object();

            synchronized (monitor) {
                while (true) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
            }
        }

    }

    @CoreMethod(names = "associated", onSingleton = true, required = 1)
    public abstract static class AssociatedNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyArray associated(RubyString string) {
            final DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
            Pointer[] associated = (Pointer[]) objectLibrary.getOrDefault(string, Layouts.ASSOCIATED_IDENTIFIER, null);

            if (associated == null) {
                associated = Pointer.EMPTY_ARRAY;
            }

            final long[] associatedValues = new long[associated.length];

            for (int n = 0; n < associated.length; n++) {
                associatedValues[n] = associated[n].getAddress();
            }

            return ArrayHelpers.createArray(getContext(), associatedValues);
        }
    }

    @CoreMethod(names = "drain_finalization_queue", onSingleton = true)
    public abstract static class DrainFinalizationQueueNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object drainFinalizationQueue() {
            getContext().getFinalizationService().drainFinalizationQueue();
            return nil;
        }

    }

    @Primitive(name = "frame_declaration_context_to_string")
    public abstract static class FrameDeclarationContextToStringNode extends PrimitiveArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @Specialization
        protected RubyString getDeclarationContextToString(VirtualFrame frame) {
            final DeclarationContext declarationContext = RubyArguments.getDeclarationContext(frame);
            return makeStringNode
                    .executeMake(declarationContext.toString(), UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }

    }

}
