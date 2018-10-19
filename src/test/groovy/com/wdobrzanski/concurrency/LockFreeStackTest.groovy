package com.wdobrzanski.concurrency

import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.stream.IntStream

class LockFreeStackTest extends Specification {

    def logicalCores = Runtime.getRuntime().availableProcessors()
    def iterations = logicalCores * 10_000;

    def executorService = Executors.newFixedThreadPool(logicalCores)

    def binaryLatch = new CountDownLatch(1)
    def allIterationsFinishedLatch = new CountDownLatch(iterations)

    def "new stack should be empty"() {
        expect:
            new LockFreeStack<Integer>().isEmpty()
    }

    def "stack should not be empty after adding a new element"() {
        given:
            LockFreeStack<Integer> stack = new LockFreeStack<>()
        when:
            stack.push(1)
        then:
            !stack.isEmpty()
    }

    def "should push and pop values correctly"() {
        given:
            LockFreeStack<Integer> stack = new LockFreeStack<>()
        expect:
            IntStream.range(1, 10).forEach{i -> stack.push(i)}
            IntStream.range(1, 10).forEach{i -> assert (10-i) == stack.pop()}
    }

    def "peek should throw an exception when there are no elements"() {
        given:
            LockFreeStack<Integer> stack = new LockFreeStack<>()
        when:
            stack.peek()
        then:
            thrown EmptyStackException
    }

    def "peek should return recently added value"() {
        given:
            LockFreeStack<Integer> stack = new LockFreeStack<>()
            stack.push(1)
            stack.push(2)
        when:
            def peekResult = stack.peek()
        then:
            peekResult == 2
    }

    def "race condition should not occur when adding distinct elements in parallel"() {
        given:
            LockFreeStack<Integer> stack = new LockFreeStack<>()
        and:
            def task = { Integer value ->
                return {
                    binaryLatch.await()
                    stack.push(value)
                    allIterationsFinishedLatch.countDown()
                }
            }
        when:
            IntStream.range(0, iterations).forEach{
                i -> executorService.submit(task(i))
            }
            binaryLatch.countDown()
            allIterationsFinishedLatch.await()
        then:
            def distinctPoppedElements = [] as Set<Integer>
            while (!stack.isEmpty()) {
                distinctPoppedElements.add(stack.pop())
            }
            distinctPoppedElements.size() == iterations
    }

    def "race condition should not occur when popping elements in parallel"() {
        given:
            LockFreeStack<Integer> stack = new LockFreeStack<>()
            BlockingQueue<Integer> poppedElements = new ArrayBlockingQueue<>(iterations)
        and:
            def task = { Integer value ->
                return {
                    binaryLatch.await()
                    poppedElements.put(stack.pop())
                    allIterationsFinishedLatch.countDown()
                }
            }
        and:
            IntStream.range(0, iterations).forEach{
                i -> stack.push(i)
            }
        when:
            IntStream.range(0, iterations).forEach{
                i -> executorService.submit(task(i))
            }
            binaryLatch.countDown()
            allIterationsFinishedLatch.await()
        then:
            def distinctPoppedElements = poppedElements.toSet()
            distinctPoppedElements.size() == iterations
    }

}