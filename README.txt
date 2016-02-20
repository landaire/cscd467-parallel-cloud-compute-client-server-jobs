Description: Homework45 for CSCD 467 (parallel cloud computing)

The following instructions assume you are in the `src/` directory

To compile:

```
javac *.java
```

To run the server:

```
java CapitalizeServer
```

To run the client tester:

```
java ParallelClient
```

NOTES:

- The clients each have a random delay before they try connecting to the server
  for testing purposes
- The workers each have a random delay before they take a job off the queue
  to simulate execution time
