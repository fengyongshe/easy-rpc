package com.fys.easyrpc.protocol;

import com.fys.easyrpc.util.JsonUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class RpcProtocol implements Serializable  {
  private static final long serialVersionUID = -1102180003395190700L;

  private String host;
  private int port;

  private List<RpcServiceInfo> serviceInfoList;

  public String toJson() {
    return JsonUtil.objectToJson(this);
  }

  private boolean isListEquals(List<RpcServiceInfo> thisList,
                               List<RpcServiceInfo> thatList) {
    if (thisList == null && thatList == null) {
      return true;
    }
    if ((thisList == null && thatList != null)
      || (thisList != null && thatList == null)
      || (thisList.size() != thatList.size())) {
      return false;
    }
    return thisList.containsAll(thatList) && thatList.containsAll(thisList);
  }

}
