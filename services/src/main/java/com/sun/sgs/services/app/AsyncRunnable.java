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
 * A {@code Runnable}-like interface that provides access to a
 * {@code TransactionRunner} for running transactions.
 */
public interface AsyncRunnable {

    /**
     * Runs this method, with no notification about the result. The provided
     * {@code TransactionRunner} may be used during the run of this method
     * to run transactional tasks.
     *
     * @param transactionRunner a {@code TransactionRunner} used to start
     *                          transactions
     */
    public void run(TransactionRunner transactionRunner);

}
