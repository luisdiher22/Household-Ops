package com.householdops.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/** Bridges the classic-XML-defined ReorderRulesEngine bean into this Boot app's context -- see reorder-rules-context.xml for why it's XML in the first place. */
@Configuration
@ImportResource("classpath:legacy/reorder-rules-context.xml")
public class LegacyRulesXmlConfig {
}
