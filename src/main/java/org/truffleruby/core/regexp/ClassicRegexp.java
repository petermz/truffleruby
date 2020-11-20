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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.truffleruby.core.regexp;

import static org.truffleruby.core.rope.CodeRange.CR_7BIT;
import static org.truffleruby.core.rope.CodeRange.CR_UNKNOWN;
import static org.truffleruby.core.string.StringUtils.EMPTY_STRING_ARRAY;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Syntax;
import org.joni.exception.JOniException;
import org.truffleruby.RubyContext;
import org.truffleruby.SuppressFBWarnings;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.rope.Rope;
import org.truffleruby.core.rope.RopeBuilder;
import org.truffleruby.core.rope.RopeOperations;
import org.truffleruby.core.string.StringSupport;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.parser.ReOptions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class ClassicRegexp implements ReOptions {
    private final RubyContext context;
    private final Regex pattern;
    private final Rope str;
    private final RegexpOptions options;

    public void setLiteral() {
        options.setLiteral(true);
    }

    public Encoding getEncoding() {
        return pattern.getEncoding();
    }

    private static Regex makeRegexp(RubyContext context, RopeBuilder bytes, RegexpOptions options, Encoding enc) {
        try {
            return new Regex(
                    bytes.getUnsafeBytes(),
                    0,
                    bytes.getLength(),
                    options.toJoniOptions(),
                    enc,
                    Syntax.DEFAULT,
                    new RegexWarnCallback(context));
        } catch (Exception e) {
            throw new RaiseException(context, context.getCoreExceptions().regexpError(e.getMessage(), null));
        }
    }

    private static Regex getRegexpFromCache(RubyContext context, RopeBuilder bytes, Encoding encoding,
            RegexpOptions options) {
        if (context == null) {
            final Regex regex = makeRegexp(null, bytes, options, encoding);
            regex.setUserObject(bytes);
            return regex;
        }

        final Rope rope = RopeOperations.ropeFromRopeBuilder(bytes);
        final int joniOptions = options.toJoniOptions();
        final RegexpCacheKey cacheKey = new RegexpCacheKey(
                rope,
                encoding,
                joniOptions,
                context.getHashing(context.getRegexpCache()));

        final Regex regex = context.getRegexpCache().get(cacheKey);
        if (regex != null) {
            return regex;
        } else {
            final Regex newRegex = makeRegexp(context, bytes, options, encoding);
            newRegex.setUserObject(bytes);
            return context.getRegexpCache().addInCacheIfAbsent(cacheKey, newRegex);
        }
    }

    public ClassicRegexp(RubyContext context, Rope str, RegexpOptions originalOptions) {
        this.context = context;
        this.options = (RegexpOptions) originalOptions.clone();

        Encoding enc = str.getEncoding();
        if (enc.isDummy()) {
            throw new UnsupportedOperationException("can't make regexp with dummy encoding");
        }

        Encoding[] fixedEnc = new Encoding[]{ null };
        RopeBuilder unescaped = preprocess(context, str, enc, fixedEnc, RegexpSupport.ErrorMode.RAISE);
        enc = computeRegexpEncoding(options, enc, fixedEnc, context);

        this.pattern = getRegexpFromCache(context, unescaped, enc, options);
        this.str = str;
    }

    @TruffleBoundary
    @SuppressWarnings("fallthrough")
    private static boolean unescapeNonAscii(RubyContext context, RopeBuilder to, Rope str, Encoding enc,
            Encoding[] encp, RegexpSupport.ErrorMode mode) {
        boolean hasProperty = false;
        byte[] buf = null;

        int p = 0;
        int end = str.byteLength();
        final byte[] bytes = str.getBytes();

        while (p < end) {
            final int cl = StringSupport
                    .characterLength(enc, enc == str.getEncoding() ? str.getCodeRange() : CR_UNKNOWN, bytes, p, end);
            if (cl <= 0) {
                raisePreprocessError(context, str, "invalid multibyte character", mode);
            }
            if (cl > 1 || (bytes[p] & 0x80) != 0) {
                if (to != null) {
                    to.append(bytes, p, cl);
                }
                p += cl;
                if (encp[0] == null) {
                    encp[0] = enc;
                } else if (encp[0] != enc) {
                    raisePreprocessError(context, str, "non ASCII character in UTF-8 regexp", mode);
                }
                continue;
            }
            int c;
            switch (c = bytes[p++] & 0xff) {
                case '\\':
                    if (p == end) {
                        raisePreprocessError(context, str, "too short escape sequence", mode);
                    }

                    switch (c = bytes[p++] & 0xff) {
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7': /* \O, \OO, \OOO or backref */
                            if (StringSupport.scanOct(bytes, p - 1, end - (p - 1)) <= 0177) {
                                if (to != null) {
                                    to.append('\\');
                                    to.append(c);
                                }
                                break;
                            }

                        case '0': /* \0, \0O, \0OO */
                        case 'x': /* \xHH */
                        case 'c': /* \cX, \c\M-X */
                        case 'C': /* \C-X, \C-\M-X */
                        case 'M': /* \M-X, \M-\C-X, \M-\cX */
                            p -= 2;
                            if (enc == USASCIIEncoding.INSTANCE) {
                                if (buf == null) {
                                    buf = new byte[1];
                                }
                                int pbeg = p;
                                p = readEscapedByte(context, buf, 0, bytes, p, end, str, mode);
                                c = buf[0];
                                if (c == -1) {
                                    return false;
                                }
                                if (to != null) {
                                    to.append(bytes, pbeg, p - pbeg);
                                }
                            } else {
                                p = unescapeEscapedNonAscii(context, to, bytes, p, end, enc, encp, str, mode);
                            }
                            break;

                        case 'u':
                            if (p == end) {
                                raisePreprocessError(context, str, "too short escape sequence", mode);
                            }
                            if (bytes[p] == (byte) '{') { /* \\u{H HH HHH HHHH HHHHH HHHHHH ...} */
                                p++;
                                p = unescapeUnicodeList(context, to, bytes, p, end, encp, str, mode);
                                if (p == end || bytes[p++] != (byte) '}') {
                                    raisePreprocessError(context, str, "invalid Unicode list", mode);
                                }
                            } else { /* \\uHHHH */
                                p = unescapeUnicodeBmp(context, to, bytes, p, end, encp, str, mode);
                            }
                            break;
                        case 'p': /* \p{Hiragana} */
                            if (encp[0] == null) {
                                hasProperty = true;
                            }
                            if (to != null) {
                                to.append('\\');
                                to.append(c);
                            }
                            break;

                        default:
                            if (to != null) {
                                to.append('\\');
                                to.append(c);
                            }
                            break;
                    } // inner switch
                    break;

                default:
                    if (to != null) {
                        to.append(c);
                    }
            } // switch
        } // while
        return hasProperty;
    }

    private static int unescapeUnicodeBmp(RubyContext context, RopeBuilder to, byte[] bytes, int p, int end,
            Encoding[] encp, Rope str, RegexpSupport.ErrorMode mode) {
        if (p + 4 > end) {
            raisePreprocessError(context, str, "invalid Unicode escape", mode);
        }
        int code = StringSupport.scanHex(bytes, p, 4);
        int len = StringSupport.hexLength(bytes, p, 4);
        if (len != 4) {
            raisePreprocessError(context, str, "invalid Unicode escape", mode);
        }
        appendUtf8(context, to, code, encp, str, mode);
        return p + 4;
    }

    private static int unescapeUnicodeList(RubyContext context, RopeBuilder to, byte[] bytes, int p, int end,
            Encoding[] encp, Rope str, RegexpSupport.ErrorMode mode) {
        while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) {
            p++;
        }

        boolean hasUnicode = false;
        while (true) {
            int code = StringSupport.scanHex(bytes, p, end - p);
            int len = StringSupport.hexLength(bytes, p, end - p);
            if (len == 0) {
                break;
            }
            if (len > 6) {
                raisePreprocessError(context, str, "invalid Unicode range", mode);
            }
            p += len;
            if (to != null) {
                appendUtf8(context, to, code, encp, str, mode);
            }
            hasUnicode = true;
            while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) {
                p++;
            }
        }

        if (!hasUnicode) {
            raisePreprocessError(context, str, "invalid Unicode list", mode);
        }
        return p;
    }

    private static void appendUtf8(RubyContext context, RopeBuilder to, int code, Encoding[] enc, Rope str,
            RegexpSupport.ErrorMode mode) {
        checkUnicodeRange(context, code, str, mode);

        if (code < 0x80) {
            if (to != null) {
                to.append(StringUtils.formatASCIIBytes("\\x%02X", code));
            }
        } else {
            if (to != null) {
                to.unsafeEnsureSpace(to.getLength() + 6);
                to.setLength(to.getLength() + utf8Decode(to.getUnsafeBytes(), to.getLength(), code));
            }
            if (enc[0] == null) {
                enc[0] = UTF8Encoding.INSTANCE;
            } else if (!(enc[0].isUTF8())) {
                raisePreprocessError(context, str, "UTF-8 character in non UTF-8 regexp", mode);
            }
        }
    }

    public static int utf8Decode(byte[] to, int p, int code) {
        if (code <= 0x7f) {
            to[p] = (byte) code;
            return 1;
        } else if (code <= 0x7ff) {
            to[p + 0] = (byte) (((code >>> 6) & 0xff) | 0xc0);
            to[p + 1] = (byte) ((code & 0x3f) | 0x80);
            return 2;
        } else if (code <= 0xffff) {
            to[p + 0] = (byte) (((code >>> 12) & 0xff) | 0xe0);
            to[p + 1] = (byte) (((code >>> 6) & 0x3f) | 0x80);
            to[p + 2] = (byte) ((code & 0x3f) | 0x80);
            return 3;
        } else if (code <= 0x1fffff) {
            to[p + 0] = (byte) (((code >>> 18) & 0xff) | 0xf0);
            to[p + 1] = (byte) (((code >>> 12) & 0x3f) | 0x80);
            to[p + 2] = (byte) (((code >>> 6) & 0x3f) | 0x80);
            to[p + 3] = (byte) ((code & 0x3f) | 0x80);
            return 4;
        } else if (code <= 0x3ffffff) {
            to[p + 0] = (byte) (((code >>> 24) & 0xff) | 0xf8);
            to[p + 1] = (byte) (((code >>> 18) & 0x3f) | 0x80);
            to[p + 2] = (byte) (((code >>> 12) & 0x3f) | 0x80);
            to[p + 3] = (byte) (((code >>> 6) & 0x3f) | 0x80);
            to[p + 4] = (byte) ((code & 0x3f) | 0x80);
            return 5;
        } else { // code <= 0x7fffffff = max int
            to[p + 0] = (byte) (((code >>> 30) & 0xff) | 0xfc);
            to[p + 1] = (byte) (((code >>> 24) & 0x3f) | 0x80);
            to[p + 2] = (byte) (((code >>> 18) & 0x3f) | 0x80);
            to[p + 3] = (byte) (((code >>> 12) & 0x3f) | 0x80);
            to[p + 4] = (byte) (((code >>> 6) & 0x3f) | 0x80);
            to[p + 5] = (byte) ((code & 0x3f) | 0x80);
            return 6;
        }
    }

    private static void checkUnicodeRange(RubyContext context, int code, Rope str, RegexpSupport.ErrorMode mode) {
        // Unicode is can be only 21 bits long, int is enough
        if ((0xd800 <= code && code <= 0xdfff) /* Surrogates */ || 0x10ffff < code) {
            raisePreprocessError(context, str, "invalid Unicode range", mode);
        }
    }

    private static int unescapeEscapedNonAscii(RubyContext context, RopeBuilder to, byte[] bytes, int p, int end,
            Encoding enc, Encoding[] encp, Rope str, RegexpSupport.ErrorMode mode) {
        byte[] chBuf = new byte[enc.maxLength()];
        int chLen = 0;

        p = readEscapedByte(context, chBuf, chLen++, bytes, p, end, str, mode);
        while (chLen < enc.maxLength() &&
                StringSupport.MBCLEN_NEEDMORE_P(StringSupport.characterLength(enc, CR_UNKNOWN, chBuf, 0, chLen))) {
            p = readEscapedByte(context, chBuf, chLen++, bytes, p, end, str, mode);
        }

        int cl = StringSupport.characterLength(enc, CR_UNKNOWN, chBuf, 0, chLen);
        if (cl == -1) {
            raisePreprocessError(context, str, "invalid multibyte escape", mode); // MBCLEN_INVALID_P
        }

        if (chLen > 1 || (chBuf[0] & 0x80) != 0) {
            if (to != null) {
                to.append(chBuf, 0, chLen);
            }

            if (encp[0] == null) {
                encp[0] = enc;
            } else if (encp[0] != enc) {
                raisePreprocessError(context, str, "escaped non ASCII character in UTF-8 regexp", mode);
            }
        } else {
            if (to != null) {
                to.append(StringUtils.formatASCIIBytes("\\x%02X", chBuf[0] & 0xff));
            }
        }
        return p;
    }

    public static int raisePreprocessError(RubyContext context, Rope str, String err, RegexpSupport.ErrorMode mode) {
        switch (mode) {
            case RAISE:
                throw new RaiseException(context, context.getCoreExceptions().regexpError(err, null));
            case PREPROCESS:
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().argumentError("regexp preprocess failed: " + err, null));
            case DESC:
                // silent ?
        }
        return 0;
    }

    @SuppressWarnings("fallthrough")
    @SuppressFBWarnings("SF")
    public static int readEscapedByte(RubyContext context, byte[] to, int toP, byte[] bytes, int p, int end, Rope str,
            RegexpSupport.ErrorMode mode) {
        if (p == end || bytes[p++] != (byte) '\\') {
            raisePreprocessError(context, str, "too short escaped multibyte character", mode);
        }

        boolean metaPrefix = false, ctrlPrefix = false;
        int code = 0;
        while (true) {
            if (p == end) {
                raisePreprocessError(context, str, "too short escape sequence", mode);
            }

            switch (bytes[p++]) {
                case '\\':
                    code = '\\';
                    break;
                case 'n':
                    code = '\n';
                    break;
                case 't':
                    code = '\t';
                    break;
                case 'r':
                    code = '\r';
                    break;
                case 'f':
                    code = '\f';
                    break;
                case 'v':
                    code = '\013';
                    break;
                case 'a':
                    code = '\007';
                    break;
                case 'e':
                    code = '\033';
                    break;

                /* \OOO */
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    p--;
                    int olen = end < p + 3 ? end - p : 3;
                    code = StringSupport.scanOct(bytes, p, olen);
                    p += StringSupport.octLength(bytes, p, olen);
                    break;

                case 'x': /* \xHH */
                    int hlen = end < p + 2 ? end - p : 2;
                    code = StringSupport.scanHex(bytes, p, hlen);
                    int len = StringSupport.hexLength(bytes, p, hlen);
                    if (len < 1) {
                        raisePreprocessError(context, str, "invalid hex escape", mode);
                    }
                    p += len;
                    break;

                case 'M': /* \M-X, \M-\C-X, \M-\cX */
                    if (metaPrefix) {
                        raisePreprocessError(context, str, "duplicate meta escape", mode);
                    }
                    metaPrefix = true;
                    if (p + 1 < end && bytes[p++] == (byte) '-' && (bytes[p] & 0x80) == 0) {
                        if (bytes[p] == (byte) '\\') {
                            p++;
                            continue;
                        } else {
                            code = bytes[p++] & 0xff;
                            break;
                        }
                    }
                    raisePreprocessError(context, str, "too short meta escape", mode);

                case 'C': /* \C-X, \C-\M-X */
                    if (p == end || bytes[p++] != (byte) '-') {
                        raisePreprocessError(context, str, "too short control escape", mode);
                    }

                case 'c': /* \cX, \c\M-X */
                    if (ctrlPrefix) {
                        raisePreprocessError(context, str, "duplicate control escape", mode);
                    }
                    ctrlPrefix = true;
                    if (p < end && (bytes[p] & 0x80) == 0) {
                        if (bytes[p] == (byte) '\\') {
                            p++;
                            continue;
                        } else {
                            code = bytes[p++] & 0xff;
                            break;
                        }
                    }
                    raisePreprocessError(context, str, "too short control escape", mode);
                default:
                    raisePreprocessError(context, str, "unexpected escape sequence", mode);
            } // switch

            if (code < 0 || code > 0xff) {
                raisePreprocessError(context, str, "invalid escape code", mode);
            }

            if (ctrlPrefix) {
                code &= 0x1f;
            }
            if (metaPrefix) {
                code |= 0x80;
            }

            to[toP] = (byte) code;
            return p;
        } // while
    }

    public static void preprocessCheck(RubyContext runtime, Rope bytes) {
        preprocess(runtime, bytes, bytes.getEncoding(), new Encoding[]{ null }, RegexpSupport.ErrorMode.RAISE);
    }

    public static RopeBuilder preprocess(RubyContext runtime, Rope str, Encoding enc, Encoding[] fixedEnc,
            RegexpSupport.ErrorMode mode) {
        RopeBuilder to = RopeBuilder.createRopeBuilder(str.byteLength());

        if (enc.isAsciiCompatible()) {
            fixedEnc[0] = null;
        } else {
            fixedEnc[0] = enc;
            to.setEncoding(enc);
        }

        boolean hasProperty = unescapeNonAscii(runtime, to, str, enc, fixedEnc, mode);
        if (hasProperty && fixedEnc[0] == null) {
            fixedEnc[0] = enc;
        }
        if (fixedEnc[0] != null) {
            to.setEncoding(fixedEnc[0]);
        }
        return to;
    }

    private static void preprocessLight(RubyContext context, Rope str, Encoding enc, Encoding[] fixedEnc,
            RegexpSupport.ErrorMode mode) {
        if (enc.isAsciiCompatible()) {
            fixedEnc[0] = null;
        } else {
            fixedEnc[0] = enc;
        }

        boolean hasProperty = unescapeNonAscii(context, null, str, enc, fixedEnc, mode);
        if (hasProperty && fixedEnc[0] == null) {
            fixedEnc[0] = enc;
        }
    }

    public static RopeBuilder preprocessDRegexp(RubyContext context, Rope[] strings, RegexpOptions options) {
        assert strings.length > 0;

        RopeBuilder string = RopeOperations.toRopeBuilderCopy(strings[0]);

        Encoding regexpEnc = processDRegexpElement(context, options, null, strings[0]);

        for (int i = 1; i < strings.length; i++) {
            Rope str = strings[i];
            regexpEnc = processDRegexpElement(context, options, regexpEnc, str);
            string.append(str);
        }

        if (regexpEnc != null) {
            string.setEncoding(regexpEnc);
        }

        return string;
    }

    @TruffleBoundary
    private static Encoding processDRegexpElement(RubyContext context, RegexpOptions options, Encoding regexpEnc,
            Rope str) {
        Encoding strEnc = str.getEncoding();

        if (options.isEncodingNone() && strEnc != ASCIIEncoding.INSTANCE) {
            if (str.getCodeRange() != CR_7BIT) {
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().regexpError(
                                "/.../n has a non escaped non ASCII character in non ASCII-8BIT script",
                                null));
            }
            strEnc = ASCIIEncoding.INSTANCE;
        }

        // This used to call preprocess, but the resulting rope builder was not
        // used. Since the preprocessing error-checking can be done without
        // creating a new rope builder, I added a "light" path.
        final Encoding[] fixedEnc = new Encoding[]{ null };
        ClassicRegexp.preprocessLight(context, str, strEnc, fixedEnc, RegexpSupport.ErrorMode.PREPROCESS);

        if (fixedEnc[0] != null) {
            if (regexpEnc != null && regexpEnc != fixedEnc[0]) {
                throw new RaiseException(
                        context,
                        context
                                .getCoreExceptions()
                                .regexpError(
                                        "encoding mismatch in dynamic regexp: " + regexpEnc + " and " + fixedEnc[0],
                                        null));
            }
            regexpEnc = fixedEnc[0];
        }
        return regexpEnc;
    }

    private static final int QUOTED_V = 11;

    /** rb_reg_quote */
    @TruffleBoundary
    public static Rope quote19(Rope bs) {
        final boolean asciiOnly = bs.isAsciiOnly();
        int p = 0;
        int end = bs.byteLength();
        final byte[] bytes = bs.getBytes();
        final Encoding enc = bs.getEncoding();
        final CodeRange cr = bs.getCodeRange();

        metaFound: do {
            while (p < end) {
                final int c;
                final int cl;
                if (enc.isAsciiCompatible()) {
                    cl = 1;
                    c = bytes[p] & 0xff;
                } else {
                    cl = StringSupport.characterLength(enc, cr, bytes, p, end);
                    c = enc.mbcToCode(bytes, p, end);
                }

                if (!Encoding.isAscii(c)) {
                    p += StringSupport.characterLength(enc, cr, bytes, p, end, true);
                    continue;
                }

                switch (c) {
                    case '[':
                    case ']':
                    case '{':
                    case '}':
                    case '(':
                    case ')':
                    case '|':
                    case '-':
                    case '*':
                    case '.':
                    case '\\':
                    case '?':
                    case '+':
                    case '^':
                    case '$':
                    case ' ':
                    case '#':
                    case '\t':
                    case '\f':
                    case QUOTED_V:
                    case '\n':
                    case '\r':
                        break metaFound;
                }
                p += cl;
            }
            if (asciiOnly) {
                return RopeOperations.withEncoding(bs, USASCIIEncoding.INSTANCE);
            }
            return bs;
        } while (false);

        RopeBuilder result = RopeBuilder.createRopeBuilder(end * 2);
        result.setEncoding(asciiOnly ? USASCIIEncoding.INSTANCE : bs.getEncoding());
        byte[] obytes = result.getUnsafeBytes();
        int op = p;
        System.arraycopy(bytes, 0, obytes, 0, op);

        while (p < end) {
            final int c;
            final int cl;
            if (enc.isAsciiCompatible()) {
                cl = 1;
                c = bytes[p] & 0xff;
            } else {
                cl = StringSupport.characterLength(enc, cr, bytes, p, end);
                c = enc.mbcToCode(bytes, p, end);
            }

            if (!Encoding.isAscii(c)) {
                int n = StringSupport.characterLength(enc, cr, bytes, p, end, true);
                while (n-- > 0) {
                    obytes[op++] = bytes[p++];
                }
                continue;
            }
            p += cl;
            switch (c) {
                case '[':
                case ']':
                case '{':
                case '}':
                case '(':
                case ')':
                case '|':
                case '-':
                case '*':
                case '.':
                case '\\':
                case '?':
                case '+':
                case '^':
                case '$':
                case '#':
                    op += enc.codeToMbc('\\', obytes, op);
                    break;
                case ' ':
                    op += enc.codeToMbc('\\', obytes, op);
                    op += enc.codeToMbc(' ', obytes, op);
                    continue;
                case '\t':
                    op += enc.codeToMbc('\\', obytes, op);
                    op += enc.codeToMbc('t', obytes, op);
                    continue;
                case '\n':
                    op += enc.codeToMbc('\\', obytes, op);
                    op += enc.codeToMbc('n', obytes, op);
                    continue;
                case '\r':
                    op += enc.codeToMbc('\\', obytes, op);
                    op += enc.codeToMbc('r', obytes, op);
                    continue;
                case '\f':
                    op += enc.codeToMbc('\\', obytes, op);
                    op += enc.codeToMbc('f', obytes, op);
                    continue;
                case QUOTED_V:
                    op += enc.codeToMbc('\\', obytes, op);
                    op += enc.codeToMbc('v', obytes, op);
                    continue;
            }
            op += enc.codeToMbc(c, obytes, op);
        }

        result.setLength(op);
        return RopeOperations.ropeFromRopeBuilder(result);
    }

    /** WARNING: This mutates options, so the caller should make sure it's a copy */
    static Encoding computeRegexpEncoding(RegexpOptions options, Encoding enc, Encoding[] fixedEnc,
            RubyContext context) {
        if (fixedEnc[0] != null) {
            if ((fixedEnc[0] != enc && options.isFixed()) ||
                    (fixedEnc[0] != ASCIIEncoding.INSTANCE && options.isEncodingNone())) {
                throw new RaiseException(
                        context,
                        context.getCoreExceptions().regexpError("incompatible character encoding", null));
            }
            if (fixedEnc[0] != ASCIIEncoding.INSTANCE) {
                options.setFixed(true);
                enc = fixedEnc[0];
            }
        } else if (!options.isFixed()) {
            enc = USASCIIEncoding.INSTANCE;
        }

        if (fixedEnc[0] != null) {
            options.setFixed(true);
        }
        return enc;
    }

    public static void appendOptions(RopeBuilder to, RegexpOptions options) {
        if (options.isMultiline()) {
            to.append((byte) 'm');
        }
        if (options.isIgnorecase()) {
            to.append((byte) 'i');
        }
        if (options.isExtended()) {
            to.append((byte) 'x');
        }
    }

    @SuppressWarnings("unused")
    public RopeBuilder toRopeBuilder() {
        RegexpOptions newOptions = (RegexpOptions) options.clone();
        int p = 0;
        int len = str.byteLength();
        byte[] bytes = str.getBytes();

        RopeBuilder result = RopeBuilder.createRopeBuilder(len);
        result.append((byte) '(');
        result.append((byte) '?');

        again: do {
            if (len >= 4 && bytes[p] == '(' && bytes[p + 1] == '?') {
                boolean err = true;
                p += 2;
                if ((len -= 2) > 0) {
                    do {
                        if (bytes[p] == 'm') {
                            newOptions.setMultiline(true);
                        } else if (bytes[p] == 'i') {
                            newOptions.setIgnorecase(true);
                        } else if (bytes[p] == 'x') {
                            newOptions.setExtended(true);
                        } else {
                            break;
                        }
                        p++;
                    } while (--len > 0);
                }
                if (len > 1 && bytes[p] == '-') {
                    ++p;
                    --len;
                    do {
                        if (bytes[p] == 'm') {
                            newOptions.setMultiline(false);
                        } else if (bytes[p] == 'i') {
                            newOptions.setIgnorecase(false);
                        } else if (bytes[p] == 'x') {
                            newOptions.setExtended(false);
                        } else {
                            break;
                        }
                        p++;
                    } while (--len > 0);
                }

                if (bytes[p] == ')') {
                    --len;
                    ++p;
                    continue again;
                }

                if (bytes[p] == ':' && bytes[p + len - 1] == ')') {
                    try {
                        new Regex(
                                bytes,
                                ++p,
                                p + (len -= 2),
                                Option.DEFAULT,
                                str.getEncoding(),
                                Syntax.DEFAULT,
                                new RegexWarnCallback(context));
                        err = false;
                    } catch (JOniException e) {
                        err = true;
                    }
                }

                if (err) {
                    newOptions = options;
                    p = 0;
                    len = str.byteLength();
                }
            }

            appendOptions(result, newOptions);

            if (!newOptions.isEmbeddable()) {
                result.append((byte) '-');
                if (!newOptions.isMultiline()) {
                    result.append((byte) 'm');
                }
                if (!newOptions.isIgnorecase()) {
                    result.append((byte) 'i');
                }
                if (!newOptions.isExtended()) {
                    result.append((byte) 'x');
                }
            }
            result.append((byte) ':');
            appendRegexpString19(result, str, p, len, null);

            result.append((byte) ')');
            result.setEncoding(getEncoding());
            return result;
            //return RubyString.newString(getRuntime(), result, getEncoding()).infectBy(this);
        } while (true);
    }

    @TruffleBoundary
    public void appendRegexpString19(RopeBuilder to, Rope str, int start, int len, Encoding resEnc) {
        int p = start;
        int end = p + len;

        final CodeRange cr = str.getCodeRange();
        final Encoding enc = str.getEncoding();
        final byte[] bytes = str.getBytes();
        boolean needEscape = false;
        while (p < end) {
            final int c;
            final int cl;
            if (enc.isAsciiCompatible()) {
                cl = 1;
                c = bytes[p] & 0xff;
            } else {
                cl = StringSupport.characterLength(enc, cr, bytes, p, end);
                c = enc.mbcToCode(bytes, p, end);
            }

            if (!Encoding.isAscii(c)) {
                p += StringSupport.characterLength(enc, cr, bytes, p, end, true);
            } else if (c != '/' && enc.isPrint(c)) {
                p += cl;
            } else {
                needEscape = true;
                break;
            }
        }
        if (!needEscape) {
            to.append(bytes, start, len);
        } else {
            p = start;
            while (p < end) {
                final int c;
                final int cl;
                if (enc.isAsciiCompatible()) {
                    cl = 1;
                    c = bytes[p] & 0xff;
                } else {
                    cl = StringSupport.characterLength(enc, cr, bytes, p, end);
                    c = enc.mbcToCode(bytes, p, end);
                }

                if (c == '\\' && p + cl < end) {
                    int n = cl + StringSupport.characterLength(enc, cr, bytes, p + cl, end);
                    to.append(bytes, p, n);
                    p += n;
                    continue;
                } else if (c == '/') {
                    to.append((byte) '\\');
                    to.append(bytes, p, cl);
                } else if (!Encoding.isAscii(c)) {
                    int l = StringSupport.characterLength(enc, cr, bytes, p, end);
                    if (l <= 0) {
                        l = 1;
                        to.append(StringUtils.formatASCIIBytes("\\x%02X", c));
                    } else if (resEnc != null) {
                        int code = enc.mbcToCode(bytes, p, end);
                        to.append(
                                String.format(StringSupport.escapedCharFormat(code, enc.isUnicode()), code).getBytes(
                                        StandardCharsets.US_ASCII));
                    } else {
                        to.append(bytes, p, l);
                    }
                    p += l;

                    continue;
                } else if (enc.isPrint(c)) {
                    to.append(bytes, p, cl);
                } else if (!enc.isSpace(c)) {
                    to.append(StringUtils.formatASCIIBytes("\\x%02X", c));
                } else {
                    to.append(bytes, p, cl);
                }
                p += cl;
            }
        }
    }

    public String[] getNames() {
        int nameLength = pattern.numberOfNames();
        if (nameLength == 0) {
            return EMPTY_STRING_ARRAY;
        }

        String[] names = new String[nameLength];
        int j = 0;
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            //intern() to improve footprint
            names[j++] = new String(e.name, e.nameP, e.nameEnd - e.nameP, pattern.getEncoding().getCharset()).intern();
        }

        return names;
    }

}
