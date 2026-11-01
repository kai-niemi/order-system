package se.cloudneutral.orders.config;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DataSourceConfig {
    public static final String SQL_TRACE_LOGGER = "se.cloudneutral.orders.SQL_TRACE";

    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSource primaryDataSource() {
        LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy();
        proxy.setTargetDataSource(loggingProxy(targetDataSource()));
        proxy.setDefaultAutoCommit(true);
        return proxy;
    }

    private DataSource loggingProxy(DataSource dataSource) {
        final Formatter formatterHighlight = FormatStyle.HIGHLIGHT.getFormatter();

        DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator() {
            @Override
            protected String formatQuery(String query) {
                return formatterHighlight.format(query);
            }
        };
        creator.setMultiline(false);

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setQueryLogEntryCreator(creator);
        listener.setLogger(SQL_TRACE_LOGGER);
        listener.setLogLevel(SLF4JLogLevel.TRACE);
        listener.setWriteConnectionId(true);
        listener.setWriteIsolation(true);

        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL-Trace")
                .asJson()
                .listener(listener)
                .build();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource targetDataSource() {
        HikariDataSource ds = dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setAutoCommit(true); // this is default
        return ds;
    }
}
