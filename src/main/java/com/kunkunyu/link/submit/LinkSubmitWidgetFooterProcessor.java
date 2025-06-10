package com.kunkunyu.link.submit;

import java.util.Properties;
import com.google.common.base.Throwables;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.RouteMatcher;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import org.springframework.web.util.pattern.PatternParseException;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.web.IWebRequest;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateFooterProcessor;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkSubmitWidgetFooterProcessor implements TemplateFooterProcessor {

    static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final PluginContext pluginContext;

    private final RouteMatcher routeMatcher = createRouteMatcher();

    @Override
    public Mono<Void> process(ITemplateContext context,IProcessableElementTag tag,
        IElementTagStructureHandler structureHandler, IModel model) {
        return settingConfigLinkSubmit.getBasicConfig()
            .doOnNext(basicConfig -> {
                if (!basicConfig.isLoadPlugInResources()) {
                    return;
                }
                if (!mismatchedRoute(context,"/links")) {
                    return;
                }
                IModelFactory modelFactory = context.getModelFactory();
                String html = linkSubmitWidgetScript(basicConfig.isDisplayTheSubmitButton());
                model.add(modelFactory.createText(html));
            }).onErrorResume(e -> {
            log.error("LinkSubmitWidgetFooterProcessor process failed", e);
            return Mono.empty();
        }).then();
    }

    private String linkSubmitWidgetScript(boolean displayTheSubmitButton) {
        String html = """
            <!-- LinkSubmitWidget start -->
            <script src="/plugins/link-submit/assets/static/link-submit-widget.iife.js?version=${version}" defer></script>
            <link rel="stylesheet" href="/plugins/link-submit/assets/static/var.css?version=${version}" />
            <!-- LinkSubmitWidget end -->
            """;

        if (displayTheSubmitButton) {
            html += """ 
                <button title="提交友链"
                    onclick="LinkSubmitWidget.open()"
                    style="position: fixed; right: 2rem; bottom: 6rem; width: 3rem; height: 3rem; border-radius: 50%; background-color: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; transition: background-color 0.3s;"><svg
                        viewBox="0 0 24 24" width="1.5em" height="1.5em">
                        <path fill="#fff"
                            d="M10.6 13.4a1 1 0 0 1-1.4 1.4a4.8 4.8 0 0 1 0-7l3.5-3.6a5.1 5.1 0 0 1 7.1 0a5.1 5.1 0 0 1 0 7.1l-1.5 1.5a6.4 6.4 0 0 0-.4-2.4l.5-.5a3.2 3.2 0 0 0 0-4.3a3.2 3.2 0 0 0-4.3 0l-3.5 3.6a2.9 2.9 0 0 0 0 4.2M23 18v2h-3v3h-2v-3h-3v-2h3v-3h2v3m-3.8-4.3a4.8 4.8 0 0 0-1.4-4.5a1 1 0 0 0-1.4 1.4a2.9 2.9 0 0 1 0 4.2l-3.5 3.6a3.2 3.2 0 0 1-4.3 0a3.2 3.2 0 0 1 0-4.3l.5-.4a7.3 7.3 0 0 1-.4-2.5l-1.5 1.5a5.1 5.1 0 0 0 0 7.1a5.1 5.1 0 0 0 7.1 0l1.8-1.8a6 6 0 0 1 3.1-4.3">
                        </path>
                    </svg>
                </button>
                """;
        }

        final Properties properties = new Properties();
        properties.setProperty("version", pluginContext.getVersion());
        return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(html, properties);
    }

    public boolean mismatchedRoute(ITemplateContext context, String rule) {
        if (!Contexts.isWebContext(context)) {
            return false;
        }
        IWebRequest request = Contexts.asWebContext(context).getExchange().getRequest();
        String requestPath = request.getRequestPath();
        RouteMatcher.Route requestRoute = routeMatcher.parseRoute(requestPath);

        return isMatchedRoute(requestRoute, rule);
    }

    private boolean isMatchedRoute(RouteMatcher.Route requestRoute, String rule) {
        try {
            return routeMatcher.match(rule, requestRoute);
        } catch (PatternParseException e) {
            // ignore
            log.warn("Parse route pattern [{}] failed", rule, Throwables.getRootCause(e));
        }
        return false;
    }

    RouteMatcher createRouteMatcher() {
        var parser = new PathPatternParser();
        parser.setPathOptions(PathContainer.Options.HTTP_PATH);
        return new PathPatternRouteMatcher(parser);
    }


}
