import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Sample: Use IDaaS PAM AKless authentication to access Amazon DynamoDB.
 *
 * <p>Prerequisites:
 * <ol>
 *   <li>Configure idaas-java-core-sdk (config file with scope "urn:cloud:idaas:pam|.all")</li>
 *   <li>Ensure the PAM role has AWS DynamoDB permissions</li>
 * </ol>
 */
public class AwsDynamoDbSample {

    public static void main(String[] args) {
        // 1. Initialize IDaaS Core SDK
        IDaaSCredentialProviderFactory.init();

        // 2. Create AWS credentials provider via factory
        AwsCredentialsProvider credentialsProvider =
                IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

        // 3. Create DynamoDB client with IDaaS credentials
        DynamoDbClient dynamoDb = DynamoDbClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();

        // 4. List all tables
        System.out.println("=== DynamoDB Tables ===");
        dynamoDb.listTables().tableNames().forEach(table ->
                System.out.printf("  %s%n", table)
        );

        // 5. Clean up
        dynamoDb.close();
    }
}
