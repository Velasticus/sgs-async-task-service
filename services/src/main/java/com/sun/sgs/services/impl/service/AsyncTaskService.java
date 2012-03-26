/*
 * Copyright 2008-2009 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Services.
 *
 * Project Darkstar Services is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Services is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.services.impl.service;

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.ObjectNotFoundException;
import com.sun.sgs.app.Task;
import com.sun.sgs.app.TaskRejectedException;

import com.sun.sgs.auth.Identity;

import com.sun.sgs.services.app.AsyncCallable;
import com.sun.sgs.services.app.AsyncRunnable;
import com.sun.sgs.services.app.AsyncTaskCallback;
import com.sun.sgs.services.app.AsyncTaskManager;
import com.sun.sgs.services.app.TransactionRunner;

import com.sun.sgs.impl.util.TransactionContext;
import com.sun.sgs.impl.util.TransactionContextFactory;

import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.kernel.KernelRunnable;
import com.sun.sgs.kernel.TaskReservation;
import com.sun.sgs.kernel.TaskScheduler;
import com.sun.sgs.kernel.TransactionScheduler;

import com.sun.sgs.service.DataService;
import com.sun.sgs.service.Node;
import com.sun.sgs.service.RecoveryListener;
import com.sun.sgs.service.Service;
import com.sun.sgs.service.SimpleCompletionHandler;
import com.sun.sgs.service.TaskService;
import com.sun.sgs.service.Transaction;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.WatchdogService;

import java.io.Serializable;

import java.util.HashSet;
import java.util.Properties;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/** Utility Service for running asynchronous tasks and calling back. */
public class AsyncTaskService implements Service, AsyncTaskManager {

    private final TransactionContextFactory<TxnState> ctxFactory;

    private static TransactionProxy transactionProxy;
    private final TaskScheduler taskScheduler;
    private final TransactionScheduler transactionScheduler;

    private final DataService dataService;
    private final TaskService taskService;

    private final TransactionRunner transactionRunner;

    private final Identity appIdentity;

    private static final String CALLBACK_NS_ROOT =
        AsyncTaskService.class.getName() + ".";

    private final String namespace;
    private final AtomicLong idGenerator;
    private final AtomicBoolean isShutdown;

    /** Creates an instance of AsyncTaskService. */
    public AsyncTaskService(Properties p, ComponentRegistry cr,
                            TransactionProxy tp) {
        ctxFactory = new TransactionContextFactoryImpl(tp);

        this.transactionProxy = tp;
        this.taskScheduler = cr.getComponent(TaskScheduler.class);
        this.transactionScheduler = cr.getComponent(TransactionScheduler.class);

        this.dataService = tp.getService(DataService.class);
        this.taskService = tp.getService(TaskService.class);

        this.transactionRunner = new TransactionRunnerImpl();

        this.appIdentity = transactionProxy.getCurrentOwner();

        WatchdogService watchdogService = tp.getService(WatchdogService.class);
        namespace = CALLBACK_NS_ROOT + dataService.getLocalNodeId() + ".";
        watchdogService.addRecoveryListener(new RecoveryListenerImpl());

        idGenerator = new AtomicLong(0);
        isShutdown = new AtomicBoolean(false);
    }

    /** {@inheritDoc} */
    public String getName() {
        return getClass().getName();
    }

    /** {@inheritDoc} */
    public void ready() throws Exception { }

    /** {@inheritDoc} */
    public void shutdown() {
        isShutdown.set(true);
    }

    /** {@inheritDoc} */
    public void startTask(AsyncRunnable r) throws TaskRejectedException {
        if (r == null)
            throw new NullPointerException("Runnable must not be null");
        ctxFactory.joinTransaction().addRunner(new RunnableKernelRunner(r));
    }

