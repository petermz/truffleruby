/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.rope;

import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes;
import org.truffleruby.core.string.StringOperations;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.language.NotProvided;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

@CoreModule("Truffle::Ropes")
public abstract class TruffleRopesNodes {

    @CoreMethod(names = "dump_string", onSingleton = true, required = 1)
    public abstract static class DumpStringNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyString dumpString(RubyString string) {
            final StringBuilder builder = new StringBuilder();

            final Rope rope = string.rope;

            for (int i = 0; i < rope.byteLength(); i++) {
                builder.append(StringUtils.format("\\x%02x", rope.get(i)));
            }

            return makeStringNode.executeMake(builder.toString(), UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
        }

    }

    @CoreMethod(names = "debug_print_rope", onSingleton = true, required = 1, optional = 1)
    public abstract static class DebugPrintRopeNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.DebugPrintRopeNode debugPrintRopeNode = RopeNodesFactory.DebugPrintRopeNodeGen
                .create();

        @TruffleBoundary
        @Specialization
        protected Object debugPrintDefault(RubyString string, NotProvided printString) {
            return debugPrint(string, true);
        }

        @TruffleBoundary
        @Specialization
        protected Object debugPrint(RubyString string, boolean printString) {
            System.err.println("Legend: ");
            System.err.println("BN = Bytes Null? (byte[] not yet populated)");
            System.err.println("BL = Byte Length");
            System.err.println("CL = Character Length");
            System.err.println("CR = Code Range");
            System.err.println("O = Byte Offset (SubstringRope only)");
            System.err.println("T = Times (RepeatingRope only)");
            System.err.println("D = Depth");
            System.err.println("LD = Left Depth (ConcatRope only)");
            System.err.println("RD = Right Depth (ConcatRope only)");
            System.err.println("E = Encoding");

            return debugPrintRopeNode.executeDebugPrint(string.rope, 0, printString);
        }
    }

    /** The returned string (when evaluated) will create a string with the same Rope structure as the string which is
     * passed as argument. */
    @CoreMethod(names = "debug_get_structure_creation", onSingleton = true, required = 1)
    public abstract static class DebugGetStructureCreationNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @TruffleBoundary
        @Specialization
        protected RubyString getStructure(RubyString string) {
            Rope rope = string.rope;
            String result = getStructure(rope);
            byte[] bytes = StringOperations.encodeBytes(result, UTF8Encoding.INSTANCE);
            return makeStringNode.executeMake(bytes, rope.getEncoding(), CodeRange.CR_7BIT);
        }

        protected static String getStructure(Rope rope) {
            if (rope instanceof LeafRope) {
                return getStructure((LeafRope) rope);
            } else if (rope instanceof ConcatRope) {
                return getStructure((ConcatRope) rope);
            } else if (rope instanceof SubstringRope) {
                return getStructure((SubstringRope) rope);
            } else if (rope instanceof RepeatingRope) {
                return getStructure((RepeatingRope) rope);
            } else {
                return "(unknown rope class: " + rope.getClass() + ")";
            }
        }

        private static String getStructure(LeafRope rope) {
            return "\"" + rope.toString() + "\"";
        }

        private static String getStructure(ConcatRope rope) {
            return "(" + getStructure(rope.getLeft()) + " + " + getStructure(rope.getRight()) + ")";
        }

        private static String getStructure(SubstringRope rope) {
            final Rope child = rope.getChild();
            final int characterOffset = RopeOperations
                    .strLength(child.getEncoding(), child.getBytes(), 0, rope.getByteOffset());
            return getStructure(child) + "[" + characterOffset + ", " + rope.characterLength() + "]";
        }

        private static String getStructure(RepeatingRope rope) {
            return "(" + getStructure(rope.getChild()) + "*" + rope.getTimes() + ")";
        }

    }

    @CoreMethod(names = "bytes?", onSingleton = true, required = 1)
    public abstract static class HasBytesNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected boolean hasBytes(RubyString string) {
            return string.rope.getRawBytes() != null;
        }

    }

    @CoreMethod(names = "flatten_rope", onSingleton = true, required = 1)
    public abstract static class FlattenRopeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyString flattenRope(RubyString string,
                @Cached RopeNodes.FlattenNode flattenNode,
                @Cached StringNodes.MakeStringNode makeStringNode) {
            final LeafRope flattened = flattenNode.executeFlatten(string.rope);
            return makeStringNode.fromRope(flattened);
        }

    }

    /* Truffle.create_simple_string creates a string 'test' without any part of the string escaping. Useful for testing
     * compilation of String because most other ways to construct a string can currently escape. */

    @CoreMethod(names = "create_simple_string", onSingleton = true)
    public abstract static class CreateSimpleStringNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyString createSimpleString(
                @Cached StringNodes.MakeStringNode makeStringNode) {
            return makeStringNode
                    .fromRope(new AsciiOnlyLeafRope(new byte[]{ 't', 'e', 's', 't' }, UTF8Encoding.INSTANCE));
        }

    }

}
