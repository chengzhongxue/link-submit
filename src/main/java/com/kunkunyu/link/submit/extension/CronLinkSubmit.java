package com.kunkunyu.link.submit.extension;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;
import java.util.List;

import static com.kunkunyu.link.submit.extension.CronLinkSubmit.KIND;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "link.submit.kunkunyu.com",
    version = "v1alpha1",
    kind = KIND,
    singular = "cronlinksubmit",
    plural = "cronlinksubmits"
)
public class CronLinkSubmit extends AbstractExtension {

    public static final String KIND = "CronLinkSubmit";


    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private CronLinkSubmitSpec spec;

    private CronLinkSubmitStatus status = new CronLinkSubmitStatus();

    @Data
    public static class CronLinkSubmitSpec {
        private String cron;
        private boolean suspend;
        private CleanConfig cleanConfig = new CleanConfig();

    }

    @Data
    public static class CleanConfig {
        private String type;
        private List<String> withoutCheckGroupNames;
        private String moveGroupName;

    }

    @Data
    public static class CronLinkSubmitStatus {
        private Instant lastScheduledTimestamp;
        private Instant nextSchedulingTimestamp;

    }

}
