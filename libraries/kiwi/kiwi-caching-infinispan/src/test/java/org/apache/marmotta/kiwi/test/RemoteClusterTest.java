/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.marmotta.kiwi.test;

import org.apache.marmotta.kiwi.caching.CacheManager;
import org.apache.marmotta.kiwi.config.CacheManagerType;
import org.apache.marmotta.kiwi.config.KiWiConfiguration;
import org.apache.marmotta.kiwi.infinispan.embedded.InfinispanEmbeddedCacheManager;
import org.apache.marmotta.kiwi.infinispan.remote.CustomJBossMarshaller;
import org.apache.marmotta.kiwi.test.cluster.BaseClusterTest;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.equivalence.ByteArrayEquivalence;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add file description here!
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
public class RemoteClusterTest extends BaseClusterTest {

    private static Logger log = LoggerFactory.getLogger(RemoteClusterTest.class);

    private static HotRodServer hotRodServer1, hotRodServer2, hotRodServer3;

    @BeforeClass
    public static void setup() {
        hotRodServer1 = buildServer(61222);
        hotRodServer2 = buildServer(61223);
        hotRodServer3 = buildServer(61224);

        ClusterTestSupport s = new ClusterTestSupport(CacheManagerType.INFINISPAN_HOTROD);

        KiWiConfiguration base = s.buildBaseConfiguration();
        base.setClusterAddress("127.0.0.1");
        s.setup(base);
    }



    private static HotRodServer buildServer(int port) {
        HotRodServer hotRodServer = new HotRodServer() {
            @Override
            public ConfigurationBuilder createTopologyCacheConfig(long distSyncTimeout) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                ConfigurationBuilder c = super.createTopologyCacheConfig(distSyncTimeout);
                c.transaction().syncCommitPhase(false).syncRollbackPhase(false);
                return c;
            }
        };

        HotRodServerConfiguration hotrodConfig = new HotRodServerConfigurationBuilder()
                .host("127.0.0.1")
                .port(port)
                .proxyHost("127.0.0.1")
                .proxyPort(port)
                .topologyStateTransfer(false)
                .defaultCacheName(BasicCacheContainer.DEFAULT_CACHE_NAME)
                .idleTimeout(0)
                .workerThreads(2)
                .build(true);


        GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
                .classLoader(InfinispanEmbeddedCacheManager.class.getClassLoader())
                .globalJmxStatistics()
                .jmxDomain("org.apache.marmotta.kiwi")
                .allowDuplicateDomains(true)
                .build();

        Configuration defaultConfiguration = new ConfigurationBuilder()
                .clustering()
                    .cacheMode(CacheMode.LOCAL)
                    .sync()
                .dataContainer()
                    .keyEquivalence(ByteArrayEquivalence.INSTANCE)
                    .valueEquivalence(ByteArrayEquivalence.INSTANCE)
                .build();

        EmbeddedCacheManager cacheManager = new DefaultCacheManager(globalConfiguration, defaultConfiguration, true);
        cacheManager.defineConfiguration(CacheManager.NODE_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.TRIPLE_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.URI_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.BNODE_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.LITERAL_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.NS_PREFIX_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.NS_URI_CACHE, defaultConfiguration);
        cacheManager.defineConfiguration(CacheManager.REGISTRY_CACHE, defaultConfiguration);
        cacheManager.getCache(CacheManager.NODE_CACHE, true);
        cacheManager.getCache(CacheManager.TRIPLE_CACHE, true);
        cacheManager.getCache(CacheManager.URI_CACHE, true);
        cacheManager.getCache(CacheManager.BNODE_CACHE, true);
        cacheManager.getCache(CacheManager.LITERAL_CACHE, true);
        cacheManager.getCache(CacheManager.NS_PREFIX_CACHE, true);
        cacheManager.getCache(CacheManager.NS_URI_CACHE, true);
        cacheManager.getCache(CacheManager.REGISTRY_CACHE, true);

        hotRodServer.start(hotrodConfig, cacheManager);

        // test if cache is available
        org.infinispan.client.hotrod.configuration.Configuration remoteCfg = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                .addServer()
                    .host("127.0.0.1")
                    .port(port)
                .marshaller(new CustomJBossMarshaller())
                .pingOnStartup(true)
                .build(true);


        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(remoteCfg);
        Assert.assertTrue(remoteCacheManager.isStarted());

        RemoteCache<String, String> m = remoteCacheManager.getCache();

        m.put("xyz", "abc");
        String n = m.get("xyz");

        Assert.assertNotNull(n);

        return hotRodServer;
    }
}
