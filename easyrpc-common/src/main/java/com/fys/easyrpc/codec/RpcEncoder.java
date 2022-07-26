package com.fys.easyrpc.codec;

import com.fys.easyrpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcEncoder extends MessageToByteEncoder {

  private Class<?> genericClass;
  private Serializer serializer;

  public RpcEncoder(Class<?> genericClass, Serializer serializer) {
    this.genericClass = genericClass;
    this.serializer = serializer;
  }

  @Override
  protected void encode(ChannelHandlerContext channelHandlerContext,
                        Object in,
                        ByteBuf out) throws Exception {
    if(genericClass.isInstance(in)) {
      try {
        byte[] data = serializer.serialize(in);
        out.writeInt(data.length);
        out.writeBytes(data);
      } catch (Exception ex) {
        log.error("Message Encode with exception: {}", ex.getMessage());
      }
    }
  }
}
