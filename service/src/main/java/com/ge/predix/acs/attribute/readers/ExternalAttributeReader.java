package com.ge.predix.acs.attribute.readers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.attribute.connector.management.dao.AttributeAdapterConnectionEntity;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.attribute.adapter.AttributesResponse;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public abstract class ExternalAttributeReader implements AttributeReader {

    @Value("${ADAPTER_HTTP_CLIENT_TIMEOUT:3}")
    private int adapterHttpClientTimeout;

    private static final String PREDIX_ZONE_ID = "Predix-Zone-Id";
    private static final String ID = "id";

    private final ZoneEntity zone;
    private final AttributeCache attributeCache;
    private final Map<Integer, OAuth2RestTemplate> adapterRestTemplateCache = new ConcurrentReferenceHashMap<>();

    public ExternalAttributeReader(final ZoneEntity zone, final AttributeCache attributeCache) {
        this.zone = zone;
        this.attributeCache = attributeCache;
    }

    ZoneEntity getZone() {
        return this.zone;
    }

    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        Set<Attribute> attributes;

        if (this.attributeCache == null) {
            attributes = this.getAttributesFromAdapters(identifier);
        } else {
            attributes = this.attributeCache.getAttributes(identifier);
            if (CollectionUtils.isEmpty(attributes)) {
                attributes = this.getAttributesFromAdapters(identifier);
                this.attributeCache.setAttributes(identifier, attributes);
            }
        }

        return attributes;
    }

    private void setRequestFactory(final OAuth2RestTemplate restTemplate) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(this.adapterHttpClientTimeout);
        requestFactory.setConnectTimeout(this.adapterHttpClientTimeout);
        requestFactory.setConnectionRequestTimeout(this.adapterHttpClientTimeout);
        restTemplate.setRequestFactory(requestFactory);
    }

    private OAuth2RestTemplate getAdapterOauth2RestTemplate(
            final AttributeAdapterConnectionEntity attributeAdapterConnectionEntity) {
        String uaaTokenUrl = attributeAdapterConnectionEntity.getUaaTokenUrl();
        String uaaClientId = attributeAdapterConnectionEntity.getUaaClientId();
        String uaaClientSecret = attributeAdapterConnectionEntity.getUaaClientSecret();

        Integer key = new HashCodeBuilder().append(uaaTokenUrl).append(uaaClientId).append(uaaClientSecret)
                .toHashCode();
        OAuth2RestTemplate oAuth2RestTemplate = this.adapterRestTemplateCache.get(key);
        if (oAuth2RestTemplate != null) {
            return oAuth2RestTemplate;
        }

        ClientCredentialsResourceDetails clientCredentials = new ClientCredentialsResourceDetails();
        clientCredentials.setAccessTokenUri(uaaTokenUrl);
        clientCredentials.setClientId(uaaClientId);
        clientCredentials.setClientSecret(uaaClientSecret);
        oAuth2RestTemplate = new OAuth2RestTemplate(clientCredentials);
        this.setRequestFactory(oAuth2RestTemplate);
        this.adapterRestTemplateCache.put(key, oAuth2RestTemplate);
        return oAuth2RestTemplate;
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    Set<Attribute> getAttributesFromAdapters(final String identifier) throws AttributeRetrievalException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(PREDIX_ZONE_ID, this.getZone().getName());

        Set<AttributeAdapterConnectionEntity> attributeAdapterConnectionEntities = this
                .getAttributeAdapterConnections();
        Set<Attribute> attributes = new HashSet<>();

        for (AttributeAdapterConnectionEntity attributeAdapterConnectionEntity : attributeAdapterConnectionEntities) {
            OAuth2RestTemplate adapterRestTemplate = this
                    .getAdapterOauth2RestTemplate(attributeAdapterConnectionEntity);
            HttpEntity<String> adapterEntity = new HttpEntity<>(headers);

            String adapterUrl = UriComponentsBuilder
                    .fromUriString(attributeAdapterConnectionEntity.getAdapterEndpoint()).queryParam(ID, identifier)
                    .toUriString();

            ResponseEntity<AttributesResponse> attributesResponse;
            try {
                attributesResponse = adapterRestTemplate
                        .exchange(adapterUrl, HttpMethod.GET, adapterEntity, AttributesResponse.class);
            } catch (final Throwable e) {
                throw new AttributeRetrievalException(AttributeRetrievalException
                        .getAdapterErrorMessage(adapterUrl, adapterRestTemplate.getResource().getAccessTokenUri(),
                                adapterRestTemplate.getResource().getClientId()), e);
            }

            attributes.addAll(attributesResponse.getBody().getAttributes());
        }

        return attributes;
    }

    abstract Set<AttributeAdapterConnectionEntity> getAttributeAdapterConnections();
}
