package com.proxy.callback;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MsgCallBack implements CallBack{
    @Override
    public void messageSuccess(String successMsg) {
        log.info(successMsg);
    }
    @Override
    public void messageError(String errorMsg) {
        log.error(errorMsg);
    }
}
