package com.alienobject.nlp.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

class JSONMemoryIndexer {

    private RAMDirectory idx;
    private IndexWriter writer;
    private IndexSearcher searcher = null;
    private ArrayList<String> g_fields = new ArrayList<String>();
    IndexReader reader = null;

    public JSONMemoryIndexer() throws Exception {
        idx = new RAMDirectory();
        /* todo: pluggable analyzer */
        writer = new IndexWriter(idx, new StandardAnalyzer(), true);
        //writer = new IndexWriter(idx, new SnowballAnalyzer("English"), true);
    }

    /* j: the only special field is "id" which is stored unindexed
     *   all other fields are stored and indexed
     *   NOTE: "id" field is required
    */
    public synchronized void add(JSONObject j) throws Exception {
        Document doc = new Document();

        Iterator md_it = j.keys();
        while (md_it.hasNext()) {
            String k = (String) md_it.next();
            if (k.equals("id")) continue;
            String v = j.getString(k);
            doc.add(new Field(k, v, Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.YES));
            if (!g_fields.contains(k)) g_fields.add(k);
        }
        doc.add(new Field("id", j.getString("id"),
                Field.Store.YES, Field.Index.NO));
        writer.addDocument(doc);
    }

    synchronized void optimize() throws Exception {
        writer.optimize();
    }

    synchronized void close() throws Exception {
        writer.close();
    }

    synchronized void makeSearchable() throws Exception {
        optimize();
        close();
        searcher = new IndexSearcher(idx);
    }

    public HashMap computeSimilarity(String[] fields) {
        try {
            if (searcher == null) {
                //System.out.println("<JSONMemoryIndexer/search> Automatically optimizing, closing & creating searcher.");
                makeSearchable();
            }

            IndexReader reader = IndexReader.open(idx);
            IndexSearcher searcher = new IndexSearcher(reader);
            HashMap sim = new HashMap();
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.document(i);
                Document[] docs = docsLike(reader, fields, i, 10);
                if (docs.length == 0) {
                    sim.put(doc.get("uid"), new JSONArray());
                    continue;
                }
                JSONArray simres = new JSONArray();
                for (int j = 0; j < docs.length; j++) {
                    Document likeThisDoc = docs[j];
                    simres.put(likeThisDoc.get("uid"));
                    //for(String fn:fields) {
                    //System.out.println("  -> " + likeThisDoc.get(fn));
                    //}
                }
                sim.put(doc.get("uid"), simres);
            }
            return sim;
        } catch (Exception ex) {
            return new HashMap();
        }
    }

    private Document[] docsLike(IndexReader reader, String[] fields, int id, int max) throws IOException {
        Document doc = reader.document(id);
        BooleanQuery ltq = new BooleanQuery();
        for (String fn : fields) {
            TermFreqVector vector = reader.getTermFreqVector(id, fn);
            BooleanQuery bq = new BooleanQuery();
            for (int j = 0; j < vector.size(); j++) {
                TermQuery tq = new TermQuery(new Term(fn, vector.getTerms()[j]));
                bq.add(tq, BooleanClause.Occur.SHOULD); // false, false);
            }
            ltq.add(bq, BooleanClause.Occur.SHOULD); // false, false);
        }

        // exclude current doc
        ltq.add(new TermQuery(
                new Term("uid", doc.get("uid"))), BooleanClause.Occur.MUST_NOT);

        //System.out.println("  Query: " + ltq.toString("contents"));
        Hits hits = searcher.search(ltq);
        int size = hits.length();
        if (size > 5) size = 5;
        //if (max > hits.length()) size = hits.length();
        Document[] docs = new Document[size];
        for (int i = 0; i < size; i++) {
            //System.out.println(">> " + hits.doc(i));
            docs[i] = hits.doc(i);
        }
        return docs;
    }


    public synchronized ArrayList<JSONObject> search(String queryString)
            throws Exception {

        //System.out.println("queryString: " + queryString);

        if (searcher == null) {
            //System.out.println("<JSONMemoryIndexer/search> Automatically optimizing, closing & creating searcher.");
            makeSearchable();
        }

        String[] qf = new String[g_fields.size()];
        for (int i = 0; i < g_fields.size(); i++) {
            qf[i] = g_fields.get(i);
        }

        MultiFieldQueryParser qp = new MultiFieldQueryParser(qf, new StandardAnalyzer() /*, Map boosts */);
        //MultiFieldQueryParser qp = new MultiFieldQueryParser(qf, new SnowballAnalyzer("English") /*, Map boosts */);
        Query query = qp.parse(queryString);

        Hits hits = searcher.search(query);

        int hitCount = hits.length();
        if (hitCount == 0) {
            return new ArrayList<JSONObject>();
        } else {
            //System.out.println("<JSONMemoryIndexer> " + hitCount + " results (" + queryString + ")");
        }

        ArrayList<JSONObject> res = new ArrayList<JSONObject>();

        // Iterate over the Documents in the Hits object
        for (int i = 0; i < hitCount; i++) {
            Document doc = hits.doc(i);

            JSONObject jo = new JSONObject();
            ArrayList<Field> fields = (ArrayList<Field>) doc.getFields();
            for (Field f : fields) {

                String field_nm = f.name();
                String field_val = doc.get(field_nm);
                if (field_val == null) continue;
                jo.put(field_nm, field_val);
            }
            res.add(jo);
        }
        return res;
    }

}
