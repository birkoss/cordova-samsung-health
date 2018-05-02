var exec = cordova.require('cordova/exec');

var SamsungHealth = function() { };

SamsungHealth.prototype.debug = function(name, onSuccess, onError) {
    var errorCallback = function(obj) { onError(obj); };
    var successCallback = function(obj) { onSuccess(obj); };

    exec(successCallback, errorCallback, 'SamsungHealth', 'debug', [name]);
};

SamsungHealth.prototype.askPermissions = function(permissions, onSuccess, onError) {
    var errorCallback = function(obj) { onError(obj); };
    var successCallback = function(obj) { onSuccess(obj); };
alert("...");
    exec(successCallback, errorCallback, 'SamsungHealth', 'askPermissions', [permissions]);
};

SamsungHealth.prototype.getData = function(name, onSuccess, onError) {
    var errorCallback = function(obj) { onError(obj); };
    var successCallback = function(obj) { onSuccess(obj); };

    exec(successCallback, errorCallback, 'SamsungHealth', 'getData', [name]);
};

if (typeof module != 'undefined' && module.exports) {
    module.exports = SamsungHealth;
}