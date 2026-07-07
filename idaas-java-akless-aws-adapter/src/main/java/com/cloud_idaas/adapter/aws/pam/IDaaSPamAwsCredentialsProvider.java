package com.cloud_idaas.adapter.aws.pam;

import com.cloud_idaas.adapter.aws.domain.constants.CredentialConstants;
import com.cloud_idaas.adapter.aws.domain.constants.RequestConstants;
import com.cloud_idaas.core.cache.RefreshResult;
import com.cloud_idaas.core.domain.constants.HttpConstants;
import com.cloud_idaas.core.exception.CredentialException;
import com.cloud_idaas.core.http.HttpClient;
import com.cloud_idaas.core.http.HttpClientFactory;
import com.cloud_idaas.core.http.HttpMethod;
import com.cloud_idaas.core.http.HttpRequest;
import com.cloud_idaas.core.http.HttpResponse;
import com.cloud_idaas.core.implementation.AbstractRefreshedCredentialProvider;
import com.cloud_idaas.core.provider.OidcTokenProvider;
import com.cloud_idaas.core.util.RequestUtil;
import com.google.gson.Gson;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IDaaS PAM AWS credentials provider.
 *
 * <p>Requests AWS STS temporary credentials from the PAM Developer API using an
 * OIDC Token, with automatic caching and expiration-based refresh backed by
 * {@link AbstractRefreshedCredentialProvider}. Implements {@link AwsCredentialsProvider}
 * so it can be used directly with all AWS service clients.
 */
