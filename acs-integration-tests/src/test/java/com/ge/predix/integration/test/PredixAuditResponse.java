package com.ge.predix.integration.test;

import java.util.Set;

public class PredixAuditResponse {

    private Set<AuditRecord> content;

    public Set<AuditRecord> getContent() {
        return content;
    }

    public void setContent(final Set<AuditRecord> content) {
        this.content = content;
    }
    
}



