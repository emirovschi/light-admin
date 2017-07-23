package org.lightadmin.core.persistence.metamodel;

import org.lightadmin.core.config.bootstrap.scanning.TypeFilterClassScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class CustomPersistentPropertyTypeClassScanner extends TypeFilterClassScanner
{
    public CustomPersistentPropertyTypeClassScanner() {
        super(new AssignableTypeFilter(CustomPersistentPropertyType.class));
    }
}
