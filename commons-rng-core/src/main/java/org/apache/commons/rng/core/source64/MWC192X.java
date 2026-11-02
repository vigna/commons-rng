/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.core.source64;

import java.math.BigInteger;

import org.apache.commons.rng.ArbitrarilyJumpableUniformRandomProvider;
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * A fast all-purpose 64-bit generator.
 *
 * <p>This is a scrambled multiply-with-carry generator with base 2<sup>64</sup>,
 * lag 2 and multiplier {@code 0xff4e24dd418f5433}. Its state is formed by two
 * 64-bit digits and a carry. Memory footprint is 192 bits and the period is
 * (<i>m</i> - 1) / 2, where <i>m</i> = {@code 0xff4e24dd418f5433} &middot;
 * 2<sup>128</sup> - 1 is a safe prime: the generator simulates a multiplicative
 * congruential generator with modulus <i>m</i> and multiplier 2<sup>-64</sup> mod <i>m</i>,
 * so jumps by any distance are multiplications of the state modulo <i>m</i> by a
 * power of the multiplier, which take time logarithmic in the distance. The output is the
 * most recent digit xored with the carry (the scrambler X).</p>
 *
 * <p>The carry must satisfy 0 &lt; <i>c</i> &lt; {@code 0xff4e24dd418f5433} - 1:
 * an invalid carry in the seed is replaced by 1.</p>
 *
 * @see <a href="https://github.com/vigna/MWCPRNG/blob/main/prngs/MWC192X.c">Original source code</a>
 * @see <a href="https://arxiv.org/abs/2610.27313">Scrambled Multiply-with-Carry Pseudorandom Number Generators</a>
 * @since 1.8
 */
