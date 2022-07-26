package com.fys.easyrpc.examples.client;

import com.fys.easyrpc.client.RpcClient;
import com.fys.easyrpc.client.connect.ConnectionManager;
import com.fys.easyrpc.examples.service.HelloService;
import com.fys.easyrpc.protocol.RpcProtocol;
import com.fys.easyrpc.protocol.RpcServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class RpcClientExample {

  public static void main(String[] args) throws Exception {
    RpcClient rpcClient = new RpcClient("127.0.0.1:18877");
    RpcProtocol protocol = new RpcProtocol();
    protocol.setHost("127.0.0.1");
    protocol.setPort(18877);
    protocol.setServiceInfoList(new ArrayList<RpcServiceInfo>());
    ConnectionManager.getInstance().connectServerNode(protocol);
    Thread.sleep(3000);
    final HelloService syncClient = rpcClient.createService(HelloService.class, "1.0");
    String result = syncClient.sayHi("Rpc Client");
    log.info("Response from server: {}", result);
  }

}
