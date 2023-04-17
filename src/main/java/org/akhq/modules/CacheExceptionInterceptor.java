package org.akhq.modules;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@InterceptorBean(CacheException.class)
public class CacheExceptionInterceptor implements MethodInterceptor<String, Map> {
    private final Logger LOG = LoggerFactory.getLogger(CacheExceptionInterceptor.class);
    private static final Map<String, ApiException> cache = new ConcurrentHashMap();

    @Override
    public Map intercept(MethodInvocationContext<String, Map> context) {
        String key = String.format("%s-%s",
            context.getMethodName(),
            Arrays.stream(context.getParameterValues()).map(Object::toString).reduce((s1, s2) -> s1 + "/" + s2));
        LOG.trace("Exception cache key: {}", key);

        if( cache.containsKey(key) && cache.get(key) instanceof UnsupportedVersionException){
            LOG.trace("Cache hit: {}, return empty hashmap.", key);
            return new HashMap();
        }

        try {
            LOG.trace("Cache no hit: {}", key);
            return context.proceed();
        }catch (Exception e){
            if( e.getCause() instanceof ApiException ){
                LOG.trace("Exception cacheing key: {} value: {}", key, e.getCause());
                cache.put(key, (ApiException) e.getCause());
                if( e instanceof UnsupportedVersionException){
                    return new HashMap();
                }
            }
            throw e;
        }
    }
}
