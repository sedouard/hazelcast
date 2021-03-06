/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.map.impl.proxy.MapProxyImpl;
import com.hazelcast.map.impl.proxy.NearCachedMapProxyImpl;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.RemoteService;

import java.util.Map;

import static com.hazelcast.map.impl.MapConfigValidator.checkInMemoryFormat;
import static com.hazelcast.map.impl.MapService.SERVICE_NAME;

/**
 * Defines remote service behavior of map service.
 *
 * @see MapService
 */
class MapRemoteService implements RemoteService {

    protected final MapServiceContext mapServiceContext;
    protected final NodeEngine nodeEngine;

    MapRemoteService(MapServiceContext mapServiceContext) {
        this.mapServiceContext = mapServiceContext;
        this.nodeEngine = mapServiceContext.getNodeEngine();
    }

    @Override
    public DistributedObject createDistributedObject(String name) {
        MapConfig mapConfig = nodeEngine.getConfig().findMapConfig(name);
        checkInMemoryFormat(mapConfig.getInMemoryFormat());

        if (mapConfig.isNearCacheEnabled()) {
            checkInMemoryFormat(mapConfig.getNearCacheConfig().getInMemoryFormat());

            return new NearCachedMapProxyImpl(name, mapServiceContext.getService(), nodeEngine);
        } else {
            return new MapProxyImpl(name, mapServiceContext.getService(), nodeEngine);
        }
    }

    @Override
    public void destroyDistributedObject(String name) {
        mapServiceContext.destroyMap(name);
        nodeEngine.getEventService().deregisterAllListeners(SERVICE_NAME, name);
        Map<String, MapContainer> mapContainers = mapServiceContext.getMapContainers();
        MapContainer mapContainer = mapContainers.remove(name);
        if (mapContainer != null) {
            mapServiceContext.getNearCacheProvider().destroyNearCache(name);
            mapContainer.getMapStoreContext().stop();
        }
    }

}
