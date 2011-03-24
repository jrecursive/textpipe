package com.alienobject.textpipe.text;

import com.wcohen.ss.BasicStringWrapperIterator;
import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.SoftTFIDF;
import com.wcohen.ss.api.Tokenizer;
import com.wcohen.ss.tokens.SimpleTokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

class ExecMatcher {
    private Tokenizer tokenizer;
    private SoftTFIDF distance;
    private boolean trained = false;

    private ExecMatcher() {
        tokenizer = new SimpleTokenizer(false, true);
        distance = new SoftTFIDF(tokenizer, new JaroWinkler(), 0.8);
        try {
            train();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void train()
            throws Exception {
        System.out.println("training NameMatcher ...");
        double minTokenSimilarity = 0.8;
        List list = new ArrayList();
        File file = new File("data/TFIDF_corpus.txt");
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = null;

        reader = new BufferedReader(new FileReader(file));
        String text = null;
        while ((text = reader.readLine()) != null) {
            list.add(distance.prepare(text));
        }

        distance.train(new BasicStringWrapperIterator(list.iterator()));
        trained = true;
    }

    double score(String s, String t)
            throws Exception {
        if (!trained) train();

        // compute the similarity
        double d = distance.score(s, t);

        // print it out
        /*
        System.out.println("========================================");
        System.out.println("String s:  '"+s+"'");
        System.out.println("String t:  '"+t+"'");
        System.out.println("Similarity: "+d);

        // a sort of system-provided debug output
        System.out.println("Explanation:\n" + distance.explainScore(s,t));
        */

        // this is equivalent to d, above, but if you compare s to
        // many strings t1, t2, ... it's a more efficient to only
        // 'prepare' s once.

        return d;
    }

    public void main(String args[])
            throws Exception {
        ExecMatcher em = new ExecMatcher();
        em.score("Darryl R. James", "Daryl James");
    }

}