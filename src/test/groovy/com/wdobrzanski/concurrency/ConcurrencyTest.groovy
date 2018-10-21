package com.wdobrzanski.concurrency

import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

// todo: extract similar methods from subclasses
class ConcurrencyTest extends Specification {

    def logicalCores = Runtime.getRuntime().availableProcessors()
    def iterations = logicalCores * 10_000;

    def executorService = Executors.newFixedThreadPool(logicalCores)

    def binaryLatch = new CountDownLatch(1)
    def allIterationsFinishedLatch = new CountDownLatch(iterations)

}
