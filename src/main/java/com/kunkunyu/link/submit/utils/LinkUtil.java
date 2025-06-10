package com.kunkunyu.link.submit.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpMethod;
import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class LinkUtil {

    public static final String HTTP_PROROCOL = "http://";
    public static final String HTTPS_PROROCOL = "https://";

    /**
     * 单位b，1kb = 1024b
     */
    private static final int DEFAULT_FACICON_MAX_SIZE = 1024 * 5;
    private static final int DEFAULT_FACICON_MIN_SIZE = 1;

    /**
     * 获取网站favicon图标的正则表达式
     */
    private static final Pattern[] ICON_PATTERNS = new Pattern[] {
        Pattern.compile("rel=[\"']icon[\"'][^\r\n>]+?((?<=href=[\"']).+?(?=[\"']))"),
        Pattern.compile("((?<=href=[\"']).+?(?=[\"']))[^\r\n<]+?rel=[\"']icon[\"']")};

    /**
     * 获取网站的favicon图标<br>
     * https站点可能存在问题
     *
     * @param url 网站地址
     * @return favicon地址
     * @deprecated 当前版本为前台用户手动录入favicon，可考虑后台应用使用该接口
     */
    @Deprecated
    public static String getFavicon(String url) {
        if (!url.startsWith(HTTP_PROROCOL) && !url.startsWith(HTTPS_PROROCOL)) {
            url = HTTP_PROROCOL + url;
        }
        String html = null;
        try {
            html = HttpUtil.get(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Pattern iconPattern : ICON_PATTERNS) {
            Matcher matcher = iconPattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * 检查favicon的有效性
     *
     * @param faviocnUrl favicon地址
     * @return true favicon大小合适，false favicon过大或者过小
     */
    public static boolean checkFavicon(String faviocnUrl) {
        int faviconLength = getFaviconSize(faviocnUrl);
        return faviconLength >= DEFAULT_FACICON_MIN_SIZE &&
            faviconLength < DEFAULT_FACICON_MAX_SIZE;
    }

    /**
     * 获取网站的favicon图标大小
     *
     * @param faviocnUrl favicon地址
     * @return favicon图标大小
     */
    private static int getFaviconSize(String faviocnUrl) {
        int contentLength = 0;
        try {
            final URL url = new URL(faviocnUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HttpMethod.GET.name());
            connection.setDoOutput(true);
            // 必须设置false，否则会自动redirect到Location的地址
            connection.setInstanceFollowRedirects(false);
            contentLength = connection.getContentLength();
            log.debug("Favicon size : {}", contentLength);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            log.error("请求地址不对", e);
        } catch (ProtocolException e) {
            e.printStackTrace();
            log.error("请求地址协议异常", e);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("数据传输异常", e);
        }
        return contentLength;
    }

    /**
     * 获取Element
     *
     * @param htmlDocument
     * @param id
     * @return
     */
    private static Element getElementById(Document htmlDocument, String id) {
        if (htmlDocument == null || id == null || id.equals("")) {
            return null;
        }
        return htmlDocument.getElementById(id);
    }

    /**
     * 爬取url所在的页面，查找是否添加了本站友情链接
     *
     * @param url 目标站地址
     * @param domainName 本站地址
     * @return true 已链接本站，false 未链接
     */
    public static boolean hasLinkByHtml(String url, String domainName) {
        if (!url.startsWith(HTTP_PROROCOL) && !url.startsWith(HTTPS_PROROCOL)) {
            url = HTTP_PROROCOL + url;
        }
        String html = null;
        try {
            html = HttpUtil.get(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CharSequenceUtil.isNotEmpty(html) && html.contains(domainName);
    }

    /**
     * 通过Chinaz接口
     * 查找是否添加了本站友情链接
     *
     * @param url 目标站地址
     * @param domainName 本站地址
     * @return true 已链接本站，false 未链接
     */
    public static boolean hasLinkByChinaz(String url, String domainName) {
        if (url.startsWith(HTTP_PROROCOL) || url.startsWith(HTTPS_PROROCOL)) {
            url = url.replace(HTTP_PROROCOL, "");
            url = url.replace(HTTPS_PROROCOL, "");
        }
        try {
            Document htmlDocument = Jsoup.parse(HttpUtil.get("https://link.chinaz.com/" + url));
            Element ulElement = getElementById(htmlDocument, "ulLink");
            int maxRequestCount = 2;
            while (ulElement == null) {
                if (maxRequestCount == 0) {
                    return false;
                }
                System.err.println("没有获取到element.还剩" + maxRequestCount + "次获取机会.");
                htmlDocument = Jsoup.parse(HttpUtil.get("https://link.chinaz.com/" + url));
                ulElement = getElementById(htmlDocument, "ulLink");
                maxRequestCount--;
            }

            Elements liElements = ulElement.getElementsByTag("li");
            if (liElements == null || liElements.isEmpty()) {
                return false;
            }
            for (int i = 1; i < liElements.size(); i++) {
                Element liElement = liElements.get(i);
                if (liElement == null) {
                    continue;
                }
                String text =
                    liElement.getElementsByClass("tl").get(0).getElementsByTag("span").get(1)
                        .getElementsByTag("a").get(0).text();
                if (text.startsWith(domainName) || text.startsWith("www." + domainName)
                    || text.startsWith(HTTP_PROROCOL + domainName) ||
                    text.startsWith(HTTP_PROROCOL + "www." + domainName)
                    || text.startsWith(HTTPS_PROROCOL + domainName) ||
                    text.startsWith(HTTPS_PROROCOL + "www." + domainName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("访问Chinaz网站失败！", e);
        }
        return false;
    }

    /**
     * 判断url是否存在本站域名
     *
     * @param url:
     * @param domainName:
     * @return: boolean
     * @author: lywq
     * @date: 2024/03/05 15:17
     **/
    public static boolean hasLinkByUrl(String url, String domainName) {
        if (domainName.startsWith(HTTP_PROROCOL) || domainName.startsWith(HTTPS_PROROCOL)) {
            domainName = domainName.replace(HTTP_PROROCOL, "");
            domainName = domainName.replace(HTTPS_PROROCOL, "");
        }
        return url.contains(domainName);
    }

    /**
     * 获取域名
     *
     * @param urlString:
     * @return: java.lang.String
     * @author: lywq
     * @date: 2024/03/05 15:17
     **/
    public static String getDomain(String urlString) {
        try {
            // 创建URL对象
            URL url = new URL(urlString);

            // 获取域名部分
            return url.getHost();
        } catch (MalformedURLException e) {
            // URL格式不正确
            return "Invalid URL: " + e.getMessage();
        }
    }

    /**
     * 校验是否是url
     *
     * @param urlString:
     * @return: boolean
     * @author: lywq
     * @date: 2024/03/05 15:17
     **/
    public static boolean isValidUrl(String urlString) {
        URI uri = null;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
        if (uri.getHost() == null) {
            return false;
        }
        if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
            return true;
        }
        return false;
    }

    /**
     * 判断url是否可用
     *
     * @param url:
     * @return: boolean
     * @author: lywq
     * @date: 2024/03/05 15:17
     **/
    public static boolean urlChecker(String url) {
        try {
            HttpResponse response = HttpRequest.get(url).setConnectionTimeout(3000).execute();
            int statusCode = response.getStatus();
            return statusCode >= 200 && statusCode < 300;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean urlCheckerByChinaz(String url) {
        try {
            Document htmlDocument = Jsoup.parse(HttpUtil.get("https://tool.chinaz.com/pagestatus/?url=" + url));
            if (ObjectUtil.isNotEmpty(htmlDocument)) {
                Elements elementsByClass = htmlDocument.getElementsByClass("bor-b1s bg-list clearfix");
                if (ObjectUtil.isNotEmpty(elementsByClass)) {
                    Element element = elementsByClass.get(0);
                    Elements elementsByTag = element.getElementsByTag("span");
                    String code = elementsByTag.text();
                    return code.equals("200");
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            log.error("访问Chinaz网站失败！", e);
        }
        return false;
    }


}
