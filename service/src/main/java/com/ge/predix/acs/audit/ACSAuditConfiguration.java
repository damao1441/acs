package com.ge.predix.acs.audit;

import java.util.List;

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
        } catch (VcapLoadException e) {
            sdkConfig = AuditConfiguration.builder().bulkMode(true).clientType(AuditClientType.ASYNC).uaaUrl(uaaUrl)
                    .uaaClientId(uaaClientId).uaaClientSecret(uaaClientSecret).ehubZoneId(ehubZoneId).ehubHost(ehubHost)
                    .ehubPort(ehubPort).build();
        }
        AuditClient auditClient = new AuditClient(sdkConfig, auditCallback());
        return auditClient;
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
