/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.truffleruby.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jcodings.specific.UTF8Encoding;
import org.joni.WarnCallback;
import org.truffleruby.RubyContext;
import org.truffleruby.core.rope.Rope;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringOperations;
import org.truffleruby.language.control.RaiseException;

public class RubyWarnings implements WarnCallback {

    private final RubyContext context;

    public RubyWarnings(RubyContext context) {
        this.context = context;
    }

    public boolean warningsEnabled() {
        return context.getCoreLibrary().warningsEnabled();
    }

    public boolean isVerbose() {
        return context != null && context.getCoreLibrary().isVerbose();
    }

    @Override
    public void warn(String message) {
        warn(null, message);
    }

    /** Prints a warning, unless $VERBOSE is nil. */
    public void warn(String fileName, int lineNumber, String message) {
        if (!warningsEnabled()) {
            return;
        }

        StringBuilder buffer = new StringBuilder(100);

        buffer.append(fileName).append(':').append(lineNumber).append(": ");
        buffer.append("warning: ").append(message).append('\n');
        printWarning(buffer.toString());
    }

    /** Prints a warning, unless $VERBOSE is nil. */
    public void warn(String fileName, String message) {
        if (!warningsEnabled()) {
            return;
        }

        StringBuilder buffer = new StringBuilder(100);

        if (fileName != null) {
            buffer.append(fileName).append(' ');
        }
        buffer.append("warning: ").append(message).append('\n');
        printWarning(buffer.toString());
    }

    /** Prints a warning, only in verbose mode. */
    public void warning(String fileName, int lineNumber, String message) {
        if (!isVerbose()) {
            return;
        }

        warn(fileName, lineNumber, message);
    }

    private void printWarning(String message) {
        if (context.getCoreLibrary().isLoaded()) {
            final Object warning = context.getCoreLibrary().warningModule;
            final Rope messageRope = StringOperations.encodeRope(message, UTF8Encoding.INSTANCE);
            final RubyString messageString = StringOperations.createString(context, messageRope);
            context.send(warning, "warn", messageString);
        } else {
            try {
                context.getEnv().err().write(message.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RaiseException(context, context.getCoreExceptions().ioError(e, null));
            }
        }
    }

}
