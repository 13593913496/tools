var allFilePaths = [];
function getAllFilePaths(file){
    var projectPath = file.projectPath;

    var fs = require('fs');

    function explorer(path){

        var files = fs.readdirSync(path);
            files.forEach(function(file){

                var stat = fs.statSync(path + '/' + file);

                if(stat.isDirectory()){
                    // 如果是文件夹遍历
                    explorer(path + '/' + file);
                }else{
                    // 读出所有的文件
                    if(file.indexOf('.java')  > -1 ){
                        allFilePaths.push(file);
                    }
                }
            });
    }

    explorer(projectPath);
    console.log(allFilePaths);
}

getAllFilePaths({
    'projectPath' : 'F:\\1_plan\\dev_tools\\WebstormProjects\\api-for-java'
});