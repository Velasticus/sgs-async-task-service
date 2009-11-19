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

package com.sun.sgs.services.impl.util;

import com.sun.sgs.auth.Identity;

import com.sun.sgs.kernel.AccessedObject;
import com.sun.sgs.kernel.AccessReporter.AccessType;
import com.sun.sgs.kernel.ComponentRegistry;

import com.sun.sgs.profile.AccessedObjectsDetail;
import com.sun.sgs.profile.AccessedObjectsDetail.ConflictType;
import com.sun.sgs.profile.ProfileListener;
import com.sun.sgs.profile.ProfileParticipantDetail;
import com.sun.sgs.profile.ProfileReport;

import java.beans.PropertyChangeEvent;

import java.math.BigInteger;

import java.net.InetAddress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;


public class SQLProfileListener implements ProfileListener {

    // see comment in SQLTagReportListener about these fields
    private final Connection dbConnection;
    private final PreparedStatement coreStatement;
    private final PreparedStatement participantStatement;
    private final PreparedStatement accessBaseStatement;
    private final PreparedStatement accessObjsStatement;

    // the statement used to insert core data
    private static final String coreStatementString =
        "INSERT INTO core " +
        "(node, txn, task_type, task_owner, success, start_scheduled, " +
        "start_actual, run_time, try_count, ready_count) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?)";

    // the statement used to insert participant data
    private static final String participantStatementString =
        "INSERT INTO participants " +
        "(task, part_name, part_prepared, part_read_only, part_committed, " +
        "part_direct, time_prepare, time_finish) " +
        "VALUES (?,?,?,?,?,?,?,?)";

    // the statement used to insert base object access data
    private static final String accessBaseStatementString =
        "INSERT INTO access_base " +
        "(task, conflict, other_txn)" +
        "VALUES (?,?,?)";

    // the statement used to insert individual object access data
    private static final String accessObjsStatementString =
        "INSERT INTO access_objs " +
        "(task, idx, id, for_read, description, src)" +
        "VALUES (?,?,?,?,?,?)";

    // the queue used to collect pending reports
    private final Queue<ProfileReport> reportQueue;

    /** The property key used to define how many reports are queued up. */
    public static final String COMMIT_SIZE_PROPERTY =
        SQLProfileListener.class.getName() + ".commit.size";

    /** The default report queue size. */
    public static final String DEFAULT_COMMIT_SIZE = "15";

    // the report queue size being used
    private final int commitSize;

    /**
     * Creates an instance of {@code SQLProfileListener}.
     *
     * @param p the application properties
     * @param owner the default task owner
     * @param registry the system registry
     *
     * @throws IllegalStateException if the database can't be configured
     */
    public SQLProfileListener(Properties p, Identity owner,
                              ComponentRegistry registry)
    {
        commitSize =
            Integer.parseInt(p.getProperty(COMMIT_SIZE_PROPERTY,
                                           DEFAULT_COMMIT_SIZE));
        if (commitSize < 1) {
            throw new IllegalStateException("commit size must be positive");
        }
        reportQueue = new ArrayDeque<ProfileReport>(commitSize);

        try {
            SQLProperties sqlProps = new SQLProperties(p);
            dbConnection = sqlProps.getConnection();
        } catch (SQLException sqle) {
            throw new IllegalStateException("couldn't create connection", sqle);
        }

        try {
            dbConnection.setAutoCommit(false);

            coreStatement = dbConnection.
                prepareStatement(coreStatementString,
                                 Statement.RETURN_GENERATED_KEYS);
            participantStatement =
                dbConnection.prepareStatement(participantStatementString);
            accessBaseStatement =
                dbConnection.prepareStatement(accessBaseStatementString);
            accessObjsStatement =
                dbConnection.prepareStatement(accessObjsStatementString);
        } catch (SQLException sqle) {
            shutdown();
            throw new IllegalStateException("couldn't setup database", sqle);
        }
    }

    /* Implement ProfileListener. */

