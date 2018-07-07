package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

public class ReadJavaCode {

    private Boolean removelog =false;//移除自定义日志
    
    private String className;// 类名
    private String path;// 全路径
    private String loggername = null;
    private int importStringIndex = 0 ;
    private String[] matchs = new String[] { ".log4j.", "interface ", "Logger " ,"class "," Log "," enum "};
    /**
     * 
     * @param name 类名
     * @param fullpath 全路径
     * @param removelog 是否删除日志
     */
    public ReadJavaCode(String name, String fullpath ,Boolean removelog) {
        this.className = name.substring(0, name.indexOf(".java"));
        this.path = fullpath;
        this.removelog = removelog;
    }

    /**
     * 是否是接口
     * 是否是枚举类型
     * 
     * @param line
     * @return
     */
    private boolean isInterface(String line) {
        boolean inter = line == null ? false : line.indexOf(matchs[1]) > -1;
        boolean enu = line == null ? false : line.indexOf(matchs[5]) > -1;
        return inter || enu;
    }

    /**
     * 是否已经定义过Logger对象
     * 
     * @param line
     * @return
     */
    private boolean hasDefineLogger(String line) {
        if(line != null && (line.indexOf(matchs[2]) > -1 || line.indexOf(matchs[4]) > -1)
                    && line.indexOf("=") > -1 
                    && !line.startsWith("//")
                    && !line.startsWith("*")
                    && !line.startsWith("/*")) {
            
            return true;
        }
        return false;   
    }
    
    
    public static void main(String[] args) {
        
        ReadJavaCode r = new ReadJavaCode("ssss.java", "ssss.java" ,true );
        String line = "/** Logger available to subclasses */";
        boolean hasDefine = r.hasDefineLogger(line);
        System.out.println(hasDefine);
        
    }

    /**
     * 获取已经定义过的logger 名字
     * 
     * @param line
     * @return
     */
    private String  getLoggerObj(String line) {
        
        if(loggername == null && hasDefineLogger(line) ) {
            //"Logger log = " 取 log
            //"Log log = " 取 log
            String name = "";
            if(line.indexOf(matchs[2]) > -1) {
                name = line.substring(line.indexOf(matchs[2]) + 8, line.indexOf("="));
            }else if(line.indexOf(matchs[4]) > -1) {
                name = line.substring(line.indexOf(matchs[4]) + 5, line.indexOf("="));
            }

            loggername = name.trim();
            //统一log格式,final
            if(line.indexOf( "static" ) == -1){
                line = line.replace( "final", "static final" );
            }
 
            line = line.replace( "getClass()", className+".class" );
        }
        return line;
    }

    /**
     * 在第一个方法的上面一行创建 logger对象
     */
    private String createLogObj() {         
        
        return "private static final Logger customLogger = (Logger) LogManager.getLogger("+className+".class.getName());";
    }
    
    
    /**
     * 最后生成的字符串 形如: 某某类第N行
     * 
     * @param linenum
     * @return
     */
    private String logString() {    
        if(loggername == null ) {
            return "customLogger".concat(".trace(\""+ className+"第{n}行......"+"\");");
        }
        return loggername.concat(".trace(\""+ className+"第{n}行......"+"\");");
    }

