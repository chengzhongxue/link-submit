package com.kunkunyu.link.submit;

import com.kunkunyu.link.submit.extension.CronLinkSubmit;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.util.Optional;



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
            indexSpecs.add(IndexSpecs.<LinkSubmit, String>single("spec.url", String.class)
                .indexFunc(post -> Optional.ofNullable(post.getSpec())
                    .map(LinkSubmit.LinkSubmitSpec::getUrl)
                    .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<LinkSubmit, String>single("spec.displayName", String.class)
                .indexFunc(post -> Optional.ofNullable(post.getSpec())
                    .map(LinkSubmit.LinkSubmitSpec::getDescription)
                    .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<LinkSubmit, String>single("spec.oldUrl", String.class)
                .indexFunc(post -> Optional.ofNullable(post.getSpec())
                    .map(LinkSubmit.LinkSubmitSpec::getOldUrl)
                    .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<LinkSubmit, LinkSubmit.LinkSubmitType>single("spec.type", LinkSubmit.LinkSubmitType.class)
                .indexFunc(post -> Optional.ofNullable(post.getSpec())
                    .map(LinkSubmit.LinkSubmitSpec::getType)
                    .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<LinkSubmit, LinkSubmit.LinkSubmitStatus>single("spec.status", LinkSubmit.LinkSubmitStatus.class)
                .indexFunc(post -> Optional.ofNullable(post.getSpec())
                    .map(LinkSubmit.LinkSubmitSpec::getStatus)
                    .orElse(null)
                )
            );
        });
        schemeManager.register(CronLinkSubmit.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(LinkSubmit.class));
        schemeManager.unregister(Scheme.buildFromType(CronLinkSubmit.class));
    }
}
