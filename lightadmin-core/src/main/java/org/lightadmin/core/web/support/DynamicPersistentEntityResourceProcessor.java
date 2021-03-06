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
package org.lightadmin.core.web.support;

import org.lightadmin.core.config.domain.DomainTypeAdministrationConfiguration;
import org.lightadmin.core.config.domain.DomainTypeBasicConfiguration;
import org.lightadmin.core.config.domain.GlobalAdministrationConfiguration;
import org.lightadmin.core.config.domain.field.FieldMetadata;
import org.lightadmin.core.config.domain.unit.DomainConfigurationUnitType;
import org.lightadmin.core.persistence.metamodel.PersistentPropertyType;
import org.lightadmin.core.storage.FileResourceStorage;
import org.lightadmin.core.util.Transformer;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.lightadmin.core.config.domain.configuration.support.ExceptionAwareTransformer.exceptionAwareNameExtractor;
import static org.lightadmin.core.config.domain.field.FieldMetadataUtils.customFields;
import static org.lightadmin.core.config.domain.field.FieldMetadataUtils.transientFields;
import static org.lightadmin.core.config.domain.unit.DomainConfigurationUnitType.FORM_VIEW;
import static org.lightadmin.core.config.domain.unit.DomainConfigurationUnitType.LIST_VIEW;
import static org.lightadmin.core.config.domain.unit.DomainConfigurationUnitType.QUICK_VIEW;
import static org.lightadmin.core.config.domain.unit.DomainConfigurationUnitType.SHOW_VIEW;
import static org.lightadmin.core.persistence.metamodel.PersistentPropertyType.FILE;

@SuppressWarnings(value = {"unchecked", "unused"})
public class DynamicPersistentEntityResourceProcessor implements ResourceProcessor<PersistentEntityResource> {

    private final GlobalAdministrationConfiguration adminConfiguration;
    private final DynamicRepositoryEntityLinks entityLinks;
    private final DomainEntityLinks domainEntityLinks;
    private final FileResourceStorage fileResourceStorage;
    private final Associations association;

    public DynamicPersistentEntityResourceProcessor(GlobalAdministrationConfiguration adminConfiguration, RepositoryRestConfiguration config,
                                                    FileResourceStorage fileResourceStorage, DynamicRepositoryEntityLinks entityLinks,
                                                    DomainEntityLinks domainEntityLinks, ResourceMappings resourceMappings) {
        this.adminConfiguration = adminConfiguration;
        this.domainEntityLinks = domainEntityLinks;
        this.entityLinks = entityLinks;
        this.fileResourceStorage = fileResourceStorage;
        this.association = new Associations(resourceMappings, config);
    }

    @Override
    public PersistentEntityResource process(PersistentEntityResource persistentEntityResource) {
        PersistentEntity persistentEntity = persistentEntityResource.getPersistentEntity();
        Object value = persistentEntityResource.getContent();

        String stringRepresentation = stringRepresentation(value, persistentEntity);
        Link domainLink = domainLink(persistentEntityResource);
        boolean managedDomainType = adminConfiguration.isManagedDomainType(persistentEntity.getType());
        String primaryKey = primaryKey(persistentEntity);

        Map<String, Map<String, Object>> dynamicProperties = dynamicPropertiesPerUnit(value, persistentEntity);

        final ManageableEntity manageableEntity = new ManageableEntity();
        manageableEntity.setDynamicProperties(dynamicProperties, stringRepresentation, domainLink, managedDomainType, primaryKey, null);

        final PersistentEntityResource.Builder builder = PersistentEntityResource.build(value, persistentEntity)
                .withEmbedded(singletonList(new ManageableEntityEmbeddedWrapper(manageableEntity)))
                .withLinks(persistentEntityResource.getLinks());

        return getBuildMethod(persistentEntityResource).apply(builder);
    }

    private Function<PersistentEntityResource.Builder, PersistentEntityResource> getBuildMethod(final PersistentEntityResource persistentEntityResource)
    {
        if (persistentEntityResource.isNested())
        {
            return PersistentEntityResource.Builder::buildNested;
        }
        else if(persistentEntityResource.isNew())
        {
            return PersistentEntityResource.Builder::forCreation;
        }
        else
        {
            return PersistentEntityResource.Builder::build;
        }
    }

    private String primaryKey(PersistentEntity persistentEntity) {
        return ofNullable(persistentEntity.getIdProperty()).map(PersistentProperty::getName).orElse(null);
    }

    private String stringRepresentation(Object value, PersistentEntity persistentEntity) {
        return ofNullable(adminConfiguration.forDomainType(persistentEntity.getType()))
                .map(this::getNameExtractor)
                .map(transformer -> transformer.apply(value))
                .orElse(null);
    }

    private Transformer<Object, String> getNameExtractor(final DomainTypeBasicConfiguration config)
    {
        return exceptionAwareNameExtractor(config.getEntityConfiguration().getNameExtractor(), config);
    }

    private Link domainLink(PersistentEntityResource persistentEntityResource) {
        PersistentEntity persistentEntity = persistentEntityResource.getPersistentEntity();
        if (domainEntityLinks.supports(persistentEntity.getType())) {
            return domainEntityLinks.linkFor(persistentEntityResource);
        }
        return null;
    }

