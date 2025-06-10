package com.kunkunyu.link.submit;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.UserIdentity;
import java.util.Set;

import static com.kunkunyu.link.submit.Constant.ADMIN_LINK_SUBMIT;
import static com.kunkunyu.link.submit.Constant.FINALIZER_NAME;
import static com.kunkunyu.link.submit.Constant.REVIEW_LINK_SUBMIT;
import static com.kunkunyu.link.submit.Constant.USER_LINK_SUBMIT;
import static run.halo.app.extension.ExtensionUtil.addFinalizers;
import static run.halo.app.extension.ExtensionUtil.removeFinalizers;

/**
 * Reconciler for {@link LinkSubmit}.
 *
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkSubmitReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationCenter notificationCenter;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    @Override
    public Result reconcile(Request request) {
        client.fetch(LinkSubmit.class, request.name())
            .ifPresent(linkSubmit -> {
                if (ExtensionUtil.isDeleted(linkSubmit)) {
                    removeFinalizers(linkSubmit.getMetadata(), Set.of(FINALIZER_NAME));
                    client.update(linkSubmit);
                    return;
                }

                var spec = linkSubmit.getSpec();
                String email = spec.getEmail();
                if (addFinalizers(linkSubmit.getMetadata(), Set.of(FINALIZER_NAME))) {
                    var basicConfig = settingConfigLinkSubmit.getBasicConfig().block();
                    boolean sendEmail = basicConfig.isSendEmail();
                    if (sendEmail) {
                        adminNoticeSubscription(basicConfig.getAdminEmail());
                    }
                    var status = spec.getStatus();
                    if (StringUtils.isNotEmpty(email) && status.equals(LinkSubmit.LinkSubmitStatus.pending)) {
                        userNoticeSubscription(email);
                    }
                    eventPublisher.publishEvent(new LinkSubmitEvent(this, linkSubmit));
                    client.update(linkSubmit);
                    return;
                }

                if (spec.getStatus().equals(LinkSubmit.LinkSubmitStatus.refuse) || spec.getStatus().equals(LinkSubmit.LinkSubmitStatus.review)) {
                    if (StringUtils.isNotEmpty(email)) {
                        reviewNoticeSubscription(email);
                    }
                    eventPublisher.publishEvent(new ReviewLinkSubmitEvent(this, linkSubmit));
                }
            });
        return Result.doNotRetry();
    }

    void adminNoticeSubscription(String email) {
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(ADMIN_LINK_SUBMIT);
        interestReason.setExpression("props.email == '%s'".formatted(email));
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(UserIdentity.anonymousWithEmail(email).name());
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    void userNoticeSubscription(String email) {
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(USER_LINK_SUBMIT);
        interestReason.setExpression("props.email == '%s'".formatted(email));
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(UserIdentity.anonymousWithEmail(email).name());
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    void reviewNoticeSubscription(String email) {
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(REVIEW_LINK_SUBMIT);
        interestReason.setExpression("props.email == '%s'".formatted(email));
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(UserIdentity.anonymousWithEmail(email).name());
        notificationCenter.subscribe(subscriber, interestReason).block();
    }


    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new LinkSubmit())
            .build();
    }
}
