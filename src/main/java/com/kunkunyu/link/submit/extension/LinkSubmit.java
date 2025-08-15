package com.kunkunyu.link.submit.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static com.kunkunyu.link.submit.extension.LinkSubmit.KIND;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "link.submit.kunkunyu.com", version = "v1alpha1",
    kind = KIND, plural = "linksubmits", singular = "linksubmit")
@AllArgsConstructor
@NoArgsConstructor
public class LinkSubmit extends AbstractExtension {

    public static final String KIND = "LinkSubmit";

    public static final String REVIEW_DESCRIPTION = "link.submit.kunkunyu.com/review-description";



    @Schema(requiredMode = REQUIRED)
    private LinkSubmitSpec spec;

    @Data
    public static class LinkSubmitSpec {

        @Schema(required = true)
        private String url;

        @Schema(required = true)
        private String displayName;

        private String logo;

        private String description;

        private String oldUrl;

        private String email;

        private String groupName;

        private String rssUrl;

        @Schema(required = true)
        private LinkSubmitType type;

        @Schema(required = true)
        private LinkSubmitStatus status;
    }

    public static enum LinkSubmitType {
        add,
        update;
    }

    public static enum LinkSubmitStatus {
        review,
        pending,
        refuse;
    }


}
