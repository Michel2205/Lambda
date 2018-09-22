/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.api;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.entitygroup.monitoringresource.api.util.GlobalVariables;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Maximino Llovera
 */
public class S3Aws {

    private static AmazonS3 s3Client = null;
    private static final Logger logger = Logger.getLogger(S3Aws.class);

    public static AmazonS3 getAmazonS3Client() {
        //AmazonSNSClient snsClient;
        if (s3Client != null) {
            return s3Client;
        }
        s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(GlobalVariables.REGION).build();

        return s3Client;
    }

    public S3Aws() {
        s3Client = S3Aws.getAmazonS3Client();
    }

    public BufferedReader getObjectBufferReader(String bucketName, String key) {
        try {
            S3Object response = s3Client.getObject(new GetObjectRequest(bucketName, key));
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getObjectContent()));
            return br;
        } catch (AmazonClientException ex) {
            logger.error(ex);
            throw ex;
        }
    }

    public String getObjectFile(String bucketName, String key) {
        String fileName = "";
        //S3Object response = s3Client.getObject(new GetObjectRequest(bucketName, key));
        try {
            Path p = Paths.get(key);
            fileName = "/tmp/" + p.getFileName().toString();
            S3Object response = s3Client.getObject(new GetObjectRequest(bucketName, key));
            S3ObjectInputStream s3is = response.getObjectContent();
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
            return fileName;
        } catch (AmazonServiceException | IOException ex) {
            logger.error(ex);
        }
        return null;
    }

    public List<Bucket> listBuckets() {
        return s3Client.listBuckets();
    }

    public void uploadFile(String bucketName, String key, File file) {
        logger.info("bucketName:" + bucketName + ", key:" + key);
        s3Client.putObject(bucketName, key, file);
    }
}
