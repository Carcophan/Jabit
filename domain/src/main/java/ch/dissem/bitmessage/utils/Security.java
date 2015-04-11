/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.utils;

import ch.dissem.bitmessage.entity.ObjectMessage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import static ch.dissem.bitmessage.utils.Bytes.inc;

/**
 * Provides some methods to help with hashing and encryption.
 */
public class Security {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BigInteger TWO = BigInteger.valueOf(2);

    static {
        java.security.Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] sha512(byte[]... data) {
        return hash("SHA-512", data);
    }

    public static byte[] doubleSha512(byte[]... data) {
        MessageDigest mda = md("SHA-512");
        for (byte[] d : data) {
            mda.update(d);
        }
        return mda.digest(mda.digest());
    }

    public static byte[] ripemd160(byte[]... data) {
        return hash("RIPEMD160", data);
    }

    public static byte[] sha1(byte[]... data) {
        return hash("SHA-1", data);
    }

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        RANDOM.nextBytes(result);
        return result;
    }

    public static void doProofOfWork(ObjectMessage object, long nonceTrialsPerByte, long extraBytes) throws IOException {
        // payload = embeddedTime + encodedObjectVersion + encodedStreamNumber + encrypted
        byte[] payload = object.getPayloadBytes();
        // payloadLength = the length of payload, in bytes, + 8 (to account for the nonce which we will append later)
        // TTL = the number of seconds in between now and the object expiresTime.
        //        initialHash = hash(payload)
        byte[] initialHash = getInitialHash(object);

        byte[] target = getProofOfWorkTarget(object, nonceTrialsPerByte, extraBytes);
        // start with trialValue = 99999999999999999999
        byte[] trialValue;
        // also start with nonce = 0 where nonce is 8 bytes in length and can be hashed as if it is a string.
        byte[] nonce = new byte[8];
        MessageDigest mda = md("SHA-512");
        do {
            inc(nonce);
            mda.update(nonce);
            mda.update(initialHash);
            trialValue = bytes(mda.digest(mda.digest()), 8);
        } while (Bytes.lt(target, trialValue));
        object.setNonce(nonce);
    }

    /**
     * @param object
     * @param nonceTrialsPerByte
     * @param extraBytes
     * @throws IOException if proof of work doesn't check out
     */
    public static void checkProofOfWork(ObjectMessage object, long nonceTrialsPerByte, long extraBytes) throws IOException {
        // nonce = the first 8 bytes of payload
        byte[] nonce = object.getNonce();
        byte[] initialHash = getInitialHash(object);
        // resultHash = hash(hash( nonce || initialHash ))
        byte[] resultHash = Security.doubleSha512(nonce, initialHash);
        // POWValue = the first eight bytes of resultHash converted to an integer
        byte[] powValue = bytes(resultHash, 8);

        if (Bytes.lt(getProofOfWorkTarget(object, nonceTrialsPerByte, extraBytes), powValue)) {
            throw new IOException("Insufficient proof of work");
        }
    }

    private static byte[] getInitialHash(ObjectMessage object) throws IOException {
        return Security.sha512(object.getPayloadBytes());
    }

    private static byte[] getProofOfWorkTarget(ObjectMessage object, long nonceTrialsPerByte, long extraBytes) throws IOException {
        BigInteger TTL = BigInteger.valueOf(object.getExpiresTime() - (System.currentTimeMillis() / 1000));
        BigInteger numerator = TWO.pow(64);
        BigInteger powLength = BigInteger.valueOf(object.getPayloadBytes().length + extraBytes);
        BigInteger denominator = BigInteger.valueOf(nonceTrialsPerByte).multiply(powLength.add(powLength.multiply(TTL).divide(BigInteger.valueOf(2).pow(16))));
        return numerator.divide(denominator).toByteArray();
    }

    private static byte[] hash(String algorithm, byte[]... data) {
        MessageDigest mda = md(algorithm);
        for (byte[] d : data) {
            mda.update(d);
        }
        return mda.digest();
    }

    private static MessageDigest md(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm, "BC");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] bytes(byte[] data, int count) {
        byte[] result = new byte[count];
        System.arraycopy(data, 0, result, 0, count);
        return result;
    }
}
