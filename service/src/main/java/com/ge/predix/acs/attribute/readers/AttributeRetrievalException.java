package com.ge.predix.acs.attribute.readers;

import java.text.MessageFormat;

public class AttributeRetrievalException extends RuntimeException {

    public AttributeRetrievalException(final String message, final Throwable cause) {
        super(message, cause);
    }

    static String getAdapterErrorMessage(final String adapterEndpoint, final String uaaTokenUrl,
            final String uaaClientId) {
        return MessageFormat
                .format("Couldn't get attributes from the adapter with endpoint \"{}\", UAA token URL \"{}\" and "
                        + "client ID \"{}\"", adapterEndpoint, uaaTokenUrl, uaaClientId);
    }
}
