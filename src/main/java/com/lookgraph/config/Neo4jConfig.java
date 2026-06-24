package com.lookgraph.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@org.springframework.context.annotation.Configuration
@EnableNeo4jRepositories(
    basePackages = "com.lookgraph.domain.repository.neo4j",
    transactionManagerRef = "neo4jTransactionManager"
)
@EnableJpaRepositories(
    basePackages = "com.lookgraph.domain.repository.jpa",
    transactionManagerRef = "jpaTransactionManager"
)
@EnableTransactionManagement
public class Neo4jConfig {

    @Bean
    public Configuration cypherDslConfiguration() {
        return Configuration.newConfig()
                .withDialect(Dialect.NEO4J_5)
                .build();
    }

    @Bean(name = "neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }

    @Bean(name = "jpaTransactionManager")
    @Primary
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
