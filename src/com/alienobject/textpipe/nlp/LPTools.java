package com.alienobject.textpipe.nlp;

import com.aliasi.chunk.AbstractCharLmRescoringChunker;
import com.aliasi.chunk.Chunk;
import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ScoredObject;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.btext.utils.Constants.*;

class LPTools {
    private static final TokenizerFactory tokenizer_Factory = new IndoEuropeanTokenizerFactory();
    // tokenizer to extract tokens and whitespace
    private HmmDecoder decoder = null;
    private static SentenceModel SENTENCE_MODEL = new IndoEuropeanSentenceModel();
    private static final int DOC_LENGTH_MAXLIMIT = DOC_PASSAGE_LIMIT
            * MAX_SENTENCE_LEN; // max. size of document that will be indexed
    public static final int DOC_LENGTH_MINLIMIT = 10; // min. size of document
    // that will be indexed
    private int maxCharsPerSentence = MAX_SENTENCE_LEN; // max. no. of chars in
    // a sentence
    private int minCharsPerSentence = MIN_SENTENCE_LEN; // min. no. of chars in
    // a sentence
    private String[] tokens = null;
    private String[] tags = null;
    private String[] whites = null;
    private int[] sentenceBoundaries = null;
    private int currentTokenPos = 0;
    private int numTokens = 0;
    protected boolean firstTime = true;
    private AbstractCharLmRescoringChunker chunker = null;
    private static Map<String, double[]> cacheMap;
    private static Map<String, double[]> cacheLogMap;

    /*
    private Pattern collapsePattern =
      org.btext.utils.StringTools.getCollapsePattern(getEntityTypes());
    */
    // pattern to collapse consecutive entity types
    private static Logger logger = Logger.getLogger(LPTools.class.getName());

    private static byte[] POS_TAGGER_MODEL_BYTES = null;
    private static byte[] ENTITY_MODEL_BYTES = null;

    public LPTools() {
        try {
            init_lptools();
            create_lptools_objects();
        } catch (Exception ex) {
            ex.printStackTrace();
            //System.exit(-1);
        }
    }

    private synchronized void init_lptools() {
        if (POS_TAGGER_MODEL_BYTES == null) {
            cacheMap = new ConcurrentHashMap<String, double[]>();
            cacheLogMap = new ConcurrentHashMap<String, double[]>();
            try {
                POS_TAGGER_MODEL_BYTES = getBytesFromFile(new File(POS_TAGGER_MODEL));
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.exit(-1);
            }
        }
        if (ENTITY_MODEL_BYTES == null) {
            try {
                ENTITY_MODEL_BYTES = getBytesFromFile(new File(ENTITY_MODEL));
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.exit(-1);
            }
        }
    }

    private void create_lptools_objects() {
        ObjectInputStream oi = null;
        try {
            SENTENCE_MODEL = new IndoEuropeanSentenceModel();
            oi = new ObjectInputStream(new ByteArrayInputStream(POS_TAGGER_MODEL_BYTES));
            HiddenMarkovModel hmm = (HiddenMarkovModel) oi.readObject();
            decoder = new HmmDecoder(hmm, cacheMap, cacheLogMap);
        } catch (IOException ie) {
            logger.error("IO Error: " + ie.getMessage());
        } catch (ClassNotFoundException ce) {
            logger.error("Class error: " + ce.getMessage());
        } finally {
            if (oi != null) {
                try {
                    oi.close();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }

        try {
            oi = null;
            oi = new ObjectInputStream(new ByteArrayInputStream(ENTITY_MODEL_BYTES));
            chunker = (AbstractCharLmRescoringChunker) oi.readObject();
            /*
            chunker = (AbstractCharLmRescoringChunker) 
                AbstractExternalizable.readExternal(oi);
            */
        } catch (IOException ie) {
            logger.error("init_lptools : IO Error : could not read " + ie.getMessage());
        } catch (ClassNotFoundException ce) {
            logger.error("init_lptools : Class Error : " + ce.getMessage());
        }
    }

    /**
     * Generate an array of tokens
     * @param sentence
     */
    public void extractTokens(String sentence) {
        char[] cs = sentence.toCharArray();
        Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0, cs.length);
        setTokens(tokenizer.tokenize());
    }

    /**
     * Generate an annotated sentence with a select group of parts of speech and
     * return
     *
     * @param text to be annotated with parts of speech
     */
    public void extractPOS(String text) {
        buildSentences(text);
        String sentence = "";
        List<String> ltags = new ArrayList<String>();
        while ((sentence = nextSentence()) != null) {
            // first get the tokens
            char[] cs = sentence.toCharArray();
            Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0, cs.length);
            String[] tokens = tokenizer.tokenize();

            // then get the tags
            List<String> tempTags = Arrays.asList(decoder.firstBest(tokens));
            ltags.addAll(tempTags);
        }
        tags = new String[ltags.size()];
        ltags.toArray(tags);
    }

