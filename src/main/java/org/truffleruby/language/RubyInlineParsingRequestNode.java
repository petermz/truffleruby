/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language;

import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.language.arguments.RubyArguments;
import org.truffleruby.language.methods.DeclarationContext;
import org.truffleruby.language.methods.InternalMethod;
import org.truffleruby.language.methods.SharedMethodInfo;
import org.truffleruby.parser.ParserContext;
import org.truffleruby.parser.RubySource;
import org.truffleruby.parser.TranslatorDriver;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.source.Source;

public class RubyInlineParsingRequestNode extends ExecutableNode {

    private final RubyContext context;
    private final InternalMethod method;

    @Child private DirectCallNode callNode;

    public RubyInlineParsingRequestNode(
            RubyLanguage language,
            RubyContext context,
            Source source,
            MaterializedFrame currentFrame) {
        super(language);
        this.context = context;

        final TranslatorDriver translator = new TranslatorDriver(context);

        // We use the current frame as the lexical scope to parse, but then we may run with a new frame in the future

        final RubyRootNode rootNode = translator.parse(
                new RubySource(source, context.getSourcePath(source)),
                ParserContext.INLINE,
                null,
                currentFrame,
                null,
                false,
                null);

        final RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
        callNode.forceInlining();

        final SharedMethodInfo sharedMethodInfo = rootNode.getSharedMethodInfo();
        this.method = new InternalMethod(
                context,
                sharedMethodInfo,
                sharedMethodInfo.getLexicalScope(),
                DeclarationContext.topLevel(context),
                sharedMethodInfo.getName(),
                context.getCoreLibrary().objectClass,
                Visibility.PUBLIC,
                callTarget);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert RubyLanguage.getCurrentContext() == context;

        // We run the Ruby code as if it was written in a block
        final Object[] arguments = RubyArguments.pack(
                frame.materialize(),
                null,
                null,
                method,
                null,
                RubyArguments.getSelf(frame),
                RubyArguments.getBlock(frame),
                RubyBaseNode.EMPTY_ARGUMENTS);

        // No need to share the returned value here, InlineParsingRequest is not exposed to the Context API
        // and is only used by instruments (e.g., the debugger) or the RubyLanguage itself.
        return callNode.call(arguments);
    }

}
