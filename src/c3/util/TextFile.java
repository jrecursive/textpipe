package c3.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class TextFile {
  static public String get(String fn) {
    File aFile = new File(fn);
    StringBuilder contents = new StringBuilder();
    try {
      BufferedReader input =  new BufferedReader(new FileReader(aFile));
      try {
        String line = null;
        while (( line = input.readLine()) != null){
          contents.append(line);
          contents.append(System.getProperty("line.separator"));
        }
      }
      finally {
        input.close();
      }
    }
    catch (IOException ex){
      //ex.printStackTrace();
      return null;
    }
    return contents.toString();
  }
  
  static public List<String> getlines(String fn) throws Exception {
    File aFile = new File(fn);
    List<String> lines = new ArrayList<String>();

    BufferedReader input = new BufferedReader(new FileReader(aFile));
    String line;
    while((line = input.readLine()) != null) {
        lines.add(line);
    }
    
    input.close();
    return lines;
  }


  static public void put(String fn, String aContents)
    throws FileNotFoundException, IOException {
    Writer output = new BufferedWriter(
        new FileWriter(
            new File(fn)));
    try {
      output.write( aContents );
    }
    finally {
      output.close();
    }
  }
} 


