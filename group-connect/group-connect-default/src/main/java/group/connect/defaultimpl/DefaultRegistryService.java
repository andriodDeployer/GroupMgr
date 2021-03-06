/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package group.connect.defaultimpl;


import group.common.util.Maps;
import group.common.util.SpiMetadata;
import group.common.util.Strings;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.connect.AbstractRegistryService;
import group.connect.RegisterMeta;
import group.transport.JConnection;
import group.transport.UnresolvedAddress;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import static group.common.util.Preconditions.checkArgument;
import static group.common.util.Preconditions.checkNotNull;

/**
 * Default registry service.
 *
 * jupiter
 * org.jupiter.registry.jupiter
 *
 * @author jiachun.fjc
 */

@SpiMetadata(name = "default")
public class DefaultRegistryService extends AbstractRegistryService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRegistryService.class);

    //注册中心地址和注册中心客户端,在consumer中一个注册中心对应一个DefultRegistry，在每个defaultRegistry中有且仅有一个channel实例
    private final ConcurrentMap<UnresolvedAddress, DefaultRegistry> clients = Maps.newConcurrentMap();

    @Override
    protected void doSubscribe(RegisterMeta.ServiceMeta serviceMeta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Subscribe: {}.", serviceMeta);
        //想每个注册中心都进行订阅。为了防止有些服务提供者执行某个注册中心进行了注册，也就是说，同一服务的注册信息不是在每个注册中心都有。
        for (DefaultRegistry c : allClients) {
            c.doSubscribe(serviceMeta);
        }
    }

    @Override
    protected void doRegister(RegisterMeta meta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Register: {}.", meta);

        for (DefaultRegistry c : allClients) {
            c.doRegister(meta);
        }
        getRegisterMetaMap().put(meta, RegisterState.DONE);//本地存放一份,放的有些早了，因为有可能并没有发送成功，但是本地却增加了一份。
    }

    @SuppressWarnings("all")
    @Override
    protected void doUnregister(RegisterMeta meta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Unregister: {}.", meta);

        for (DefaultRegistry c : allClients) {
            c.doUnregister(meta);
        }
    }

    @Override
    protected void doCheckRegisterNodeStatus() {
        // the default registry service does nothing
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        checkNotNull(connectString, "connectString");

        String[] array = Strings.split(connectString, ',');
        for (String s : array) {
            String[] addressStr = Strings.split(s, ':');
            String host = addressStr[0];
            int port = Integer.parseInt(addressStr[1]);
            UnresolvedAddress address = new UnresolvedAddress(host, port);
            DefaultRegistry client = clients.get(address);
            if (client == null) {
                DefaultRegistry newClient = new DefaultRegistry(this);
                client = clients.putIfAbsent(address, newClient);
                if (client == null) {
                    client = newClient;
                    JConnection connection = client.connect(address);
                    client.connectionManager().manage(connection);
                } else {
                    newClient.shutdownGracefully();
                }
            }
        }
    }

    @Override
    public void destroy() {
        for (DefaultRegistry c : clients.values()) {
            c.shutdownGracefully();
        }
    }
}