    /** {@inheritDoc} */
    public <T> void startTask(AsyncCallable<T> c, AsyncTaskCallback<T> callback)
        throws TaskRejectedException
    {
        if (c == null)
            throw new NullPointerException("Callable must not be null");
        if (callback == null)
            throw new NullPointerException("Callback must not be null");
        String name = namespace + idGenerator.getAndIncrement();
        dataService.
            setServiceBinding(name, new CallbackWrapper<T>(callback));
        ctxFactory.joinTransaction().
            addRunner(new CallableKernelRunner<T>(c, name));
    }

    /** Local implementation of TransactionContext to manage state. */
    private class TxnState extends TransactionContext {
        private final HashSet<TaskReservation> reservations =
            new HashSet<TaskReservation>();
        private final Identity owner = transactionProxy.getCurrentOwner();
        TxnState(Transaction txn) {
            super(txn);
        }
        /** {@inheritDoc} */
        public void commit() {
            for (TaskReservation r : reservations)
                r.use();
        }
        /** {@inheritDoc} */
        public void abort(boolean retryable) {
            for (TaskReservation r : reservations)
                r.cancel();
        }
        /** Adds a runner to the pending set to start on commit. */
        public void addRunner(KernelRunnable r) throws TaskRejectedException {
            if (isShutdown.get())
                throw new IllegalStateException("Service is shutdown");
            reservations.add(taskScheduler.reserveTask(r, owner));
        }
    }

    /** Basic implementation of TransactionContextFactory. */
    private class TransactionContextFactoryImpl
        extends TransactionContextFactory<TxnState>
    {
        /** Creates an instance with the given proxy. */
        TransactionContextFactoryImpl(TransactionProxy proxy) {
            super(proxy, "AsyncTaskService.TransactionContextFactoryImpl");
        }
        /** {@inheritDoc} */
        protected TxnState createContext(Transaction txn) {
            return new TxnState(txn);
        }
    }

    /** Private implementation of TransactionRunner. */
    private class TransactionRunnerImpl implements TransactionRunner {
        public void runTransaction(final Task task) throws Exception {
            KernelRunnable r = new KernelRunnable() {
                    public String getBaseTaskType() {
                        return task.getClass().getName();
                    }
                    public void run() throws Exception {
                        task.run();
                    }
                };
            transactionScheduler.runTask(r, transactionProxy.getCurrentOwner());
        }
    }

    /** Wrapper so that a Serializable can be managed. */
    private static class CallbackWrapper<T>
        implements ManagedObject, Serializable
    {
        private final static long serialVersionUID = 1L;
        private final ManagedReference<AsyncTaskCallback<T>> managedRef;
        private final AsyncTaskCallback<T> ref;
        private final Identity owner;
        CallbackWrapper(AsyncTaskCallback<T> callback) {
            if (callback instanceof ManagedObject) {
                managedRef = AsyncTaskService.transactionProxy.
                    getService(DataService.class).createReference(callback);
                ref = null;
            } else {
                managedRef = null;
                ref = callback;
            }
            this.owner = transactionProxy.getCurrentOwner();
        }
        Identity getOwner() {
            return owner;
        }
        @SuppressWarnings("unchecked")
        void notifyResult(T result) {
            AsyncTaskCallback<T> c = null;
            try {
                c = (ref != null) ? ref : managedRef.get();
            } catch (ObjectNotFoundException onfe) {
                // the app removed the callback object, so we're done
                return;
            }
            c.notifyResult(result);
        }
        @SuppressWarnings("unchecked")
        void notifyFailure(Throwable t) {
            AsyncTaskCallback c = null;
            try {
                c = (ref != null) ? ref : managedRef.get();
            } catch (ObjectNotFoundException onfe) {
                // the app removed the callback object, so we're done
                return;
            }
            c.notifyFailed(t);
        }
    }

    /** Non-persisted wrapper for simple Runnables. */
    private final class RunnableKernelRunner implements KernelRunnable {
        private final AsyncRunnable r;
        RunnableKernelRunner(AsyncRunnable r) {
            this.r = r;
        }
        public String getBaseTaskType() {
            return getClass().getName();
        }
        public void run() {
            r.run(transactionRunner);
        }
    }

