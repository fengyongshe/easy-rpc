package com.fys.easyrpc.examples.server;

import com.fys.easyrpc.examples.service.HelloService;
import com.fys.easyrpc.examples.service.HelloServiceImpl;
import com.fys.easyrpc.server.core.EasyRpcServer;

public class RpcServerExample {

  public static void main(String[] args) {
    String serverAddr = "127.0.0.1:18877";
    EasyRpcServer rpcServer = new EasyRpcServer(serverAddr);
    rpcServer.addService(HelloService.class.getName(), "1.0", new HelloServiceImpl());
    rpcServer.start();
  }

}
