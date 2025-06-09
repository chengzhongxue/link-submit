package com.kunkunyu.link.submit.vo;

import com.kunkunyu.link.submit.extension.LinkGroup;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkGroupVo {

    private String displayName;
    private String groupName;
    private Integer priority;

    public static LinkGroupVo from(LinkGroup linkGroup) {
        return LinkGroupVo.builder()
            .displayName(linkGroup.getSpec().getDisplayName())
            .groupName(linkGroup.getMetadata().getName())
            .priority(linkGroup.getSpec().getPriority())
            .build();
    }
}
