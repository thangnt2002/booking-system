package com.booking.identityservice.configuration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlywayConfiguration {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String table;

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .table(table)
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    static BeanFactoryPostProcessor flywayBeforeJpa() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] dependencies = entityManagerFactory.getDependsOn();
            String[] updatedDependencies = new String[(dependencies == null ? 0 : dependencies.length) + 1];

            if (dependencies != null) {
                System.arraycopy(dependencies, 0, updatedDependencies, 0, dependencies.length);
            }
            updatedDependencies[updatedDependencies.length - 1] = "flyway";
            entityManagerFactory.setDependsOn(updatedDependencies);
        };
    }
}
