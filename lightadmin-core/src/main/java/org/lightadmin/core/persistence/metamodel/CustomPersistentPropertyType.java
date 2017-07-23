package org.lightadmin.core.persistence.metamodel;

import org.lightadmin.core.view.editor.JspFragmentFieldControl;
import org.springframework.data.mapping.PersistentProperty;

public interface CustomPersistentPropertyType
{
    boolean is(final PersistentProperty persistentProperty);

    String script();

    JspFragmentFieldControl editor();
}
