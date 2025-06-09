package com.kunkunyu.link.submit.service;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.vo.LinkGroupVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LinkService {

    Mono<Link> getName(String name);

    Flux<LinkGroupVo> listGroup();

    Mono<Boolean> isExists(String url);

    Mono<Link> create(LinkSubmit linkSubmit);

    Mono<Link> delete(Link link);

    Mono<Link> update(Link link);
}
