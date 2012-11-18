package org.gaewebpubsub.services.appengine;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.appengine.api.datastore.Transaction;

import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

/**
 * This is a helper class for running Datastore operations in a transaction, providing automatic support for opening
 * and cleaning up a transaction, as well as retries. This class is normally used by creating an anonymous class that
 * overrides ONE of the txnBlock methods. For example:
 *
 * <pre>
 *     return new TransactionRunner&lt;Long&gt;(datastoreService) {
 *         protected Long txnBlock() {
 *             //access methods on the datastore member var
 *             Entity someEntity = datastore.get(someKey);
 *             return (Long) someEntity.getProperty("longProperty");
 *         }
 *     }.run();
 * </pre>
 *
 * @param <T> The return type of the object your transaction will return from the run() method.
 */
public abstract class TransactionRunner<T> {
    private static final Logger logger = Logger.getLogger(TransactionRunner.class.getName());

    protected DatastoreService datastore;

    private int retryCount;

    protected TransactionRunner(DatastoreService datastoreService) {
        this (datastoreService, 3);
    }

    public TransactionRunner(DatastoreService datastoreService, int retryCount) {
        this.datastore = datastoreService;
        this.retryCount = retryCount;
    }

    /**
     * You call this method to execute your block within a transaction.
     *
     * @return the return value from the txnBlock method.
     */
    public T run() {
        int currentIteration = 0;
        long waitTimeIfTimeout = 5;
        while (true) {
            Transaction txn = datastore.beginTransaction();
            try {
                return txnBlock(txn);
            } catch (ConcurrentModificationException cme) {
                if (currentIteration == retryCount) {
                    logger.warning("Failed with ConcurrentModificationException after " + retryCount + " retries: " + cme.getMessage());
                    throw cme;
                }
                logger.warning("ConcurrentModificationException caught on iteration " + currentIteration + ", retrying.");
            } catch (DatastoreTimeoutException dte) {
                if (currentIteration == retryCount) {
                    logger.warning("Failed with DatastoreTimeoutException after " + retryCount + " retries: " + dte.getMessage());
                    throw dte;
                }
                logger.warning("DatastoreTimeoutException caught on iteration " + currentIteration + ", retrying.");

                //sleep at most 10 secs, but we wait longer on successive failures
                try { Thread.sleep(Math.min(waitTimeIfTimeout, 10000)); } catch (InterruptedException ie) { /* ok */ }
                waitTimeIfTimeout *= waitTimeIfTimeout;
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                }
            }

            //if here, we failed but we're retrying
            currentIteration++;
        }
    }

    /**
     * You should ONLY override this method if you need access to the current Transaction object, and thus you will
     * be responsible for handling your own commits. If you don't need special access to the transaction (that is,
     * you just want the transaction to commit if there are no exceptions), then you should override the other
     * txnBlock method.
     *
     * @param txn The currently executing transaction.
     * @return The return value from your datastore operations.
     */
    protected T txnBlock(Transaction txn) {
        T retVal = txnBlock();
        txn.commit();
        return retVal;
    }

    /**
     * In most cases you will not need special access to the current transaction, and thus you should override this
     * method. If the operations succeed without error then commit will automatically be called when run() is executed.
     *
     * @return  The return value from your datastore operations.
     */
    protected T txnBlock() {
        return null;
    }
}
