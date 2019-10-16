/**
 * @(#)ShardUtil.java, 10月 07, 2019.
 * <p>
 * Copyright 2019 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.skycloud.fastdao.core.plugins.shard;

/**
 * @author yuntian
 */
public class ShardUtil {

    private static ThreadLocal<String> threadLocalSuffix = new ThreadLocal<>();

    private static ThreadLocal<Boolean> autoClear = new ThreadLocal<>();

    public static void setShardSuffixOnce(String suffix) {
        threadLocalSuffix.set(suffix);
        autoClear.set(true);
    }

    public static void setShardSuffixTillClear(String suffix) {
        threadLocalSuffix.set(suffix);
        autoClear.set(false);
    }

    public static void clearShardSuffix() {
        threadLocalSuffix.remove();
    }

    public static String getCurrentSuffix() {
        return threadLocalSuffix.get();
    }

    public static boolean needAutoClear() {
        return autoClear.get();
    }
}