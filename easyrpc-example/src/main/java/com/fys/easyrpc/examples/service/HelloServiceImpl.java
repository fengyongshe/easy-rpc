package com.fys.easyrpc.examples.service;

public class HelloServiceImpl implements HelloService {
  @Override
  public String sayHi(String msg) {
    return "Msg From Server:" + msg;
  }

}
