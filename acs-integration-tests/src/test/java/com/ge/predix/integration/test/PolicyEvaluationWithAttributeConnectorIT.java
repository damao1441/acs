package com.ge.predix.integration.test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class PolicyEvaluationWithAttributeConnectorIT extends AbstractTestNGSpringContextTests {

    @Value("${ADAPTER_ENDPOINT}")
    private String adapterEndpoint;

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ACS_UAA_URL}")
    private String acsUaaUrl;

    @Value("${ASSET_TOKEN_URL}")
    private String assetTokenUrl;

    @Value("${ASSET_CLIENT_ID}")
    private String assetClientId;

    @Value("${ASSET_CLIENT_SECRET}")
    private String assetClientSecret;

    @Value("${ASSET_ZONE_ID}")
    private String assetZoneId;

    @Value("${ASSET_URL}")
    private String assetUrl;

    @Value("${ADAPTER_UAA_TOKEN_URL:${ASSET_TOKEN_URL}}")
    private String adapterUaaTokenUrl;

    @Value("${ADAPTER_UAA_CLIENT_ID:${ASSET_CLIENT_ID}}")
    private String adapterUaaClientId;

    @Value("${ADAPTER_UAA_CLIENT_SECRET:${ASSET_CLIENT_SECRET}}")
    private String adapterUaaClientSecret;

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

    private static final String TEST_PART_ID = "part/03f95db1-4255-4265-a509-f7bca3e1fee4";
    private static final String ASSET_URI_PATH_SEGMENT = '/' + TEST_PART_ID;

    private Zone zone;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private OAuth2RestTemplate assetRestTemplate;
    private boolean registerWithZac;
    private URI resourceAttributeConnectorUrl = URI.create(this.zoneHelper.getAcsBaseURL() + "/v1/resource/connector");

    private HttpHeaders zoneHeader() throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.zone.getSubdomain());
        return httpHeaders;
    }

    private HttpHeaders getHeadersWithAssetZoneId() throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.assetZoneId);
        return httpHeaders;
    }

    private void createAssetRestTemplate() {
        ClientCredentialsResourceDetails clientCredentials = new ClientCredentialsResourceDetails();
        clientCredentials.setAccessTokenUri(this.assetTokenUrl);
        clientCredentials.setClientId(this.assetClientId);
        clientCredentials.setClientSecret(this.assetClientSecret);
        this.assetRestTemplate = new OAuth2RestTemplate(clientCredentials);

        CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.assetRestTemplate.setRequestFactory(requestFactory);
    }

    private void configureMockAssetData() throws IOException {
        this.createAssetRestTemplate();

        HttpHeaders httpHeaders = this.getHeadersWithAssetZoneId();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject part = new JSONObject();
        part.put("id", "03f95db1-4255-4265-a509-f7bca3e1fee4");
        part.put("collection", "part");
        part.put("partModel", "/partmodels/9a92831d-42f1-4f9e-86bf-4c0914f481e4");
        part.put("structureModel", "/structureModels/8c787978-bd8b-417a-b759-f63a8a6d43ee");
        part.put("serialNumber", "775277328");
        part.put("parent", "/part/01af94ed-5425-44e4-9f6e-2a58cba7b559");
        part.put("aircraftPart", "/aircraftPart/13a71359-db68-4602-aac5-a8fa401c3194");
        part.put("aircraftPartModel", "/aircraftPartModels/1dc6a36d-a24e-4fec-a181-f576c95a8104");
        part.put("uri", ASSET_URI_PATH_SEGMENT);

        JSONArray partArray = new JSONArray();
        partArray.add(part);

        this.assetRestTemplate.exchange(this.assetUrl + "/part", HttpMethod.POST,
                new HttpEntity<>(partArray.toString(), httpHeaders), String.class);
    }

    private void configureAttributeConnector(final boolean isActive) throws IOException {

        List<AttributeAdapterConnection> adapters = Collections.singletonList(new AttributeAdapterConnection(
                this.adapterEndpoint, this.adapterUaaTokenUrl, this.adapterUaaClientId, this.adapterUaaClientSecret));

        AttributeConnector attributeConnector = new AttributeConnector();
        attributeConnector.setIsActive(isActive);
        attributeConnector.setAdapters(new HashSet<>(adapters));
        HttpHeaders headers = this.zoneHeader();
        headers.setContentType(MediaType.APPLICATION_JSON);
        this.acsAdminRestTemplate.exchange(this.resourceAttributeConnectorUrl, HttpMethod.PUT,
                new HttpEntity<>(attributeConnector, headers), AttributeConnector.class);
    }

    private void setupPredixAcs() throws IOException {
        this.zacTestUtil.assumeZacServerAvailable();
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.registerWithZac = true;

    }

    private void setupPublicAcs() throws IOException {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.acsUaaUrl);
        uaaTestUtil.setup(Collections.singletonList(this.acsZone1Name));
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
        this.configureMockAssetData();
    }

    private void deconfigureMockAssetData() throws IOException {
        HttpHeaders httpHeaders = this.getHeadersWithAssetZoneId();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        this.assetRestTemplate.exchange(this.assetUrl + ASSET_URI_PATH_SEGMENT, HttpMethod.DELETE,
                new HttpEntity<>(httpHeaders), Void.class);
    }

    private void deconfigureAttributeConnector() throws IOException {
        this.acsAdminRestTemplate.exchange(this.resourceAttributeConnectorUrl, HttpMethod.DELETE,
                new HttpEntity<>(zoneHeader()), Void.class);
    }

    @AfterClass
    void afterClass() throws IOException {
        this.deconfigureMockAssetData();
        this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

    @Test(dataProvider = "adapterStatusesAndResultingEffects")
    public void testPolicyEvaluationWithAdapters(final boolean adapterActive, final Effect expectedEffect)
            throws Exception {
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, zoneHeader(),
                this.zoneHelper.getAcsBaseURL(),
                "src/test/resources/policy-set-with-one-policy-using-resource-attributes-from-asset-adapter.json");

        try {
            this.configureAttributeConnector(adapterActive);
            PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper.createEvalRequest("GET",
                    "testSubject", TEST_PART_ID, null);
            ResponseEntity<PolicyEvaluationResult> policyEvaluationResponse = this.acsAdminRestTemplate.postForEntity(
                    this.zoneHelper.getAcsBaseURL() + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, zoneHeader()), PolicyEvaluationResult.class);
            Assert.assertEquals(policyEvaluationResponse.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult policyEvaluationResult = policyEvaluationResponse.getBody();
            Assert.assertEquals(policyEvaluationResult.getEffect(), expectedEffect);
        } finally {
            this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getAcsBaseURL(),
                    testPolicyName, zoneHeader());
            this.deconfigureAttributeConnector();
        }
    }

    @DataProvider
    private Object[][] adapterStatusesAndResultingEffects() {
        return new Object[][] { { true, Effect.PERMIT }, { false, Effect.NOT_APPLICABLE } };
    }
}
