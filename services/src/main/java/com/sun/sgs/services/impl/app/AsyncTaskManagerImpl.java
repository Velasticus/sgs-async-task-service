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

package com.sun.sgs.services.impl.app;

import com.sun.sgs.services.app.AsyncCallable;
import com.sun.sgs.services.app.AsyncRunnable;
import com.sun.sgs.services.app.AsyncTaskCallback;
import com.sun.sgs.services.app.AsyncTaskManager;


/**
 * A simple implementation of {@code AsyncTaskManager}.
 */
public class AsyncTaskManagerImpl implements AsyncTaskManager {

    // the service instance
    private final AsyncTaskManager backingManager;

    /**
     * Creates an instance of {@code AsyncTaskManagerImpl}.
     *
     * @param backingManager the backing {@code AsyncTaskManager}
     */
    public AsyncTaskManagerImpl(AsyncTaskManager backingManager) {
        this.backingManager = backingManager;
    }

    /** {@inheritDoc} */
    public void startTask(AsyncRunnable r) {
        backingManager.startTask(r);
    }

    /** {@inheritDoc} */
    public <T> void startTask(AsyncCallable<T> c,
                              AsyncTaskCallback<T> callback) {
        backingManager.startTask(c, callback);
    }

}
