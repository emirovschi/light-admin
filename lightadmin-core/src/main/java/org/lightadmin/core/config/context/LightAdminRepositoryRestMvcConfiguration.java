/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lightadmin.core.config.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lightadmin.core.config.LightAdminConfiguration;
import org.lightadmin.core.config.bootstrap.RepositoriesFactoryBean;
import org.lightadmin.core.config.domain.GlobalAdministrationConfiguration;
import org.lightadmin.core.persistence.metamodel.CustomPersistentPropertyTypeLoader;
import org.lightadmin.core.persistence.repository.event.FileManipulationRepositoryEventListener;
import org.lightadmin.core.persistence.repository.invoker.DynamicRepositoryInvokerFactory;
import org.lightadmin.core.persistence.support.DynamicDomainObjectMerger;
import org.lightadmin.core.storage.FileResourceStorage;
import org.lightadmin.core.web.json.DomainTypeToJsonMetadataConverter;
import org.lightadmin.core.web.json.LightAdminJacksonModule;
import org.lightadmin.core.web.support.ConfigurationHandlerMethodArgumentResolver;
import org.lightadmin.core.web.support.DomainEntityLinks;
import org.lightadmin.core.web.support.DynamicPersistentEntityResourceProcessor;
import org.lightadmin.core.web.support.DynamicRepositoryEntityLinks;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.support.DomainObjectMerger;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.LinkedList;
import java.util.List;

import static org.springframework.beans.PropertyAccessorFactory.forDirectFieldAccess;

@Configuration
@ComponentScan(basePackages = {"org.lightadmin.core.web"},
        includeFilters = @ComponentScan.Filter(RepositoryRestController.class), useDefaultFilters = false)
public class LightAdminRepositoryRestMvcConfiguration extends RepositoryRestMvcConfiguration {

    @Autowired
    private ListableBeanFactory beanFactory;

    @Bean
    public DomainEntityLinks domainEntityLinks() {
        return new DomainEntityLinks(globalAdministrationConfiguration(), backendIdConverterRegistry(), lightAdminConfiguration());
    }

    @Bean
    public DynamicRepositoryEntityLinks dynamicRepositoryEntityLinks() {
        return DynamicRepositoryEntityLinks.wrap(super.entityLinks());
    }

    @Bean
    @Autowired
    public DynamicPersistentEntityResourceProcessor dynamicPersistentEntityResourceProcessor(RepositoryRestConfiguration repositoryConfiguration) {
        return new DynamicPersistentEntityResourceProcessor(globalAdministrationConfiguration(), repositoryConfiguration,
                fileResourceStorage(), dynamicRepositoryEntityLinks(), domainEntityLinks(), resourceMappings());
    }

    @Bean
    public DomainTypeToJsonMetadataConverter domainTypeToJsonMetadataConverter() {
        return new DomainTypeToJsonMetadataConverter(globalAdministrationConfiguration(), entityLinks());
    }

    @Bean
    public Repositories repositories() {
        try {
            return new RepositoriesFactoryBean(beanFactory).getObject();
        } catch (Exception e) {
            throw new BeanInstantiationException(Repositories.class, "Repositories bean instantiation problem!", e);
        }
    }

    @Bean
    public DomainObjectMerger domainObjectMerger() throws Exception {
        return new DynamicDomainObjectMerger(repositories(), defaultConversionService(), globalAdministrationConfiguration());
    }

    @Bean
    public RepositoryInvokerFactory repositoryInvokerFactory(@Qualifier ConversionService defaultConversionService) {
        RepositoryInvokerFactory repositoryInvokerFactory = super.repositoryInvokerFactory(defaultConversionService);

        return new DynamicRepositoryInvokerFactory(repositories(), repositoryInvokerFactory);
    }

    @Bean
    public ConfigurationHandlerMethodArgumentResolver configurationHandlerMethodArgumentResolver() {
        return new ConfigurationHandlerMethodArgumentResolver(globalAdministrationConfiguration(), resourceMetadataHandlerMethodArgumentResolver());
    }

    @Bean
    @Autowired
    public FileManipulationRepositoryEventListener domainRepositoryEventListener(GlobalAdministrationConfiguration configuration, FileResourceStorage fileResourceStorage) {
        return new FileManipulationRepositoryEventListener(configuration, fileResourceStorage);
    }

    @Bean
    @Autowired
    public CustomPersistentPropertyTypeLoader customPersistentPropertyTypeLoader(final LightAdminConfiguration configuration)
    {
        return new CustomPersistentPropertyTypeLoader(configuration);
    }

    @Override
    public RepositoryRestConfiguration config()
    {
        final RepositoryRestConfiguration config = super.config();
        config.setDefaultPageSize(10);
        config.setBasePath(lightAdminConfiguration().getApplicationRestBaseUrl().toString());
        config.exposeIdsFor(globalAdministrationConfiguration().getAllDomainTypesAsArray());
        config.setReturnBodyOnCreate(true);
        config.setReturnBodyOnUpdate(true);
        return config;
    }

    @Override
    public RequestMappingHandlerAdapter repositoryExporterHandlerAdapter() {
        RequestMappingHandlerAdapter requestMappingHandlerAdapter = super.repositoryExporterHandlerAdapter();
        configureRepositoryExporterHandlerAdapter(requestMappingHandlerAdapter);
        return requestMappingHandlerAdapter;
    }

    @Override
    public ValidatingRepositoryEventListener validatingRepositoryEventListener(final ObjectFactory<PersistentEntities> entities)
    {
        final ValidatingRepositoryEventListener validatingListener = super.validatingRepositoryEventListener(entities);
        validatingListener.addValidator("beforeCreate", validator());
        validatingListener.addValidator("beforeSave", validator());
        return validatingListener;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
        argumentResolvers.add(configurationHandlerMethodArgumentResolver());
    }

    @Override
    public ObjectMapper objectMapper()
    {
        final ObjectMapper objectMapper = super.objectMapper();
        objectMapper.registerModule(new LightAdminJacksonModule(globalAdministrationConfiguration()));
        return objectMapper;
    }

    @SuppressWarnings("unchecked")
    private void configureRepositoryExporterHandlerAdapter(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        List<HandlerMethodArgumentResolver> defaultArgumentResolvers = (List<HandlerMethodArgumentResolver>) forDirectFieldAccess(requestMappingHandlerAdapter).getPropertyValue("argumentResolvers");

        List<HandlerMethodArgumentResolver> argumentResolvers = new LinkedList<>(defaultArgumentResolvers);

        argumentResolvers.add(configurationHandlerMethodArgumentResolver());

        forDirectFieldAccess(requestMappingHandlerAdapter).setPropertyValue("argumentResolvers", argumentResolvers);
    }

    private GlobalAdministrationConfiguration globalAdministrationConfiguration() {
        return beanFactory.getBean(GlobalAdministrationConfiguration.class);
    }

    private FileResourceStorage fileResourceStorage() {
        return beanFactory.getBean(FileResourceStorage.class);
    }

    private Validator validator() {
        return beanFactory.getBean("validator", Validator.class);
    }

    private LightAdminConfiguration lightAdminConfiguration() {
        return beanFactory.getBean(LightAdminConfiguration.class);
    }
}