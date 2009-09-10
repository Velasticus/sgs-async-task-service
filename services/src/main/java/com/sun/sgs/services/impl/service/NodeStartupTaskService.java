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

package com.sun.sgs.services.impl.service;

import com.sun.sgs.impl.sharedutil.LoggerWrapper;

import com.sun.sgs.kernel.ComponentRegistry;

import com.sun.sgs.service.Service;
import com.sun.sgs.service.TransactionProxy;

import com.sun.sgs.services.app.AsyncRunnable;

import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A simple utility {@code Service} that runs a single task on startup of
 * a node. Because this may be run on any given node at startup, the
 * task is node-local, and so the application will not learn if running
 * the task has failed for any reason.
 * <p>
 * The task to run must implement the {@code AsyncRunnable} interface, and
 * must have a no-arg constructor. The task is run through the
 * {@code AsyncTaskService}, so while there may be a delay before
 * execution, once started the task is not bound by the transaction
 * timeout limit. Because the task implements the {@code AsyncRunnable}
 * interface it will be able to start transactions as needed.
 * <p>
 * To specify the implementation of {@code AsyncRunnable} to use, the
 * {@code com.sun.sgs.services.impl.service.NodeStartupTaskService.task.class}
 * property should be set to the fully qualified name of the class, and that
 * class should be available in the classpath. If no class is specified,
 * then no startup task is run.
 */
public class NodeStartupTaskService implements Service {

    /** The property used to define the startup task class. */
    public static final String RUNNABLE_PROPERTY =
        NodeStartupTaskService.class.getName() + ".task.class";

    // the service's logger
    private static final LoggerWrapper logger =
        new LoggerWrapper(Logger.getLogger(NodeStartupTaskService.
                                           class.getName()));

    // the service used to run the startup task
    private final AsyncTaskService asyncTaskService;

    // the task to run on startup, or null if no task is being run
    private final AsyncRunnable runnable;

    /** Creates an instance of {@code NodeStartupTaskService}. */
    public NodeStartupTaskService(Properties p, ComponentRegistry cr,
                                  TransactionProxy tp) {
        this.asyncTaskService = tp.getService(AsyncTaskService.class);

        String runnableProp = p.getProperty(RUNNABLE_PROPERTY);
        if (runnableProp != null) {
            try {
                Class<?> runnableClass = Class.forName(runnableProp);
                runnable = (AsyncRunnable)(runnableClass.newInstance());
            } catch (Exception e) {
                if (logger.isLoggable(Level.SEVERE))
                    logger.logThrow(Level.SEVERE, e,
                                    "Failed to load startup class");
                throw new IllegalArgumentException("Couldn't load startup " +
                                                   "runnable class: " +
                                                   runnableProp, e);
            }
        } else {
            logger.log(Level.WARNING, "No startup task will be run for " +
                       "this node because no class was specified");
            runnable = null;
        }
    }

    /** {@inheritDoc} */
    public String getName() {
        return getClass().getName();
    }

    /** {@inheritDoc} */
    public void ready() throws Exception {
        if (runnable != null) {
            logger.log(Level.INFO, "running startup task");
            asyncTaskService.startTask(runnable);
        }
    }

    /** {@inheritDoc} */
    public void shutdown() { }

}
