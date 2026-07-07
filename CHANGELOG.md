# Changelog

## [0.0.1-beta] - 2026-06-14

### Added
- Initial beta release of IDaaS Akless AWS Adapter SDK
- Core provider `IDaaSPamAwsCredentialsProvider` with Builder pattern for obtaining AWS STS credentials via PAM Developer API
- AWS `AwsCredentialsProvider` adapter (`IDaaSPamAwsCredentialsProviderAdapter`) compatible with all AWS SDK v2 service clients
- Static factory `IDaaSPamAklessCredentialFactory` with `createAwsCredentialsProvider` methods
- STS credential caching with stale-before-expiry automatic refresh
- Automatic protocol prefix stripping for `developerApiEndpoint`
- Builder parameter validation (developerApiEndpoint, idaasInstanceId, credentialProvider, roleExternalId)
