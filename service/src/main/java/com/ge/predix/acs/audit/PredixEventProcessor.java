package com.ge.predix.acs.audit;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.AuditEventProcessor;
import com.ge.predix.audit.sdk.AuditCallback;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.AuditClientType;
import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.eventhub.EventHubClientException;

@Component
@Profile("predix")
public class PredixEventProcessor implements AuditEventProcessor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PredixEventProcessor.class);

    @Autowired
    private PredixEventMapper eventMapper;

    private VcapLoaderServiceImpl vcapLoaderService = new VcapLoaderServiceImpl();

    private AuditClient auditClient;

    private AuditConfiguration sdkConfig;

    private String uaaUrl;

    private String uaaClientId;

    private String uaaClientSecret;

    private String ehubZoneId;

    private String ehubHost;

    private int ehubPort;

    public PredixEventProcessor() throws AuditException, EventHubClientException {
        try {
            sdkConfig = vcapLoaderService.getConfigFromVcap();
        } catch (VcapLoadException e) {
            sdkConfig = AuditConfiguration.builder().bulkMode(true).clientType(AuditClientType.ASYNC).uaaUrl(uaaUrl)
                    .uaaClientId(uaaClientId).uaaClientSecret(uaaClientSecret).ehubZoneId(ehubZoneId).ehubHost(ehubHost)
                    .ehubPort(ehubPort).build();
        }
        auditClient = new AuditClient(sdkConfig, auditCallback());
    }

    @Override
    public boolean process(final AuditEvent auditEvent) {
        AuditEventV2 predixEvent = eventMapper.map(auditEvent);
        try {
            this.auditClient.audit(predixEvent);
        } catch (AuditException e) {
            LOGGER.warn("Audit failed on process with event: " + predixEvent.toString());
            return false;
        }
        return true;
    }

    public AuditCallback auditCallback() {
        return new AuditCallback() {
            @Override
            public void onFailure(final com.ge.predix.audit.sdk.message.AuditEvent arg0, final FailReport arg1,
                    final String arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onSuccees(final com.ge.predix.audit.sdk.message.AuditEvent arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onValidate(final com.ge.predix.audit.sdk.message.AuditEvent arg0,
                    final List<ValidatorReport> arg1) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onFailure(final FailReport arg0, final String arg1) {
                // TODO Auto-generated method stub
            }
        };
    }

}