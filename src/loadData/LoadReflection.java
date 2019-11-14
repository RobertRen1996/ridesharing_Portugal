package loadData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoadReflection {

	public static Map<String, String> getReflection() {
		ArrayList<String> reflectionSet = readCsv("./reflection_portugal.csv");
//	      System.out.println("\n\n\n\n---------------reflectionSet----------------");
	      reflectionSet.remove(0);
	      
	      Map<String,String> reflection = new HashMap<String, String>();
	      
	      for ( String line: reflectionSet
	      ) {
	    	  String [] strings = line.split(",");
	    	  reflection.put(strings[0], strings[1]);
	      }
	      
	      return reflection;
	}
	
	public static ArrayList<String> readCsv(String filepath) {
        File csv = new File(filepath); // CSV文件路径
        csv.setReadable(true);//设置可读
        csv.setWritable(true);//设置可写
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csv));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = "";
        String everyLine = "";
        ArrayList<String> allString = new ArrayList<>();
        try {
            while ((line = br.readLine()) != null) // 读取到的内容给line变量
            {
                everyLine = line;
//                System.out.println(everyLine);
                allString.add(everyLine);
            }
//            System.out.println("csv表格中所有行数：" + allString.size());
        } catch (IOException e) {
            try {
                br.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return allString;

    }

}
