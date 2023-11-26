package my.cute.bot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ForwardingMap;

/**
 * a concurrentmap that doesn't allow re-mapping of keys via put() - ie, once
 * a key is associated with a value in the map, no other value can be 
 * associated with that key by calling put() (calling put with a key that
 * already exists has no effect)
 * 
 * <p>note this class still allows the use of remove() which makes it
 * probably really not useful in general since you could just remove() an 
 * entry and then put() a different value for that key but im not building 
 * an api here
 */
public final class ConcurrentFinalEntryMap<K, V> extends ForwardingMap<K, V> {
	
	private final ConcurrentMap<K, V> map;

	public ConcurrentFinalEntryMap(int capacity) {
		this.map = new ConcurrentHashMap<K, V>(capacity);
	}

	public ConcurrentFinalEntryMap(int capacity, float loadFactor) {
		this.map = new ConcurrentHashMap<K, V>(capacity, loadFactor);
	}
	
	@Override
	protected Map<K, V> delegate() {
		return this.map;
	}
	
	/**
	 * associates the given key with the given value if the given key is not already
	 * present in the map. if the given key is already present, no changes are made
	 */
	@Override
	public V put(K key, V value) {
		return this.map.putIfAbsent(key, value);
	}
	
	/*
	 * same as doing foreach over map and calling this.put on each k,v pair
	 * forwardingmap docs warn that standard methods dont guarantee thread
	 * safety
	 * is there any reason why this wouldnt be thread safe? depends on
	 * the given map and its entrySet() method i guess? maybe could 
	 * synchronize on it or something bt this is probably fine given how
	 * we're using this
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		this.standardPutAll(map);
	}

}