    /** {@inheritDoc} */
    public void propertyChange(PropertyChangeEvent event) {
        // listen for the announcement of the node id
        if (event.getPropertyName().equals("com.sun.sgs.profile.nodeid")) {
            try {
                // set it for all future use as the first param
                short nodeId = ((Long) event.getNewValue()).shortValue();
                coreStatement.setShort(1, nodeId);

                // insert into the nodes table some detail about this node
                String addr = InetAddress.getLocalHost().getHostAddress();
                Statement nodeStmt = dbConnection.createStatement();
                nodeStmt.executeUpdate("INSERT INTO nodes (id, addr) " +
                                       "VALUES (" + nodeId + ",\'" + addr
                                       + "\')");
                dbConnection.commit();
                nodeStmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** {@inheritDoc} */
    public void report(ProfileReport profileReport) {
        try {
            // only proceed if we've hit the commit size
            if (reportQueue.size() < commitSize) {
                return;
            }
            long start = System.currentTimeMillis();
            // start with the core table to get the auto-generated keys that
            // are used for the other inserts
            for (ProfileReport report : reportQueue) {
                insertCoreDetail(coreStatement, report);
            }
            coreStatement.executeBatch();
            ResultSet rs = coreStatement.getGeneratedKeys();
            if (! rs.first()) {
                throw new IllegalStateException("no task keys generated");
            }
            do {
                long key = rs.getLong(1);
                ProfileReport report = reportQueue.remove();

                insertParticipantDetail(participantStatement,
                                        report.getParticipantDetails(), key);

                AccessedObjectsDetail accessDetail =
                    report.getAccessedObjectsDetail();
                if (accessDetail != null) {
                    insertAccessBaseDetail(accessBaseStatement,
                                           accessDetail, key);
                    insertAccessObjsDetail(accessObjsStatement,
                                           accessDetail.getAccessedObjects(),
                                           key);
                }
            } while (rs.next());
            if (! reportQueue.isEmpty()) {
                throw new IllegalStateException("not enough task keys");
            }
            dbConnection.commit();
            System.out.println("Core: " + (System.currentTimeMillis() - start));
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } finally {
            reportQueue.add(profileReport);
        }
    }

    /** {@inheritDoc} */
    public void shutdown() {
        try {
            dbConnection.close();
        } catch (SQLException sqle) {}
    }

    /* Private helper methods. */

    /** Turns a transaction identifier into a long. */
    private static long txnToLong(byte [] txn) {
        return (new BigInteger(1, txn)).longValue();
    }

    /** Inserts all core task detail. */
    private static void insertCoreDetail(PreparedStatement stmt,
                                         ProfileReport report)
        throws SQLException
    {
        if (report.wasTaskTransactional()) {
            stmt.setLong(2, txnToLong(report.getTransactionId()));
        } else {
            stmt.setNull(2, Types.BIGINT);
        }
        stmt.setString(3, report.getTask().getBaseTaskType());
        stmt.setString(4, report.getTaskOwner().getName());
        stmt.setBoolean(5, report.wasTaskSuccessful());
        stmt.setLong(6, report.getScheduledStartTime());
        stmt.setLong(7, report.getActualStartTime());
        stmt.setShort(8, (short) report.getRunningTime());
        stmt.setShort(9, (short) report.getRetryCount());
        stmt.setShort(10, (short) report.getReadyCount());
        stmt.addBatch();
    }

    /** Inserts all participant detail. */
    private static void insertParticipantDetail(PreparedStatement stmt,
                                                Set<ProfileParticipantDetail>
                                                details,
                                                long taskKey)
        throws SQLException
    {
        stmt.setLong(1, taskKey);
        for (ProfileParticipantDetail detail : details) {
            stmt.setString(2, detail.getParticipantName());
            stmt.setBoolean(3, detail.wasPrepared());
            stmt.setBoolean(4, detail.wasReadOnly());
            stmt.setBoolean(5, detail.wasCommitted());
            stmt.setBoolean(6, detail.wasCommittedDirectly());
            stmt.setShort(7, (short) detail.getPrepareTime());
            stmt.setShort(8,(short) (detail.wasCommitted() ?
                                     detail.getCommitTime() :
                                     detail.getAbortTime()));
            stmt.executeUpdate();
        }
    }

    /** Inserts base detail about all object accesses. */
    private static void insertAccessBaseDetail(PreparedStatement stmt,
                                               AccessedObjectsDetail detail,
                                               long taskKey)
        throws SQLException
    {
        stmt.setLong(1, taskKey);
        stmt.setShort(2, (short) detail.getConflictType().ordinal());
        byte [] txn = detail.getConflictingId();
        if (txn != null) {
            stmt.setLong(3, txnToLong(txn));
        } else {
            stmt.setNull(3, Types.BIGINT);
        }
        stmt.executeUpdate();
    }

    /** Inserts detail about each object access. */
    private static void insertAccessObjsDetail(PreparedStatement stmt,
                                               List<AccessedObject> accesses,
                                               long taskKey)
        throws SQLException
    {
        short count = 1;
        stmt.setLong(1, taskKey);
        for (AccessedObject obj : accesses) {
            stmt.setShort(2, count++);
            stmt.setString(3, obj.getObjectId().toString());
            stmt.setBoolean(4, obj.getAccessType() == AccessType.READ);
            Object desc = obj.getDescription();
            if (desc != null) {
                stmt.setString(5, desc.getClass().toString());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            stmt.setString(6, obj.getSource());
            stmt.executeUpdate();
        }
    }

}
