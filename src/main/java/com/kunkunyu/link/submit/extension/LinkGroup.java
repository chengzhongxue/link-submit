package com.kunkunyu.link.submit.extension;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.util.LinkedHashSet;


@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "LinkGroup", plural = "linkgroups", singular = "linkgroup")
public class LinkGroup extends AbstractExtension {

    private LinkGroupSpec spec;

    @Data
    public static class LinkGroupSpec {
        @Schema(required = true)
        private String displayName;

        private Integer priority;
    }
}