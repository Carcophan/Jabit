/*
 * Copyright 2017 Christian Basler
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

package ch.dissem.bitmessage.entity.payload

import ch.dissem.bitmessage.entity.EncryptedStreamableWriter
import ch.dissem.bitmessage.utils.Decode
import ch.dissem.bitmessage.utils.Encode
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * A version 2 public key.
 */
open class V2Pubkey constructor(
    version: Long,
    override val stream: Long,
    override val behaviorBitfield: Int,
    signingKey: ByteArray,
    encryptionKey: ByteArray
) : Pubkey(version) {

    override val signingKey: ByteArray = if (signingKey.size == 64) add0x04(signingKey) else signingKey
    override val encryptionKey: ByteArray = if (encryptionKey.size == 64) add0x04(encryptionKey) else encryptionKey

    override fun writer(): EncryptedStreamableWriter = Writer(this)

    protected open class Writer(
        private val item: V2Pubkey
    ) : EncryptedStreamableWriter {

        override fun write(out: OutputStream) {
            Encode.int32(item.behaviorBitfield, out)
            out.write(item.signingKey, 1, 64)
            out.write(item.encryptionKey, 1, 64)
        }

        override fun write(buffer: ByteBuffer) {
            Encode.int32(item.behaviorBitfield, buffer)
            buffer.put(item.signingKey, 1, 64)
            buffer.put(item.encryptionKey, 1, 64)
        }

        override fun writeBytesToSign(out: OutputStream) {
            // Nothing to do
        }

        override fun writeUnencrypted(out: OutputStream) {
            write(out)
        }

        override fun writeUnencrypted(buffer: ByteBuffer) {
            write(buffer)
        }

    }

    class Builder {
        internal var streamNumber: Long = 0
        internal var behaviorBitfield: Int = 0
        internal var publicSigningKey: ByteArray? = null
        internal var publicEncryptionKey: ByteArray? = null

        fun stream(streamNumber: Long): Builder {
            this.streamNumber = streamNumber
            return this
        }

        fun behaviorBitfield(behaviorBitfield: Int): Builder {
            this.behaviorBitfield = behaviorBitfield
            return this
        }

        fun publicSigningKey(publicSigningKey: ByteArray): Builder {
            this.publicSigningKey = publicSigningKey
            return this
        }

        fun publicEncryptionKey(publicEncryptionKey: ByteArray): Builder {
            this.publicEncryptionKey = publicEncryptionKey
            return this
        }

        fun build(): V2Pubkey {
            return V2Pubkey(
                version = 2,
                stream = streamNumber,
                behaviorBitfield = behaviorBitfield,
                signingKey = add0x04(publicSigningKey!!),
                encryptionKey = add0x04(publicEncryptionKey!!)
            )
        }
    }

    companion object {
        @JvmStatic fun read(input: InputStream, stream: Long): V2Pubkey {
            return V2Pubkey(
                version = 2,
                stream = stream,
                behaviorBitfield = Decode.uint32(input).toInt(),
                signingKey = Decode.bytes(input, 64),
                encryptionKey = Decode.bytes(input, 64)
            )
        }
    }
}
