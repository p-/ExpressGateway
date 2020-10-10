/*
 * This file is part of ShieldBlaze ExpressGateway. [www.shieldblaze.com]
 * Copyright (c) 2020 ShieldBlaze
 *
 * ShieldBlaze ExpressGateway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ShieldBlaze ExpressGateway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ShieldBlaze ExpressGateway.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.shieldblaze.expressgateway.core.loadbalancer.l7;

import com.shieldblaze.expressgateway.core.concurrent.async.L4FrontListenerEvent;
import com.shieldblaze.expressgateway.core.configuration.CommonConfiguration;
import com.shieldblaze.expressgateway.core.utils.EventLoopFactory;
import com.shieldblaze.expressgateway.core.utils.PooledByteBufAllocator;
import com.shieldblaze.expressgateway.core.server.L4FrontListener;
import com.shieldblaze.expressgateway.core.server.L7FrontListener;
import com.shieldblaze.expressgateway.loadbalance.backend.Cluster;
import com.shieldblaze.expressgateway.loadbalance.l7.L7Balance;
import io.netty.buffer.ByteBufAllocator;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * {@link L7LoadBalancer} holds base functions for a L7-Load Balancer.
 */
public abstract class L7LoadBalancer {

    private final InetSocketAddress bindAddress;
    private final L7Balance l7Balance;
    private final L7FrontListener l7FrontListener;
    private final Cluster cluster;
    private final CommonConfiguration commonConfiguration;

    private final ByteBufAllocator byteBufAllocator;
    private final EventLoopFactory eventLoopFactory;

    /**
     * @param bindAddress         {@link InetSocketAddress} on which {@link L4FrontListener} will bind and listen.
     * @param l7Balance           {@link L7Balance} for Load Balance
     * @param l7FrontListener     {@link L7FrontListener} for listening and handling traffic
     * @param cluster             {@link Cluster} to be Load Balanced
     * @param commonConfiguration {@link CommonConfiguration} to be applied
     * @throws NullPointerException If any parameter is {@code null}
     */
    public L7LoadBalancer(InetSocketAddress bindAddress, L7Balance l7Balance, L7FrontListener l7FrontListener, Cluster cluster, CommonConfiguration commonConfiguration) {
        this.bindAddress = Objects.requireNonNull(bindAddress, "bindAddress");
        this.l7Balance = Objects.requireNonNull(l7Balance, "l7Balance");
        this.l7FrontListener = Objects.requireNonNull(l7FrontListener, "l7FrontListener");
        this.cluster = Objects.requireNonNull(cluster, "cluster");
        this.commonConfiguration = Objects.requireNonNull(commonConfiguration);
        this.byteBufAllocator = new PooledByteBufAllocator(commonConfiguration.getPooledByteBufAllocatorConfiguration()).getInstance();
        this.eventLoopFactory = new EventLoopFactory(commonConfiguration);
    }

    /**
     * Start L7 Load Balancer
     *
     * @return {@link List} containing {@link CompletableFuture} of {@link L4FrontListenerEvent}
     * which will notify when server is completely started.
     */
    public List<CompletableFuture<L4FrontListenerEvent>> start() {
        return l7FrontListener.start();
    }

    /**
     * Stop L4 Load Balancer
     *
     * @return {@link CompletableFuture} of {@link Boolean} which is set to {@code true} when server is successfully closed.
     */
    public CompletableFuture<Boolean> stop() {
        return l7FrontListener.stop();
    }

    /**
     * Get {@link InetSocketAddress} on which {@link L4FrontListener} is bind.
     */
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    /**
     * Get {@link L7Balance} used to Load Balance
     */
    public L7Balance getL7Balance() {
        return l7Balance;
    }

    /**
     * Get {@link L7FrontListener} which is listening and handling traffic
     */
    public L7FrontListener getL4FrontListener() {
        return l7FrontListener;
    }

    /**
     * Get {@link Cluster} which is being Load Balanced
     */
    public Cluster getCluster() {
        return cluster;
    }

    /**
     * Get {@link CommonConfiguration} which is applied
     */
    public CommonConfiguration getCommonConfiguration() {
        return commonConfiguration;
    }

    /**
     * Get {@link ByteBufAllocator} created from {@link PooledByteBufAllocator}
     */
    public ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    /**
     * Get {@link EventLoopFactory} being used
     */
    public EventLoopFactory getEventLoopFactory() {
        return eventLoopFactory;
    }
}