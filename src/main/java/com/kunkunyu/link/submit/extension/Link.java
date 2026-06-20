package com.kunkunyu.link.submit.extension;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "Link", plural = "links", singular = "link")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Link extends AbstractExtension {

    private LinkSpec spec;

    private Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LinkSpec {
        @Schema(required = true)
        private String url;

        @Schema(required = true)
        private String displayName;

        private String logo;

        private String description;

        private Integer priority;

        private String groupName;

        private Map<String, Object> additionalProperties = new LinkedHashMap<>();

        @JsonAnySetter
        public void setAdditionalProperty(String key, Object value) {
            this.additionalProperties.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }
    }
}
