# mongo-lucene (for mailor)

Fork of [mongo-lucene](https://github.com/rstiller/mongo-lucene) created for testing of lucene&mongo powered search on [mailor](http://mailor.us/) project.

Modifications:

- Java 8 is now required
- Support for Lucene version 5.3.0
- Maven was replaced with [gradle](http://gradle.org/)
- Minor code changes

## License

Apache 2.0 License (http://www.apache.org/licenses/LICENSE-2.0)

## Requirements / Dependencies

* Java 8 JDK 
* Apache Lucene 5.3.0 
* MongoDB Java-Driver 2.12.3
* MongoDB 3.x

## Building

1. Download and build from sources:

		git clone https://github.com/krablak/mongo-lucene.git
		cd mongo-lucene/
		./gradlew clean build -x test
		# Or without tests skipping when your mongodb required for tests instance is running
		#./gradlew clean build
	
2. Ready to use JAR file is present at: */build/libs/mongo-lucene-mailor-x.x.x.jarr*

## Usage

Following usage sample is present in file *mongo-lucene/src/test/java/com/github/mongoutils/lucene/CodeSample.java*:

(*Don't forget to startup your MongoDB before running sample code.*)


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