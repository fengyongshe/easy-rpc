package com.fys.easyrpc.codec;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class RpcResponse implements Serializable {

  private static final long serialVersionUID = 8215493329459772524L;

  private String requestId;
  private String error;
  private Object result;

  public boolean isError() {
    return error != null;
  }

}
