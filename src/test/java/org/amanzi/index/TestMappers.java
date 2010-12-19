package org.amanzi.index;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.amanzi.index.mappers.CharacterStringMapper;
import org.amanzi.index.mappers.FloatMapper;
import org.amanzi.index.mappers.IntegerMapper;
import org.amanzi.index.mappers.Mapper;
import org.amanzi.index.mappers.NumberMapper;
import org.junit.Test;

public class TestMappers extends TestCase {

	class MapperResults<T extends Comparable<T>> {
		HashMap<Integer, ArrayList<T>> results = new HashMap<Integer, ArrayList<T>>();
		Mapper<T> mapper;
		int count = 0;
		protected boolean verbose;

		public MapperResults(Mapper<T> mapper, boolean verbose) {
			this.mapper = mapper;
			this.verbose = verbose;
		}

		public void add(T value) {
			getResult(mapper.toKey(value)).add(value);
			count++;
		}

		public ArrayList<T> getResult(int key) {
			ArrayList<T> result = results.get(key);
			if (result == null) {
				result = new ArrayList<T>();
				results.put(key, result);
			}
			return result;
		}

		public List<Integer> getIndexes() {
			ArrayList<Integer> keys = new ArrayList<Integer>(results.keySet());
			Collections.sort(keys);
			Collections.reverse(keys);
			return keys;
		}

		public void print(PrintStream out) {
			if (verbose) {
				out.println("Have results with " + results.size() + " keys:");
				for (Integer key : getIndexes()) {
					ArrayList<T> result = results.get(key);
					Collections.sort(result);
					out.println("\t" + mapper.getRangeText(key) + ":\t("
							+ result.size() + ")" + result);
				}
			}
		}
	}

	class NumberMapperResults<T extends Comparable<T>> extends MapperResults<T> {
		private NumberMapper<T> numberMapper;

		public NumberMapperResults(NumberMapper<T> mapper, boolean verbose) {
			super(mapper, verbose);
			this.numberMapper = (NumberMapper<T>) mapper;
		}

		public void print(PrintStream out) {
			System.out.println("Tested " + count + " numbers in "
					+ getIndexes().size() + " blocks using mapper: " + mapper);
			super.print(System.out);
		}

		public void checkResults(int block) {
			this.print(System.out);
			List<Integer> keys = getIndexes();
			assertEquals("The origin should be the min of the 0th block",
					numberMapper.getMin(0), numberMapper.getOrigin());
			if (block == 10)
				assertEquals("Should be 2*" + block + "+1 keys", 2 * block + 1,
						keys.size());
			int minKey = keys.get(keys.size() - 1) + 1;
			int maxKey = keys.get(0) - 1;
			for (int key = minKey; key < maxKey; key++) {
				assertEquals(
						"Should be " + block + " values in "
								+ mapper.getRangeText(key), block,
						getResult(key).size());
			}
			if (block == 10) {
				assertEquals("Should be only 1 value in 100-109 range", 1,
						getResult(10).size());
				assertEquals("Value in 100-109 range should be 100", "100",
						getResult(10).get(0).toString().substring(0, 3));
				assertEquals(
						"First two digits of all values in -10 (-100..-91) '-9'",
						"-9", getResult(-10).get(0).toString().substring(0, 2));
			}
		}

		public void checkResults(float block) {
			this.print(System.out);
			assertEquals("The origin should be the min of the 0th block",
					numberMapper.getMin(0), numberMapper.getOrigin());
			List<Integer> keys = getIndexes();
			int minKey = keys.get(keys.size() - 1) + 1;
			int maxKey = keys.get(0) - 1;
			float tblock = 0;
			int count = 0;
			float origin = Float.valueOf(numberMapper.getOrigin().toString());
			for (int key = minKey; key < maxKey; key++) {
				count++;
				tblock += getResult(key).size();
				float min = origin + key * block;
				assertEquals("The minimum of the " + key
						+ " block should be the origin " + origin
						+ " plus the block size " + block + " times the index "
						+ key, min, numberMapper.getMin(key));
			}
			float tolerance = block / (5 * count);
			float average = tblock / count;
			float blockMin = block - tolerance;
			float blockMax = block + tolerance;
			assertTrue("Should be an average of nearly " + block
					+ " values, but " + average + " is not > " + blockMin,
					average > blockMin);
			assertTrue("Should be an average of nearly " + block
					+ " values, but " + average + " is not < " + blockMax,
					average < blockMax);
			System.out.println("Found average block size of " + average
					+ " over " + count + " test indexes, which is near "
					+ block + ", or within expected range (" + blockMin + ","
					+ blockMax + ")");
		}
	}

