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

import com.sun.sgs.kernel.ComponentRegistry;

import com.sun.sgs.service.DataService;
import com.sun.sgs.service.Service;
import com.sun.sgs.service.Transaction;
import com.sun.sgs.service.TransactionListener;
import com.sun.sgs.service.TransactionProxy;

import com.sun.sgs.services.app.TagManager;

import java.lang.reflect.Constructor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * {@code Service} implementation of the {@code TagManager} interface that
 * generates a sequential stream of {@code TagReport}s from the transactional
 * tasks that are tagged by application code. These reports are consumed
 * by {@code TagReportListener}s, any number of which may be used on a given
 * node. The {@code REPORT_LISTENERS_PROPERTY} is used to define the
 * colon-separated list of listeners.
 * <p>
 * By default, the service will only generate reports for tasks that commit.
 * This behavior is defined by the {@code REPORT_ON_COMMIT_PROPERTY}. If
 * that property is set to "false" then any tagged task will be reported.
 */
public class TagService implements TagManager, Service {

    private static final String NAME = TagService.class.getName();
    private static final Logger logger = Logger.getLogger(NAME);

    // the system's transaction proxy
    private final TransactionProxy proxy;

    // state bit for the service's current status
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // local state for the current thread, also the transaction listener
    private final ThreadLocal<TagReportImpl> localTagReport =
        new ThreadLocal<TagReportImpl>() {
            protected TagReportImpl initialValue() {
                Transaction txn = proxy.getCurrentTransaction();
                TagReportImpl report =
                    new TagReportImpl(txn.getId(), txn.getCreationTime());
                txn.registerListener(new TagTransactionListener(report));
                return report;
            }
        };

    // the queue of pending reports
    private final BlockingQueue<TagReport> reportQueue;

    // the thread used to consume available reports
    private final Thread consumerThread;

    /** Property key used to define a colon-separated list of listeners. */
    public static final String REPORT_LISTENERS_PROPERTY =
        NAME + ".report.listeners";

    // the collection of listeners
    private Set<TagReportListener> reportListeners;

    /** Property key used to specify if reports only happen on commit. */
    public static final String REPORT_ON_COMMIT_PROPERTY =
        NAME + "report.on.commit";

    /** Default value for the report.on.commit property. */
    public static final String REPORT_ON_COMMIT_DEFAULT = "true";

    // a flag indicating if reports are only made for committing transactions
    private final boolean onlyReportOnCommit;

