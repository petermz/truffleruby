/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.cext;

import static org.truffleruby.core.symbol.CoreSymbols.idToIndex;

import com.oracle.truffle.api.dsl.CachedLanguage;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.core.symbol.CoreSymbols;
import org.truffleruby.core.symbol.RubySymbol;
import org.truffleruby.language.RubyBaseNode;
import org.truffleruby.language.control.RaiseException;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

@GenerateUncached
@ReportPolymorphism
public abstract class IDToSymbolNode extends RubyBaseNode {

    public abstract Object execute(Object value);

    public static IDToSymbolNode create() {
        return IDToSymbolNodeGen.create();
    }

    @Specialization(guards = "isStaticSymbol(value)")
    protected Object unwrapStaticUncached(long value,
            @CachedLanguage RubyLanguage language,
            @CachedContext(RubyLanguage.class) RubyContext context,
            @Cached BranchProfile errorProfile) {
        final int index = idToIndex(value);
        final RubySymbol symbol = language.coreSymbols.STATIC_SYMBOLS[index];
        if (symbol == null) {
            errorProfile.enter();
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().runtimeError(
                            StringUtils.format("invalid static ID2SYM id: %d", value),
                            this));
        }
        return symbol;
    }

    @Specialization(guards = "!isStaticSymbol(value)")
    protected Object unwrapObject(Object value,
            @Cached UnwrapNode unwrapNode) {
        return unwrapNode.execute(value);
    }

    public static boolean isStaticSymbol(Object value) {
        if (!(value instanceof Long)) {
            return false;
        }
        return CoreSymbols.isStaticSymbol((long) value);
    }

}
