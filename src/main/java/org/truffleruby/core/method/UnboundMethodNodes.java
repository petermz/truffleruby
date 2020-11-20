/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.method;

import com.oracle.truffle.api.dsl.CachedLanguage;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.RubyLanguage;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.UnaryCoreMethodNode;
import org.truffleruby.core.Hashing;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.module.MethodLookupResult;
import org.truffleruby.core.module.ModuleOperations;
import org.truffleruby.core.module.RubyModule;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.language.RubyGuards;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.arguments.ArgumentDescriptorUtils;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.methods.CanBindMethodToModuleNode;
import org.truffleruby.language.methods.InternalMethod;
import org.truffleruby.language.objects.AllocateHelperNode;
import org.truffleruby.language.objects.MetaClassNode;
import org.truffleruby.parser.ArgumentDescriptor;
import org.truffleruby.utils.Utils;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;

@CoreModule(value = "UnboundMethod", isClass = true)
public abstract class UnboundMethodNodes {

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected boolean equal(RubyUnboundMethod self, RubyUnboundMethod other) {
            return self.origin == other.origin &&
                    MethodNodes.areInternalMethodEqual(self.method, other.method);
        }

        @Specialization(guards = "!isRubyUnboundMethod(other)")
        protected boolean equal(RubyUnboundMethod self, Object other) {
            return false;
        }

    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected int arity(RubyUnboundMethod unboundMethod) {
            return unboundMethod.method.getArityNumber();
        }

    }

    @CoreMethod(names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();

        @Specialization
        protected RubyMethod bind(RubyUnboundMethod unboundMethod, Object object,
                @Cached MetaClassNode metaClassNode,
                @Cached CanBindMethodToModuleNode canBindMethodToModuleNode,
                @Cached BranchProfile errorProfile,
                @CachedLanguage RubyLanguage language) {
            final RubyClass objectMetaClass = metaClassNode.execute(object);

            if (!canBindMethodToModuleNode
                    .executeCanBindMethodToModule(unboundMethod.method, objectMetaClass)) {
                errorProfile.enter();
                final RubyModule declaringModule = unboundMethod.method.getDeclaringModule();
                if (RubyGuards.isSingletonClass(declaringModule)) {
                    throw new RaiseException(getContext(), coreExceptions().typeError(
                            "singleton method called for a different object",
                            this));
                } else {
                    throw new RaiseException(getContext(), coreExceptions().typeError(
                            Utils.concat(
                                    "bind argument must be an instance of ",
                                    declaringModule.fields.getName()),
                            this));
                }
            }
            final RubyMethod instance = new RubyMethod(
                    coreLibrary().methodClass,
                    RubyLanguage.methodShape,
                    object,
                    unboundMethod.method);
            allocateNode.trace(instance, this, language);
            return instance;
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected long hash(RubyUnboundMethod rubyMethod) {
            final InternalMethod method = rubyMethod.method;
            long h = getContext().getHashing(this).start(method.getDeclaringModule().hashCode());
            h = Hashing.update(h, rubyMethod.origin.hashCode());
            h = Hashing.update(h, MethodNodes.hashInternalMethod(method));
            return Hashing.end(h);
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubySymbol name(RubyUnboundMethod unboundMethod) {
            return getSymbol(unboundMethod.method.getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyModule origin(RubyUnboundMethod unboundMethod) {
            return unboundMethod.origin;
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyModule owner(RubyUnboundMethod unboundMethod) {
            return unboundMethod.method.getDeclaringModule();
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyArray parameters(RubyUnboundMethod method) {
            final ArgumentDescriptor[] argsDesc = method.method
                    .getSharedMethodInfo()
                    .getArgumentDescriptors();

            return ArgumentDescriptorUtils.argumentDescriptorsToParameters(getContext(), argsDesc, true);
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected Object sourceLocation(RubyUnboundMethod unboundMethod) {
            SourceSection sourceSection = unboundMethod.method
                    .getSharedMethodInfo()
                    .getSourceSection();

            if (!sourceSection.isAvailable()) {
                return nil;
            } else {
                RubyString file = makeStringNode.executeMake(
                        getContext().getSourcePath(sourceSection.getSource()),
                        UTF8Encoding.INSTANCE,
                        CodeRange.CR_UNKNOWN);
                Object[] objects = new Object[]{ file, sourceSection.getStartLine() };
                return createArray(objects);
            }
        }

    }

    @CoreMethod(names = "super_method")
    public abstract static class SuperMethodNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected Object superMethod(RubyUnboundMethod unboundMethod,
                @Cached AllocateHelperNode allocateHelperNode,
                @CachedLanguage RubyLanguage language) {
            InternalMethod internalMethod = unboundMethod.method;
            RubyModule origin = unboundMethod.origin;
            MethodLookupResult superMethod = ModuleOperations.lookupSuperMethod(internalMethod, origin);
            if (!superMethod.isDefined()) {
                return nil;
            } else {
                final RubyUnboundMethod instance = new RubyUnboundMethod(
                        coreLibrary().unboundMethodClass,
                        RubyLanguage.unboundMethodShape,
                        superMethod.getMethod().getDeclaringModule(),
                        superMethod.getMethod());
                allocateHelperNode.trace(instance, this, language);
                return instance;
            }
        }

    }

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        protected Object allocate(RubyClass rubyClass) {
            throw new RaiseException(getContext(), coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
