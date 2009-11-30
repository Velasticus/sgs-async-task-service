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

package com.sun.sgs.services.impl.util;

import com.sun.sgs.services.impl.service.TagReport;
import com.sun.sgs.services.impl.service.TagReportListener;

import java.math.BigInteger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.util.ArrayDeque;
import java.util.Properties;
import java.util.Queue;


/**
 * An implementation of {@code TagReportListener} that periodically dumps
 * all reported data to an SQL container.
 */
public class SQLTagReportListener implements TagReportListener {

    // the connection and the statement...these should be more dynamic,
    // but as an example it's easier just to keep these
    private final Connection dbConnection;
    private final PreparedStatement statement;

    // the statement used to insert tag details
    private static final String statementString =
        "INSERT INTO tags (node, txn, tag_id, tag_value) " +
        "VALUES (?,?,?,?)";

    // the queue of reports
    private final Queue<TagReport> reportQueue;

    /** The property key used to define how many reports are queued up. */
    public static final String COMMIT_SIZE_PROPERTY =
        SQLTagReportListener.class.getName() + ".commit.size";

    /** The default report queue size. */
    public static final String DEFAULT_COMMIT_SIZE = "10";

    // the report queue size being used
    private final int commitSize;

    /**
     * Create an instance of {@code SQLTagReportListener}.
     *
     * @param p the node properties
     * @param nodeId the node identifier
     *
     * @throws IllegalStateException if the database can't be configured
     */
    public SQLTagReportListener(Properties p, long nodeId) {
        commitSize =
            Integer.parseInt(p.getProperty(COMMIT_SIZE_PROPERTY,
                                           DEFAULT_COMMIT_SIZE));
        if (commitSize < 1) {
            throw new IllegalStateException("commit size must be positive");
        }
        reportQueue = new ArrayDeque<TagReport>(commitSize);

        try {
            SQLProperties sqlProps = new SQLProperties(p);
            dbConnection = sqlProps.getConnection();
        } catch (SQLException sqle) {
            throw new IllegalStateException("couldn't create connection", sqle);
        }

        try {
            dbConnection.setAutoCommit(false);
            statement =
                dbConnection.prepareStatement(statementString);
            statement.setShort(1, (short) nodeId);
        } catch (SQLException sqle) {
            shutdown();
            throw new IllegalStateException("couldn't setup database", sqle);
        }
    }

    /* Implement TagReportListener. */

    /** {@inheritDoc} */
    public void report(TagReport tagReport) {
        try {
            // only proceed if we've hit the commit size
            if (reportQueue.size() < commitSize) {
                return;
            }
            long start = System.currentTimeMillis();
            TagReport report = reportQueue.remove();
            while (report != null) {
                insertTagDetail(statement, report);
                report = reportQueue.poll();
            }
            // commit the collection of updates
            dbConnection.commit();
            System.out.println("Tag: " + (System.currentTimeMillis() - start));
        } catch (SQLException sqle) {
            // TODO: we might want a more formal error processing mechanism
            sqle.printStackTrace();
        } finally {
            // queue up the provided report
            reportQueue.add(tagReport);
        }
    }

    /** {@inheritDoc} */
    public void shutdown() {
        try {
            dbConnection.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    /* Private helper methods. */

    /** Inserts all detail from the given report. */
    private static void insertTagDetail(PreparedStatement stmt,
                                        TagReport report)
        throws SQLException
    {
        long txn = (new BigInteger(1, report.getTransactionId())).longValue();
        stmt.setLong(2, txn);
        for (long tag : report.getTags()) {
            Object tagValue = report.getTagValue(tag);
            stmt.setLong(3, tag);
            if (tagValue == null) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, tagValue.toString());
            }
            stmt.executeUpdate();
        }
    }

}
