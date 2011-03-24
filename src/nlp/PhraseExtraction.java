package nlp;

import com.aliasi.util.Files;
import org.btext.utils.LingpipeTools;
import org.btext.utils.MathTools;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.btext.utils.Constants.LOCALE;
import static org.btext.utils.Constants.getStopWords;

/**
 * Phrase (bigrams and trigrams) Extraction using the Dunning likelihood ratio
 * method taken from "Foundations of Statistical NLP" (pp 172-174)
 * 
 */
public class PhraseExtraction {
  public LingpipeTools lt = new LingpipeTools();
  private Set <String> stopWords; // set of stopwords copied from Constants
  private static Pattern spacesPattern = Pattern.compile("\\s+");
  private static Pattern startPattern = Pattern.compile("^[^a-zA-Z0-9].*$");

  public PhraseExtraction() {
    //PropertyConfigurator.configure(LOG4J_FILE);
    this.stopWords = new HashSet<String> (Arrays.asList(getStopWords()));
  }

  /**
   * Pass the text string from which phrases will be extracted
   * 
   * @param text
   * @return list of phrases
   */
  public String[] getPhrases(String text) {
    return getPhrases(text, 10);
  }

  public String[] getPhrases(String text, int numPhrases) {
    if (text == null)
      return null;
    Matcher matcher = spacesPattern.matcher(text);
    text = matcher.replaceAll(" ");

    // *-- Use Lingpipe to get the parts of speech for every token
    // *-- convert all tokens to lower case
    lt.extractPOS(text);
    String[] tags = lt.getTags();
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].equals("jj") || tags[i].equals("jjr")
          || tags[i].equals("jjs") || tags[i].equals("jjss"))
        tags[i] = "adjective";
      if (tags[i].equals("nn") || tags[i].equals("nns")
          || tags[i].equals("nnp") || tags[i].equals("nnps")
          || tags[i].equals("np") || tags[i].equals("nps"))
        tags[i] = "noun";
    }
    String[] tokens = lt.getTokens();
    int numTokens = tokens.length;
    for (int i = 0; i < numTokens; i++)
      tokens[i] = tokens[i].toLowerCase(LOCALE);

    // count the number of occurrences of words and bigrams in a hashmap
    HashMap<String, Integer> wcount = new HashMap<String, Integer>();
    HashMap<String, Integer> bigrams = new HashMap<String, Integer>();
    LOOP: for (int i = 0; i < numTokens - 1; i++) {
      // count the number of occurrences of tokens in the text
      if (wcount.get(tokens[i]) == null)
        wcount.put(tokens[i], new Integer(1));
      else {
        wcount
            .put(tokens[i], new Integer(wcount.get(tokens[i]).intValue() + 1));
      }

      // skip short tokens
      if ((tokens[i].length() < 2) || (tokens[i + 1].length() < 2))
        continue LOOP;

      // skip stopword token
      if (stopWords.contains(tokens[i]) || stopWords.contains(tokens[i + 1]))
        continue LOOP;

      // skip tokens that do not begin with an alphanumeric character
      if (startPattern.matcher(tokens[i]).matches()
          || startPattern.matcher(tokens[i + 1]).matches())
        continue LOOP;

      // skip tokens that are not 'noun noun' or 'adjective noun'
      if (!(tags[i].equals("adjective")) && !(tags[i].equals("noun")))
        continue LOOP;
      if (!(tags[i + 1].equals("noun")))
        continue LOOP;

      // count the number of bigrams
      String bigram = tokens[i] + "!!!" + tokens[i + 1];
      if (bigrams.get(bigram) == null)
        bigrams.put(bigram, new Integer(1));
      else {
        bigrams.put(bigram, new Integer(bigrams.get(bigram).intValue() + 1));
      }

    } // *-- end of for loop

    // count the last token
    int last = numTokens - 1;
    if (wcount.get(tokens[last]) == null)
      wcount.put(tokens[last], new Integer(1));
    else {
      wcount.put(tokens[last], new Integer(
          wcount.get(tokens[last]).intValue() + 1));
    }

    // loop thru the bigrams and assign a likelihood weight to each bigram
    int N = tokens.length;
    HashMap<String, Double> bweight = new HashMap<String, Double>();
    Set<Map.Entry<String, Integer>> keys = bigrams.entrySet();
    Iterator<Map.Entry<String, Integer>> iterator = keys.iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Integer> element = iterator.next();
      String bigram = element.getKey();
      String[] words = bigram.split("!!!");
      int bigramInt = element.getValue().intValue();

      // compute the weight of the bigrams
      bweight.put(bigram, 1.0
          * wcount.get(words[0])
          / N
          * MathTools.lratio(wcount.get(words[0]).intValue(), wcount.get(
              words[1]).intValue(), bigramInt, N) * bigramInt);

    }

    // sort the bweight HashMap by weight
    List<Map.Entry<String, Double>> wlist = new Vector<Map.Entry<String, Double>>(
        bweight.entrySet());
    java.util.Collections.sort(wlist,
        new Comparator<Map.Entry<String, Double>>() {
          public int compare(Map.Entry<String, Double> entry1,
              Map.Entry<String, Double> entry2) {
            return (entry2.getValue().equals(entry1.getValue()) ? 0 : (entry2
                .getValue() > entry1.getValue() ? 1 : -1));
          }
        });

    // skip entries whose first word has been seen earlier, restrict the
    // number of phrases
    bweight = new HashMap<String, Double>();
    ArrayList<String> list = new ArrayList<String>();
    int nump = 0;
    LOOP: for (Map.Entry<String, Double> entry : wlist) {
      String[] words = entry.getKey().split("!!!");
      if (list.indexOf(words[0]) == -1) {
        bweight.put(entry.getKey(), entry.getValue());
        list.add(words[0]);
        if (++nump == numPhrases)
          break LOOP;
      }
    }

    // combine bigrams to form trigrams and add the trigrams first
    Set<String> keys2 = bweight.keySet();
    Iterator<String> iterator1 = keys2.iterator();
    ArrayList<String> phraseList = new ArrayList<String>();
    ArrayList<String> seenList = new ArrayList<String>();
    OLOOP: while (iterator1.hasNext()) {
      String bigram = iterator1.next();
      String[] words1 = bigram.split("!!!");
      Iterator<String> iterator2 = keys2.iterator();
      ILOOP: while (iterator2.hasNext()) {
        bigram = iterator2.next();
        String[] words2 = bigram.split("!!!");
        if (!(words1[1].equals(words2[0])))
          continue ILOOP;
        String phrase = "^.*" + words1[0] + "\\s+" + words1[1] + "\\s+"
            + words2[1] + ".*$";
        Pattern pattern = Pattern.compile(phrase, Pattern.CASE_INSENSITIVE
            | Pattern.MULTILINE);
        if (pattern.matcher(text).matches()) {
          phraseList.add(words1[0] + ' ' + words1[1] + ' ' + words2[1]);
          seenList.add(words1[0] + ' ' + words1[1]);
          seenList.add(words2[0] + ' ' + words2[1]);
          continue OLOOP;
        }
      } // end of inner while

    } // end of outer while

    // next add any remaining bigrams that have not been used in the
    // trigram list
    iterator1 = keys2.iterator();
    while (iterator1.hasNext()) {
      String phrase = iterator1.next().replaceAll("!!!", " ");
      if (seenList.indexOf(phrase) == -1) {
        phraseList.add(phrase);
      }
    }

    String[] phrases = new String[phraseList.size()];
    phraseList.toArray(phrases);
    return (phrases);
  }

  public static void main(String[] args) {
    PhraseExtraction pt = new PhraseExtraction();
    System.out.println("Start Phrase Extraction");
    String textFile = args[0];
    String text = "";
    try {
      text = Files.readFromFile(new File(textFile));
    } catch (IOException ie) {
      System.err.println("Could not open " + textFile + " " + ie.getMessage());
    }
    
    String[] phrases = pt.getPhrases(text, 10);
    if (phrases != null) {
      for (int i = 0; i < phrases.length; i++) {
        System.out.println("Phrase " + i + ": " + phrases[i]);
      }
    }
    System.out.println("End Phrase Extraction");
  }

}
