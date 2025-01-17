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

package com.shieldblaze.expressgateway.integration.aws.ec2.instance;

import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.util.List;

public final class ScaleOutRequest {

    private final String imageId;
    private final String subnetId;
    private final InstanceType instanceType;
    private final List<String> securityGroups;
    private final boolean autoscaled;

    ScaleOutRequest(String imageId, String subnetId, InstanceType instanceType, List<String> securityGroups, boolean autoscaled) {
        this.imageId = imageId;
        this.subnetId = subnetId;
        this.instanceType = instanceType;
        this.securityGroups = securityGroups;
        this.autoscaled = autoscaled;
    }

    public String imageId() {
        return imageId;
    }

    public String subnetId() {
        return subnetId;
    }

    public InstanceType instanceType() {
        return instanceType;
    }

    public List<String> securityGroups() {
        return securityGroups;
    }

    public boolean autoscaled() {
        return autoscaled;
    }
}
