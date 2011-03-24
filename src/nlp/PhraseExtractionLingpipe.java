package nlp;

import com.aliasi.lm.TokenizedLM;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Files;
import com.aliasi.util.ScoredObject;

import java.io.File;
import java.io.IOException;

/**
 * Extract phrases from text using Lingpipe
 */
public class PhraseExtractionLingpipe {

  private static int NGRAM = 3;
  private static int MIN_COUNT = 10;
  // private static int MAX_NGRAM_REPORTING_LENGTH = 2;
  private static int NGRAM_REPORTING_LENGTH = 2;
  private static int MAX_COUNT = 5000;

  // private static File FOREGROUND_DIR = new
  // File("../../data/rec.sport.hockey/test");

  public static void main(String[] args) throws IOException {
    IndoEuropeanTokenizerFactory tokenizerFactory = new IndoEuropeanTokenizerFactory();

    // Train the model using the list of files found in the directory
    TokenizedLM model = buildModel(tokenizerFactory, NGRAM, new File(args[0]));

    // limit the size of model by removing all sequences that
    // occur less than three times
    model.sequenceCounter().prune(3);

    // dump the list of collocations
     ScoredObject[] coll = model.collocations(NGRAM_REPORTING_LENGTH,
     MIN_COUNT,MAX_COUNT);

     for (int i = 0; i < coll.length; i++) {
     String[] toks = (String[]) coll[i].getObject();
     System.out.println("S: " + coll[i].score() + " " + toks[0] + " " +
     toks[1]);
     if ( (i == 99) || ((i+1) == coll.length) ) break;
     }
     System.out.println("\nCollocations in Order of Significance:");
     report(coll);

    // show the new collocations in the directories txt and txt2
    //TokenizedLM newModel = buildModel(tokenizerFactory, NGRAM, new File(
    //    TESTING_DIR + FS + "txt2"));
    //newModel.sequenceCounter().prune(3);
    //ScoredObject<String[]>[] newTerms = newModel.newTerms(NGRAM_REPORTING_LENGTH,
    //    MIN_COUNT, MAX_COUNT, model);
    //System.out.println("\nNew Terms in Order of Signficance:");
    //report(newTerms);

    System.out.println("\nDone.");
  }

  private static TokenizedLM buildModel(TokenizerFactory tokenizerFactory,
      int ngram, File directory) throws IOException {
    TokenizedLM model = new TokenizedLM(tokenizerFactory, ngram);
    for (String file : directory.list())
      model.train(Files.readFromFile(new File(directory, file)));
    return model;
  }

  private static void report(ScoredObject<String[]>[] nGrams) {
    for (int i = 0; i < nGrams.length; ++i) {
      double score = nGrams[i].score();
      String[] toks = (String[]) nGrams[i].getObject();
      report_filter(score, toks);
    }
  }

  private static void report_filter(double score, String[] toks) {
    String accum = "";
    for (int j = 0; j < toks.length; ++j) {
      if (nonCapWord(toks[j]))
        return;
      accum += " " + toks[j];
    }
    System.out.println("Score: " + score + " with :" + accum);
  }

  private static boolean nonCapWord(String tok) {
    if (!Character.isUpperCase(tok.charAt(0)))
      return true;
    for (int i = 1; i < tok.length(); ++i)
      if (!Character.isLowerCase(tok.charAt(i)))
        return true;
    return false;
  }
}
