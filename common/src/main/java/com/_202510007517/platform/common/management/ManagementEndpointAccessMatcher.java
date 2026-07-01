package com._202510007517.platform.common.management;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ManagementEndpointAccessMatcher {

    private final ManagementEndpointAccessProperties properties;
    private final List<CidrBlock> allowedCidrs;

    public ManagementEndpointAccessMatcher(ManagementEndpointAccessProperties properties) {
        this.properties = properties;
        this.allowedCidrs = parseCidrs(properties.getAllowedCidrs());
    }

    public boolean isProtectedPath(String path) {
        return path != null && ("/actuator".equals(path) || path.startsWith("/actuator/"));
    }

    public boolean isAllowed(String remoteAddress) {
        if (!properties.isEnabled()) {
            return true;
        }
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddress.trim());
            for (CidrBlock cidr : allowedCidrs) {
                if (cidr.matches(address)) {
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    private static List<CidrBlock> parseCidrs(List<String> configuredCidrs) {
        List<CidrBlock> result = new ArrayList<>();
        if (configuredCidrs == null) {
            return result;
        }
        for (String cidr : configuredCidrs) {
            if (cidr != null && !cidr.isBlank()) {
                result.add(CidrBlock.parse(cidr.trim()));
            }
        }
        return result;
    }

    private static final class CidrBlock {
        private final byte[] network;
        private final int prefixLength;

        private CidrBlock(byte[] network, int prefixLength) {
            this.network = network;
            this.prefixLength = prefixLength;
        }

        private static CidrBlock parse(String cidr) {
            String[] parts = cidr.split("/", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid CIDR: " + cidr);
            }
            try {
                InetAddress address = InetAddress.getByName(parts[0]);
                int prefixLength = Integer.parseInt(parts[1]);
                int maxPrefixLength = address.getAddress().length * 8;
                if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                    throw new IllegalArgumentException("Invalid CIDR prefix: " + cidr);
                }
                return new CidrBlock(address.getAddress(), prefixLength);
            } catch (UnknownHostException ex) {
                throw new IllegalArgumentException("Invalid CIDR address: " + cidr, ex);
            }
        }

        private boolean matches(InetAddress candidate) {
            byte[] candidateBytes = candidate.getAddress();
            if (candidateBytes.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (network[index] != candidateBytes[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (network[fullBytes] & mask) == (candidateBytes[fullBytes] & mask);
        }
    }
}
