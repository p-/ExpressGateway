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
package com.shieldblaze.expressgateway.backend.services;

import com.shieldblaze.expressgateway.backend.Node;
import com.shieldblaze.expressgateway.backend.State;
import com.shieldblaze.expressgateway.healthcheck.Health;

import java.util.Objects;

final class HealthCheckService implements Runnable {

    private final Node node;

    HealthCheckService(Node node) {
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public void run() {
        Health oldHealth = node.health();
        node.healthCheck().run();

        // If new Health is GOOD and old Health is not GOOD then update.
        if (node.health() == Health.GOOD && oldHealth != Health.GOOD) {
            node.state(State.ONLINE);
        } else if (node.health() == Health.MEDIUM && oldHealth != Health.MEDIUM) {
            node.state(State.IDLE);
        } else if (node.health() == Health.BAD && oldHealth != Health.BAD) {
            node.state(State.OFFLINE);
        } else {
            // Maybe In case of Health is UNKNOWN.
        }
    }
}