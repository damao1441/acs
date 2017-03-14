package com.ge.predix.acs.attribute.cache;

// CHECKSTYLE:OFF: FinalClass
public class AttributeCacheFactory {

    private AttributeCacheFactory() {
        throw new AssertionError();
    }

    public static AttributeCache createResourceAttributeCache(final long maxStorageInMegabytes) {
        // TODO: Flesh this out when working on caching-related changes
        return null;
    }

    public static AttributeCache createSubjectAttributeCache(final long maxStorageInMegabytes) {
        // TODO: Flesh this out when working on caching-related changes
        return null;
    }
}
// CHECKSTYLE:ON: FinalClass
