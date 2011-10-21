package org.amanzi.index.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BigTextFileLoader implements Iterable<String> {

	private ZipFile _zipFile;
    private BufferedReader _reader;
    private long _size = 0;
    private String _name;
    
 
    public void open(String filePath) throws IOException {
		_reader = getBufferedReader(filePath);
    }
    
    public void close() {
    	if (_reader != null) {
	    	try {
	    		_reader.close();
	    		_reader = null;
	    	} catch (IOException e) {
    			// TODO log
    			e.printStackTrace();	    		
	    	}
    	}
    	
    	if (_zipFile != null) {
    		try {
    			_zipFile.close();
    			_zipFile = null;
    		} catch (IOException e) {
    			// TODO log
    			e.printStackTrace();
    		}
    	}
    }
 
    public Iterator<String> iterator() {
    	return new FileIterator();
    }
    
    public long getFileSize() {
    	return _size;
    }
    
    public String getFileName() {
    	return _name;
    }
    
    private BufferedReader getBufferedReader(String filePath) throws IOException {
    	File file = new File(filePath);
    	if (file.isDirectory()) {
    		System.err.println("Invalid to load a directory, please load a file!");
    		return null;
    	} else {
    		String name = file.getName();
    		if (name.toLowerCase().endsWith("zip")) {
    			// One entry only
    			_zipFile = new ZipFile(file);
    			ZipEntry ze = (ZipEntry) _zipFile.entries().nextElement();
				_name = ze.getName();
				_size = ze.getSize();
				return getBufferedReader(_zipFile.getInputStream(ze));		
    		} else {
        		_size = file.length();
        		_name = name;
        		if (_name.toLowerCase().endsWith("gz")) {
        			return getBufferedReader(new GZIPInputStream(new FileInputStream(file)));
        		} else {
        			return getBufferedReader(new FileInputStream(file));
        		}
        	}
    	}    	
    }
 
    private BufferedReader getBufferedReader(InputStream stream) {
    	return new BufferedReader(new InputStreamReader(stream));
    }
    
    private class FileIterator implements Iterator<String> {
    	
		private String _cachedLine = null;
	 
		public boolean hasNext() {
			checkReaderIsOpen();
			
			if (_cachedLine == null) {
				_cachedLine = readLine();
			}
			
			return _cachedLine != null;
		}
	 
		public String next() {
			checkReaderIsOpen();
			
		    if (_cachedLine != null) {
		    	String result = _cachedLine;
		    	_cachedLine = null;
		    	return result;
		    } else {
		    	return readLine();
		    }
		}
	 
		public void remove() {
			throw new UnsupportedOperationException();
		}
	
		private String readLine() {
		    try {
		    	return _reader.readLine();
		    } catch (IOException e) {
				// TODO log
				e.printStackTrace();
				
		    	return null;				
		    }			
		}
		
		private void checkReaderIsOpen() {
			if (_reader == null) {
				throw new IllegalStateException("No open File");
			}
		}
    }
}