package com.fys.easyrpc.client.connect;

import com.fys.easyrpc.client.handler.RpcClientHandler;
import com.fys.easyrpc.client.handler.RpcClientInitializer;
import com.fys.easyrpc.protocol.RpcProtocol;
import com.fys.easyrpc.protocol.RpcServiceInfo;
import com.fys.easyrpc.util.ServiceUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.HelloWorld;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private RpcProtocol protocol ;
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private volatile boolean isRunning = true;

    private ConnectionManager() {
    }

    private static class SingletonHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        RpcClientHandler handler = connectedServerNodes.get(this.protocol);

        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }

    public void connectServerNode(RpcProtocol rpcProtocol) {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            //return;
            rpcProtocol.getServiceInfoList().add(new RpcServiceInfo(
              ServiceUtil.makeServiceKey(HelloWorld.class.getName(),"1.0"),
              "1.0"));
        }
        this.protocol = rpcProtocol;
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup)
          .channel(NioSocketChannel.class)
          .handler(new RpcClientInitializer());

        ChannelFuture channelFuture = b.connect(remotePeer);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
                    RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                    connectedServerNodes.put(rpcProtocol, handler);
                    handler.setRpcProtocol(rpcProtocol);
                    signalAvailableHandler();
                } else {
                    logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                }
            }
        });
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    private void removeAndCloseHandler(RpcProtocol rpcProtocol) {
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(rpcProtocol);
    }

    public void removeHandler(RpcProtocol rpcProtocol) {
        connectedServerNodes.remove(rpcProtocol);
        logger.info("Remove one connection, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
    }

    public void stop() {
        isRunning = false;
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
