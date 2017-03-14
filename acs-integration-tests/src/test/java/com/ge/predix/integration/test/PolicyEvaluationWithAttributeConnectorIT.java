package com.ge.predix.integration.test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.web.AcsApiUriTemplates;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class PolicyEvaluationWithAttributeConnectorIT extends AbstractTestNGSpringContextTests {

    @Value("${ADAPTER_IS_ACTIVE:true}")
    private boolean adapterIsActive;

    @Value("${ADAPTER_MAX_CACHED_INTERVAL_MINUTES:60}")
    private int adapterMaxCachedIntervalMinutes;

    @Value("${ADAPTER_ENDPOINT}")
    private String adapterEndpoint;

    @Value("${ADAPTER_UAA_TOKEN_URL}")
    private String adapterUaaTokenUrl;

    @Value("${ADAPTER_UAA_CLIENT_ID}")
    private String adapterUaaClientId;

    @Value("${ADAPTER_UAA_CLIENT_SECRET}")
    private String adapterUaaClientSecret;

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String acsZone2Name;

    @Value("${ZONE3_NAME:testzone3}")
    private String acsZone3Name;

    @Value("${ACS_UAA_URL}")
    private String acsUaaUrl;

    @Autowired
    private Environment environment;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PolicyHelper policyHelper;

    private static final String IDENTIFIER = "part/03f95db1-4255-4265-a509-f7bca3e1fee4";

    private Zone zone;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private boolean registerWithZac;
    private URI resourceAttributeConnectorUrl;

    private HttpHeaders getHeadersWithZoneId() throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.zone.getSubdomain());
        return httpHeaders;
    }

    private void configureAttributeConnector(final boolean isActive, final int maxCachedIntervalMinutes,
            final AttributeAdapterConnection... adapters) throws IOException {
        AttributeConnector attributeConnector = new AttributeConnector();
        attributeConnector.setIsActive(isActive);
        attributeConnector.setMaxCachedIntervalMinutes(maxCachedIntervalMinutes);
        attributeConnector.setAdapters(new HashSet<>(Arrays.asList(adapters)));
        this.resourceAttributeConnectorUrl = URI.create(this.zoneHelper.getAcsBaseURL() + AcsApiUriTemplates.V1
                + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL);
        HttpHeaders httpHeaders = this.getHeadersWithZoneId();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        this.acsAdminRestTemplate.exchange(this.resourceAttributeConnectorUrl, HttpMethod.PUT,
                new HttpEntity<>(attributeConnector, httpHeaders), AttributeConnector.class);
    }

    private void setupPredixAcs() throws IOException {
        this.zacTestUtil.assumeZacServerAvailable();
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.registerWithZac = true;

    }

    private void setupPublicAcs() throws IOException {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.acsUaaUrl);
        uaaTestUtil.setup(Arrays.asList(this.acsZone1Name, this.acsZone2Name, this.acsZone3Name));
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.registerWithZac = false;
    }

    @BeforeClass
    void beforeClass() throws IOException {
        if (Arrays.asList(this.environment.getActiveProfiles()).contains("public")) {
            setupPublicAcs();
        } else {
            setupPredixAcs();
        }

        this.zone = this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);

        this.configureAttributeConnector(this.adapterIsActive, this.adapterMaxCachedIntervalMinutes,
                new AttributeAdapterConnection(this.adapterEndpoint, this.adapterUaaTokenUrl, this.adapterUaaClientId,
                        this.adapterUaaClientSecret));

        // TODO: Currently data is pre-seeded by a script in the acs-asset-adapter Git repo. At some point, we might
        // want to instead seed the data to the underlying service encapsulated by the resource- or subject-related
        // adapter here.
    }

    private void deconfigureAttributeConnector() throws IOException {
        HttpHeaders httpHeaders = this.getHeadersWithZoneId();
        this.acsAdminRestTemplate
                .exchange(this.resourceAttributeConnectorUrl, HttpMethod.DELETE, new HttpEntity<>(httpHeaders),
                        Void.class);
    }

    @AfterClass
    void afterClass() throws IOException {
        this.deconfigureAttributeConnector();
        this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

    @Test
    public void testPolicyEvaluationWithActiveAdapters() throws Exception {
        HttpHeaders httpHeaders = this.getHeadersWithZoneId();
        String testPolicyName = this.policyHelper
                .setTestPolicy(this.acsAdminRestTemplate, httpHeaders, this.zoneHelper.getAcsBaseURL(),
                        "src/test/resources/policy-set-with-one-policy-using-resource-attributes-from-asset-adapter"
                                + ".json");
        try {
            PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                    .createMultiplePolicySetsEvalRequest("", "", IDENTIFIER, null,
                            new LinkedHashSet<>(Collections.singletonList(testPolicyName)));
            ResponseEntity<PolicyEvaluationResult> policyEvaluationResponse = this.acsAdminRestTemplate
                    .postForEntity(this.zoneHelper.getAcsBaseURL() + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                            new HttpEntity<>(policyEvaluationRequest, httpHeaders), PolicyEvaluationResult.class);
            Assert.assertEquals(policyEvaluationResponse.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult policyEvaluationResult = policyEvaluationResponse.getBody();
            Assert.assertEquals(policyEvaluationResult.getEffect(), Effect.PERMIT);
        } finally {
            this.policyHelper
                    .deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getAcsBaseURL(), testPolicyName,
                            httpHeaders);
        }
    }

    @Test
    public void testPolicyEvaluationWithInactiveAdapters() throws Exception {
        // TODO: Flesh this out when isActive is added to AttributeConnectorEntity
    }
}
