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
 *
 * Sun designates this particular file as subject to the "Classpath"
 * exception as provided by Sun in the LICENSE file that accompanied
 * this code.
 */

package com.sun.sgs.services.app;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ManagedObject;

import java.io.Serializable;


/**
 * A utility implementation of {@code Stopwatch} that also implements
 * {@code ManagedObject} so that it can be shared between objects. Two extra
 * methods are porovided, {@code stop} and {@code reset} to make it easier
 * to share the state of the {@code Stopwatch}.
 */
public class SharedStopwatch implements Stopwatch, ManagedObject, Serializable {

    private final static long serialVersionUID = 1L;

    private Stopwatch stopwatch;

    private long totalTime = 0;

    /**
     * Creates an instance of {@code SharedStopwatch}.
     */
    public SharedStopwatch() {
        this.stopwatch = AppContext.getManager(StopwatchManager.class).
            createStopwatch();
    }

    /* Implement Stopwatch */

    /**
     * {@inheritDoc}
     * <p>
     * 
     */
    public void start() {
        if (totalTime != 0) {
            restart();
        } else {
            AppContext.getDataManager().markForUpdate(this);
            stopwatch.start();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     */
    public void split() {
        if (totalTime != 0) {
            throw new IllegalStateException("SharedStopwatch is stopped.");
        }
        AppContext.getDataManager().markForUpdate(this);
        stopwatch.split();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     */
    public long getElapsedTime() {
        if (totalTime != 0) {
            return totalTime;
        }
        return stopwatch.getElapsedTime();
    }

    /** {@inheritDoc} */
    public long getLastSplit() {
        return stopwatch.getLastSplit();
    }

    /**
     * {@inheritDoc}
     * <p>
     *
     */
    public long getCurrentSplit() {
        if (totalTime != 0) {
            return 0;
        }
        return stopwatch.getCurrentSplit();
    }

    /* SharedStopwatch utility methods */

    /**
     *
     */
    public void stop() {
        if (totalTime != 0) {
            return;
        }
        AppContext.getDataManager().markForUpdate(this);
        totalTime = stopwatch.getElapsedTime();
    }

    /**
     *
     */
    public void restart() {
        AppContext.getDataManager().markForUpdate(this);
        totalTime = 0;
        stopwatch = AppContext.getManager(StopwatchManager.class).
            createStopwatch();
    }

}
