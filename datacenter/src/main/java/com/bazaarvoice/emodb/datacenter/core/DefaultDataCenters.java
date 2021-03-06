package com.bazaarvoice.emodb.datacenter.core;

import com.bazaarvoice.emodb.datacenter.api.DataCenter;
import com.bazaarvoice.emodb.datacenter.api.DataCenters;
import com.bazaarvoice.emodb.datacenter.db.DataCenterDAO;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultDataCenters implements DataCenters {
    private static final Logger _log = LoggerFactory.getLogger(DefaultDataCenters.class);

    private final DataCenterDAO _dataCenterDao;
    private final String _selfDataCenter;
    private final String _systemDataCenter;
    private volatile Supplier<CachedInfo> _cache;

    @Inject
    public DefaultDataCenters(DataCenterDAO dataCenterDao,
                              @SelfDataCenter String selfDataCenter,
                              @SystemDataCenter String systemDataCenter) {
        _dataCenterDao = checkNotNull(dataCenterDao, "dataCenterDao");
        _selfDataCenter = checkNotNull(selfDataCenter, "selfDataCenter");
        _systemDataCenter = checkNotNull(systemDataCenter, "systemDataCenter");
        refresh();
    }

    @Override
    public void refresh() {
        _cache = Suppliers.memoizeWithExpiration(new Supplier<CachedInfo>() {
            @Override
            public CachedInfo get() {
                return new CachedInfo(_dataCenterDao.loadAll());
            }
        }, 10, TimeUnit.MINUTES);
    }

    @Override
    public Collection<DataCenter> getAll() {
        return _cache.get().getAll();
    }

    @Override
    public DataCenter getSelf() {
        return get(_selfDataCenter);
    }

    @Override
    public DataCenter getSystem() {
        return get(_systemDataCenter);
    }

    private DataCenter get(String name) {
        DataCenter dataCenter = _cache.get().get(name);
        checkArgument(dataCenter != null, "Unknown data center: {}", name);
        return dataCenter;
    }

    @Override
    public Collection<DataCenter> getForKeyspace(String keyspace) {
        return _cache.get().getForKeyspace(keyspace);
    }

    private static void verifySystemDataCenters(Collection<DataCenter> dataCenters) {
        Set<String> systemDataCenters = Sets.newTreeSet();
        for (DataCenter dataCenter : dataCenters) {
            if (dataCenter.isSystem()) {
                systemDataCenters.add(dataCenter.getName());
            }
        }
        if (systemDataCenters.size() > 1) {
            _log.error("Multiple data centers are configured as system data centers: {}", systemDataCenters);
        }
    }

    private static class CachedInfo {
        private final Map<String, DataCenter> _dataCenterByName;
        private final Multimap<String, DataCenter> _dataCenterByKeyspace;

        private CachedInfo(Map<String, DataCenter> dataCenterByName) {
            verifySystemDataCenters(dataCenterByName.values());

            _dataCenterByName = dataCenterByName;

            ImmutableMultimap.Builder<String, DataCenter> builder = ImmutableMultimap.builder();
            for (DataCenter dataCenter : dataCenterByName.values()) {
                for (String keyspace : dataCenter.getCassandraKeyspaces()) {
                    builder.put(keyspace, dataCenter);
                }
            }
            _dataCenterByKeyspace = builder.build();
        }

        private Collection<DataCenter> getAll() {
            return _dataCenterByName.values();
        }

        private DataCenter get(String name) {
            return _dataCenterByName.get(name);
        }

        private Collection<DataCenter> getForKeyspace(String keyspace) {
            return _dataCenterByKeyspace.get(keyspace);
        }
    }
}
