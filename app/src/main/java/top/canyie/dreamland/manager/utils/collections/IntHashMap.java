package top.canyie.dreamland.manager.utils.collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.canyie.dreamland.manager.utils.Preconditions;

import java.util.Map;
import java.util.Objects;

/**
 * @author canyie
 */
@SuppressWarnings("unused")
public class IntHashMap<V> {
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Node<V>[] table;
    private int size;
    private int threshold;
    private float loadFactor;

    public IntHashMap() {
        init(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public IntHashMap(int initialCapacity) {
        Preconditions.checkArgument(initialCapacity >= 0, "invalid initialCapacity");
        if (initialCapacity > MAXIMUM_CAPACITY) initialCapacity = MAXIMUM_CAPACITY;
        init(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public IntHashMap(int initialCapacity, float loadFactor) {
        Preconditions.checkArgument(initialCapacity >= 0, "invalid initialCapacity");
        Preconditions.checkArgument(loadFactor > 0 && !Float.isNaN(loadFactor), "invalid loadFactor");
        if (initialCapacity > MAXIMUM_CAPACITY) initialCapacity = MAXIMUM_CAPACITY;
        init(initialCapacity, loadFactor);
    }

    private void init(int initialCapacity, float loadFactor) {
        this.loadFactor = loadFactor;
        int tableSize = tableSizeFor(initialCapacity);
        // noinspection unchecked
        table = new Node[tableSize];
        threshold = (int) (initialCapacity * loadFactor);
    }

    @Nullable public V get(int key) {
        int index = indexFor(key, table.length);
        for (Node<V> node = table[index]; node != null; node = node.next) {
            if (key == node.key) return node.value;
        }
        return null;
    }

    public void put(int key, @NonNull V value) {
        Preconditions.checkNotNull(value, "value == null");
        int index = indexFor(key, table.length);
        Node<V> lastFoundNode = null;
        for (Node<V> node = table[index]; node != null; lastFoundNode = node, node = node.next) {
            if (key == node.key) {
                node.value = value;
                return;
            }
        }
        if (size == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("IntHashMap: table overflow");
        }
        Node<V> newNode = new Node<>(key, value);
        if (lastFoundNode != null) {
            lastFoundNode.next = newNode;
        } else {
            table[index] = newNode;
        }

        if (++size > threshold) resize();
    }

    public V remove(int key) {
        int index = indexFor(key, table.length);
        for (Node<V> node = table[index], previous = null; node != null; previous = node, node = node.next) {
            if (key == node.key) {
                if (previous != null) {
                    previous.next = node.next;
                } else {
                    table[index] = node.next;
                }
                --size;
                return node.value;
            }
        }
        return null;
    }

    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        size = 0;
    }

    public boolean containsKey(int key) {
        int index = indexFor(key, table.length);
        for (Node<V> node = table[index]; node != null; node = node.next) {
            if (key == node.key) return true;
        }
        return false;
    }

    public boolean containsValue(V value) {
        Node<V>[] table = this.table;
        for (Node<V> node : table) {
            for (; node != null; node = node.next) {
                if (Objects.equals(value, node.value)) return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void resize() {
        int oldTableSize = table.length;
        if (oldTableSize >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        int newTableSize = oldTableSize << 1;
        // noinspection unchecked
        Node<V>[] newTable = new Node[newTableSize];
        for (Node<V> node : table) {
            while (node != null) {
                Node<V> next = node.next;
                int newIndex = indexFor(node.key, newTableSize);
                node.next = newTable[newIndex];
                newTable[newIndex] = node;
                node = next;
            }
        }
        table = newTable;
        threshold = (int) (newTableSize * loadFactor);
    }

    @Override public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (this.size != map.size()) return false;
            for (Node<V> node : table) {
                for (; node != null; node = node.next) {
                    if (!Objects.equals(map.get(node.key), node.value)) return false;
                }
            }
            return true;
        }

        if (obj instanceof IntHashMap) {
            IntHashMap<?> map = (IntHashMap<?>) obj;
            if (this.size != map.size) return false;
            if (hashCode() != map.hashCode()) return false;
            for (Node<V> node : table) {
                for (; node != null; node = node.next) {
                    if (!Objects.equals(map.get(node.key), node.value)) return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override public int hashCode() {
        int h = 1;
        for (Node<V> node : table) {
            for (; node != null; node = node.next) {
                h = 31 * h + node.hashCode();
            }
        }
        return h;
    }

    @NonNull @Override public String toString() {
        boolean notFirst = false;
        StringBuilder sb = new StringBuilder("{");
        for (Node<V> node : table) {
            for (; node != null; node = node.next) {
                if (notFirst) {
                    sb.append(',');
                } else {
                    notFirst = true;
                }
                sb.append(node.key).append('=').append(node.value);
            }
        }
        return sb.append('}').toString();
    }

    public static class Node<V> {
        int key;
        @NonNull V value;
        Node<V> next;

        Node(int key, @NonNull V value) {
            this.key = key;
            this.value = value;
        }

        public int getKey() {
            return key;
        }

        @NonNull public V getValue() {
            return value;
        }

        public V setValue(V newValue) {
            V oldValue = value;
            this.value = newValue;
            return oldValue;
        }

        @Nullable public Node<V> next() {
            return next;
        }

        @Override public boolean equals(@Nullable Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IntHashMap.Node)) return false;
            Node<?> other = (Node<?>) obj;
            return this.key == other.key && Objects.equals(this.value, other.value);
        }

        @Override public int hashCode() {
            return 31 * key + value.hashCode();
        }

        @NonNull @Override public String toString() {
            return key + "=" + value;
        }
    }

    private static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    private static int indexFor(int key, int len) {
        int h = key ^ (key >>> 16);
        return h & (len - 1);
    }
}