    /**
     * 将方法读取
     * 
     * @return
     */
    private List<String> read() {
        BufferedReader br = null;
        List<String> result = new LinkedList<String>();
        try {
            br = new BufferedReader(new FileReader(path));
            String line = null;
            while ( (line = br.readLine()) != null) {

                if (line != null && line.length() > 0 ) {
                    try {
                        if(isInterface(line)) {
                            br.close();                 
                            return null;
                        }
                        line = getLoggerObj(line);//获取当前类 日志对象,如果已经获取了则不在获取。   
                        setImportStringIndex(line,result.size());//获取"import*"行号
                        result.add(line);   
                    } catch (Exception e) {
                        System.out.println(line);
                        e.printStackTrace();
                    }
                                
                }               

                
            }                       
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(removelog) {
            result = removeLog(result);
        }else {
            result = addLog(result);//将每一行读到result,然后给其中的方法添加 log
            //二次处理数据,交换位置
            result = swapLocation(result);
        }
        
        //去掉方法起始结束的标识
        clearFlag(result);
        
        return result;
    }
    
    /**
     * log不能打在return前面,如果方法里面有return,则将log打在方法内第一行
     * @param result
     * @return
     */
    private List<String> swapLocation( List<String> result )
    {
        String line;
        String preLine;
        int startIndex = 0;
        
        for ( int i = 0; i < result.size(); i++ )
        {
            line = result.get( i );
            if(line.indexOf( "methodStartLine" ) > -1){
                startIndex = i + 1 ;
            }
            
            if(line.indexOf( ".trace(" ) > -1){
                
                for ( int j = (i-1); j >0 ; j-- )
                {
                    preLine = result.get( j );
                    if(preLine.indexOf( "methodStartLine" ) > -1){
                        break;
                    }
                    if(preLine.indexOf( " return " )>-1 
                            && !preLine.startsWith( "*" )){
                        result.remove( i );
                        result.add( startIndex, line );
                        break;
                    }                    
                }
            }
        }
        return result;
    }

    /**
     * 去掉方法起始行的标识
     * methodStartLine
     * methodEndLine
     * @param result
     */
    private void clearFlag( List<String> result )
    {
        if(result == null || result.size() == 0){
            return;
        }
        String line;
        String temp;
        for ( int i = 0; i < result.size(); i++ )
        {
            line = result.get( i );
            if(line.indexOf( "methodStartLine" ) > -1 
                    || line.indexOf( "methodEndLine" ) > -1){
                temp = line.replace( "methodStartLine" , "" );
                temp = temp.replace( "methodEndLine" , "" );
                result.set( i, temp );//String 对象
            }
        }
    }

    
    /**
     * 找到当前方法最后一行m
     * methodStartLine
     * methodEndLine
     * @param result
     * @param i
     * @return
     */
    private int getMethodLastLine( List<String> result, int i )
    {
        
        String line;
        for ( int j = (i + 1); j < result.size(); j++ )
        {
            line =  result.get( j );
            if(line.indexOf( "methodEndLine" ) > -1){
                return j-1;
            }
        }
        return 0;
    }

    private void setImportStringIndex(String line, int size) {
        if(importStringIndex != 0) {
            return;
        }
        if(line.indexOf("import ") > -1 || line.indexOf("package ") > -1) {
            importStringIndex = size + 1;
        }
    }
    /**
     * 移除日志
     * @param result
     * @return
     */
    private List<String> removeLog(List<String> result) {
        String line = null;
        String logObj = createLogObj();
        for (int i = result.size() -1 ; i >= 0 ; i--) {
            line= result.get(i);
            if(line.indexOf("customLogger.trace") > -1 
                    || line.indexOf(loggername + ".trace(\"" + className) > -1 
                    || line.indexOf("import org.apache.log4j.*;") > -1  
                    || line.indexOf(logObj) > -1  ) {
                result.remove(line);
            }
        }
        return result;
    }
    
    
    /**
     * 给方法添加log
     * @param result
     * @return
     */
    private List<String> addLog(List<String> data) {
        
        if(data == null || data.size() ==0 ) return null;
        
        List<Integer> methodIndex =setIndexOfMethod2(data);//存方法的坐标
        if(methodIndex == null || methodIndex.size() == 0) {
            return data;
        }   
        
        String line ;
        String log = logString(); 
        String temp;
        List<String> result = new ArrayList<String>();
        for ( int i = 0 ; i < data.size(); i++ )
        {
            line = data.get( i );
            if(line.indexOf( "methodEndLine" ) > -1){                       
                temp = spaceLog(log,data, i-1 , true); //true表示当前行加上 空格         
                result.add( temp );
            }
            result.add( line );            
        }
        
        importPackageAndNewLogobj(result);//引入log 包
        setLogLineNum(result);//给result 设置行号
        return result;
    }

    private void importPackageAndNewLogobj(List<String> result) {
        if(loggername == null) {
        
            String line = result.get(result.size()-2);//倒数第2行的长度
            String temp = line.substring(0, line.indexOf(line.trim()));
            
            //在类的最后一行添加
            String logObjline = createLogObj();     
            result.add(result.size() -1   , temp + logObjline);
            
            //在类 import的地方 导入log4j需要的jar包
            String importlog = "import org.apache.log4j.*;";
            
            if(result.get(3).indexOf("import ") > -1) {
                result.add(3, importlog);
            }else {
                //如果第3行不是 import开头
                result.add(importStringIndex, importlog);
            }
            
        }
    }

    private void setLogLineNum(List<String> result) {
        //给log日志带上行号
        String line;
        for (int i = 0,j = result.size(); i < j; i++) {
            line = result.get(i);
            if( line.indexOf("第{n}行")> -1) {                
                result.set(i, line.replace("{n}", i+1+"" ));
            }
        }
    }
    
    
    
    
    /**
     * {
     *      logger.trace
     *  {
     *      logger.trace
     *      {
     *      logger.trace
     *      }   
     *  }
     * }
     * 满足对称的{}结构则为方法,如果 { 的个数 == }的个数
     * 当前方法适合给每一个{}结构增加一个logger
     */
    private List<Integer> getIndexOfMethod(List<String> result) {
        
        List<Integer> methodIndex = new ArrayList<Integer>();//存方法的坐标

        int index = 0;//当前方式的位置
        int numS   = 0;//统计 {的个数
        int numE   = 0;//统计 }的个数
        String line = null;
        for (int i = 0,j= result.size(); i < j; i++) {
            line = result.get(i);

            if("{".equals(line.charAt(line.length()-1) + "" ) && (line.indexOf(matchs[3]) == -1) ) {
                //最后一个字符串为 { 
                numS++ ;
                if(index == 0 ) {
                    index = i;
                }
                
            }else if(numS == (numE+1) ) {
                methodIndex.add(index);
                //重置
                numS = numE = index= 0;                             
            }           
        }
        return methodIndex;
    }
    
    /**
     * {
     *      logger.trace
     *  {
     *      {}  
     *  }
     * }
     * 满足对称的{}结构则为方法,如果 { 的个数 == }的个数
     * 当前方法适合： 只在第一个{}写入logger
     */
    private List<Integer> setIndexOfMethod2(List<String> result) {
        
        List<Integer> methodIndex = new ArrayList<Integer>();//存方法的坐标

        int index = 0;//当前方式的位置
        int numS   = 0;//统计 {的个数
        int numE   = 0;//统计 }的个数
        String line = null;
        for (int i = 0,j= result.size(); i < j; i++) {
            line = result.get(i);
            if(line.indexOf("*") > -1) {
                continue;
            }

            if(line.indexOf("{") > -1 && line.indexOf("}") > -1 ) {
                //如果{}为同一行
                numS++ ;
                numE++;
            }
            else if("{".equals(line.charAt(line.length()-1) + "" ) 
                    && i != 0
                    && (line.indexOf(matchs[3]) == -1) 
                    && result.get(i-1).indexOf(matchs[3]) == -1 ) {
                //最后一个字符串为 { 
                numS++ ;
                if(index == 0 ) {
                    index = i;
                    line = line + "methodStartLine";//标识方法起始行
                    result.set( i, line );
                }
                
            }else if(numS == (numE+1) && "}".equals(line.charAt(line.length()-1) + "" )) {
                methodIndex.add(index);
                //重置
                numS = numE = index= 0;             
                line = line + "methodEndLine";//标识方法结束行
                result.set( i, line );
                
            }else if("}".equals(line.charAt(line.length()-1) + "" ) 
                    || (line.indexOf("}") > -1 && line.indexOf("\"") == -1)) {
                //最后一个字符串为 }
                if(numS > 0) {
                    numE++;
                }
            }           
        }
        return methodIndex;
    }
    
    
    /**
     * log字符串与当前行对齐
     * @param result
     * @param methodIndex
     * @param pre
     * @param i
     * @return
     */
    private String spaceLog(String log, List<String> result, int i,
            boolean isSpace) {

        if (!isSpace) {
            return log;
        }   
        String line = result.get(i);
        String temp = line.substring(0, line.indexOf(line.trim()));

        return temp + log;
    }
    
    /**
     * 1行1行的写入
     * 
     * @param result
     */
    private void write(List<String> result) {
        if (result == null || result.size() == 0)
            return;

        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(path));
            for (String string : result) {
                bufferedWriter.write(string);
                bufferedWriter.newLine();// 换行
            }
            bufferedWriter.flush();
            bufferedWriter.close();
            System.out.println("已完成" + path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读写
     */
    public void init() {
        write(read());
    }

}
