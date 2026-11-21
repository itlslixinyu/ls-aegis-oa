package com.ls.aegis.biz.sign.chain;

public interface SignTask {

    void execute(SignChainContext context) throws Exception;

    String getTaskName();

    default boolean canSkip(SignChainContext context) {
        return false;
    }
}
