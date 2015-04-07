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

import ch.dissem.bitmessage.utils.Encode;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by chris on 24.03.15.
 */
public class V2Pubkey implements Pubkey {
    protected long stream;
    protected long behaviorBitfield;
    protected byte[] publicSigningKey;
    protected byte[] publicEncryptionKey;

    protected V2Pubkey() {
    }

    private V2Pubkey(Builder builder) {
        stream = builder.streamNumber;
        behaviorBitfield = builder.behaviorBitfield;
        publicSigningKey = builder.publicSigningKey;
        publicEncryptionKey = builder.publicEncryptionKey;
    }

    @Override
    public long getVersion() {
        return 2;
    }

    @Override
    public long getStream() {
        return stream;
    }

    @Override
    public byte[] getSigningKey() {
        return publicSigningKey;
    }

    @Override
    public byte[] getEncryptionKey() {
        return publicEncryptionKey;
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        Encode.int32(behaviorBitfield, stream);
        stream.write(publicSigningKey);
        stream.write(publicEncryptionKey);
    }

    public static class Builder {
        private long streamNumber;
        private long behaviorBitfield;
        private byte[] publicSigningKey;
        private byte[] publicEncryptionKey;

        public Builder() {
        }

        public Builder streamNumber(long streamNumber) {
            this.streamNumber = streamNumber;
            return this;
        }

        public Builder behaviorBitfield(long behaviorBitfield) {
            this.behaviorBitfield = behaviorBitfield;
            return this;
        }

        public Builder publicSigningKey(byte[] publicSigningKey) {
            this.publicSigningKey = publicSigningKey;
            return this;
        }

        public Builder publicEncryptionKey(byte[] publicEncryptionKey) {
            this.publicEncryptionKey = publicEncryptionKey;
            return this;
        }

        public V2Pubkey build() {
            return new V2Pubkey(this);
        }
    }
}
