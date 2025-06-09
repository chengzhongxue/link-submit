package com.kunkunyu.link.submit.endpoint;

import com.kunkunyu.link.submit.LinkSubmitQuery;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.LinkSubmitService;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

@Component
@RequiredArgsConstructor
public class LinkSubmitEndpoint implements CustomEndpoint {

    private final String tag = "console.api.link.submit.kunkunyu.com/v1alpha1/ListLinkSubmit";

    private final LinkSubmitService linkSubmitService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("linksubmits", this::listLinkSubmits, builder -> {
                    builder.operationId("ListLinkSubmits")
                        .description("List LinkSubmits.")
                        .tag(tag)
                        .response(responseBuilder()
                            .implementation(ListResult.generateGenericClass(LinkSubmit.class))
                        );
                    LinkSubmitQuery.buildParameters(builder);
                }
            )
            .POST("linksubmits/{name}/check", this::check,
                builder -> builder.operationId("check")
                    .description("友链提交审核操作")
                    .tag(tag)
                    .parameter(parameterBuilder().name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class))
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(CheckLinkSubmitRequest.class))
                        ))
                    .response(responseBuilder()
                        .implementation(LinkSubmit.class))
            )
            .build();
    }

    Mono<ServerResponse> listLinkSubmits(ServerRequest request) {
        LinkSubmitQuery query = new LinkSubmitQuery(request);
        return linkSubmitService.listLinkSubmit(query)
            .flatMap(linkSubmits -> ServerResponse.ok().bodyValue(linkSubmits));
    }

    Mono<ServerResponse> check(ServerRequest request) {
        var name = request.pathVariable("name");
        return request.bodyToMono(CheckLinkSubmitRequest.class)
            .flatMap(checkLinkSubmitRequest -> linkSubmitService.checkLink(name,checkLinkSubmitRequest))
            .flatMap(linkSubmit -> ServerResponse.ok().bodyValue(linkSubmit));
    }

    @Data
    public static class CheckLinkSubmitRequest {

        @NotBlank
        private Boolean checkStatus;

        private String reason;

        private String linkName;

    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.link.submit.kunkunyu.com/v1alpha1");
    }
}
