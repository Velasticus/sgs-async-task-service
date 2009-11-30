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

package com.sun.sgs.services.impl.service;


/**
 * The interface used to listen for {@code TagReport}s as they are generated.
 * As with the interface used to listen to {@code ProfileReport}s, all
 * reports are sequential meaning that implementations do not need to
 * handle multiple reports in parallel. Listeners should do the minimum
 * work required to process any given report so that they do not block
 * other listeners or the progress of the stream of all reports.
 * <p>
 * All implementations of {@code TagReportListener} must have a constructor
 * with two parameters: {@code Properties} and {@code long}. The first
 * parameter provides access to the properties used to start the local node
 * and the second parameter is the local node's unique identifier.
 */
public interface TagReportListener {

    /**
     * Notifies the listener that a tagged task has completed and provides
     * the complete report associated with that task.
     *
     * @param tagReport the report for the tagged task
     */
    void report(TagReport tagReport);

    /**
     * Tells the listener to stop working, most likely because the local node
     * is being shutdown. Implementations should use this as a chance to
     * free up any resources being held like open files or network
     * connections. Note that in cases of node failure, this method may
     * not be called, so implementors should not assume that they will always
     * have a chance to perform this graceful shutdown.
     */
    void shutdown();

}
