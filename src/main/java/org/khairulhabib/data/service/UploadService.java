package org.khairulhabib.data.service;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class UploadService {

    private WebClient wc;
    private static String PRESIGN_BASE_URL = "https://us-south.functions.appdomain.cloud";
    
    public void upload(IcosConfig config, MemoryBuffer buffer){
        wc = WebClient.builder()
                .baseUrl(PRESIGN_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        Map<String,String> params = new HashMap<>();
        params.put("access_key", config.getAccessKey());
        params.put("secret_key", config.getSecretKey());
        params.put("region", config.getRegion());
        params.put("bucket", config.getBucket());
        params.put("object_key", config.getObjectKey());
        ObjectMapper mapper = new ObjectMapper();
        try{
            String requestJson = mapper.writeValueAsString(params);
            String strResponse = wc
                .post()
                .uri("/api/v1/web/7fd17f8c-4a89-4d08-9529-f9aa7737c52d/default/icos-python.json")
                .body(BodyInserters.fromValue(requestJson))
                .exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(String.class))
                .block();
            System.out.println(strResponse);
            PresignedResponse nextUrl = new ObjectMapper().readValue(strResponse,PresignedResponse.class);
            System.out.println(nextUrl.getPresign_url());

            WebClient uploadClient = WebClient.builder()
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                        .build();
            String responseUpload = uploadClient.put()
                .uri(URI.create(nextUrl.getPresign_url()))
                .body(BodyInserters.fromValue(IOUtils.toByteArray(buffer.getInputStream())))
                .exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(String.class))
                .block();
            System.out.println("Result >> " + responseUpload);
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }

    public MultiValueMap<String, HttpEntity<?>> fromFile(byte[] file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file);
        return builder.build();
    }
}
