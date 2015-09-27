package com.github.mongoutils.lucene;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.junit.Before;
import org.junit.Test;

import com.github.mongoutils.collections.DBObjectSerializer;
import com.github.mongoutils.collections.MongoConcurrentMap;
import com.github.mongoutils.collections.SimpleFieldDBObjectSerializer;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class CodeSample {

	@Before
	public void setUp() throws Exception {
		// Clean
		new MongoClient("localhost", 27017).getDB("mongo-lucene-test").getCollection("samplecollection").drop();
	}

	@Test
	public void sampleUsage() throws Exception {
		// Prepare mongo connection
		MongoClient mongo = new MongoClient("localhost", 27017);
		DB db = mongo.getDB("mongo-lucene-test");
		DBCollection dbCollection = db.getCollection("samplecollection");

		// Prepare storage
		DBObjectSerializer<String> keySerializer = new SimpleFieldDBObjectSerializer<String>("key");
		DBObjectSerializer<MapDirectoryEntry> valueSerializer = new MapDirectoryEntrySerializer("value");
		ConcurrentMap<String, MapDirectoryEntry> store = new MongoConcurrentMap<String, MapDirectoryEntry>(dbCollection, keySerializer, valueSerializer);

		// Create lucene directory
		Directory dir = new MapDirectory(store);

		// Write content to lucene
		IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));
		// Index first document
		Document doc = new Document();
		doc.add(new TextField("title", "My file's content ...", Field.Store.YES));
		writer.addDocument(doc);
		// Index second document
		Document doc0 = new Document();
		doc0.add(new TextField("title", "Other content", Field.Store.YES));
		writer.addDocument(doc0);
		writer.close();

		// Search for our content
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(10);

		Query q = new QueryParser("title", new StandardAnalyzer()).parse("My file");
		searcher.search(q, collector);

		// Check search results
		ScoreDoc[] topDocs = collector.topDocs().scoreDocs;
		assertEquals(1, topDocs.length);
		assertEquals("My file's content ...", searcher.doc(topDocs[0].doc).getField("title").stringValue());
	}

}
