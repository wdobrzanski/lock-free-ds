package com.wdobrzanski.concurrency

import java.util.concurrent.CountDownLatch
import java.util.stream.IntStream

class LockFreeSetTest extends ConcurrencyTest {

    def iterations = logicalCores * 2_000
    def allIterationsFinishedLatch = new CountDownLatch(iterations)

    def "add should return true when adding a new element"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        expect:
            set.add(1)
    }

    def "add should return false when adding an already existing element"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        and:
            set.add(1)
        expect:
            !set.add(1)
    }

    def "remove should return false when removing non-existing element"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        expect:
            !set.remove(1)
    }

    def "remove should return true when removing previously added element"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        and:
            set.add(5)
        expect:
            set.remove(5)
    }

    def "contains should return false when checking non-existing element"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        expect:
            !set.contains(5)
    }

    def "contains should return true when checking previously added element"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        and:
            set.add(5)
        expect:
            set.contains(5)
    }

    def "data race should not occur when adding distinct elements in parallel"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        and:
            def task = { Integer value ->
                return {
                    binaryLatch.await()
                    set.add(value)
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
            IntStream.range(0, iterations).forEach{
                i -> assert set.contains(i)
            }
    }

    def "data race should not occur when removing distinct elements in parallel"() {
        given:
            LockFreeSet<Integer> set = new LockFreeSet<>()
        and:
            def task = { Integer value ->
                return {
                    binaryLatch.await()
                    assert set.remove(value)
                    allIterationsFinishedLatch.countDown()
                }
            }
        and:
            IntStream.range(0, iterations).forEach{
                i -> set.add(i)
            }
        expect:
            IntStream.range(0, iterations).forEach{
                i -> executorService.submit(task(i))
            }
            binaryLatch.countDown()
            allIterationsFinishedLatch.await()
    }

}