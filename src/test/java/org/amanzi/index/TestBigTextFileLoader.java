package org.amanzi.index;

import java.io.IOException;
import java.util.Iterator;

import junit.framework.TestCase;

import org.amanzi.index.loader.BigTextFileLoader;
import org.amanzi.index.util.FileUtilities;
import org.junit.Test;


public class TestBigTextFileLoader extends TestCase {
	
	@Test
	public void testFileLoader() {
		System.out.println("Loading file: test.txt");
		int lineCount = 0;
		BigTextFileLoader file = new BigTextFileLoader();
		try {
			file.open("test.txt");
			
			String delimiter;
			for (String line : file) {
				if (lineCount == 0) {
					delimiter = FileUtilities.getDelimiter(line);
					System.out.println("Line Delimiter: [" + delimiter + "]"
							+ "\nFile Header: [" + line + "]");
				}
			    lineCount ++;
			}
			System.out.println("Text file=>" + file.getFileName() + "@" 
					+ file.getFileSize() / 1024 + "KB: " + lineCount + " lines readed.");
		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} finally {
			file.close();
		}
		
		System.out.println("Loading file: test.zip");
		lineCount = 0;
		file = new BigTextFileLoader();
		try {
			file.open("test.zip");
			
			String delimiter;
			for (String line : file) {
				if (lineCount == 0) {
					delimiter = FileUtilities.getDelimiter(line);
					System.out.println("Line Delimiter: [" + delimiter + "]"
							+ "\nFile Header: [" + line + "]");
				}
			    lineCount ++;
			}
			System.out.println("Zip file=>" + file.getFileName() + "@" 
					+ file.getFileSize() / 1024 + "KB: " + lineCount + " lines readed.");
		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} finally {
			file.close();
		}

		// use Iterator explicitly and without calling hasNext (bugfix test)
		System.out.println("Loading file: test.zip");
		file = new BigTextFileLoader();
		try {
			file.open("test.zip");
			
			Iterator<String> it = file.iterator();
			
			// calling hasNext shouldn't move iterator cursor
			it.hasNext();
			it.hasNext();
			it.hasNext();
			
			String line = it.next();
			int itLineCount = 0;
			while (line != null) {
				itLineCount++;
				line = it.next();
			}
			
			assertEquals(lineCount, itLineCount);
			
			System.out.println("Zip file=>" + file.getFileName() + "@" 
					+ file.getFileSize() / 1024 + "KB: " + lineCount + " lines readed.");
		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} finally {
			file.close();
		}		
	}
}
