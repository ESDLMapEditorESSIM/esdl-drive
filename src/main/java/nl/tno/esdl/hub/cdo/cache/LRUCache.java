package nl.tno.esdl.hub.cdo.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Implements a Least recently used cache with a specific size
 * @author werkmane
 *
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 1362168190272073180L;
	private final int size;

	private LRUCache(int size) {
		super(size, 0.75f, true); //accessOrder = true
		this.size = size;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > size;
	}

	/**
	 * Creates a new Least Recently Used cache with a maximum size
	 * @param <K> key type
	 * @param <V> value type
	 * @param size the number of items in the map before they get evicted
	 * @return
	 */
	public static <K, V> LRUCache<K, V> newInstance(int size) {
		return new LRUCache<K, V>(size);
	}

	public static <K, V> Map<K, V> newSynchronizedInstance(int size) {
		LRUCache<K,V> lruCache = new LRUCache<K, V>(size);
		return Collections.synchronizedMap(lruCache);
	}

}
