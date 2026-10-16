package com.clockwork.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;
    private final String localFallbackDir;

    public StorageService(@org.springframework.beans.factory.annotation.Autowired(required = false) S3Client s3Client,
                          @Value("${app.storage.local-fallback-dir:./storage-data}") String localFallbackDir) {
        this.s3Client = s3Client;
        this.localFallbackDir = localFallbackDir;
        ensureLocalDirExists();
    }

    private void ensureLocalDirExists() {
        try {
            Files.createDirectories(Paths.get(localFallbackDir));
        } catch (IOException e) {
            log.warn("Could not create local storage fallback directory: {}", e.getMessage());
        }
    }

    public String uploadBytes(String bucket, String key, byte[] content, String contentType) {
        if (s3Client != null) {
            try {
                ensureBucketExists(bucket);
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(putRequest, RequestBody.fromBytes(content));
                log.info("Successfully uploaded {} bytes to S3/MinIO bucket {}/{}", content.length, bucket, key);
                return "s3://" + bucket + "/" + key;
            } catch (Exception e) {
                log.warn("S3 upload failed ({}), falling back to local storage: {}", e.getMessage(), key);
            }
        }

        // Local filesystem fallback
        try {
            Path targetPath = Paths.get(localFallbackDir, bucket, key);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
            log.info("Saved {} bytes to local storage fallback at {}", content.length, targetPath.toAbsolutePath());
            return "file://" + targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist file locally: " + key, e);
        }
    }

    public int pruneSnapshots(String bucket, String prefix, int retentionCount) {
        log.info("Pruning snapshots in bucket '{}' with prefix '{}', retaining last {}", bucket, prefix, retentionCount);
        if (s3Client != null) {
            try {
                ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build();
                ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
                List<S3Object> objects = listRes.contents().stream()
                        .sorted(Comparator.comparing(S3Object::lastModified).reversed())
                        .collect(Collectors.toList());

                if (objects.size() > retentionCount) {
                    List<S3Object> toDelete = objects.subList(retentionCount, objects.size());
                    List<ObjectIdentifier> identifiers = toDelete.stream()
                            .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                            .collect(Collectors.toList());

                    DeleteObjectsRequest deleteReq = DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(identifiers).build())
                            .build();
                    s3Client.deleteObjects(deleteReq);
                    log.info("Pruned {} old snapshots from MinIO/S3 bucket '{}'", toDelete.size(), bucket);
                    return toDelete.size();
                }
                return 0;
            } catch (Exception e) {
                log.warn("Failed to prune via S3 client ({}), falling back to local check", e.getMessage());
            }
        }

        // Local fallback pruning
        try {
            Path dir = Paths.get(localFallbackDir, bucket, prefix);
            if (Files.exists(dir)) {
                List<Path> files = Files.list(dir)
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                        .collect(Collectors.toList());

                if (files.size() > retentionCount) {
                    List<Path> toDelete = files.subList(retentionCount, files.size());
                    for (Path p : toDelete) {
                        Files.deleteIfExists(p);
                    }
                    log.info("Pruned {} old snapshots locally from {}", toDelete.size(), dir);
                    return toDelete.size();
                }
            }
        } catch (IOException e) {
            log.warn("Error during local snapshot pruning: {}", e.getMessage());
        }
        return 0;
    }

    private void ensureBucketExists(String bucket) {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder().bucket(bucket).build();
            s3Client.headBucket(headRequest);
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' does not exist. Creating it...", bucket);
            CreateBucketRequest createRequest = CreateBucketRequest.builder().bucket(bucket).build();
            s3Client.createBucket(createRequest);
        } catch (Exception e) {
            // Bucket might exist or server doesn't support head
            log.debug("HeadBucket check response for {}: {}", bucket, e.getMessage());
        }
    }
}
