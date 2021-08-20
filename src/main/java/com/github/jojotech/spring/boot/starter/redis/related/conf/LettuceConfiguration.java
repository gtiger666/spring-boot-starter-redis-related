package com.github.jojotech.spring.boot.starter.redis.related.conf;

import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.metrics.DefaultCommandLatencyCollector;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class LettuceConfiguration {
    /**
     * 每 10s 采集一次命令统计
     *
     * @return
     */
    @Bean
    public DefaultClientResources getDefaultClientResources() {
        DefaultClientResources build = DefaultClientResources.builder()
                .commandLatencyRecorder(
                        new DefaultCommandLatencyCollector(
                                // define collector
                                DefaultCommandLatencyCollectorOptions.builder().enable().resetLatenciesAfterEvent(true).build()
                        )
                )
                .commandLatencyPublisherOptions(
                        //每 10s 采集一次命令统计
                        DefaultEventPublisherOptions.builder().eventEmitInterval(Duration.ofSeconds(10)).build()
                ).build();
        return build;
    }
}
