package com.lookgraph.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "lookgraph.scanner")
public class ScannerProperties {

    private List<String> excludes = new ArrayList<>();
}
