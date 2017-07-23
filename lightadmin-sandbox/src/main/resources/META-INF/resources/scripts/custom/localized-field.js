var LocalizedType = (function ()
{
    return {
        NAME: 'LOCALIZED',

        serialize: function(attrVal, property)
        {
            return attrVal.indexOf("test ") == 0 ? attrVal.substring(5, attrVal.length) : attrVal;
        },

        load: function(domainEntity, form, property)
        {
            console.log(domainEntity, form, property);
            form.find('[name="' + property['name'] + '"]').val("test " + domainEntity.getPropertyValue(property, 'formView').toString());
        },
    };
}());

ApplicationConfig.CUSTOM_EDITORS[LocalizedType.NAME] = LocalizedType;
