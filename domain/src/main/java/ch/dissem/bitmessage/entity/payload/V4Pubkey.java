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

package ch.dissem.bitmessage.entity.payload;

import ch.dissem.bitmessage.utils.Decode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A version 4 public key. When version 4 pubkeys are created, most of the data in the pubkey is encrypted. This is
 * done in such a way that only someone who has the Bitmessage address which corresponds to a pubkey can decrypt and
 * use that pubkey. This prevents people from gathering pubkeys sent around the network and using the data from them
 * to create messages to be used in spam or in flooding attacks.
 */
public class V4Pubkey implements Pubkey {
    private long stream;
    private byte[] tag;
    private byte[] encrypted;
    private V3Pubkey decrypted;

    private V4Pubkey(long stream, byte[] tag, byte[] encrypted) {
        this.stream = stream;
        this.tag = tag;
        this.encrypted = encrypted;
    }

    public V4Pubkey(V3Pubkey decrypted) {
        this.stream = decrypted.stream;
        // TODO: this.tag = new BitmessageAddress(this).doubleHash
        this.decrypted = decrypted;
        // TODO: this.encrypted
    }

    public static ObjectPayload read(InputStream stream, long streamNumber, int length) throws IOException {
        return new V4Pubkey(streamNumber, Decode.bytes(stream, 32), Decode.bytes(stream, length - 32));
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        stream.write(tag);
        stream.write(encrypted);
    }

    @Override
    public long getVersion() {
        return 4;
    }

    @Override
    public long getStream() {
        return stream;
    }

    public byte[] getTag() {
        return tag;
    }

    @Override
    public byte[] getSigningKey() {
        return decrypted.getSigningKey();
    }

    @Override
    public byte[] getEncryptionKey() {
        return decrypted.getEncryptionKey();
    }
}