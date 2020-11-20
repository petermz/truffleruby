/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.module;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.collections.ConcurrentOperations;
import org.truffleruby.core.basicobject.BasicObjectNodes.ObjectIDNode;
import org.truffleruby.core.klass.ClassNodes;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.method.MethodFilter;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.language.RubyConstant;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.RubyGuards;
import org.truffleruby.language.constants.GetConstantNode;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.library.RubyLibrary;
import org.truffleruby.language.loader.ReentrantLockFreeingMap;
import org.truffleruby.language.methods.InternalMethod;
import org.truffleruby.language.objects.ObjectGraph;
import org.truffleruby.language.objects.ObjectGraphNode;
import org.truffleruby.language.objects.shared.SharedObjects;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public class ModuleFields extends ModuleChain implements ObjectGraphNode {

    public static void debugModuleChain(RubyModule module) {
        ModuleChain chain = module.fields;
        final StringBuilder builder = new StringBuilder();
        while (chain != null) {
            builder.append(chain.getClass());
            if (!(chain instanceof PrependMarker)) {
                RubyModule real = chain.getActualModule();
                builder.append(" " + real.fields.getName());
            }
            builder.append(System.lineSeparator());
            chain = chain.getParentModule();
        }
        RubyLanguage.LOGGER.info(builder.toString());
    }

    public final RubyModule rubyModule;

    // The context is stored here - objects can obtain it via their class (which is a module)
    private final RubyContext context;
    private final SourceSection sourceSection;

    private final PrependMarker start;

    private final RubyModule lexicalParent;
    public final String givenBaseName;

    private boolean hasFullName = false;
    private String name = null;

    /** Whether this is a refinement module (R), created by #refine */
    private boolean isRefinement = false;
    /** The module or class (C) refined by this refinement module */
    private RubyModule refinedModule;
    /** The namespace module (M) around the #refine call */
    private RubyModule refinementNamespace;

    private final ConcurrentMap<String, InternalMethod> methods = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RubyConstant> constants = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> classVariables = new ConcurrentHashMap<>();

    /** The refinements (calls to Module#refine) nested under/contained in this namespace module (M). Represented as a
     * map of refined classes and modules (C) to refinement modules (R). */
    private final ConcurrentMap<RubyModule, RubyModule> refinements = new ConcurrentHashMap<>();

    private final CyclicAssumption methodsUnmodifiedAssumption;
    private final CyclicAssumption constantsUnmodifiedAssumption;

    // Concurrency: only modified during boot
    private final Map<String, Assumption> inlinedBuiltinsAssumptions = new HashMap<>();

    public ModuleFields(
            RubyContext context,
            SourceSection sourceSection,
            RubyModule lexicalParent,
            String givenBaseName,
            RubyModule rubyModule) {
        super(null);
        this.context = context;
        this.sourceSection = sourceSection;
        this.lexicalParent = lexicalParent;
        this.givenBaseName = givenBaseName;
        this.rubyModule = rubyModule;
        this.methodsUnmodifiedAssumption = new CyclicAssumption("methods are unmodified");
        this.constantsUnmodifiedAssumption = new CyclicAssumption("constants are unmodified");
        start = new PrependMarker(this);
    }

    public RubyConstant getAdoptedByLexicalParent(
            RubyContext context,
            RubyModule lexicalParent,
            String name,
            Node currentNode) {
        RubyConstant previous = lexicalParent.fields.setConstantInternal(
                context,
                currentNode,
                name,
                rubyModule,
                false);

        if (!hasFullName()) {
            // Tricky, we need to compare with the Object class, but we only have a Class at hand.
            final RubyClass classClass = getLogicalClass().getLogicalClass();
            final RubyClass objectClass = ClassNodes.getSuperClass(ClassNodes.getSuperClass(classClass));

            if (lexicalParent == objectClass) {
                this.setFullName(name);
                updateAnonymousChildrenModules(context);
            } else if (lexicalParent.fields.hasFullName()) {
                this.setFullName(lexicalParent.fields.getName() + "::" + name);
                updateAnonymousChildrenModules(context);
            }
            // else: Our lexicalParent is also an anonymous module
            // and will name us when it gets named via updateAnonymousChildrenModules()
        }
        return previous;
    }

    public void updateAnonymousChildrenModules(RubyContext context) {
        for (Map.Entry<String, RubyConstant> entry : constants.entrySet()) {
            RubyConstant constant = entry.getValue();
            if (constant.hasValue() && constant.getValue() instanceof RubyModule) {
                RubyModule module = (RubyModule) constant.getValue();
                if (!module.fields.hasFullName()) {
                    module.fields.getAdoptedByLexicalParent(
                            context,
                            rubyModule,
                            entry.getKey(),
                            null);
                }
            }
        }
    }

    private boolean hasPrependedModules() {
        return start.getParentModule() != this;
    }

    public ModuleChain getFirstModuleChain() {
        return start.getParentModule();
    }

    @TruffleBoundary
    public void initCopy(RubyModule from) {
        // Do not copy name, the copy is an anonymous module
        final ModuleFields fromFields = from.fields;

        for (InternalMethod method : fromFields.methods.values()) {
            this.methods.put(method.getName(), method.withDeclaringModule(rubyModule));
        }

        for (Entry<String, RubyConstant> entry : fromFields.constants.entrySet()) {
            this.constants.put(entry.getKey(), entry.getValue());
        }

        this.classVariables.putAll(fromFields.classVariables);

        if (fromFields.hasPrependedModules()) {
            // Then the parent is the first in the prepend chain
            this.parentModule = fromFields.start.getParentModule();
        } else {
            this.parentModule = fromFields.parentModule;
        }

        if (rubyModule instanceof RubyClass) {
            // Singleton classes cannot be instantiated
            if (!((RubyClass) from).isSingleton) {
                ClassNodes.setInstanceShape((RubyClass) rubyModule, (RubyClass) from);
            }

            ((RubyClass) rubyModule).superclass = ((RubyClass) from).superclass;
        }
    }

    public void checkFrozen(RubyContext context, Node currentNode) {
        if (context.getCoreLibrary() != null && RubyLibrary.getUncached().isFrozen(rubyModule)) {
            String name;
            if (rubyModule instanceof RubyClass) {
                final RubyClass cls = (RubyClass) rubyModule;
                name = "object";
                if (cls.isSingleton) {
                    if (cls.attached instanceof RubyClass) {
                        name = "Class";
                    } else if (cls.attached instanceof RubyModule) {
                        name = "Module";
                    }
                } else {
                    name = "class";
                }
            } else {
                name = "module";
            }
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().frozenError(
                            StringUtils.format("can't modify frozen %s", name),
                            currentNode));
        }
    }

    @TruffleBoundary
    public void include(RubyContext context, Node currentNode, RubyModule module) {
        checkFrozen(context, currentNode);

        // If the module we want to include already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModule)) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().argumentError("cyclic include detected", currentNode));
        }

        SharedObjects.propagate(context, rubyModule, module);

        // We need to include the module ancestors in reverse order for a given inclusionPoint
        ModuleChain inclusionPoint = this;
        Deque<RubyModule> modulesToInclude = new ArrayDeque<>();
        for (RubyModule ancestor : module.fields.ancestors()) {
            if (ModuleOperations.includesModule(rubyModule, ancestor)) {
                if (isIncludedModuleBeforeSuperClass(ancestor)) {
                    // Include the modules at the appropriate inclusionPoint
                    performIncludes(inclusionPoint, modulesToInclude);
                    assert modulesToInclude.isEmpty();

                    // We need to include the others after that module
                    inclusionPoint = parentModule;
                    while (inclusionPoint.getActualModule() != ancestor) {
                        inclusionPoint = inclusionPoint.getParentModule();
                    }
                } else {
                    // Just ignore this module, as it is included above the superclass
                }
            } else {
                modulesToInclude.push(ancestor);
            }
        }

        performIncludes(inclusionPoint, modulesToInclude);

        newHierarchyVersion();
    }

    public void performIncludes(ModuleChain inclusionPoint, Deque<RubyModule> moduleAncestors) {
        while (!moduleAncestors.isEmpty()) {
            RubyModule mod = moduleAncestors.pop();
            inclusionPoint.insertAfter(mod);
        }
    }

    public boolean isIncludedModuleBeforeSuperClass(RubyModule module) {
        ModuleChain included = parentModule;
        while (included instanceof IncludedModule) {
            if (included.getActualModule() == module) {
                return true;
            }
            included = included.getParentModule();
        }
        return false;
    }

    @TruffleBoundary
    public void prepend(RubyContext context, Node currentNode, RubyModule module) {
        checkFrozen(context, currentNode);

        // If the module we want to prepend already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModule)) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().argumentError("cyclic prepend detected", currentNode));
        }

        SharedObjects.propagate(context, rubyModule, module);

        ModuleChain mod = module.fields.start;
        final ModuleChain topPrependedModule = start.getParentModule();
        ModuleChain cur = start;
        while (mod != null &&
                !(mod instanceof ModuleFields && ((ModuleFields) mod).rubyModule instanceof RubyClass)) {
            if (!(mod instanceof PrependMarker)) {
                if (!ModuleOperations.includesModule(rubyModule, mod.getActualModule())) {
                    cur.insertAfter(mod.getActualModule());
                    cur = cur.getParentModule();
                }
            }
            mod = mod.getParentModule();
        }

        // If there were already prepended modules, invalidate the first of them
        if (topPrependedModule != this) {
            topPrependedModule.getActualModule().fields.newHierarchyVersion();
        } else {
            this.newHierarchyVersion();
        }

        invalidateBuiltinsAssumptions();
    }

    /** Set the value of a constant, possibly redefining it. */
    @TruffleBoundary
    public RubyConstant setConstant(RubyContext context, Node currentNode, String name, Object value) {
        if (value instanceof RubyModule) {
            return ((RubyModule) value).fields.getAdoptedByLexicalParent(
                    context,
                    rubyModule,
                    name,
                    currentNode);
        } else {
            return setConstantInternal(context, currentNode, name, value, false);
        }
    }

    @TruffleBoundary
    public void setAutoloadConstant(RubyContext context, Node currentNode, String name, RubyString filename) {
        RubyConstant autoloadConstant = setConstantInternal(context, currentNode, name, filename, true);
        if (autoloadConstant == null) {
            return;
        }

        if (context.getOptions().LOG_AUTOLOAD) {
            RubyLanguage.LOGGER.info(() -> String.format(
                    "%s: setting up autoload %s with %s",
                    RubyContext.fileLine(context.getCallStack().getTopMostUserSourceSection()),
                    autoloadConstant,
                    filename));
        }
        final ReentrantLockFreeingMap<String> fileLocks = getContext().getFeatureLoader().getFileLocks();
        final ReentrantLock lock = fileLocks.get(filename.getJavaString());
        if (lock.isLocked()) {
            // We need to handle the new autoload constant immediately
            // if Object.autoload(name, filename) is executed from filename.rb
            GetConstantNode.autoloadConstantStart(autoloadConstant);
        }

        context.getFeatureLoader().addAutoload(autoloadConstant);
    }

    private RubyConstant setConstantInternal(RubyContext context, Node currentNode, String name, Object value,
            boolean autoload) {
        checkFrozen(context, currentNode);

        SharedObjects.propagate(context, rubyModule, value);

        final String autoloadPath = autoload ? ((RubyString) value).getJavaString() : null;
        RubyConstant previous;
        RubyConstant newConstant;
        do {
            previous = constants.get(name);
            if (autoload && previous != null) {
                if (previous.hasValue()) {
                    // abort, do not set an autoload constant, the constant already has a value
                    return null;
                } else if (previous.isAutoload() &&
                        previous.getAutoloadConstant().getAutoloadPath().equals(autoloadPath)) {
                    // already an autoload constant with the same path,
                    // do nothing so we don't replace the AutoloadConstant#autoloadLock which might be already acquired
                    return null;
                }
            }
            newConstant = newConstant(currentNode, name, value, autoload, previous);
        } while (!ConcurrentOperations.replace(constants, name, previous, newConstant));

        newConstantsVersion();
        return autoload ? newConstant : previous;
    }

    private RubyConstant newConstant(Node currentNode, String name, Object value, boolean autoload,
            RubyConstant previous) {
        final boolean isPrivate = previous != null && previous.isPrivate();
        final boolean isDeprecated = previous != null && previous.isDeprecated();
        final SourceSection sourceSection = currentNode != null ? currentNode.getSourceSection() : null;
        return new RubyConstant(rubyModule, name, value, isPrivate, autoload, isDeprecated, sourceSection);
    }

    @TruffleBoundary
    public RubyConstant removeConstant(RubyContext context, Node currentNode, String name) {
        checkFrozen(context, currentNode);
        final RubyConstant oldConstant = constants.remove(name);
        newConstantsVersion();
        return oldConstant;
    }

    @TruffleBoundary
    public void addMethod(RubyContext context, Node currentNode, InternalMethod method) {
        assert ModuleOperations.canBindMethodTo(method, rubyModule) ||
                ModuleOperations.assignableTo(context.getCoreLibrary().objectClass, method.getDeclaringModule()) ||
                // TODO (pitr-ch 24-Jul-2016): find out why undefined methods sometimes do not match above assertion
                // e.g. "block in _routes route_set.rb:525" in rails/actionpack/lib/action_dispatch/routing/
                (method.isUndefined() && methods.get(method.getName()) != null);

        checkFrozen(context, currentNode);

        if (SharedObjects.isShared(rubyModule)) {
            Set<Object> adjacent = ObjectGraph.newObjectSet();
            ObjectGraph.addProperty(adjacent, method);
            for (Object object : adjacent) {
                SharedObjects.writeBarrier(context, object);
            }
        }

        method.getSharedMethodInfo().setDefinitionModuleIfUnset(rubyModule);

        methods.put(method.getName(), method);

        if (!context.getCoreLibrary().isInitializing()) {
            newMethodsVersion();
            // invalidate assumptions to not use an AST-inlined methods
            changedMethod(method.getName());
            if (refinedModule != null) {
                refinedModule.fields.changedMethod(method.getName());
            }
        }

        if (context.getCoreLibrary().isLoaded() && !method.isUndefined()) {
            final RubySymbol methodSymbol = context.getSymbol(method.getName());
            if (RubyGuards.isSingletonClass(rubyModule)) {
                RubyDynamicObject receiver = ((RubyClass) rubyModule).attached;
                context.send(currentNode, receiver, "singleton_method_added", methodSymbol);
            } else {
                context.send(currentNode, rubyModule, "method_added", methodSymbol);
            }
        }
    }

    @TruffleBoundary
    public boolean removeMethod(String methodName) {
        final InternalMethod method = getMethod(methodName);
        if (method == null) {
            return false;
        }

        methods.remove(methodName);

        newMethodsVersion();
        changedMethod(methodName);
        return true;
    }

    @TruffleBoundary
    public void undefMethod(RubyContext context, Node currentNode, String methodName) {
        checkFrozen(context, currentNode);

        final InternalMethod method = ModuleOperations.lookupMethodUncached(rubyModule, methodName, null);
        if (method == null || method.isUndefined()) {
            final RubyModule moduleForError;
            if (RubyGuards.isMetaClass(rubyModule)) {
                moduleForError = (RubyModule) ((RubyClass) rubyModule).attached;
            } else {
                moduleForError = rubyModule;
            }

            throw new RaiseException(context, context.getCoreExceptions().nameErrorUndefinedMethod(
                    methodName,
                    moduleForError,
                    currentNode));
        } else {
            addMethod(context, currentNode, method.undefined());

            final RubySymbol methodSymbol = context.getSymbol(methodName);
            if (RubyGuards.isSingletonClass(rubyModule)) {
                final RubyDynamicObject receiver = ((RubyClass) rubyModule).attached;
                context.send(currentNode, receiver, "singleton_method_undefined", methodSymbol);
            } else {
                context.send(currentNode, rubyModule, "method_undefined", methodSymbol);
            }
        }
    }

    /** Also searches on Object for modules. Used for alias_method, visibility changes, etc. */
    @TruffleBoundary
    public InternalMethod deepMethodSearch(RubyContext context, String name) {
        InternalMethod method = ModuleOperations.lookupMethodUncached(rubyModule, name, null);
        if (method != null && !method.isUndefined()) {
            return method;
        }

        // Also search on Object if we are a Module. JRuby calls it deepMethodSearch().
        if (!(rubyModule instanceof RubyClass)) { // TODO: handle undefined methods
            method = ModuleOperations.lookupMethodUncached(context.getCoreLibrary().objectClass, name, null);

            if (method != null && !method.isUndefined()) {
                return method;
            }
        }

        if (isRefinement()) {
            return getRefinedModule().fields.deepMethodSearch(context, name);
        } else {
            return null;
        }
    }

    @TruffleBoundary
    public void changeConstantVisibility(
            final RubyContext context,
            final Node currentNode,
            final String name,
            final boolean isPrivate) {

        while (true) {
            final RubyConstant previous = constants.get(name);

            if (previous == null) {
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().nameErrorUninitializedConstant(
                                rubyModule,
                                name,
                                currentNode));
            }

            if (constants.replace(name, previous, previous.withPrivate(isPrivate))) {
                newConstantsVersion();
                break;
            }
        }
    }

    @TruffleBoundary
    public void deprecateConstant(RubyContext context, Node currentNode, String name) {
        while (true) {
            final RubyConstant previous = constants.get(name);

            if (previous == null) {
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().nameErrorUninitializedConstant(
                                rubyModule,
                                name,
                                currentNode));
            }

            if (constants.replace(name, previous, previous.withDeprecated())) {
                newConstantsVersion();
                break;
            }
        }
    }

    @TruffleBoundary
    public boolean undefineConstantIfStillAutoload(RubyConstant autoloadConstant) {
        if (constants.replace(autoloadConstant.getName(), autoloadConstant, autoloadConstant.undefined())) {
            newConstantsVersion();
            return true;
        } else {
            return false;
        }
    }

    public RubyContext getContext() {
        return context;
    }

    public String getName() {
        final String name = this.name;
        if (name == null) {
            // Lazily compute the anonymous name because it is expensive
            return getAnonymousName();
        }
        return name;
    }

    @TruffleBoundary
    public String getSimpleName() {
        String name = getName();
        int i = name.lastIndexOf("::");
        if (i == -1) {
            return name;
        } else {
            return name.substring(i + "::".length());
        }
    }

    @TruffleBoundary
    private String getAnonymousName() {
        final String anonymousName = createAnonymousName();
        this.name = anonymousName;
        return anonymousName;
    }

    public void setFullName(String name) {
        assert name != null;
        hasFullName = true;
        this.name = name;
    }

    @TruffleBoundary
    private String createAnonymousName() {
        if (givenBaseName != null) {
            return lexicalParent.fields.getName() + "::" + givenBaseName;
        } else if (getLogicalClass() == rubyModule) { // For the case of class Class during initialization
            return "#<cyclic>";
        } else {
            return "#<" + getLogicalClass().fields.getName() + ":0x" +
                    Long.toHexString(ObjectIDNode.uncachedObjectID(context, rubyModule)) + ">";
        }
    }

    public boolean hasFullName() {
        return hasFullName;
    }

    public boolean hasPartialName() {
        return hasFullName() || givenBaseName != null;
    }

    public boolean isRefinement() {
        return isRefinement;
    }

    public void setupRefinementModule(RubyModule refinedModule, RubyModule refinementNamespace) {
        this.isRefinement = true;
        this.refinedModule = refinedModule;
        this.refinementNamespace = refinementNamespace;
        this.parentModule = refinedModule.fields.start;
    }

    public RubyModule getRefinedModule() {
        return refinedModule;
    }

    public RubyModule getRefinementNamespace() {
        return refinementNamespace;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getName() + ")";
    }

    public void newConstantsVersion() {
        constantsUnmodifiedAssumption.invalidate(givenBaseName);
    }

    public void newHierarchyVersion() {
        newConstantsVersion();
        newMethodsVersion();

        if (isRefinement()) {
            getRefinedModule().fields.invalidateBuiltinsAssumptions();
        }
    }

    public void newMethodsVersion() {
        methodsUnmodifiedAssumption.invalidate(givenBaseName);
    }

    public Assumption getConstantsUnmodifiedAssumption() {
        return constantsUnmodifiedAssumption.getAssumption();
    }

    public Assumption getMethodsUnmodifiedAssumption() {
        return methodsUnmodifiedAssumption.getAssumption();
    }

    public Assumption getHierarchyUnmodifiedAssumption() {
        // Both assumptions are invalidated on hierarchy changes, just pick one of them.
        return getMethodsUnmodifiedAssumption();
    }

    public Iterable<Entry<String, RubyConstant>> getConstants() {
        return constants.entrySet();
    }

    @TruffleBoundary
    public RubyConstant getConstant(String name) {
        return constants.get(name);
    }

    public Iterable<InternalMethod> getMethods() {
        return methods.values();
    }

    @TruffleBoundary
    public InternalMethod getMethod(String name) {
        return methods.get(name);
    }

    public ConcurrentMap<String, Object> getClassVariables() {
        return classVariables;
    }

    public ConcurrentMap<RubyModule, RubyModule> getRefinements() {
        return refinements;
    }

    public void setSuperClass(RubyClass superclass) {
        assert rubyModule instanceof RubyClass;
        this.parentModule = superclass.fields.start;
        newHierarchyVersion();
    }

    @Override
    public RubyModule getActualModule() {
        return rubyModule;
    }

    /** Iterate over all ancestors, skipping PrependMarker and resolving IncludedModule. */
    public Iterable<RubyModule> ancestors() {
        return () -> new AncestorIterator(start);
    }

    /** Iterates over prepend'ed and include'd modules. */
    public Iterable<RubyModule> prependedAndIncludedModules() {
        return () -> new IncludedModulesIterator(start, this);
    }

    public Collection<RubySymbol> filterMethods(RubyContext context, boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModule);
        } else {
            allMethods = methods;
        }
        return filterMethods(context, allMethods, filter);
    }

    public Collection<RubySymbol> filterMethodsOnObject(
            RubyContext context,
            boolean includeAncestors,
            MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModule);
        } else {
            allMethods = ModuleOperations.getMethodsUntilLogicalClass(rubyModule);
        }
        return filterMethods(context, allMethods, filter);
    }

    public Collection<RubySymbol> filterSingletonMethods(
            RubyContext context,
            boolean includeAncestors,
            MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getMethodsBeforeLogicalClass(rubyModule);
        } else {
            allMethods = methods;
        }
        return filterMethods(context, allMethods, filter);
    }

    public Collection<RubySymbol> filterMethods(
            RubyContext context,
            Map<String, InternalMethod> allMethods,
            MethodFilter filter) {
        final Map<String, InternalMethod> methods = ModuleOperations.withoutUndefinedMethods(allMethods);

        final Set<RubySymbol> filtered = new HashSet<>();
        for (InternalMethod method : methods.values()) {
            if (filter.filter(method)) {
                filtered.add(context.getSymbol(method.getName()));
            }
        }

        return filtered;
    }

    public RubyClass getLogicalClass() {
        return rubyModule.getLogicalClass();
    }

    @Override
    public void getAdjacentObjects(Set<Object> adjacent) {
        if (lexicalParent != null) {
            adjacent.add(lexicalParent);
        }

        for (RubyModule module : prependedAndIncludedModules()) {
            ObjectGraph.addProperty(adjacent, module);
        }

        if (rubyModule instanceof RubyClass) {
            RubyClass superClass = ClassNodes.getSuperClass((RubyClass) rubyModule);
            ObjectGraph.addProperty(adjacent, superClass);
        }

        for (RubyConstant constant : constants.values()) {
            ObjectGraph.addProperty(adjacent, constant);
        }

        for (Object value : classVariables.values()) {
            ObjectGraph.addProperty(adjacent, value);
        }

        for (InternalMethod method : methods.values()) {
            ObjectGraph.addProperty(adjacent, method);
        }
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    /** Registers an Assumption for a given method name, which is invalidated when a method with same name is defined or
     * undefined in this class or when a module is prepended to this class. This does not check re-definitions in
     * subclasses. */
    public void registerAssumption(String methodName, Assumption assumption) {
        assert context.getCoreLibrary().isInitializing();
        Assumption old = inlinedBuiltinsAssumptions.put(methodName, assumption);
        assert old == null;
    }

    private void changedMethod(String name) {
        Assumption assumption = inlinedBuiltinsAssumptions.get(name);
        if (assumption != null) {
            assumption.invalidate();
        }
    }

    private void invalidateBuiltinsAssumptions() {
        if (!inlinedBuiltinsAssumptions.isEmpty()) {
            for (Assumption assumption : inlinedBuiltinsAssumptions.values()) {
                assumption.invalidate();
            }
        }
    }
}
