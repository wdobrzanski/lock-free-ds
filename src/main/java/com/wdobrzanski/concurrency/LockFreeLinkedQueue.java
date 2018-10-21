package com.wdobrzanski.concurrency;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This implementation is based on Michael & Scott <a href="http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf">algorithm</a>,
 * similar to {@link java.util.concurrent.ConcurrentLinkedQueue} (probably a little bit slower, but definitely more straightforward and easier to understand)
 *
 * @param <T> the type of values
 */
public class LockFreeLinkedQueue<T> {

    private static class Node<T> {
        private T value;
        private AtomicReference<Node<T>> next = new AtomicReference<>();

        private Node() {}

        private Node(T value) {
            this.value = value;
        }
    }

    private AtomicReference<Node<T>> head = new AtomicReference<>();
    private AtomicReference<Node<T>> tail = new AtomicReference<>();

    public LockFreeLinkedQueue() {
        final Node<T> node = new Node<>();
        head.set(node);
        tail.set(node);
    }

    public boolean add(T value) {
        Node<T> newNode = new Node<>(value);

        while(true) {
            Node<T> last = tail.get();
            Node<T> lastNext = last.next.get();

            if (lastNext == null) {
                if (last.next.compareAndSet(null, newNode)) {
                    tail.compareAndSet(last, newNode);
                    return true;
                }
            } else {
                tail.compareAndSet(last, lastNext);
            }
        }
    }

    public T poll() {
        while (true) {
            Node<T> first = head.get();
            Node<T> firstNext = first.next.get();
            Node<T> last = tail.get();
            if (first == last) {
                if (firstNext == null) {
                    return null;
                } else {
                    tail.compareAndSet(last, firstNext);
                }
            }
            if (head.compareAndSet(first, firstNext)) {
                return firstNext.value;
            }
        }
    }

    public boolean isEmpty() {
        return head.get().next.get() == null;
    }
}