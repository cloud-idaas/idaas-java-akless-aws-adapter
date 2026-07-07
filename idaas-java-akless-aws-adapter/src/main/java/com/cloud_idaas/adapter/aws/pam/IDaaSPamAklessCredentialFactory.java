package com.cloud_idaas.adapter.aws.pam;

import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;

/**
 * Factory for creating AWS credentials providers with IDaaS PAM AKless authentication.
 *
 * <p>Provides a convenient static method that creates an {@link IDaaSPamAwsCredentialsProvider}
 * backed by IDaaS PAM, enabling AK-free (AccessKey-less) access to AWS services.
 */
public class IDaaSPamAklessCredentialFactory {

    private IDaaSPamAklessCredentialFactory() {
    }

    /**
     * Creates an AWS credentials provider using configuration from the Core SDK Factory.
     *
     * <p>The Developer API endpoint, IDaaS instance ID, and OIDC Token provider are
     * automatically obtained from {@link IDaaSCredentialProviderFactory}.
     *
     * @param roleArn Cloud Account Role ARN
     * @return an {@link IDaaSPamAwsCredentialsProvider} backed by IDaaS PAM
     */
    public static IDaaSPamAwsCredentialsProvider getAwsCredentialsProvider(String roleArn) {
        return IDaaSPamAwsCredentialsProvider.builder()
                .oidcTokenProvider(IDaaSCredentialProviderFactory.getIDaaSCredentialProvider())
                .developerApiEndpoint(IDaaSCredentialProviderFactory.getDeveloperApiEndpoint())
                .idaasInstanceId(IDaaSCredentialProviderFactory.getIDaasInstanceId())
                .roleArn(roleArn)
                .build();
    }
}
