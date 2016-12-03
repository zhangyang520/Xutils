package com.lidroid.xutils.exception;

/**
 * Created by zhangyang on 2016/11/30 17 07 V1.0.
 * http请求异常的状态值枚举类
 */
public enum HttpExceptionState {
    IP_CONNECT_ERROR("ip连接失败,可能是位置的host主机,可能是连接主机异常等非超时异常!"),
    CONNECT_TIME_OUT("ip连接连接超时,套接字连接超时异常!"),
    READ_DATA_ERROR("从服务端获取数据失败!"),
    OTHER_EXCEPTION("其他类型的异常");
    String errorDes;

     HttpExceptionState(String errorDes) {
        this.errorDes=errorDes;
    }
}
