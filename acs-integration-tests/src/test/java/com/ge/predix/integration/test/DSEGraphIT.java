package com.ge.predix.integration.test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class DSEGraphIT extends AbstractTestNGSpringContextTests {
    public void testWithTinkerPopGremlinDriver() throws Exception {
        Cluster cluster = null;
        Client client = null;
        try {
            cluster = Cluster.build().addContactPoint("127.0.0.1").create();
            System.out.println("testWithTinkerPopGremlinDriver(): cluster = " + cluster);

            client = cluster.connect();
            System.out.println("testWithTinkerPopGremlinDriver(): client = " + client.toString());

            // 1. Any exceptions executing this command are propagated to the client in the ResultSet.
            // For example, setting graph name to "my-graph" produces the following exception in gremlin server:
            // IllegalArgumentException:
            // gremlin.graph.name must must only consist of letters, numbers, and underscores: my-graph is invalid
            //
            // 2. Keyspace name = graph name
            //
            // 3. Running the test second time produces IllegalArgumentException: Graph mygraph already exists.
            CompletableFuture<List<Result>> results = client.submit("system.graph('mygraph').ifNotExists().create()")
                    .all();
            System.out.println("testWithTinkerPopGremlinDriver(): create graph results = " + results.get().toString());

            Client aliasClient = null;
            try {
                aliasClient = client.alias("mygraph.g");
                results = aliasClient.submit("g").all();
                System.out.println("testWithTinkerPopGremlinDriver(): alias results = " + results.get().toString());
                Assert.assertEquals(results.get().get(0).getString(),
                        "graphtraversalsource[dsegraphimpl[mygraph], standard]");

                results = aliasClient.submit("g.V().count()").all();
                System.out.println("testWithTinkerPopGremlinDriver(): V() count results = " + results.get().toString());
                Assert.assertEquals(0, results.get().get(0).getLong());

                results = aliasClient.submit("g.addV('author').property('name','Julia Child')").all();
                System.out.println(
                        "testWithTinkerPopGremlinDriver(): addV('author') results = " + results.get().toString());

                results = aliasClient
                        .submit("g.addV('book').property('name', 'The French Chef Cookbook').property('year' , 1968)")
                        .all();
                System.out.println(
                        "testWithTinkerPopGremlinDriver(): addV('book') results = " + results.get().toString());

                results = aliasClient.submit("g.V().count()").all();
                System.out.println("testWithTinkerPopGremlinDriver(): V() count results = " + results.get().toString());
                Assert.assertEquals(2, results.get().get(0).getLong());
            } finally {
                // System commands cannot be executed while alias is set.
                // There is no explicit to clear aliases but closing the client
                if (aliasClient != null) {
                    aliasClient.close();
                }
            }

            // Will fails if aliases are not cleared.
            results = client.submit("system.graph('mygraph').drop()").all();
            System.out.println("testWithTinkerPopGremlinDriver(): remove graph results = " + results.get().toString());
        } finally {
            if (client != null) {
                client.close();
            }
            if (cluster != null) {
                cluster.close();
            }
        }
    }

    public void testWithTinkerGraph() throws Exception {
//        Configuration c = new BaseConfiguration();
//        //c.setProperty("blueprints.graph", "com.datastax.driver.dse.graph");
//        c.setProperty("gremlin.graph", "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph");
//        Graph graph = GraphFactory.open(c);
//        System.out.println("testWithTinkerPopGraph(): graph = " + graph.toString());

        Cluster cluster = null;
        GraphTraversalSource g = null;
        try {
            cluster = Cluster.build().addContactPoint("127.0.0.1").create();
            System.out.println("testWithTinkerGraph(): cluster = " + cluster);

            g = EmptyGraph.instance().traversal().withRemote(DriverRemoteConnection.using(cluster, "g"));
            System.out.println("testWithTinkerGraph(): traversal source = " + g.toString());

            g.addV("author").property("name", "Julia Child");
            g.addV("book").property("name", "The French Chef Cookbook").property("year", 1968);
            System.out.println("testWithTinkerGraph(): traversal source = " + g.toString());
            Long count = g.V().count().next();
            Assert.assertEquals(count.longValue(), 2);
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
