package org.motechproject.server.verboice.it;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ektorp.CouchDbConnector;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.motechproject.decisiontree.model.Node;
import org.motechproject.decisiontree.model.TextToSpeechPrompt;
import org.motechproject.decisiontree.model.Transition;
import org.motechproject.decisiontree.model.Tree;
import org.motechproject.decisiontree.repository.AllTrees;
import org.motechproject.testing.utils.SpringIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.HashMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testVerboiceContext.xml"})
public class VerboiceIVRControllerDecisionTreeIT extends SpringIntegrationTest {
    static private Server server;

    @Autowired
    AllTrees allTrees;

    @Autowired
    @Qualifier("treesDatabase")
    private CouchDbConnector connector;


    @BeforeClass
    public static void startServer() throws Exception {


        server = new Server(7080);
        Context context = new Context(server, "/");//new Context(server, "/", Context.SESSIONS);

        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setContextConfigLocation("classpath:testVerboiceContext.xml");

        ServletHolder servletHolder = new ServletHolder(dispatcherServlet);
        context.addServlet(servletHolder, "/*");
        server.setHandler(context);
        server.start();
    }

    @Before
    public void setup() {
        createTree();
    }

    private void createTree() {
        Tree tree = new Tree();
        tree.setName("someTree");
        HashMap<String, Transition> transitions = new HashMap<String, Transition>();
        final Node textToSpeechNode = new Node().addPrompts(new TextToSpeechPrompt().setMessage("Say this"));
        transitions.put("1", new Transition().setDestinationNode(textToSpeechNode));

        tree.setRootNode(new Node().addPrompts(
                new TextToSpeechPrompt().setMessage("Hello Welcome to motech")
                //,new AudioPrompt().setAudioFileUrl("https://tamaproject.in/tama/wav/stream/en/signature_music.wav").setName("audioFile")
        ).setTransitions(transitions));
        allTrees.addOrReplace(tree);
        markForDeletion(tree);
    }


    @Test
    public void shouldTestVerboiceXMLResponse() throws Exception {

        HttpClient client = new DefaultHttpClient();
        final String rootUrl = "http://localhost:7080/verboice/ivr?tree=someTree&trP=Lw&ln=en";
        final String response = client.execute(new HttpGet(rootUrl), new BasicResponseHandler());
        Assert.assertTrue(response.contains("<Gather method=\"POST\" action=\"http://localhost:7080/verboice/ivr?type=verboice&amp;ln=en&amp;tree=someTree&amp;trP=Lw\" numDigits=\"1\"></Gather>"));

        final String transitionUrl = "http://localhost:7080/verboice/ivr?tree=someTree&trP=Lw&ln=en&Digits=1";
        final String response2 = client.execute(new HttpGet(transitionUrl), new BasicResponseHandler());
        Assert.assertTrue(response2.contains("<Say>Say this</Say>"));
    }

    @Test
    public void shouldReturnVerboiceML() throws Exception {
        HttpClient client = new DefaultHttpClient();
        final String vmlUrl = "http://localhost:7080/verboice/ivr?tree=someTree&type=verboice&pId=asd&ln=en&tNm=someTree&trP=Lw==";
        final String response = client.execute(new HttpGet(vmlUrl), new BasicResponseHandler());
        Assert.assertTrue(response.contains("<Response>"));
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stop();
    }

    @Override
    public CouchDbConnector getDBConnector() {
        return connector;
    }

}
