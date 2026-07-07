# idaas-java-akless-aws-adapter

[![Java Version](https://img.shields.io/badge/java-8%2B-blue)](https://www.java.com/)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)
[![Development Status](https://img.shields.io/badge/status-Beta-orange)](https://github.com/cloud-idaas/idaas-java-akless-aws-adapter)
[![Version](https://img.shields.io/badge/version-0.0.1--beta-blue)](https://github.com/cloud-idaas/idaas-java-akless-aws-adapter)

简体中文 | [English](README.md)

IDaaS（身份即服务）AKless AWS 适配器的 Java SDK —— 通过 IDaaS PAM（特权访问管理）获取 AWS STS 临时凭证，实现无 AccessKey 访问 AWS 服务。

## 工作原理

```
┌──────────┐    OIDC Token    ┌──────────────┐   AWS STS 临时凭证       ┌─────────────┐
│  IDaaS   │ ──────────────►  │  PAM          │ ──────────────────────► │  AWS        │
│  Core    │                  │  Developer    │   (accessKeyId,         │  服务       │
│  SDK     │                  │  API          │    secretAccessKey,     │  (S3 等)    │
└──────────┘                  └──────────────┘    sessionToken)         └─────────────┘
```

1. IDaaS Core SDK 通过机器对机器认证获取 **OIDC Token**
2. 本适配器将 OIDC Token 发送到 **PAM Developer API** 获取 AWS STS 临时凭证
3. 临时凭证用于 **AWS 服务** 认证（S3、DynamoDB、Lambda、EC2、SQS 等）
4. 凭证在过期前 **自动缓存和刷新**

## 功能特性

- **免 AK 认证**：无需长期 AWS AccessKey，使用 OIDC Token 通过 IDaaS PAM 获取 AWS STS 临时凭证，降低凭证泄露风险
- **AWS SDK 兼容**：实现 `AwsCredentialsProvider` 接口，可直接用于所有 AWS SDK v2 服务客户端（S3、DynamoDB、Lambda、EC2、SQS 等）
- **自动凭证刷新**：内置凭证缓存，基于过期时间自动刷新（支持 staleTime 和 prefetchTime 机制），确保凭证无缝轮转
- **简单集成**：工厂类提供一行代码创建凭证提供器，最大程度降低集成成本

## 环境要求

- Java >= 8
- 依赖：
  - com.cloud-idaas:idaas-java-core-sdk >= 0.0.6-beta
  - software.amazon.awssdk:auth >= 2.25.0

## 安装

在 `pom.xml` 中添加以下依赖：

```xml
<dependency>
    <groupId>com.cloud-idaas</groupId>
    <artifactId>idaas-java-akless-aws-adapter</artifactId>
    <version>0.0.1-beta</version>
</dependency>
```

## 前置准备

本 SDK 依赖 [idaas-java-core-sdk](https://github.com/cloud-idaas/idaas-java-core-sdk)，使用本适配器前需要先完成 IDaaS Core SDK 的初始化。

1. 添加并配置 `idaas-java-core-sdk`，详情参见 [idaas-java-core-sdk README](https://github.com/cloud-idaas/idaas-java-core-sdk/blob/main/README_zh.md)。

2. 在配置文件中，将 `scope` 设置为 IDaaS PAM 内置 scope：

   ```json
   {
       "scope": "urn:cloud:idaas:pam|.all"
   }
   ```

3. 完成 IDaaS Core SDK 初始化：

   ```java
   import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;

   IDaaSCredentialProviderFactory.init();
   ```

## 快速开始

最简单的使用方式是通过 `IDaaSPamAklessCredentialFactory` 工厂类：

```java
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

// 1. 初始化 IDaaS Core SDK
IDaaSCredentialProviderFactory.init();

// 2. 创建 AWS 凭证提供器
AwsCredentialsProvider credentialsProvider =
    IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

// 3. 配合任意 AWS 服务客户端使用
S3Client s3 = S3Client.builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .build();
```

## 使用示例

### Amazon S3

```java
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

// 初始化
IDaaSCredentialProviderFactory.init();

// 创建凭证提供器
AwsCredentialsProvider credentialsProvider =
    IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

// 创建 S3 客户端并列出存储桶
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

// 初始化
IDaaSCredentialProviderFactory.init();

// 创建凭证提供器
AwsCredentialsProvider credentialsProvider =
    IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

// 创建 DynamoDB 客户端
DynamoDbClient dynamoDb = DynamoDbClient.builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .build();

dynamoDb.listTables().tableNames().forEach(table ->
    System.out.println("Table: " + table)
);
```

### 使用 Builder 进行高级配置

如果需要自定义端点、超时时间或使用自定义的 OIDC Token 提供器，可以使用 Builder 模式：

```java
import com.cloud_idaas.adapter.aws.pam.IDaaSPamAwsCredentialsProvider;
import com.cloud_idaas.core.provider.OidcTokenProvider;

OidcTokenProvider oidcTokenProvider = // ... 从 Core SDK 获取

IDaaSPamAwsCredentialsProvider provider = IDaaSPamAwsCredentialsProvider.builder()
    .developerApiEndpoint("https://your-pam-endpoint.example.com")
    .idaasInstanceId("your-instance-id")
    .oidcTokenProvider(oidcTokenProvider)
    .roleArn("your-role-arn")
    .connectionTimeout(3000)
    .readTimeout(8000)
    .build();

// 直接作为 AwsCredentialsProvider 使用
S3Client s3 = S3Client.builder()
    .credentialsProvider(provider)
    .region(Region.US_EAST_1)
    .build();
```

## API 参考

### IDaaSPamAklessCredentialFactory

工厂类，自动从 Core SDK 读取配置。

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getAwsCredentialsProvider(String roleArn)` | `IDaaSPamAwsCredentialsProvider` | 使用 Core SDK 工厂配置创建凭证提供器 |

### IDaaSPamAwsCredentialsProvider

核心凭证提供器，实现 `AwsCredentialsProvider` 接口。使用 OIDC Token 从 PAM Developer API 获取 AWS STS 临时凭证，自动缓存和刷新。

#### Builder 参数

| 参数 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| developerApiEndpoint | String | 是 | - | PAM Developer API 端点 |
| idaasInstanceId | String | 是 | - | IDaaS 实例 ID |
| oidcTokenProvider | OidcTokenProvider | 是 | - | 来自 Core SDK 的 OIDC Token 提供器 |
| roleArn | String | 是 | - | 云账号角色 ARN |
| connectionTimeout | Integer | 否 | 5000 | 连接超时时间（毫秒） |
| readTimeout | Integer | 否 | 10000 | 读取超时时间（毫秒） |

#### 核心方法

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `resolveCredentials()` | `AwsCredentials` | 返回缓存的 `AwsSessionCredentials`（包含 `accessKeyId`、`secretAccessKey`、`sessionToken`、`expirationTime`），过期时自动刷新 |

## 环境变量

| 变量 | 描述 |
|------|------|
| `CLOUD_IDAAS_CONFIG_PATH` | IDaaS 配置文件路径，默认从 classpath 中加载 `cloud_idaas.json` |
| `IDAAS_CLIENT_SECRET` | IDaaS 认证的客户端密钥，建议通过环境变量传递而非写入配置文件 |

## 支持与反馈

- **邮箱**：cloudidaas@list.alibaba-inc.com
- **问题反馈**：[提交 Issue](https://github.com/cloud-idaas/idaas-java-akless-aws-adapter/issues)

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可证授权。
