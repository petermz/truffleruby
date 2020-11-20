# Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 2.0, or
# GNU General Public License version 2, or
# GNU Lesser General Public License version 2.1.

ABC_ROPE_1 = 'abc'
ABC_ROPE_2 = 'ab' + 'c'
ABC_ROPE_USASCII = 'abc'.force_encoding('us-ascii')
ABC_ROPE_UTF8 = 'abc'.force_encoding('utf-8')

simple_string = 'test'

example "Truffle::Ropes.create_simple_string.length", simple_string.length
example "Truffle::Ropes.create_simple_string.getbyte(0)", simple_string.getbyte(0)
example "Truffle::Ropes.create_simple_string.ord", simple_string.ord

example "'abc'.length", 3
example "'こにちわ'.length", 4

example "'abc'.bytesize", 3
example "'こにちわ'.bytesize", 12

# Comparison against the same rope node instance
example "'abc' == 'abc'", true
example "x = 'abc'; x == x", true
example "x = 'abc'; x == x.dup", true
example "x = 'abc'; 'abc' == x.dup", true
example "ABC_ROPE_1 == ABC_ROPE_1", true

# Comparison against a stable but different string instance, with a stable but
# different rope node instance with the same encoding
example "ABC_ROPE_1 == ABC_ROPE_2", true

# Comparison against an unstable string instance, with a stable but different
# rope node instance with the same encoding
example "ABC_ROPE_1 == 'abc'", true

# Comparison against a stable but different string instance, with a stable but
# different rope node instance with a different but compatible encoding
example "ABC_ROPE_USASCII == ABC_ROPE_UTF8", true

# Comparison against a stable but different string instance, with a stable but
# different rope node instance with a different but compatible encoding
example "ABC_ROPE_USASCII == 'abc'", true

example "'A' == String.from_codepoint(65, Encoding::US_ASCII)", true
example "'A' == 65.chr", true
tagged example "'A'.ord == 65", true

example "'aba'[0] == 'aca'[-1]", true

example "x = 'abc'; x == x.b", true

example "'abc'.ascii_only?", true
example "'こにちわ'.ascii_only?", false

example "'abc'.valid_encoding?", true
example "'こにちわ'.valid_encoding?", true

example "''.empty?", true
example "'abc'.empty?", false
example "'こにちわ'.empty?", false

example "x = 'abc'; y = 'xyz'; x.replace(y) == y", true

tagged example "'abc'.getbyte(0) == 97", true
tagged example "'abc'.getbyte(-1) == 99", true
example "'abc'.getbyte(10_000) == nil", true

example "14.to_s.length", 2
counter example "14.to_s.getbyte(0)" # Doesn't work becuase the bytes are only populated on demand and so aren't constant
