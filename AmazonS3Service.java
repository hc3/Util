package br.com.selectcare.ws.model.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

@Service
public class AmazonS3Service {

    private static final String clientRegion = "";
    private static final String bucketName = "";
	private static final String accessKeyId = "";
	private static final String secretKey = "";
    private static final String regexMatch = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.jpg";
    
    public static String generatedUrl(String imageName, String bucketFolder) {
    	String returnedUrl = null;
        try {            
            AmazonS3 s3Client = buildConnection();
    
            // Set the presigned URL to expire after one hour.
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 60;
            expiration.setTime(expTimeMillis);

            // Generate the presigned URL.
            System.out.println("Generating pre-signed URL.");
            GeneratePresignedUrlRequest generatePresignedUrlRequest =  
                    new GeneratePresignedUrlRequest(bucketName+bucketFolder, imageName)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    
            returnedUrl =  url.toString();
        }
        catch(AmazonServiceException e) {
            e.printStackTrace();
        }
        catch(SdkClientException e) {
            e.printStackTrace();
        }
        
        return returnedUrl;
    }
    
	public static String uploadS3Image(String image, String imageName, String bucketFolder) throws IOException {
		
		String imageUrl = null;
		String keyName = null;
		
		AmazonS3 s3Client = buildConnection();
		
		try {
			
			if(imageName.matches(regexMatch)) {
				keyName = imageName;
			} else {				
				keyName = UUID.randomUUID().toString()+".jpg";
			}
			
			byte[] data = Base64.decodeBase64(image);
			
			InputStream input = new ByteArrayInputStream(data);
			
			ObjectMetadata objectMetadata = new ObjectMetadata();
			
			objectMetadata.setContentType("image/jpeg");
			
			PutObjectRequest request = new PutObjectRequest(bucketName+bucketFolder, keyName, input, objectMetadata);
			
			s3Client.putObject(request);
			
			imageUrl = keyName;

		} catch(AmazonServiceException e) {
            e.printStackTrace();
        }
        catch(SdkClientException e) {
            e.printStackTrace();
        }
		
		return imageUrl;
	}
	
	public static String downloadImageS3(String imageName, String bucketFolder) {
		
		String base64 = null;
		
		AmazonS3 s3Client = buildConnection();
		
		if(imageName != null) {			
			try {
				S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketName+bucketFolder, imageName));		
				BufferedImage imgBuf;
				imgBuf = ImageIO.read(s3Object.getObjectContent());
				base64 = encodeBase64URL(imgBuf);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	    return base64;

	}
	
	private static AmazonS3 buildConnection() {
		return AmazonS3ClientBuilder.standard()
	            .withRegion(clientRegion)
	            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey)))
	            .build();
	}
	
	private static String encodeBase64URL(BufferedImage imgBuf) throws IOException {
	    String base64;

	    if (imgBuf == null) {
	        base64 = null;
	    } else {
	        Base64 encoder = new Base64();
	        ByteArrayOutputStream out = new ByteArrayOutputStream();

	        ImageIO.write(imgBuf, "PNG", out);

	        byte[] bytes = out.toByteArray();
	        base64 = "data:image/png;base64," + new String(encoder.encode(bytes), "UTF-8");
	    }

	    return base64;
	}
	
}