    private Map<String, Map<String, Object>> dynamicPropertiesPerUnit(Object value, PersistentEntity persistentEntity) {
        if (!adminConfiguration.isManagedDomainType(persistentEntity.getType())) {
            return Collections.emptyMap();
        }

        DomainTypeAdministrationConfiguration managedDomainTypeConfiguration = adminConfiguration.forManagedDomainType(persistentEntity.getType());

        List<DomainConfigurationUnitType> units = newArrayList(LIST_VIEW, FORM_VIEW, SHOW_VIEW, QUICK_VIEW);

        List<PersistentProperty> persistentProperties = findPersistentFileProperties(persistentEntity);
        List<Association> associations = findLinkableAssociations(persistentEntity);

        Map<String, Map<String, Object>> dynamicPropertiesPerUnit = newHashMap();
        for (DomainConfigurationUnitType unit : units) {
            Map<String, Object> dynamicProperties = newLinkedHashMap();
            for (PersistentProperty persistentProperty : persistentProperties) {
                dynamicProperties.put(persistentProperty.getName(), filePropertyValue(persistentProperty, value));
            }
            for (Association association : associations) {
                dynamicProperties.put(association.getInverse().getName(), associationPropertyValue(association, value));
            }
            for (FieldMetadata customField : customFields(managedDomainTypeConfiguration.fieldsForUnit(unit))) {
                dynamicProperties.put(customField.getUuid(), customField.getValue(value));
            }
            for (FieldMetadata transientField : transientFields(managedDomainTypeConfiguration.fieldsForUnit(unit))) {
                dynamicProperties.put(transientField.getUuid(), transientField.getValue(value));
            }
            dynamicPropertiesPerUnit.put(unit.getName(), dynamicProperties);
        }
        return dynamicPropertiesPerUnit;
    }

    private Object associationPropertyValue(Association association, Object instance) {
        PersistentProperty persistentProperty = association.getInverse();
        PersistentEntity persistentEntity = persistentProperty.getOwner();

        if (persistentProperty.isMap()) {
            return null;
        }

        if (persistentProperty.isCollectionLike()) {
            return associatedPersistentEntities(association, instance);
        }

        Object associationValue = beanWrapper(instance).getPropertyValue(persistentProperty.getName());

        return associatedPersistentEntity(persistentProperty, associationValue);
    }

    private List<EmbeddedWrapper> associatedPersistentEntities(Association association, Object instance) {
        PersistentProperty persistentProperty = association.getInverse();

        Object associationValue = beanWrapper(instance).getPropertyValue(persistentProperty.getName());
        if (associationValue == null) {
            return null;
        }

        List<EmbeddedWrapper> result = newArrayList();

        if (persistentProperty.isArray()) {
            for (Object item : (Object[]) associationValue) {
                result.add(associatedPersistentEntity(persistentProperty, item));
            }
            return result;
        }

        for (Object item : (Iterable<Object>) associationValue) {
            result.add(associatedPersistentEntity(persistentProperty, item));
        }
        return result;
    }

    private EmbeddedWrapper associatedPersistentEntity(PersistentProperty persistentProperty, Object associationValue) {
        if (associationValue == null) {
            return null;
        }

        Class associationType = persistentProperty.getActualType();
        boolean managedDomainType = adminConfiguration.isManagedDomainType(associationType);
        PersistentEntity associationPersistentEntity = adminConfiguration.forDomainType(associationType).getPersistentEntity();

        String stringRepresentation = stringRepresentation(associationValue, associationPersistentEntity);
        String primaryKey = primaryKey(associationPersistentEntity);
        Object primaryKeyValue = beanWrapper(associationValue).getPropertyValue(primaryKey);

        Link domainLink = null;
        if (domainEntityLinks.supports(associationType)) {
            domainLink = domainEntityLinks.linkToSingleResource(associationType, primaryKeyValue);
        }

        final ManageableEntity entity = ManageableEntity.associatedPersistentEntity(stringRepresentation, managedDomainType, primaryKey, primaryKeyValue, domainLink);
        return new EmbeddedWrapper(new ManageableEntityEmbeddedWrapper(entity));
    }

    private static DirectFieldAccessFallbackBeanWrapper beanWrapper(Object instance) {
        return new DirectFieldAccessFallbackBeanWrapper(instance);
    }

    private List<PersistentProperty> findPersistentFileProperties(PersistentEntity persistentEntity) {
        final List<PersistentProperty> result = newArrayList();
        persistentEntity.doWithProperties(new SimplePropertyHandler() {
            @Override
            public void doWithPersistentProperty(PersistentProperty<?> property) {
                if (PersistentPropertyType.forPersistentProperty(property) == FILE) {
                    result.add(property);
                }
            }
        });
        return result;
    }

    private List<Association> findLinkableAssociations(PersistentEntity persistentEntity) {
        final List<Association> result = newArrayList();
        persistentEntity.doWithAssociations(new SimpleAssociationHandler() {
            @Override
            public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {
                if (DynamicPersistentEntityResourceProcessor.this.association.isLinkableAssociation(association.getInverse())) {
                    result.add(association);
                }
            }
        });
        return result;
    }

    private FilePropertyValue filePropertyValue(PersistentProperty persistentProperty, Object value) {
        try {
            if (!fileResourceStorage.fileExists(value, persistentProperty)) {
                return new FilePropertyValue(false);
            }
            return new FilePropertyValue(entityLinks.linkForFilePropertyLink(value, persistentProperty));
        } catch (Exception e) {
            return null;
        }
    }
}
