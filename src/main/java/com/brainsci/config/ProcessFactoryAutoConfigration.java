package com.brainsci.config;

import com.brainsci.form.BsciProcessor;
import com.brainsci.utils.BsciProcessorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(
        name = {"bsci.processor.enabled"},
        matchIfMissing = true
)
@ConfigurationProperties("bsci.processor")
public class ProcessFactoryAutoConfigration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${filesys.dir}")
    private String filedir;

    public void setHandlers(List<BsciProcessor> handlers) {
        BsciProcessorHandler.getProcessorHandler()
                .setHandlers(handlers)
                .setFiledir(filedir);
        logger.info("自动配置成功");
    }
}
