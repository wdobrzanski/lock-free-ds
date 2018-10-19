package com.wdobrzanski.concurrency;

import java.util.EmptyStackException;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {

    private static class Node<T> {
        private T value;
        private Node<T> next;

        private Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    private AtomicReference<Node<T>> top = new AtomicReference<Node<T>>();

    public boolean push(T value) {
        while (true) {
            Node<T> currentValue = top.get();
            Node<T> newValue = new Node<T>(value, top.get());
            if (top.compareAndSet(currentValue, newValue)) {
                return true;
            }
        }
    }

    public T pop() {
        while (true) {
            Node<T> currentTop = top.get();
            if (top.compareAndSet(currentTop, currentTop.next)) {
                return currentTop.value;
            }
        }
    }

    public T peek() {
        Node<T> currentTop = top.get();
        if (currentTop == null) {
            throw new EmptyStackException();
        }
        return currentTop.value;
    }

    public boolean isEmpty() {
        return top.get() == null;
    }

}