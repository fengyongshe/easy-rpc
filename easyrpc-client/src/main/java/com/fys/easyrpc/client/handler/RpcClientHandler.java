package com.fys.easyrpc.client.handler;

import com.fys.easyrpc.client.connect.ConnectionManager;
import com.fys.easyrpc.codec.Beat;
import com.fys.easyrpc.codec.RpcRequest;
import com.fys.easyrpc.codec.RpcResponse;
import com.fys.easyrpc.protocol.RpcProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

  private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();
  private volatile Channel channel;
  private SocketAddress remotePeer;
  private RpcProtocol rpcProtocol;

  public void setRpcProtocol(RpcProtocol rpcProtocol) {
    this.rpcProtocol = rpcProtocol;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                              RpcResponse response) throws Exception {
    String requestId = response.getRequestId();
    log.info("Receive response: " + requestId);
    RpcFuture rpcFuture = pendingRPC.get(requestId);
    if(rpcFuture != null) {
      pendingRPC.remove(requestId);
      rpcFuture.done(response);
    } else {
      log.warn("Can't get pending response for request:{}", requestId);
    }
  }

  public RpcFuture sendRequest(RpcRequest request) {
    log.info("Request send to rpc Server: {}", request);
    RpcFuture rpcFuture = new RpcFuture(request);
    pendingRPC.put(request.getRequestId(), rpcFuture);
    try {
      ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
      if (!channelFuture.isSuccess()) {
        log.error("Send request {} error", request.getRequestId());
      }
    } catch (InterruptedException e) {
      log.error("Send request exception: " + e.getMessage());
    }

    return rpcFuture;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    this.remotePeer = this.channel.remoteAddress();
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    super.channelRegistered(ctx);
    this.channel = ctx.channel();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Client caught exception: " + cause.getMessage());
    ctx.close();
  }


  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      //Send ping
      sendRequest(Beat.BEAT_PING);
      log.debug("Client send beat-ping to " + remotePeer);
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }


  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    ConnectionManager.getInstance().removeHandler(rpcProtocol);
  }

  public void close() {
    channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
  }


}
