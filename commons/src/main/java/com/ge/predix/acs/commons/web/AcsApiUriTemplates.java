/*******************************************************************************
 * Copyright 2016 General Electric Company. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.acs.commons.web;

/**
 * Constants that define the set of URI Templates of the resources exposed by the ACS.
 *
 * @author 212360328
 */
@SuppressWarnings({ "nls" })
public final class AcsApiUriTemplates {

    private AcsApiUriTemplates() {
    }

    public static final String V1 = "/v1";

    public static final String TENANT_URL_BASE = "/tenant/{tenantId}";

    public static final String POLICY_SETS_URL = "/policy-set";

    public static final String POLICY_SET_URL = POLICY_SETS_URL + "/{policySetId}";

    public static final String POLICY_DELETE_URL = POLICY_SETS_URL + "/{policySetId}";

    public static final String MANAGED_RESOURCES_URL = "/resource";

    public static final String MANAGED_RESOURCE_URL = MANAGED_RESOURCES_URL + "/{resourceIdentifier}";

    public static final String SUBJECTS_URL = "/subject";

    public static final String SUBJECT_URL = SUBJECTS_URL + "/{subjectIdentifier:.+}";

    public static final String ISSUERS_URL = "/issuer";

    public static final String ISSUER_URL = ISSUERS_URL + "/{issuerId}";

    public static final String MONITORING_URL = "/monitoring";

    public static final String HEARTBEAT_URL = MONITORING_URL + "/heartbeat";

    public static final String ZONE_URL = "/zone/{zoneName}";

    public static final String ZONECLIENT_URL = "/client";
    
    public static final String RESOURCE_CONNECTOR_URL = "/connector/resource";
}
