/*
 * This file is part of ShieldBlaze ExpressGateway. [www.shieldblaze.com]
 * Copyright (c) 2020-2022 ShieldBlaze
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
package com.shieldblaze.expressgateway.core.loadbalancer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shieldblaze.expressgateway.backend.cluster.Cluster;
import com.shieldblaze.expressgateway.common.annotation.NonNull;
import com.shieldblaze.expressgateway.concurrent.eventstream.EventStream;
import com.shieldblaze.expressgateway.configuration.ConfigurationContext;
import com.shieldblaze.expressgateway.core.L4FrontListener;
import com.shieldblaze.expressgateway.core.events.L4FrontListenerShutdownEvent;
import com.shieldblaze.expressgateway.core.events.L4FrontListenerStartupEvent;
import com.shieldblaze.expressgateway.core.events.L4FrontListenerStopEvent;
import com.shieldblaze.expressgateway.core.factory.EventLoopFactory;
import com.shieldblaze.expressgateway.core.factory.PooledByteBufAllocatorFactory;
import com.shieldblaze.expressgateway.core.handlers.ConnectionTracker;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link L4LoadBalancer} holds base functions for a L4-Load Balancer.
 */
public abstract class L4LoadBalancer {

    private static final Logger logger = LogManager.getLogger(L4LoadBalancer.class);

    public static final String DEFAULT = "DEFAULT";
    private final String ID = UUID.randomUUID().toString();

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private String name = "L4LoadBalancer#" + COUNTER.incrementAndGet();

    private final EventStream eventStream;
    private final InetSocketAddress bindAddress;
    private final L4FrontListener l4FrontListener;
    private final Map<String, Cluster> clusterMap = new ConcurrentHashMap<>();
    private final ConfigurationContext configurationContext;
    private final ChannelHandler channelHandler;

    private final ByteBufAllocator byteBufAllocator;
    private final EventLoopFactory eventLoopFactory;

    private L4FrontListenerStartupEvent l4FrontListenerStartupEvent;
    private final ConnectionTracker connectionTracker = new ConnectionTracker();

    /**
     * @param name                 Name of this Load Balancer
     * @param bindAddress          {@link InetSocketAddress} on which {@link L4FrontListener} will bind and listen.
     * @param l4FrontListener      {@link L4FrontListener} for listening traffic
     * @param configurationContext {@link ConfigurationContext} to be applied
     * @param channelHandler       {@link ChannelHandler} to use for handling traffic
     * @throws NullPointerException If a required parameter if {@code null}
     */
    public L4LoadBalancer(String name,
                          @NonNull InetSocketAddress bindAddress,
                          @NonNull L4FrontListener l4FrontListener,
                          @NonNull ConfigurationContext configurationContext,
                          ChannelHandler channelHandler) {

        if (name != null && !name.isEmpty()) {
            this.name = name;
        }

        this.bindAddress = bindAddress;
        this.l4FrontListener = l4FrontListener;
        this.configurationContext = configurationContext;
        this.eventStream = configurationContext.eventStreamConfiguration().newEventStream();
        this.channelHandler = channelHandler;

        this.byteBufAllocator = new PooledByteBufAllocatorFactory(configurationContext.bufferConfiguration()).instance();
        this.eventLoopFactory = new EventLoopFactory(configurationContext);

        l4FrontListener.l4LoadBalancer(this);
    }

    /**
     * Load Balancer UUID
     *
     * @return Returns the Load Balancer UUID
     */
    public String id() {
        return ID;
    }

    /**
     * Name of this L4 Load Balancer
     */
    public String name() {
        return name;
    }

    /**
     * Start L4 Load Balancer
     */
    public L4FrontListenerStartupEvent start() {
        try {
            logger.info("Trying to start L4FrontListener");

            // Start the listener
            l4FrontListenerStartupEvent = l4FrontListener.start();
            return l4FrontListenerStartupEvent;
        } catch (Exception ex) {
            logger.fatal("Failed to start L4FrontListener", ex);
            throw ex;
        } finally {
            logger.info("Started L4FrontListener: {}", l4FrontListenerStartupEvent);
        }
    }

    /**
     * Stop L4 Load Balancer, and it's child operations and services.
     *
     * @return {@link L4FrontListenerStopEvent} instance
     */
    public L4FrontListenerStopEvent stop() {
        L4FrontListenerStopEvent event = null;
        try {
            logger.info("Trying to stop L4FrontListener");

            // Start the listener
            event = l4FrontListener.stop();
            return event;
        } catch (Exception ex) {
            logger.fatal("Failed to stop L4FrontListener", ex);
            throw ex;
        } finally {
            logger.info("Stopped L4FrontListener: {}", event);
        }
    }

    /**
     * Shutdown L4 Load Balancer
     *
     * @return {@link L4FrontListenerShutdownEvent} instance
     */
    public L4FrontListenerShutdownEvent shutdown() {
        L4FrontListenerShutdownEvent event = null;
        try {
            logger.info("Trying to shutdown L4FrontListener");

            // Start the listener
            event = l4FrontListener.shutdown();
            return event;
        } catch (Exception ex) {
            logger.fatal("Failed to shutdown L4FrontListener", ex);
            throw ex;
        } finally {
            logger.info("Shutdown L4FrontListener: {}", event);
        }
    }

    /**
     * Get the {@link EventStream} of this Load Balancer
     */
    public EventStream eventStream() {
        return eventStream;
    }

    /**
     * Get {@link InetSocketAddress} on which {@link L4FrontListener} is bind.
     */
    public InetSocketAddress bindAddress() {
        return bindAddress;
    }

