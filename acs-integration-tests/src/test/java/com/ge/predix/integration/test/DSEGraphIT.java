package com.ge.predix.integration.test;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_LABEL;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class DSEGraphIT extends AbstractTestNGSpringContextTests {
    static final String SYSTEM_CREATE_GRAPH = "system.graph('%s').ifNotExists().create()";
    static final String SYSTEM_DROP_GRAPH = "system.graph('%s').drop()";

    static final String SCHEMA_CREATE_PROPERTY_KEY = "schema.propertyKey('%s').Text().ifNotExists().create()";
    static final String SCHEMA_CREATE_CUSTOM_VERTEX_ID = "schema.vertexLabel('%s').partitionKey('%s')"
            + ".clusteringKey('%s').ifNotExists().create()";
    static final String SCHEMA_DESCRIBE = "schema.describe()";
    static final String SCHEMA_CLEAR = "schema.clear()";

    static final String QUERY_VERTEX_COUNT = "g.V().count()";
    static final String QUERY_ADD_VERTEX = "g.addV('%s').property('%s','%s').property('%s','%s')";
    static final String QUERY_VERTEX = "g.V(['~label':'%s','%s':'%s','%s':'%s'])";

    public void testWithTinkerPopGremlinDriver() throws Exception {
        final String graphName = "mygraph";
        final String zoneIdKey = ZONE_ID_KEY;
        final String resourceIdKey = RESOURCE_ID_KEY;
        final String resourceLabel = RESOURCE_LABEL;
        
        Cluster cluster = null;
        Client client = null;
        try {
            cluster = Cluster.build().addContactPoint("127.0.0.1").create();
            System.out.println("testWithTinkerPopGremlinDriver(): cluster = " + cluster);

            client = cluster.connect();
            System.out.println("testWithTinkerPopGremlinDriver(): client = " + client.toString());

            // Create an empty DSE graph
            // Cassandra keyspace name is equal to graph name
            CompletableFuture<List<Result>> results = client.submit(String.format(SYSTEM_CREATE_GRAPH, graphName))
                    .all();
            System.out.println("testWithTinkerPopGremlinDriver(): create graph results = " + results.get().toString());

            Client aliasClient = null;
            try {
                aliasClient = client.alias(graphName + ".g");
                results = aliasClient.submit("g").all();
                System.out.println("testWithTinkerPopGremlinDriver(): traversal source = " + results.get().toString());
                Assert.assertEquals(results.get().get(0).getString(),
                        "graphtraversalsource[dsegraphimpl[" + graphName +"], standard]");

                // Create graph schema
                // Schema should be created after alias is set
                results = aliasClient.submit(String.format(SCHEMA_CREATE_PROPERTY_KEY, zoneIdKey)).all();
                System.out.println("testWithTinkerPopGremlinDriver(): create zone id property key result = "
                        + results.get().toString());
                results = aliasClient.submit(String.format(SCHEMA_CREATE_PROPERTY_KEY, resourceIdKey)).all();
                System.out.println("testWithTinkerPopGremlinDriver(): create resource id property key result = "
                        + results.get().toString());
                results = aliasClient
                        .submit(String.format(SCHEMA_CREATE_CUSTOM_VERTEX_ID, resourceLabel, zoneIdKey, resourceIdKey))
                        .all();
                System.out.println("testWithTinkerPopGremlinDriver(): create custom vertex id result = "
                        + results.get().toString());

                results = aliasClient.submit(SCHEMA_DESCRIBE).all();
                System.out.println("testWithTinkerPopGremlinDriver(): resulting schema = " + results.get().toString());

                results = aliasClient.submit(QUERY_VERTEX_COUNT).all();
                System.out.println("testWithTinkerPopGremlinDriver(): vertices count = " + results.get().toString());
                Assert.assertEquals(0, results.get().get(0).getLong());

                // Add vertices
                results = aliasClient.submit(String.format(QUERY_ADD_VERTEX, resourceLabel, zoneIdKey, "myzone",
                        resourceIdKey, "resourceOne")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): add vertex " + results.get().toString());
//                Assert.assertEquals(results.get().get(0).getString(), String.format("v[{~label=%s, %s=%s, %s=%s}]",
//                        resourceLabel, zoneIdKey, "myzone", resourceIdKey, "resourceOne"));

                results = aliasClient.submit(String.format(QUERY_ADD_VERTEX, resourceLabel, zoneIdKey, "myzone",
                        resourceIdKey, "resourceTwo")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): add vertex " + results.get().toString());
//                Assert.assertEquals(results.get().get(0).getString(), String.format("v[{~label=%s, %s=%s, %s=%s}]",
//                        resourceLabel, zoneIdKey, "myzone", resourceIdKey, "resourceTwo"));

                results = aliasClient.submit(String.format(QUERY_ADD_VERTEX, resourceLabel, zoneIdKey, "otherzone",
                        resourceIdKey, "resourceOne")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): add vertex " + results.get().toString());
//                Assert.assertEquals(results.get().get(0).getString(), String.format("v[{~label=%s, %s=%s, %s=%s}]",
//                        resourceLabel, zoneIdKey, "otherzone", resourceIdKey, "resourceOne"));

                results = aliasClient.submit(String.format(QUERY_ADD_VERTEX, resourceLabel, zoneIdKey, "otherzone",
                        resourceIdKey, "resourceTwo")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): add vertex " + results.get().toString());
//                Assert.assertEquals(results.get().get(0).getString(), String.format("v[{~label=%s, %s=%s, %s=%s}]",
//                        resourceLabel, zoneIdKey, "otherzone", resourceIdKey, "resourceTwo"));

                results = aliasClient.submit(String.format(QUERY_ADD_VERTEX, resourceLabel, zoneIdKey, "otherzone",
                        resourceIdKey, "resourceThree")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): add vertex " + results.get().toString());
//                Assert.assertEquals(results.get().get(0).getString(), String.format("v[{~label=%s, %s=%s, %s=%s}]",
//                        resourceLabel, zoneIdKey, "otherzone", resourceIdKey, "resourceThree"));

                results = aliasClient.submit(QUERY_VERTEX_COUNT).all();
                System.out.println("testWithTinkerPopGremlinDriver(): vertices count = " + results.get().toString());
                Assert.assertEquals(5, results.get().get(0).getLong());

                results = aliasClient.submit(String.format(QUERY_VERTEX, resourceLabel, zoneIdKey, "otherzone",
                        resourceIdKey, "resourceOne")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): get vertex result = " + results.get().toString());
                Assert.assertEquals(1, results.get().size());
                Vertex vertex = results.get().get(0).getVertex();
                Assert.assertEquals(resourceLabel, vertex.label());
                Assert.assertTrue(vertex.id().toString().contains(zoneIdKey + "=otherzone"));
                Assert.assertTrue(vertex.id().toString().contains(resourceIdKey + "=resourceOne"));
            } finally {
                // System commands cannot be executed while alias is set.
                // There is no explicit to clear aliases but closing the client
                if (aliasClient != null) {
                    results = aliasClient.submit(SCHEMA_CLEAR).all();
                    System.out.println(
                            "testWithTinkerPopGremlinDriver(): clearing schema results = " + results.get().toString());
                    aliasClient.close();
                }

                // Will fails if aliases are not cleared.
                results = client.submit(String.format(SYSTEM_DROP_GRAPH, "mygraph")).all();
                System.out.println("testWithTinkerPopGremlinDriver(): remove graph results = " + results.get().toString());
            }
        } finally {
            if (client != null) {
                client.close();
            }
            if (cluster != null) {
                cluster.close();
            }
        }
    }

    public void testWithTinkerPopGraph() throws Exception {
        Configuration c = new BaseConfiguration();
        c.setProperty("gremlin.graph", "com.datastax.driver.dse.graph");
        //c.setProperty("gremlin.graph", "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph");
        Graph graph = GraphFactory.open(c);
        System.out.println("testWithTinkerPopGraph(): graph = " + graph.toString());

        Cluster cluster = null;
        GraphTraversalSource g = null;
        try {
            cluster = Cluster.build().addContactPoint("127.0.0.1").create();
            System.out.println("testWithTinkerPopGraph(): cluster = " + cluster);

            g = graph.traversal().withRemote(DriverRemoteConnection.using(cluster, "g"));
            System.out.println("testWithTinkerPopGraph(): traversal source = " + g.toString());

//            g.addV("author").property("name", "Julia Child").next();
//            g.addV("book").property("name", "The French Chef Cookbook").property("year", 1968).next();
//            graph.tx().commit();
//
//            System.out.println("testWithTinkerPopGraph(): traversal source = " + g.toString());
//            Long count = g.V().count().next();
//            Assert.assertEquals(count.longValue(), 2);
        } finally {
            if (g != null) {
                g.close();
            }
            if (cluster != null) {
                cluster.close();
            }
        }
    }
}
