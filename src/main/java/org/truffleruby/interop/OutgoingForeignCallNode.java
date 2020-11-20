/*
 * Copyright (c) 2014, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.interop;

import java.util.Arrays;

import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.core.array.ArrayUtils;
import org.truffleruby.core.cast.NameToJavaStringNode;
import org.truffleruby.core.cast.ToSymbolNode;
import org.truffleruby.language.RubyBaseNode;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.dispatch.DispatchNode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

@GenerateUncached
/* This node is called either with cached name from CachedForeignDispatchNode or from DSLUncachedDispatchNode where it
 * uses uncached version of this node. */
public abstract class OutgoingForeignCallNode extends RubyBaseNode {

    // TODO (pitr-ch 01-Apr-2019): support to_int special form with new interop, consider others
    // TODO (pitr-ch 16-Sep-2019): merge into a dispatch node when it is migrated to DSL
    // FIXME (pitr 13-Sep-2019): @Shared("arity") does not work, It thinks "The cache initializer does not match"

    public abstract Object executeCall(Object receiver, String name, Object[] args);

    protected final static String INDEX_READ = "[]";
    protected final static String INDEX_WRITE = "[]=";
    protected final static String CALL = "call";
    protected final static String NEW = "new";
    protected final static String TO_A = "to_a";
    protected final static String TO_ARY = "to_ary";
    protected final static String TO_I = "to_i";
    protected final static String TO_F = "to_f";
    protected final static String RESPOND_TO = "respond_to?";
    protected final static String SEND = "__send__";
    protected final static String NIL = "nil?";
    protected final static String EQUAL = "equal?";
    protected final static String DELETE = "delete";
    protected final static String SIZE = "size";
    protected final static String KEYS = "keys";
    protected final static String CLASS = "class";
    protected final static String INSPECT = "inspect";
    protected final static String TO_S = "to_s";
    protected final static String TO_STR = "to_str";
    protected final static String IS_A = "is_a?";
    protected final static String KIND_OF = "kind_of?";
    protected final static String OBJECT_ID = "object_id";
    protected final static String ID = "__id__";

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(OBJECT_ID) || cachedName.equals(ID)",
                    "args.length == 0" },
            limit = "1")
    protected int objectId(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedLibrary("receiver") InteropLibrary interop,
            @Cached ConditionProfile hasIdentityProfile,
            @Cached TranslateInteropExceptionNode translateInteropException) {
        if (hasIdentityProfile.profile(interop.hasIdentity(receiver))) {
            try {
                return interop.identityHashCode(receiver);
            } catch (UnsupportedMessageException e) {
                throw translateInteropException.execute(e);
            }
        } else {
            return System.identityHashCode(receiver);
        }
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(INDEX_READ)",
                    "args.length == 1",
                    "isBasicInteger(first(args))" },
            limit = "1")
    protected Object readArrayElement(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.ReadArrayElementNode readNode) {
        return readNode.execute(receiver, args[0]);
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(INDEX_READ)",
                    "args.length == 1",
                    "isRubySymbolOrString(first(args))" },
            limit = "1")
    protected Object readMember(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.ReadMemberNode readNode) {
        return readNode.execute(receiver, args[0]);
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(INDEX_WRITE)",
                    "args.length == 2",
                    "isBasicInteger(first(args))" },
            limit = "1")
    protected Object writeArrayElement(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.WriteArrayElementNode writeNode) {

        return writeNode.execute(receiver, args[0], args[1]);
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(INDEX_WRITE)",
                    "args.length == 2",
                    "isRubySymbolOrString(first(args))" },
            limit = "1")
    protected Object writeMember(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.WriteMemberNode writeNode) {
        return writeNode.execute(receiver, args[0], args[1]);
    }

    protected static Object first(Object[] args) {
        return args[0];
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(CALL)" }, limit = "1")
    protected Object call(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.ExecuteNode executeNode) {
        return executeNode.execute(receiver, args);
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(NEW)" }, limit = "1")
    protected Object newOutgoing(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.NewNode newNode) {
        return newNode.execute(receiver, args);
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(SEND)", "args.length >= 1" }, limit = "1")
    protected Object sendOutgoing(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached @Shared("dispatch") DispatchNode dispatchNode,
            @Cached NameToJavaStringNode nameToJavaString) {

        final Object sendName = args[0];
        final Object[] sendArgs = Arrays.copyOfRange(args, 1, args.length);

        return dispatchNode.dispatch(null, receiver, nameToJavaString.execute(sendName), null, sendArgs);
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(NIL)", "args.length == 0" }, limit = "1")
    protected Object nil(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.NullNode nullNode) {
        return nullNode.execute(receiver);
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(EQUAL)", "args.length == 1" }, limit = "1")
    protected boolean isEqual(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedLibrary("receiver") InteropLibrary lhsInterop,
            @CachedLibrary("first(args)") InteropLibrary rhsInterop) {
        return lhsInterop.isIdentical(receiver, first(args), rhsInterop);
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(DELETE)",
                    "args.length == 1",
                    "isBasicInteger(first(args))" },
            limit = "1")
    protected Object deleteArrayElement(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedContext(RubyLanguage.class) RubyContext context,
            @Cached @Shared("dispatch") DispatchNode dispatchNode) {

        return dispatchNode
                .call(context.getCoreLibrary().truffleInteropModule, "remove_array_element", receiver, args[0]);
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "cachedName.equals(DELETE)",
                    "args.length == 1",
                    "isRubySymbolOrString(first(args))" },
            limit = "1")
    protected Object deleteMember(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedContext(RubyLanguage.class) RubyContext context,
            @Cached @Shared("dispatch") DispatchNode dispatchNode) {

        return dispatchNode.call(context.getCoreLibrary().truffleInteropModule, "remove_member", receiver, args[0]);
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(TO_F)", "args.length == 0" }, limit = "1")
    protected double toF(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedLibrary("receiver") InteropLibrary interop,
            @Cached TranslateInteropExceptionNode translateInteropException,
            @CachedContext(RubyLanguage.class) RubyContext context,
            @Cached BranchProfile errorProfile) {
        try {
            if (interop.fitsInDouble(receiver)) {
                return interop.asDouble(receiver);
            } else if (interop.fitsInLong(receiver)) {
                return /* (double) */ interop.asLong(receiver);
            } else {
                errorProfile.enter();
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().typeError("can't convert foreign object to Float", this));
            }
        } catch (UnsupportedMessageException e) {
            throw translateInteropException.execute(e);
        }
    }

    @Specialization(guards = { "name == cachedName", "cachedName.equals(TO_I)", "args.length == 0" }, limit = "1")
    protected Object toI(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedLibrary("receiver") InteropLibrary interop,
            @Cached TranslateInteropExceptionNode translateInteropException,
            @CachedContext(RubyLanguage.class) RubyContext context,
            @Cached BranchProfile errorProfile) {
        try {
            if (interop.fitsInInt(receiver)) {
                return interop.asInt(receiver);
            } else if (interop.fitsInLong(receiver)) {
                return interop.asLong(receiver);
            } else {
                errorProfile.enter();
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().typeError("can't convert foreign object to Integer", this));
            }
        } catch (UnsupportedMessageException e) {
            throw translateInteropException.execute(e);
        }
    }

    protected static boolean canHaveBadArguments(String cachedName) {
        return cachedName.equals(INDEX_READ) || cachedName.equals(INDEX_WRITE) || cachedName.equals(SEND) ||
                cachedName.equals(NIL) || cachedName.equals(EQUAL) || cachedName.equals(OBJECT_ID) ||
                cachedName.equals(ID);
    }

    protected static boolean badArity(Object[] args, int cachedArity, String cachedName) {
        return cachedName.equals(SEND) ? args.length < cachedArity : args.length != cachedArity;
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "canHaveBadArguments(cachedName)",
                    "badArity(args, cachedArity, cachedName)" },
            limit = "1" /* the name is constant */)
    protected Object withBadArguments(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached(value = "expectedArity(cachedName)", allowUncached = true) int cachedArity,
            @CachedContext(RubyLanguage.class) RubyContext context) {
        throw new RaiseException(
                context,
                context.getCoreExceptions().argumentError(args.length, cachedArity, this));
    }

    @TruffleBoundary
    protected static int expectedArity(String name) {
        switch (name) {
            case TO_A:
            case TO_ARY:
            case SIZE:
            case KEYS:
            case INSPECT:
            case CLASS:
            case TO_F:
            case TO_I:
            case TO_S:
            case TO_STR:
            case NIL:
            case OBJECT_ID:
            case ID:
                return 0;
            case RESPOND_TO:
            case DELETE:
            case IS_A:
            case KIND_OF:
            case INDEX_READ:
            case EQUAL:
            case SEND:
                return 1;
            case INDEX_WRITE:
                return 2;
            default:
                throw new IllegalStateException();
        }
    }

    @TruffleBoundary
    protected static String specialToInteropMethod(String name) {
        switch (name) {
            case TO_A:
            case TO_ARY:
                return "to_array";
            case SIZE:
                return "array_size";
            case KEYS:
                return "members";
            case RESPOND_TO:
                return "foreign_respond_to?";
            case INSPECT:
                return "foreign_inspect";
            case CLASS:
                return "foreign_class";
            case TO_S:
                return "foreign_to_s";
            case TO_STR:
                return "foreign_to_str";
            case IS_A:
            case KIND_OF:
                return "foreign_is_a?";
            default:
                return null;
        }
    }

    protected static boolean isRedirectToTruffleInterop(String cachedName) {
        return specialToInteropMethod(cachedName) != null;
    }

    @Specialization(
            guards = { "isRedirectToTruffleInterop(cachedName)", "args.length == cachedArity" },
            limit = "1" /* the name is constant */)
    protected Object redirectToTruffleInterop(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached(value = "expectedArity(cachedName)", allowUncached = true) int cachedArity,
            @Cached(value = "specialToInteropMethod(cachedName)", allowUncached = true) String interopMethodName,
            @CachedContext(RubyLanguage.class) RubyContext context,
            @Cached @Shared("dispatch") DispatchNode dispatchNode,
            @Cached ConditionProfile errorProfile) {

        if (errorProfile.profile(args.length == cachedArity)) {
            final Object[] arguments = ArrayUtils.unshift(args, receiver);
            return dispatchNode.call(
                    context.getCoreLibrary().truffleInteropModule,
                    interopMethodName,
                    arguments);
        } else {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().argumentError(args.length, cachedArity, this));
        }
    }

    @Specialization(guards = { "name == cachedName", "isOperatorMethod(cachedName)" }, limit = "1")
    protected Object operator(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached PrimitiveConversionForOperatorAndReDispatchOutgoingNode node) {
        return node.executeCall(receiver, name, args);
    }

    @Specialization(guards = {
            "name == cachedName",
            "!isOperatorMethod(cachedName)",
            "isAssignmentMethod(cachedName)",
            "args.length != 1"
    }, limit = "1")
    protected Object assignmentBadArgs(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @CachedContext(RubyLanguage.class) RubyContext context) {
        throw new RaiseException(
                context,
                context.getCoreExceptions().argumentError(args.length, 1, this));
    }

    @Specialization(guards = {
            "name == cachedName",
            "!isOperatorMethod(cachedName)",
            "isAssignmentMethod(cachedName)",
            "args.length == 1"
    }, limit = "1")
    protected Object assignment(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached(value = "getPropertyFromName(name)", allowUncached = true) String propertyName,
            @CachedLibrary("receiver") InteropLibrary receivers,
            @Cached TranslateInteropExceptionNode translateInteropException) {
        try {
            receivers.writeMember(receiver, propertyName, args[0]);
        } catch (InteropException e) {
            throw translateInteropException.execute(e);
        }
        return args[0];
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "!cachedName.equals(INDEX_READ)",
                    "!cachedName.equals(INDEX_WRITE)",
                    "!cachedName.equals(CALL)",
                    "!cachedName.equals(NEW)",
                    "!cachedName.equals(SEND)",
                    "!cachedName.equals(NIL)",
                    "!cachedName.equals(EQUAL)",
                    "!isRedirectToTruffleInterop(cachedName)",
                    "!isOperatorMethod(cachedName)",
                    "!isAssignmentMethod(cachedName)",
                    "args.length == 0"
            },
            limit = "1")
    protected Object readOrInvoke(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached ToSymbolNode toSymbolNode,
            @Cached InteropNodes.InvokeNode invokeNode,
            @Cached InteropNodes.ReadMemberNode readNode,
            @Cached ConditionProfile invocable,
            @CachedLibrary("receiver") InteropLibrary receivers) {
        if (invocable.profile(receivers.isMemberInvocable(receiver, name))) {
            return invokeNode.execute(receiver, name, args);
        } else {
            return readNode.execute(receiver, toSymbolNode.execute(cachedName));
        }
    }

    @Specialization(
            guards = {
                    "name == cachedName",
                    "!cachedName.equals(INDEX_READ)",
                    "!cachedName.equals(INDEX_WRITE)",
                    "!cachedName.equals(CALL)",
                    "!cachedName.equals(NEW)",
                    "!cachedName.equals(SEND)",
                    "!cachedName.equals(NIL)",
                    "!cachedName.equals(EQUAL)",
                    "!isRedirectToTruffleInterop(cachedName)",
                    "!isOperatorMethod(cachedName)",
                    "!isAssignmentMethod(cachedName)",
                    "args.length != 0"
            },
            limit = "1")
    protected Object notOperatorOrAssignment(Object receiver, String name, Object[] args,
            @Cached(value = "name", allowUncached = true) @Shared("name") String cachedName,
            @Cached InteropNodes.InvokeNode invokeNode) {
        return invokeNode.execute(receiver, name, args);
    }

    @TruffleBoundary
    protected static boolean isOperatorMethod(String name) {
        return !name.isEmpty() && !Character.isLetter(name.charAt(0));
    }

    @TruffleBoundary
    protected static boolean isAssignmentMethod(String name) {
        return !name.isEmpty() && !name.equals(INDEX_WRITE) && '=' == name.charAt(name.length() - 1);
    }

    protected static String getPropertyFromName(String name) {
        return name.substring(0, name.length() - 1);
    }

    @GenerateUncached
    protected abstract static class PrimitiveConversionForOperatorAndReDispatchOutgoingNode
            extends
            RubyBaseNode {

        protected int getCacheLimit() {
            return RubyLanguage.getCurrentContext().getOptions().METHOD_LOOKUP_CACHE;
        }

        protected abstract Object executeCall(Object receiver, String name, Object[] args);

        @Specialization(guards = "receivers.isBoolean(receiver)", limit = "getCacheLimit()")
        protected Object callBoolean(Object receiver, String name, Object[] args,
                @CachedLibrary("receiver") InteropLibrary receivers,
                @Cached TranslateInteropExceptionNode translateInteropException,
                @Cached DispatchNode dispatch) {
            try {
                return dispatch.call(receivers.asBoolean(receiver), name, args);
            } catch (InteropException e) {
                throw translateInteropException.execute(e);
            }
        }

        @Specialization(guards = "receivers.isString(receiver)", limit = "getCacheLimit()")
        protected Object callString(Object receiver, String name, Object[] args,
                @Cached ForeignToRubyNode foreignToRubyNode,
                @CachedLibrary("receiver") InteropLibrary receivers,
                @Cached TranslateInteropExceptionNode translateInteropException,
                @Cached DispatchNode dispatch) {
            try {
                Object rubyString = foreignToRubyNode.executeConvert(receivers.asString(receiver));
                return dispatch.call(rubyString, name, args);
            } catch (InteropException e) {
                throw translateInteropException.execute(e);
            }
        }

        @Specialization(
                guards = { "receivers.isNumber(receiver)", "receivers.fitsInInt(receiver)" },
                limit = "getCacheLimit()")
        protected Object callInt(Object receiver, String name, Object[] args,
                @CachedLibrary("receiver") InteropLibrary receivers,
                @Cached TranslateInteropExceptionNode translateInteropException,
                @Cached DispatchNode dispatch) {
            try {
                return dispatch.call(receivers.asInt(receiver), name, args);
            } catch (InteropException e) {
                throw translateInteropException.execute(e);
            }
        }

        @Specialization(
                guards = {
                        "receivers.isNumber(receiver)",
                        "!receivers.fitsInInt(receiver)",
                        "receivers.fitsInLong(receiver)" },
                limit = "getCacheLimit()")
        protected Object callLong(Object receiver, String name, Object[] args,
                @CachedLibrary("receiver") InteropLibrary receivers,
                @Cached TranslateInteropExceptionNode translateInteropException,
                @Cached DispatchNode dispatch) {
            try {
                return dispatch.call(receivers.asLong(receiver), name, args);
            } catch (InteropException e) {
                throw translateInteropException.execute(e);
            }
        }

        @Specialization(
                guards = {
                        "receivers.isNumber(receiver)",
                        "!receivers.fitsInLong(receiver)",
                        "receivers.fitsInDouble(receiver)" },
                limit = "getCacheLimit()")
        protected Object callDouble(Object receiver, String name, Object[] args,
                @CachedLibrary("receiver") InteropLibrary receivers,
                @Cached TranslateInteropExceptionNode translateInteropException,
                @Cached DispatchNode dispatch) {
            try {
                return dispatch.call(receivers.asDouble(receiver), name, args);
            } catch (InteropException e) {
                throw translateInteropException.execute(e);
            }
        }

        @Specialization(
                guards = {
                        "!receivers.isBoolean(receiver)",
                        "!receivers.isString(receiver)",
                        "!receivers.isNumber(receiver)" },
                limit = "getCacheLimit()")
        protected Object call(Object receiver, String name, Object[] args,
                @CachedLibrary("receiver") InteropLibrary receivers,
                @Cached InteropNodes.InvokeNode invokeNode) {
            return invokeNode.execute(receiver, name, args);
        }

    }

}
