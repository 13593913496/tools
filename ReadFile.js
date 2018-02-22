
var fileObject = {
    className : '',//类名
    fullPath : '',//类全路径
    enterMethod : '',//入口方法
    errorMsg : '',//异常信息
    type : ''
};
var types = ['JAVA','JS'];
var defaultErrorMsg = [
    'Params Exception , Not Java Or JS !',
    'Params Exception , Not match "class.method" !',
    'Params Exception , File not exist ! ',
    'Params Exception , Method not exist !'
];

var fs = require('fs');

var ReadFile = {

    read : function (params) {
        var classPointMethod = params.classPointMethod;
        var type = params.type;
        validParams(classPointMethod,type);
        fileObject.projectPath = params.projectPath;
        if(fileObject.errorMsg.length > 0){
            console.log(fileObject.errorMsg);
            return fileObject;
        }
        return fileObject;
    }
}
//满足 类.方法名 且为Java / JS
function validParams(classPointMethod, type) {

    if(!types.includes(type.toUpperCase()) )
    {
        fileObject.errorMsg = defaultErrorMsg[0];
        return;
    }
    var check = classPointMethod.indexOf('.') > -1;
    if(!check){
        fileObject.errorMsg = defaultErrorMsg[1];
        return;
    }
    var array = classPointMethod.split('.');
    var javaOrJs = (types[0] == type.toUpperCase() ? '.java' : '.js');
    //校验文件是否存在
    try{
        var file = fs.readFileSync(array[0] +  javaOrJs, 'utf-8');
    } catch (e){
        console.log(e.toString());
        fileObject.errorMsg = defaultErrorMsg[2];
        return;
    }
    //校验方法是否存在

    if( !ifMethod(file,array[1],javaOrJs) )
    {
        fileObject.errorMsg = defaultErrorMsg[3];
        return;
    }

    //验证ok设置基础信息
    fileObject.fullPath = array[0] + javaOrJs;
    fileObject.enterMethod = array[1];
    var path = require('path');
    fileObject.className =  array[0].substring(array[0].lastIndexOf(path.sep) + 1 );
    fileObject.type = javaOrJs;
    //console.log(fileObject);
}
function ifMethod(file,method,javaOrJs){
    var index;
    if( javaOrJs == '.js'){
        //形如 function aaa(){} or aaa = function (){}
        //去掉所有空格,如果包含 "aaa=function(" or "functionaaa("
        var newFile = file.replace(/ /gi,'');
        var a = newFile.indexOf( method + '=function(');
        var b = newFile.indexOf('function' +method +'(' );
        if( a> -1 || b > -1){
            return true;
        }
        return false;
    }else if(javaOrJs == '.java'){
        //形如 xxx(
        var newFile = file.replace(/ /gi,'');
        index = newFile.indexOf( method + '(');
        if(index > -1 ){
            return true;
        }
        return false;
    }
    return true;
}
module.exports = ReadFile;

