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


/**
 * An interface used to measure elapsed time from the point of view of an
 * application. A {@code Stopwatch}, once started, reports how much
 * time has passed as measured by the application clock. For convenience it
 * can also measure splits. There is no explicit way to stop a
 * {@code Stopwatch}, so it should just be discarded when no longer needed.
 * <p>
 * All implementations must also implement {@code Serializable}. They may
 * optionally implement {@code ManagedObject}.
 */
public interface Stopwatch {

    /**
     * Starts the {@code Stopwatch}. Calling this more than once on the same
     * instance will result in an {@code IllegalStateException}.
     */
    void start();

    /**
     * Marks the current time as the new split time.
     */
    void split();

    /**
     * Returns the total application time in milliseconds since this
     * {@code Stopwatch} was started. If this {@code Stopwatch} has not
     * been started then this always returns 0.
     *
     * @return the length in milliseconds that the {@code Stopwatch} has
     *         been running
     */
    long getElapsedTime();

    /**
     * Gets the length of the last full split in milliseconds. This is the
     * application time that has passed between the last two calls to
     * {@code split}. If this {@code Stopwatch} has not been started or has
     * never been split then this always returns 0.
     *
     * @return the length in milliseconds of the last split interval
     */
    long getLastSplit();

    /**
     * Returns the total application time in milliseconds since the last
     * split. If this {@code Stopwatch} has not been started then this
     * always returns 0. If this {@code Stopwatch} has never been split
     * then this returns the total elapsed time.
     *
     * @return the length in milliseconds of the current split interval
     */
    long getCurrentSplit();

}
