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
 * Created by chris on 27.03.15.
 */
public class V3Pubkey extends V2Pubkey {
    long nonceTrialsPerByte;
    long extraBytes;
    byte[] signature;

    protected V3Pubkey(Builder builder) {
        stream = builder.streamNumber;
        behaviorBitfield = builder.behaviorBitfield;
        publicSigningKey = builder.publicSigningKey;
        publicEncryptionKey = builder.publicEncryptionKey;

        nonceTrialsPerByte = builder.nonceTrialsPerByte;
        extraBytes = builder.extraBytes;
        signature = builder.signature;
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        super.write(stream);
        Encode.varInt(nonceTrialsPerByte, stream);
        Encode.varInt(extraBytes, stream);
        Encode.varInt(signature.length, stream);
        stream.write(signature);
    }

    @Override
    public long getVersion() {
        return 3;
    }

    public static class Builder extends V2Pubkey.Builder {
        private long streamNumber;
        private long behaviorBitfield;
        private byte[] publicSigningKey;
        private byte[] publicEncryptionKey;

        private long nonceTrialsPerByte;
        private long extraBytes;
        private byte[] signature;

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

        public Builder nonceTrialsPerByte(long nonceTrialsPerByte) {
            this.nonceTrialsPerByte = nonceTrialsPerByte;
            return this;
        }

        public Builder extraBytes(long extraBytes) {
            this.extraBytes = extraBytes;
            return this;
        }

        public Builder signature(byte[] signature) {
            this.signature = signature;
            return this;
        }

        public V3Pubkey build() {
            return new V3Pubkey(this);
        }
    }
}
