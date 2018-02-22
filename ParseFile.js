var allFilePaths = [];//存储当前项目下面所有的文件路径

var ParseFile = {
    setFileObject : function (file) {
        readFile(file);
    }
}

function readFile(file){

    file.currentMethod = [];
    file.methodClose = false;
    file.cotainMethods = [];
    file.importClass = [];
    file.map = [];//存储每个对象的类名

    if( allFilePaths.length == 0 ){
        getAllFilePaths(file);

    }

    const readline = require('readline');
    const fs = require('fs');
    //一行一行读
    const rl = readline.createInterface({
        input: fs.createReadStream(file.fullPath),
        crlfDelay: Infinity
    });
    rl.on('line',function (line) {
        line = dataProcess(line);
        setImportClass(file,line);
        setMap(file,line);

        if(line != ''){
            var result = getMethodBlock(file,line);
            if(result != ''){
                file.currentMethod.push(result);
            }
        }

    }) ;
    rl.on('close',function () {
        //console.log(file.currentMethod);
        //console.log(file.importClass);
        //console.log(file.map);
        //筛选出方法名
        filterMethods(file);
        //console.log(file.cotainMethods);
        cylceRead(file);
    }) ;
}

function getAllFilePaths(file){
    var projectPath = file.projectPath;
    var apipath = require('path');
    var fs = require('fs');

    function explorer(path){

        var files = fs.readdirSync(path);
        files.forEach(function(file){

            var stat = fs.statSync(path + apipath.sep + file);

            if(stat.isDirectory()){
                // 如果是文件夹遍历
                explorer(path + apipath.sep + file);
            }else{
                // 读出所有的文件
                if(file.indexOf('.java')  > -1 ){
                    allFilePaths.push({
                        'className': file,
                        'fullPath' : path + apipath.sep + file
                    });
                }
            }
        });
    }

    explorer(projectPath);
    //fs.closeSync(path + apipath.sep + file);
}

function dataProcess(result) {
    // 如果 是注释代码则不考虑
    if(result.indexOf('//') > -1){
        return '';
    }

    // 去掉特殊字符

    result = result.replace(/\t/g,'');

    // TO-DO
    //如果同一行里面既有{又有} 则再 {后面换行插入
    return  result;
}

function setImportClass(file,line){
    if(line.indexOf('import ') > -1){
        file.importClass.push(line.substring(0,line.length -1));
    }
}

/**
 * 形如: File fileWrite = new File("D:/testWrite.xls");
 * 形如: IBussinessService bussinessService;
 * @param line
 */
function setMap(file,line){
    var className ;
    var objName ;
    var package ;
    var array;
    var obj;
    for (var i=0,j=file.importClass.length;i<j;i++){
            className = file.importClass[i].substring(file.importClass[i].lastIndexOf('.') + 1 );
            if(line.indexOf(className) > -1 ){
                array = line.split(' ');
                for (var n=0,m=array.length;n<m;n++){
                    if( array[n] == className ){
                        objName = array[n+1];
                        package = file.importClass[i];

                        obj = {
                            line : line ,
                            objName : objName,
                            className : className,
                            package : package
                        };
                        if(package != ''){
                            if(!isSame(obj,file) ){
                                file.map.push(obj);
                            }
                        }

                    }
                }
            }else{
                continue;
            }

    }

    function isSame(t,file) {
        var str = t.className+t.objName+ t.package;
        var temp;
        for(var i=0,j=file.map.length;i<j;i++){
            temp = file.map[i].className+file.map[i].objName+ file.map[i].package;
            if(str == temp){
                return true;
            }
        }

        return false;
    }

}



function getMethodBlock(file,line) {

    if(file.currentMethod.length > 0){
        if(!file.methodClose){
            setMethodClose(file,line);
            return line;
        }
    }else{
        var index = line.indexOf( file.enterMethod + '(');
        if(index > -1){
            return line;
        }
    }
    return '';

    /**
     *{
 *  {
 *      {
 *      }
 *  }
 *
 *  {
 *  }
 * }
     * 满足{ 个数  =  }个数 + 1(表示当前的})
     */
    function setMethodClose(file,line) {
        if( line.indexOf('}') > -1 ){
            var preNum = 0;
            var afterNum = 0;
            for(var i = 0 ; i< file.currentMethod.length ; i++){
                if(file.currentMethod[i].indexOf('{')> -1){
                    preNum ++;
                }
                if(file.currentMethod[i].indexOf('}')> -1){
                    afterNum ++;
                }
            }
            if(preNum == (afterNum + 1 ) ){
                file.methodClose = true;
            }
        }
    }


}




function filterMethods(file) {
    //从currentMethod代码块 筛选出方法
    //满足: '.xxxx(' or ' xxxx('的方法筛选出来; xxxx目前暂定位 . or 空格 加上一段字符串 + (

    var reg = /\(/;
    var isMethod = false;
    var method='';
    var fileObject;
    var line;
    for(var i = 0 ;i< file.currentMethod.length ; i++){
        method = file.currentMethod[i];
        line = file.currentMethod[i];
        if(reg.test(method)
            && !(method.indexOf('new') > -1)
            && !isKeyWord(method)
            && !(method.indexOf(file.enterMethod) > -1) ){//满足 ( 且去掉new
            method = method.split('.');

            fileObject = getfileObject(line,method,file);
            if(fileObject.enterMethod && fileObject.enterMethod != '' && fileObject.enterMethod.length > 0){
                file.cotainMethods.push(fileObject);
            }

        }
    }

    //是否是关键字
    function isKeyWord(method) {
        var words = ['catch','printStackTrace','if','else',' from '];
        for(var i = 0;i < words.length; i++){
            if(method.indexOf(words[i]) > -1){
                return true;
            }
        }
        return false;
    }

}

function getfileObject(line,method,file) {
    var fileObject = {};
    if(method.length == 1 ){//当前类方法
        fileObject.className = file.className;
        fileObject.enterMethod = line.substring(0,line.lastIndexOf('('));
    }else{

        //引入类方法
        var obj;

        for( var n = 0,m=file.map.length;n<m;n++ ){
            obj= file.map[n];

            if( line.indexOf(obj.objName+'.') > -1){//满足实例. 则表明这是个方法
                fileObject.objName = obj.objName;
                fileObject.enterMethod = line.substring(line.indexOf(obj.objName+'.') + obj.objName.length + 1 , line.indexOf('('));
                fileObject.className = obj.className;
            }
        }
    }
    fileObject.father = file.className+'.'+file.enterMethod;
    fileObject.type = file.type;
    return fileObject;
}



function cylceRead(file) {

    var result;
    var current;
    write(file);
    for (var i=0,j=file.cotainMethods.length;i<j;i++){
        current = file.cotainMethods[i];

        if(ifExist(current)){
            readFile(current);
        }
    }
}
function ifExist(current) {
    //怎样判断这个路径是否存在
    for (var i= 0,j=allFilePaths.length;i<j;i++){
        if(allFilePaths[i].className == (current.className + current.type) ){
            current.fullPath = allFilePaths[i].fullPath;
            return true;
        }
    }
    return false;
}
function write(file) {
    console.log(file.cotainMethods);
}

module.exports = ParseFile;