    /** Non-persisted wrapper for Callables with associated callbacks. */
    private final class CallableKernelRunner<T> implements KernelRunnable {
        private final AsyncCallable<T> c;
        private final String name;
        CallableKernelRunner(AsyncCallable<T> c, String name) {
            this.c = c;
            this.name = name;
        }
        public String getBaseTaskType() {
            return getClass().getName();
        }
        public void run() {
            KernelRunnable r = null;
            try {
                r = new CallbackKernelRunner<T>(c.call(transactionRunner),
                                                name, true);
            } catch (Throwable throwable) {
                // there was a failure running the task itself
                r  =  new CallbackKernelRunner<Throwable>(throwable, name,
                                                          false);
            }
            try {
                transactionScheduler.
                    scheduleTask(r, transactionProxy.getCurrentOwner());
            } catch (TaskRejectedException tre) {
                handleNotifyFailure(tre);
            }
        }
    }

    /** Private KernelRunnable used to call-back the app. */
    private class CallbackKernelRunner<T> implements KernelRunnable {
        private final T t;
        private final String name;
        private final boolean succeeded;
        private final Identity owner;
        CallbackKernelRunner(T t, String name, boolean succeeded) {
            if ((! succeeded) && (! (t instanceof Throwable)))
                throw new IllegalArgumentException("Notification of failure " +
                                                   "must include a throwable");
            this.t = t;
            this.name = name;
            this.succeeded = succeeded;
            this.owner = transactionProxy.getCurrentOwner();
        }
        CallbackKernelRunner(String name, Identity owner) {
            this.t = null;
            this.name = name;
            this.succeeded = false;
            this.owner = owner;
        }
        Identity getOwner() {
            return owner;
        }
        public String getBaseTaskType() {
            return getClass().getName();
        }
        public void run() throws Exception {
            @SuppressWarnings("unchecked")
            CallbackWrapper<T> wrapper =
                (CallbackWrapper<T>)(dataService.getServiceBinding(name));
            if (succeeded)
                wrapper.notifyResult(t);
            else
                wrapper.notifyFailure((Throwable)t);
            dataService.removeServiceBinding(name);
            dataService.removeObject(wrapper);
        }
    }

    /** Private implementation of RecoveryListener used to handle failures */
    private class RecoveryListenerImpl implements RecoveryListener {
        public void recover(Node node, SimpleCompletionHandler handler) {
            final String nsRoot = CALLBACK_NS_ROOT + "." + node.getId();
            final HashSet<CallbackKernelRunner<Throwable>> taskSet =
                new HashSet<CallbackKernelRunner<Throwable>>();

            try {
                transactionScheduler.runTask(new KernelRunnable() {
                        public String getBaseTaskType() {
                            return "AsyncTaskService.RecoveryIterator";
                        }
                        public void run() throws Exception {
                            taskSet.clear();
                            String name =
                                dataService.nextServiceBoundName(nsRoot);
                            while (name.startsWith(nsRoot)) {
                                String newName = namespace +
                                    idGenerator.getAndIncrement();
                                CallbackWrapper<?> obj =
                                    (CallbackWrapper<?>)
                                    (dataService.getServiceBinding(name));
                                dataService.
                                    setServiceBinding(newName, obj);
                                dataService.removeServiceBinding(name);
                                taskSet.add(new CallbackKernelRunner<Throwable>
                                            (name, obj.getOwner()));
                                name = dataService.nextServiceBoundName(name);
                            }
                        }
                    }, appIdentity);
            } catch (Exception e) {
                handleNotifyFailure(e);
                return;
            }

            for (CallbackKernelRunner<Throwable> r : taskSet) {
                try {
                    transactionScheduler.scheduleTask(r, r.getOwner());
                } catch (Exception e) {
                    handleNotifyFailure(e);
                }
            }

            handler.completed();
        }
    }

    /** Method that is called when notification can't be done. */
    private void handleNotifyFailure(Throwable t) {
        // TODO: This is a really critical failure that happens when we've
        // tried to notify the applicaiton about a failure and couldn't
        // do so...it's unclear what recourse there actually is.
    }

}
