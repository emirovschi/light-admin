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
package org.lightadmin.core.web.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.Maps;
import org.lightadmin.core.config.domain.field.CustomFieldMetadata;
import org.lightadmin.core.config.domain.field.PersistentFieldMetadata;
import org.lightadmin.core.config.domain.field.TransientFieldMetadata;
import org.lightadmin.core.persistence.metamodel.CustomPersistentPropertyType;
import org.lightadmin.core.persistence.metamodel.CustomPersistentPropertyTypeLoader;
import org.lightadmin.core.persistence.metamodel.PersistentPropertyType;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import java.util.Map;
import java.util.TreeSet;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Optional.ofNullable;
import static org.lightadmin.core.persistence.metamodel.PersistentPropertyType.STRING;
import static org.lightadmin.core.persistence.metamodel.PersistentPropertyType.UNKNOWN;

@SuppressWarnings("unused")
@JsonPropertyOrder(value = {"name", "managed_type", "original_properties", "dynamic_properties"})
public class JsonConfigurationMetadata extends Resource<Map<String, JsonConfigurationMetadata.Property>> {

    private String name;
    private boolean managedDomainType;
    private static final TreeSet<String> lightAdminProperties = new TreeSet();

    static {
        lightAdminProperties.add("stringRepresentation");
        lightAdminProperties.add("managedDomainType");
        lightAdminProperties.add("primaryKey");
        lightAdminProperties.add("domainLink");
        lightAdminProperties.add("dynamicProperties");
    }

    private Map<String, Map<String, Property>> dynamicProperties;

