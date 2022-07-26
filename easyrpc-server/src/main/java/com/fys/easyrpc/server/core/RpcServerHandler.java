package com.fys.easyrpc.server.core;

import com.fys.easyrpc.codec.Beat;
import com.fys.easyrpc.codec.RpcRequest;
import com.fys.easyrpc.codec.RpcResponse;
import com.fys.easyrpc.util.ServiceUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

  private final Map<String, Object> handlerMap;
  private final ThreadPoolExecutor executor;

  public RpcServerHandler(Map<String, Object> handlerMap, ThreadPoolExecutor executor) {
    this.handlerMap = handlerMap;
    this.executor = executor;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx,
                              RpcRequest request) throws Exception {
    if(Beat.BEAT_ID.equalsIgnoreCase(request.getRequestId())) {
      log.info("Heartbeat received !!!");
      return;
    }

    executor.execute( () -> {
      log.info("Receive Request: {}", request.getRequestId());
      RpcResponse.RpcResponseBuilder responseBuilder = RpcResponse.builder();
      responseBuilder.requestId(request.getRequestId());
      try {
        Object result = handle(request);
        responseBuilder.result(result);
      } catch (Throwable ex) {
        responseBuilder.error(ex.toString());
        log.error("Rpc Server handle Request with error: {}", executor);
      }

      ctx.writeAndFlush(responseBuilder.build())
         .addListener(
           new ChannelFutureListener() {
             @Override
             public void operationComplete(ChannelFuture channelFuture) throws Exception {
               log.info("Send response for request :{}" , request.getRequestId());
             }
           }
         );

    });
  }

  private Object handle(RpcRequest request) throws Throwable {
    String className = request.getClassName();
    String version = request.getVersion();
    String serviceKey = ServiceUtil.makeServiceKey(className, version);
    Object serviceBean = handlerMap.get(serviceKey);
    if(serviceBean == null) {
      log.info("Unsupport for the request with interface: {} and version:{}", className, version);
      return null;
    }
    Class<?> serviceClass = serviceBean.getClass();
    String methodName = request.getMethodName();
    Class<?>[]  parameterTypes = request.getParameterTypes();
    Object[] parameters = request.getParameters();

    log.info("Invoke ServiceName: {} with method", serviceClass.getName(), methodName);
    log.info("Invoke Parameters Type:{} wit value: {}", parameterTypes , parameters);

    Method method = serviceClass.getMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return method.invoke(serviceBean, parameters);

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.warn("Server caught exception: " + cause.getMessage());
    ctx.close();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      ctx.channel().close();
      log.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

}
