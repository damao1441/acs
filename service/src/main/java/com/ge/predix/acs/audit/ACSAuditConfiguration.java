package com.ge.predix.acs.audit;

import java.util.List;

import org.slf4j.LoggerFactory;
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
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.eventhub.EventHubClientException;

@Configuration
@Profile("predix")
public class ACSAuditConfiguration {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ACSAuditConfiguration.class);

    private VcapLoaderServiceImpl vcapLoaderService = new VcapLoaderServiceImpl();

    private AuditConfiguration sdkConfig;

    private String uaaUrl;

    private String uaaClientId;

    private String uaaClientSecret;

    private String ehubZoneId;

    private String ehubHost;

    private int ehubPort;

    @Bean
    public AuditClient createAuditClient() throws AuditException, EventHubClientException {
        try {
            sdkConfig = vcapLoaderService.getConfigFromVcap();
            sdkConfig.setUaaClientId("acs-audit-client");
            sdkConfig.setUaaClientSecret("acs-audit-secret");
            sdkConfig.setUaaUrl("https://predix-uaa.run.aws-usw02-dev.ice.predix.io/oauth/token");
            sdkConfig.setClientType(AuditClientType.ASYNC);
            System.out.println(System.getenv("AUDIT_UAA_CLIENT_SECRET"));
            System.out.println("START HERE");
            System.out.println(sdkConfig.getUaaClientId());
            System.out.println(sdkConfig.getUaaClientSecret());
            System.out.println(sdkConfig.getUaaUrl());
            System.out.println(sdkConfig.getEhubZoneId());
            System.out.println(sdkConfig.getEhubHost());
            System.out.println(sdkConfig.getEhubPort());
        } catch (VcapLoadException e) {
            e.printStackTrace();
        }
        AuditClient auditClient = new AuditClient(sdkConfig, auditCallback());
        return auditClient;
    }

    public AuditCallback auditCallback() {
        return new AuditCallback() {
            @Override
            public void onFailure(final com.ge.predix.audit.sdk.message.AuditEvent arg0, final FailReport arg1,
                    final String arg2) {
                LOGGER.info("AUDIT EVENT FAILED: " + arg0.toString());
                LOGGER.info("AUDIT FAIL REPORT: " + arg1.toString());
            }

            @Override
            public void onSuccees(final com.ge.predix.audit.sdk.message.AuditEvent arg0) {
                LOGGER.info("AUDIT EVENT SUCCESS: " + arg0.toString());
            }

            @Override
            public void onValidate(final com.ge.predix.audit.sdk.message.AuditEvent arg0,
                    final List<ValidatorReport> arg1) {
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
