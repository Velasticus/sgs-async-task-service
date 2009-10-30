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

package com.sun.sgs.services.impl.service;

import java.util.Collection;


/**
 * Details about a single task that was tagged through the {@code TaskManager}.
 */
public interface TagReport {

    /**
     * Returns the identifier for the transaction. Any tagged task is run in
     * the context of a transaction, and all transaction identifiers are
     * unique on a given node.
     *
     * @return the task's transaction's identifier
     */
    byte [] getTransactionId();

    /**
     * Returns the time at which the transaction started running. This is an
     * absolute value, as observed by the local node, measured in milliseconds
     * since January 1, 1970.
     *
     * @return the task's starting time
     */
    long getTransactionStartTime();

    /**
     * Returns the tags that were applied to the task.
     *
     * @return a {@code Collection} of reported tag identifiers
     */
    Collection<Long> getTags();

    /**
     * Returns the value associated with the given tag. This may return
     * {@code null} if no value was associated with the tag or if this
     * tag identifier was not applied to the task.
     *
     * @param tag identifier of a tag
     *
     * @return the value associated with the tag, or {@code null} if no value
     *         was associated with the tag or the tag was never applied
     */
    Object getTagValue(long tag);

}
