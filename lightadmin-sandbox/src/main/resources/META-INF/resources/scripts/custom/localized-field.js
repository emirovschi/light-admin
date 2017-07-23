var LocalizedType = (function ()
{
    function saveInternal(resourceName, entityId, domForm, property)
     {
         console.log("start save");
         setTimeout(function() {
             console.log(resourceName, domForm, property);
             resolve(undefined);
         }, 500);
     }

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

        save: function(resourceName, entityId, domForm, property)
        {
            return new Promise(function(resolve) { saveInternal(resourceName, entityId, domForm, property); })
        },
    };
}());

ApplicationConfig.CUSTOM_EDITORS[LocalizedType.NAME] = LocalizedType;
