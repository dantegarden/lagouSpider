package com.example.demo.anti.proxy;

import org.omg.CosNaming.NamingContextPackage.NotEmpty;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CircleBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> , java.io.Serializable {
    private static final long serialVersionUID = -690393397752209194L;

    static class Node<E> {
        E item;
        Node<E> next;
        Node(E x) { item = x; }
    }

    /**容量*/
    private final int capacity;
    /**队列长度*/
    private final AtomicInteger count = new AtomicInteger();
    /**头*/
    transient Node<E> head;
    /**尾*/
    private transient Node<E> last;
    /**显式锁*/
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public CircleBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public CircleBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }

    private void enqueue(Node<E> node) {
        // assert putLock.isHeldByCurrentThread();
        // assert last.next == null;
        last = last.next = node;
        node.next = head;
    }

    private E dequeue() {
        // assert takeLock.isHeldByCurrentThread();
        // assert head.item == null;
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // help GC

        head = first;
        last.next = head;

        E x = first.item;
        first.item = null;
        return x;
    }
    @Override
    public boolean add(E e) {
        return false;
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E remove() {
        return null;
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
        if (count.get() == 0)
            return null;
        final ReentrantLock takeLock = this.lock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.item;
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        int c = -1;
        final ReentrantLock putLock = this.lock;
        final AtomicInteger count = this.count;
        putLock.lock();
        try {
            while (count.get() == capacity) {
                notFull.await();
            }
            enqueue(node);
            c = count.getAndIncrement();
            if(c + 1 < capacity){
                notFull.signal();
            }
            if(c == 0){
                notEmpty.signal();
            }
        } finally {
            putLock.unlock();
        }

    }

    @Override
    public E take() throws InterruptedException {
        E x = null;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.lock;
        takeLock.lock();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
            if (c == capacity){
                notFull.signal();
            }
        } finally {
            takeLock.unlock();
        }

        return x;
    }


    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public int remainingCapacity() {
        return capacity - count.get();
    }

    void unlink(Node<E> p, Node<E> trail) {
        // assert isFullyLocked();
        // p.next is not changed, to allow iterators that are
        // traversing p to maintain their weak-consistency guarantee.
        p.item = null;
        trail.next = p.next; //把p脱钩
        if (last == p)
            last = trail;
        if (count.getAndDecrement() == capacity)
            notFull.signal();
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        lock.lock();
        try {
            for (Node<E> p, h = head; (p = h.next)!=null && p!= h; h = p) {
                if (o.equals(p.item)) {
                    unlink(p, h);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
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
        lock.lock();
        try {
            for (Node<E> p, h = head; (p = h.next)!=null && p!= h; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            // assert head.item == null && head.next == null;
            if (count.getAndSet(0) == capacity)
                notFull.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return count.get();
    }

    @Override
    public boolean isEmpty() {
        return head == last || count.get() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        lock.lock();
        try {
            for (Node<E> p = head.next; p != null && p != p.next; p = p.next)
                if (o.equals(p.item))
                    return true;
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement; //当前节点的内容

        Itr() {
            lock.lock();
            try {
                lastRet = head;
                current = head.next;
                if (current != null)
                    currentElement = current.item;
            } finally {
                lock.unlock();
            }
        }
        @Override
        public boolean hasNext() {
            return current != current.next;
        }

        private Node<E> nextNode(Node<E> p) {
            for (;;) {
                Node<E> s = p.next;
                if (s == p)
                    return head.next;
                if (s == null || s.item != null)
                    return s;
                p = s;
            }
        }

        @Override
        public E next() {
            E x = null;
            lock.lock();
            try {
                while (current == null || current == current.next){
                    notEmpty.await();
                    current = lastRet.next;
                    currentElement = current.item;
                }
                x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.item;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
            return x;
        }
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
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }

}
