package org.khairulhabib.data.service;

import lombok.Getter;
import lombok.Setter;

public class IcosConfig {

    @Setter @Getter private String accessKey;
    @Setter @Getter private String secretKey;
    @Setter @Getter private String region;
    @Setter @Getter private String bucket;
    @Setter @Getter private String objectKey;
    
}
