package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.attribute.connector.management.dao.AttributeAdapterConnectionEntity;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class ExternalResourceAttributeReader extends ExternalAttributeReader implements ResourceAttributeReader {

    public ExternalResourceAttributeReader(final ZoneEntity zone, final AttributeCache resourceAttributeCache) {
        super(zone, resourceAttributeCache);
    }

    @Override
    Set<AttributeAdapterConnectionEntity> getAttributeAdapterConnections() {
        return this.getZone().getResourceAttributeConnector().getAttributeAdapterConnections();
    }
}
