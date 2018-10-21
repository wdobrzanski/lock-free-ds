package com.wdobrzanski.concurrency

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.stream.IntStream

class LockFreeLinkedQueueTest extends ConcurrencyTest {

    def "new queue should be empty"() {
        expect:
            new LockFreeLinkedQueue<>().isEmpty()
    }

    def "queue should not be empty after adding a new element"() {
        given:
            LockFreeLinkedQueue<Integer> queue = new LockFreeLinkedQueue<>()
        when:
            queue.add(1)
        then:
            !queue.isEmpty()
    }

    def "should add and poll values correctly"() {
        given:
            LockFreeLinkedQueue<Integer> queue = new LockFreeLinkedQueue<>()
        expect:
            IntStream.range(1, 10).forEach{i -> queue.add(i)}
            IntStream.range(1, 10).forEach{i -> assert i == queue.poll()}
    }

    def "data race should not occur when adding distinct elements in parallel"() {
        given:
            LockFreeLinkedQueue<Integer> queue = new LockFreeLinkedQueue<>()
        and:
            def task = { Integer value ->
                return {
                    binaryLatch.await()
                    queue.add(value)
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
            while (!queue.isEmpty()) {
                distinctPoppedElements.add(queue.poll())
            }
            distinctPoppedElements.size() == iterations
    }

    def "data race should not occur when polling elements in parallel"() {
        given:
            LockFreeLinkedQueue<Integer> queue = new LockFreeLinkedQueue<>()
            BlockingQueue<Integer> poppedElements = new ArrayBlockingQueue<>(iterations)
        and:
            def task = { Integer value ->
                return {
                    binaryLatch.await()
                    poppedElements.put(queue.poll())
                    allIterationsFinishedLatch.countDown()
                }
            }
        and:
            IntStream.range(0, iterations).forEach{
                i -> queue.add(i)
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