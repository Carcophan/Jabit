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

package ch.dissem.bitmessage.networking;

import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.cryptography.bc.BouncyCryptography;
import ch.dissem.bitmessage.entity.CustomMessage;
import ch.dissem.bitmessage.entity.MessagePayload;
import ch.dissem.bitmessage.entity.valueobject.NetworkAddress;
import ch.dissem.bitmessage.exception.NodeException;
import ch.dissem.bitmessage.ports.*;
import ch.dissem.bitmessage.utils.Property;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.util.concurrent.Future;

import static ch.dissem.bitmessage.utils.Singleton.cryptography;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * FIXME: there really should be sensible tests for the network handler
 */
public class NetworkHandlerTest {
    private static NetworkAddress localhost = new NetworkAddress.Builder().ipv4(127, 0, 0, 1).port(6001).build();

    private static TestInventory peerInventory;
    private static TestInventory nodeInventory;

    private static BitmessageContext peer;
    private static BitmessageContext node;
    private static NetworkHandler networkHandler;

    @BeforeClass
    public static void setUp() {
        peerInventory = new TestInventory();
        peer = new BitmessageContext.Builder()
                .addressRepo(mock(AddressRepository.class))
                .inventory(peerInventory)
                .messageRepo(mock(MessageRepository.class))
                .powRepo(mock(ProofOfWorkRepository.class))
                .port(6001)
                .nodeRegistry(new TestNodeRegistry())
                .networkHandler(new DefaultNetworkHandler())
                .cryptography(new BouncyCryptography())
                .listener(mock(BitmessageContext.Listener.class))
                .build();
        peer.startup();

        nodeInventory = new TestInventory();
        networkHandler = new DefaultNetworkHandler();
        node = new BitmessageContext.Builder()
                .addressRepo(mock(AddressRepository.class))
                .inventory(nodeInventory)
                .messageRepo(mock(MessageRepository.class))
                .powRepo(mock(ProofOfWorkRepository.class))
                .port(6002)
                .nodeRegistry(new TestNodeRegistry(localhost))
                .networkHandler(networkHandler)
                .cryptography(new BouncyCryptography())
                .listener(mock(BitmessageContext.Listener.class))
                .customCommandHandler(new CustomCommandHandler() {
                    @Override
                    public MessagePayload handle(CustomMessage request) {
                        byte[] data = request.getData();
                        if (data.length > 0) {
                            switch (data[0]) {
                                case 0:
                                    return null;
                                case 1:
                                    break;
                                case 3:
                                    data[0] = 0;
                            }
                        }
                        return new CustomMessage("test response", request.getData());
                    }
                })
                .build();
    }

    @AfterClass
    public static void cleanUp() {
        shutdown(peer);
    }

    private static void shutdown(BitmessageContext node) {
        node.shutdown();
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        } while (node.isRunning());
    }

    @Test(timeout = 5_000)
    public void ensureNodesAreConnecting() {
        try {
            node.startup();
            Property status;
            do {
                Thread.yield();
                status = node.status().getProperty("network", "connections", "stream 0");
            } while (status == null);
            assertEquals(1, status.getProperty("outgoing").getValue());
        } finally {
            shutdown(node);
        }
    }

    @Test(timeout = 5_000)
    public void ensureCustomMessageIsSentAndResponseRetrieved() throws Exception {
        byte[] data = cryptography().randomBytes(8);
        data[0] = (byte) 1;
        CustomMessage request = new CustomMessage("test request", data);
        node.startup();

        CustomMessage response = networkHandler.send(InetAddress.getLocalHost(), 6002, request);

        assertThat(response, notNullValue());
        assertThat(response.getCustomCommand(), is("test response"));
        assertThat(response.getData(), is(data));

        shutdown(node);
    }

    @Test(timeout = 5_000, expected = NodeException.class)
    public void ensureCustomMessageWithoutResponsYieldsException() throws Exception {
        try {
            byte[] data = cryptography().randomBytes(8);
            data[0] = (byte) 0;
            CustomMessage request = new CustomMessage("test request", data);
            node.startup();

            CustomMessage response = networkHandler.send(InetAddress.getLocalHost(), 6002, request);

            assertThat(response, notNullValue());
            assertThat(response.getCustomCommand(), is("test response"));
            assertThat(response.getData(), is(request.getData()));
        } finally {
            shutdown(node);
        }
    }

    @Test(timeout = 5_000)
    public void ensureObjectsAreSynchronizedIfBothHaveObjects() throws Exception {
        peerInventory.init(
                "V4Pubkey.payload",
                "V5Broadcast.payload"
        );

        nodeInventory.init(
                "V1Msg.payload",
                "V4Pubkey.payload"
        );

        Future<?> future = networkHandler.synchronize(InetAddress.getLocalHost(), 6001,
                mock(NetworkHandler.MessageListener.class),
                10);
        future.get();
        assertInventorySize(3, nodeInventory);
        assertInventorySize(3, peerInventory);
    }

    @Test(timeout = 5_000)
    public void ensureObjectsAreSynchronizedIfOnlyPeerHasObjects() throws Exception {
        peerInventory.init(
                "V4Pubkey.payload",
                "V5Broadcast.payload"
        );

        nodeInventory.init();

        Future<?> future = networkHandler.synchronize(InetAddress.getLocalHost(), 6001,
                mock(NetworkHandler.MessageListener.class),
                10);
        future.get();
        assertInventorySize(2, nodeInventory);
        assertInventorySize(2, peerInventory);
    }

    @Test(timeout = 5_000)
    public void ensureObjectsAreSynchronizedIfOnlyNodeHasObjects() throws Exception {
        peerInventory.init();

        nodeInventory.init(
                "V1Msg.payload"
        );

        Future<?> future = networkHandler.synchronize(InetAddress.getLocalHost(), 6001,
                mock(NetworkHandler.MessageListener.class),
                10);
        future.get();
        assertInventorySize(1, nodeInventory);
        assertInventorySize(1, peerInventory);
    }

    private void assertInventorySize(int expected, TestInventory inventory) throws InterruptedException {
        long timeout = System.currentTimeMillis() + 1000;
        while (expected != inventory.getInventory().size() && System.currentTimeMillis() < timeout) {
            Thread.sleep(10);
        }
        assertEquals(expected, inventory.getInventory().size());
    }

}
