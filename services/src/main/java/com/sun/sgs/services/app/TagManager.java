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
 * A {@code Manager} that provides application-level functionality similar
 * to profile reporting. An application developer may use this interface to
 * report various details that will be associated with the current transaction.
 * The resulting collections of details are reported as a stream to
 * listener objects on the local node.
 * <p>
 * Note that this is still experimental, with several open questions. For
 * instance, would a registration system like the one in the profiling
 * interfaces be useful? You'd get a handle that would allow for typing,
 * or other possible meta-data but that would require extra state and
 * data store access with each report. Samples or aggregations might be
 * possible, but anything detailed will be very hard to provide in a
 * cluster-wide sense.
 * <p>
 * Currently the only value that can be reported is a "tag", which is just
 * some numeric identifier. This allows application code to tag a given
 * transaction with multiple identifiers that capture something specific
 * about the application logic (i.e. game-specific events). A tag may
 * optionally be applied with some un-typed value to capture some state
 * associated with the event. For now, a given tag may only be used once
 * in a given transaction.
 */
public interface TagManager {

    /**
     * Tags the current task with the given identifier.
     *
     * @param tag the tag identifier
     *
     * @return {@code true} if the tag was applied, {@code false} if the
     *         identifier has already been used in this task
     */
    boolean tagTask(long tag);

    /**
     * Tags the current task with the given identifier, associating the
     * given value with this tag. The value may be any arbitrary object,
     * but will later be accessed in a non-transactional state and so
     * should not be a reference into the data store.
     *
     * @param tag the tag identifier
     * @param tagValue opaque value for the tag
     *
     * @return {@code true} if the tag was applied, {@code false} if the
     *         identifier has already been used in this task
     */
    boolean tagTask(long tag, Object tagValue);

}
