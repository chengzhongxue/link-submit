package com.kunkunyu.link.submit;

import com.kunkunyu.link.submit.extension.CronLinkSubmit;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;


@Component
public class LinkSubmitPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public LinkSubmitPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(LinkSubmit.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.url")
                .setIndexFunc(
                    simpleAttribute(LinkSubmit.class, linkSubmit -> linkSubmit.getSpec().getUrl()))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.displayName")
                .setIndexFunc(
                    simpleAttribute(LinkSubmit.class, linkSubmit -> linkSubmit.getSpec().getDescription()))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.type")
                .setIndexFunc(simpleAttribute(LinkSubmit.class, linkSubmit -> {
                    var type = linkSubmit.getSpec().getType();
                    return type == null ? null : type.name();
                })));
            indexSpecs.add(new IndexSpec()
                .setName("spec.status")
                .setIndexFunc(simpleAttribute(LinkSubmit.class, linkSubmit -> {
                    var status = linkSubmit.getSpec().getStatus();
                    return status == null ? null : status.name();
                })));
        });
        schemeManager.register(CronLinkSubmit.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(LinkSubmit.class));
        schemeManager.unregister(Scheme.buildFromType(CronLinkSubmit.class));
    }
}