    /**
     * Pass the sentence to be tagged and return the scores for tagged sentences
     *
     * @param sentence
     */
    public void getPOSDump(String sentence) {
        char[] cs = sentence.toCharArray();
        Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0, cs.length);
        String[] tokens = tokenizer.tokenize();
        Iterator nBest = decoder.nBestConditional(tokens);
        int lines = 0;
        while (nBest.hasNext()) {
            ScoredObject<String[]> tagScores = (ScoredObject<String[]>) nBest.next();
            System.out.print(lines + ": " + tagScores.score() + " ");
            String[] tags = tagScores.getObject();
            for (int i = 0; i < tags.length; i++)
                System.out.print(" " + tokens[i] + "_" + tags[i]);
            System.out.println("");
            if (++lines == 3)
                break;
        }
    }

    /**
     * Generate an annotated sentence with a select group of parts of speech and
     * return
     *
     * @param sentence to be annotated with parts of speech
     * @return annotated sentence
     */
    public String extractXMLPOS(String sentence) {
        StringBuffer xmlOutput = new StringBuffer();
        char[] cs = sentence.toCharArray();
        List<String> tokensList = new ArrayList<String>();
        List<String> whitesList = new ArrayList<String>();
        Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0, cs.length);
        tokenizer.tokenize(tokensList, whitesList);
        tokens = new String[tokensList.size()];
        tokensList.toArray(tokens);
        whites = new String[whitesList.size()];
        whitesList.toArray(whites);
        String[] tags = decoder.firstBest(tokens);

        int len = tokens.length;
        for (int i = 0; i < len; i++) {
            // *-- set the adjective tags
            if (tags[i].startsWith("j") || tags[i].equals("cd")
                    || tags[i].endsWith("od")) {
                xmlOutput.append("<Adjective>");
                xmlOutput.append(tokens[i]);
                xmlOutput.append("</Adjective>");
            }
            // *-- next, the noun tags
            else if (tags[i].startsWith("n")) {
                xmlOutput.append("<Noun>");
                xmlOutput.append(tokens[i]);
                xmlOutput.append("</Noun>");
            }
            // *-- finally, the verb tags, skipping auxiliary verbs
            else if (tags[i].startsWith("v")) {
                xmlOutput.append("<Verb>");
                xmlOutput.append(tokens[i]);
                xmlOutput.append("</Verb>");
            }
            // *-- skip all other tags
            else {
                //xmlOutput.append(" ");
                xmlOutput.append(tokens[i]);
            }
            xmlOutput.append("<White>");
            xmlOutput.append(whites[i + 1]);
            xmlOutput.append("</White>");
        }
        return (xmlOutput.toString());
    }

    /**
     * Build the list of tokens, white spaces, and sentence boundaries for the
     * paragraph passed
     *
     * @param in text chunk
     */
    public void buildSentences(String in) {
        // limit the length of the input string
        if (in.length() > DOC_LENGTH_MAXLIMIT)
            in = in.substring(0, DOC_LENGTH_MAXLIMIT - 1);

        // tokenize the string
        List<String> tokensList = new ArrayList<String>();
        List<String> whitesList = new ArrayList<String>();
        Tokenizer tokenizer = tokenizer_Factory.tokenizer(
                in.toCharArray(), 0, in.length());
        tokenizer.tokenize(tokensList, whitesList);
        tokens = new String[tokensList.size()];
        tokensList.toArray(tokens);
        whites = new String[whitesList.size()];
        whitesList.toArray(whites);

        // identify sentence boundaries
        sentenceBoundaries = SENTENCE_MODEL.boundaryIndices(tokens, whites);

        // set a default sentence boundary, if no sentence boundaries were found
        if (sentenceBoundaries.length < 1) {
            sentenceBoundaries = new int[1];
            sentenceBoundaries[0] = tokensList.size() - 1;
        }

        // set the current token position and the total number of tokens
        currentTokenPos = 0;
        numTokens = tokensList.size();
    }

    /**
     * Fetch the next sentence
     *
     * @return String
     */
    public String nextSentence() {
        StringBuffer pSentence = new StringBuffer();
        int i = currentTokenPos;
        boolean builtSentence = false;

        int nextBoundary = nextSentenceBoundary(i);

        while ((i < numTokens) && !builtSentence) {
            //if (isSentenceBoundary(i)) {
            if (i == nextBoundary) { // check if the current token position is at
                // a sentence boundary
                pSentence.append(tokens[i]).append(whites[i + 1]);
                i++;
                builtSentence = true; // if sentence length exceeds max.
            } else if ((pSentence.length() + tokens[i].length() + whites[i + 1]
                    .length()) > MAX_SENTENCE_LEN) {
                builtSentence = true;
            } else { // otherwise, keep adding tokens and whitespaces
                pSentence.append(tokens[i]).append(whites[i + 1]);
                i++;
            }
        }

        currentTokenPos = i;
        String sentence = null;
        if (pSentence.length() > 0) {
            sentence = pSentence.toString();
            sentence = sentence.replaceAll("\\n", " ");
            sentence = sentence.replaceAll("\\s+", " ");
        }
        return (sentence);
    }

    private int nextSentenceBoundary(int j) {
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            if (j < sentenceBoundaries[i])
                return sentenceBoundaries[i];
        }
        return -1;
    }

    private boolean isSentenceBoundary(int j) {
        boolean boundary = false;
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            if (j == sentenceBoundaries[i])
                boundary = true;
        }
        return boundary;
    }

    // ** Read the model file for NE extraction */
    public boolean setforIE() {
        return true;
    }

    /**
     * Generate an annotated sentence with named entities such as person, place,
     * org, weight, speed, etc. and return. Set the default number of entities to
     * the square root of the sentence length
     *
     * @param sentence to be annotated with named entities
     * @return annotated sentence
     */
    public ArrayList getEntities(String sentence) {
        int num = (int) (Math.sqrt(sentence.length()));
        return getEntities(sentence, num);
    }

    ArrayList getEntities(String sentence, int numEntities) {
        char[] cs = sentence.toCharArray();
        Iterator it = chunker.nBestChunks(cs, 0, cs.length, numEntities);

        HashMap<String, String> startH = new HashMap<String, String>();
        // hash for the start locations of entities

        HashMap<String, String> endH = new HashMap<String, String>();
        // hash for the end locations of entities

        HashMap<String, String> typeH = new HashMap<String, String>();
        // hash for the types of entities

        String[] entities = new String[numEntities];
        for (int i = 0; i < numEntities; i++)
            entities[i] = ""; // *-- initialize the list of entities
        for (int i = 0; it.hasNext(); i++) {
            Chunk chunk = (Chunk) it.next();
            int start = chunk.start();
            int end = chunk.end();
            String ent = sentence.substring(start, end);
            String type = chunk.type();
            // populate the hashes and the entities array
            startH.put(ent, String.valueOf(start));
            endH.put(ent, String.valueOf(end));
            typeH.put(ent, type);
            entities[i] = ent;
        }

        // sort the entities in descending order of length and pick the
        // largest non-overlapping entities
        Arrays.sort(entities, new StringLenComparator());
        int sentenceLen = sentence.length();
        BitSet occupied = new BitSet(sentenceLen); // flags to indicate if the
        // character of the sentence is
        // part of an entity
        BitSet validEntity = new BitSet(numEntities); // flags to indicate if the
        // entity should be added to
        // the list of returned ents.
        int validEntities = 0;
        for (int i = 0; i < entities.length; i++) {
            if (startH.get(entities[i]) == null || startH.get(entities[i]).equals(""))
                continue;
            int start = Integer.parseInt(startH.get(entities[i]));
            int end = Integer.parseInt(endH.get(entities[i]));

            // *-- check for overlap
            boolean overlap = false;
            for (int j = start; j < end; j++)
                if (occupied.get(j))
                    overlap = true;

            // *-- add to the list of entities, if no overlap
            validEntity.set(i, (!overlap));
            if (!overlap)
                for (int j = start; j < end; j++)
                    occupied.set(j, true);
            validEntities++;

        }

        // *-- build the arrays of entity types, start, and end locations
        int[] startL = new int[validEntities];
        int[] endL = new int[validEntities];
        int entCount = 0;
        for (int i = 0; i < entities.length; i++) {
            if (!validEntity.get(i))
                continue;
            int start = Integer.parseInt(startH.get(entities[i]));
            int end = Integer.parseInt(endH.get(entities[i]));
            startL[entCount] = start;
            endL[entCount] = end;
            entCount++;
        }

        // *-- sort start and end arrays in ascending order
        int[] newStartL = new int[entCount];
        int[] newEndL = new int[entCount];
        for (int i = 0; i < entCount; i++) {
            newStartL[i] = startL[i];
            newEndL[i] = endL[i];
        }
        Arrays.sort(newStartL);
        Arrays.sort(newEndL);

        // *-- build the output string buffer with tags for entities
        ArrayList ents = new ArrayList();

        int currentLoc = 0;
        for (int i = 0; i < entCount; i++) {
            String entity = sentence.substring(newStartL[i], newEndL[i]);
            String etype = org.btext.utils.StringTools.firstLetterUC(typeH.get(entity));
            currentLoc = newEndL[i] + 1;
            ents.add(etype + "|" + entity);
        }

        /*Matcher matcher = collapsePattern.matcher(xmlOutput);
        return (matcher.replaceAll(" "));
        */
        return ents;
    }

    public int getMaxCharsPerSentence() {
        return maxCharsPerSentence;
    }

    public void setMaxCharsPerSentence(int maxCharsPerSentence) {
        this.maxCharsPerSentence = maxCharsPerSentence;
    }

    public int getMinCharsPerSentence() {
        return minCharsPerSentence;
    }

    public void setMinCharsPerSentence(int minCharsPerSentence) {
        this.minCharsPerSentence = minCharsPerSentence;
    }

    public void releaseResources() {
        decoder = null;
        chunker = null;
        decoder = null;
    }

    public String[] getTags() {
        String[] tags = this.tags;
        return tags;
    }

    private void setTags(String[] tags) {
        this.tags = tags;
    }

    public String[] getTokens() {
        String[] tokens = this.tokens;
        return tokens;
    }

    private void setTokens(String[] tokens) {
        this.tokens = tokens;
    }

    /**
     * Inner class to compare two strings by their lengths
     */
    private static class StringLenComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            if (o1 == null || o2 == null)
                return (0);
            Integer i1 = new Integer(o1.length());
            Integer i2 = new Integer(o2.length());
            return (-i1.compareTo(i2));
        }
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length &&
                (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }
}