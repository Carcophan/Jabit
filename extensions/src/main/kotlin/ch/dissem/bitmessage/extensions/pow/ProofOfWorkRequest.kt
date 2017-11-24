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

package ch.dissem.bitmessage.extensions.pow

import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.Streamable
import ch.dissem.bitmessage.entity.StreamableWriter
import ch.dissem.bitmessage.extensions.CryptoCustomMessage
import ch.dissem.bitmessage.utils.Decode.bytes
import ch.dissem.bitmessage.utils.Decode.varBytes
import ch.dissem.bitmessage.utils.Decode.varString
import ch.dissem.bitmessage.utils.Encode
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * @author Christian Basler
 */
data class ProofOfWorkRequest @JvmOverloads constructor(val sender: BitmessageAddress, val initialHash: ByteArray, val request: ProofOfWorkRequest.Request, val data: ByteArray = ByteArray(0)) : Streamable {

    override fun writer(): StreamableWriter = Writer(this)

    private class Writer(
        private val item: ProofOfWorkRequest
    ) : StreamableWriter {

        override fun write(out: OutputStream) {
            out.write(item.initialHash)
            Encode.varString(item.request.name, out)
            Encode.varBytes(item.data, out)
        }

        override fun write(buffer: ByteBuffer) {
            buffer.put(item.initialHash)
            Encode.varString(item.request.name, buffer)
            Encode.varBytes(item.data, buffer)
        }

    }

    class Reader(private val identity: BitmessageAddress) : CryptoCustomMessage.Reader<ProofOfWorkRequest> {

        override fun read(sender: BitmessageAddress, input: InputStream): ProofOfWorkRequest {
            return ProofOfWorkRequest.read(identity, input)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProofOfWorkRequest) return false

        if (sender != other.sender) return false
        if (!Arrays.equals(initialHash, other.initialHash)) return false
        if (request != other.request) return false
        return Arrays.equals(data, other.data)
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + Arrays.hashCode(initialHash)
        result = 31 * result + request.hashCode()
        result = 31 * result + Arrays.hashCode(data)
        return result
    }

    enum class Request {
        CALCULATE,
        CALCULATING,
        COMPLETE
    }

    companion object {
        @JvmStatic
        fun read(client: BitmessageAddress, input: InputStream): ProofOfWorkRequest {
            return ProofOfWorkRequest(
                client,
                bytes(input, 64),
                Request.valueOf(varString(input)),
                varBytes(input)
            )
        }
    }
}
