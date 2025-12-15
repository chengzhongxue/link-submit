package com.kunkunyu.link.submit.service.impl;

import com.kunkunyu.link.submit.LinkSubmitQuery;
import com.kunkunyu.link.submit.endpoint.AnonymousEndpoint;
import com.kunkunyu.link.submit.endpoint.LinkSubmitEndpoint;
import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.LinkService;
import com.kunkunyu.link.submit.service.LinkSubmitService;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import com.kunkunyu.link.submit.utils.CommonUtil;
import com.kunkunyu.link.submit.utils.LinkUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import org.springframework.web.server.ServerWebInputException;
import run.halo.app.extension.router.selector.FieldSelector;

import static com.kunkunyu.link.submit.extension.LinkSubmit.REVIEW_DESCRIPTION;
import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;

@Component
@RequiredArgsConstructor
public class LinkSubmitServiceImpl implements LinkSubmitService {

    private final ReactiveExtensionClient client;

    private final LinkService linkService;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final CommonUtil commonUtil;

    @Override
    public Mono<ListResult<LinkSubmit>> listLinkSubmit(LinkSubmitQuery query) {
        return client.listBy(LinkSubmit.class, query.toListOptions(),
            PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort()));
    }

    @Override
    public Mono<LinkSubmit> createLinkSubmit(AnonymousEndpoint.CreateLinkSubmitRequest createLinkSubmitRequest) {

        String url = createLinkSubmitRequest.getUrl();
        String displayName = createLinkSubmitRequest.getDisplayName();
        String logo = createLinkSubmitRequest.getLogo();
        String email = createLinkSubmitRequest.getEmail();
        LinkSubmit.LinkSubmitType type = createLinkSubmitRequest.getType();
        String oldUrl = createLinkSubmitRequest.getOldUrl();

        if (StringUtils.isEmpty(url)) {
            return Mono.error(new ServerWebInputException("网站地址不能为空！"));
        }
        if (StringUtils.isEmpty(displayName)) {
            return Mono.error(new ServerWebInputException("网站名称不能为空！"));
        }
        if (!LinkUtil.isValidUrl(url)) {
            return Mono.error(new ServerWebInputException("网站地址格式有误！"));
        }
        if (!StringUtils.isEmpty(logo) && !LinkUtil.isValidUrl(createLinkSubmitRequest.getLogo())) {
            return Mono.error(new ServerWebInputException("网站Logo地址格式有误！"));
        }
        if (!StringUtils.isEmpty(email) && !commonUtil.isValidEmail(email)) {
            return Mono.error(new ServerWebInputException("邮箱格式有误！"));
        }

        String domain = LinkUtil.getDomain(url);
        if (type.equals(LinkSubmit.LinkSubmitType.update)) {
            if (StringUtils.isEmpty(oldUrl)) {
                return Mono.error(new ServerWebInputException("请填写旧的站点链接！"));
            }
            domain = LinkUtil.getDomain(oldUrl);
        }
        String finalDomain = domain;
        return linkService.isExists(domain)
            .flatMap(exists -> {
                if (type.equals(LinkSubmit.LinkSubmitType.add)) {
                    if (exists) {
                        return Mono.error(new ServerWebInputException("链接已存在！"));
                    }
                }
                if (type.equals(LinkSubmit.LinkSubmitType.update)) {
                    if (!exists) {
                        return Mono.error(new ServerWebInputException("链接不存在，请提交链接，而不是提交修改链接！"));
                    }
                }

                LinkSubmit linkSubmit = new LinkSubmit();
                Metadata metadata = new Metadata();
                metadata.setGenerateName("link-submit-");
                linkSubmit.setMetadata(metadata);
                LinkSubmit.LinkSubmitSpec linkSubmitSpec = new LinkSubmit.LinkSubmitSpec();
                linkSubmitSpec.setUrl(url);
                linkSubmitSpec.setDisplayName(displayName);
                linkSubmitSpec.setLogo(logo);
                linkSubmitSpec.setDescription(createLinkSubmitRequest.getDescription());
                if (type.equals(LinkSubmit.LinkSubmitType.update)) {
                    linkSubmitSpec.setOldUrl(createLinkSubmitRequest.getOldUrl());
                }
                linkSubmitSpec.setEmail(email);
                linkSubmitSpec.setGroupName(createLinkSubmitRequest.getGroupName());
                linkSubmitSpec.setRssUrl(createLinkSubmitRequest.getRssUrl());
                linkSubmitSpec.setType(createLinkSubmitRequest.getType());
                linkSubmitSpec.setStatus(LinkSubmit.LinkSubmitStatus.pending);
                linkSubmit.setSpec(linkSubmitSpec);
                return createNewLink(finalDomain,linkSubmit);
            });
    }

    @Override
    @Transactional
    public Mono<LinkSubmit> checkLink(String name,LinkSubmitEndpoint.CheckLinkSubmitRequest checkLinkSubmitRequest) {
        return client.fetch(LinkSubmit.class,name)
            .filter(linkSubmit -> linkSubmit.getSpec().getStatus().equals(LinkSubmit.LinkSubmitStatus.pending))
            .switchIfEmpty(Mono.error(new ServerWebInputException("已审核或不存在！")))
            .flatMap(linkSubmit -> {
                var spec = linkSubmit.getSpec();
                Boolean checkStatus = checkLinkSubmitRequest.getCheckStatus();
                String linkName = checkLinkSubmitRequest.getLinkName();
                String reason = checkLinkSubmitRequest.getReason();
                var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
                if (StringUtils.isNotEmpty(reason)) {
                    annotations.put(REVIEW_DESCRIPTION,reason);
                }

                spec.setStatus(checkStatus ? LinkSubmit.LinkSubmitStatus.review : LinkSubmit.LinkSubmitStatus.refuse);
                if (spec.getType().equals(LinkSubmit.LinkSubmitType.add)) {
                    if (checkStatus) {
                        return linkService.create(linkSubmit)
                            .then(client.update(linkSubmit));
                    }
                    return client.update(linkSubmit);
                }else {
                    if (checkStatus) {
                        return linkService.getName(linkName)
                            .switchIfEmpty(Mono.error(new ServerWebInputException("链接不存在！")))
                            .flatMap(link -> updateLink(link,linkSubmit))
                            .then(client.update(linkSubmit));
                    }
                    return client.update(linkSubmit);
                }
            });
    }

    private Mono<Link> updateLink(Link link,LinkSubmit linkSubmit) {
        var linkSubmitSpec = linkSubmit.getSpec();
        var spec = link.getSpec();
        spec.setUrl(linkSubmitSpec.getUrl());
        spec.setDisplayName(linkSubmitSpec.getDisplayName());
        spec.setLogo(linkSubmitSpec.getLogo());
        spec.setGroupName(linkSubmitSpec.getGroupName());
        spec.setDescription(linkSubmitSpec.getDescription());
        var annotations = MetadataUtil.nullSafeAnnotations(link);
        if (StringUtils.isNotEmpty(linkSubmitSpec.getEmail())) {
            annotations.put("email", linkSubmitSpec.getEmail());
        }
        if (StringUtils.isNotEmpty(linkSubmitSpec.getRssUrl())) {
            annotations.put("rss_url", linkSubmitSpec.getRssUrl());
        }
        return linkService.update(link);
    }

    private Mono<LinkSubmit> createNewLink(String submitDomain, LinkSubmit linkSubmit) {

        var basicConfig = settingConfigLinkSubmit.getBasicConfig();

        return basicConfig.flatMap(basic -> {
            String domain = commonUtil.getDomain();
            var spec = linkSubmit.getSpec();

            if (LinkUtil.hasLinkByUrl(spec.getUrl(), domain)) {
                return Mono.error(new ServerWebInputException("请不要输入本站地址！"));
            }
            if (StringUtils.isEmpty(spec.getLogo())) {
                String favicon = LinkUtil.getFavicon(spec.getUrl());
                if (!StringUtils.isEmpty(favicon) && LinkUtil.checkFavicon(favicon)) {
                    spec.setLogo(favicon);
                }
            }

            return linkSubmitExistence(submitDomain,spec.getType().name())
                .flatMap(exists -> {
                    boolean checkFlag = basic.isAutoAudit();
                    if (exists) {
                        return Mono.error(new ServerWebInputException("请勿重复提交，请等待审核！"));
                    }
                    if (checkFlag && linkSubmit.getSpec().getType().equals(LinkSubmit.LinkSubmitType.add)) {
                        return linkService.create(linkSubmit).flatMap(linkNew -> {
                            linkSubmit.getSpec().setStatus(LinkSubmit.LinkSubmitStatus.review);
                            return client.create(linkSubmit);
                        });
                    } else {
                        return client.create(linkSubmit);
                    }
                });
        });
    }

    public Mono<Boolean> linkSubmitExistence(String url,String type) {
        var listOptions = new ListOptions();
        FieldSelector fieldSelector = FieldSelector.of(and(equal("spec.type", type),
            equal("spec.status", LinkSubmit.LinkSubmitStatus.pending.name())));
        if (type.equals(LinkSubmit.LinkSubmitType.add.name())) {
            fieldSelector =  fieldSelector.andQuery(contains("spec.url",url));
        }else {
            fieldSelector =  fieldSelector.andQuery(contains("spec.oldUrl",url));
        }

        listOptions.setFieldSelector(fieldSelector);
        return client.listAll(LinkSubmit.class, listOptions, Sort.unsorted()).hasElements();
    }
}
