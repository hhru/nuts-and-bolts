package ru.hh.nab.datasource;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.jdbc.NabJdbcCommonConfig;

@Configuration
@Import({
    NabJdbcCommonConfig.class,
})
public class NabDataSourceCommonConfig {
}