public class IDaaSPamAwsCredentialsProvider
        extends AbstractRefreshedCredentialProvider<AwsSessionCredentials>
        implements AwsCredentialsProvider {

    private static final Gson GSON = new Gson();

    private final String roleArn;

    // Volatile: updated during refresh which may be triggered from different threads.
    private volatile String oidcToken;
    private final OidcTokenProvider oidcTokenProvider;

    /** Connection timeout in milliseconds. */
    private final int connectTimeout;
    /** Read timeout in milliseconds. */
    private final int readTimeout;

    private final String idaasInstanceId;
    private String developerApiEndpoint;
    private String developerApiPath;

    private IDaaSPamAwsCredentialsProvider(BuilderImpl builder) {
        super(builder);

        if (builder.developerApiEndpoint == null || builder.developerApiEndpoint.isEmpty()) {
            throw new IllegalArgumentException("developerApiEndpoint cannot be empty.");
        }
        if (builder.idaasInstanceId == null || builder.idaasInstanceId.isEmpty()) {
            throw new IllegalArgumentException("idaasInstanceId cannot be empty.");
        }
        if (builder.oidcTokenProvider == null) {
            throw new IllegalArgumentException("oidcTokenProvider cannot be null.");
        }
        if (builder.roleArn == null || builder.roleArn.isEmpty()) {
            throw new IllegalArgumentException("roleArn cannot be empty.");
        }

        this.roleArn = builder.roleArn;
        this.oidcTokenProvider = builder.oidcTokenProvider;
        this.connectTimeout = builder.connectionTimeout == null ? 5000 : builder.connectionTimeout;
        this.readTimeout = builder.readTimeout == null ? 10000 : builder.readTimeout;
        this.idaasInstanceId = builder.idaasInstanceId;

        this.developerApiEndpoint = builder.developerApiEndpoint;

        this.developerApiPath = String.format(RequestConstants.OBTAIN_ACCESS_CREDENTIAL_PATH, this.idaasInstanceId);
    }

    /**
     * Creates a new builder for {@link IDaaSPamAwsCredentialsProvider}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Resolves AWS session credentials.
     *
     * <p>Returns cached credentials if they have not expired; otherwise
     * triggers an automatic refresh from the PAM Developer API.
     *
     * @return AWS session credentials containing accessKeyId, secretAccessKey,
     *         sessionToken, and expirationTime
     */
    @Override
    public AwsCredentials resolveCredentials() {
        return this.getCachedResultSupplier().get();
    }

    @Override
    public RefreshResult<AwsSessionCredentials> refreshCredential() {
        HttpClient client = HttpClientFactory.getDefaultHttpClient();
        return getNewSessionCredentials(client);
    }

    @SuppressWarnings("unchecked")
    RefreshResult<AwsSessionCredentials> getNewSessionCredentials(HttpClient client) {
        String token = oidcTokenProvider.getOidcToken();
        this.oidcToken = token;
        Map<String, String> queries = new HashMap<>();
        queries.put(RequestConstants.CLOUD_ACCOUNT_ROLE_EXTERNAL_ID, this.roleArn);
        String url = RequestUtil.composeUrl(this.developerApiEndpoint, this.developerApiPath, queries);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(HttpConstants.AUTHORIZATION_HEADER, Collections.singletonList(HttpConstants.BEARER + HttpConstants.SPACE + token));
        HttpRequest httpRequest = new HttpRequest.Builder()
                .httpMethod(HttpMethod.GET)
                .url(url)
                .headers(headers)
                .build();
        HttpResponse httpResponse = client.send(httpRequest);
        if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() >= 300) {
            throw new CredentialException(String.format(
                    "PAM Developer API returned HTTP %d: %s.", httpResponse.getStatusCode(), httpResponse.getBody()));
        }
        Map<String, Object> map = GSON.fromJson(httpResponse.getBody(), Map.class);
        if (null == map || !map.containsKey(CredentialConstants.CLOUD_ACCOUNT_ROLE_ACCESS_CREDENTIAL)) {
            throw new CredentialException(String.format("Error retrieving credentials from PAM result: %s.", httpResponse.getBody()));
        }
        Map<String, Object> result = (Map<String, Object>) map.get(CredentialConstants.CLOUD_ACCOUNT_ROLE_ACCESS_CREDENTIAL);
        if (null == result || !result.containsKey(CredentialConstants.AWS_STS_TOKEN)) {
            throw new CredentialException(String.format("Error retrieving credentials from PAM result: %s.", httpResponse.getBody()));
        }
        Map<String, String> stsResult = (Map<String, String>) result.get(CredentialConstants.AWS_STS_TOKEN);
        if (null == stsResult || !stsResult.containsKey(CredentialConstants.AWS_ACCESS_KEY_ID)
                || !stsResult.containsKey(CredentialConstants.AWS_SECRET_ACCESS_KEY)
                || !stsResult.containsKey(CredentialConstants.AWS_SESSION_TOKEN)
                || !stsResult.containsKey(CredentialConstants.AWS_EXPIRATION)) {
            throw new CredentialException(String.format("Error retrieving credentials from PAM result: %s.", httpResponse.getBody()));
        }
        long expirationMillis = RequestUtil.getUTCDate(stsResult.get(CredentialConstants.AWS_EXPIRATION)).getTime();
        long expirationSecs = expirationMillis / 1000;
        long nowSecs = System.currentTimeMillis() / 1000;
        long expiresIn = expirationSecs - nowSecs;

        AwsSessionCredentials credential = AwsSessionCredentials.builder()
                .accessKeyId(stsResult.get(CredentialConstants.AWS_ACCESS_KEY_ID))
                .secretAccessKey(stsResult.get(CredentialConstants.AWS_SECRET_ACCESS_KEY))
                .sessionToken(stsResult.get(CredentialConstants.AWS_SESSION_TOKEN))
                .expirationTime(Instant.ofEpochMilli(expirationMillis))
                .build();

        // staleTime: 4/5 of lifetime, prefetchTime: 2/3 of lifetime
        Instant staleTime = Instant.ofEpochSecond(expirationSecs - expiresIn / 5);
        Instant prefetchTime = Instant.ofEpochSecond(expirationSecs - expiresIn / 3);

        return RefreshResult.builder(credential)
                .staleTime(staleTime)
                .prefetchTime(prefetchTime)
                .build();
    }

    public String getIdaasInstanceId() {
        return idaasInstanceId;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public String getOIDCToken() {
        return oidcToken;
    }

    public OidcTokenProvider getOidcTokenProvider() {
        return oidcTokenProvider;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String getDeveloperApiEndpoint() {
        return developerApiEndpoint;
    }

    public String getDeveloperApiPath() {
        return developerApiPath;
    }

    /**
     * Builder for {@link IDaaSPamAwsCredentialsProvider}.
     */
    public interface Builder
            extends AbstractRefreshedCredentialProvider.Builder<IDaaSPamAwsCredentialsProvider, Builder> {

        Builder developerApiEndpoint(String developerApiEndpoint);

        Builder idaasInstanceId(String idaasInstanceId);

        Builder oidcTokenProvider(OidcTokenProvider oidcTokenProvider);

        Builder roleArn(String roleArn);

        Builder connectionTimeout(Integer connectionTimeout);

        Builder readTimeout(Integer readTimeout);

        @Override
        IDaaSPamAwsCredentialsProvider build();
    }

    private static final class BuilderImpl
            extends AbstractRefreshedCredentialProvider.BuilderImpl<IDaaSPamAwsCredentialsProvider, Builder>
            implements Builder {

        private String developerApiEndpoint;
        private String idaasInstanceId;
        private OidcTokenProvider oidcTokenProvider;
        private String roleArn;
        private Integer connectionTimeout;
        private Integer readTimeout;

        @Override
        public Builder developerApiEndpoint(String developerApiEndpoint) {
            this.developerApiEndpoint = developerApiEndpoint;
            return this;
        }

        @Override
        public Builder idaasInstanceId(String idaasInstanceId) {
            this.idaasInstanceId = idaasInstanceId;
            return this;
        }

        @Override
        public Builder oidcTokenProvider(OidcTokenProvider oidcTokenProvider) {
            this.oidcTokenProvider = oidcTokenProvider;
            return this;
        }

        @Override
        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        @Override
        public Builder connectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        @Override
        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        @Override
        public IDaaSPamAwsCredentialsProvider build() {
            return new IDaaSPamAwsCredentialsProvider(this);
        }
    }
}
