package com.fys.easyrpc.client;

import com.fys.easyrpc.client.connect.ConnectionManager;
import com.fys.easyrpc.client.proxy.ObjectProxy;
import com.fys.easyrpc.client.proxy.RpcService;
import com.fys.easyrpc.common.RpcAutowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcClient implements ApplicationContextAware, DisposableBean {

  private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
    600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

  public RpcClient(String address) {
  }

  @SuppressWarnings("unchecked")
  public  <T, P> T createService(Class<T> interfaceClass, String version) {
    return (T) Proxy.newProxyInstance(
      interfaceClass.getClassLoader(),
      new Class<?>[]{interfaceClass},
      new ObjectProxy<T, P>(interfaceClass, version)
    );
  }

  public static <T, P> RpcService createAsyncService(Class<T> interfaceClass, String version) {
    return new ObjectProxy<T, P>(interfaceClass, version);
  }

  public static void submit(Runnable task) {
    threadPoolExecutor.submit(task);
  }

  public void stop() {
    threadPoolExecutor.shutdown();
    ConnectionManager.getInstance().stop();
  }

  @Override
  public void destroy() throws Exception {
    this.stop();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    String[] beanNames = applicationContext.getBeanDefinitionNames();
    for (String beanName : beanNames) {
      Object bean = applicationContext.getBean(beanName);
      Field[] fields = bean.getClass().getDeclaredFields();
      try {
        for (Field field : fields) {
          RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
          if (rpcAutowired != null) {
            String version = rpcAutowired.version();
            field.setAccessible(true);
            field.set(bean, createService(field.getType(), version));
          }
        }
      } catch (IllegalAccessException e) {
        log.error(e.toString());
      }
    }
  }

}
