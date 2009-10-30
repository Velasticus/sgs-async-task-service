/*
 * Copyright 2009 Sun Microsystems, Inc.
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

import com.sun.sgs.services.app.TagManager;


/** Implementation of {@code TagManager} that delegates to a backing manager. */
public class TagManagerImpl implements TagManager {

    // the backing manager
    private final TagManager backingManager;

    /**
     * Creates an instance of {@code TagManagerImpl}.
     *
     * @param backingManager the backing {@code TagManager}
     */
    public TagManagerImpl(TagManager backingManager) {
        this.backingManager = backingManager;
    }

    /** {@inheritDoc} */
    public boolean tagTask(long tag) {
        return backingManager.tagTask(tag);
    }

    /** {@inheritDoc} */
    public boolean tagTask(long tag, Object tagValue) {
        return backingManager.tagTask(tag, tagValue);
    }

}
