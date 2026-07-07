package com.cloud_idaas.adapter.aws.domain.constants;

/**
 * Credential field name constants for AWS STS token responses from the PAM Developer API.
 */
public final class CredentialConstants {

    private CredentialConstants() {
    }

    /** Top-level key for the cloud account role access credential in the response JSON. */
    public static final String CLOUD_ACCOUNT_ROLE_ACCESS_CREDENTIAL = "cloudAccountRoleAccessCredential";

    /** Key for the AWS STS Token object within the credential structure. */
    public static final String AWS_STS_TOKEN = "awsStsToken";

    /** Key for the Access Key ID in the STS Token. */
    public static final String AWS_ACCESS_KEY_ID = "accessKeyId";

    /** Key for the Secret Access Key in the STS Token. */
    public static final String AWS_SECRET_ACCESS_KEY = "secretAccessKey";

    /** Key for the Session Token in the STS Token. */
    public static final String AWS_SESSION_TOKEN = "sessionToken";

    /** Key for the expiration time in the STS Token. */
    public static final String AWS_EXPIRATION = "expiration";
}
