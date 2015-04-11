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

package ch.dissem.bitmessage.factory;

import ch.dissem.bitmessage.entity.*;
import ch.dissem.bitmessage.entity.payload.ObjectPayload;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.entity.valueobject.NetworkAddress;
import ch.dissem.bitmessage.utils.Decode;
import ch.dissem.bitmessage.utils.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates protocol v3 network messages from {@link InputStream InputStreams}
 */
class V3MessageFactory {
    private Logger LOG = LoggerFactory.getLogger(V3MessageFactory.class);

    public NetworkMessage read(InputStream stream) throws IOException {
        if (testMagic(stream)) {
            String command = getCommand(stream);
            int length = (int) Decode.uint32(stream);
            byte[] checksum = Decode.bytes(stream, 4);

            byte[] payloadBytes = Decode.bytes(stream, length);

            if (testChecksum(checksum, payloadBytes)) {
                MessagePayload payload = getPayload(command, new ByteArrayInputStream(payloadBytes), length);
                return new NetworkMessage(payload);
            } else {
                throw new IOException("Checksum failed for message '" + command + "'");
            }
        } else {
            LOG.debug("Failed test for MAGIC bytes");
            return null;
        }
    }

    private MessagePayload getPayload(String command, InputStream stream, int length) throws IOException {
        switch (command) {
            case "version":
                return parseVersion(stream);
            case "verack":
                return new VerAck();
            case "addr":
                return parseAddr(stream);
            case "inv":
                return parseInv(stream);
            case "getdata":
                return parseGetData(stream);
            case "object":
                return parseObject(stream, length);
            default:
                LOG.debug("Unknown command: " + command);
                return null;
        }
    }

    private ObjectMessage parseObject(InputStream stream, int length) throws IOException {
        byte nonce[] = Decode.bytes(stream, 8);
        long expiresTime = Decode.int64(stream);
        long objectType = Decode.uint32(stream);
        long version = Decode.varInt(stream);
        long streamNumber = Decode.varInt(stream);

        ObjectPayload payload = Factory.getObjectPayload(objectType, version, streamNumber, stream, length);

        return new ObjectMessage.Builder()
                .nonce(nonce)
                .expiresTime(expiresTime)
                .objectType(objectType)
                .version(version)
                .streamNumber(streamNumber)
                .payload(payload)
                .build();
    }

    private GetData parseGetData(InputStream stream) throws IOException {
        long count = Decode.varInt(stream);
        GetData.Builder builder = new GetData.Builder();
        for (int i = 0; i < count; i++) {
            builder.addInventoryVector(parseInventoryVector(stream));
        }
        return builder.build();
    }

    private Inv parseInv(InputStream stream) throws IOException {
        long count = Decode.varInt(stream);
        Inv.Builder builder = new Inv.Builder();
        for (int i = 0; i < count; i++) {
            builder.addInventoryVector(parseInventoryVector(stream));
        }
        return builder.build();
    }

    private Addr parseAddr(InputStream stream) throws IOException {
        long count = Decode.varInt(stream);
        Addr.Builder builder = new Addr.Builder();
        for (int i = 0; i < count; i++) {
            builder.addAddress(parseAddress(stream, false));
        }
        return builder.build();
    }

    private Version parseVersion(InputStream stream) throws IOException {
        int version = Decode.int32(stream);
        long services = Decode.int64(stream);
        long timestamp = Decode.int64(stream);
        NetworkAddress addrRecv = parseAddress(stream, true);
        NetworkAddress addrFrom = parseAddress(stream, true);
        long nonce = Decode.int64(stream);
        String userAgent = Decode.varString(stream);
        long[] streamNumbers = Decode.varIntList(stream);

        return new Version.Builder()
                .version(version)
                .services(services)
                .timestamp(timestamp)
                .addrRecv(addrRecv).addrFrom(addrFrom)
                .nonce(nonce)
                .userAgent(userAgent)
                .streams(streamNumbers).build();
    }

    private InventoryVector parseInventoryVector(InputStream stream) throws IOException {
        return new InventoryVector(Decode.bytes(stream, 32));
    }

    private NetworkAddress parseAddress(InputStream stream, boolean light) throws IOException {
        long time;
        long streamNumber;
        if (!light) {
            time = Decode.int64(stream);
            streamNumber = Decode.uint32(stream); // This isn't consistent, not sure if this is correct
        } else {
            time = 0;
            streamNumber = 0;
        }
        long services = Decode.int64(stream);
        byte[] ipv6 = Decode.bytes(stream, 16);
        int port = Decode.uint16(stream);
        return new NetworkAddress.Builder().time(time).stream(streamNumber).services(services).ipv6(ipv6).port(port).build();
    }

    private boolean testChecksum(byte[] checksum, byte[] payload) {
        byte[] payloadChecksum = Security.sha512(payload);
        for (int i = 0; i < checksum.length; i++) {
            if (checksum[i] != payloadChecksum[i]) {
                return false;
            }
        }
        return true;
    }

    private String getCommand(InputStream stream) throws IOException {
        byte[] bytes = new byte[12];
        int end = -1;
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) stream.read();
            if (end == -1) {
                if (bytes[i] == 0) end = i;
            } else {
                if (bytes[i] != 0) throw new IOException("'\\0' padding expected for command");
            }
        }
        return new String(bytes, 0, end, "ASCII");
    }

    private boolean testMagic(InputStream stream) throws IOException {
        for (byte b : NetworkMessage.MAGIC_BYTES) {
            if (b != (byte) stream.read()) return false;
        }
        return true;
    }
}
