package net.atos;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
@ContextName("my-camel-ctx")
public class MyJettyRoute extends RouteBuilder {

    public static final String jettyEndpointString = "jetty:http://0.0.0.0:18080/proxy";

    @Inject @Uri(jettyEndpointString)
    private Endpoint jettyEndpoint;

    @Override
    public void configure() throws Exception {
      from(jettyEndpoint)
        .to("log:?level=INFO&showAll=true&showStreams=true")
        .convertBodyTo(String.class)
        .transform()
          .jsonpath("$..['ac','name']")
        .to("log:?level=INFO&showAll=true&showStreams=true")
        .to("mock:result");
    }
}
