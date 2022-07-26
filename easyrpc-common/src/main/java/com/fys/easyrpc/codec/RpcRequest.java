package com.fys.easyrpc.codec;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RpcRequest implements Serializable {

  private static final long serialVersionUID = -2524587347775862771L;

  private String requestId;
  private String className;
  private String methodName;
  private Class<?>[] parameterTypes;
  private Object[] parameters;
  private String version;

}