	@Test
	public void testIntegerMapper() throws Exception {
		IntegerMapper mapper = IntegerMapper.withStep(10);
		assertEquals("Zero should map to zero", 0, mapper.toKey(0));
		assertEquals("9 should map to 0", 0, mapper.toKey(9));
		assertEquals("10 should map to 1", 1, mapper.toKey(10));
		assertEquals("-1 should map to -1", -1, mapper.toKey(-1));
		assertEquals("-10 should map to -1", -1, mapper.toKey(-10));
		assertEquals("-11 should map to -2", -2, mapper.toKey(-11));
		assertEquals("100 should map to 10", 10, mapper.toKey(100));
		NumberMapperResults<Integer> results = new NumberMapperResults<Integer>(
				mapper, false);
		for (int i = 100; i > -100; i -= 1) {
			results.add(i);
		}
		results.checkResults(10);
	}

	@Test
	public void testVariousIntegerMappers() throws Exception {
		runIntegerMapperOffsets(-10, 100, 8, -100, 100);
		runIntegerMapperOffsets(-100, 10, 5, -100, 100);
		runIntegerMapperOffsets(100, 200, 20, 100, 200);
		runIntegerMapperOffsets(100, 200, 50, 100, 500);
	}

	private void runIntegerMapperOffsets(int min, int max, int step, int dmin,
			int dmax) throws Exception {
		IntegerMapper mapper = IntegerMapper.withRangeAndStep(min, max, step);
		int origin = (max + min) / 2;
		assertEquals("Origin should be (" + max + "+" + min + ")/2",
				(Integer) origin, mapper.getOrigin());
		NumberMapperResults<Integer> results = new NumberMapperResults<Integer>(
				mapper, false);
		for (int i = dmax; i > dmin; i -= 1) {
			results.add(i);
		}
		results.checkResults(step);
	}

	@Test
	public void testFloatMapper() throws Exception {
		FloatMapper mapper = FloatMapper.withStep(10);
		assertEquals("Zero should map to zero", 0, mapper.toKey(0f));
		assertEquals("9 should map to 0", 0, mapper.toKey(9f));
		assertEquals("10 should map to 1", 1, mapper.toKey(10f));
		assertEquals("-1 should map to -1", -1, mapper.toKey(-1f));
		assertEquals("-10 should map to -1", -1, mapper.toKey(-10f));
		assertEquals("-11 should map to -2", -2, mapper.toKey(-11f));
		assertEquals("100 should map to 10", 10, mapper.toKey(100f));
		NumberMapperResults<Float> results = new NumberMapperResults<Float>(
				mapper, false);
		for (float i = 100; i > -100; i -= 1) {
			results.add(i);
		}
		results.checkResults(10);
	}

	@Test
	public void testVariousFloatMappers() throws Exception {
		runFloatMapperWithIntegerStep(-10, 100, 8, -100, 100);
		runFloatMapperWithIntegerStep(-100, 10, 5, -100, 100);
		runFloatMapperWithIntegerStep(100, 200, 20, 100, 200);
		runFloatMapperWithIntegerStep(100, 200, 50, 100, 500);

		runFloatMapperWithFractionalStep(-10, 100, 8.5f, -100, 100);
		runFloatMapperWithFractionalStep(-100, 10, 5.2567f, -100, 100);
		runFloatMapperWithFractionalStep(100, 200, 18.9999f, 100, 200);
		runFloatMapperWithFractionalStep(100, 200, 45.0001f, 100, 500);
	}

	private void runFloatMapperWithIntegerStep(float min, float max, int step,
			float dmin, float dmax) throws Exception {
		FloatMapper mapper = FloatMapper.withRangeAndStep(min, max, step);
		float origin = (max + min) / 2;
		assertEquals("Origin should be (" + max + "+" + min + ")/2", origin,
				mapper.getOrigin());
		NumberMapperResults<Float> results = new NumberMapperResults<Float>(
				mapper, false);
		for (float i = dmax; i > dmin; i -= 1) {
			results.add(i);
		}
		results.checkResults(step);
	}

	private void runFloatMapperWithFractionalStep(float min, float max,
			float step, float dmin, float dmax) throws Exception {
		FloatMapper mapper = FloatMapper.withRangeAndStep(min, max, step);
		float origin = (max + min) / 2;
		assertEquals("Origin should be (" + max + "+" + min + ")/2", origin,
				mapper.getOrigin());
		NumberMapperResults<Float> results = new NumberMapperResults<Float>(
				mapper, false);
		for (float i = dmax; i > dmin; i -= 1) {
			results.add(i);
		}
		results.checkResults(step);
	}

	@Test
	public void testDefaultCharacterStringMapper() throws Exception {
		String[] testData = new String[] { " ", "0", "9", "0A", "0B", "A ",
				"A", "AA", "AAA", "A0", "A00", "AB", "AC", "A~", "B ", "B0",
				"B9", "B~", "BA", "BB", "BC", "a", "aa", "ab", "abc", "abcd",
				"abcde", "abcdef" };
		CharacterStringMapper mapper = CharacterStringMapper.getDefault();
		assertEquals("AA should map to zero", 0, mapper.toKey("AA"));
		MapperResults<String> results = new MapperResults<String>(mapper, true);
		for (String test : testData) {
			results.add(test);
		}
		results.print(System.out);
		mapper = CharacterStringMapper.withDepth(1);
		assertEquals("AA should map to zero", 0, mapper.toKey("AA"));
		results = new MapperResults<String>(mapper, true);
		for (String test : testData) {
			results.add(test);
		}
		results.print(System.out);
	}

