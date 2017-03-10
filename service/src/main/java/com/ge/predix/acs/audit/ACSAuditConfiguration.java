package com.ge.predix.acs.audit;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.ge.predix.audit.sdk.AuditCallback;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.AuditClientType;
import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.eventhub.EventHubClientException;

@Configuration
@Profile("predixAudit")
public class ACSAuditConfiguration {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ACSAuditConfiguration.class);

    private VcapLoaderServiceImpl vcapLoaderService = new VcapLoaderServiceImpl();

    private AuditConfiguration sdkConfig;

    @Value("${AUDIT_UAA_URL}")
    private String auditUaaUrl;

    @Value("${AUDIT_UAA_CLIENT_ID}")
    private String auditUaaClientId;

    @Value("${AUDIT_UAA_CLIENT_SECRET}")
    private String auditUaaClientSecret;

    @Bean
    public AuditClient createAuditClient() throws AuditException, EventHubClientException {
        try {
            sdkConfig = vcapLoaderService.getConfigFromVcap();
            sdkConfig.setClientType(AuditClientType.ASYNC);
            sdkConfig.setUaaUrl(auditUaaUrl);
            sdkConfig.setUaaClientId(auditUaaClientId);
            sdkConfig.setUaaClientSecret(auditUaaClientSecret);

        } catch (VcapLoadException e) {
            e.printStackTrace();
        }
        AuditClient auditClient = new AuditClient(sdkConfig, auditCallback());
        return auditClient;
    }

    public AuditCallback auditCallback() {
        return new AuditCallback() {
            @Override
            public void onFailure(final AuditEvent arg0, final FailReport arg1, final String arg2) {
                LOGGER.info("AUDIT EVENT FAILED: " + arg0.toString());
                LOGGER.info("AUDIT FAIL REPORT: " + arg1.toString());
            }

            @Override
            public void onSuccees(final AuditEvent arg0) {
                LOGGER.info("AUDIT EVENT SUCCESS: " + arg0.toString());
            }

            @Override
            public void onValidate(final AuditEvent arg0, final List<ValidatorReport> arg1) {
                LOGGER.info("AUDIT EVENT VALIDATE: " + arg0.toString());
                for (ValidatorReport report : arg1) {
                    LOGGER.info("AUDIT ValidatorReport: " + report.toString());
                }
            }

            @Override
            public void onFailure(final FailReport arg0, final String arg1) {
                LOGGER.info("AUDIT EVENT FAILED: " + arg0.toString());
                LOGGER.info("AUDIT FAIL REPORT: " + arg1);
            }
        };
    }

}
