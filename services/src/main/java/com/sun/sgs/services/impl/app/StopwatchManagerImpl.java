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

package com.sun.sgs.services.impl.app;

import com.sun.sgs.services.app.Stopwatch;
import com.sun.sgs.services.app.StopwatchManager;


/**
 * Implementation of {@code StopwatchManager} that delegates to a backing
 * manager.
 */
public class StopwatchManagerImpl implements StopwatchManager {

    // the backing manager
    private final StopwatchManager backingManager;

    /**
     * Creates an instance of {@code StopwatchManagerImpl}.
     *
     * @param backingManager the backing {@code StopwatchManager}
     */
    public StopwatchManagerImpl(StopwatchManager backingManager) {
        this.backingManager = backingManager;
    }

    /** {@inheritDoc} */
    public Stopwatch createStopwatch() {
        return backingManager.createStopwatch();
    }

}
