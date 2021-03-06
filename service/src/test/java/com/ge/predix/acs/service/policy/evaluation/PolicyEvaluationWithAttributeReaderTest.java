package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.PolicyContextResolver;
import com.ge.predix.acs.attribute.readers.ExternalResourceAttributeReader;
import com.ge.predix.acs.attribute.readers.ExternalSubjectAttributeReader;
import com.ge.predix.acs.attribute.readers.AttributeReaderFactory;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionCache;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.service.policy.admin.PolicyManagementService;
import com.ge.predix.acs.service.policy.matcher.PolicyMatcherImpl;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.service.policy.validation.PolicySetValidatorImpl;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@ContextConfiguration(classes = { GroovyConditionCache.class, PolicySetValidatorImpl.class })
public class PolicyEvaluationWithAttributeReaderTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private PolicyEvaluationServiceImpl evaluationService;
    @Mock
    private PolicyManagementService policyService;
    @Mock
    private PolicyContextResolver policyScopeResolver;
    @Mock
    private ZoneResolver zoneResolver;
    @Mock
    private PolicyEvaluationCacheCircuitBreaker cache;
    @Mock
    private AttributeReaderFactory attributeReaderFactory;
    @Mock
    private ExternalResourceAttributeReader externalResourceAttributeReader;
    @Mock
    private ExternalSubjectAttributeReader externalSubjectAttributeReader;
    @Autowired
    private PolicySetValidator policySetValidator;

    private final PolicyMatcherImpl policyMatcher = new PolicyMatcherImpl();

    @BeforeClass
    public void setupClass() {
        ((PolicySetValidatorImpl) this.policySetValidator)
                .setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH");
        ((PolicySetValidatorImpl) this.policySetValidator).init();
    }

    @BeforeMethod
    public void setupMethod() throws Exception {
        this.evaluationService = new PolicyEvaluationServiceImpl();
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(this.policyMatcher, "attributeReaderFactory", this.attributeReaderFactory);
        Whitebox.setInternalState(this.evaluationService, "policyMatcher", this.policyMatcher);
        Whitebox.setInternalState(this.evaluationService, "policySetValidator", this.policySetValidator);
        when(this.zoneResolver.getZoneEntityOrFail()).thenReturn(new ZoneEntity(0L, "testzone"));
        when(this.cache.get(any(PolicyEvaluationRequestCacheKey.class))).thenReturn(null);
    }

    @Test
    public void testPolicyEvaluation() throws Exception {
        PolicySet policySet = new ObjectMapper().readValue(
                new File("src/test/resources/policy-set-with-one-policy-one-condition-using-res-attributes.json"),
                PolicySet.class);
        when(this.policyService.getAllPolicySets()).thenReturn(Arrays.asList(policySet));

        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "location", "sanramon"));
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "role_required", "admin"));
        BaseResource testResource = new BaseResource("/sites/1234", resourceAttributes);

        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "admin"));
        BaseSubject testSubject = new BaseSubject("test-subject", subjectAttributes);

        when(this.attributeReaderFactory.getResourceAttributeReader()).thenReturn(this.externalResourceAttributeReader);
        when(this.externalResourceAttributeReader.getAttributes(anyString())).thenReturn(testResource.getAttributes());
        when(this.attributeReaderFactory.getSubjectAttributeReader()).thenReturn(this.externalSubjectAttributeReader);
        when(this.externalSubjectAttributeReader.getAttributesByScope(anyString(), anySetOf(Attribute.class)))
                .thenReturn(testSubject.getAttributes());

        PolicyEvaluationResult evalResult = this.evaluationService.evalPolicy(
                createRequest(testResource.getResourceIdentifier(), testSubject.getSubjectIdentifier(), "GET"));
        Assert.assertEquals(evalResult.getEffect(), Effect.PERMIT);
    }

    private PolicyEvaluationRequestV1 createRequest(final String resource, final String subject, final String action) {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(action);
        request.setSubjectIdentifier(subject);
        request.setResourceIdentifier(resource);
        return request;
    }
}
