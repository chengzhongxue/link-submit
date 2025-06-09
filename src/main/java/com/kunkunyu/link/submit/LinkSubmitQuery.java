package com.kunkunyu.link.submit;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.server.ServerRequest;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;

import java.util.Optional;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.QueryFactory.*;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;


@Slf4j
public class LinkSubmitQuery extends SortableRequest {


    public LinkSubmitQuery(ServerRequest request) {
        super(request.exchange());
    }

    @Nullable
    public String getKeyword() {
        return queryParams.getFirst("keyword");
    }

    @Nullable
    public String getType() {
        return queryParams.getFirst("type");
    }

    @Nullable
    public String getStatus() {
        return queryParams.getFirst("status");
    }


    /**
     * Convert to list options.
     */
    public ListOptions toListOptions() {
        var builder = ListOptions.builder(super.toListOptions());

        Optional.ofNullable(getKeyword())
            .filter(StringUtils::isNotBlank)
            .ifPresent(keyword -> builder.andQuery(or(
                contains("spec.url", keyword),
                contains("spec.displayName", keyword)
            )));


        Optional.ofNullable(getType())
            .filter(StringUtils::isNotBlank)
            .ifPresent(type -> builder.andQuery(equal("spec.type", type)));

        Optional.ofNullable(getStatus())
            .filter(StringUtils::isNotBlank)
            .ifPresent(status -> builder.andQuery(equal("spec.status", status)));

        return builder.build();
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter())
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("keyword")
                .description("LinkSubmits filtered by keyword.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("type")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("status")
                .implementation(String.class)
                .required(false));
    }
}
