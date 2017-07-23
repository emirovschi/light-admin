package org.lightadmin.demo.config;

import org.lightadmin.api.config.utils.Editors;
import org.lightadmin.core.persistence.metamodel.CustomPersistentPropertyType;
import org.lightadmin.core.view.editor.JspFragmentFieldControl;
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

    @Override
    public JspFragmentFieldControl editor()
    {
        return Editors.textArea();
    }

    @Override
    public String name()
    {
        return "LOCALIZED";
    }
}
