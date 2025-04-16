package ru.hh.nab.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.datasource.NabDataSourceProdConfig;

@Configuration
@Import({
    NabJpaCommonConfig.class,
    NabDataSourceProdConfig.class,
})
public class NabJpaProdConfig {
}
