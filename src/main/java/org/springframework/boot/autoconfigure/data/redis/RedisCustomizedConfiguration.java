package org.springframework.boot.autoconfigure.data.redis;

import com.github.jojotech.spring.boot.starter.redis.related.conf.MultiRedisProperties;
import com.github.jojotech.spring.boot.starter.redis.related.lettuce.MultiRedisLettuceConnectionFactory;
import com.google.common.collect.Maps;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Map;

@ConditionalOnProperty(prefix = "spring.redis", value = "enable-multi", matchIfMissing = false)
@Configuration(proxyBeanMethods = false)
public class RedisCustomizedConfiguration {

    /**
     * @param builderCustomizers
     * @param clientResources
     * @param multiRedisProperties
     * @return
     * @see org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration
     */
    @Bean
    public MultiRedisLettuceConnectionFactory multiRedisLettuceConnectionFactory(
            ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
            ClientResources clientResources,
            MultiRedisProperties multiRedisProperties,
            ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
            ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider
    ) {
        Map<String, LettuceConnectionFactory> connectionFactoryMap = Maps.newHashMap();
        Map<String, RedisProperties> multi = multiRedisProperties.getMulti();
        multi.forEach((k, v) -> {
            LettuceConnectionConfiguration lettuceConnectionConfiguration = new LettuceConnectionConfiguration(
                    v,
                    sentinelConfigurationProvider,
                    clusterConfigurationProvider
            );
            LettuceConnectionFactory lettuceConnectionFactory = lettuceConnectionConfiguration.redisConnectionFactory(builderCustomizers, clientResources);
            connectionFactoryMap.put(k, lettuceConnectionFactory);
        });
        return new MultiRedisLettuceConnectionFactory(connectionFactoryMap);
    }

}
