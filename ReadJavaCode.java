package io.addlog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ReadJavaCode {

	private Boolean removelog =false;//移除自定义日志
	
	private String className;// 类名
	private String path;// 全路径
	private String loggername = null;
	private int importStringIndex = 0 ;
	private String[] matchs = new String[] { ".log4j.", "public interface", "Logger " ,"class "," Log "};
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
	 * 
	 * @param line
	 * @return
	 */
	private boolean isInterface(String line) {
		return line == null ? false : line.indexOf(matchs[1]) > -1;
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
	private void getLoggerObj(String line) {
		
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
		}
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
						getLoggerObj(line);//获取当前类 日志对象,如果已经获取了则不在获取。	
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
		}
		
		
		return result;
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
	private List<String> addLog(List<String> result) {
		
		if(result == null || result.size() ==0 ) return null;
		
		List<Integer> methodIndex =getIndexOfMethod2(result);//存方法的坐标
				
		if(methodIndex == null || methodIndex.size() == 0) {
			return result;
		}	
		
		int pre = 0;//位置偏移量
		
		for (int i = 0,j= methodIndex.size(); i < j; i++) {
			
			pre = (i+1);
			String log = logString();				
			
			log = spaceLog(log,result, methodIndex, pre, i , true);	//true表示当前行加上 空格				
			result.add(methodIndex.get(i) + pre  , log);
		}
		
		importPackageAndNewLogobj(result, methodIndex);//引入log 包
		setLogLineNum(result);//给result 设置行号
		return result;
	}

	private void importPackageAndNewLogobj(List<String> result, List<Integer> methodIndex) {
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
	 * 		logger.trace
	 * 	{
	 * 		logger.trace
	 * 		{
	 * 		logger.trace
	 * 		}	
	 * 	}
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
	 * 		logger.trace
	 * 	{
	 * 		{}	
	 * 	}
	 * }
	 * 满足对称的{}结构则为方法,如果 { 的个数 == }的个数
	 * 当前方法适合： 只在第一个{}写入logger
	 */
	private List<Integer> getIndexOfMethod2(List<String> result) {
		
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
				}
				
			}else if(numS == (numE+1) && "}".equals(line.charAt(line.length()-1) + "" )) {
				methodIndex.add(index);
				//重置
				numS = numE = index= 0;				
				
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
	private String spaceLog(String log, List<String> result, List<Integer> methodIndex, int pre, int i,
			boolean isSpace) {

		if (!isSpace) {
			return log;
		}	
		String line = result.get(methodIndex.get(i) + pre);
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
