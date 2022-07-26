package com.fys.easyrpc.server.core;

import com.fys.easyrpc.util.ServiceUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EasyRpcServer implements Server {

  private String serverAddress;
  private Map<String, Object> serviceMap = new HashMap<>();

  public Thread thread;

  public EasyRpcServer(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public void addService(String interfaceName, String version, Object serviceBean) {
    log.info("Adding Service with interface:{}, version:{}, bean:{}",
      interfaceName, version, serviceBean);
    String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
    serviceMap.put(serviceKey, serviceBean);
  }

  @Override
  public void start() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
      16,
      32,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<Runnable>(1000),
      new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "netty-rpc-" + EasyRpcServer.class.getSimpleName() + "-" + r.hashCode());
        }
      },
      new ThreadPoolExecutor.AbortPolicy());

    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup= new NioEventLoopGroup();
        try {
          ServerBootstrap bootstrap = new ServerBootstrap();
          bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
            .childHandler(new RpcServerInitializer(serviceMap, executor))
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
          String[] array = serverAddress.split(":");
          String host = array[0];
          int port = Integer.parseInt(array[1]);
          ChannelFuture future = bootstrap.bind(host, port).sync();
          log.info("Server started on port:{}", port);
          future.channel().closeFuture().sync();
        } catch (Exception ex) {
          log.error("Rpc Server failed to start with exception:{}", ex);
        } finally {
          workerGroup.shutdownGracefully();
          bossGroup.shutdownGracefully();
        }
      }
    });
    thread.start();
  }

  @Override
  public void stop() {
    if (thread != null && thread.isAlive()) {
      thread.interrupt();
    }
  }

}
