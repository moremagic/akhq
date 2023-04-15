package org.akhq.modules;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@InterceptorBean(CacheException.class)
public class CacheExceptionInterceptor implements MethodInterceptor<String, Map> {
    private static final Map<String, Map> cache = new ConcurrentHashMap();

    @Override
    public Map intercept(MethodInvocationContext<String, Map> context) {
        //Object配列をカンマ区切りの文字列にする
        Arrays.stream(context.getParameterValues()).map(Object::toString).reduce((s1, s2) -> s1 + "," + s2).ifPresent(System.out::println);

        String key = String.format("%s-%s-%s", context.getMethodName(), context.getParameterValues()[0].getClass().getName(), context.getParameterValues()[0].toString());
        System.out.println(String.format("==== cache instance: %s ====", cache.toString()));
        System.out.println(String.format("==== %s ====", key));

        if( cache.containsKey(key) ){
            System.out.println("==== Cache hit ====");
            return cache.get(key);
        }

        try {
            System.out.println("==== Cache No hit ====");
            Map ret = context.proceed();

            if(ret.isEmpty()){
                cache.put(key , ret);
            }

            return ret;
        }catch (Exception e){
            throw e;
        }
    }
}
