package com.ge.predix.acs.attribute.readers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.ge.predix.acs.attribute.cache.AttributeCacheFactory;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

// CHECKSTYLE:OFF: FinalClass
@Component
public class AttributeReaderFactory {

    // TODO: Rethink how to configure this when fleshing out caching-related changes
    @Value("${MAX_CACHE_STORAGE_SIZE_PER_ZONE_IN_MB:100}")
    private long maxCacheStorageSizePerZoneInMb;

    @Autowired
    private ZoneResolver zoneResolver;

    @Autowired
    private PrivilegeServiceResourceAttributeReader privilegeServiceResourceAttributeReader;

    @Autowired
    private PrivilegeServiceSubjectAttributeReader privilegeServiceSubjectAttributeReader;

    // Caches that use the multiton design pattern (keyed off the zone name)
    private final Map<String, ExternalResourceAttributeReader> externalResourceAttributeReaderCache = new
            ConcurrentReferenceHashMap<>();
    private final Map<String, ExternalSubjectAttributeReader> externalSubjectAttributeReaderCache = new
            ConcurrentReferenceHashMap<>();

    public ResourceAttributeReader getResourceAttributeReader() {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        if (zone.getResourceAttributeConnector() == null || !zone.getResourceAttributeConnector().isActive()) {
            return this.privilegeServiceResourceAttributeReader;
        }

        String zoneName = zone.getName();
        ExternalResourceAttributeReader externalResourceAttributeReader = this.externalResourceAttributeReaderCache
                .get(zoneName);
        if (externalResourceAttributeReader != null) {
            return externalResourceAttributeReader;
        }

        externalResourceAttributeReader = new ExternalResourceAttributeReader(zone,
                AttributeCacheFactory.createResourceAttributeCache(this.maxCacheStorageSizePerZoneInMb));
        this.externalResourceAttributeReaderCache.put(zoneName, externalResourceAttributeReader);
        return externalResourceAttributeReader;
    }

    public SubjectAttributeReader getSubjectAttributeReader() {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        if (zone.getSubjectAttributeConnector() == null || !zone.getSubjectAttributeConnector().isActive()) {
            return this.privilegeServiceSubjectAttributeReader;
        }

        String zoneName = zone.getName();
        ExternalSubjectAttributeReader externalSubjectAttributeReader = this.externalSubjectAttributeReaderCache
                .get(zoneName);
        if (externalSubjectAttributeReader != null) {
            return externalSubjectAttributeReader;
        }

        externalSubjectAttributeReader = new ExternalSubjectAttributeReader(zone,
                AttributeCacheFactory.createSubjectAttributeCache(this.maxCacheStorageSizePerZoneInMb));
        this.externalSubjectAttributeReaderCache.put(zoneName, externalSubjectAttributeReader);
        return externalSubjectAttributeReader;
    }
}
// CHECKSTYLE:ON: FinalClass
