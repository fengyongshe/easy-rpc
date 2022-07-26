package com.fys.easyrpc.client.handler;

public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);

}
