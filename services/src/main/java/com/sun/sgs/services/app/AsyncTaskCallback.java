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
 *
 * Sun designates this particular file as subject to the "Classpath"
 * exception as provided by Sun in the LICENSE file that accompanied
 * this code.
 */

package com.sun.sgs.services.app;


/**
 * A callback interface used to be notified when the result of an asynchronous
 * task has completed, or if that task failed. All implementations must
 * implement {@code Serializable} and may optionally implement
 * {@code ManagedObject}.
 */
public interface AsyncTaskCallback<T> {

    /**
     * Notifies the callback listener of the given result. This method will
     * be called within a transaction.
     *
     * @param t the result returned from successfully running the task
     */
    public void notifyResult(T t);

    /**
     * Notifies the callback listener that the task failed to complete. This
     * may happen because of a failure running the task, or because the node
     * where this task was running failed. It is up to the callback
     * implementation to decide how to react to failures.
     *
     * @param t the reson that the task failed, or {@code null} if the failure
     *          was due to the executing node itself failing
     */
    public void notifyFailed(Throwable t);

}
