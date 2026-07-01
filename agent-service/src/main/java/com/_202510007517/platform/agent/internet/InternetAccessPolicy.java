package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

public class InternetAccessPolicy {
    private final AgentInternetProperties properties;
    private final HostAddressResolver hostAddressResolver;

    public InternetAccessPolicy(AgentInternetProperties properties) {
        this(properties, InetAddress::getAllByName);
    }

    InternetAccessPolicy(AgentInternetProperties properties, HostAddressResolver hostAddressResolver) {
        this.properties = properties;
        this.hostAddressResolver = hostAddressResolver;
    }

    public void validate(URI uri) {
        if (uri == null) {
            throw new InternetAccessDeniedException("联网地址不能为空。");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new InternetAccessDeniedException("只允许访问 http 或 https 地址。");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InternetAccessDeniedException("联网地址缺少有效域名。");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        validateAllowList(normalizedHost);
        if (properties.isBlockPrivateNetwork()) {
            validatePublicTarget(normalizedHost);
        }
    }

    private void validateAllowList(String host) {
        if (properties.getAllowedDomains().isEmpty()) {
            return;
        }
        boolean allowed = properties.getAllowedDomains().stream()
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
        if (!allowed) {
            throw new InternetAccessDeniedException("目标域名不在允许访问域名列表中。");
        }
    }

    private void validatePublicTarget(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost")) {
            throw new InternetAccessDeniedException("禁止访问本机地址。");
        }
        if (isIpv4Literal(host) && isPrivateIpv4(host)) {
            throw new InternetAccessDeniedException("禁止访问内网地址。");
        }
        validateResolvedAddresses(host);
    }

    private void validateResolvedAddresses(String host) {
        try {
            for (InetAddress address : hostAddressResolver.resolve(host)) {
                validatePublicAddress(address);
            }
        } catch (UnknownHostException ex) {
            throw new InternetAccessDeniedException("无法解析目标地址。");
        }
    }

    private void validatePublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            throw new InternetAccessDeniedException("禁止访问本机、链路本地或内网地址。");
        }
    }

    @FunctionalInterface
    interface HostAddressResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private boolean isIpv4Literal(String host) {
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private boolean isPrivateIpv4(String host) {
        String[] pieces = host.split("\\.");
        int first = Integer.parseInt(pieces[0]);
        int second = Integer.parseInt(pieces[1]);
        if (first == 0 || first == 10 || first == 127) {
            return true;
        }
        if (first == 100 && second >= 64 && second <= 127) {
            return true;
        }
        if (first == 169 && second == 254) {
            return true;
        }
        if (first == 172 && second >= 16 && second <= 31) {
            return true;
        }
        return first == 192 && second == 168;
    }
}
