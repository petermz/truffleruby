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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
package org.truffleruby.core.format.write.bytes;

import org.truffleruby.collections.ByteArrayBuilder;
import org.truffleruby.core.rope.RopeOperations;

public class EncodeUM {

    private static final byte[] uu_table = RopeOperations
            .encodeAsciiBytes("`!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_");
    private static final byte[] b64_table = RopeOperations
            .encodeAsciiBytes("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
    public static final byte[] sHexDigits = RopeOperations.encodeAsciiBytes("0123456789abcdef0123456789ABCDEFx");
    public static final int[] b64_xtable = new int[256];

    static {
        // b64_xtable for decoding Base 64
        for (int i = 0; i < 256; i++) {
            b64_xtable[i] = -1;
        }
        for (int i = 0; i < 64; i++) {
            b64_xtable[b64_table[i]] = i;
        }
    }

    public static void encodeUM(Object runtime, byte[] lCurElemString, int occurrences, boolean ignoreStar, char type,
            ByteArrayBuilder result) {
        if (occurrences == 0 && type == 'm' && !ignoreStar) {
            encodes(
                    runtime,
                    result,
                    lCurElemString,
                    0,
                    lCurElemString.length,
                    lCurElemString.length,
                    (byte) type,
                    false);
            return;
        }

        occurrences = occurrences <= 2 ? 45 : occurrences / 3 * 3;
        if (lCurElemString.length == 0) {
            return;
        }

        byte[] charsToEncode = lCurElemString;
        for (int i = 0; i < lCurElemString.length; i += occurrences) {
            encodes(
                    runtime,
                    result,
                    charsToEncode,
                    i,
                    lCurElemString.length - i,
                    occurrences,
                    (byte) type,
                    true);
        }
    }

    private static ByteArrayBuilder encodes(Object runtime, ByteArrayBuilder io2Append, byte[] charsToEncode,
            int startIndex, int length, int charCount, byte encodingType, boolean tailLf) {
        charCount = charCount < length ? charCount : length;

        io2Append.unsafeEnsureSpace(charCount * 4 / 3 + 6);
        int i = startIndex;
        byte[] lTranslationTable = encodingType == 'u' ? uu_table : b64_table;
        byte lPadding;
        if (encodingType == 'u') {
            if (charCount >= lTranslationTable.length) {
                //throw runtime.newArgumentError(charCount
                //    + " is not a correct value for the number of bytes per line in a u directive.  Correct values range from 0 to "
                //    + lTranslationTable.length);
                throw new UnsupportedOperationException();
            }
            io2Append.append(lTranslationTable[charCount]);
            lPadding = '`';
        } else {
            lPadding = '=';
        }
        while (charCount >= 3) {
            byte lCurChar = charsToEncode[i++];
            byte lNextChar = charsToEncode[i++];
            byte lNextNextChar = charsToEncode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >>> 4) & 017))]);
            io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | ((lNextNextChar >>> 6) & 03))]);
            io2Append.append(lTranslationTable[077 & lNextNextChar]);
            charCount -= 3;
        }
        if (charCount == 2) {
            byte lCurChar = charsToEncode[i++];
            byte lNextChar = charsToEncode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >> 4) & 017))]);
            io2Append.append(lTranslationTable[077 & ((lNextChar << 2) & 074)]);
            io2Append.append(lPadding);
        } else if (charCount == 1) {
            byte lCurChar = charsToEncode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & ((lCurChar << 4) & 060)]);
            io2Append.append(lPadding);
            io2Append.append(lPadding);
        }
        if (tailLf) {
            io2Append.append('\n');
        }
        return io2Append;
    }

}
