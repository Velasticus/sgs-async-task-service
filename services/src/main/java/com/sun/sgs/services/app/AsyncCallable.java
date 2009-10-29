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


/**
 * A {@code Callable}-like interface that provides access to a
 * {@code TransactionRunner} for running transactions.
 *
 * @param <T> the type returned by this callable on completion
 */
public interface AsyncCallable<T> {

    /**
     * Calls this method, usually doing some work that returns a value
     * if successful or fails with an exception. The provided
     * {@code TransactionRunner} may be used during the run of this method
     * to run transactional tasks.
     *
     * @param transactionRunner a {@code TransactionRunner} used to start
     *                          transactions
     *
     * @return the result of calling this method
     *
     * @throws Exception if there is any error calling this method
     */
    public T call(TransactionRunner transactionRunner) throws Exception;

}
