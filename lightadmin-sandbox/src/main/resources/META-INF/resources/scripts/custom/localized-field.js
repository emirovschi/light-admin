var LocalizedType = (function ()
{
    function saveInternal(resourceName, jsonForm, resolve)
     {
         console.log("start save");
         setTimeout(function() {
             console.log(resourceName, jsonForm);
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

        save: function(resourceName, jsonForm)
        {
            return new Promise(function(resolve) { saveInternal(resourceName, jsonForm, resolve); })
        },
    };
}());

ApplicationConfig.CUSTOM_EDITORS[LocalizedType.NAME] = LocalizedType;