	@Test
	public void testSingleCharacterStringMapper() throws Exception {
		CharacterStringMapper mapper = CharacterStringMapper.withOrigin("A", 1);
		assertEquals("A should map to zero", 0, mapper.toKey("A"));
		boolean verbose = false;
		int origin = (char) 'A';
		for (int i = 32; i < 126; i++) {
			char c = (char) i;
			int key = i - origin;
			if (verbose) {
				System.out.println("Char[" + (int) c + "]:'" + c + "' => "
						+ mapper.toKey(String.valueOf(c)));
			}
			assertEquals("Key should be the same as " + i + " minus " + origin,
					key, mapper.toKey(String.valueOf(c)));
		}
	}

	@Test
	public void testMultiCharacterStringMapper() throws Exception {
		testMultiCharacterStringMapper(' ', ' ', 16, 2, false);
		testMultiCharacterStringMapper(' ', '*', 16, 2, false);
		testMultiCharacterStringMapper('A', 'A', 4, 2, true);
		testMultiCharacterStringMapper('A', 'A', 4, 3, false);
		testMultiCharacterStringMapper('A', 'C', 4, 2, true);
		testMultiCharacterStringMapper('A', 'C', 4, 3, false);
		testMultiCharacterStringMapper('A', 'B', 2, 5, true);
		testMultiCharacterStringMapper('A', 'B', 2, 6, true);
	}

	private void testMultiCharacterStringMapper(char root, char origin,
			int width, int depth, boolean verbose) throws Exception {
		System.out.println("Testing multi[" + depth
				+ "] character string mapper with root '"
				+ String.valueOf(root) + "' and origin '"
				+ String.valueOf(origin) + "'");
		CharacterStringMapper mapper = CharacterStringMapper.withConfig(
				String.valueOf(origin), depth, root, width);
		assertEquals(
				"Origin[" + String.valueOf(origin) + "] should map to zero", 0,
				mapper.toKey(String.valueOf(origin)));
		char[] data = new char[depth];
		multiCharacterStringMapperTest(root, origin, width, mapper, verbose,
				data, 0);
	}

	private void multiCharacterStringMapperTest(char root, char origin,
			int width, CharacterStringMapper mapper, boolean verbose,
			char[] data, int depth) {
		if (depth == data.length) {
			String text = String.valueOf(data);
			int key = 0;
			for (int i = 0; i < depth; i++) {
				key += Math.pow(width, depth - i - 1) * (data[i] - origin);
			}
			if (verbose) {
				System.out.println("\tString[" + (int) data[0] + ":"
						+ (int) data[1] + "]:'" + text + "' => "
						+ mapper.toKey(text) + " => '" + mapper.getBase(key)
						+ "'");
			}
			assertEquals("Key incorrect", key, mapper.toKey(text));
			assertEquals("Base incorrect", text, mapper.getBase(key));
		} else {
			for (data[depth] = root; data[depth] < root + width; data[depth]++) {
				multiCharacterStringMapperTest(root, origin, width, mapper,
						verbose, data, depth + 1);
			}
		}
	}

	@Test
	public void testCharacterStringMappers() throws Exception {
		String amz = "AMZ";
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(5);
		nf.setMaximumIntegerDigits(5);
		nf.setGroupingUsed(false);
		ArrayList<String> names = new ArrayList<String>();
		// First create representative set and choose an origin (prefix) from
		// that
		for (int i = 0; i < 100; i += 3) {
			names.add(amz + nf.format(i));
		}
		CharacterStringMapper mapper = CharacterStringMapper.withSample(names);
		String prefix = mapper.getOrigin();
		assertEquals("Prefix should map to zero", 0, mapper.toKey(prefix));
		MapperResults<String> results = new MapperResults<String>(mapper, true);
		for (String name : names) {
			results.add(name);
		}
		results.print(System.out);
		// Now add to the set, but deviate from expected distribution, and see
		// how index copes
		for (int i = 0; i < 1000; i += 37) {
			String name = amz + nf.format(i) + "B";
			names.add(name);
			results.add(name);
		}
		assertEquals("Prefix should map to zero", 0, mapper.toKey(prefix));
		results.print(System.out);
		for (int i = 0; i < 10; i++) {
			String name = names.get(i);
			int key = mapper.toKey(name);
			assertEquals("Key incorrect", key, mapper.toKey(name));
			assertEquals("Base incorrect",
					name.substring(0, prefix.length()), mapper.getBase(key));
			ArrayList<String> result = results.getResult(key);
			assertTrue(
					"Early samples should be in dense index (" + result.size()
							+ " should be <= 3)", result.size() >= 3);
			key = mapper.toKey(names.get(names.size() - i - 1));
			result = results.getResult(key);
			assertEquals("Late samples should be in light index", 1,
					result.size());
		}
	}

}
