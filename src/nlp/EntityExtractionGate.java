package nlp;

import com.aliasi.util.Files;
import org.btext.utils.GateTools;

import java.io.File;

public class EntityExtractionGate {
    private static GateTools gt = new GateTools();

    public static void main(String[] args) throws Exception {
        String text = Files.readFromFile(new File(args[0]));
        String[] x = text.split("\n");
        text = x[0];
        System.out.println("extracting from: " + text);
        System.out.println("\n\n--\n\n");
        System.out.println(extractEntities(text));
    }
  
    public static String extractEntities(String text) throws Exception {
        gt.setforIE();
        return gt.getEntities(text);
    }

}

