package com.wdobrzanski.concurrency;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Based on Harris-Michael algorithm
 */
public class LockFreeSet<T> {

    private static class Node<T> {
        private T value;
        private long key;
        private AtomicMarkableReference<Node<T>> next = new AtomicMarkableReference<>(null, false);

        private Node(long key) {
            this.key = key;
        }

        private Node(T value) {
            this.value = value;
            this.key = value.hashCode();
        }

        private Node(long key, AtomicMarkableReference<Node<T>> next) {
            this.key = key;
            this.next = next;
        }
    }

    private static class Window<T> {
        private Node<T> previous;
        private Node<T> next;

        private Window(Node<T> previous, Node<T> next) {
            this.previous = previous;
            this.next = next;
        }
    }

    private Node<T> head = new Node<>(
            Long.MIN_VALUE,
            new AtomicMarkableReference<>(new Node<>(Long.MAX_VALUE), false)
    );


    public boolean add(T value) {
        int key = value.hashCode();
        while(true) {
            Window<T> window = find(head, key);
            Node<T> previous = window.previous;
            Node<T> current = window.next;
            if (current.key == key) {
                return false;
            } else {
                Node<T> node = new Node<>(value);
                node.next = new AtomicMarkableReference<>(current, false);
                if (previous.next.compareAndSet(current, node, false, false)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(T value) {
        int key = value.hashCode();
        while(true) {
            Window<T> window = find(head, key);
            Node<T> previous = window.previous;
            Node<T> current = window.next;
            if (current.key != key) {
                return false;
            } else {
                Node<T> next = current.next.getReference();
                boolean markedForRemoval = current.next.compareAndSet(next, next, false, true);
                if (markedForRemoval) {
                    previous.next.compareAndSet(current, next, false, false);
                    return true;
                }
            }
        }
    }

    private Window<T> find(Node<T> head, int key) {
        Node<T> previous;
        Node<T> current;
        Node<T> next;
        boolean markedForRemoval;

        outer: while (true) {
            previous = head;
            current = previous.next.getReference();
            while(true) {
                next = current.next.getReference();
                markedForRemoval = current.next.isMarked();
                while (markedForRemoval) {
                    boolean casSucceeded = previous.next.compareAndSet(current, next, false, false);
                    if (casSucceeded) {
                        current = next;
                        next = current.next.getReference();
                        markedForRemoval = current.next.isMarked();
                    } else {
                        continue outer;
                    }
                }
                if (current.key >= key) {
                    return new Window<>(previous, current);
                } else {
                    previous = current;
                    current = next;
                }
            }
        }
    }

    /**
     * wait-free
     */
    public boolean contains(T value) {
        int key = value.hashCode();
        boolean markedForRemoval = false;
        Node<T> current = head;
        while (current.key < key) {
            current = current.next.getReference();
            markedForRemoval = current.next.isMarked();
        }
        return current.key == key && !markedForRemoval;
    }

}