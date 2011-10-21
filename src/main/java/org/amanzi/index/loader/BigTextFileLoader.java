package org.amanzi.index.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BigTextFileLoader implements Iterable<String> {
	
    private BufferedReader _reader;
    private long _size = 0;
    private String _name;
    
 
    public BigTextFileLoader(String filePath) {
		try {
			_reader = getBufferedReader(filePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
    public void close() {
		try {
		    _reader.close();
		} catch (Exception ex) {}
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
    			ZipFile zf = new ZipFile(file);
    			Enumeration<? extends ZipEntry> entries = zf.entries();
				if (entries.hasMoreElements()) {
					ZipEntry ze = (ZipEntry) entries.nextElement();
					_name = ze.getName();
					_size = ze.getSize();
					return new BufferedReader(new InputStreamReader(
							zf.getInputStream(ze)));		
				}
    		} else {
        		_size = file.length();
        		_name = name;
        		if (_name.toLowerCase().endsWith("gz"))
        			return new BufferedReader(new InputStreamReader(
        					new GZIPInputStream(new FileInputStream(file))));
        		else
        			return new BufferedReader(new InputStreamReader(
        					new FileInputStream(file)));
        	}
    	}
    	return null;
    }
 
    private class FileIterator implements Iterator<String> {
		private String _currentLine;
	 
		public boolean hasNext() {
		    try {
		    	if (_reader != null)
		    		_currentLine = _reader.readLine();
		    } catch (Exception ex) {
				_currentLine = null;
				ex.printStackTrace();
		    }
	 
		    return _currentLine != null;
		}
	 
		public String next() {
		    return _currentLine;
		}
	 
		public void remove() {
		}
    }
}