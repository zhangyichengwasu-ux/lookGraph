package com.lookgraph.common.util;

import java.util.UUID;

public final class IdUtil {

    private IdUtil() {}

    public static String simpleUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
