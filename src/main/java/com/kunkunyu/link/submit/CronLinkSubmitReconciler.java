package com.kunkunyu.link.submit;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.kunkunyu.link.submit.extension.CronLinkSubmit;
import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.service.LinkService;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import com.kunkunyu.link.submit.utils.LinkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;

import static com.kunkunyu.link.submit.Constant.DELETE;
import static com.kunkunyu.link.submit.Constant.MOVE;
import static com.kunkunyu.link.submit.Constant.ORIGINAL_GROUP_NAME;
import static run.halo.app.extension.ExtensionUtil.defaultSort;
import static run.halo.app.extension.ExtensionUtil.notDeleting;

@Component
public class CronLinkSubmitReconciler implements Reconciler<Reconciler.Request> {
    private static final Logger log = LoggerFactory.getLogger(CronLinkSubmitReconciler.class);
    private final ExtensionClient client;
    private Clock clock;
    public static final String TIME_ZONE = "Asia/Shanghai";
    private final LinkService linkService;
    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    public CronLinkSubmitReconciler(ExtensionClient client, LinkService linkService,
        SettingConfigLinkSubmit settingConfigLinkSubmit) {
        this.client = client;
        this.linkService = linkService;
        this.settingConfigLinkSubmit = settingConfigLinkSubmit;
        this.clock = Clock.systemDefaultZone();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result reconcile(Request request) {
        return (Result)this.client.fetch(CronLinkSubmit.class, request.name()).map((cronLinkSubmit) -> {
            if (ExtensionUtil.isDeleted(cronLinkSubmit)) {
                return Result.doNotRetry();
            } else {
                CronLinkSubmit.CronLinkSubmitSpec spec = cronLinkSubmit.getSpec();
                if (!spec.isSuspend()) {
                    return Result.doNotRetry();
                } else {
                    String cron = spec.getCron();
                    String timezone = TIME_ZONE;
                    ZoneId zoneId = ZoneId.systemDefault();
                    if (timezone != null) {
                        try {
                            zoneId = (ZoneId)ApplicationConversionService.getSharedInstance().convert(timezone, ZoneId.class);
                        } catch (DateTimeException var18) {
                            DateTimeException e = var18;
                            log.error("Invalid zone ID {}", timezone, e);
                            return Result.doNotRetry();
                        }
                    }

                    Instant now = Instant.now(this.clock);
                    if (!CronExpression.isValidExpression(cron)) {
                        log.error("Cron expression {} is invalid.", cron);
                        return Result.doNotRetry();
                    } else {
                        CronExpression cronExp = CronExpression.parse(cron);
                        CronLinkSubmit.CronLinkSubmitStatus status = cronLinkSubmit.getStatus();
                        Instant lastScheduledTimestamp = status.getLastScheduledTimestamp();
                        if (lastScheduledTimestamp == null) {
                            lastScheduledTimestamp = cronLinkSubmit.getMetadata().getCreationTimestamp();
                        }

                        ZonedDateTime nextFromNow = (ZonedDateTime)cronExp.next(now.atZone(zoneId));
                        ZonedDateTime nextFromLast = (ZonedDateTime)cronExp.next(lastScheduledTimestamp.atZone(zoneId));
                        if (nextFromNow != null && nextFromLast != null) {
                            if (Objects.equals(nextFromNow, nextFromLast)) {
                                log.info("Skip scheduling and next scheduled at {}", nextFromNow);
                                status.setNextSchedulingTimestamp(nextFromNow.toInstant());
                                this.client.update(cronLinkSubmit);
                                return new Result(true, Duration.between(now, nextFromNow));
                            } else {

                                this.cleanLinks(spec);
                                log.info("执行定时清理无效友链");
                                ZonedDateTime zonedNow = now.atZone(zoneId);
                                ZonedDateTime scheduleTimestamp = now.atZone(zoneId);

                                ZonedDateTime next;
                                for(next = lastScheduledTimestamp.atZone(zoneId); next != null && next.isBefore(zonedNow); next = (ZonedDateTime)cronExp.next(next)) {
                                    scheduleTimestamp = next;
                                }

                                status.setLastScheduledTimestamp(scheduleTimestamp.toInstant());
                                if (next != null) {
                                    status.setNextSchedulingTimestamp(next.toInstant());
                                }

                                this.client.update(cronLinkSubmit);
                                log.info("Scheduled at {} and next scheduled at {}", scheduleTimestamp, next);
                                return new Result(true, Duration.between(now, next));
                            }
                        } else {
                            return Result.doNotRetry();
                        }
                    }
                }
            }
        }).orElseGet(Result::doNotRetry);
    }

    private void cleanLinks(CronLinkSubmit.CronLinkSubmitSpec spec) {
        CronLinkSubmit.CleanConfig cleanConfig = spec.getCleanConfig();
        LinkUtil linkUtil = new LinkUtil();
        Optional<SettingConfigLinkSubmit.BasicConfig> basicConfig =
            settingConfigLinkSubmit.getBasicConfig().blockOptional();
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.of(notDeleting()));
        List<Link> links = client.listAll(Link.class, listOptions, defaultSort());
        if (!links.isEmpty()) {
            for (Link link : links) {

                List<String> withoutCheckGroupNames = cleanConfig.getWithoutCheckGroupNames();
                String moveGroupName = cleanConfig.getMoveGroupName();
                String type = cleanConfig.getType();
                if (!withoutCheckGroupNames.contains(link.getSpec().getGroupName())
                    && !CharSequenceUtil.equals(moveGroupName,link.getSpec().getGroupName())
                    && !linkUtil.urlChecker(link.getSpec().getUrl())) {
                    if (CharSequenceUtil.equals(type, DELETE)) {
                        linkService.delete(link).block();
                    } else if (CharSequenceUtil.equals(type, MOVE)) {
                        // 保存原来分组名
                        if (ObjectUtil.isEmpty(link.getMetadata().getAnnotations())) {
                            Map<String, String> annotations = new HashMap<>();
                            link.getMetadata().setAnnotations(annotations);
                        }
                        link.getMetadata().getAnnotations().put(ORIGINAL_GROUP_NAME, link.getSpec().getGroupName());
                        link.getSpec().setGroupName(moveGroupName);
                        linkService.update(link).block();
                    }
                }else if (!withoutCheckGroupNames.contains(link.getSpec().getGroupName())
                    && CharSequenceUtil.equals(link.getSpec().getGroupName(),moveGroupName)) {
                    if (linkUtil.urlChecker(link.getSpec().getUrl())) {
                        Map<String, String> annotations = link.getMetadata().getAnnotations();
                        if (ObjectUtil.isEmpty(annotations)) {
                            annotations = new HashMap<>();
                            link.getMetadata().setAnnotations(annotations);
                        }
                        String originalGroupName = annotations.get(ORIGINAL_GROUP_NAME);
                        originalGroupName = CharSequenceUtil.blankToDefault(originalGroupName, basicConfig.get().getGroupName());
                        if (CharSequenceUtil.isNotBlank(originalGroupName)) {
                            link.getSpec().setGroupName(originalGroupName);
                            annotations.remove(ORIGINAL_GROUP_NAME);
                            link.getMetadata().setAnnotations(annotations);
                            linkService.update(link).block();
                        }
                    }
                }

            }

        }

    }

    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new CronLinkSubmit()).workerCount(1).build();
    }
}
