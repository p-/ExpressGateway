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
package com.shieldblaze.expressgateway.backend;

final class ConnectionCleaner implements Runnable {

    private final Backend backend;

    ConnectionCleaner(Backend backend) {
        this.backend = backend;
    }

    @Override
    public void run() {
        // Remove connection from queue if they're not active.
        backend.connectionList.removeIf(connection -> {
            if (connection.hasConnectionTimedOut() && !connection.isActive()) {
                backend.removeConnection(connection);
                return true;
            } else {
                return false;
            }
        });
    }
}