
import java.util.*;

/**
 * Thread-safe queue implementation
 * @author Lander Brandt
 * @param <E>
 */
public class SharedQueue<E>  implements Queue<E> {
    // size lock is required so that TOCTOU does not occur
    private final Object sizeLock = new Object();

    private final Node head = new Node();
    private Node tail = head;

    private int size = 0;
    // capacity of -1 indicates no capacity limit
    private int capacity = -1;

    SharedQueue() {
        this(-1);
    }

    SharedQueue(int capacity) {
        this.capacity = capacity;
    }

    private class Node {
        private Node next;
        private E data;
        private boolean live = true;
        private final Object lock = new Object();

        synchronized void setNext(Node node) {
            this.next = node;
        }

        synchronized Node getNext() {
            return this.next;
        }
    }

    /**
     * Adds an item to the end of the queue
     * @param element element to add
     * @return boolean indicating whether or not the add was successful
     */
    public boolean add(E element) {
        synchronized (sizeLock) {
            if (size == capacity && capacity != -1) {
                return false;
            }
        }

        Node newNode = new Node();
        newNode.data = element;

        synchronized (tail.lock) {
            if (!tail.live) {
                synchronized (head.lock) {
                    tail = newNode;
                    head.setNext(newNode);
                }
            } else {
                tail.setNext(newNode);
                tail = newNode;
            }
        }


        synchronized (sizeLock) {
            size++;
        }

        // Only notify one thread
        synchronized (this) {
            this.notify();
        }

        return true;
    }

    /**
     * Removes an item from the front of the queue
     * @return E item at the front of the queue
     */
    public E remove() {
        E data;

        synchronized (head.lock) {
            if (head.getNext() == null) {
                return null;
            }

            Node front = head.getNext();
            synchronized (front.lock) {
                front.live = false;
                data = front.data;
                head.setNext(front.getNext());
            }
        }

        synchronized (sizeLock) {
            size--;
        }

        synchronized (this) {
            notify();
        }

        return data;
    }

    /**
     * The calling thread will wait until an item has been removed from the queue
     */
    public synchronized void waitForRemove() {
        int s = size();

        while (s == size()) {
            try {
                wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    /**
     * Blocking removeFirst()
     * @return
     * @throws InterruptedException
     */
    public synchronized E take() throws InterruptedException {
        E data = null;

        while (data == null) {
            while (size() == 0) {
                this.wait();
            }

            data = remove();
        }

        return data;
    }


    public int size() {
        synchronized (sizeLock) {
            return this.size;
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /** UNUSED METHODS **/

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E element() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }


    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}
