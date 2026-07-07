# idaas-java-akless-aws-adapter

[![Java Version](https://img.shields.io/badge/java-8%2B-blue)](https://www.java.com/)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)
[![Development Status](https://img.shields.io/badge/status-Beta-orange)](https://github.com/cloud-idaas/idaas-java-akless-aws-adapter)
[![Version](https://img.shields.io/badge/version-0.0.1--beta-blue)](https://github.com/cloud-idaas/idaas-java-akless-aws-adapter)

[简体中文](README_zh.md) | English

Java SDK for IDaaS (Identity as a Service) AKless AWS Adapter — Enables AK-free authentication for AWS services using IDaaS PAM (Privileged Access Management) to obtain AWS STS temporary credentials.

## How It Works

```
┌──────────┐    OIDC Token    ┌──────────────┐   AWS STS Credentials   ┌─────────────┐
│  IDaaS   │ ──────────────►  │  PAM          │ ──────────────────────► │  AWS        │
│  Core    │                  │  Developer    │   (accessKeyId,         │  Service    │
│  SDK     │                  │  API          │    secretAccessKey,     │  (S3, etc.) │
└──────────┘                  └──────────────┘    sessionToken)         └─────────────┘
```

1. The IDaaS Core SDK obtains an **OIDC Token** via machine-to-machine authentication
2. This adapter sends the OIDC Token to the **PAM Developer API** to obtain AWS STS temporary credentials
3. The temporary credentials are used to authenticate with **AWS services** (S3, DynamoDB, Lambda, EC2, SQS, etc.)
4. Credentials are **automatically cached and refreshed** before expiration

## Features

- **AK-free Authentication**: Eliminates the need for long-term AWS AccessKey, uses OIDC Token to obtain AWS STS temporary credentials via IDaaS PAM, reducing the risk of credential leakage
- **AWS SDK Compatible**: Implements `AwsCredentialsProvider` interface, can be used directly with all AWS SDK v2 service clients (S3, DynamoDB, Lambda, EC2, SQS, etc.)
- **Automatic Credential Refresh**: Built-in credential caching with stale-time and prefetch-time based automatic refresh, ensuring seamless credential rotation
- **Simple Integration**: Factory class provides one-line creation of credential providers, minimizing integration effort

## Requirements

- Java >= 8
- Dependencies:
  - com.cloud-idaas:idaas-java-core-sdk >= 0.0.6-beta
  - software.amazon.awssdk:auth >= 2.25.0

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.cloud-idaas</groupId>
    <artifactId>idaas-java-akless-aws-adapter</artifactId>
    <version>0.0.1-beta</version>
</dependency>
```

## Prerequisites

This SDK depends on [idaas-java-core-sdk](https://github.com/cloud-idaas/idaas-java-core-sdk). You need to complete the IDaaS Core SDK initialization before using this adapter.

1. Add and configure `idaas-java-core-sdk`, refer to [idaas-java-core-sdk README](https://github.com/cloud-idaas/idaas-java-core-sdk/blob/main/README.md) for details.

2. In the configuration file, set the `scope` to the IDaaS built-in scope for PAM:

   ```json
   {
       "scope": "urn:cloud:idaas:pam|.all"
   }
   ```

3. Complete the IDaaS Core SDK initialization:

   ```java
   import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;

   IDaaSCredentialProviderFactory.init();
   ```

## Quick Start

The simplest way to use this SDK is through the `IDaaSPamAklessCredentialFactory` factory class:

```java
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

// 1. Initialize IDaaS Core SDK
IDaaSCredentialProviderFactory.init();

// 2. Create an AWS credentials provider
AwsCredentialsProvider credentialsProvider =
    IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

// 3. Use with any AWS service client
S3Client s3 = S3Client.builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .build();
```

## Usage Examples

### Amazon S3

```java
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

// Initialize
IDaaSCredentialProviderFactory.init();

// Create credentials provider
AwsCredentialsProvider credentialsProvider =
    IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

// Create S3 client and list buckets
S3Client s3 = S3Client.builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .build();

for (Bucket bucket : s3.listBuckets().buckets()) {
    System.out.println("Bucket: " + bucket.name());
}
```

### Amazon DynamoDB

```java
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

// Initialize
IDaaSCredentialProviderFactory.init();

// Create credentials provider
AwsCredentialsProvider credentialsProvider =
    IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

// Create DynamoDB client
DynamoDbClient dynamoDb = DynamoDbClient.builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .build();

dynamoDb.listTables().tableNames().forEach(table ->
    System.out.println("Table: " + table)
);
```

### Using Builder for Advanced Configuration

If you need to customize the endpoint, timeouts, or provide your own OIDC Token provider, use the builder pattern:

```java
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAwsCredentialsProvider;
import com.cloud_idaas.core.provider.OidcTokenProvider;

OidcTokenProvider oidcTokenProvider = // ... obtain from Core SDK

IDaaSPamAwsCredentialsProvider provider = IDaaSPamAwsCredentialsProvider.builder()
    .developerApiEndpoint("https://your-pam-endpoint.example.com")
    .idaasInstanceId("your-instance-id")
    .oidcTokenProvider(oidcTokenProvider)
    .roleArn("your-role-arn")
    .connectionTimeout(3000)
    .readTimeout(8000)
    .build();

// Use directly as AwsCredentialsProvider
S3Client s3 = S3Client.builder()
    .credentialsProvider(provider)
    .region(Region.US_EAST_1)
    .build();
```

## API Reference

### IDaaSPamAklessCredentialFactory

Factory class providing a static method to create credential providers. Automatically reads configuration from the Core SDK.

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAwsCredentialsProvider(String roleArn)` | `IDaaSPamAwsCredentialsProvider` | Creates a provider using Core SDK Factory configuration |

### IDaaSPamAwsCredentialsProvider

Core credentials provider that implements `AwsCredentialsProvider`. Obtains AWS STS temporary credentials from the PAM Developer API using an OIDC Token, with automatic caching and refresh.

#### Builder Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| developerApiEndpoint | String | Yes | - | PAM Developer API endpoint |
| idaasInstanceId | String | Yes | - | IDaaS instance ID |
| oidcTokenProvider | OidcTokenProvider | Yes | - | OIDC Token provider from Core SDK |
| roleArn | String | Yes | - | Cloud Account Role ARN |
| connectionTimeout | Integer | No | 5000 | Connection timeout in milliseconds |
| readTimeout | Integer | No | 10000 | Read timeout in milliseconds |

#### Key Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `resolveCredentials()` | `AwsCredentials` | Returns cached `AwsSessionCredentials` (includes `accessKeyId`, `secretAccessKey`, `sessionToken`, `expirationTime`), auto-refreshes if expired |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `CLOUD_IDAAS_CONFIG_PATH` | Path to the IDaaS configuration file. Defaults to `cloud_idaas.json` in the classpath |
| `IDAAS_CLIENT_SECRET` | Client secret for IDaaS authentication. Recommended over storing secrets in the config file |

## Support and Feedback

- **Email**: cloudidaas@list.alibaba-inc.com
- **Issues**: [Submit an Issue](https://github.com/cloud-idaas/idaas-java-akless-aws-adapter/issues)

## License

This project is licensed under the [Apache License 2.0](LICENSE).
