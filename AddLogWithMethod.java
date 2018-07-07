package test;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author chenli
 * 给指路径下的 所有java文件的所有方法加上 log日志,日志级别TRACE
 */
public class AddLogWithMethod {
    
    
    //org.apache.logging.log4j.status ParameterizedBeanPropertyRowMapper
    private static String filepath = 
           "";
    
    public static void main(String[] args) {
                
        File file = new File(filepath); 

        //获取所有java文件
        File[] files = file.listFiles();
        List<Code> codes = new ArrayList<Code>();
        
        if(files != null) {
            recursiveFolder( files, codes );
        }else {
            codes.add( new Code(file.getName(), file.getAbsolutePath()) );
        }
        
        if(codes.size() > 0) {
            int num = 0;
            ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 20, 2000L, TimeUnit.SECONDS, 
                    new LinkedBlockingQueue<Runnable>(2000));
            tpe.setRejectedExecutionHandler( new RejectedExecutionHandler()
            {
                
                @Override
                public void rejectedExecution( Runnable r, ThreadPoolExecutor executor )
                {
                 
                }
            } );
            
            try
            {

                int size = codes.size();
                
                Map<String,Object> permap = null;
                for (int i = 0; i < size; i++) {
                    num = i;
                    permap= new HashMap<String,Object>();
                    permap.put(codes.get(i).getFilepath(), codes.get(i).getFilename());
                    tpe.execute(new ReadTask(permap, i));
                    
                }
                tpe.shutdown();
            } catch ( Exception e )
            {                
                System.out.println("处理掉异常 : " + num);
                System.out.println(e.toString());

            }

        }

    }
    //迭代文件夹
    public static void recursiveFolder(File[] files,List<Code> codes) {
        if(files == null || files.length == 0 ){
            return;
        }
        for (File f : files) {
            if(f.isFile() && f.getName().indexOf(".java") > -1 ) {                
                codes.add( new Code(f.getName(), f.getAbsolutePath()) );
            }else if(f.isDirectory()){
                recursiveFolder( f.listFiles(), codes );
            }
        }        
    }
    
    
    static class ReadTask implements Runnable{
        
        Map<String,Object> map ;
        int n ;
        public ReadTask(Map<String,Object> map , int n ) {
            this.map = map;
            this.n = n;
        }

        
        private void process(Map<String, Object> map , int n) {         
            Set<Entry<String, Object>> entry = map.entrySet();  
            
            for (Entry<String, Object> e : entry) {
                ReadJavaCode r = new ReadJavaCode((String) e.getValue(), e.getKey() ,false );
                r.init();
            }
        }

        @Override
        public void run() {
            //当前批次n
            process(map,  n);
        }

    }
    
    static class Code implements Serializable{

        /**
         * 
         */
        private static final long serialVersionUID = 4616676105884882896L;
        private String filename;
        private String filepath;
        
        public Code(String filename, String filepath) {
            super();
            this.filename = filename;
            this.filepath = filepath;
        }
        
        public String getFilename() {
            return filename;
        }
        public void setFilename(String filename) {
            this.filename = filename;
        }
        public String getFilepath() {
            return filepath;
        }
        public void setFilepath(String filepath) {
            this.filepath = filepath;
        }

    }
}
