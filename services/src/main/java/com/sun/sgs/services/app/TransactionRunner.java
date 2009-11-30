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

import com.sun.sgs.app.Task;


/**
 * An interface used to run transactions. Each transaction has full access
 * to the {@code AppContext} and all associated {@code Manager}s.
 */
public interface TransactionRunner {

    /**
     * Runs the given task synchronously, returning when the task has
     * completed or throwing an exception if the task fails. Normal re-try
     * is handled by this method, so an exception signifies a non-retriable,
     * permanent failure.
     * <p>
     * Note that unlike with the methods on {@code TaskManager}, the
     * {@code Task} instance provided here does not need to implement
     * {@code Serializable} nor {@code ManagedObject}.
     *
     * @param task a {@code Task} that will be run in a transaction
     *
     * @throws Exception if the task fails permanently
     */
    public void runTransaction(Task task) throws Exception;

}
