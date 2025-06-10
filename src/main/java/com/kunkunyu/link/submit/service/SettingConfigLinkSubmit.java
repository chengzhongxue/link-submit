package com.kunkunyu.link.submit.service;

import lombok.Data;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SettingConfigLinkSubmit {

    Mono<BasicConfig> getBasicConfig();


    @Data
    class BasicConfig {
        public static final String GROUP = "basic";

        private boolean loadPlugInResources;

        private boolean displayTheSubmitButton;

        private boolean autoAudit;

        private boolean sendEmail;

        private String adminEmail;

        private String groupName;

        private List<String> forbidSelectedGroupName;

    }
}
