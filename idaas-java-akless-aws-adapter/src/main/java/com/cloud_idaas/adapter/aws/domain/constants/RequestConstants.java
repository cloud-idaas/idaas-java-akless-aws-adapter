package com.cloud_idaas.adapter.aws.domain.constants;

/**
 * PAM Developer API request constants.
 */
public final class RequestConstants {

    private RequestConstants() {
    }

    /** API path template for obtaining access credentials. {@code %s} is the IDaaS instance ID. */
    public static final String OBTAIN_ACCESS_CREDENTIAL_PATH = "/v2/%s/cloudAccountRoles/_/actions/obtainAccessCredential";

    /** Query parameter key for the role external ID. */
    public static final String CLOUD_ACCOUNT_ROLE_EXTERNAL_ID = "cloudAccountRoleExternalId";
}