public class MWC192X extends LongProvider
    implements LongJumpableUniformRandomProvider, ArbitrarilyJumpableUniformRandomProvider {
    /** Size of the state vector. */
    private static final int SEED_SIZE = 3;
    /** The multiplier. */
    private static final long A = 0xff4e24dd418f5433L;
    /** The modulus A * 2^128 - 1. */
    private static final BigInteger MOD = toUnsigned(A).shiftLeft(128).subtract(BigInteger.ONE);
    /** Multiplier for the jump: 2^(-64 * 2^96) mod m. */
    private static final BigInteger JUMP_MULTIPLIER =
        new BigInteger("702eb67f0959a76b21239e0fbd1ccbb5d323a2a406f64957", 16);
    /** Multiplier for the long jump: 2^(-64 * 2^144) mod m. */
    private static final BigInteger LONG_JUMP_MULTIPLIER =
        new BigInteger("d6c731b43ecad305bceb1c681d8e45f7671d31e08a897fff", 16);
    /** The multiplier of the underlying multiplicative congruential generator:
     * 2<sup>-64</sup> mod m = A * 2<sup>64</sup>. */
    private static final BigInteger MCG_MULTIPLIER = toUnsigned(A).shiftLeft(64);
    /** Base-2 logarithm of the smallest power of two larger than the period (m - 1) / 2. */
    private static final int LOG_PERIOD = 191;
    /** The period (m - 1) / 2, rounded down to a double. */
    private static final double PERIOD = roundDown(MOD.shiftRight(1));

    /** First digit. */
    private long x;
    /** Second (most recent) digit. */
    private long y;
    /** Carry. */
    private long c;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed (two digits and the carry).
     * If the length is larger than 3, only the first 3 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set.
     */
    public MWC192X(long[] seed) {
        setState(extendSeed(seed, SEED_SIZE));
    }

    /**
     * Creates a new instance using a 3 element seed.
     *
     * @param seed0 Initial seed element 0 (first digit).
     * @param seed1 Initial seed element 1 (second digit).
     * @param seed2 Initial seed element 2 (carry).
     */
    public MWC192X(long seed0, long seed1, long seed2) {
        setState(new long[] {seed0, seed1, seed2});
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected MWC192X(MWC192X source) {
        super(source);
        x = source.x;
        y = source.y;
        c = source.c;
    }

    /**
     * Copies the state from the array into the generator state.
     *
     * @param state the new state
     */
    private void setState(long[] state) {
        x = state[0];
        y = state[1];
        // The carry must be in [1, A - 2]
        c = Long.compareUnsigned(state[2] - 1, A - 3) <= 0 ? state[2] : 1;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new long[] {x, y, c}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] parts = splitStateInternal(s, SEED_SIZE * 8);
        setState(NumberFactory.makeLongArray(parts[0]));
        super.setStateInternal(parts[1]);
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        final long result = y ^ c;
        final long lo = A * x;
        final long hi = PhiloxSupport.unsignedMultiplyHigh(A, x);
        final long t = lo + c;
        x = y;
        y = t;
        // Carry of the unsigned sum lo + c
        c = hi + (((lo & c) | ((lo | c) & ~t)) >>> 63);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>96</sup>
     * calls to {@link UniformRandomProvider#nextLong() nextLong()}. It can provide
     * up to 2<sup>95</sup> non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final UniformRandomProvider copy = new MWC192X(this);
        performJump(JUMP_MULTIPLIER);
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>144</sup> calls to
     * {@link UniformRandomProvider#nextLong() nextLong()}. It can provide up to
     * 2<sup>47</sup> non-overlapping subsequences of length 2<sup>144</sup>; each
     * subsequence can provide up to 2<sup>48</sup> non-overlapping subsequences of
     * length 2<sup>96</sup> using the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = new MWC192X(this);
        performJump(LONG_JUMP_MULTIPLIER);
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump is performed by a single modular exponentiation.</p>
     */
    @Override
    public ArbitrarilyJumpableUniformRandomProvider jump(double distance) {
        LongJumpDistances.validateJump(distance, PERIOD);
        final MWC192X copy = new MWC192X(this);
        // Integer part of the distance, least significant word first
        final long[] words = new long[SEED_SIZE + 1];
        LongJumpDistances.writeUnsignedInteger(distance, words);
        BigInteger n = BigInteger.ZERO;
        for (int i = words.length - 1; i >= 0; i--) {
            n = n.shiftLeft(64).or(toUnsigned(words[i]));
        }
        if (n.signum() != 0) {
            performJump(MCG_MULTIPLIER.modPow(n, MOD));
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump is performed by a single modular exponentiation.</p>
     */
    @Override
    public ArbitrarilyJumpableUniformRandomProvider jumpPowerOfTwo(int logDistance) {
        LongJumpDistances.validateJumpPowerOfTwo(logDistance, LOG_PERIOD);
        final MWC192X copy = new MWC192X(this);
        // Negative logDistance is a distance in (0, 1): no jump
        if (logDistance >= 0) {
            performJump(MCG_MULTIPLIER.modPow(BigInteger.ONE.shiftLeft(logDistance), MOD));
        }
        return copy;
    }

    /**
     * Multiplies the state, read as the integer x + y 2<sup>64</sup> + c 2<sup>128</sup>,
     * by the given multiplier modulo m. Resets the cached state of the generator.
     *
     * @param multiplier Multiplier.
     */
    private void performJump(BigInteger multiplier) {
        final BigInteger s = toUnsigned(c).shiftLeft(64).or(toUnsigned(y)).shiftLeft(64).or(toUnsigned(x))
            .multiply(multiplier).mod(MOD);
        x = s.longValue();
        y = s.shiftRight(64).longValue();
        c = s.shiftRight(128).longValue();
        resetCachedState();
    }

    /**
     * Converts a positive BigInteger to the largest double not greater than it.
     *
     * @param v Value.
     * @return the rounded value
     */
    private static double roundDown(BigInteger v) {
        // Truncate to the 53 most significant bits, which convert exactly to a double
        final int shift = v.bitLength() - 53;
        return Math.scalb((double) v.shiftRight(shift).longValue(), shift);
    }

    /**
     * Converts a long to a non-negative BigInteger, interpreting it as unsigned.
     *
     * @param v Value.
     * @return the unsigned value
     */
    private static BigInteger toUnsigned(long v) {
        final BigInteger b = BigInteger.valueOf(v);
        return v < 0 ? b.add(BigInteger.ONE.shiftLeft(64)) : b;
    }
}
