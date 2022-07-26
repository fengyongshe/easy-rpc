package com.fys.easyrpc.server.core;

import com.fys.easyrpc.codec.Beat;
import com.fys.easyrpc.codec.RpcDecoder;
import com.fys.easyrpc.codec.RpcEncoder;
import com.fys.easyrpc.codec.RpcRequest;
import com.fys.easyrpc.codec.RpcResponse;
import com.fys.easyrpc.serializer.KryoSerializer;
import com.fys.easyrpc.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcServerInitializer  extends ChannelInitializer<SocketChannel> {

  private Map<String,Object> handlerMap;
  private ThreadPoolExecutor executor;

  public RpcServerInitializer(Map<String, Object> handlerMap, ThreadPoolExecutor executor) {
    this.handlerMap = handlerMap;
    this.executor = executor;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {

    Serializer serializer = KryoSerializer.class.newInstance();
    ChannelPipeline pipeline = socketChannel.pipeline();
    pipeline.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
    pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
    pipeline.addLast(new RpcDecoder(RpcRequest.class, serializer));
    pipeline.addLast(new RpcEncoder(RpcResponse.class, serializer));
    pipeline.addLast(new RpcServerHandler(handlerMap, executor));
  }

}
