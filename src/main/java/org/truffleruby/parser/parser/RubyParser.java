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
 * Copyright (C) 2008-2017 Thomas E Enebo <enebo@acm.org>
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
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 ***** END LICENSE BLOCK *****/
// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

// line 2 "RubyParser.y"
package org.truffleruby.parser.parser;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.RubyContext;
import org.truffleruby.SuppressFBWarnings;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.rope.Rope;
import org.truffleruby.core.rope.RopeConstants;
import org.truffleruby.core.rope.RopeOperations;
import org.truffleruby.core.string.StringOperations;
import org.truffleruby.language.SourceIndexLength;
import org.truffleruby.parser.RubyWarnings;
import org.truffleruby.parser.TranslatorEnvironment;
import org.truffleruby.parser.ast.ArgsParseNode;
import org.truffleruby.parser.ast.ArgumentParseNode;
import org.truffleruby.parser.ast.ArrayParseNode;
import org.truffleruby.parser.ast.AssignableParseNode;
import org.truffleruby.parser.ast.BackRefParseNode;
import org.truffleruby.parser.ast.BeginParseNode;
import org.truffleruby.parser.ast.BlockAcceptingParseNode;
import org.truffleruby.parser.ast.BlockArgParseNode;
import org.truffleruby.parser.ast.BlockParseNode;
import org.truffleruby.parser.ast.BlockPassParseNode;
import org.truffleruby.parser.ast.BreakParseNode;
import org.truffleruby.parser.ast.ClassParseNode;
import org.truffleruby.parser.ast.ClassVarAsgnParseNode;
import org.truffleruby.parser.ast.ClassVarParseNode;
import org.truffleruby.parser.ast.Colon3ParseNode;
import org.truffleruby.parser.ast.ConstDeclParseNode;
import org.truffleruby.parser.ast.ConstParseNode;
import org.truffleruby.parser.ast.DStrParseNode;
import org.truffleruby.parser.ast.DSymbolParseNode;
import org.truffleruby.parser.ast.DXStrParseNode;
import org.truffleruby.parser.ast.DefnParseNode;
import org.truffleruby.parser.ast.DefsParseNode;
import org.truffleruby.parser.ast.DotParseNode;
import org.truffleruby.parser.ast.EncodingParseNode;
import org.truffleruby.parser.ast.EnsureParseNode;
import org.truffleruby.parser.ast.EvStrParseNode;
import org.truffleruby.parser.ast.FCallParseNode;
import org.truffleruby.parser.ast.FalseParseNode;
import org.truffleruby.parser.ast.FileParseNode;
import org.truffleruby.parser.ast.FixnumParseNode;
import org.truffleruby.parser.ast.FloatParseNode;
import org.truffleruby.parser.ast.ForParseNode;
import org.truffleruby.parser.ast.GlobalAsgnParseNode;
import org.truffleruby.parser.ast.GlobalVarParseNode;
import org.truffleruby.parser.ast.HashParseNode;
import org.truffleruby.parser.ast.IfParseNode;
import org.truffleruby.parser.ast.InstAsgnParseNode;
import org.truffleruby.parser.ast.InstVarParseNode;
import org.truffleruby.parser.ast.IterParseNode;
import org.truffleruby.parser.ast.LambdaParseNode;
import org.truffleruby.parser.ast.ListParseNode;
import org.truffleruby.parser.ast.LiteralParseNode;
import org.truffleruby.parser.ast.ModuleParseNode;
import org.truffleruby.parser.ast.MultipleAsgnParseNode;
import org.truffleruby.parser.ast.NextParseNode;
import org.truffleruby.parser.ast.NilImplicitParseNode;
import org.truffleruby.parser.ast.NilParseNode;
import org.truffleruby.parser.ast.NonLocalControlFlowParseNode;
import org.truffleruby.parser.ast.NumericParseNode;
import org.truffleruby.parser.ast.OpAsgnAndParseNode;
import org.truffleruby.parser.ast.OpAsgnOrParseNode;
import org.truffleruby.parser.ast.OptArgParseNode;
import org.truffleruby.parser.ast.ParseNode;
import org.truffleruby.parser.ast.PostExeParseNode;
import org.truffleruby.parser.ast.PreExe19ParseNode;
import org.truffleruby.parser.ast.RationalParseNode;
import org.truffleruby.parser.ast.RedoParseNode;
import org.truffleruby.parser.ast.RegexpParseNode;
import org.truffleruby.parser.ast.RequiredKeywordArgumentValueParseNode;
import org.truffleruby.parser.ast.RescueBodyParseNode;
import org.truffleruby.parser.ast.RescueParseNode;
import org.truffleruby.parser.ast.RestArgParseNode;
import org.truffleruby.parser.ast.RetryParseNode;
import org.truffleruby.parser.ast.ReturnParseNode;
import org.truffleruby.parser.ast.SClassParseNode;
import org.truffleruby.parser.ast.SelfParseNode;
import org.truffleruby.parser.ast.StarParseNode;
import org.truffleruby.parser.ast.StrParseNode;
import org.truffleruby.parser.ast.TrueParseNode;
import org.truffleruby.parser.ast.UnnamedRestArgParseNode;
import org.truffleruby.parser.ast.UntilParseNode;
import org.truffleruby.parser.ast.VAliasParseNode;
import org.truffleruby.parser.ast.WhileParseNode;
import org.truffleruby.parser.ast.XStrParseNode;
import org.truffleruby.parser.ast.YieldParseNode;
import org.truffleruby.parser.ast.ZArrayParseNode;
import org.truffleruby.parser.ast.ZSuperParseNode;
import org.truffleruby.parser.ast.types.ILiteralNode;
import org.truffleruby.parser.lexer.LexerSource;
import org.truffleruby.parser.lexer.RubyLexer;
import org.truffleruby.parser.lexer.StrTerm;
import org.truffleruby.parser.lexer.SyntaxException.PID;

import static org.truffleruby.core.rope.CodeRange.CR_UNKNOWN;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_BEG;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_END;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_ENDARG;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_ENDFN;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_FITEM;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_FNAME;
import static org.truffleruby.parser.lexer.RubyLexer.EXPR_LABEL;
import static org.truffleruby.parser.parser.ParserSupport.value_expr;

// @formatter:off
// CheckStyle: start generated
@SuppressFBWarnings("IP")
@SuppressWarnings({"unchecked", "fallthrough", "cast"})
public class RubyParser {
    protected final ParserSupport support;
    protected final RubyLexer lexer;

