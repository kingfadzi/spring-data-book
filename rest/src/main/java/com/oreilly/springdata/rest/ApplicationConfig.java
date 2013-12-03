/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oreilly.springdata.rest;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.cloudfoundry.runtime.env.CloudEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring JavaConfig configuration class to setup a Spring container and infrastructure components like a
 * {@link DataSource}, a {@link EntityManagerFactory} and a {@link PlatformTransactionManager}.
 * 
 * @author Oliver Gierke
 */
@Configuration
@ComponentScan
@EnableJpaRepositories
@EnableTransactionManagement
//@PropertySource("classpath:/META-INF/spring/database.properties")
class ApplicationConfig {

	
	String host;
	String username;
	String password;
	String port;
	String database;
	
	@Bean
    public CloudEnvironment cloudEnvironment() {
        return new CloudEnvironment();
    }
	 
	@Bean
	public DataSource dataSource() {
	
		List<Map<String, Object>>  services = this.cloudEnvironment().getServices();
		Iterator<Map<String, Object>> servicesIter = services.iterator();
		
		
		while(servicesIter.hasNext()){
			Map<String, Object> map = servicesIter.next();
			Set<String> keys = map.keySet();
			
			for(String key : keys){
				
				if(key.equals("credentials")){

					System.out.println("key" +" - value: "+map.get(key));

					Map<String, Object> credentials = (Map<String, Object>) map.get(key);
					
					host = (String) credentials.get("host");
					username = (String) credentials.get("username");
					password = (String) credentials.get("password");
					port = (String) credentials.get("port");
					database = (String) credentials.get("database");
					
				}
			}
		}
		
		DataSource ds = new org.apache.commons.dbcp.BasicDataSource();
		  ((BasicDataSource) ds).setDriverClassName("org.postgresql.Driver");
		  ((BasicDataSource) ds).setUrl("jdbc:postgresql://" +host + ":" + port +"/"+database);
		  ((BasicDataSource) ds).setUsername(username);
		  ((BasicDataSource) ds).setPassword(password);
		  
		  ((BasicDataSource) ds).setInitialSize(5);
		  ((BasicDataSource) ds).setMaxActive(10);
		  ((BasicDataSource) ds).setMaxIdle(2);
		  
		  return ds;
		
	}

	/**
	 * Sets up a {@link LocalContainerEntityManagerFactoryBean} to use Hibernate. Activates picking up entities from the
	 * project's base package.
	 * 
	 * @return
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setDatabase(Database.POSTGRESQL);
		vendorAdapter.setGenerateDdl(true);

		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setJpaVendorAdapter(vendorAdapter);
		factory.setPackagesToScan(getClass().getPackage().getName());
		factory.setDataSource(dataSource());

		return factory;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {

		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return txManager;
	}
}
