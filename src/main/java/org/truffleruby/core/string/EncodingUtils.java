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
package org.truffleruby.core.string;

import java.util.ArrayList;
import java.util.List;

import org.jcodings.Encoding;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.rope.RopeOperations;

public class EncodingUtils {

    // rb_enc_asciicompat
    public static boolean encAsciicompat(Encoding enc) {
        return encMbminlen(enc) == 1 && !encDummy(enc);
    }

    // rb_enc_mbminlen
    public static int encMbminlen(Encoding encoding) {
        return encoding.minLength();
    }

    // rb_enc_dummy_p
    public static boolean encDummy(Encoding enc) {
        return enc.isDummy();
    }

    public static boolean DECORATOR_P(byte[] sname, byte[] dname) {
        return sname == null || sname.length == 0 || sname[0] == 0;
    }

    public static List<String> encodingNames(byte[] name, int p, int end) {
        final List<String> names = new ArrayList<>();

        Encoding enc = ASCIIEncoding.INSTANCE;
        int s = p;

        int code = name[s] & 0xff;
        if (enc.isDigit(code)) {
            return names;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        if (enc.isUpper(code)) {
            hasUpper = true;
            while (++s < end && (enc.isAlnum(name[s] & 0xff) || name[s] == (byte) '_')) {
                if (enc.isLower(name[s] & 0xff)) {
                    hasLower = true;
                }
            }
        }

        boolean isValid = false;
        if (s >= end) {
            isValid = true;
            names.add(RopeOperations.decodeAscii(name, p, end));
        }

        if (!isValid || hasLower) {
            if (!hasLower || !hasUpper) {
                do {
                    code = name[s] & 0xff;
                    if (enc.isLower(code)) {
                        hasLower = true;
                    }
                    if (enc.isUpper(code)) {
                        hasUpper = true;
                    }
                } while (++s < end && (!hasLower || !hasUpper));
            }

            byte[] constName = new byte[end - p];
            System.arraycopy(name, p, constName, 0, end - p);
            s = 0;
            code = constName[s] & 0xff;

            if (!isValid) {
                if (enc.isLower(code)) {
                    constName[s] = AsciiTables.ToUpperCaseTable[code];
                }
                for (; s < constName.length; ++s) {
                    if (!enc.isAlnum(constName[s] & 0xff)) {
                        constName[s] = (byte) '_';
                    }
                }
                if (hasUpper) {
                    names.add(RopeOperations.decodeAscii(constName));
                }
            }
            if (hasLower) {
                for (s = 0; s < constName.length; ++s) {
                    code = constName[s] & 0xff;
                    if (enc.isLower(code)) {
                        constName[s] = AsciiTables.ToUpperCaseTable[code];
                    }
                }
                names.add(RopeOperations.decodeAscii(constName));
            }
        }

        return names;
    }


    // rb_enc_ascget
    public static int encAscget(byte[] pBytes, int p, int e, int[] len, Encoding enc, CodeRange codeRange) {
        int c;
        int l;

        if (e <= p) {
            return -1;
        }

        if (encAsciicompat(enc)) {
            c = pBytes[p] & 0xFF;
            if (!Encoding.isAscii((byte) c)) {
                return -1;
            }
            if (len != null) {
                len[0] = 1;
            }
            return c;
        }
        l = StringSupport.characterLength(enc, codeRange, pBytes, p, e);
        if (!StringSupport.MBCLEN_CHARFOUND_P(l)) {
            return -1;
        }
        c = enc.mbcToCode(pBytes, p, e);
        if (!Encoding.isAscii(c)) {
            return -1;
        }
        if (len != null) {
            len[0] = l;
        }
        return c;
    }

    // rb_enc_codepoint_len
    public static int encCodepointLength(byte[] pBytes, int p, int e, int[] len_p, Encoding enc, CodeRange codeRange) {
        int r;
        if (e <= p) {
            throw new IllegalArgumentException("empty string");
        }
        r = StringSupport.characterLength(enc, codeRange, pBytes, p, e);
        if (!StringSupport.MBCLEN_CHARFOUND_P(r)) {
            throw new IllegalArgumentException("invalid byte sequence in " + enc);
        }
        if (len_p != null) {
            len_p[0] = StringSupport.MBCLEN_CHARFOUND_LEN(r);
        }
        return StringSupport.codePoint(enc, codeRange, pBytes, p, e);
    }

    // rb_enc_mbcput
    public static void encMbcput(int c, byte[] buf, int p, Encoding enc) {
        enc.codeToMbc(c, buf, p);
    }

}