    /** Create an instance of {@code TagService}. */
    public TagService(Properties p, ComponentRegistry registry,
                      TransactionProxy proxy)
    {
        this.proxy = proxy;

        // see if we should limit reports to commits
        String commitOnlyString = p.getProperty(REPORT_ON_COMMIT_PROPERTY,
                                                REPORT_ON_COMMIT_DEFAULT);
        this.onlyReportOnCommit = Boolean.valueOf(commitOnlyString);

        // try to get the set of listeners
        String listenerList = p.getProperty(REPORT_LISTENERS_PROPERTY);
        if (listenerList != null) {
            long nodeId = proxy.getService(DataService.class).getLocalNodeId();
            reportListeners = new HashSet<TagReportListener>();
            for (String listenerName : listenerList.split(":")) {
                try {
                    Class<?> listenerClass = Class.forName(listenerName);
                    Constructor<?> listenerConstructor =
                        listenerClass.getConstructor(Properties.class,
                                                     Long.TYPE);
                    TagReportListener listener =
                        (TagReportListener) (listenerConstructor.
                                             newInstance(p, nodeId));
                    reportListeners.add(listener);
                    logger.config("Loaded listener: " + listenerName);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not construct listener: " +
                               listenerName, e);
                }
            }
            // since there is at least one valid listener, finish
            // constructing the state for handling reports
            this.reportQueue = new LinkedBlockingQueue<TagReport>();
            this.consumerThread = new Thread(new ReportConsumerRunnable());
            logger.config("Finished creating Service");
        } else {
            // if there are no listeners, then just shutdown
            logger.config("No listeners specified; shutting down");
            isShutdown.set(true);
            this.reportQueue = null;
            this.consumerThread = null;
        }
    }

    /* Implement Service */

    /** {@inheritDoc} */
    public String getName() {
        return NAME;
    }

    /** {@inheritDoc} */
    public void ready() {
        if (! isShutdown.get()) {
            consumerThread.start();
            logger.config("Service is ready");
        }
    }

    /** {@inheritDoc} */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            consumerThread.interrupt();
        }
    }

    /* Implement TagManager */

    /** {@inheritDoc} */
    public boolean tagTask(long tag) {
        return tagTask(tag, null);
    }

    /** {@inheritDoc} */
    public boolean tagTask(long tag, Object tagValue) {
        TagReportImpl report = localTagReport.get();
        if (report.tagMap.containsKey(tag)) {
            return false;
        }
        report.tagMap.put(tag, tagValue);
        return true;
    }

    /* Private utility classes */

    /** Private Runnable used to consume and report available reports. */
    private class ReportConsumerRunnable implements Runnable {
        /** {@inheritDoc} */
        public void run() {
            // process any reports as they become available until the thread
            // is interrupted specifically because the Service shut down
            while (true) {
                try {
                    if (consumerThread.isInterrupted() && isShutdown.get()) {
                        logger.info("Consumer thread is shutting down");
                        break;
                    }
                    TagReport tagReport = reportQueue.take();
                    for (TagReportListener listener : reportListeners) {
                        try {
                            listener.report(tagReport);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "listener failed to " +
                                       "accept report", e);
                        }
                    }
                } catch (InterruptedException ie) {
                    if (isShutdown.get()) {
                        logger.info("Consumer thread is shutting down");
                        break;
                    }
                }
            }

            // TODO: at this point the queue could be drained to make
            // sure that all remaining reports are reported
        }
    }

    /**
     * Private class that tracks the reported tag state associated with a
     * transaction and is also the TransactionListener used to report
     * the collected tag state when the transaction completes.
     */
    private class TagTransactionListener implements TransactionListener {
        private final TagReport report;
        /** Creates an instance of TagTransactionListener. */
        TagTransactionListener(TagReport report) {
            this.report = report;
        }
        /** {@inheritDoc} */
        public void beforeCompletion() { }
        /** {@inheritDoc} */
        public void afterCompletion(boolean committed) {
            if (committed || (! onlyReportOnCommit)) {
                if (! reportQueue.offer(report)) {
                    // this should never happen, since it would require the
                    // system to be over-loaded to the point that it should
                    // be failing elsewhere...though this queue could be
                    // intentionally limited and this could be used as an
                    // indicator of node health
                    logger.warning("Failed to enqueue a report");
                }
            }
            localTagReport.remove();
        }
        /** {@inheritDoc} */
        public String getTypeName() {
            return TagTransactionListener.class.getName();
        }
    }

    /** Private implementation of TagReport. */
    private static class TagReportImpl implements TagReport {
        private final byte [] id;
        private final long startTime;
        final Map<Long,Object> tagMap = new HashMap<Long,Object>();
        /** Creates an instance of TagReportImpl. */
        TagReportImpl(byte [] id, long startTime) {
            this.id = id;
            this.startTime = startTime;
        }
        /** {@inheritDoc} */
        public byte [] getTransactionId() {
            return id;
        }
        /** {@inheritDoc} */
        public long getTransactionStartTime() {
            return startTime;
        }
        /** {@inheritDoc} */
        public Collection<Long> getTags() {
            return Collections.unmodifiableSet(tagMap.keySet());
        }
        /** {@inheritDoc} */
        public Object getTagValue(long tag) {
            return tagMap.get(tag);
        }
    }

}
