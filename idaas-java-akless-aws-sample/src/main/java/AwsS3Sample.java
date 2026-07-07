import com.cloud_idaas.adapter.aws.pam.IDaaSPamAklessCredentialFactory;
import com.cloud_idaas.core.factory.IDaaSCredentialProviderFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

/**
 * Sample: Use IDaaS PAM AKless authentication to access Amazon S3.
 *
 * <p>Prerequisites:
 * <ol>
 *   <li>Configure idaas-java-core-sdk (config file with scope "urn:cloud:idaas:pam|.all")</li>
 *   <li>Ensure the PAM role has AWS S3 permissions</li>
 * </ol>
 */
public class AwsS3Sample {

    public static void main(String[] args) {
        // 1. Initialize IDaaS Core SDK
        IDaaSCredentialProviderFactory.init();

        // 2. Create AWS credentials provider via factory
        AwsCredentialsProvider credentialsProvider =
                IDaaSPamAklessCredentialFactory.getAwsCredentialsProvider("your-role-arn");

        // 3. Create S3 client with IDaaS credentials
        S3Client s3 = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();

        // 4. List all S3 buckets
        System.out.println("=== S3 Buckets ===");
        ListBucketsResponse response = s3.listBuckets();
        for (Bucket bucket : response.buckets()) {
            System.out.printf("  %s (created: %s)%n", bucket.name(), bucket.creationDate());
        }
        System.out.printf("Total: %d buckets%n", response.buckets().size());

        // 5. Clean up
        s3.close();
    }
}
