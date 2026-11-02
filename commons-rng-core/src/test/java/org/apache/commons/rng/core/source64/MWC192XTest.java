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

import java.util.function.Function;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MWC192XTest {
    /** The size of the array SEED. */
    private static final int SEED_SIZE = 3;

    /*
     * Data from running the executable compiled from the author's C code:
     *   https://github.com/vigna/MWCPRNG/blob/main/prngs/MWC192X.c
     */

    private static final long[] SEED = {
        0x012de1babb3c4104L, 0xa5a818b8fc5aa503L, 0x00000000000000feL
    };

    private static final long[] EXPECTED_SEQUENCE = {
        0xa5a818b8fc5aa5fdL, 0xb343debe279fc431L, 0x6bb88874a850aa47L,
        0x82b629a647a8d1c7L, 0x40dd91cd5af88169L, 0x997a0b0c1ebb52aaL,
        0x5146ae55cd637486L, 0x8474c8ce17b68fb5L, 0x120deb26f40932daL,
        0x57c7dad8ffba1bbbL, 0x82a713db5088f760L, 0xa983e76ec165ecdcL,
        0xa69b2cfdc1d465f5L, 0x03281f845d2944f3L, 0xc5de907125dff23cL,
        0x6a173b1cd732219cL, 0x75a8c535d15d5047L, 0x936180aab69907baL,
        0xa09cc6c9890c10f0L, 0x401d84953799cbb4L, 0x85da38bdeb6fffdaL,
        0x1231e3d7c8b4772fL, 0xf5f26c8ca2032970L, 0xa361b184700addb5L,
        0x202ae7ed1ca3713fL, 0x716b70f8e6165f76L, 0xae23628f3e8f412aL,
        0xc2f617bfebf10206L, 0x4bfa9a024cd211b1L, 0xa498b9996e2de38fL
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        0xbedfe50f5e4de612L, 0x941028c4c40d7297L, 0x0de36f1d7795a42bL,
        0x86a4654ba91408f0L, 0x3590c0c1dc608427L, 0x4ea03ed576593b3bL,
        0x67408343bbdb9fd2L, 0xd99d43a0d6e5d8c1L, 0x24ec7a802245d064L,
        0x12e26485e839476dL, 0x71b295f4724b44b8L, 0xf29676ccb4f3e406L,
        0x78c3c97c69d30ee7L, 0x6926f10d1635a6c9L, 0x99b568fbb94d7e66L,
        0x4b3ab6fbaadfdf76L, 0x36a814e899f1e476L, 0x5d06c798f95fbf22L,
        0x8e5d8c9ebda96f80L, 0x13707f71496af007L, 0x02bef1e136f0477bL,
        0xac056ad84ec3646eL, 0xbebe25fb53d85105L, 0xfcebc7f99d699bc8L,
        0x5063bc2d0e793a79L, 0xc27ff4e996f29aedL, 0xe9ac9a9726f12b65L,
        0x2712444c7c46dd92L, 0x1dc134e40da9be4cL, 0xdc59fe8f0ddcf995L
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        0x19dfe5fe76b1c292L, 0x604f7e46867a54fdL, 0xe62c22294411937aL,
        0x9a21b609eb4b9158L, 0x8092cec3bbc74cdbL, 0x589a24f60dd7961dL,
        0xfa6595eaa74d652dL, 0xe4e25f9c7ac15774L, 0x9f86c062af45c8f3L,
        0x13438ba5c5c0953bL, 0x4dc62a2eb647f361L, 0x7de1e99f9e52b1a3L,
        0xef8415e5b41382b5L, 0x197e7b2f3030addcL, 0xf3ea5d192e6fe65aL,
        0x1d7072bf205db6d1L, 0xd154be7501be3326L, 0x1de853dce60020ebL,
        0x28614150ebaf5811L, 0x3e10e2f161d51799L, 0xf4611ccb5c2897f9L,
        0xa8ac0b6fc644a206L, 0x725437f56ffd43edL, 0xf2f71aaa68db4773L,
        0xa57c7552cdc15730L, 0xded3ca178d4ff733L, 0x8c860d0968136a01L,
        0xa221a7965a6a57c2L, 0xb1e37377e5202cccL, 0x3417060e997c67a6L
    };

    /** Expected sequence after jump_ce(0x1234567, 77), that is, a jump of 0x1234567 * 2^77 steps. */
    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP_CE = {
        0x2f2cf45ac7441492L, 0x6a2464519651c473L, 0xa5d186d4ed685efcL,
        0x0f97dc650818ffc2L, 0xdf10f41f7355a596L, 0x8e689e6858d2b281L,
        0xb2d3b3d46a3b854cL, 0xe4179c13886cc5caL, 0xdbeac0959afa140bL,
        0xcc53dcc55f272966L, 0x47938fbad8dabd6dL, 0xa144816524b952b3L,
        0x571dd9d44eeaee6dL, 0x2bde674b120f1cfeL, 0xadd0543f4cb81f78L,
        0x4b388ebd3eb17144L, 0xd333e3f747bb135dL, 0x4e388e3eb19f69a3L,
        0xa01a8d71362fea5eL, 0x3d2b1e1c267d7235L, 0x838128a9232adb03L,
        0xc1bce4ab6b8d7df0L, 0xe72dd006fcf12e60L, 0x591d281cb39372abL,
        0x45ee274d18151b80L, 0xa86a67c30cd6014cL, 0x736f579a8aab7341L,
        0x14235324ec3a6080L, 0xd0f3565e0aec6272L, 0xbebfae3347c564ecL
    };

    /** Expected sequence after jump_ce(1, 190), that is, a jump of 2^190 steps. */
    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP_190 = {
        0x571563380103f314L, 0x4f7a99c27de997b7L, 0x0c4fbb6093d2d1b0L,
        0x14de74cbedfcb015L, 0x4d9cd5c38ac0f0c7L, 0x2f446f05f0ba828fL,
        0xbcd20d2bf82b9aaeL, 0x72a839c6398a153dL, 0x48e515e839ee5a35L,
        0x76e1c3e1c0e61f76L, 0x5d832142f73387faL, 0x90ae7d884338e905L,
        0x6eeb592d8409b9ffL, 0xbf5793f7592b1eebL, 0x6cde043d92601a39L,
        0xb32bd3c6115e78a4L, 0xd2a295bb0c3a5d1eL, 0x1fb8f502b5d0eb35L,
        0x028fb49ba274470fL, 0x83b24902b576d182L, 0x4ac3685dad8dcbc5L,
        0x97b7422546b99601L, 0x02237b79e08ef510L, 0x01c7b75492a4f308L,
        0x05d3d9e122e5e296L, 0xac040dc6b84a492aL, 0xd2617fbc7ab126b7L,
        0xc03f1d60bc4ca54bL, 0xa001babb12892131L, 0xbbc3d6c978051955L
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, new MWC192X(SEED));
    }

    @Test
    void testConstructorWithZeroSeedIsFunctional() {
        // The invalid zero carry is replaced by one
        RandomAssert.assertNextLongNonZeroOutput(new MWC192X(new long[SEED_SIZE]), 2 * SEED_SIZE, 2 * SEED_SIZE);
    }

    @Test
    void testInvalidCarry() {
        final long a = 0xff4e24dd418f5433L;
        for (final long carry : new long[] {0, a - 1, a, -1}) {
            RandomAssert.assertNextLongEquals(10, new MWC192X(SEED[0], SEED[1], 1),
                                              new MWC192X(SEED[0], SEED[1], carry));
        }
        // Largest valid carry is kept
        Assertions.assertNotEquals(new MWC192X(SEED[0], SEED[1], 1).nextLong(),
                                   new MWC192X(SEED[0], SEED[1], a - 2).nextLong());
    }

    @Test
    void testConstructorWithoutFullLengthSeed() {
        // Hit the case when the input seed is self-seeded when not full length
        RandomAssert.assertNextLongNonZeroOutput(new MWC192X(new long[] {SEED[0]}),
                SEED_SIZE, SEED_SIZE);
    }

    @Test
    void testElementConstructor() {
        final MWC192X rng1 = new MWC192X(SEED);
        final MWC192X rng2 = new MWC192X(SEED[0], SEED[1], SEED[2]);
        RandomAssert.assertNextLongEquals(SEED.length * 2, rng1, rng2);
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_JUMP, new MWC192X(SEED));
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new MWC192X(SEED));
    }

    /**
     * Applies the arbitrary jump to a generator seeded with SEED and checks that the returned
     * copy has the original sequence, and that the generator has the expected sequence.
     */
    private static void assertArbitraryJump(long[] expectedAfter, Function<MWC192X, UniformRandomProvider> jump) {
        final MWC192X rng = new MWC192X(SEED);
        final UniformRandomProvider copy = jump.apply(rng);
        RandomAssert.assertEquals(EXPECTED_SEQUENCE, copy);
        RandomAssert.assertEquals(expectedAfter, rng);
    }

    @Test
    void testArbitraryJumpMatchesJump() {
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_JUMP, rng -> rng.jump(0x1.0p96));
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_JUMP, rng -> rng.jumpPowerOfTwo(96));
    }

    @Test
    void testArbitraryJumpMatchesLongJump() {
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_LONG_JUMP, rng -> rng.jump(0x1.0p144));
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_LONG_JUMP, rng -> rng.jumpPowerOfTwo(144));
    }

    @Test
    void testArbitraryJumpReferenceCode() {
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_JUMP_CE, rng -> rng.jump(0x1234567 * 0x1.0p77));
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_JUMP_190, rng -> rng.jump(0x1.0p190));
        assertArbitraryJump(EXPECTED_SEQUENCE_AFTER_JUMP_190, rng -> rng.jumpPowerOfTwo(190));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 17, 1000})
    void testSmallArbitraryJump(int distance) {
        final MWC192X rng1 = new MWC192X(SEED);
        final MWC192X rng2 = new MWC192X(SEED);
        for (int i = 0; i < distance; i++) {
            rng1.nextLong();
        }
        // The fractional part is discarded
        rng2.jump(distance + 0.5);
        RandomAssert.assertNextLongEquals(10, rng1, rng2);
    }

    @Test
    void testSmallArbitraryJumpPowerOfTwo() {
        for (int logDistance = -2; logDistance < 10; logDistance++) {
            final MWC192X rng1 = new MWC192X(SEED);
            final MWC192X rng2 = new MWC192X(SEED);
            for (int i = logDistance < 0 ? 0 : 1 << logDistance; i > 0; i--) {
                rng1.nextLong();
            }
            rng2.jumpPowerOfTwo(logDistance);
            RandomAssert.assertNextLongEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0x1.0p-1074, 0x1.0p191, Double.POSITIVE_INFINITY, Double.NaN})
    void testArbitraryJumpThrows(double distance) {
        final MWC192X rng = new MWC192X(SEED);
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jump(distance));
    }

    @ParameterizedTest
    @ValueSource(ints = {191, 192, Integer.MAX_VALUE})
    void testArbitraryJumpPowerOfTwoThrows(int logDistance) {
        final MWC192X rng = new MWC192X(SEED);
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumpPowerOfTwo(logDistance));
    }
}
