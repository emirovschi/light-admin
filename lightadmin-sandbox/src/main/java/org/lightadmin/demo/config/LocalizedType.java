package org.lightadmin.demo.config;

import org.lightadmin.core.persistence.metamodel.CustomPersistentPropertyType;
import org.lightadmin.demo.model.Localized;
import org.springframework.data.mapping.PersistentProperty;

public class LocalizedType implements CustomPersistentPropertyType
{
    @Override
    public boolean is(final PersistentProperty persistentProperty)
    {
        return persistentProperty.getField().getAnnotation(Localized.class) != null;
    }

    @Override
    public String script()
    {
        return "/scripts/custom/localized-field.js";
    }
}
