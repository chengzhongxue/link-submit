package com.kunkunyu.link.submit.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.infra.ExternalUrlSupplier;

import java.net.URL;


@Component
@RequiredArgsConstructor
public class CommonUtil {
    private final ExternalUrlSupplier externalUrl;

    public String getExternalUrl() {
        URL externalUrlRaw = externalUrl.getRaw();
        return externalUrlRaw.getHost();
    }

    public String getDomain() {
        URL externalUrlRaw = externalUrl.getRaw();
        return externalUrlRaw.getAuthority();
    }

    /**
     * 校验是否是正确的邮箱格式
     *
     **/
    public boolean isValidEmail(String email) {
        return email.matches("^([a-zA-Z0-9._-])+@([a-zA-Z0-9_-])+((.[a-zA-Z0-9_-]{2,3}){1,2})$");
    }

}
