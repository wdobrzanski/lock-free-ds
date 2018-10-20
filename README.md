# Lock-free data structures
This repo contains non-blocking, [lock-free](https://en.wikipedia.org/wiki/Non-blocking_algorithm#Lock-freedom) implementations of some of the most useful data structures. 


The nice thing about lock-free algorithms is that they provide a very strong guarantee - global progress, although they are not necessarily better/faster than their lock-based counterparts.

Currently, all implementations in this repository are vulnerable to the infamous [ABA problem](https://en.wikipedia.org/wiki/ABA_problem).

# Running
    mvn package 
