package com.whatsnext.authapi.config;

import org.h2.tools.Server;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.sql.SQLException;

@Configuration
@Profile("test")
public class H2ServerConfig implements BeanFactoryPostProcessor, DisposableBean {

    private Server server;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            // Pre-create the in-memory DB with DB_CLOSE_DELAY=-1 before the TCP server starts.
            // H2 2.x refuses to create databases via remote TCP by default; a local connection
            // must initialize the named DB first so TCP clients can attach to it.
            java.sql.DriverManager.getConnection(
                "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "").close();
            server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start H2 TCP server on port 9092", e);
        }
    }

    @Override
    public void destroy() {
        if (server != null) {
            server.stop();
        }
    }
}