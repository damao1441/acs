package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class ExternalSubjectAttributeReader extends ExternalAttributeReader implements SubjectAttributeReader {

    public ExternalSubjectAttributeReader(final ZoneEntity zone, final AttributeCache subjectAttributeCache) {
        super(zone, subjectAttributeCache);
    }

    @Override
    Set<AttributeAdapterConnection> getAttributeAdapterConnections() {
        return this.getZone().getSubjectAttributeConnector().getAdapters();
    }

    @Override
    public Set<Attribute> getAttributesByScope(final String subjectId, final Set<Attribute> scopes) {
        // Connectors have no notion of scoped attributes
        return this.getAttributes(subjectId);
    }
}
