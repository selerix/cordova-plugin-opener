//////////////////////////////////////////
// Cache.js
// Copyright (C) 2019 Selerix Systems Inc.
//
//////////////////////////////////////////
var exec = require('cordova/exec');

var Opener =
{
    open : function(url, success, error )
    {
        exec(success, error, "Opener", "open", [url])
    }
}

module.exports = Opener;
