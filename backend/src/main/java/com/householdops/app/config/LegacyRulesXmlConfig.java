package com.householdops.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:legacy/reorder-rules-context.xml")
public class LegacyRulesXmlConfig {
}