    public RubyParser(RubyContext context, LexerSource source, RubyWarnings warnings) {
        this.support = new ParserSupport(context, source, warnings);
        this.lexer = new RubyLexer(support, source, warnings);
        support.setLexer(lexer);
    }
// line 126 "-"
  // %token constants
  public static final int keyword_class = 257;
  public static final int keyword_module = 258;
  public static final int keyword_def = 259;
  public static final int keyword_undef = 260;
  public static final int keyword_begin = 261;
  public static final int keyword_rescue = 262;
  public static final int keyword_ensure = 263;
  public static final int keyword_end = 264;
  public static final int keyword_if = 265;
  public static final int keyword_unless = 266;
  public static final int keyword_then = 267;
  public static final int keyword_elsif = 268;
  public static final int keyword_else = 269;
  public static final int keyword_case = 270;
  public static final int keyword_when = 271;
  public static final int keyword_while = 272;
  public static final int keyword_until = 273;
  public static final int keyword_for = 274;
  public static final int keyword_break = 275;
  public static final int keyword_next = 276;
  public static final int keyword_redo = 277;
  public static final int keyword_retry = 278;
  public static final int keyword_in = 279;
  public static final int keyword_do = 280;
  public static final int keyword_do_cond = 281;
  public static final int keyword_do_block = 282;
  public static final int keyword_return = 283;
  public static final int keyword_yield = 284;
  public static final int keyword_super = 285;
  public static final int keyword_self = 286;
  public static final int keyword_nil = 287;
  public static final int keyword_true = 288;
  public static final int keyword_false = 289;
  public static final int keyword_and = 290;
  public static final int keyword_or = 291;
  public static final int keyword_not = 292;
  public static final int modifier_if = 293;
  public static final int modifier_unless = 294;
  public static final int modifier_while = 295;
  public static final int modifier_until = 296;
  public static final int modifier_rescue = 297;
  public static final int keyword_alias = 298;
  public static final int keyword_defined = 299;
  public static final int keyword_BEGIN = 300;
  public static final int keyword_END = 301;
  public static final int keyword__LINE__ = 302;
  public static final int keyword__FILE__ = 303;
  public static final int keyword__ENCODING__ = 304;
  public static final int keyword_do_lambda = 305;
  public static final int tIDENTIFIER = 306;
  public static final int tFID = 307;
  public static final int tGVAR = 308;
  public static final int tIVAR = 309;
  public static final int tCONSTANT = 310;
  public static final int tCVAR = 311;
  public static final int tLABEL = 312;
  public static final int tCHAR = 313;
  public static final int tUPLUS = 314;
  public static final int tUMINUS = 315;
  public static final int tUMINUS_NUM = 316;
  public static final int tPOW = 317;
  public static final int tCMP = 318;
  public static final int tEQ = 319;
  public static final int tEQQ = 320;
  public static final int tNEQ = 321;
  public static final int tGEQ = 322;
  public static final int tLEQ = 323;
  public static final int tANDOP = 324;
  public static final int tOROP = 325;
  public static final int tMATCH = 326;
  public static final int tNMATCH = 327;
  public static final int tDOT = 328;
  public static final int tDOT2 = 329;
  public static final int tDOT3 = 330;
  public static final int tAREF = 331;
  public static final int tASET = 332;
  public static final int tLSHFT = 333;
  public static final int tRSHFT = 334;
  public static final int tANDDOT = 335;
  public static final int tCOLON2 = 336;
  public static final int tCOLON3 = 337;
  public static final int tOP_ASGN = 338;
  public static final int tASSOC = 339;
  public static final int tLPAREN = 340;
  public static final int tLPAREN2 = 341;
  public static final int tRPAREN = 342;
  public static final int tLPAREN_ARG = 343;
  public static final int tLBRACK = 344;
  public static final int tRBRACK = 345;
  public static final int tLBRACE = 346;
  public static final int tLBRACE_ARG = 347;
  public static final int tSTAR = 348;
  public static final int tSTAR2 = 349;
  public static final int tAMPER = 350;
  public static final int tAMPER2 = 351;
  public static final int tTILDE = 352;
  public static final int tPERCENT = 353;
  public static final int tDIVIDE = 354;
  public static final int tPLUS = 355;
  public static final int tMINUS = 356;
  public static final int tLT = 357;
  public static final int tGT = 358;
  public static final int tPIPE = 359;
  public static final int tBANG = 360;
  public static final int tCARET = 361;
  public static final int tLCURLY = 362;
  public static final int tRCURLY = 363;
  public static final int tBACK_REF2 = 364;
  public static final int tSYMBEG = 365;
  public static final int tSTRING_BEG = 366;
  public static final int tXSTRING_BEG = 367;
  public static final int tREGEXP_BEG = 368;
  public static final int tWORDS_BEG = 369;
  public static final int tQWORDS_BEG = 370;
  public static final int tSTRING_DBEG = 371;
  public static final int tSTRING_DVAR = 372;
  public static final int tSTRING_END = 373;
  public static final int tLAMBDA = 374;
  public static final int tLAMBEG = 375;
  public static final int tNTH_REF = 376;
  public static final int tBACK_REF = 377;
  public static final int tSTRING_CONTENT = 378;
  public static final int tINTEGER = 379;
  public static final int tIMAGINARY = 380;
  public static final int tFLOAT = 381;
  public static final int tRATIONAL = 382;
  public static final int tREGEXP_END = 383;
  public static final int tSYMBOLS_BEG = 384;
  public static final int tQSYMBOLS_BEG = 385;
  public static final int tDSTAR = 386;
  public static final int tSTRING_DEND = 387;
  public static final int tLABEL_END = 388;
  public static final int tLOWEST = 389;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 652
    -1,   152,     0,   139,   140,   140,   140,   140,   141,   141,
    37,    36,    38,    38,    38,    38,    44,   155,    44,   156,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    31,    31,    31,    31,
    31,    31,    31,    31,    61,    61,    61,    40,    40,    40,
    40,    40,    40,    45,    32,    32,    60,    60,   113,   148,
    43,    43,    43,    43,    43,    43,    43,    43,    43,    43,
    43,   116,   116,   127,   127,   117,   117,   117,   117,   117,
   117,   117,   117,   117,   117,    74,    74,   103,   103,   104,
   104,    75,    75,    75,    75,    75,    75,    75,    75,    75,
    75,    75,    75,    75,    75,    75,    75,    75,    75,    75,
    80,    80,    80,    80,    80,    80,    80,    80,    80,    80,
    80,    80,    80,    80,    80,    80,    80,    80,    80,     8,
     8,    30,    30,    30,     7,     7,     7,     7,     7,   120,
   120,   121,   121,    64,   158,    64,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,   136,
   136,   136,   136,    51,    51,    76,    79,    79,    79,    79,
    62,    62,    54,    58,    58,   130,   130,   130,   130,   130,
    52,    52,    52,    52,    52,   160,    56,   107,   106,   106,
    82,    82,    82,    82,    35,    35,    73,    73,    73,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
   161,    42,   162,    42,   163,   164,    42,    42,    42,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,    42,   166,   168,    42,   169,   170,
    42,    42,    42,   171,   172,    42,   173,    42,   175,    42,
   176,    42,   177,   178,    42,   179,   180,    42,    42,    42,
    42,    42,    46,   150,   151,   149,   165,   165,   165,   167,
   167,    49,    49,    47,    47,   129,   129,   131,   131,    87,
    87,   132,   132,   132,   132,   132,   132,   132,   132,   132,
    94,    94,    94,    94,    93,    93,    69,    69,    69,    69,
    69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
    69,    71,    71,    70,    70,    70,   124,   124,   123,   123,
   133,   133,   181,   182,   126,    68,    68,   125,   125,   112,
    59,    59,    59,    59,    22,    22,    22,    22,    22,    22,
    22,    22,    22,   111,   111,   183,   184,   114,   185,   186,
   115,    77,    48,    48,   118,   118,    78,    78,    78,    50,
    50,    53,    53,    28,    28,    28,    15,    16,    16,    16,
    17,    18,    19,    25,    84,    84,    27,    27,    90,    88,
    88,    26,    91,    83,    83,    89,    89,    20,    20,    21,
    21,    24,    24,    23,   187,    23,   188,   189,   190,   191,
   192,    23,    65,    65,    65,    65,     2,     1,     1,     1,
     1,    29,    33,    33,    34,    34,    34,    34,    57,    57,
    57,    57,    57,    57,    57,    57,    57,    57,    57,    57,
   119,   119,   119,   119,   119,   119,   119,   119,   119,   119,
   119,   119,    66,    66,   193,    55,    55,    72,   194,    72,
    95,    95,    95,    95,    92,    92,    67,    67,    67,    67,
    67,    67,    67,    67,    67,    67,    67,    67,    67,    67,
    67,   135,   135,   135,   135,     9,     9,   147,   122,   122,
    85,    85,   144,    96,    96,    97,    97,    98,    98,    99,
    99,   142,   142,   143,   143,    63,   128,   105,   105,    86,
    86,    10,    10,    13,    13,    12,    12,   110,   109,   109,
    14,   195,    14,   100,   100,   101,   101,   102,   102,   102,
   102,     3,     3,     3,     4,     4,     4,     4,     5,     5,
     5,    11,    11,   145,   145,   146,   146,   153,   153,   157,
   157,   137,   138,   159,   159,   159,   174,   174,   154,   154,
    81,   108,
    }, yyLen = {
//yyLen 652
     2,     0,     2,     2,     1,     1,     3,     2,     1,     4,
     4,     2,     1,     1,     3,     2,     1,     0,     5,     0,
     4,     3,     3,     3,     2,     3,     3,     3,     3,     3,
     4,     1,     3,     3,     3,     1,     3,     3,     6,     5,
     5,     5,     5,     3,     1,     3,     1,     1,     3,     3,
     3,     2,     1,     1,     1,     1,     1,     4,     3,     1,
     2,     3,     4,     5,     4,     5,     2,     2,     2,     2,
     2,     1,     3,     1,     3,     1,     2,     3,     5,     2,
     4,     2,     4,     1,     3,     1,     3,     2,     3,     1,
     3,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     4,     3,     3,     3,     3,     2,     1,     1,
     1,     2,     1,     3,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     0,     4,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     3,     3,
     6,     5,     5,     5,     5,     4,     3,     3,     2,     3,
     2,     3,     3,     3,     3,     3,     3,     4,     2,     2,
     3,     3,     3,     3,     1,     3,     3,     3,     3,     3,
     2,     2,     3,     3,     3,     3,     3,     6,     1,     1,
     1,     1,     1,     3,     3,     1,     1,     2,     4,     2,
     1,     3,     3,     1,     1,     1,     1,     2,     4,     2,
     1,     2,     2,     4,     1,     0,     2,     2,     2,     1,
     1,     2,     3,     4,     1,     1,     3,     4,     2,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     4,     0,     3,     0,     0,     5,     3,     3,     2,
     3,     3,     1,     4,     3,     1,     5,     4,     3,     2,
     1,     2,     2,     6,     6,     0,     0,     7,     0,     0,
     7,     5,     4,     0,     0,     9,     0,     6,     0,     7,
     0,     5,     0,     0,     7,     0,     0,     9,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     2,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     3,     1,
     3,     1,     4,     6,     3,     5,     2,     4,     1,     3,
     4,     2,     2,     1,     2,     0,     6,     8,     4,     6,
     4,     2,     6,     2,     4,     6,     2,     4,     2,     4,
     1,     1,     1,     3,     1,     4,     1,     4,     1,     3,
     1,     1,     0,     0,     4,     4,     1,     3,     3,     3,
     2,     4,     5,     5,     2,     4,     4,     3,     3,     3,
     2,     1,     4,     3,     3,     0,     0,     4,     0,     0,
     4,     5,     1,     1,     6,     0,     1,     1,     1,     2,
     1,     2,     1,     1,     1,     1,     1,     1,     1,     2,
     3,     3,     3,     4,     0,     3,     1,     2,     4,     0,
     3,     4,     4,     0,     3,     0,     3,     0,     2,     0,
     2,     0,     2,     1,     0,     3,     0,     0,     0,     0,
     0,     8,     1,     1,     1,     1,     2,     1,     1,     1,
     1,     3,     1,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     0,     4,     0,     3,     0,     3,
     4,     2,     2,     1,     2,     0,     6,     8,     4,     6,
     4,     6,     2,     4,     6,     2,     4,     2,     4,     1,
     0,     1,     1,     1,     1,     1,     1,     1,     1,     3,
     1,     3,     1,     2,     1,     2,     1,     1,     3,     1,
     3,     1,     1,     2,     1,     3,     3,     1,     3,     1,
     3,     1,     1,     2,     1,     1,     1,     2,     2,     0,
     1,     0,     4,     1,     2,     1,     3,     3,     2,     4,
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     0,     1,     0,
     1,     2,     2,     0,     1,     1,     1,     1,     1,     2,
     0,     0,
    }, yyDefRed = {
//yyDefRed 1107
     1,     0,     0,     0,   363,   364,     0,     0,   310,     0,
     0,     0,   335,   338,     0,     0,     0,   360,   361,   365,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   467,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   487,   489,   491,     0,     0,   422,   542,
   543,   514,   517,   515,   516,     0,     0,   464,    59,   300,
     0,   468,   301,   302,     0,   303,   304,   299,   465,    31,
    47,   463,   512,     0,     0,     0,     0,     0,     0,     0,
   307,     0,    55,     0,     0,    85,     0,     4,   305,   306,
     0,     0,    71,     0,     2,     0,     5,     0,     0,     0,
     0,     7,   186,   197,   187,   210,   183,   203,   193,   192,
   213,   214,   208,   191,   190,   185,   211,   215,   216,   195,
   184,   198,   202,   204,   196,   189,   205,   212,   207,     0,
     0,     0,     0,   182,   201,   200,   217,   181,   188,   179,
   180,     0,     0,     0,     0,   136,   520,   519,     0,   522,
   171,   172,   168,   149,   150,   151,   158,   155,   157,   152,
   153,   173,   174,   159,   160,   611,   165,   164,   148,   170,
   167,   166,   162,   163,   156,   154,   146,   169,   147,   175,
   161,   137,   352,     0,   610,   138,   206,   199,   209,   194,
   176,   177,   178,   134,   135,   140,   139,   142,     0,   141,
   143,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   646,   647,     0,     0,     0,   648,     0,
     0,   358,   359,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   362,     0,     0,   375,   376,     0,     0,   322,     0,
     0,     0,     0,   487,     0,     0,   280,    69,     0,     0,
     0,   615,   284,    70,     0,    67,     0,     0,   440,    66,
     0,   640,     0,     0,    19,     0,     0,     0,   238,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    13,
    12,     0,     0,     0,     0,     0,   266,     0,     0,     0,
   613,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   251,    51,   250,   509,   508,   510,   506,   507,     0,     0,
     0,     0,   474,   483,   332,     0,   479,   485,   469,   448,
   445,   331,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   261,   262,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   260,   259,     0,     0,     0,     0,   448,   430,   633,   634,
     0,     0,     0,     0,   636,   635,     0,     0,    87,     0,
     0,     0,     0,     0,     0,     3,     0,   434,     0,   329,
    68,   524,   523,   525,   526,   528,   527,   529,     0,     0,
     0,     0,   132,     0,     0,   308,   350,     0,   353,   631,
   632,   355,   144,     0,     0,     0,   367,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   649,
     0,     0,     0,   513,     0,     0,     0,     0,   343,   618,
   291,   287,     0,   620,     0,     0,   281,   289,     0,   282,
     0,   324,     0,   286,   276,   275,     0,     0,     0,     0,
   328,    50,    21,    23,    22,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   317,    11,     0,
     0,   313,     0,   320,     0,   644,   267,     0,   269,   321,
   614,     0,    89,     0,     0,     0,     0,     0,   496,   494,
   511,   493,   490,   470,   488,   471,   472,   492,     0,     0,
   576,   573,   572,   571,   574,   582,   591,     0,     0,   602,
   601,   606,   605,   592,   577,     0,     0,     0,   599,   426,
   423,     0,     0,   569,   589,     0,   553,   580,   575,     0,
     0,     0,     0,     0,     0,     0,   449,     0,   446,    25,
    26,    27,    28,    29,    48,    49,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   437,     0,   439,     0,     0,   626,
     0,     0,   627,   438,     0,   624,   625,     0,    46,     0,
     0,     0,    43,   226,     0,     0,     0,     0,    36,   218,
    33,   290,     0,     0,     0,     0,    88,    32,    34,   294,
     0,    37,   219,     6,   445,    61,     0,   129,     0,   131,
   544,   346,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   311,     0,   368,     0,     0,     0,     0,     0,
     0,     0,     0,   342,   370,   336,   369,   339,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   617,     0,     0,
     0,   288,   616,   323,   641,     0,     0,   272,   327,    20,
     0,     9,    30,     0,   225,     0,     0,    14,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   497,     0,   473,
   476,     0,   481,     0,     0,     0,   377,     0,   379,     0,
     0,   603,   607,     0,   567,     0,     0,   562,     0,   565,
     0,   551,   593,     0,   552,   583,     0,   478,     0,   482,
     0,   444,     0,   443,     0,     0,   429,     0,     0,   436,
     0,     0,     0,     0,     0,   274,     0,   435,   273,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    86,
     0,     0,     0,     0,     0,     0,     0,     0,   133,     0,
     0,   612,     0,     0,     0,   356,   145,   457,     0,     0,
   458,     0,     0,   373,     0,   371,     0,     0,     0,     0,
     0,     0,     0,   341,     0,     0,     0,     0,     0,     0,
   619,   293,   283,     0,   326,     0,   316,   268,    90,     0,
   498,   502,   503,   504,   495,   505,   475,   477,   484,     0,
     0,     0,     0,   579,     0,     0,     0,   554,   578,     0,
     0,   424,     0,     0,   581,     0,   600,     0,   590,   608,
     0,   595,   480,   486,   414,     0,   412,     0,   411,     0,
     0,    42,   223,    41,   224,    65,     0,   642,    39,   221,
    40,   222,    63,   433,   432,    45,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    58,     0,     0,     0,   442,
   351,     0,     0,     0,     0,     0,     0,   460,   374,     0,
    10,   462,     0,   333,     0,   334,   292,     0,     0,     0,
   344,     0,    18,   499,   378,     0,     0,     0,   380,   425,
     0,     0,   568,     0,     0,     0,   560,     0,   558,     0,
   563,   566,   550,     0,     0,     0,   410,   587,     0,     0,
   393,     0,   597,     0,     0,     0,   450,   447,     0,     0,
    38,     0,     0,     0,   545,   347,   547,   354,   549,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   459,     0,   461,     0,   452,
   451,   453,   337,   340,     0,   500,     0,     0,     0,     0,
   420,     0,   418,   421,   428,   427,     0,     0,     0,     0,
     0,   408,     0,     0,   403,     0,   391,     0,   406,   413,
   392,     0,     0,     0,     0,     0,   349,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   417,
   561,     0,   556,   559,   564,     0,   394,   415,     0,     0,
   588,     0,     0,     0,   598,   319,     0,     0,   357,     0,
     0,     0,     0,     0,   454,   372,   345,     0,     0,   419,
     0,     0,   409,     0,   400,     0,   398,   390,     0,   404,
   407,     0,     0,     0,   501,   557,     0,     0,     0,     0,
   402,     0,   396,   399,   405,     0,   397,
    }, yyDgoto = {
//yyDgoto 196
     1,   346,    67,    68,   690,   614,   615,   206,   432,   554,
   555,   441,   556,   557,   193,    69,    70,    71,    72,    73,
   349,   348,    74,   532,   351,    75,    76,   731,    77,    78,
   433,    79,    80,    81,    82,   648,   443,   444,   307,   308,
    84,    85,    86,    87,   309,   227,   299,   815,  1000,   816,
   916,    89,   484,   920,   616,   661,   285,    90,   777,    91,
    92,   638,   639,   558,   208,   844,   229,   559,   560,   954,
   876,   877,   803,   640,    94,    95,   278,   458,   809,   315,
   230,   310,   486,   539,   538,   561,   562,   737,   573,   574,
    98,    99,   744,  1021,  1056,   857,   564,   957,   958,   565,
   321,   487,   281,   100,   523,   959,   476,   282,   477,   751,
   566,   419,   397,   655,   577,   575,   101,   102,   671,   231,
   209,   210,   567,  1011,   854,   861,   354,   312,   962,   266,
   488,   738,   739,  1012,   195,   568,   395,   481,   771,   104,
   105,   106,   569,   570,   571,   664,   406,   858,   107,   108,
   109,   110,     2,   236,   237,   505,   495,   482,   669,   516,
   286,   211,   313,   314,   718,   447,   239,   685,   826,   240,
   827,   695,  1004,   795,   448,   793,   665,   438,   667,   668,
   914,   355,   745,   578,   764,   576,   762,   728,   727,   840,
   933,  1005,  1045,   794,   804,   437,
    }, yySindex = {
//yySindex 1107
     0,     0, 20171, 21470,     0,     0, 19536, 19926,     0, 22631,
 22631, 18748,     0,     0,  3368, 20560, 20560,     0,     0,     0,
  -235,  -198,     0,     0,     0,     0,    84, 19796,   157,  -124,
  -111,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 22760, 22760,  -228,  -136, 20301,     0, 20950, 21340, 19016,
 22760, 22889, 19666,     0,     0,     0,   233,   248,     0,     0,
     0,     0,     0,     0,     0,   284,   300,     0,     0,     0,
  -168,     0,     0,     0,  -178,     0,     0,     0,     0,     0,
     0,     0,     0,  1597,   -72,  4863,     0,    79,   565,   419,
     0,   547,     0,   112,   430,     0,   312,     0,     0,     0,
  3872,   416,     0,   158,     0,   134,     0,  -212, 20560, 23147,
 23276,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  -206,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   443,     0,
     0, 20431,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   207, 20431,   -72,   319,   604,   220,
   515,   276,   319,     0,     0,   134,   260,   558,     0, 22631,
 22631,     0,     0,  -235,  -198,     0,     0,     0,     0,   283,
   157,     0,     0,     0,     0,     0,     0,     0,     0,  -228,
   367,     0,  1126,     0,     0,     0,   350,  -212,     0, 22760,
 22760, 22760, 22760,     0, 22760,  4863,     0,     0,   348,   595,
   664,     0,     0,     0, 17129,     0, 20560, 20560,     0,     0,
 18881,     0, 22631,   361,     0, 21728, 20171, 20431,     0,  1300,
   393,   405,   390, 21599,     0, 20301,   387,   134,  1597,     0,
     0,     0,   157,   157, 21599,   388,     0,   195,   268,   348,
     0,   384,   268,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   440, 23405,  1425,     0,   709,
     0,     0,     0,     0,     0,     0,     0,     0,   848,   977,
  1006,   592,     0,     0,     0,  2872,     0,     0,     0,     0,
     0,     0, 22631, 22631, 22631, 22631, 21599, 22631, 22631, 22760,
 22760, 22760, 22760, 22760,     0,     0, 22760, 22760, 22760, 22760,
 22760, 22760, 22760, 22760, 22760, 22760, 22760, 22760, 22760, 22760,
     0,     0, 22760, 22760, 22760, 22760,     0,     0,     0,     0,
  6249, 20560,  6668, 22760,     0,     0, 24135, 22889,     0, 21857,
 20301, 19149,   711, 21857, 22889,     0, 19278,     0,   425,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 22631,   -48,     0,   424,  1430,     0,     0, 22631,     0,     0,
     0,     0,     0,   527,   530,   390,     0, 20431,   532,  6746,
 20560,  6802, 22760, 22760, 22760, 20431,   260, 21986,   533,     0,
    81,    81,   462,     0,     0,  7172, 20560,  7250,     0,     0,
     0,     0,   947,     0, 22760, 20690,     0,     0, 21080,     0,
   157,     0,   465,     0,     0,     0,   766,   777,   157,   470,
     0,     0,     0,     0,     0, 19926, 22631,  4863,   442,   460,
  6746,  6802, 22760, 22760,  1597,   469,   157,     0,     0, 19407,
     0,     0,  1597,     0, 21210,     0,     0, 21340,     0,     0,
     0,     0,     0,   789,  7306, 20560, 13697, 23405,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1251,   131,
     0,     0,     0,     0,     0,     0,     0,  1657,  2897,     0,
     0,     0,     0,     0,     0,   528,   534,   798,     0,     0,
     0,   800,   802,     0,     0,   810,     0,     0,     0,   549,
   813, 22760,   801,  1448,   230,   601,     0,   506,     0,     0,
     0,     0,     0,     0,     0,     0,   393,  3370,  3370,  3370,
  3370,  3761,  5302,  3370,  3370,  4295,  4295,   906,   906,   393,
  2885,   393,   393,   844,   844,  2799,  2799, 13208,  3874,   608,
   543,     0,   554,  -198,     0,     0,     0,   157,   556,     0,
   557,  -198,     0,     0,  3874,     0,     0,  -198,     0,   602,
  4378,  1523,     0,     0,   112,   841, 22760,  4378,     0,     0,
     0,     0,   859,   157, 23405,   861,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   -72,     0,     0,     0,
     0,     0, 23743, 20560, 23799, 20431,   470,   570, 20056, 19926,
 22115,   643,     0,    21,     0,   575,   579,   157,   584,   586,
   643,   666,   749,     0,     0,     0,     0,     0,     0,     0,
  -198,   157,     0,     0,  -198, 22631, 22760,     0, 22760,   348,
   664,     0,     0,     0,     0, 20690, 21080,     0,     0,     0,
   470,     0,     0,   393,     0, 20171,     0,     0,   157,   268,
 23405,     0,     0,   157,     0,     0,   789,     0,  1484,     0,
     0,   282,     0,   901,  1657,   826,     0,   892,     0,   157,
   157,     0,     0,  3224,     0,  -185,  2897,     0,  2897,     0,
  -120,     0,     0,   234,     0,     0, 22760,     0,   320,     0,
   909,     0,   236,     0,   236,   887,     0, 22889, 22889,     0,
   425,   609,   606, 22889, 22889,     0,   425,     0,     0,    79,
  -178, 21599, 22760, 23855, 20560, 23911, 22889,     0, 22244,     0,
   789, 23405,   593,   134, 22631, 20431,     0,     0,     0,   157,
   693,     0,  2897, 20431,  2897,     0,     0,     0,     0,   620,
     0, 20431,   704,     0, 22631,     0,   710, 22760, 22760,   635,
 22760, 22760,   712,     0, 22373, 20431, 20431, 20431,     0,    81,
     0,     0,     0,   935,     0,   626,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   157,
  2197,   952,  1802,     0,   656,   954,   967,     0,     0, 20431,
 20431,     0,   970,   987,     0,   988,     0,   967,     0,     0,
   813,     0,     0,     0,     0,  2021,     0, 20431,     0, 20431,
 22760,     0,     0,     0,     0,     0, 22889,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  4863,   543,   554,   157,
   556,   557, 22760,     0,   789,     0, 20431,   134,   773,     0,
     0,   157,   776,   134,   570, 23534,   319,     0,     0, 20431,
     0,     0,   319,     0, 22760,     0,     0,   412,   790,   803,
     0, 21080,     0,     0,     0,  1031,  2197,   871,     0,     0,
  1260,  3224,     0,   815,   721,  3224,     0,  2897,     0,  3224,
     0,     0,     0,  1041,   157,  1045,     0,     0,  1049,  1050,
     0,   739,     0,   813, 23663,  1038,     0,     0,  4863,  4863,
     0,   609,     0,   837,     0,     0,     0,     0,     0, 20431,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   793,  1576,     0,     0, 20431,     0, 20431,     0,
     0,     0,     0,     0, 20431,     0,  2197,  1031,  2197,  1060,
     0,   623,     0,     0,     0,     0,   967,  1062,   967,   967,
  3224,     0,   751,  2897,     0,  -120,     0,  2897,     0,     0,
     0,     0,     0,   799,  1588, 23663,     0,   851,     0, 23967,
 20560, 24023,   527,    21,   866, 20431,  1031,  2197,  1260,     0,
     0,  3224,     0,     0,     0,  1087,     0,     0,  1102,  1109,
     0,   813,  1111,  1087,     0,     0, 24079,  1588,     0,     0,
     0,   157,     0,     0,     0,     0,     0,   778,  1031,     0,
   967,  3224,     0,  3224,     0,  2897,     0,     0,  3224,     0,
     0,     0,     0,     0,     0,     0,  1087,  1120,  1087,  1087,
     0,  3224,     0,     0,     0,  1087,     0,
    }, yyRindex = {
//yyRindex 1107
     0,     0,   272,     0,     0,     0,     0,     0,     0,     0,
     0,   904,     0,     0,     0, 11052, 11156,     0,     0,     0,
  5109,  4647, 12529, 12638, 12830, 12939, 23018,     0, 22502,     0,
     0, 13016, 13131, 13317,  5440,  3639, 13432, 13509,  5571, 13618,
     0,     0,     0,     0,     0,   237, 18612,   840,   832,    74,
     0,     0,  1463,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 10318,     0,     0,     0, 10440,     0,     0,     0,     0,     0,
     0,     0,     0,    59,   876, 16743, 10624, 16793,     0,  1380,
     0, 16869,     0, 13810,     0,     0,     0,     0,     0,     0,
   194,     0,     0,     0,     0,    25,     0, 20820, 11268,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  2315,
  2741,  3245,  3749,     0,     0,     0,     0,     0,     0,     0,
     0,  4253,  4736,  5198,  5681,     0,     0,     0,  5728,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  2306,     0,
     0,   849,  8176,  8298,  8482,  8604,  8788,  8910,  9094,  2500,
  9216,  9400,  2631,  9522,     0,   237,  1268,     0,     0, 10012,
     0,     0,     0,     0,     0,   904,     0,   918,     0,     0,
     0,     0,     0, 10746,  9706,   687,   699,  1097,  1682,     0,
   855,  1808,  1869,  1957,  2382,  2016,  2189,  4803,  2257,     0,
     0,     0,     0,  2311,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 16305,     0,     0, 16432, 16560,
 16560,     0,     0,     0,   858,     0,     0,    64,     0,     0,
   858,     0,     0,     0,     0,     0,    28,    28,     0,     0,
 11710, 10930, 13919,     0, 18074,   237,     0,  2760,  1324,     0,
     0,   153,   858,   858,     0,     0,     0,   863,   863,     0,
     0,     0,   852,  2212,  2214,  5631,  6078,  6582,  7086,  7590,
  1536,  8699,  8999,  1647,  9311,     0,     0,     0,  9432,   298,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  -173,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 11404, 11574,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    60,     0,     0,     0,     0,     0,     0,     0,     0,
   237,   310,   336,     0,     0,     0,    37,     0, 12774,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 17260, 17399,
     0,     0,     0, 18210,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   550,     0, 10134,     0,   984, 17941,     0,
    60,     0,     0,     0,     0,   761,     0,     0,     0,     0,
     0,     0,     0,     0,  2418,     0,    60,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   858,     0,     0,     0,     0,     0,    35,    35,   858,   858,
     0,     0,     0,     0,     0,     0,     0,  1018,     0,     0,
     0,     0,     0,     0,  1513,     0,   858,     0,     0,  2771,
   125,     0,    76,     0,   877,     0,     0,   -68,     0,     0,
     0,  9554,     0,   577,     0,    60,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   271,     0,
     0,     0,     0,     0,     0,   224,     0,    65,     0,     0,
     0,    65,    65,     0,     0,   118,     0,     0,     0,   259,
   118,   200,   330,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0, 11819,  1759, 15243, 15358,
 15484, 15587, 15860, 15676, 15762, 15949, 16035, 14386, 14512, 11936,
 14659, 12062, 12179, 14026, 14141, 14785, 14888,  1169, 15014,     0,
  5944,  4012,  7456, 20820,     0,  4143,     0,   884,  6075,     0,
  6448,  4978,     0,     0, 15129,     0,     0,  7829,     0,  8712,
 16393,     0,     0,     0, 14271,     0,     0,   942,     0,     0,
     0,     0,     0,   858,     0,   627,     0,     0,     0,     0,
  8406,     0,     0,     0,     0,     0,   184,     0, 17804,     0,
     0,     0,     0,    60,     0,   849,   858,  1439,     0,     0,
   724,   398,     0,   966,     0,  3004,  4516,   884,  3135,  3508,
   966,     0,     0,     0,     0,     0,     0,     0,  5265,   421,
     0,   884,  5827,  6200,  9828,     0,     0,     0,     0, 16519,
 16560,     0,     0,     0,     0,   171,   173,     0,     0,     0,
   858,     0,     0, 12288,     0,    28,   103,     0,   858,   863,
     0,  2191,  1123,   884,  7992,  8392,   648,     0,     0,     0,
     0,     0,     0,     0,     0,    88,     0,   121,     0,   858,
   -20,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 18343,     0, 18479,     0,     0,     0,     0,     0,
 16622,  2095,     0,     0,     0,     0, 16706,     0,     0, 16922,
 11518,     0,     0,     0,    60,     0,     0, 16972,     0,     0,
   675,     0,     0,     0,     0,   849, 17532, 17671,     0,   884,
     0,     0,   149,   849,   235,     0,     0,     0,   720,   741,
     0,   993,   966,     0,     0,     0,     0,     0,     0,  7960,
     0,     0,     0,     0,     0,   689,   621,   621,  1254,     0,
     0,     0,     0,    35,     0,     0,     0,     0,     0,  1263,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   858,
     0,   211,     0,     0,     0,  -164,    65,     0,     0,   849,
    28,     0,    65,    65,     0,    65,     0,    65,     0,     0,
   118,     0,     0,     0,     0,    -1,     0,   849,     0,    28,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  1949,  6579,  7587,   884,
  6952,  7083,     0, 16983,   702,     0,   849,     0,     0,     0,
     0,   858,     0,     0,  1439,     0,     0,     0,     0,   621,
     0,     0,     0,     0,     0,     0,     0,   966,     0,     0,
     0,   193,     0,     0,     0,   226,     0,   227,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,    68,    -1,    68,     0,     0,   161,    68,
     0,     0,     0,   161,    44,    90,     0,     0, 16133, 16219,
     0, 12424, 17035,     0,     0,     0,     0,     0,     0,   849,
   725,  1023,  1095,  1711,  2070,  2141,  2160,  1354,  2352,  8383,
  1450,  8431,     0,     0,  8703,     0,   849,     0,   984,     0,
     0,     0,     0,     0,   621,     0,     0,   232,     0,   238,
     0,   -85,     0,     0,     0,     0,    65,    65,    65,    65,
     0,     0,     0,   166,     0,     0,     0,     0,     0,     0,
     0,  2309,  2350,     0,   126,     0,     0,     0,  8956,     0,
    60,     0,   550,   966,     0,    26,   246,     0,     0,     0,
     0,     0,     0,     0,     0,    68,     0,     0,    68,    68,
     0,   161,    68,    68,     0,     0,     0,   129,     0,  8974,
  1573,   884,  9009,  9315,     0,     0,     0,     0,   254,     0,
    65,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  8017,   269,  1791,     0,     0,    68,    68,    68,    68,
     0,     0,     0,     0,     0,    68,     0,
    }, yyGindex = {
//yyGindex 196
     0,     0,     7,     0,  -240,     0,    56,    14,  -312,   154,
     0,     0,     0,   102,     0,     0,     0,  1161,     0,     0,
   961,  1187,     0,  -237,     0,     0,     0,   669,     0,    24,
  1134,  -227,   -28,     0,    95,     0,   247,   463,     0,    87,
    31,  1101,    46,    40,   737,    70,     1,   -64,     0,   204,
     0,     0,    33,     0,   -15,     0,     9,  1243,   642,     0,
     0,  -218,  -147,  -671,     0,     0,   123,    75,     0,     0,
     0,   486,   340,  -298,   -73,    -2,   750,  -435,     0,     0,
   297,   159,    57,     0,     0,   245,   524,    48,     0,     0,
     0,     0,  -518,  2066,   396,  -320,   525,   249,     0,     0,
     0,    49,  -460,     0,    45,   256,  -279,  -456,     0,  -494,
   316,   -65,   498,  -419,   629,   895,  1274,    34,   252,   641,
     0,   -14,  -639,     0,  -754,     0,     0,  -188,  -894,     0,
  -308,  -791,   563,   255,     0,  -835,  1213,   329,  -624,  -272,
     0,    23,     0,  1654,  2087,   -83,     0,  -156,  1559,  1796,
     0,     0,     0,   -34,   -23,     0,     0,   -26,     0,  -290,
     0,     0,     0,     0,     0,  -199,     0,  -454,     0,     0,
     0,     0,     0,     0,    18,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,null,"'\\n'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"' '",null,null,null,null,null,
    null,null,null,null,null,null,"','",null,null,null,null,null,null,
    null,null,null,null,null,null,null,"':'","';'",null,"'='",null,"'?'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "'['",null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "keyword_class","keyword_module","keyword_def","keyword_undef",
    "keyword_begin","keyword_rescue","keyword_ensure","keyword_end",
    "keyword_if","keyword_unless","keyword_then","keyword_elsif",
    "keyword_else","keyword_case","keyword_when","keyword_while",
    "keyword_until","keyword_for","keyword_break","keyword_next",
    "keyword_redo","keyword_retry","keyword_in","keyword_do",
    "keyword_do_cond","keyword_do_block","keyword_return","keyword_yield",
    "keyword_super","keyword_self","keyword_nil","keyword_true",
    "keyword_false","keyword_and","keyword_or","keyword_not",
    "modifier_if","modifier_unless","modifier_while","modifier_until",
    "modifier_rescue","keyword_alias","keyword_defined","keyword_BEGIN",
    "keyword_END","keyword__LINE__","keyword__FILE__",
    "keyword__ENCODING__","keyword_do_lambda","tIDENTIFIER","tFID",
    "tGVAR","tIVAR","tCONSTANT","tCVAR","tLABEL","tCHAR","tUPLUS",
    "tUMINUS","tUMINUS_NUM","tPOW","tCMP","tEQ","tEQQ","tNEQ","tGEQ",
    "tLEQ","tANDOP","tOROP","tMATCH","tNMATCH","tDOT","tDOT2","tDOT3",
    "tAREF","tASET","tLSHFT","tRSHFT","tANDDOT","tCOLON2","tCOLON3",
    "tOP_ASGN","tASSOC","tLPAREN","tLPAREN2","tRPAREN","tLPAREN_ARG",
    "tLBRACK","tRBRACK","tLBRACE","tLBRACE_ARG","tSTAR","tSTAR2","tAMPER",
    "tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS","tMINUS","tLT","tGT",
    "tPIPE","tBANG","tCARET","tLCURLY","tRCURLY","tBACK_REF2","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tLAMBDA","tLAMBEG",
    "tNTH_REF","tBACK_REF","tSTRING_CONTENT","tINTEGER","tIMAGINARY",
    "tFLOAT","tRATIONAL","tREGEXP_END","tSYMBOLS_BEG","tQSYMBOLS_BEG",
    "tDSTAR","tSTRING_DEND","tLABEL_END","tLOWEST",
    };


  /** computes list of expected tokens on error by tracing the tables.
      @param state for which to compute the list.
      @return list of token names.
    */
  protected String[] yyExpecting (int state) {
    int token, n, len = 0;
    boolean[] ok = new boolean[yyNames.length];

    if ((n = yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = yyNames[token];
    return result;
  }

  /** the generated parser, with debugging messages.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex, Object ayydebug) {
    return yyparse(yyLex);
  }

  /** initial size and increment of the state/value stack [default 256].
      This is not final so that it can be overwritten outside of invocations
      of {@link #yyparse}.
    */
  protected int yyMax;

  /** executed at the beginning of a reduce action.
      Used as <tt>$$ = yyDefault($1)</tt>, prior to the user-specified action, if any.
      Can be overwritten to provide deep copy, etc.
      @param first value for <tt>$1</tt>, or <tt>null</tt>.
      @return first.
    */
  protected Object yyDefault (Object first) {
    return first;
  }

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex) {
    if (yyMax <= 0) yyMax = 256;			// initial size
    int yyState = 0, yyStates[] = new int[yyMax];	// state stack
    Object yyVal = null, yyVals[] = new Object[yyMax];	// value stack
    int yyToken = -1;					// current input
    int yyErrorFlag = 0;				// #tokens to shift

    yyLoop: for (int yyTop = 0;; ++ yyTop) {
      if (yyTop >= yyStates.length) {			// dynamically increase
        int[] i = new int[yyStates.length+yyMax];
        System.arraycopy(yyStates, 0, i, 0, yyStates.length);
        yyStates = i;
        Object[] o = new Object[yyVals.length+yyMax];
        System.arraycopy(yyVals, 0, o, 0, yyVals.length);
        yyVals = o;
      }
      yyStates[yyTop] = yyState;
      yyVals[yyTop] = yyVal;

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
            yyToken = yyLex.nextToken();
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
            yyState = yyTable[yyN];		// shift to yyN
            yyVal = yyLex.value();
            yyToken = -1;
            if (yyErrorFlag > 0) -- yyErrorFlag;
            continue yyLoop;
          }
          if ((yyN = yyRindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken)
            yyN = yyTable[yyN];			// reduce (yyN)
          else
            switch (yyErrorFlag) {
  
            case 0:
              support.yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
              break;
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
              } while (-- yyTop >= 0);
              support.yyerror("irrecoverable syntax error");
              break;

            case 3:
              if (yyToken == 0) {
                support.yyerror("irrecoverable syntax error at end-of-file");
              }
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        ParserState state = states[yyN];
        if (state == null) {
            yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        } else {
            yyVal = state.execute(support, lexer, yyVal, yyVals, yyTop);
        }
//        switch (yyN) {
// ACTIONS_END
//        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.nextToken();
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if (yyToken == 0) {
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
        continue yyLoop;
      }
    }
  }

static ParserState[] states = new ParserState[652];
static {
states[1] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_BEG);
    support.initTopLocalVariables();
    return yyVal;
};
states[2] = (support, lexer, yyVal, yyVals, yyTop) -> {
  /* ENEBO: Removed !compile_for_eval which probably is to reduce warnings*/
                  if (((ParseNode)yyVals[0+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((ParseNode)yyVals[0+yyTop]) instanceof BlockParseNode) {
                          support.checkUselessStatement(((BlockParseNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((ParseNode)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.addRootNode(((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[3] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-1+yyTop]) instanceof BlockParseNode) {
        support.checkUselessStatements(((BlockParseNode)yyVals[-1+yyTop]));
    }
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[5] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newline_node(((ParseNode)yyVals[0+yyTop]), support.getPosition(((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[6] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.appendToBlock(((ParseNode)yyVals[-2+yyTop]), support.newline_node(((ParseNode)yyVals[0+yyTop]), support.getPosition(((ParseNode)yyVals[0+yyTop]))));
    return yyVal;
};
states[7] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[9] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.getResult().addBeginNode(new PreExe19ParseNode(((SourceIndexLength)yyVals[-3+yyTop]), support.getCurrentScope(), ((ParseNode)yyVals[-1+yyTop])));
    yyVal = null;
    return yyVal;
};
states[10] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode node = ((ParseNode)yyVals[-3+yyTop]);

    if (((RescueBodyParseNode)yyVals[-2+yyTop]) != null) {
        node = new RescueParseNode(support.getPosition(((ParseNode)yyVals[-3+yyTop])), ((ParseNode)yyVals[-3+yyTop]), ((RescueBodyParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    } else if (((ParseNode)yyVals[-1+yyTop]) != null) {
        support.warn(support.getPosition(((ParseNode)yyVals[-3+yyTop])), "else without rescue is useless");
        node = support.appendToBlock(((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    }
    if (((ParseNode)yyVals[0+yyTop]) != null) {
        if (node != null) {
            node = new EnsureParseNode(support.getPosition(((ParseNode)yyVals[-3+yyTop])), support.makeNullNil(node), ((ParseNode)yyVals[0+yyTop]));
        } else {
            node = support.appendToBlock(((ParseNode)yyVals[0+yyTop]), NilImplicitParseNode.NIL);
        }
    }

    support.fixpos(node, ((ParseNode)yyVals[-3+yyTop]));
    yyVal = node;
    return yyVal;
};
states[11] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-1+yyTop]) instanceof BlockParseNode) {
        support.checkUselessStatements(((BlockParseNode)yyVals[-1+yyTop]));
    }
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[13] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newline_node(((ParseNode)yyVals[0+yyTop]), support.getPosition(((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[14] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.appendToBlock(((ParseNode)yyVals[-2+yyTop]), support.newline_node(((ParseNode)yyVals[0+yyTop]), support.getPosition(((ParseNode)yyVals[0+yyTop]))));
    return yyVal;
};
states[15] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[16] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[17] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("BEGIN is permitted only at toplevel");
    return yyVal;
};
states[18] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new BeginParseNode(((SourceIndexLength)yyVals[-4+yyTop]), support.makeNullNil(((ParseNode)yyVals[-3+yyTop])));
    return yyVal;
};
states[19] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_FNAME|EXPR_FITEM);
    return yyVal;
};
states[20] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newAlias(((SourceIndexLength)yyVals[-3+yyTop]), ((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[21] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new VAliasParseNode(((SourceIndexLength)yyVals[-2+yyTop]), support.symbolID(((Rope)yyVals[-1+yyTop])), support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[22] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new VAliasParseNode(((SourceIndexLength)yyVals[-2+yyTop]), support.symbolID(((Rope)yyVals[-1+yyTop])), support.symbolID(((BackRefParseNode)yyVals[0+yyTop]).getByteName()));
    return yyVal;
};
states[23] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("can't make alias for the number variables");
    return yyVal;
};
states[24] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[25] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IfParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[-2+yyTop]), null);
    support.fixpos(((ParseNode)yyVal), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[26] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IfParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.getConditionNode(((ParseNode)yyVals[0+yyTop])), null, ((ParseNode)yyVals[-2+yyTop]));
    support.fixpos(((ParseNode)yyVal), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[27] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-2+yyTop]) != null && ((ParseNode)yyVals[-2+yyTop]) instanceof BeginParseNode) {
        yyVal = new WhileParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((BeginParseNode)yyVals[-2+yyTop]).getBodyNode(), false);
    } else {
        yyVal = new WhileParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[-2+yyTop]), true);
    }
    return yyVal;
};
states[28] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-2+yyTop]) != null && ((ParseNode)yyVals[-2+yyTop]) instanceof BeginParseNode) {
        yyVal = new UntilParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((BeginParseNode)yyVals[-2+yyTop]).getBodyNode(), false);
    } else {
        yyVal = new UntilParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[-2+yyTop]), true);
    }
    return yyVal;
};
states[29] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newRescueModNode(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[30] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) {
        support.warn(((SourceIndexLength)yyVals[-3+yyTop]), "END in method; use at_exit");
    }
    yyVal = new PostExeParseNode(((SourceIndexLength)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[32] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    ((MultipleAsgnParseNode)yyVals[-2+yyTop]).setValueNode(((ParseNode)yyVals[0+yyTop]));
    yyVal = ((MultipleAsgnParseNode)yyVals[-2+yyTop]);
    return yyVal;
};
states[33] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.node_assign(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[34] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(((ParseNode)yyVals[0+yyTop]));
    yyVal = ((MultipleAsgnParseNode)yyVals[-2+yyTop]);
    ((MultipleAsgnParseNode)yyVals[-2+yyTop]).setPosition(support.getPosition(((MultipleAsgnParseNode)yyVals[-2+yyTop])));
    return yyVal;
};
states[36] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.node_assign(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[37] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));

    SourceIndexLength pos = ((AssignableParseNode)yyVals[-2+yyTop]).getPosition();
    Rope asgnOp = ((Rope)yyVals[-1+yyTop]);
    if (asgnOp == RopeConstants.OR_OR) {
        ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(((ParseNode)yyVals[0+yyTop]));
        yyVal = new OpAsgnOrParseNode(pos, support.gettable2(((AssignableParseNode)yyVals[-2+yyTop])), ((AssignableParseNode)yyVals[-2+yyTop]));
    } else if (asgnOp == RopeConstants.AMPERSAND_AMPERSAND) {
        ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(((ParseNode)yyVals[0+yyTop]));
        yyVal = new OpAsgnAndParseNode(pos, support.gettable2(((AssignableParseNode)yyVals[-2+yyTop])), ((AssignableParseNode)yyVals[-2+yyTop]));
    } else {
        ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableParseNode)yyVals[-2+yyTop])), asgnOp, ((ParseNode)yyVals[0+yyTop])));
        ((AssignableParseNode)yyVals[-2+yyTop]).setPosition(pos);
        yyVal = ((AssignableParseNode)yyVals[-2+yyTop]);
    }
    return yyVal;
};
states[38] = (support, lexer, yyVal, yyVals, yyTop) -> {
  /* FIXME: arg_concat logic missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(((ParseNode)yyVals[-5+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[39] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newOpAsgn(support.getPosition(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[40] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newOpAsgn(support.getPosition(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[41] = (support, lexer, yyVal, yyVals, yyTop) -> {
    SourceIndexLength pos = ((ParseNode)yyVals[-4+yyTop]).getPosition();
    yyVal = support.newOpConstAsgn(pos, support.new_colon2(pos, ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop])), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[42] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newOpAsgn(support.getPosition(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[43] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.backrefAssignError(((ParseNode)yyVals[-2+yyTop]));
    return yyVal;
};
states[44] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[45] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[-2+yyTop]));
    yyVal = support.newRescueModNode(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[48] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newAndNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), ((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[49] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newOrNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), ((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[50] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(support.getConditionNode(((ParseNode)yyVals[0+yyTop])), RopeConstants.BANG);
    return yyVal;
};
states[51] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[53] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[57] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[58] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((IterParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[59] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_fcall(((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[60] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.frobnicate_fcall_args(((FCallParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    yyVal = ((FCallParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[61] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.frobnicate_fcall_args(((FCallParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((IterParseNode)yyVals[0+yyTop]));
    yyVal = ((FCallParseNode)yyVals[-2+yyTop]);
    return yyVal;
};
states[62] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[63] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((IterParseNode)yyVals[0+yyTop])); 
    return yyVal;
};
states[64] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[65] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((IterParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[66] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_super(((SourceIndexLength)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[67] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_yield(((SourceIndexLength)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[68] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ReturnParseNode(((SourceIndexLength)yyVals[-1+yyTop]), support.ret_args(((ParseNode)yyVals[0+yyTop]), ((SourceIndexLength)yyVals[-1+yyTop])));
    return yyVal;
};
states[69] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new BreakParseNode(((SourceIndexLength)yyVals[-1+yyTop]), support.ret_args(((ParseNode)yyVals[0+yyTop]), ((SourceIndexLength)yyVals[-1+yyTop])));
    return yyVal;
};
states[70] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new NextParseNode(((SourceIndexLength)yyVals[-1+yyTop]), support.ret_args(((ParseNode)yyVals[0+yyTop]), ((SourceIndexLength)yyVals[-1+yyTop])));
    return yyVal;
};
states[72] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[73] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((MultipleAsgnParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[74] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((SourceIndexLength)yyVals[-2+yyTop]), support.newArrayNode(((SourceIndexLength)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop])), null, null);
    return yyVal;
};
states[75] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[0+yyTop]).getPosition(), ((ListParseNode)yyVals[0+yyTop]), null, null);
    return yyVal;
};
states[76] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]).add(((ParseNode)yyVals[0+yyTop])), null, null);
    return yyVal;
};
states[77] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-2+yyTop]).getPosition(), ((ListParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]), (ListParseNode) null);
    return yyVal;
};
states[78] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-4+yyTop]).getPosition(), ((ListParseNode)yyVals[-4+yyTop]), ((ParseNode)yyVals[-2+yyTop]), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[79] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]), new StarParseNode(lexer.getPosition()), null);
    return yyVal;
};
states[80] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), new StarParseNode(lexer.getPosition()), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[81] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ParseNode)yyVals[0+yyTop]).getPosition(), null, ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[82] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ParseNode)yyVals[-2+yyTop]).getPosition(), null, ((ParseNode)yyVals[-2+yyTop]), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[83] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(lexer.getPosition(), null, new StarParseNode(lexer.getPosition()), null);
    return yyVal;
};
states[84] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(lexer.getPosition(), null, new StarParseNode(lexer.getPosition()), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[86] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[87] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((ParseNode)yyVals[-1+yyTop]).getPosition(), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[88] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[89] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((ParseNode)yyVals[0+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[90] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[91] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.assignableLabelOrIdentifier(((Rope)yyVals[0+yyTop]), null);
    return yyVal;
};
states[92] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new InstAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[93] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new GlobalAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[94] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) support.compile_error("dynamic constant assignment");
    yyVal = new ConstDeclParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), null, NilImplicitParseNode.NIL);
    return yyVal;
};
states[95] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ClassVarAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[96] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to nil");
    yyVal = null;
    return yyVal;
};
states[97] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't change the value of self");
    yyVal = null;
    return yyVal;
};
states[98] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to true");
    yyVal = null;
    return yyVal;
};
states[99] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to false");
    yyVal = null;
    return yyVal;
};
states[100] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __FILE__");
    yyVal = null;
    return yyVal;
};
states[101] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __LINE__");
    yyVal = null;
    return yyVal;
};
states[102] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __ENCODING__");
    yyVal = null;
    return yyVal;
};
states[103] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.aryset(((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[104] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.attrset(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[105] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.attrset(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[106] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.attrset(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[107] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) support.yyerror("dynamic constant assignment");

    SourceIndexLength position = support.getPosition(((ParseNode)yyVals[-2+yyTop]));

    yyVal = new ConstDeclParseNode(position, (Rope) null, support.new_colon2(position, ((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[108] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) {
        support.yyerror("dynamic constant assignment");
    }

    SourceIndexLength position = lexer.tokline;

    yyVal = new ConstDeclParseNode(position, (Rope) null, support.new_colon3(position, ((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[109] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.backrefAssignError(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[110] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.assignableLabelOrIdentifier(((Rope)yyVals[0+yyTop]), null);
    return yyVal;
};
states[111] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new InstAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[112] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new GlobalAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[113] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) support.compile_error("dynamic constant assignment");

    yyVal = new ConstDeclParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), null, NilImplicitParseNode.NIL);
    return yyVal;
};
states[114] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ClassVarAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[115] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to nil");
    yyVal = null;
    return yyVal;
};
states[116] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't change the value of self");
    yyVal = null;
    return yyVal;
};
states[117] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to true");
    yyVal = null;
    return yyVal;
};
states[118] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to false");
    yyVal = null;
    return yyVal;
};
states[119] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __FILE__");
    yyVal = null;
    return yyVal;
};
states[120] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __LINE__");
    yyVal = null;
    return yyVal;
};
states[121] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __ENCODING__");
    yyVal = null;
    return yyVal;
};
states[122] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.aryset(((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[123] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.attrset(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[124] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.attrset(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[125] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.attrset(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[126] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) {
        support.yyerror("dynamic constant assignment");
    }

    SourceIndexLength position = support.getPosition(((ParseNode)yyVals[-2+yyTop]));

    yyVal = new ConstDeclParseNode(position, (Rope) null, support.new_colon2(position, ((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[127] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) {
        support.yyerror("dynamic constant assignment");
    }

    SourceIndexLength position = lexer.tokline;

    yyVal = new ConstDeclParseNode(position, (Rope) null, support.new_colon3(position, ((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[128] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.backrefAssignError(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[129] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("class/module name must be CONSTANT");
    return yyVal;
};
states[130] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[131] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_colon3(lexer.tokline, ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[132] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_colon2(lexer.tokline, null, ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[133] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_colon2(support.getPosition(((ParseNode)yyVals[-2+yyTop])), ((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[134] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[135] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[136] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[137] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_ENDFN);
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[138] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_ENDFN);
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[139] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new LiteralParseNode(lexer.getPosition(), support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[140] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new LiteralParseNode(lexer.getPosition(), support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[141] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((LiteralParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[142] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[143] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newUndef(((ParseNode)yyVals[0+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[144] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_FNAME|EXPR_FITEM);
    return yyVal;
};
states[145] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.appendToBlock(((ParseNode)yyVals[-3+yyTop]), support.newUndef(((ParseNode)yyVals[-3+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[146] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[147] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[148] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[149] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[150] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[151] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[152] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[153] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[154] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[155] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[156] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[157] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[158] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[159] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[160] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[161] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[162] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[163] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[164] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[165] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[166] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[167] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[168] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[169] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[170] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[171] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[172] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[173] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[174] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[175] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[176] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.__LINE__.bytes;
    return yyVal;
};
states[177] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.__FILE__.bytes;
    return yyVal;
};
states[178] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.__ENCODING__.bytes;
    return yyVal;
};
states[179] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.LBEGIN.bytes;
    return yyVal;
};
states[180] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.LEND.bytes;
    return yyVal;
};
states[181] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.ALIAS.bytes;
    return yyVal;
};
states[182] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.AND.bytes;
    return yyVal;
};
states[183] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.BEGIN.bytes;
    return yyVal;
};
states[184] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.BREAK.bytes;
    return yyVal;
};
states[185] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.CASE.bytes;
    return yyVal;
};
states[186] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.CLASS.bytes;
    return yyVal;
};
states[187] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.DEF.bytes;
    return yyVal;
};
states[188] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.DEFINED_P.bytes;
    return yyVal;
};
states[189] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.DO.bytes;
    return yyVal;
};
states[190] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.ELSE.bytes;
    return yyVal;
};
states[191] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.ELSIF.bytes;
    return yyVal;
};
states[192] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.END.bytes;
    return yyVal;
};
states[193] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.ENSURE.bytes;
    return yyVal;
};
states[194] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.FALSE.bytes;
    return yyVal;
};
states[195] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.FOR.bytes;
    return yyVal;
};
states[196] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.IN.bytes;
    return yyVal;
};
states[197] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.MODULE.bytes;
    return yyVal;
};
states[198] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.NEXT.bytes;
    return yyVal;
};
states[199] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.NIL.bytes;
    return yyVal;
};
states[200] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.NOT.bytes;
    return yyVal;
};
states[201] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.OR.bytes;
    return yyVal;
};
states[202] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.REDO.bytes;
    return yyVal;
};
states[203] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.RESCUE.bytes;
    return yyVal;
};
states[204] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.RETRY.bytes;
    return yyVal;
};
states[205] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.RETURN.bytes;
    return yyVal;
};
states[206] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.SELF.bytes;
    return yyVal;
};
states[207] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.SUPER.bytes;
    return yyVal;
};
states[208] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.THEN.bytes;
    return yyVal;
};
states[209] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.TRUE.bytes;
    return yyVal;
};
states[210] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.UNDEF.bytes;
    return yyVal;
};
states[211] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.WHEN.bytes;
    return yyVal;
};
states[212] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.YIELD.bytes;
    return yyVal;
};
states[213] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.IF.bytes;
    return yyVal;
};
states[214] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.UNLESS.bytes;
    return yyVal;
};
states[215] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.WHILE.bytes;
    return yyVal;
};
states[216] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.UNTIL.bytes;
    return yyVal;
};
states[217] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = RubyLexer.Keyword.RESCUE.bytes;
    return yyVal;
};
states[218] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.node_assign(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    /* FIXME: Consider fixing node_assign itself rather than single case*/
    ((ParseNode)yyVal).setPosition(support.getPosition(((ParseNode)yyVals[-2+yyTop])));
    return yyVal;
};
states[219] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));

    SourceIndexLength pos = ((AssignableParseNode)yyVals[-2+yyTop]).getPosition();
    Rope asgnOp = ((Rope)yyVals[-1+yyTop]);
    if (asgnOp == RopeConstants.OR_OR) {
        ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(((ParseNode)yyVals[0+yyTop]));
        yyVal = new OpAsgnOrParseNode(pos, support.gettable2(((AssignableParseNode)yyVals[-2+yyTop])), ((AssignableParseNode)yyVals[-2+yyTop]));
    } else if (asgnOp == RopeConstants.AMPERSAND_AMPERSAND) {
        ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(((ParseNode)yyVals[0+yyTop]));
        yyVal = new OpAsgnAndParseNode(pos, support.gettable2(((AssignableParseNode)yyVals[-2+yyTop])), ((AssignableParseNode)yyVals[-2+yyTop]));
    } else {
        ((AssignableParseNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableParseNode)yyVals[-2+yyTop])), asgnOp, ((ParseNode)yyVals[0+yyTop])));
        ((AssignableParseNode)yyVals[-2+yyTop]).setPosition(pos);
        yyVal = ((AssignableParseNode)yyVals[-2+yyTop]);
    }
    return yyVal;
};
states[220] = (support, lexer, yyVal, yyVals, yyTop) -> {
  /* FIXME: arg_concat missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(((ParseNode)yyVals[-5+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[221] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newOpAsgn(support.getPosition(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[222] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newOpAsgn(support.getPosition(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[223] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newOpAsgn(support.getPosition(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[224] = (support, lexer, yyVal, yyVals, yyTop) -> {
    SourceIndexLength pos = support.getPosition(((ParseNode)yyVals[-4+yyTop]));
    yyVal = support.newOpConstAsgn(pos, support.new_colon2(pos, ((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-2+yyTop])), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[225] = (support, lexer, yyVal, yyVals, yyTop) -> {
    SourceIndexLength pos = lexer.getPosition();
    yyVal = support.newOpConstAsgn(pos, new Colon3ParseNode(pos, support.symbolID(((Rope)yyVals[-2+yyTop]))), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[226] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.backrefAssignError(((ParseNode)yyVals[-2+yyTop]));
    return yyVal;
};
states[227] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[-2+yyTop]));
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    
    boolean isLiteral = ((ParseNode)yyVals[-2+yyTop]) instanceof FixnumParseNode && ((ParseNode)yyVals[0+yyTop]) instanceof FixnumParseNode;
    yyVal = new DotParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.makeNullNil(((ParseNode)yyVals[-2+yyTop])), support.makeNullNil(((ParseNode)yyVals[0+yyTop])), false, isLiteral);
    return yyVal;
};
states[228] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.checkExpression(((ParseNode)yyVals[-1+yyTop]));

    boolean isLiteral = ((ParseNode)yyVals[-1+yyTop]) instanceof FixnumParseNode;
    yyVal = new DotParseNode(support.getPosition(((ParseNode)yyVals[-1+yyTop])), support.makeNullNil(((ParseNode)yyVals[-1+yyTop])), NilImplicitParseNode.NIL, false, isLiteral);
    return yyVal;
};
states[229] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[-2+yyTop]));
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));

    boolean isLiteral = ((ParseNode)yyVals[-2+yyTop]) instanceof FixnumParseNode && ((ParseNode)yyVals[0+yyTop]) instanceof FixnumParseNode;
    yyVal = new DotParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), support.makeNullNil(((ParseNode)yyVals[-2+yyTop])), support.makeNullNil(((ParseNode)yyVals[0+yyTop])), true, isLiteral);
    return yyVal;
};
states[230] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.checkExpression(((ParseNode)yyVals[-1+yyTop]));

    boolean isLiteral = ((ParseNode)yyVals[-1+yyTop]) instanceof FixnumParseNode;
    yyVal = new DotParseNode(support.getPosition(((ParseNode)yyVals[-1+yyTop])), support.makeNullNil(((ParseNode)yyVals[-1+yyTop])), NilImplicitParseNode.NIL, true, isLiteral);
    return yyVal;
};
states[231] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[232] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[233] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[234] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[235] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[236] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[237] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((NumericParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition()), ((Rope)yyVals[-3+yyTop]));
    return yyVal;
};
states[238] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[239] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[240] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[241] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[242] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[243] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[244] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[245] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[246] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[247] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[248] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getMatchNode(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
  /* ENEBO
        $$ = match_op($1, $3);
        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
            $$ = reg_named_capture_assign($1->nd_lit, $$);
        }
  */
    return yyVal;
};
states[249] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[250] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(support.getConditionNode(((ParseNode)yyVals[0+yyTop])), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[251] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[0+yyTop]), ((Rope)yyVals[-1+yyTop]));
    return yyVal;
};
states[252] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[253] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[254] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newAndNode(((ParseNode)yyVals[-2+yyTop]).getPosition(), ((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[255] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newOrNode(((ParseNode)yyVals[-2+yyTop]).getPosition(), ((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[256] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_defined(((SourceIndexLength)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[257] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[-5+yyTop]));
    yyVal = new IfParseNode(support.getPosition(((ParseNode)yyVals[-5+yyTop])), support.getConditionNode(((ParseNode)yyVals[-5+yyTop])), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[258] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[259] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[260] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[261] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[262] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[263] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[264] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.warning(lexer.getPosition(), "comparison '" + ((Rope)yyVals[-1+yyTop]).getString() + "' after comparison");
    yyVal = support.getOperatorCallNode(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
};
states[265] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.makeNullNil(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[267] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[268] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.arg_append(((ParseNode)yyVals[-3+yyTop]), support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[269] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((HashParseNode)yyVals[-1+yyTop]).getPosition(), support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[270] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[271] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[-2+yyTop]));
    yyVal = support.newRescueModNode(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[272] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    if (yyVal != null) ((ParseNode)yyVal).setPosition(((SourceIndexLength)yyVals[-2+yyTop]));
    return yyVal;
};
states[277] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[278] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.arg_append(((ParseNode)yyVals[-3+yyTop]), support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[279] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((HashParseNode)yyVals[-1+yyTop]).getPosition(), support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[280] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = support.newArrayNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[281] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.arg_blk_pass(((ParseNode)yyVals[-1+yyTop]), ((BlockPassParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[282] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((HashParseNode)yyVals[-1+yyTop]).getPosition(), support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop])));
    yyVal = support.arg_blk_pass((ParseNode)yyVal, ((BlockPassParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[283] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.arg_append(((ParseNode)yyVals[-3+yyTop]), support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop])));
    yyVal = support.arg_blk_pass((ParseNode)yyVal, ((BlockPassParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[284] = (support, lexer, yyVal, yyVals, yyTop) -> yyVal;
states[285] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getCmdArgumentState().getStack();
    lexer.getCmdArgumentState().begin();
    return yyVal;
};
states[286] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[287] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new BlockPassParseNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[288] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((BlockPassParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[290] = (support, lexer, yyVal, yyVals, yyTop) -> {
    SourceIndexLength pos = ((ParseNode)yyVals[0+yyTop]) == null ? lexer.getPosition() : ((ParseNode)yyVals[0+yyTop]).getPosition();
    yyVal = support.newArrayNode(pos, ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[291] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newSplatNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[292] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode node = support.splat_array(((ParseNode)yyVals[-2+yyTop]));

    if (node != null) {
        yyVal = support.list_append(node, ((ParseNode)yyVals[0+yyTop]));
    } else {
        yyVal = support.arg_append(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    }
    return yyVal;
};
states[293] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode node = null;

    /* FIXME: lose syntactical elements here (and others like this)*/
    if (((ParseNode)yyVals[0+yyTop]) instanceof ArrayParseNode &&
        (node = support.splat_array(((ParseNode)yyVals[-3+yyTop]))) != null) {
        yyVal = support.list_concat(node, ((ParseNode)yyVals[0+yyTop]));
    } else {
        yyVal = support.arg_concat(support.getPosition(((ParseNode)yyVals[-3+yyTop])), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    }
    return yyVal;
};
states[294] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[295] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[296] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode node = support.splat_array(((ParseNode)yyVals[-2+yyTop]));

    if (node != null) {
        yyVal = support.list_append(node, ((ParseNode)yyVals[0+yyTop]));
    } else {
        yyVal = support.arg_append(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    }
    return yyVal;
};
states[297] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode node = null;

    if (((ParseNode)yyVals[0+yyTop]) instanceof ArrayParseNode &&
        (node = support.splat_array(((ParseNode)yyVals[-3+yyTop]))) != null) {
        yyVal = support.list_concat(node, ((ParseNode)yyVals[0+yyTop]));
    } else {
        yyVal = support.arg_concat(((ParseNode)yyVals[-3+yyTop]).getPosition(), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    }
    return yyVal;
};
states[298] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newSplatNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[305] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
    return yyVal;
};
states[306] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
    return yyVal;
};
states[309] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_fcall(((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[310] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getCmdArgumentState().getStack();
    lexer.getCmdArgumentState().reset();
    return yyVal;
};
states[311] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
    yyVal = new BeginParseNode(((SourceIndexLength)yyVals[-3+yyTop]), support.makeNullNil(((ParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[312] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_ENDARG);
    return yyVal;
};
states[313] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null; /*FIXME: Should be implicit nil?*/
    return yyVal;
};
states[314] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getCmdArgumentState().getStack();
    lexer.getCmdArgumentState().reset();
    return yyVal;
};
states[315] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_ENDARG); 
    return yyVal;
};
states[316] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop]).longValue());
    yyVal = ((ParseNode)yyVals[-2+yyTop]);
    return yyVal;
};
states[317] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-1+yyTop]) != null) {
        /* compstmt position includes both parens around it*/
        ((ParseNode)yyVals[-1+yyTop]).setPosition(((SourceIndexLength)yyVals[-2+yyTop]));
        yyVal = ((ParseNode)yyVals[-1+yyTop]);
    } else {
        yyVal = new NilParseNode(((SourceIndexLength)yyVals[-2+yyTop]));
    }
    return yyVal;
};
states[318] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_colon2(support.getPosition(((ParseNode)yyVals[-2+yyTop])), ((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[319] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_colon3(lexer.tokline, ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[320] = (support, lexer, yyVal, yyVals, yyTop) -> {
    SourceIndexLength position = support.getPosition(((ParseNode)yyVals[-1+yyTop]));
    if (((ParseNode)yyVals[-1+yyTop]) == null) {
        yyVal = new ZArrayParseNode(position); /* zero length array */
    } else {
        yyVal = ((ParseNode)yyVals[-1+yyTop]);
    }
    return yyVal;
};
states[321] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((HashParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[322] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ReturnParseNode(((SourceIndexLength)yyVals[0+yyTop]), NilImplicitParseNode.NIL);
    return yyVal;
};
states[323] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_yield(((SourceIndexLength)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[324] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new YieldParseNode(((SourceIndexLength)yyVals[-2+yyTop]), null);
    return yyVal;
};
states[325] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new YieldParseNode(((SourceIndexLength)yyVals[0+yyTop]), null);
    return yyVal;
};
states[326] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_defined(((SourceIndexLength)yyVals[-4+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[327] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(support.getConditionNode(((ParseNode)yyVals[-1+yyTop])), RopeConstants.BANG);
    return yyVal;
};
states[328] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.getOperatorCallNode(NilImplicitParseNode.NIL, RopeConstants.BANG);
    return yyVal;
};
states[329] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.frobnicate_fcall_args(((FCallParseNode)yyVals[-1+yyTop]), null, ((IterParseNode)yyVals[0+yyTop]));
    yyVal = ((FCallParseNode)yyVals[-1+yyTop]);                    
    return yyVal;
};
states[331] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-1+yyTop]) != null && 
          ((BlockAcceptingParseNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassParseNode) {
          lexer.compile_error(PID.BLOCK_ARG_AND_BLOCK_GIVEN, "Both block arg and actual block given.");
    }
    yyVal = ((BlockAcceptingParseNode)yyVals[-1+yyTop]).setIterNode(((IterParseNode)yyVals[0+yyTop]));
    ((ParseNode)yyVal).setPosition(((ParseNode)yyVals[-1+yyTop]).getPosition());
    return yyVal;
};
states[332] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((LambdaParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[333] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IfParseNode(((SourceIndexLength)yyVals[-5+yyTop]), support.getConditionNode(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[334] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IfParseNode(((SourceIndexLength)yyVals[-5+yyTop]), support.getConditionNode(((ParseNode)yyVals[-4+yyTop])), ((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[-2+yyTop]));
    return yyVal;
};
states[335] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().begin();
    return yyVal;
};
states[336] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().end();
    return yyVal;
};
states[337] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = support.makeNullNil(((ParseNode)yyVals[-1+yyTop]));
    yyVal = new WhileParseNode(((SourceIndexLength)yyVals[-6+yyTop]), support.getConditionNode(((ParseNode)yyVals[-4+yyTop])), body);
    return yyVal;
};
states[338] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().begin();
    return yyVal;
};
states[339] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().end();
    return yyVal;
};
states[340] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = support.makeNullNil(((ParseNode)yyVals[-1+yyTop]));
    yyVal = new UntilParseNode(((SourceIndexLength)yyVals[-6+yyTop]), support.getConditionNode(((ParseNode)yyVals[-4+yyTop])), body);
    return yyVal;
};
states[341] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newCaseNode(((SourceIndexLength)yyVals[-4+yyTop]), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[342] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newCaseNode(((SourceIndexLength)yyVals[-3+yyTop]), null, ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[343] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().begin();
    return yyVal;
};
states[344] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().end();
    return yyVal;
};
states[345] = (support, lexer, yyVal, yyVals, yyTop) -> {
    /* ENEBO: Lots of optz in 1.9 parser here*/
  yyVal = new ForParseNode(((SourceIndexLength)yyVals[-8+yyTop]), ((ParseNode)yyVals[-7+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[-4+yyTop]), support.getCurrentScope());
    return yyVal;
};
states[346] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) {
        support.yyerror("class definition in method body");
    }
    support.pushLocalScope();
    yyVal = support.isInClass(); /* MRI reuses $1 but we use the value for position.*/
    support.setIsInClass(true);
    return yyVal;
};
states[347] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = support.makeNullNil(((ParseNode)yyVals[-1+yyTop]));

    yyVal = new ClassParseNode(support.extendedUntil(((SourceIndexLength)yyVals[-5+yyTop]), lexer.getPosition()), ((Colon3ParseNode)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((ParseNode)yyVals[-3+yyTop]));
    support.popCurrentScope();
    support.setIsInClass(((Boolean)yyVals[-2+yyTop]).booleanValue());
    return yyVal;
};
states[348] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = (support.isInClass() ? 2 : 0) | (support.isInDef() ? 1 : 0);
    support.setInDef(false);
    support.setIsInClass(false);
    support.pushLocalScope();
    return yyVal;
};
states[349] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = support.makeNullNil(((ParseNode)yyVals[-1+yyTop]));

    yyVal = new SClassParseNode(support.extendedUntil(((SourceIndexLength)yyVals[-6+yyTop]), lexer.getPosition()), ((ParseNode)yyVals[-4+yyTop]), support.getCurrentScope(), body);
    support.popCurrentScope();
    support.setInDef(((((Integer)yyVals[-3+yyTop]).intValue()) & 1) != 0);
    support.setIsInClass(((((Integer)yyVals[-3+yyTop]).intValue()) & 2) != 0);
    return yyVal;
};
states[350] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) { 
        support.yyerror("module definition in method body");
    }
    yyVal = support.isInClass();
    support.setIsInClass(true);
    support.pushLocalScope();
    return yyVal;
};
states[351] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = support.makeNullNil(((ParseNode)yyVals[-1+yyTop]));

    yyVal = new ModuleParseNode(support.extendedUntil(((SourceIndexLength)yyVals[-4+yyTop]), lexer.getPosition()), ((Colon3ParseNode)yyVals[-3+yyTop]), support.getCurrentScope(), body);
    support.popCurrentScope();
    support.setIsInClass(((Boolean)yyVals[-2+yyTop]).booleanValue());
    return yyVal;
};
states[352] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.pushLocalScope();
    yyVal = lexer.getCurrentArg();
    lexer.setCurrentArg(null);
    return yyVal;
};
states[353] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.isInDef();
    support.setInDef(true);
    return yyVal;
};
states[354] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = support.makeNullNil(((ParseNode)yyVals[-1+yyTop]));

    yyVal = new DefnParseNode(support.extendedUntil(((SourceIndexLength)yyVals[-6+yyTop]), ((SourceIndexLength)yyVals[0+yyTop])), support.symbolID(((Rope)yyVals[-5+yyTop])), (ArgsParseNode) yyVals[-2+yyTop], support.getCurrentScope(), body);
    support.popCurrentScope();
    support.setInDef(((Boolean)yyVals[-3+yyTop]).booleanValue());
    lexer.setCurrentArg(((Rope)yyVals[-4+yyTop]));
    return yyVal;
};
states[355] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_FNAME); 
    yyVal = support.isInDef();
    support.setInDef(true);
    return yyVal;
};
states[356] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.pushLocalScope();
    lexer.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
    yyVal = lexer.getCurrentArg();
    lexer.setCurrentArg(null);
    return yyVal;
};
states[357] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode body = ((ParseNode)yyVals[-1+yyTop]);
    if (body == null) body = NilImplicitParseNode.NIL;

    yyVal = new DefsParseNode(support.extendedUntil(((SourceIndexLength)yyVals[-8+yyTop]), ((SourceIndexLength)yyVals[0+yyTop])), ((ParseNode)yyVals[-7+yyTop]), support.symbolID(((Rope)yyVals[-4+yyTop])), (ArgsParseNode) yyVals[-2+yyTop], support.getCurrentScope(), body);
    support.popCurrentScope();
    support.setInDef(((Boolean)yyVals[-5+yyTop]).booleanValue());
    lexer.setCurrentArg(((Rope)yyVals[-3+yyTop]));
    return yyVal;
};
states[358] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new BreakParseNode(((SourceIndexLength)yyVals[0+yyTop]), NilImplicitParseNode.NIL);
    return yyVal;
};
states[359] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new NextParseNode(((SourceIndexLength)yyVals[0+yyTop]), NilImplicitParseNode.NIL);
    return yyVal;
};
states[360] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new RedoParseNode(((SourceIndexLength)yyVals[0+yyTop]));
    return yyVal;
};
states[361] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new RetryParseNode(((SourceIndexLength)yyVals[0+yyTop]));
    return yyVal;
};
states[362] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    if (yyVal == null) yyVal = NilImplicitParseNode.NIL;
    return yyVal;
};
states[363] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((SourceIndexLength)yyVals[0+yyTop]);
    return yyVal;
};
states[364] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((SourceIndexLength)yyVals[0+yyTop]);
    return yyVal;
};
states[365] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInClass() && !support.isInDef() && !support.getCurrentScope().isBlockScope()) {
        lexer.compile_error(PID.TOP_LEVEL_RETURN, "Invalid return in class/module body");
    }
    yyVal = ((SourceIndexLength)yyVals[0+yyTop]);
    return yyVal;
};
states[372] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IfParseNode(((SourceIndexLength)yyVals[-4+yyTop]), support.getConditionNode(((ParseNode)yyVals[-3+yyTop])), ((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[374] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[376] = (support, lexer, yyVal, yyVals, yyTop) -> yyVal;
states[377] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.assignableInCurr(((Rope)yyVals[0+yyTop]), NilImplicitParseNode.NIL);
    return yyVal;
};
states[378] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[379] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((ParseNode)yyVals[0+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[380] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[381] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[0+yyTop]).getPosition(), ((ListParseNode)yyVals[0+yyTop]), null, null);
    return yyVal;
};
states[382] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), support.assignableInCurr(((Rope)yyVals[0+yyTop]), null), null);
    return yyVal;
};
states[383] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), support.assignableInCurr(((Rope)yyVals[-2+yyTop]), null), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[384] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-2+yyTop]).getPosition(), ((ListParseNode)yyVals[-2+yyTop]), new StarParseNode(lexer.getPosition()), null);
    return yyVal;
};
states[385] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(((ListParseNode)yyVals[-4+yyTop]).getPosition(), ((ListParseNode)yyVals[-4+yyTop]), new StarParseNode(lexer.getPosition()), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[386] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(lexer.getPosition(), null, support.assignableInCurr(((Rope)yyVals[0+yyTop]), null), null);
    return yyVal;
};
states[387] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(lexer.getPosition(), null, support.assignableInCurr(((Rope)yyVals[-2+yyTop]), null), ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[388] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(lexer.getPosition(), null, new StarParseNode(lexer.getPosition()), null);
    return yyVal;
};
states[389] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new MultipleAsgnParseNode(support.getPosition(((ListParseNode)yyVals[0+yyTop])), null, null, ((ListParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[390] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-1+yyTop]), ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[391] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]), (Rope) null, ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[392] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(lexer.getPosition(), null, ((Rope)yyVals[-1+yyTop]), ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[393] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(((BlockArgParseNode)yyVals[0+yyTop]).getPosition(), null, (Rope) null, ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[394] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
    return yyVal;
};
states[395] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(lexer.getPosition(), null, (Rope) null, null);
    return yyVal;
};
states[396] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), ((ListParseNode)yyVals[-3+yyTop]), ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[397] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-7+yyTop]).getPosition(), ((ListParseNode)yyVals[-7+yyTop]), ((ListParseNode)yyVals[-5+yyTop]), ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[398] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[399] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), ((ListParseNode)yyVals[-3+yyTop]), null, ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[400] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), null, ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[401] = (support, lexer, yyVal, yyVals, yyTop) -> {
    RestArgParseNode rest = new UnnamedRestArgParseNode(((ListParseNode)yyVals[-1+yyTop]).getPosition(), TranslatorEnvironment.TEMP_PREFIX + "anon_rest", support.getCurrentScope().addVariable("*"), false);
    yyVal = support.new_args(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]), null, rest, null, (ArgsTailHolder) null);
    return yyVal;
};
states[402] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), null, ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[403] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[404] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(support.getPosition(((ListParseNode)yyVals[-3+yyTop])), null, ((ListParseNode)yyVals[-3+yyTop]), ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[405] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(support.getPosition(((ListParseNode)yyVals[-5+yyTop])), null, ((ListParseNode)yyVals[-5+yyTop]), ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[406] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(support.getPosition(((ListParseNode)yyVals[-1+yyTop])), null, ((ListParseNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[407] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), null, ((ListParseNode)yyVals[-3+yyTop]), null, ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[408] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((RestArgParseNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[409] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((RestArgParseNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[410] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[411] = (support, lexer, yyVal, yyVals, yyTop) -> {
    /* was $$ = null;*/
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
};
states[412] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.commandStart = true;
    yyVal = ((ArgsParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[413] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
};
states[414] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
};
states[415] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = ((ArgsParseNode)yyVals[-2+yyTop]);
    return yyVal;
};
states[416] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[417] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[418] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[419] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[420] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.new_bv(((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[421] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[422] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.pushBlockScope();
    yyVal = lexer.getLeftParenBegin();
    lexer.setLeftParenBegin(lexer.incrementParenNest());
    return yyVal;
};
states[423] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
    lexer.getCmdArgumentState().reset();
    return yyVal;
};
states[424] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
    lexer.getCmdArgumentState().restart();
    yyVal = new LambdaParseNode(((ArgsParseNode)yyVals[-2+yyTop]).getPosition(), ((ArgsParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]), support.getCurrentScope());
    lexer.setLeftParenBegin(((Integer)yyVals[-3+yyTop]));
    support.popCurrentScope();
    return yyVal;
};
states[425] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ArgsParseNode)yyVals[-2+yyTop]);
    return yyVal;
};
states[426] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ArgsParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[427] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[428] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[429] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((IterParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[430] = (support, lexer, yyVal, yyVals, yyTop) -> {
    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
    if (((ParseNode)yyVals[-1+yyTop]) instanceof YieldParseNode) {
        lexer.compile_error(PID.BLOCK_GIVEN_TO_YIELD, "block given to yield");
    }
    if (((ParseNode)yyVals[-1+yyTop]) instanceof BlockAcceptingParseNode && ((BlockAcceptingParseNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassParseNode) {
        lexer.compile_error(PID.BLOCK_ARG_AND_BLOCK_GIVEN, "Both block arg and actual block given.");
    }
    if (((ParseNode)yyVals[-1+yyTop]) instanceof NonLocalControlFlowParseNode) {
        ((BlockAcceptingParseNode) ((NonLocalControlFlowParseNode)yyVals[-1+yyTop]).getValueNode()).setIterNode(((IterParseNode)yyVals[0+yyTop]));
    } else {
        ((BlockAcceptingParseNode)yyVals[-1+yyTop]).setIterNode(((IterParseNode)yyVals[0+yyTop]));
    }
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    ((ParseNode)yyVal).setPosition(((ParseNode)yyVals[-1+yyTop]).getPosition());
    return yyVal;
};
states[431] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[432] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((IterParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[433] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-4+yyTop]), ((Rope)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((IterParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[434] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.frobnicate_fcall_args(((FCallParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    yyVal = ((FCallParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[435] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[436] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[437] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[0+yyTop]), null, null);
    return yyVal;
};
states[438] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-2+yyTop]), ((Rope)yyVals[-1+yyTop]), RopeConstants.CALL, ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[439] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_call(((ParseNode)yyVals[-2+yyTop]), RopeConstants.CALL, ((ParseNode)yyVals[0+yyTop]), null);
    return yyVal;
};
states[440] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_super(((SourceIndexLength)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[441] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ZSuperParseNode(((SourceIndexLength)yyVals[0+yyTop]));
    return yyVal;
};
states[442] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-3+yyTop]) instanceof SelfParseNode) {
        yyVal = support.new_fcall(RopeConstants.LBRACKET_RBRACKET);
        support.frobnicate_fcall_args(((FCallParseNode)yyVal), ((ParseNode)yyVals[-1+yyTop]), null);
    } else {
        yyVal = support.new_call(((ParseNode)yyVals[-3+yyTop]), RopeConstants.LBRACKET_RBRACKET, ((ParseNode)yyVals[-1+yyTop]), null);
    }
    return yyVal;
};
states[443] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((IterParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[444] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((IterParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[445] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getPosition();
    return yyVal;
};
states[446] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.pushBlockScope();
    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack()) >> 1;
    lexer.getCmdArgumentState().reset();
    return yyVal;
};
states[447] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IterParseNode(((SourceIndexLength)yyVals[-3+yyTop]), ((ArgsParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), support.getCurrentScope());
     support.popCurrentScope();
    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
    return yyVal;
};
states[448] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getPosition();
    return yyVal;
};
states[449] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.pushBlockScope();
    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
    lexer.getCmdArgumentState().reset();
    return yyVal;
};
states[450] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new IterParseNode(((SourceIndexLength)yyVals[-3+yyTop]), ((ArgsParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]), support.getCurrentScope());
     support.popCurrentScope();
    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
    return yyVal;
};
states[451] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newWhenNode(((SourceIndexLength)yyVals[-4+yyTop]), ((ParseNode)yyVals[-3+yyTop]), ((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[454] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode node;
    if (((ParseNode)yyVals[-3+yyTop]) != null) {
        node = support.appendToBlock(support.node_assign(((ParseNode)yyVals[-3+yyTop]), new GlobalVarParseNode(((SourceIndexLength)yyVals[-5+yyTop]), support.symbolID(RopeConstants.DOLLAR_BANG))), ((ParseNode)yyVals[-1+yyTop]));
        if (((ParseNode)yyVals[-1+yyTop]) != null) {
            node.setPosition(((SourceIndexLength)yyVals[-5+yyTop]));
        }
    } else {
        node = ((ParseNode)yyVals[-1+yyTop]);
    }
    ParseNode body = support.makeNullNil(node);
    yyVal = new RescueBodyParseNode(((SourceIndexLength)yyVals[-5+yyTop]), ((ParseNode)yyVals[-4+yyTop]), body, ((RescueBodyParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[455] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null; 
    return yyVal;
};
states[456] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newArrayNode(((ParseNode)yyVals[0+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[457] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.splat_array(((ParseNode)yyVals[0+yyTop]));
    if (yyVal == null) yyVal = ((ParseNode)yyVals[0+yyTop]); /* ArgsCat or ArgsPush*/
    return yyVal;
};
states[459] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[461] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[463] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((NumericParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[464] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.asSymbol(lexer.getPosition(), ((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[466] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]) instanceof EvStrParseNode ? new DStrParseNode(((ParseNode)yyVals[0+yyTop]).getPosition(), lexer.getEncoding()).add(((ParseNode)yyVals[0+yyTop])) : ((ParseNode)yyVals[0+yyTop]);
    /*
    NODE *node = $1;
    if (!node) {
        node = NEW_STR(STR_NEW0());
    } else {
        node = evstr2dstr(node);
    }
    $$ = node;
    */
    return yyVal;
};
states[467] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((StrParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[468] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[469] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.literal_concat(((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[470] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.heredoc_dedent(((ParseNode)yyVals[-1+yyTop]));
    lexer.setHeredocIndent(0);
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[471] = (support, lexer, yyVal, yyVals, yyTop) -> {
    SourceIndexLength position = support.getPosition(((ParseNode)yyVals[-1+yyTop]));

    lexer.heredoc_dedent(((ParseNode)yyVals[-1+yyTop]));
    lexer.setHeredocIndent(0);

    if (((ParseNode)yyVals[-1+yyTop]) == null) {
        yyVal = new XStrParseNode(position, null, CodeRange.CR_7BIT);
    } else if (((ParseNode)yyVals[-1+yyTop]) instanceof StrParseNode) {
        yyVal = new XStrParseNode(position, (Rope) ((StrParseNode)yyVals[-1+yyTop]).getValue(), ((StrParseNode)yyVals[-1+yyTop]).getCodeRange());
    } else if (((ParseNode)yyVals[-1+yyTop]) instanceof DStrParseNode) {
        yyVal = new DXStrParseNode(position, ((DStrParseNode)yyVals[-1+yyTop]));

        ((ParseNode)yyVal).setPosition(position);
    } else {
        yyVal = new DXStrParseNode(position).add(((ParseNode)yyVals[-1+yyTop]));
    }
    return yyVal;
};
states[472] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.newRegexpNode(support.getPosition(((ParseNode)yyVals[-1+yyTop])), ((ParseNode)yyVals[-1+yyTop]), ((RegexpParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[473] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[474] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(lexer.getPosition());
    return yyVal;
};
states[475] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[-1+yyTop]) instanceof EvStrParseNode ? new DStrParseNode(((ListParseNode)yyVals[-2+yyTop]).getPosition(), lexer.getEncoding()).add(((ParseNode)yyVals[-1+yyTop])) : ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[476] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[477] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.literal_concat(((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[478] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[479] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(lexer.getPosition());
    return yyVal;
};
states[480] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[-1+yyTop]) instanceof EvStrParseNode ? new DSymbolParseNode(((ListParseNode)yyVals[-2+yyTop]).getPosition()).add(((ParseNode)yyVals[-1+yyTop])) : support.asSymbol(((ListParseNode)yyVals[-2+yyTop]).getPosition(), ((ParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[481] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[482] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[483] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(lexer.getPosition());
    return yyVal;
};
states[484] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[485] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(lexer.getPosition());
    return yyVal;
};
states[486] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(support.asSymbol(((ListParseNode)yyVals[-2+yyTop]).getPosition(), ((ParseNode)yyVals[-1+yyTop])));
    return yyVal;
};
states[487] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.createStr(RopeOperations.emptyRope(lexer.getEncoding()), 0);
    return yyVal;
};
states[488] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.literal_concat(((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[489] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[490] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.literal_concat(((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[491] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[492] = (support, lexer, yyVal, yyVals, yyTop) -> {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(((ParseNode)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[493] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[494] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getStrTerm();
    lexer.setStrTerm(null);
    lexer.setState(EXPR_BEG);
    return yyVal;
};
states[495] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
    yyVal = new EvStrParseNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[496] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getStrTerm();
    lexer.setStrTerm(null);
    lexer.getConditionState().stop();
    return yyVal;
};
states[497] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getCmdArgumentState().getStack();
    lexer.getCmdArgumentState().reset();
    return yyVal;
};
states[498] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getState();
    lexer.setState(EXPR_BEG);
    return yyVal;
};
states[499] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getBraceNest();
    lexer.setBraceNest(0);
    return yyVal;
};
states[500] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.getHeredocIndent();
    lexer.setHeredocIndent(0);
    return yyVal;
};
states[501] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.getConditionState().restart();
    lexer.setStrTerm(((StrTerm)yyVals[-6+yyTop]));
    lexer.getCmdArgumentState().reset(((Long)yyVals[-5+yyTop]).longValue());
    lexer.setState(((Integer)yyVals[-4+yyTop]));
    lexer.setBraceNest(((Integer)yyVals[-3+yyTop]));
    lexer.setHeredocIndent(((Integer)yyVals[-2+yyTop]));
    lexer.setHeredocLineIndent(-1);

    yyVal = support.newEvStrNode(support.getPosition(((ParseNode)yyVals[-1+yyTop])), ((ParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[502] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new GlobalVarParseNode(lexer.getPosition(), support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[503] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new InstVarParseNode(lexer.getPosition(), support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[504] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ClassVarParseNode(lexer.getPosition(), support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[506] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_END|EXPR_ENDARG);
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[508] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[509] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[510] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[511] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_END|EXPR_ENDARG);

    /* DStrNode: :"some text #{some expression}"*/
    /* StrNode: :"some text"*/
    /* EvStrNode :"#{some expression}"*/
    /* Ruby 1.9 allows empty strings as symbols*/
    if (((ParseNode)yyVals[-1+yyTop]) == null) {
        yyVal = support.asSymbol(lexer.getPosition(), RopeConstants.EMPTY_US_ASCII_ROPE);
    } else if (((ParseNode)yyVals[-1+yyTop]) instanceof DStrParseNode) {
        yyVal = new DSymbolParseNode(((ParseNode)yyVals[-1+yyTop]).getPosition(), ((DStrParseNode)yyVals[-1+yyTop]));
    } else if (((ParseNode)yyVals[-1+yyTop]) instanceof StrParseNode) {
        yyVal = support.asSymbol(((ParseNode)yyVals[-1+yyTop]).getPosition(), ((ParseNode)yyVals[-1+yyTop]));
    } else {
        yyVal = new DSymbolParseNode(((ParseNode)yyVals[-1+yyTop]).getPosition());
        ((DSymbolParseNode)yyVal).add(((ParseNode)yyVals[-1+yyTop]));
    }
    return yyVal;
};
states[512] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((NumericParseNode)yyVals[0+yyTop]);  
    return yyVal;
};
states[513] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.negateNumeric(((NumericParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[514] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[515] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((FloatParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[516] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((RationalParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[517] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[518] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.declareIdentifier(((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[519] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new InstVarParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[520] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new GlobalVarParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[521] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ConstParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[522] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ClassVarParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])));
    return yyVal;
};
states[523] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new NilParseNode(lexer.tokline);
    return yyVal;
};
states[524] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new SelfParseNode(lexer.tokline);
    return yyVal;
};
states[525] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new TrueParseNode((SourceIndexLength) yyVal);
    return yyVal;
};
states[526] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new FalseParseNode((SourceIndexLength) yyVal);
    return yyVal;
};
states[527] = (support, lexer, yyVal, yyVals, yyTop) -> {
    Encoding encoding = support.getConfiguration().getContext() == null ? UTF8Encoding.INSTANCE : support.getConfiguration().getContext().getEncodingManager().getLocaleEncoding();
    yyVal = new FileParseNode(lexer.tokline, StringOperations.encodeRope(lexer.getFile(), encoding, CR_UNKNOWN));
    return yyVal;
};
states[528] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new FixnumParseNode(lexer.tokline, lexer.tokline.toSourceSection(lexer.getSource()).getStartLine() + lexer.getLineOffset());
    return yyVal;
};
states[529] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new EncodingParseNode(lexer.tokline, lexer.getEncoding());
    return yyVal;
};
states[530] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.assignableLabelOrIdentifier(((Rope)yyVals[0+yyTop]), null);
    return yyVal;
};
states[531] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new InstAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[532] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new GlobalAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[533] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (support.isInDef()) support.compile_error("dynamic constant assignment");

    yyVal = new ConstDeclParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), null, NilImplicitParseNode.NIL);
    return yyVal;
};
states[534] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ClassVarAsgnParseNode(lexer.tokline, support.symbolID(((Rope)yyVals[0+yyTop])), NilImplicitParseNode.NIL);
    return yyVal;
};
states[535] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to nil");
    yyVal = null;
    return yyVal;
};
states[536] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't change the value of self");
    yyVal = null;
    return yyVal;
};
states[537] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to true");
    yyVal = null;
    return yyVal;
};
states[538] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to false");
    yyVal = null;
    return yyVal;
};
states[539] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __FILE__");
    yyVal = null;
    return yyVal;
};
states[540] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __LINE__");
    yyVal = null;
    return yyVal;
};
states[541] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.compile_error("Can't assign to __ENCODING__");
    yyVal = null;
    return yyVal;
};
states[542] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[543] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[544] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_BEG);
    lexer.commandStart = true;
    return yyVal;
};
states[545] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[546] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[547] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ArgsParseNode)yyVals[-1+yyTop]);
    lexer.setState(EXPR_BEG);
    lexer.commandStart = true;
    return yyVal;
};
states[548] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = lexer.inKwarg;
    lexer.inKwarg = true;
    lexer.setState(lexer.getState() | EXPR_LABEL);
    return yyVal;
};
states[549] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.inKwarg = ((Boolean)yyVals[-2+yyTop]);
     yyVal = ((ArgsParseNode)yyVals[-1+yyTop]);
     lexer.setState(EXPR_BEG);
     lexer.commandStart = true;
    return yyVal;
};
states[550] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), ((Rope)yyVals[-1+yyTop]), ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[551] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]), (Rope) null, ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[552] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(lexer.getPosition(), null, ((Rope)yyVals[-1+yyTop]), ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[553] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(((BlockArgParseNode)yyVals[0+yyTop]).getPosition(), null, (Rope) null, ((BlockArgParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[554] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
    return yyVal;
};
states[555] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args_tail(lexer.getPosition(), null, (Rope) null, null);
    return yyVal;
};
states[556] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), ((ListParseNode)yyVals[-3+yyTop]), ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[557] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-7+yyTop]).getPosition(), ((ListParseNode)yyVals[-7+yyTop]), ((ListParseNode)yyVals[-5+yyTop]), ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[558] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[559] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), ((ListParseNode)yyVals[-3+yyTop]), null, ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[560] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), ((ListParseNode)yyVals[-3+yyTop]), null, ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[561] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), ((ListParseNode)yyVals[-5+yyTop]), null, ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[562] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-1+yyTop]).getPosition(), ((ListParseNode)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[563] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), null, ((ListParseNode)yyVals[-3+yyTop]), ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[564] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-5+yyTop]).getPosition(), null, ((ListParseNode)yyVals[-5+yyTop]), ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[565] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-1+yyTop]).getPosition(), null, ((ListParseNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[566] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ListParseNode)yyVals[-3+yyTop]).getPosition(), null, ((ListParseNode)yyVals[-3+yyTop]), null, ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[567] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((RestArgParseNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgParseNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[568] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((RestArgParseNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgParseNode)yyVals[-3+yyTop]), ((ListParseNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[569] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
};
states[570] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
};
states[571] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("formal argument cannot be a constant");
    return yyVal;
};
states[572] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("formal argument cannot be an instance variable");
    return yyVal;
};
states[573] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("formal argument cannot be a global variable");
    return yyVal;
};
states[574] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.yyerror("formal argument cannot be a class variable");
    return yyVal;
};
states[575] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]); /* Not really reached*/
    return yyVal;
};
states[576] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.formal_argument(((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[577] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(((Rope)yyVals[0+yyTop]));
    yyVal = support.arg_var(((Rope)yyVals[0+yyTop]));
    return yyVal;
};
states[578] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = ((ArgumentParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[579] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    /*            {
            ID tid = internal_id();
            arg_var(tid);
            if (dyna_in_block()) {
$2->nd_value = NEW_DVAR(tid);
            }
            else {
$2->nd_value = NEW_LVAR(tid);
            }
            $$ = NEW_ARGS_AUX(tid, 1);
            $$->nd_next = $2;*/
    return yyVal;
};
states[580] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(lexer.getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[581] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[0+yyTop]));
    yyVal = ((ListParseNode)yyVals[-2+yyTop]);
    return yyVal;
};
states[582] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.arg_var(support.formal_argument(((Rope)yyVals[0+yyTop])));
    lexer.setCurrentArg(((Rope)yyVals[0+yyTop]));
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[583] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = support.keyword_arg(((ParseNode)yyVals[0+yyTop]).getPosition(), support.assignableKeyword(((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[584] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = support.keyword_arg(lexer.getPosition(), support.assignableKeyword(((Rope)yyVals[0+yyTop]), RequiredKeywordArgumentValueParseNode.INSTANCE));
    return yyVal;
};
states[585] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.keyword_arg(support.getPosition(((ParseNode)yyVals[0+yyTop])), support.assignableKeyword(((Rope)yyVals[-1+yyTop]), ((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[586] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.keyword_arg(lexer.getPosition(), support.assignableKeyword(((Rope)yyVals[0+yyTop]), RequiredKeywordArgumentValueParseNode.INSTANCE));
    return yyVal;
};
states[587] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(((ParseNode)yyVals[0+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[588] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[589] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new ArrayParseNode(((ParseNode)yyVals[0+yyTop]).getPosition(), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[590] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((ListParseNode)yyVals[-2+yyTop]).add(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[591] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[592] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[593] = (support, lexer, yyVal, yyVals, yyTop) -> {
    support.shadowing_lvar(((Rope)yyVals[0+yyTop]));
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[594] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ParserSupport.INTERNAL_ID;
    return yyVal;
};
states[595] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = new OptArgParseNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), support.assignableLabelOrIdentifier(((ArgumentParseNode)yyVals[-2+yyTop]).getName(), ((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[596] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setCurrentArg(null);
    yyVal = new OptArgParseNode(support.getPosition(((ParseNode)yyVals[0+yyTop])), support.assignableLabelOrIdentifier(((ArgumentParseNode)yyVals[-2+yyTop]).getName(), ((ParseNode)yyVals[0+yyTop])));
    return yyVal;
};
states[597] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new BlockParseNode(((ParseNode)yyVals[0+yyTop]).getPosition()).add(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[598] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.appendToBlock(((ListParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[599] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new BlockParseNode(((ParseNode)yyVals[0+yyTop]).getPosition()).add(((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[600] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.appendToBlock(((ListParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[601] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[602] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[603] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (!support.is_local_id(((Rope)yyVals[0+yyTop]))) {
        support.yyerror("rest argument must be local variable");
    }
                    
    yyVal = new RestArgParseNode(support.arg_var(support.shadowing_lvar(((Rope)yyVals[0+yyTop]))));
    return yyVal;
};
states[604] = (support, lexer, yyVal, yyVals, yyTop) -> {
  /* FIXME: bytelist_love: somewhat silly to remake the empty bytelist over and over but this type should change (using null vs "" is a strange distinction).*/
  yyVal = new UnnamedRestArgParseNode(lexer.getPosition(), TranslatorEnvironment.TEMP_PREFIX + "rest", support.getCurrentScope().addVariable("*"), true);
    return yyVal;
};
states[605] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[606] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[607] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (!support.is_local_id(((Rope)yyVals[0+yyTop]))) {
        support.yyerror("block argument must be local variable");
    }
                    
    yyVal = new BlockArgParseNode(support.arg_var(support.shadowing_lvar(((Rope)yyVals[0+yyTop]))));
    return yyVal;
};
states[608] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((BlockArgParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[609] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[610] = (support, lexer, yyVal, yyVals, yyTop) -> {
    value_expr(lexer, ((ParseNode)yyVals[0+yyTop]));
    yyVal = ((ParseNode)yyVals[0+yyTop]);
    return yyVal;
};
states[611] = (support, lexer, yyVal, yyVals, yyTop) -> {
    lexer.setState(EXPR_BEG);
    return yyVal;
};
states[612] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-1+yyTop]) == null) {
        support.yyerror("can't define single method for ().");
    } else if (((ParseNode)yyVals[-1+yyTop]) instanceof ILiteralNode) {
        support.yyerror("can't define single method for literals.");
    }
    value_expr(lexer, ((ParseNode)yyVals[-1+yyTop]));
    yyVal = ((ParseNode)yyVals[-1+yyTop]);
    return yyVal;
};
states[613] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new HashParseNode(lexer.getPosition());
    return yyVal;
};
states[614] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.remove_duplicate_keys(((HashParseNode)yyVals[-1+yyTop]));
    return yyVal;
};
states[615] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = new HashParseNode(lexer.getPosition(), ((ParseNodeTuple)yyVals[0+yyTop]));
    return yyVal;
};
states[616] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((HashParseNode)yyVals[-2+yyTop]).add(((ParseNodeTuple)yyVals[0+yyTop]));
    return yyVal;
};
states[617] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.createKeyValue(((ParseNode)yyVals[-2+yyTop]), ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[618] = (support, lexer, yyVal, yyVals, yyTop) -> {
    ParseNode label = support.asSymbol(support.getPosition(((ParseNode)yyVals[0+yyTop])), ((Rope)yyVals[-1+yyTop]));
    yyVal = support.createKeyValue(label, ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[619] = (support, lexer, yyVal, yyVals, yyTop) -> {
    if (((ParseNode)yyVals[-2+yyTop]) instanceof StrParseNode) {
        DStrParseNode dnode = new DStrParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), lexer.getEncoding());
        dnode.add(((ParseNode)yyVals[-2+yyTop]));
        yyVal = support.createKeyValue(new DSymbolParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), dnode), ((ParseNode)yyVals[0+yyTop]));
    } else if (((ParseNode)yyVals[-2+yyTop]) instanceof DStrParseNode) {
        yyVal = support.createKeyValue(new DSymbolParseNode(support.getPosition(((ParseNode)yyVals[-2+yyTop])), ((DStrParseNode)yyVals[-2+yyTop])), ((ParseNode)yyVals[0+yyTop]));
    } else {
        support.compile_error("Uknown type for assoc in strings: " + ((ParseNode)yyVals[-2+yyTop]));
    }

    return yyVal;
};
states[620] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = support.createKeyValue(null, ((ParseNode)yyVals[0+yyTop]));
    return yyVal;
};
states[621] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[622] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[623] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[624] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[625] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[626] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[627] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[628] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[629] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[630] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[631] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[632] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[633] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[634] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[636] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[641] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[642] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = ((Rope)yyVals[0+yyTop]);
    return yyVal;
};
states[650] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
states[651] = (support, lexer, yyVal, yyVals, yyTop) -> {
    yyVal = null;
    return yyVal;
};
}
// line 2738 "RubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration) {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        yyparse(lexer, null);
        
        return support.getResult();
    }
}
// CheckStyle: stop generated
// @formatter:on
// line 10642 "-"
