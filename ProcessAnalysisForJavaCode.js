/**
 * java代码流程分析工具
 * 1.ReadFile
 * 2.ParseFile
 * 3.OutputFile
 * 读 > 解析 > 输出
 */

var ReadFile = require('./ReadFile');
var file = ReadFile.read({
    'projectPath' : 'F:\\SVN_CODE\\CRM_sys\\src',
    'classPointMethod': 'F:\\SVN_CODE\\CRM_sys\\src\\crm\\hjq\\service\\imp\\CstCustomerDaoImp.CstCustomerquery',
    'type':'java'
});

var ParseFile = require('./ParseFile');
ParseFile.setFileObject(file);//异步