    /**
     * Get {@link Cluster} which is being Load Balanced for specific Hostname
     *
     * @param hostname FQDN Hostname
     * @throws NullPointerException If {@link Cluster} is not found
     */
    @NonNull
    public Cluster cluster(String hostname) {
        logger.info("Looking up for Cluster with hostname: {}", hostname);
        try {
            Cluster cluster = clusterMap.get(hostname);
            if (cluster == null) {
                cluster = clusterMap.get("DEFAULT");
                if (cluster == null) {
                    throw new NullPointerException("Cluster not found with Hostname: " + hostname);
                }
            }
            return cluster;
        } catch (Exception ex) {
            logger.error("Failed to lookup for Cluster", ex);
            throw ex;
        }
    }

    /**
     * Get all {@link Cluster}
     */
    public Map<String, Cluster> clusters() {
        return clusterMap;
    }

    /**
     * Set the default {@link Cluster}
     */
    public void defaultCluster(Cluster cluster) {
        mapCluster("DEFAULT", cluster);
    }

    /**
     * Get the default {@link Cluster}
     */
    public Cluster defaultCluster() {
        return cluster("DEFAULT");
    }

    /**
     * Add new mapping of Cluster with Hostname
     *
     * @param hostname Fully qualified Hostname and Port if non-default port is used
     * @param cluster  {@link Cluster} to be mapped
     */
    public void mapCluster(String hostname, Cluster cluster) {
        Objects.requireNonNull(hostname, "Hostname");
        Objects.requireNonNull(cluster, "Cluster");

        try {
            logger.info("Mapping Cluster: {} with Hostname: {} and EventStream: {}", cluster, hostname, eventStream);

            cluster.useEventStream(eventStream);
            clusterMap.put(hostname, cluster);

            logger.info("Successfully mapped Cluster");
        } catch (Exception ex) {
            logger.error("Failed to map cluster", ex);
            throw ex;
        }
    }

    /**
     * Remap a {@link Cluster} from old hostname to new hostname.
     *
     * @param oldHostname Old Hostname
     * @param newHostname New Hostname
     */
    public void remapCluster(String oldHostname, String newHostname) {
        Objects.requireNonNull(oldHostname, "OldHostname");
        Objects.requireNonNull(newHostname, "NewHostname");

        try {
            logger.info("Remapping Cluster from Hostname: {} to Hostname: {}", oldHostname, newHostname);

            Cluster cluster = clusterMap.remove(oldHostname);
            if (cluster == null) {
                throw new NullPointerException("Cluster not found with Hostname: " + oldHostname);
            }

            clusterMap.put(newHostname, cluster);
            logger.info("Successfully remapped Cluster: {}, from Hostname: {} to Hostname: {}", cluster, oldHostname, newHostname);
        } catch (Exception ex) {
            logger.error("Failed to Remap Cluster", ex);
            throw ex;
        }
    }

    /**
     * Remove a Cluster from mapping
     *
     * @param hostname Hostname of the Cluster
     * @return Returns {@link Boolean#TRUE} if removal was successful else {@link Boolean#FALSE}
     */
    public boolean removeCluster(String hostname) {
        boolean removed = false;
        try {
            Cluster cluster = clusterMap.remove(hostname);
            if (cluster == null) {
                return false;
            }

            cluster.close();
            removed = true;
        } catch (Exception ex) {
            logger.error("Failed to remove Hostname: {} from Cluster", hostname);
            throw ex;
        } finally {
            if (removed) {
                logger.info("Successfully removed Cluster from Hostname mapping: {}", hostname);
            } else {
                logger.info("Failed to remove Cluster from Hostname mapping: {}", hostname);
            }
        }
        return true;
    }

    /**
     * Get {@link ConfigurationContext} which is applied
     */
    public ConfigurationContext configurationContext() {
        return configurationContext;
    }

    /**
     * Get {@link ChannelHandler} used for handling traffic
     */
    public ChannelHandler channelHandler() {
        return channelHandler;
    }

    /**
     * Get {@link ByteBufAllocator} created from {@link PooledByteBufAllocatorFactory}
     */
    public ByteBufAllocator byteBufAllocator() {
        return byteBufAllocator;
    }

    /**
     * Get {@link EventLoopFactory} being used
     */
    public EventLoopFactory eventLoopFactory() {
        return eventLoopFactory;
    }

    /**
     * Get {@link ConnectionTracker} Handler
     */
    public ConnectionTracker connectionTracker() {
        return connectionTracker;
    }

    /**
     * Return the Type of Load Balancer
     */
    public abstract String type();

    @Override
    public String toString() {
        return "L4LoadBalancer{" + toJson() + '}';
    }

    /**
     * Convert Load Balancer data into {@link JsonObject}
     *
     * @return {@link JsonObject} Instance
     */
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        String state;
        if (l4FrontListenerStartupEvent.isFinished()) {
            if (l4FrontListenerStartupEvent.isSuccess()) {
                state = "Running";
            } else {
                state = "Failed; " + l4FrontListenerStartupEvent.cause().getMessage();
            }
        } else {
            state = "Pending";
        }

        jsonObject.addProperty("ID", ID);
        jsonObject.addProperty("Name", name);
        jsonObject.addProperty("Type", type());
        jsonObject.addProperty("State", state);

        JsonArray clusters = new JsonArray();
        clusterMap.forEach((hostname, cluster) -> clusters.add(cluster.toJson()));
        jsonObject.add("Clusters", clusters);

        return jsonObject;
    }
}
