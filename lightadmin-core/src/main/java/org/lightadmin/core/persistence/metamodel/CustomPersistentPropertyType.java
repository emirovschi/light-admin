package org.lightadmin.core.persistence.metamodel;

import org.springframework.data.mapping.PersistentProperty;

public interface CustomPersistentPropertyType
{
    boolean is(final PersistentProperty persistentProperty);
}
