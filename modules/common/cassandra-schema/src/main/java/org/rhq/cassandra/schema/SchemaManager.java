/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import static com.datastax.driver.core.ProtocolOptions.Compression.SNAPPY;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.CassandraNode;

/**
 * @author John Sanda
 */
public class SchemaManager {

    private final Log log = LogFactory.getLog(SchemaManager.class);

    private Session session;

    private String username;

    private String password;

    private List<CassandraNode> nodes = new ArrayList<CassandraNode>();

    public SchemaManager(String username, String password, String... nodes) {
        try {
            this.username = username;
            this.password = password;

            for (String node : nodes) {
                CassandraNode cassandraNode = CassandraNode.parseNode(node);
                this.nodes.add(cassandraNode);
            }

            initCluster();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create session.", e);
        }
    }

    public SchemaManager(String username, String password, List<CassandraNode> nodes) {
        try {
            this.username = username;
            this.password = password;
            this.nodes = nodes;

            initCluster();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create session.", e);
        }
    }

    private void initCluster() throws NoHostAvailableException {
        String[] hostNames = new String[nodes.size()];
        for (int i = 0; i < hostNames.length; ++i) {
            hostNames[i] = nodes.get(i).getHostName();
        }

        SimpleAuthInfoProvider authInfoProvider = new SimpleAuthInfoProvider();
        authInfoProvider.add("username", "cassandra").add("password", "cassandra");

        Cluster cluster = Cluster.builder()
            .addContactPoints(hostNames)
            .withAuthInfoProvider(authInfoProvider)
            .withCompression(SNAPPY)
            .withPort(nodes.get(0).getNativeTransportPort())
            .build();
        session = cluster.connect("system");
    }

    public void createSchema() {
        try {
            session.execute("CREATE USER rhqadmin SUPERUSER");
            session.execute(
                "CREATE KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};");
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void dropSchema() {
        try {
            session.execute("DROP KEYSPACE rhq");
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean schemaExists() {
        try {
            String cql = "SELECT keyspace_name FROM schema_keyspaces WHERE keyspace_name = 'rhq'";
            ResultSet resultSet = session.execute(cql);
            return !resultSet.all().isEmpty();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSchema() {
        try {
            session.execute(
                "CREATE TABLE rhq.raw_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time) " +
                    ") WITH COMPACT STORAGE"
            );
            session.execute(
                "CREATE TABLE rhq.one_hour_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "type int, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time, type) " +
                ") WITH COMPACT STORAGE"
            );
            session.execute(
                "CREATE TABLE rhq.six_hour_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "type int, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time, type) " +
                ") WITH COMPACT STORAGE;"
            );
            session.execute(
                "CREATE TABLE rhq.twenty_four_hour_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "type int, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time, type) " +
                ") WITH COMPACT STORAGE;"
            );
            session.execute(
                "CREATE TABLE rhq.metrics_index (" +
                    "bucket varchar, " +
                    "time timestamp, " +
                    "schedule_id int, " +
                    "null_col boolean, " +
                    "PRIMARY KEY (bucket, time, schedule_id) " +
                ") WITH COMPACT STORAGE;"
            );
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        session.getCluster().shutdown();
    }

}
