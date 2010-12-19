package org.amanzi.index.mappers;

public interface Mapper<T extends Object> {

	/** Main active method for mapping, convert the mapped type into a index key */
	public int toKey(Object object);

	/** Get the current known minimum of mapped data */
	public T getMin();

	/** Get the current known maximum of mapped data */
	public T getMax();

	/** Get the fixed origin where values in the range [median,step) are mapped to index 0 */
	public T getOrigin();

	/** Get the number of categories defined for the index */
	public int getCategories();

	/** Provide a description of the values represented by the index key */
	public String getRangeText(int key);

}
