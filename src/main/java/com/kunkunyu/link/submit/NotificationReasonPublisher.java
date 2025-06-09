package com.kunkunyu.link.submit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;
import java.util.Map;

import static com.kunkunyu.link.submit.Constant.ADMIN_LINK_SUBMIT;
import static com.kunkunyu.link.submit.Constant.MARK_AS_NOTIFIED;
import static com.kunkunyu.link.submit.Constant.REVIEW_LINK_SUBMIT;
import static com.kunkunyu.link.submit.Constant.USER_LINK_SUBMIT;
import static com.kunkunyu.link.submit.extension.LinkSubmit.REVIEW_DESCRIPTION;

@Component
@RequiredArgsConstructor
public class NotificationReasonPublisher {

    private final ExtensionClient client;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final AdminLinkSubmitNoticeReasonPublisher adminLinkSubmitNoticeReasonPublisher;

    private final UserLinkSubmitNoticeReasonPublisher userLinkSubmitNoticeReasonPublisher;

    private final ReviewLinkSubmitNoticeReasonPublisher reviewLinkSubmitNoticeReasonPublisher;


    @Async
    @EventListener(LinkSubmitEvent.class)
    public void onPostPublished(LinkSubmitEvent event) {
        LinkSubmit linkSubmit = event.getLinkSubmit();
        var basicConfig = settingConfigLinkSubmit.getBasicConfig().block();
        boolean sendEmail = basicConfig.isSendEmail();
        if (sendEmail) {
            adminLinkSubmitNoticeReasonPublisher.publishReasonBy(linkSubmit,basicConfig.getAdminEmail());
        }
        String email = linkSubmit.getSpec().getEmail();
        if (StringUtils.isNotEmpty(email)) {
            userLinkSubmitNoticeReasonPublisher.publishReasonBy(linkSubmit,email);
        }

    }

    @Async
    @EventListener(ReviewLinkSubmitEvent.class)
    public void onUserCouponSend(ReviewLinkSubmitEvent event) {
        LinkSubmit linkSubmit = event.getLinkSubmit();
        String email = linkSubmit.getSpec().getEmail();
        if (StringUtils.isNotEmpty(email)) {
            var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
            if (annotations.containsKey(MARK_AS_NOTIFIED)) {
                return;
            }
            reviewLinkSubmitNoticeReasonPublisher.publishReasonBy(linkSubmit,email);
            ReviewLinkSubmitMarkAsNotified(linkSubmit.getMetadata().getName());
        }
    }

    private void ReviewLinkSubmitMarkAsNotified(String name) {
        client.fetch(LinkSubmit.class, name).ifPresent(linkSubmit -> {
            MetadataUtil.nullSafeAnnotations(linkSubmit).put(MARK_AS_NOTIFIED, "true");
            client.update(linkSubmit);
        });
    }

    @Component
    @RequiredArgsConstructor
    static class AdminLinkSubmitNoticeReasonPublisher {
        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;


        public void publishReasonBy(LinkSubmit linkSubmit,String email) {
            String url = externalLinkProcessor.processLink("/console/linksubmit");
            var spec = linkSubmit.getSpec();
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(linkSubmit.getApiVersion())
                .kind(linkSubmit.getKind())
                .name(linkSubmit.getMetadata().getName())
                .title(linkSubmit.getSpec().getDisplayName())
                .url(url)
                .build();
            notificationReasonEmitter.emit(ADMIN_LINK_SUBMIT,
                builder -> {
                    var attributes = ReasonData.builder()
                        .email(email)
                        .displayName(spec.getDisplayName())
                        .url(spec.getUrl())
                        .description(spec.getDescription())
                        .logo(spec.getLogo())
                        .type(spec.getType().name())
                        .review(spec.getStatus().equals(LinkSubmit.LinkSubmitStatus.review))
                        .reviewUrl(url)
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.anonymousWithEmail(email))
                        .subject(reasonSubject);
                }).block();
        }


        @Builder
        record ReasonData(String email, String displayName, String url, String description,
                          String logo, String type, Boolean review, String reviewUrl) {
        }
    }


    @Component
    @RequiredArgsConstructor
    static class UserLinkSubmitNoticeReasonPublisher {
        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;


        public void publishReasonBy(LinkSubmit linkSubmit,String email) {
            String url = externalLinkProcessor.processLink("/console/linksubmit");
            var spec = linkSubmit.getSpec();
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(linkSubmit.getApiVersion())
                .kind(linkSubmit.getKind())
                .name(linkSubmit.getMetadata().getName())
                .title(linkSubmit.getSpec().getDisplayName())
                .url(url)
                .build();
            notificationReasonEmitter.emit(USER_LINK_SUBMIT,
                builder -> {
                    var attributes = ReasonData.builder()
                        .email(email)
                        .type(spec.getType().name())
                        .review(spec.getStatus().equals(LinkSubmit.LinkSubmitStatus.review))
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.anonymousWithEmail(email))
                        .subject(reasonSubject);
                }).block();
        }


        @Builder
        record ReasonData(String email, String type, Boolean review) {
        }
    }

    @Component
    @RequiredArgsConstructor
    static class ReviewLinkSubmitNoticeReasonPublisher {
        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;


        public void publishReasonBy(LinkSubmit linkSubmit,String email) {
            var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
            String reviewDescription = annotations.get(REVIEW_DESCRIPTION);
            String url = externalLinkProcessor.processLink("/console/linksubmit");
            var spec = linkSubmit.getSpec();
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(linkSubmit.getApiVersion())
                .kind(linkSubmit.getKind())
                .name(linkSubmit.getMetadata().getName())
                .title(linkSubmit.getSpec().getDisplayName())
                .url(url)
                .build();
            notificationReasonEmitter.emit(REVIEW_LINK_SUBMIT,
                builder -> {
                    var attributes = ReasonData.builder()
                        .email(email)
                        .type(spec.getType().name())
                        .reviewDescription(reviewDescription)
                        .review(spec.getStatus().equals(LinkSubmit.LinkSubmitStatus.review))
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.anonymousWithEmail(email))
                        .subject(reasonSubject);
                }).block();
        }


        @Builder
        record ReasonData(String email, String type, String reviewDescription, Boolean review) {
        }
    }

    @UtilityClass
    static class ReasonDataConverter {
        public static <T> Map<String, Object> toAttributeMap(T data) {
            Assert.notNull(data, "Reason attributes must not be null");
            return JsonUtils.mapper().convertValue(data, new TypeReference<>() {
            });
        }
    }
}
