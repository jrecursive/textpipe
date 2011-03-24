package nlp;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classifier;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.Files;
import com.alienobject.nlp.extractors.LPHTMLTextExtractor;
import com.alienobject.nlp.lingpipe.LPTools;
import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NLP {
	static File modelFile;
	static Classifier classifier;
	LPHTMLTextExtractor lphtml;
	
	LPTools lpt = new LPTools();
	String[] pos_tags = null;
	String[] pos_tokens = null;
	
	private final static String wordnetPath = "data/wordnet/dict";
	private IDictionary dict;

	public NLP() throws Exception {
        modelFile = new File("data/models/langid-leipzig-classifier");
		classifier = (Classifier) AbstractExternalizable.readObject(modelFile);
		lphtml = new LPHTMLTextExtractor();
		
		//alternatively, use stnadard env for wnhome if it can't find it in data/wordnet ?
		//String wnhome = System.getenv("WNHOME");
        URL url = null;
        try { url = new URL("file", null, wordnetPath); } 
        catch(MalformedURLException e){ e.printStackTrace(); }
        if(url == null) return;
        dict = new edu.mit.jwi.Dictionary(url);
        dict.open();
	}
	
    public String extract_text_url(String u) throws Exception {
        return lphtml.extractTextFromURL(u);
    }
    
    public String extract_text(String s) throws Exception {
        return lphtml.extractText(s);
    }
    
    public String get_language(String s) throws Exception {
        return lphtml.getLanguageId(s);
    }
    
    public double get_language_score(String s) throws Exception {
        return Double.parseDouble(lphtml.getLanguageScore(s));
    }
    
    public JSONObject get_language_score_and_id(String text) throws Exception {
        Classification classification = classifier.classify(text);
        String cl = classification.toString();
        String[] cl_ar = cl.split("\n");
        String[] cl2_ar = cl_ar[1].split(" ");
        JSONObject jo = new JSONObject();
        jo.put("id", cl2_ar[0].replace("0=", ""));
        jo.put("score", cl2_ar[1]);
        return jo;
    }
    
    public void process_pos(String s) throws Exception {
        lpt.extractPOS(s);
        pos_tags = lpt.getTags();
        pos_tokens = lpt.getTokens();
    }
    
    public String[] get_pos_tags() throws Exception {
        return pos_tags;
    }
    
    public String[] get_pos_tokens() throws Exception {
        return pos_tokens;
    }
    
    public ArrayList<String> extract_sentences(String text) throws Exception {
        String s;
        lpt.buildSentences(text);
        ArrayList<String> sentences = new ArrayList<String>();
        for(int i=1; ((s = lpt.nextSentence()) != null); i++) {
            sentences.add(s);
        }
        return sentences;
    }
    
    public ArrayList extract_entities(String s) throws Exception {
        ArrayList p_ents = new ArrayList();
        ArrayList ents = lpt.getEntities(s);
        for(int i=0; i<ents.size(); i++) {
            String ent_s = (String) ents.get(i);
            String[] ent_tup = ent_s.split("\\|");
            String etype = ent_tup[0];
            String ent = ent_tup[1];
            /*
            if (ent.indexOf("?")!=-1) continue;
            if (ent.length() <3 ) continue;
            if (ent.indexOf("\n") != -1) continue;
            if (etype.equals("Person")) {
                p_ents.add(ent);
                trace_dbg("ent>> " + etype + ": " + ent);
            }
            */
            p_ents.add(ent_s);
        }
        return p_ents;
    }
    
    public String[] extract_phrases(String s) throws Exception {
        return extract_phrases(s, 10);
    }
    
    public String[] extract_phrases(String s, int phraseCount) throws Exception {
        PhraseExtraction pe = new PhraseExtraction();
        return pe.getPhrases(s, phraseCount);
    }
    
    public String extract_entities_gate(String text) throws Exception {
        EntityExtractionGate eeg = new EntityExtractionGate();
        return eeg.extractEntities(text);
    }

    /*
     *
     * wordnet functions
     * 
    */
    
    public void test_wordnet(String s) throws Exception {
        WordnetStemmer stemmer = new WordnetStemmer(dict);
        List<String> stems = stemmer.findStems(s);
        for(String stem : stems) {
            for(POS pos: POS.values()) {
                IIndexWord idxWord = dict.getIndexWord(stem, pos);
                if (idxWord == null) {
                    continue;
                }
                System.out.println("\n<<" + stem + ">> pos = " + pos);
                List<IWordID> words = idxWord.getWordIDs();
                for(IWordID wordID : words) {
                    IWord word = dict.getWord(wordID);
                    ISynset synset = word.getSynset();
                    System.out.println("");
                    System.out.println("    Id = " + wordID);
                    System.out.println("    Lemma = " + word.getLemma());
                    System.out.println("    Gloss = " + synset.getGloss());
                    System.out.println("    Verb Frames = " + word.getVerbFrames());
                    for(Pointer pointer : Pointer.values()) {
                        List<ISynsetID> pointerSynsetIDs = 
                            synset.getRelatedSynsets(pointer);
                        if (pointerSynsetIDs.size() > 0) {
                            System.out.print("        " + pointer + " -> ");
                            for(ISynsetID pointerSynsetID : pointerSynsetIDs) {
                                ISynset pointerSynset = dict.getSynset(pointerSynsetID);
                                List<IWord> pointerSynsetWords = pointerSynset.getWords();
                                
                                for(IWord pointerSynsetWord : pointerSynsetWords) {
                                    System.out.print(pointerSynsetWord.getLemma() + " ");
                                }
                            }
                            System.out.print("\n");
                        }
                    }
                }
            }
        }
    }
    
    public JSONArray wordnet_lookup(String s) throws Exception {
        WordnetStemmer stemmer = new WordnetStemmer(dict);
        List<String> stems = stemmer.findStems(s);
        
        JSONArray res = new JSONArray();
        
        for(String stem : stems) {
            
            JSONArray resObj = new JSONArray();

            for(POS pos: POS.values()) {
                IIndexWord idxWord = dict.getIndexWord(stem, pos);
                if (idxWord == null) {
                    continue;
                }
                //System.out.println("\n<<" + stem + ">> pos = " + pos);
                List<IWordID> words = idxWord.getWordIDs();

                for(IWordID wordID : words) {
                    IWord word = dict.getWord(wordID);
                    ISynset synset = word.getSynset();
                    
                    JSONObject wordObj = new JSONObject();
                    wordObj.put("id", ""+wordID);
                    wordObj.put("lemma", ""+word.getLemma());
                    wordObj.put("gloss", ""+synset.getGloss());
                    wordObj.put("pos", ""+pos);
                    wordObj.put("stem", stem);
                    
                    /*
                    System.out.println("");
                    System.out.println("    Id = " + wordID);
                    System.out.println("    Lemma = " + word.getLemma());
                    System.out.println("    Gloss = " + synset.getGloss());
                    */
                    for(Pointer pointer : Pointer.values()) {
                        List<ISynsetID> pointerSynsetIDs = 
                            synset.getRelatedSynsets(pointer);
                        if (pointerSynsetIDs.size() > 0) {
                        
                            JSONArray pointerJsonObj = new JSONArray();
                            
                            //System.out.print("        " + pointer + " -> ");
                            for(ISynsetID pointerSynsetID : pointerSynsetIDs) {
                                ISynset pointerSynset = dict.getSynset(pointerSynsetID);
                                List<IWord> pointerSynsetWords = pointerSynset.getWords();
                                
                                for(IWord pointerSynsetWord : pointerSynsetWords) {
                                    //System.out.print(pointerSynsetWord.getLemma() + " ");
                                    
                                    //
                                    JSONObject pointerWordObj = new JSONObject();
                                    pointerWordObj.put("lexical_id", ""+pointerSynsetWord.getLexicalID());
                                    pointerWordObj.put("lemma", ""+pointerSynsetWord.getLemma());
                                    pointerWordObj.put("gloss", ""+pointerSynset.getGloss());
                                    pointerJsonObj.put(pointerWordObj);
                                }
                            }
                            //System.out.print("\n");
                            wordObj.put("_" + pointer, pointerJsonObj);
                        }
                    }
                    resObj.put(wordObj);
                }
            }
            res.put(resObj);
        }
        return res;
    }

    
public static String replaceOld(
    final String aInput,
    final String aOldPattern,
    final String aNewPattern
    ){
     if ( aOldPattern.equals("") ) {
        throw new IllegalArgumentException("Old pattern must have content.");
     }
    
     final StringBuffer result = new StringBuffer();
     //startIdx and idxOld delimit various chunks of aInput; these
     //chunks always end where aOldPattern begins
     int startIdx = 0;
     int idxOld = 0;
     while ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {
       //grab a part of aInput which does not include aOldPattern
       result.append( aInput.substring(startIdx, idxOld) );
       //add aNewPattern to take place of aOldPattern
       result.append( aNewPattern );
    
       //reset the startIdx to just after the current match, to see
       //if there are any further matches
       startIdx = idxOld + aOldPattern.length();
     }
     //the final chunk will go to the end of aInput
     result.append( aInput.substring(startIdx) );
     return result.toString();
    }

    
    public static void main(String args[]) throws Exception {
        NLP nlp = new NLP();
        if (args[0].equals("test_wordnet")) {
            nlp.test_wordnet(args[1]);
        } else if (args[0].equals("entities")) {
            String text = Files.readFromFile(new File(args[1]));
            System.out.println("extracting entities from " + text);
            System.out.println(nlp.extract_entities(text));
        } else if (args[0].equals("pos")) {
            String filesListText = Files.readFromFile(new File(args[1]));
            String[] filenames = filesListText.split("\n");
            for(int j=0; j<filenames.length-1; j++) {
                String[] fnar = filenames[j].split("\\/");
                String fnstr = fnar[fnar.length-1];
                String[] fn_words = fnstr.split("-");
                List<String> fnw = new ArrayList<String>();
                String compname = "";
                for(String fn_word : fn_words) {
                    String wsec = fn_word.replaceAll("\\.txt", "").trim();
                    fnw.add(wsec);
                    compname += wsec + " ";
                }
                compname = compname.trim();
                while(compname.indexOf("  ")!=-1) {
                    compname = NLP.replaceOld(compname, "  ", " ");
                }
                //System.out.println(compname);
                //System.out.println("______");
                System.out.println("\n## " + filenames[j]);
                String text = Files.readFromFile(new File(filenames[j]));
                //text = text.replaceAll("\"" + compname + "\"", "");
                //text = NLP.replaceOld(text, compname, "<COMPANY>");
                //System.out.println("text = '''" + text + "'''");
                nlp.process_pos(text);
                String[] tokens = nlp.get_pos_tokens();
                String[] tags = nlp.get_pos_tags();
                /*
                JSONArray pos_ar = new JSONArray();
                for(int i=0; i<tokens.length; i++) {
                    JSONObject obj = new JSONObject();
                    obj.put("token", tokens[i]);
                    obj.put("tag", tags[i]);
                    pos_ar.put(obj);
                }
                res.put("pos", pos_ar);
                System.out.println(res.toString(4));
                */
                int snum = 0;
                System.out.print(snum + "|");
                GIMP: for(int i=0; i<tokens.length; i++) {
                    String token = tokens[i].trim();
                    for(String fnwstr: fnw) {
                        if (fnwstr.length()  < 3) continue;
                        if (fnwstr.equalsIgnoreCase(token)) {
                            System.out.print("{C} ");
                            continue GIMP;
                        }
                    }
                        
                    if (token.length() > 2 && token.toUpperCase().equals(token)) {
                        // ignore all upper case tokens
                        continue;
                    }
                    String tag = tags[i];
                    
                    if (tag.equals(",")) {
                        System.out.print(", ");
                        continue;
                    }                    
                    
                    if (tag.equals("nns$")) { // possesive plural noun, e.g., 's
                        continue;  
                    } else if (tag.equals("cd") || tag.equals("od")) { // cardinal number
                        System.out.print("# ");
                        continue;
                    } else if (
                        !tag.equals("nil") &&
                        /* !tag.equals("nns") && */
                        tag.startsWith("n")) { // noun, excluding plural noun
                        //System.out.print("[" + tag + ":" + token.toUpperCase() + "] ");
                        System.out.print(tag + ":" + token + " ");
                        continue;
                    } else if (tag.equals("jj")) { // adjective
                        // "New" Jersey, thinks "New" is an adjective
                        if (token.substring(0,1).toUpperCase().equals(token.substring(0,1))) continue;
                        //System.out.print("(" + tag + ":" + token.toUpperCase() + ") ");
                        //System.out.print(tokens[i] + " ");
                        System.out.print(tag + ":" + token + " ");
                    } else if (tag.startsWith("v")) {
                        System.out.print(tag + ":" + token + " ");
                    } else if (tags[i].equals(".")) {
                        System.out.println(".");
                        snum++;
                        System.out.print(snum + "|");
                    } else {
                        //System.out.print(tags[i] + ":" + tokens[i] + " ");
                        System.out.print(tag + ":" + tokens[i] + " ");
                    }
                }
                //System.out.println("\n-- -- --");
            }
        } else if (args[0].equals("wordnet_lookup")) {
            System.out.println(nlp.wordnet_lookup(args[1]).toString(4));
        } else {
            System.out.println("no such command");
        }
            
    }
    
}
