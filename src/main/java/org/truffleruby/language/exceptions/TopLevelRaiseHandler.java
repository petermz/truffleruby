/*
 * Copyright (c) 2014, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.exceptions;

import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.truffleruby.core.cast.IntegerCastNodeGen;
import org.truffleruby.core.exception.RubyException;
import org.truffleruby.core.kernel.AtExitManager;
import org.truffleruby.core.thread.GetCurrentRubyThreadNode;
import org.truffleruby.language.RubyContextNode;
import org.truffleruby.language.control.ExitException;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.dispatch.DispatchNode;

import com.oracle.truffle.api.CompilerDirectives;
import org.truffleruby.language.objects.IsANodeGen;

public class TopLevelRaiseHandler extends RubyContextNode {

    @Child private GetCurrentRubyThreadNode getCurrentRubyThreadNode;

    public int execute(Runnable body) {
        int exitCode = 0;
        RubyException caughtException = null;

        // Execute the main script
        try {
            body.run();
        } catch (RaiseException e) {
            caughtException = e.getException();
            exitCode = statusFromException(caughtException);
            setLastException(caughtException); // Set $! for at_exit
            // printing the main script exception is delayed after at_exit hooks
        } catch (ExitException e) {
            // hard #exit!, return immediately, skip at_exit hooks
            return e.getCode();
        }

        // Execute at_exit hooks (except if hard #exit!)
        try {
            RubyException atExitException = getContext().getAtExitManager().runAtExitHooks();
            if (atExitException != null) {
                exitCode = statusFromException(atExitException);
            }

            if (caughtException != null) {
                // print the main script exception now
                if (!AtExitManager.isSilentException(getContext(), caughtException)) {
                    getContext().getDefaultBacktraceFormatter().printTopLevelRubyExceptionOnEnvStderr(caughtException);
                }

                handleSignalException(caughtException);
            }
        } catch (ExitException e) {
            // hard #exit! during at_exit: ignore the main script exception
            exitCode = e.getCode();
        }

        return exitCode;
    }

    private int statusFromException(RubyException exception) {
        if (IsANodeGen.getUncached().executeIsA(exception, coreLibrary().systemExitClass)) {
            final Object status = DynamicObjectLibrary.getUncached().getOrDefault(exception, "@status", null);
            return IntegerCastNodeGen.getUncached().executeCastInt(status);
        } else {
            return 1;
        }
    }

    private void setLastException(RubyException exception) {
        if (getCurrentRubyThreadNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getCurrentRubyThreadNode = insert(GetCurrentRubyThreadNode.create());
        }

        getCurrentRubyThreadNode.execute().threadLocalGlobals.exception = exception;
    }

    private void handleSignalException(RubyException exception) {
        if (exception.getLogicalClass() == coreLibrary().signalExceptionClass) {
            // Calls raise(3) or no-op
            DispatchNode.getUncached().call(exception, "reached_top_level");
        }
    }

}
