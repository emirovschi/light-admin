var LocalizedType = (function ()
{
    return {
        NAME: 'LOCALIZED',

        serialize: function(attrVal, property)
        {
            return attrVal;
        },
    };
}());

ApplicationConfig.CUSTOM_EDITORS[LocalizedType.NAME] = LocalizedType;
