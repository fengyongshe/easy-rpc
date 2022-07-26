package com.fys.easyrpc.protocol;

import com.fys.easyrpc.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class RpcServiceInfo implements Serializable  {

  private String serviceName;
  private String version;

  @Override
  public String toString() {
    return JsonUtil.objectToJson(this);
  }

}
