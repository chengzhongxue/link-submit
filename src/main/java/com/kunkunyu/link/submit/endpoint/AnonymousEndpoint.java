package com.kunkunyu.link.submit.endpoint;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.LinkService;
import com.kunkunyu.link.submit.service.LinkSubmitService;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import com.kunkunyu.link.submit.vo.LinkGroupVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

import java.util.List;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

@Slf4j
@Component
public class AnonymousEndpoint implements CustomEndpoint {

    private final String tag = "anonymous.link.submit.kunkunyu.com/v1alpha1/LinkSubmit";

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final LinkService linkService;

    private final LinkSubmitService linkSubmitService;

    public AnonymousEndpoint(SettingConfigLinkSubmit settingConfigLinkSubmit,
        LinkService linkService, LinkSubmitService linkSubmitService) {
        this.settingConfigLinkSubmit = settingConfigLinkSubmit;
        this.linkService = linkService;
        this.linkSubmitService = linkSubmitService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("linkgroups", this::linkGroups, builder -> {
                builder.operationId("linkGroups")
                    .description("友链分组")
                    .tag(tag)
                    .response(
                        responseBuilder()
                            .implementationArray(LinkGroupVo.class)
                    );
            })
            .POST("linksubmits/-/submit", this::submit,
                builder -> builder.operationId("submit")
                    .description("自助提交友链")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(CreateLinkSubmitRequest.class))
                        ))
                    .response(responseBuilder()
                        .implementation(LinkSubmit.class))
            ).build();
    }

    Mono<ServerResponse> linkGroups(ServerRequest request) {
        var basicConfig = settingConfigLinkSubmit.getBasicConfig();

        return basicConfig.flatMap(basic -> {
            List<String> forbidSelectedGroupNames = basic.getForbidSelectedGroupName();

            return linkService.listGroup()
                .filter(linkGroupVo -> {
                    if (forbidSelectedGroupNames!=null) {
                       return  !forbidSelectedGroupNames.contains(linkGroupVo.getGroupName());
                    }
                    return true;
                }).collectList();
        }).flatMap(linkGroupVoList -> ServerResponse.ok().bodyValue(linkGroupVoList));
    }

    Mono<ServerResponse> submit(ServerRequest request) {
        return request.bodyToMono(CreateLinkSubmitRequest.class)
            .flatMap(linkSubmitService::createLinkSubmit)
            .flatMap(resultsVo -> ServerResponse.ok().bodyValue(resultsVo));
    }

    @Data
    public static class CreateLinkSubmitRequest {

        @NotBlank
        private String url;

        @NotBlank
        private String displayName;

        private String logo;

        private String description;

        private String oldUrl;

        private String email;

        @NotBlank
        private String groupName;

        private String rssUrl;

        @NotBlank
        private LinkSubmit.LinkSubmitType type;

    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("anonymous.link.submit.kunkunyu.com/v1alpha1");
    }
}
