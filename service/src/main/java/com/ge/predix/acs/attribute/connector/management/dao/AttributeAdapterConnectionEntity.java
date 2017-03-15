package com.ge.predix.acs.attribute.connector.management.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "attribute_adapter_connection")
public class AttributeAdapterConnectionEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "connector_id", referencedColumnName = "id", nullable = false, updatable = false)
    private AttributeConnectorEntity connector;

    @Column(name = "adapter_endpoint", nullable = false, length = 256)
    private String adapterEndpoint;

    @Column(name = "adapter_token_url", nullable = false, length = 256)
    private String adapterTokenUrl;

    @Column(name = "adapter_client_id", nullable = false, length = 128)
    private String adapterClientId;

    @Column(name = "adapter_client_secret", nullable = false, length = 128)
    private String adapterClientSecret;

    public AttributeAdapterConnectionEntity(final AttributeConnectorEntity connector, final String adapterEndpoint,
            final String uaaTokenUrl, final String uaaClientId, final String uaaClientSecret) {
        this.connector = connector;
        this.adapterEndpoint = adapterEndpoint;
        this.adapterTokenUrl = uaaTokenUrl;
        this.adapterClientId = uaaClientId;
        this.adapterClientSecret = uaaClientSecret;
    }

    public AttributeAdapterConnectionEntity() {
    }

    public long getId() {
        return this.id;
    }

    public String getAdapterEndpoint() {
        return adapterEndpoint;
    }

    public String getUaaTokenUrl() {
        return adapterTokenUrl;
    }

    public String getUaaClientId() {
        return adapterClientId;
    }

    public String getUaaClientSecret() {
        return adapterClientSecret;
    }

}
