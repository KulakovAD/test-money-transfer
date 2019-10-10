Task: Design and implement a RESTful API for money transfers between accounts.

Technologies used: Jetty + RESTEasy + Guice. JUnit for testing.

Main class - email.kulakov.test.moneytransfer.MoneyTransferApplication
Default port - 8080, you can setup it in MoneyTransferApplication.PORT

I have implemented 2 Datastores, both with multithreading support:
1) ConcurrentDatastore - based on atomic CAS operations, non-blocking at all. Side effects: Non-transactional.
2) BlockingDatastore - based on synchronized on accounts. Transactional.
By default I use ConcurrentDatastore, you can setup it in MoneyTransferApplication.DATASTORE_CLASS.

All Datastores are tested in AccountResourceTest.
I have written only functional tests(In ideal case we should separate API tests and Datastore tests, but it depend on project guidlines).