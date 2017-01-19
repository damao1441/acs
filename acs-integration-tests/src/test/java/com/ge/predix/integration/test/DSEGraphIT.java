package com.ge.predix.integration.test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class DSEGraphIT extends AbstractTestNGSpringContextTests {
    public void testDSEGraph() throws Exception {
        Cluster cluster = Cluster.build().addContactPoint("127.0.0.1").create();
        System.out.println("testDSEGraph(): cluster = " + cluster);

        Client client = cluster.connect();
        System.out.println("testDSEGraph(): client = " + client.toString());

        // 1. Any exceptions executing this command are propagated to the client in the ResultSet.
        // For example, setting graph name to "my-graph" produces the following exception in gremlin server:
        // IllegalArgumentException:
        // gremlin.graph.name must must only consist of letters, numbers, and underscores: my-graph is invalid
        //
        // 2. Keyspace name = graph name
        //
        // 3. Running the test second time produces IllegalArgumentException: Graph mygraph already exists.
        CompletableFuture<List<Result>> results = client.submit("system.graph('mygraph').ifNotExists().create()").all();
        System.out.println("testDSEGraph(): create graph results = " + results.get().toString());

        Client aliasClient = client.alias("mygraph.g");
        results = aliasClient.submit("g").all();
        System.out.println("testDSEGraph(): alias results = " + results.get().toString());
        Assert.assertEquals(results.get().get(0).getString(), "graphtraversalsource[dsegraphimpl[mygraph], standard]");

        results = aliasClient.submit("g.V().count()").all();
        System.out.println("testDSEGraph(): V() count results = " + results.get().toString());
        Assert.assertEquals(0, results.get().get(0).getLong());

        results = aliasClient.submit("g.addV('author').property('name','Julia Child')").all();
        System.out.println("testDSEGraph(): addV('author') results = " + results.get().toString());

        results = aliasClient
                .submit("g.addV('book').property('name', 'The French Chef Cookbook').property('year' , 1968)").all();
        System.out.println("testDSEGraph(): addV('book') results = " + results.get().toString());

        results = aliasClient.submit("g.V().count()").all();
        System.out.println("testDSEGraph(): V() count results = " + results.get().toString());
        Assert.assertEquals(2, results.get().get(0).getLong());

        // System commands cannot be executed while alias is set.
        // There is no explicit to clear aliases but closing the client
        aliasClient.close();

        // Will fails if aliases are not cleared.
        results = client.submit("system.graph('mygraph').drop()").all();
        System.out.println("testDSEGraph(): remove graph results = " + results.get().toString());

        client.close();
        cluster.close();
    }
}
