var exec = cordova.require('cordova/exec');

var SamsungHealth = function() {
};

SamsungHealth.prototype.greet = function(name, onSuccess, onError) {
    var errorCallback = function(obj) {
        onError(obj);
    };

    var successCallback = function(obj) {
        onSuccess(obj);
    };

    exec(successCallback, errorCallback, 'SamsungHealth', 'greet', [name]);
};

SamsungHealth.prototype.connect = function(name, onSuccess, onError) {
    var errorCallback = function(obj) {
        onError(obj);
    };

    var successCallback = function(obj) {
        onSuccess(obj);
    };

    exec(successCallback, errorCallback, 'SamsungHealth', 'connect', [name]);
};

if (typeof module != 'undefined' && module.exports) {
    module.exports = SamsungHealth;
}