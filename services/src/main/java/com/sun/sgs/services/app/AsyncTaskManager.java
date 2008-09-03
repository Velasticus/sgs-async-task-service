/*
 * Copyright 2008 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.services.app;

import com.sun.sgs.app.TaskRejectedException;


/**
 * A utility {@code Manager} that is used to run asynchronous, untimed,
 * non-transactional tasks. Common uses for this include loading large files
 * and talking to web servers or other external components. If a
 * {@code Callable} is provided then the caller will be notified with the
 * result of the task when it completes, or with a reson for failure. Because
 * these tasks are run non-transactionally, the {@code AppContext} and its
 * associated {@code Manager}s are unavailable to the running task. If some
 * transactional work needs to be done, the task may use the provided
 * {@code TransactionRunner}.
 * <p>
 * Note that no attempt is made to load-balance these tasks. The asynchronous
 * task is run locally, and if it or the node fails, no attempt is made to
 * retry the task. If the caller needs to guarentee that the task runs to
 * completion then an {@code AsynchTaskCallback} should be provided, and on
 * failure the task should be re-started.
 */
public interface AsyncTaskManager {

    /**
     * Makes a best effort to run the given {@code AsyncRunnable}
     * asynchronously, returning immediately to the calling code. The
     * {@code AsyncRunnable} will start after the calling transaction
     * commits. If the {@code AsyncRunnable} fails it is not re-tried and
     * the caller is not notified.
     *
     * @param r an {@code AsyncRunnable} to run non-transactionally after
     *          the current transaction completes
     *
     * @throws TaskRejectedException if the task is not accepted to run
     */
    public void startTask(AsyncRunnable r) throws TaskRejectedException;

    /**
     * Makes a best effort to run the given {@code AsyncCallable}
     * asynchronously, returning immediately to the calling code. The
     * {@code AsyncCallable} will start after the calling transaction
     * commits. Upon completion or failure, the {@code AsyncTaskCallback}
     * will be notified with the result in a new transaction.
     * <p>
     * Note that the {@code AsyncTaskCallback} follows the same rules as
     * other parameters that must be {@code Serializable} but optionally
     * implement {@code ManagedObject}. Specifically, if the
     * {@code AsyncTaskCallback} implements {@code ManagedObject} then it's
     * assumed that the application has managed the object and will handle
     * removing it. Otherwise, the {@code Manager} will take care of
     * managing and removing the object when the task has finished.
     *
     * @param T the type that the {@code AsyncCallable} will return and that
     *          will be provided to the {@code AsyncTaskCallback}
     * @param c an {@code AsyncCallable} to run non-transactionally after
     *          the current transaction completes
     * @param callback an {@code AsynchTaskCallback} to notify of the result
     *                 of running the given {@code AsyncCallable}
     *
     * @throws TaskRejectedException if the task is not accepted to run
     */
    public <T> void startTask(AsyncCallable<T> c, AsyncTaskCallback<T> callback)
        throws TaskRejectedException;

}