    public JsonConfigurationMetadata(String name, boolean managedDomainType) {
        super(Maps.<String, JsonConfigurationMetadata.Property>newLinkedHashMap());

        this.name = name;
        this.managedDomainType = managedDomainType;
        this.dynamicProperties = newLinkedHashMap();
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("managed_type")
    public boolean isManagedDomainType() {
        return managedDomainType;
    }

    @JsonProperty("dynamic_properties")
    @JsonInclude(NON_EMPTY)
    public Map<String, Map<String, Property>> getDynamicProperties() {
        return dynamicProperties;
    }

    @Override
    @JsonProperty("original_properties")
    public Map<String, JsonConfigurationMetadata.Property> getContent() {
        return super.getContent();
    }

    public JsonConfigurationMetadata addAssociationProperty(Association association, Link restTemplateLink) {
        PersistentProperty persistentProperty = association.getInverse();

        addProperty(persistentProperty.getName(), newAssociationProperty(persistentProperty, persistentProperty.getName(), restTemplateLink));

        return this;
    }

    public JsonConfigurationMetadata addAssociationProperty(Association association, Link restTemplateLink, String unitType) {
        PersistentProperty persistentProperty = association.getInverse();
        String persistentPropertyName = persistentProperty.getName();

        addDynamicProperty(persistentPropertyName, newAssociationProperty(persistentProperty, persistentPropertyName, restTemplateLink), unitType);

        return this;
    }

    public JsonConfigurationMetadata addAssociationProperty(PersistentFieldMetadata persistentFieldMetadata, Link restTemplateLink, String unitType) {
        PersistentProperty persistentProperty = persistentFieldMetadata.getPersistentProperty().getAssociation().getInverse();
        String persistentPropertyName = persistentProperty.getName();
        String persistentPropertyTitle = persistentFieldMetadata.getName();

        addDynamicProperty(persistentPropertyName, newAssociationProperty(persistentProperty, persistentPropertyTitle, restTemplateLink), unitType);

        return this;
    }

    public JsonConfigurationMetadata addPersistentProperty(PersistentProperty persistentProperty) {
        String persistentPropertyName = persistentProperty.getName();

        if (!lightAdminProperties.contains(persistentPropertyName)) {
            addProperty(persistentPropertyName, newProperty(persistentProperty, persistentPropertyName));
        }

        return this;
    }

    public JsonConfigurationMetadata addPersistentProperty(PersistentProperty persistentProperty, String unitType) {
        String persistentPropertyName = persistentProperty.getName();

        addDynamicProperty(persistentPropertyName, newProperty(persistentProperty, persistentPropertyName), unitType);

        return this;
    }

    public JsonConfigurationMetadata addPersistentProperty(PersistentFieldMetadata persistentField, String unitType) {
        PersistentProperty persistentProperty = persistentField.getPersistentProperty();
        String persistentPropertyName = persistentProperty.getName();
        String persistentPropertyTitle = persistentField.getName();

        addDynamicProperty(persistentPropertyName, newProperty(persistentProperty, persistentPropertyTitle), unitType);

        return this;
    }

    public JsonConfigurationMetadata addDynamicProperty(TransientFieldMetadata transientField, String unitType) {
        Property property = new Property(transientField.getUuid(), transientField.getName(), UNKNOWN, false, false, null);

        addDynamicProperty(transientField.getUuid(), property, unitType);

        return this;
    }

    public JsonConfigurationMetadata addDynamicProperty(CustomFieldMetadata customField, String unitType) {
        Property property = new Property(customField.getUuid(), customField.getName(), STRING, false, false, null);

        addDynamicProperty(customField.getUuid(), property, unitType);

        return this;
    }

    private JsonConfigurationMetadata addDynamicProperty(String name, Property property, String configurationUnitType) {
        if (!dynamicProperties.containsKey(configurationUnitType)) {
            dynamicProperties.put(configurationUnitType, Maps.<String, Property>newLinkedHashMap());
        }
        if (!lightAdminProperties.contains(name)) {
            dynamicProperties.get(configurationUnitType).put(name, property);
        }
        return this;
    }

    private JsonConfigurationMetadata addProperty(String name, JsonConfigurationMetadata.Property property) {
        getContent().put(name, property);
        return this;
    }

    private JsonConfigurationMetadata.Property newAssociationProperty(PersistentProperty persistentProperty, String title, Link restTemplateLink) {
        PersistentPropertyType type = PersistentPropertyType.forPersistentProperty(persistentProperty);
        return new JsonConfigurationMetadata.AssociationProperty(persistentProperty.getName(), title, type, true, false, getCustomType(persistentProperty), restTemplateLink);
    }

    private JsonConfigurationMetadata.Property newProperty(PersistentProperty persistentProperty, String title) {
        PersistentPropertyType type = PersistentPropertyType.forPersistentProperty(persistentProperty);
        return new JsonConfigurationMetadata.Property(persistentProperty.getName(), title, type, true, persistentProperty.isIdProperty(), getCustomType(persistentProperty));
    }

    private String getCustomType(final PersistentProperty persistentProperty)
    {
        return ofNullable(CustomPersistentPropertyTypeLoader.of(persistentProperty))
                .map(CustomPersistentPropertyType::name)
                .orElse(null);
    }

    static class AssociationProperty extends JsonConfigurationMetadata.Property {

        private Link restLink;

        public AssociationProperty(String name, String title, PersistentPropertyType propertyType, boolean persistable, boolean primaryKey, String customType, Link restLink) {
            super(name, title, propertyType, persistable, primaryKey, customType);
            this.restLink = restLink;
        }

        @JsonProperty("rest_link")
        @JsonInclude(NON_NULL)
        public Link getRestLink() {
            return restLink;
        }
    }

    static class Property {

        private String name;
        private String title;
        private String type;
        private boolean persistable;
        private boolean primaryKey;
        private String customType;

        public Property(String name, String title, PersistentPropertyType propertyType, boolean persistable, boolean primaryKey, final String customType) {
            this.name = name;
            this.title = title;
            this.type = propertyType.name();
            this.persistable = persistable;
            this.primaryKey = primaryKey;
            this.customType = customType;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public String getType() {
            return type;
        }

        public boolean isPersistable() {
            return persistable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public String getCustomType()
        {
            return customType;
        }
    }
}
