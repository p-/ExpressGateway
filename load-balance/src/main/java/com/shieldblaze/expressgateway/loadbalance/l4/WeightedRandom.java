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
package com.shieldblaze.expressgateway.loadbalance.l4;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import com.shieldblaze.expressgateway.backend.Backend;
import com.shieldblaze.expressgateway.backend.State;
import com.shieldblaze.expressgateway.backend.cluster.Cluster;
import com.shieldblaze.expressgateway.backend.events.BackendEvent;
import com.shieldblaze.expressgateway.concurrent.eventstream.EventListener;
import com.shieldblaze.expressgateway.backend.loadbalance.SessionPersistence;
import com.shieldblaze.expressgateway.loadbalance.exceptions.BackendNotOnlineException;
import com.shieldblaze.expressgateway.loadbalance.l4.sessionpersistence.NOOPSessionPersistence;

import java.net.InetSocketAddress;

/**
 * Select {@link Backend} based on Weight Randomly
 */
@SuppressWarnings("UnstableApiUsage")
public final class WeightedRandom extends L4Balance implements EventListener {
    private final java.util.Random RANDOM_INSTANCE = new java.util.Random();

    private final TreeRangeMap<Integer, Backend> backendsMap = TreeRangeMap.create();
    private int totalWeight;

    public WeightedRandom() {
        super(new NOOPSessionPersistence());
    }

    public WeightedRandom(Cluster cluster) {
        this(new NOOPSessionPersistence(), cluster);
    }

    public WeightedRandom(SessionPersistence<Backend, Backend, InetSocketAddress, Backend> sessionPersistence, Cluster cluster) {
        super(sessionPersistence);
        setCluster(cluster);
    }

    @Override
    public void setCluster(Cluster cluster) {
        super.setCluster(cluster);
        init();
        cluster.subscribeStream(this);
    }

    private void init() {
        totalWeight = 0;
        sessionPersistence.clear();
        backendsMap.clear();
        cluster.getOnlineBackends().forEach(backend -> backendsMap.put(Range.closed(totalWeight, totalWeight += backend.getWeight()), backend));
    }

    @Override
    public L4Response getResponse(L4Request l4Request) throws BackendNotOnlineException {
        Backend backend = sessionPersistence.getBackend(l4Request);
        if (backend != null) {
            // If Backend is ONLINE then return the response
            // else remove it from session persistence.
            if (backend.getState() == State.ONLINE) {
                return new L4Response(backend);
            } else {
                sessionPersistence.removeRoute(l4Request.getSocketAddress(), backend);
            }
        }

        int index = RANDOM_INSTANCE.nextInt(totalWeight);
        backend = backendsMap.get(index);

        if (backend == null) {
            // If Backend is `null` then we don't have any
            // backend to return so we will throw exception.
            throw new BackendNotOnlineException("No Backend available for Cluster: " + cluster);
        } else if (backend.getState() != State.ONLINE) {
            init(); // We'll reset the mapping because it could be outdated.

            // If selected Backend is not online then
            // we'll throw an exception.
            throw new BackendNotOnlineException("Randomly selected Backend is not online");
        }

        sessionPersistence.addRoute(l4Request.getSocketAddress(), backend);
        return new L4Response(backend);
    }

    @Override
    public void accept(Object event) {
        if (event instanceof BackendEvent) {
            BackendEvent backendEvent = (BackendEvent) event;
            switch (backendEvent.getType()) {
                case ADDED:
                case ONLINE:
                case OFFLINE:
                case REMOVED:
                    init();
                default:
                    throw new IllegalArgumentException("Unsupported Backend Event Type: " + backendEvent.getType());
            }
        }
    }
}
