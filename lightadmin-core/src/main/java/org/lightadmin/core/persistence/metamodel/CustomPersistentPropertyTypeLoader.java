package org.lightadmin.core.persistence.metamodel;

import org.lightadmin.core.config.LightAdminConfiguration;
import org.springframework.data.mapping.PersistentProperty;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class CustomPersistentPropertyTypeLoader
{
    private static List<CustomPersistentPropertyType> persistentPropertyTypeList = new ArrayList<>();

    public CustomPersistentPropertyTypeLoader(final LightAdminConfiguration configuration)
    {
        persistentPropertyTypeList = new CustomPersistentPropertyTypeClassScanner()
                .scan(configuration.getBasePackage())
                .stream()
                .map(this::createInstance)
                .collect(toList());
    }

    private CustomPersistentPropertyType createInstance(final Class typeClass)
    {
        try
        {
            return (CustomPersistentPropertyType) typeClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new IllegalArgumentException("Invalid property type class");
        }
    }

    public static boolean is(final PersistentProperty persistentProperty)
    {
        return persistentPropertyTypeList.stream().anyMatch(type -> type.is(persistentProperty));
    }

    public static CustomPersistentPropertyType of(final PersistentProperty persistentProperty)
    {
        return persistentPropertyTypeList.stream().filter(type -> type.is(persistentProperty)).findFirst().orElse(null);
    }
}
