/*
 * Copyright 2009 Sun Microsystems, Inc.
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

import com.sun.sgs.kernel.ComponentRegistry;

import com.sun.sgs.service.Service;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.WatchdogService;

import com.sun.sgs.services.app.Stopwatch;
import com.sun.sgs.services.app.StopwatchManager;

import java.io.Serializable;

import java.util.Properties;


/**
 * A {@code Service} implementation of {@code StopwatchManager}. Note that
 * the instances of {@code Stopwatch} returned by this class are
 * {@code Serializable} but do not implement {@code ManagedObject}. This
 * is done for a few reasons, but mainly becuse it's assumed that most
 * {@code Stopwatch} instances will be tied to a specific object's
 * state and will be consulted regularly. A side-effect is that the
 * {@code Stopwatch} will not mark itself when updating its state (on calls
 * to {@code start} or {@code split}. 
 */
public class StopwatchService implements StopwatchManager, Service {

    // system components
    private static TransactionProxy proxy;
    private final WatchdogService watchdog;

    // flag to note when the service is shutdown
    private volatile boolean isShutdown = false;

    /**
     * Creates a new instance of {@code StopwatchService}.
     *
     * @param p the application properties
     * @param registry the system component registry
     * @param proxy the system transaction proxy
     */
    public StopwatchService(Properties p, ComponentRegistry registry,
                            TransactionProxy proxy)
    {
        this.proxy = proxy;
        this.watchdog = proxy.getService(WatchdogService.class);
    }

    /* Implement Service */

    /** {@inheritDoc} */
    public String getName() {
        return StopwatchService.class.getName();
    }

    /** {@inheritDoc} */
    public void ready() {  }

    /** {@inheritDoc} */
    public void shutdown() {
        isShutdown = true;
    }

    /* Implement StopwatchService */

    /** {@inheritDoc} */
    public Stopwatch createStopwatch() {
        if (isShutdown) {
            throw new IllegalStateException("StopwatchService is shutdown");
        }
        return new StopwatchImpl();
    }

    /* Private Utilities */

    /** Returns the current application time if the service is still running. */
    private static long currentAppTime() {
        StopwatchService service = proxy.getService(StopwatchService.class);
        if (service.isShutdown) {
            throw new IllegalStateException("StopwatchService is shutdown");
        }
        return service.watchdog.currentAppTimeMillis();
    }

    /** Private implementation of {@code Stopwatch}. */
    private static class StopwatchImpl implements Stopwatch, Serializable {
        private final static long serialVersionUID = 1L;
        private long startInstant = 0;
        private long lastSplitInstant = 0;
        private long lastSplitDuration = 0;

        /** {@inheritDoc} */
        public void start() {
            if (startInstant != 0) {
                throw new IllegalStateException("Already started");
            }
            startInstant = StopwatchService.currentAppTime();
            lastSplitInstant = startInstant;
        }
        /** {@inheritDoc} */
        public void split() {
            if (startInstant == 0) {
                throw new IllegalStateException("Not started");
            }
            long newSplitInstant = StopwatchService.currentAppTime();
            lastSplitDuration = newSplitInstant - lastSplitInstant;
            lastSplitInstant = newSplitInstant;
        }
        /** {@inheritDoc} */
        public long getElapsedTime() {
            if (startInstant == 0) {
                return 0;
            }
            return StopwatchService.currentAppTime() - startInstant;
        }
        /** {@inheritDoc} */
        public long getLastSplit() {
            return lastSplitDuration;
        }
        /** {@inheritDoc} */
        public long getCurrentSplit() {
            if (lastSplitInstant == 0) {
                return 0;
            }
            return StopwatchService.currentAppTime() - lastSplitInstant;
        }
    }

}
