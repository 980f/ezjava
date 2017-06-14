package pers.hal42.lang;

import java.util.Map;

public class SimpleEntry<K, V> implements Map.Entry<K, V> {
  public K key;
  public V value;

  public SimpleEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V value) {
    return this.value = value;
  }
}
