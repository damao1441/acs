package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class ExternalResourceAttributeReader extends ExternalAttributeReader implements ResourceAttributeReader {

    public ExternalResourceAttributeReader(final ZoneEntity zone, final AttributeCache resourceAttributeCache) {
        super(zone, resourceAttributeCache);
    }

    @Override
    Set<AttributeAdapterConnection> getAttributeAdapterConnections() {
        return this.getZone().getResourceAttributeConnector().getAdapters();
    }
}
