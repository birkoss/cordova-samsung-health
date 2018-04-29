window.echo = function(str, callback) {
    cordova.exec(callback, function(err) {
        callback('Nothing to echo: ' + err);
    }, "SamsungHealth", "greet", [str]);
};

/*
module.exports = {
    greet: function (name, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "SamsungHealth8383", "greet", [name]);
    }
};*/