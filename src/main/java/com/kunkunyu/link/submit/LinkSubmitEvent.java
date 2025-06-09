package com.kunkunyu.link.submit;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LinkSubmitEvent extends ApplicationEvent {

    private final LinkSubmit linkSubmit;

    public LinkSubmitEvent(Object source, LinkSubmit linkSubmit) {
        super(source);
        this.linkSubmit = linkSubmit;
    }
}
