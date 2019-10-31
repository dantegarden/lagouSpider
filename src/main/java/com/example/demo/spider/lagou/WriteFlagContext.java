package com.example.demo.spider.lagou;

/**
 * @description:
 * @author: lij
 * @create: 2019-10-31 14:01
 */
public class WriteFlagContext {
    private static ThreadLocal<Boolean> writeFlagHolder = new ThreadLocal<Boolean>();

    public static void setWriteFlag(Boolean writeFlag) {
        writeFlagHolder.set(writeFlag);
    }

    public static Boolean getWriteFlag() {
        return writeFlagHolder.get();
    }
}
