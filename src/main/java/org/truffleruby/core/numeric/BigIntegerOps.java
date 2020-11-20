/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.numeric;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.truffleruby.SuppressFBWarnings;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Wrapper for methods of {@link BigInteger} decorated with a {@link TruffleBoundary} annotation, as these methods
 * should not be called in PE code (non-trivial JDK code with no Truffle-level profiling and might make things like
 * Throwable methods reachable) and are therefore blacklisted by SVM. */
public final class BigIntegerOps {

    @TruffleBoundary
    public static BigInteger create(byte[] value) {
        return new BigInteger(value);
    }

    @TruffleBoundary
    public static BigInteger valueOf(long value) {
        return BigInteger.valueOf(value);
    }

    @TruffleBoundary
    public static BigInteger negate(BigInteger value) {
        return value.negate();
    }

    @TruffleBoundary
    public static BigInteger negate(long value) {
        return valueOf(value).negate();
    }

    @TruffleBoundary
    public static BigInteger add(BigInteger a, BigInteger b) {
        return a.add(b);
    }

    @TruffleBoundary
    public static BigInteger add(long a, long b) {
        return valueOf(a).add(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger add(BigInteger a, long b) {
        return a.add(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger subtract(BigInteger a, BigInteger b) {
        return a.subtract(b);
    }

    @TruffleBoundary
    public static BigInteger subtract(long a, long b) {
        return valueOf(a).subtract(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger subtract(BigInteger a, long b) {
        return a.subtract(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger subtract(long a, BigInteger b) {
        return valueOf(a).subtract(b);
    }

    @TruffleBoundary
    public static BigInteger multiply(BigInteger a, BigInteger b) {
        return a.multiply(b);
    }

    @TruffleBoundary
    public static BigInteger multiply(long a, long b) {
        return valueOf(a).multiply(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger multiply(BigInteger a, long b) {
        return a.multiply(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger not(BigInteger value) {
        return value.not();
    }

    @TruffleBoundary
    public static BigInteger and(BigInteger a, BigInteger b) {
        return a.and(b);
    }

    @TruffleBoundary
    public static BigInteger and(BigInteger a, long b) {
        return a.and(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger or(BigInteger a, BigInteger b) {
        return a.or(b);
    }

    @TruffleBoundary
    public static BigInteger or(BigInteger a, long b) {
        return a.or(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger xor(BigInteger a, BigInteger b) {
        return a.xor(b);
    }

    @TruffleBoundary
    public static BigInteger xor(BigInteger a, long b) {
        return a.xor(valueOf(b));
    }

    @TruffleBoundary
    public static BigInteger shiftLeft(BigInteger value, int n) {
        return value.shiftLeft(n);
    }

    @TruffleBoundary
    public static BigInteger shiftLeft(long value, int n) {
        return valueOf(value).shiftLeft(n);
    }

    @TruffleBoundary
    public static BigInteger shiftRight(BigInteger value, int n) {
        return value.shiftRight(n);
    }

    @TruffleBoundary
    public static BigInteger shiftRight(long value, int n) {
        return valueOf(value).shiftRight(n);
    }

    @TruffleBoundary
    public static BigInteger pow(BigInteger value, int exponent) {
        return value.pow(exponent);
    }

    @TruffleBoundary
    public static double pow(BigInteger value, double exponent) {
        return Math.pow(value.doubleValue(), exponent);
    }

    @TruffleBoundary
    public static BigInteger modPow(BigInteger base, BigInteger exponent, BigInteger m) {
        return base.modPow(exponent, m);
    }

    @TruffleBoundary
    public static BigInteger abs(BigInteger value) {
        return value.abs();
    }

    @TruffleBoundary
    public static BigInteger abs(long value) {
        return valueOf(value).abs();
    }

    @TruffleBoundary
    public static int compare(BigInteger a, BigInteger b) {
        return a.compareTo(b);
    }

    @TruffleBoundary
    public static int compare(BigInteger a, long b) {
        return a.compareTo(valueOf(b));
    }

    @TruffleBoundary
    public static int compare(long a, BigInteger b) {
        return valueOf(a).compareTo(b);
    }

    @TruffleBoundary
    public static int compare(BigInteger a, double b) {
        // Emulate MRI behaviour.
        // This is also more precise than converting the BigInteger to a double.
        int cmp = a.compareTo(fromDouble(b));
        double fractional = b % 1;
        return cmp != 0 || fractional == 0.0
                ? cmp
                : fractional < 0 ? -1 : 1;
    }

    // We add these RubyBignum overloads for compare because it is used relatively often,
    // and it helps readability.

    public static int compare(long a, RubyBignum b) {
        return compare(a, b.value);
    }

    public static int compare(RubyBignum a, long b) {
        return compare(a.value, b);
    }

    public static int compare(RubyBignum a, RubyBignum b) {
        return compare(a.value, b.value);
    }

    public static int compare(RubyBignum a, double b) {
        return compare(a.value, b);
    }

    @SuppressFBWarnings("RV") // compare only returns -1, 0 or 1
    public static int compare(double a, RubyBignum b) {
        return -compare(b.value, a);
    }

    @TruffleBoundary
    public static int signum(BigInteger value) {
        return value.signum();
    }

    public static boolean isPositive(RubyBignum value) {
        // The distinction between x > 0 and x >= 0 is moot because bignums are never long-valued.
        return signum(value.value) > 0;
    }

    public static boolean isNegative(RubyBignum value) {
        // The distinction between x < 0 and x <= 0 is moot because bignums are never long-valued.
        return signum(value.value) < 0;
    }

    @TruffleBoundary
    public static int bitLength(BigInteger value) {
        return value.bitLength();
    }

    @TruffleBoundary
    public static boolean testBit(BigInteger value, int n) {
        return value.testBit(n);
    }

    @TruffleBoundary
    public static double doubleValue(BigInteger value) {
        return value.doubleValue();
    }

    public static double doubleValue(RubyBignum value) {
        return doubleValue(value.value);
    }

    @TruffleBoundary
    public static long longValue(BigInteger value) {
        return value.longValue();
    }

    public static long longValue(RubyBignum value) {
        return longValue(value.value);
    }

    @TruffleBoundary
    public static BigInteger fromDouble(double value) {
        return new BigDecimal(value).toBigInteger();
    }

    @TruffleBoundary
    public static byte[] toByteArray(BigInteger value) {
        return value.toByteArray();
    }

    public static Object asUnsignedFixnumOrBignum(long value) {
        // Positive means the initial bit is clear.
        return value >= 0 ? value : BignumOperations.createBignum(negativeValueAsUnsignedBigInteger(value));
    }

    public static Object asUnsignedPrimitiveOrBigInteger(long value) {
        // Positive means the initial bit is clear.
        return value >= 0 ? value : negativeValueAsUnsignedBigInteger(value);
    }

    @TruffleBoundary
    private static BigInteger negativeValueAsUnsignedBigInteger(long value) {
        assert value < 0;
        return BigInteger.valueOf(value & 0x7fff_ffff_ffff_ffffL).setBit(Long.SIZE - 1);
    }

    @TruffleBoundary
    public static boolean equals(BigInteger a, BigInteger b) {
        return a.equals(b);
    }

    @TruffleBoundary
    public static int hashCode(BigInteger value) {
        return value.hashCode();
    }

    public static int hashCode(RubyBignum value) {
        return hashCode(value.value);
    }

    @TruffleBoundary
    public static String toString(BigInteger value) {
        return value.toString();
    }

    @TruffleBoundary
    public static String toString(BigInteger value, int base) {
        return value.toString(base);
    }
}
