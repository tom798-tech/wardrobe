package com.tom.wardrobe.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * XSS 请求包装器
 * 对请求参数进行 XSS 过滤
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    /**
     * XSS 攻击字符模式
     */
    private static final Pattern[] XSS_PATTERNS = {
            // 过滤 <script> 标签
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
            // 过滤 <script 标签（没有闭合的）
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
            // 过滤事件处理器
            Pattern.compile("on\\w+\\s*=\\s*[\"'].*?[\"']", Pattern.CASE_INSENSITIVE),
            // 过滤 javascript: 协议
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            // 过滤 vbscript: 协议
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            // 过滤 data: 协议（可能包含恶意脚本）
            Pattern.compile("data:\\s*text/html", Pattern.CASE_INSENSITIVE),
            // 过滤 iframe 标签
            Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE),
            // 过滤 on* 属性
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            // 过滤 eval() 函数
            Pattern.compile("eval\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE),
            // 过滤 expression() 函数
            Pattern.compile("expression\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE),
            // 过滤 alert() 函数
            Pattern.compile("alert\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE),
            // 过滤 document.cookie
            Pattern.compile("document\\s*\\.\\s*cookie", Pattern.CASE_INSENSITIVE),
            // 过滤 document.write
            Pattern.compile("document\\s*\\.\\s*write", Pattern.CASE_INSENSITIVE),
            // 过滤 window.location
            Pattern.compile("window\\s*\\.\\s*location", Pattern.CASE_INSENSITIVE)
    };

    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return filterXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = filterXss(values[i]);
        }
        return values;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return filterXss(value);
    }

    /**
     * 过滤 XSS 攻击字符
     */
    private String filterXss(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        // 先进行特殊字符转义
        value = escapeHtml(value);

        // 再进行模式匹配过滤
        for (Pattern pattern : XSS_PATTERNS) {
            value = pattern.matcher(value).replaceAll("");
        }

        // 移除多余的空白字符
        value = value.replaceAll("\\s+", " ").trim();

        return value;
    }

    /**
     * HTML 特殊字符转义
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }
}