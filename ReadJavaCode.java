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

	private Boolean removelog =false;//�Ƴ��Զ�����־
	
	private String className;// ����
	private String path;// ȫ·��
	private String loggername = null;
	private int importStringIndex = 0 ;
	private String[] matchs = new String[] { ".log4j.", "public interface", "Logger " ,"class "," Log "};
	/**
	 * 
	 * @param name ����
	 * @param fullpath ȫ·��
	 * @param removelog �Ƿ�ɾ����־
	 */
	public ReadJavaCode(String name, String fullpath ,Boolean removelog) {
		this.className = name.substring(0, name.indexOf(".java"));
		this.path = fullpath;
		this.removelog = removelog;
	}

	/**
	 * �Ƿ��ǽӿ�
	 * 
	 * @param line
	 * @return
	 */
	private boolean isInterface(String line) {
		return line == null ? false : line.indexOf(matchs[1]) > -1;
	}

	/**
	 * �Ƿ��Ѿ������Logger����
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
	 * ��ȡ�Ѿ��������logger ����
	 * 
	 * @param line
	 * @return
	 */
	private void getLoggerObj(String line) {
		
		if(loggername == null && hasDefineLogger(line) ) {
			//"Logger log = " ȡ log
			//"Log log = " ȡ log
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
	 * �ڵ�һ������������һ�д��� logger����
	 */
	private String createLogObj() {			
		
		return "private static final Logger customLogger = (Logger) LogManager.getLogger("+className+".class.getName());";
	}
	
	
	/**
	 * ������ɵ��ַ��� ����: ĳĳ���N��
	 * 
	 * @param linenum
	 * @return
	 */
	private String logString() {	
		if(loggername == null ) {
			return "customLogger".concat(".trace(\""+ className+"��{n}��......"+"\");");
		}
		return loggername.concat(".trace(\""+ className+"��{n}��......"+"\");");
	}

	/**
	 * ��������ȡ
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
						getLoggerObj(line);//��ȡ��ǰ�� ��־����,����Ѿ���ȡ�����ڻ�ȡ��	
						setImportStringIndex(line,result.size());//��ȡ"import*"�к�
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
			result = addLog(result);//��ÿһ�ж���result,Ȼ������еķ������ log
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
	 * �Ƴ���־
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
	 * ���������log
	 * @param result
	 * @return
	 */
	private List<String> addLog(List<String> result) {
		
		if(result == null || result.size() ==0 ) return null;
		
		List<Integer> methodIndex =getIndexOfMethod2(result);//�淽��������
				
		if(methodIndex == null || methodIndex.size() == 0) {
			return result;
		}	
		
		int pre = 0;//λ��ƫ����
		
		for (int i = 0,j= methodIndex.size(); i < j; i++) {
			
			pre = (i+1);
			String log = logString();				
			
			log = spaceLog(log,result, methodIndex, pre, i , true);	//true��ʾ��ǰ�м��� �ո�				
			result.add(methodIndex.get(i) + pre  , log);
		}
		
		importPackageAndNewLogobj(result, methodIndex);//����log ��
		setLogLineNum(result);//��result �����к�
		return result;
	}

	private void importPackageAndNewLogobj(List<String> result, List<Integer> methodIndex) {
		if(loggername == null) {
		
			String line = result.get(result.size()-2);//������2�еĳ���
			String temp = line.substring(0, line.indexOf(line.trim()));
			
			//��������һ�����
			String logObjline = createLogObj();		
			result.add(result.size() -1   , temp + logObjline);
			
			//���� import�ĵط� ����log4j��Ҫ��jar��
			String importlog = "import org.apache.log4j.*;";
			
			if(result.get(3).indexOf("import ") > -1) {
				result.add(3, importlog);
			}else {
				//�����3�в��� import��ͷ
				result.add(importStringIndex, importlog);
			}
			
		}
	}

	private void setLogLineNum(List<String> result) {
		//��log��־�����к�
		String line;
		for (int i = 0,j = result.size(); i < j; i++) {
			line = result.get(i);
			if( line.indexOf("��{n}��")> -1) {				
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
	 * ����ԳƵ�{}�ṹ��Ϊ����,��� { �ĸ��� == }�ĸ���
	 * ��ǰ�����ʺϸ�ÿһ��{}�ṹ����һ��logger
	 */
	private List<Integer> getIndexOfMethod(List<String> result) {
		
		List<Integer> methodIndex = new ArrayList<Integer>();//�淽��������

		int index = 0;//��ǰ��ʽ��λ��
		int numS   = 0;//ͳ�� {�ĸ���
		int numE   = 0;//ͳ�� }�ĸ���
		String line = null;
		for (int i = 0,j= result.size(); i < j; i++) {
			line = result.get(i);

			if("{".equals(line.charAt(line.length()-1) + "" ) && (line.indexOf(matchs[3]) == -1) ) {
				//���һ���ַ���Ϊ { 
				numS++ ;
				if(index == 0 ) {
					index = i;
				}
				
			}else if(numS == (numE+1) ) {
				methodIndex.add(index);
				//����
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
	 * ����ԳƵ�{}�ṹ��Ϊ����,��� { �ĸ��� == }�ĸ���
	 * ��ǰ�����ʺϣ� ֻ�ڵ�һ��{}д��logger
	 */
	private List<Integer> getIndexOfMethod2(List<String> result) {
		
		List<Integer> methodIndex = new ArrayList<Integer>();//�淽��������

		int index = 0;//��ǰ��ʽ��λ��
		int numS   = 0;//ͳ�� {�ĸ���
		int numE   = 0;//ͳ�� }�ĸ���
		String line = null;
		for (int i = 0,j= result.size(); i < j; i++) {
			line = result.get(i);
			if(line.indexOf("*") > -1) {
				continue;
			}

			if(line.indexOf("{") > -1 && line.indexOf("}") > -1 ) {
				//���{}Ϊͬһ��
				numS++ ;
				numE++;
			}
			else if("{".equals(line.charAt(line.length()-1) + "" ) 
					&& i != 0
					&& (line.indexOf(matchs[3]) == -1) 
					&& result.get(i-1).indexOf(matchs[3]) == -1 ) {
				//���һ���ַ���Ϊ { 
				numS++ ;
				if(index == 0 ) {
					index = i;
				}
				
			}else if(numS == (numE+1) && "}".equals(line.charAt(line.length()-1) + "" )) {
				methodIndex.add(index);
				//����
				numS = numE = index= 0;				
				
			}else if("}".equals(line.charAt(line.length()-1) + "" ) 
					|| (line.indexOf("}") > -1 && line.indexOf("\"") == -1)) {
				//���һ���ַ���Ϊ }
				if(numS > 0) {
					numE++;
				}
			}			
		}
		return methodIndex;
	}
	
	
	/**
	 * log�ַ����뵱ǰ�ж���
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
	 * 1��1�е�д��
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
				bufferedWriter.newLine();// ����
			}
			bufferedWriter.flush();
			bufferedWriter.close();
			System.out.println("�����" + path);
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
	 * ��д
	 */
	public void init() {
		write(read());
	}

}
