package com.github.mongoutils.lucene;

import static java.lang.String.format;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Accountable;

public class MapDirectory extends BaseDirectory implements Accountable {

	public static final int DEFAULT_BUFFER_SIZE = 1024;

	ConcurrentMap<String, MapDirectoryEntry> store;
	protected final AtomicLong sizeInBytes = new AtomicLong();
	private int bufferSize = DEFAULT_BUFFER_SIZE;

	public MapDirectory(final ConcurrentMap<String, MapDirectoryEntry> store) throws IOException {
		super(new SingleInstanceLockFactory());
		this.store = store;
	}

	@Override
	public String[] listAll() throws IOException {
		String[] files = new String[store.size()];
		int index = 0;

		for (String file : store.keySet()) {
			files[index++] = file;
		}

		return files;
	}

	@Override
	public void deleteFile(final String name) throws IOException {
		ensureOpen();
		MapDirectoryEntry file = store.remove(name);
		if (file != null) {
			sizeInBytes.addAndGet(-file.sizeInBytes);
		} else {
			throw new FileNotFoundException(name);
		}
	}

	@Override
	public long fileLength(final String name) throws IOException {
		ensureOpen();
		if (!store.containsKey(name)) {
			throw new FileNotFoundException(name);
		}
		return store.get(name).getLength();
	}

	@Override
	public void close() throws IOException {
		isOpen = false;
		store.clear();
	}

	@Override
	public IndexOutput createOutput(String s, IOContext ioContext) throws IOException {
		ensureOpen();
		MapDirectoryEntry file = new MapDirectoryEntry();
		file.setBufferSize(bufferSize);
		store.put(s, file);
		return new MapDirectoryOutputStream(file, s, store, bufferSize);
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		// TODO Missing method implementation needs investigation
		/*
		 * ensureOpen(); MapDirectoryEntry file; //Set<String> toSync = new
		 * HashSet<String>(names); //toSync.retainAll(staleFiles); for (String
		 * name : names) { if (!store.containsKey(name)) { throw new
		 * FileNotFoundException(name); } file = store.get(name);
		 * file.setLastModified(System.currentTimeMillis()); store.put(name,
		 * file); }
		 */

	}

	@Override
	public IndexInput openInput(String s, IOContext ioContext) throws IOException {
		ensureOpen();
		if (!store.containsKey(s)) {
			throw new FileNotFoundException(s);
		}
		return new MapDirectoryInputStream(s, store.get(s));
	}

	@Override
	public long ramBytesUsed() {
		ensureOpen();
		return sizeInBytes.get();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<Accountable> getChildResources() {
		return (Collection) this.store.values();
	}

	@Override
	public void renameFile(String source, String dest) throws IOException {
		ensureOpen();
		if (store.containsKey(source)) {
			store.put(dest, store.get(source));
			store.remove(source);
		} else {
			throw new FileNotFoundException(format("No record was found under name '%s' in storage.", source));
		}
	}
}
