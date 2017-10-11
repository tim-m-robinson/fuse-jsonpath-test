package net.atos;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.http.HttpStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.inject.Inject;
import javax.json.Json;
import java.io.File;
import java.net.URL;

import static io.restassured.RestAssured.*;
import static io.restassured.path.json.JsonPath.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasKey;


@RunWith(Arquillian.class)
public class MyJettyRouteTest {

  @Deployment
  public static Archive<?> deploy() {
    File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
                    .resolve("org.apache.camel:camel-jetty",
                             "org.apache.camel:camel-jsonpath",
                             "org.glassfish:javax.json",
                             "io.rest-assured:rest-assured",
                             "org.mockito:mockito-core")
                    .withTransitivity().asFile();

    Archive war = ShrinkWrap.create(WebArchive.class, "test.war")
                  .addClass(MyJettyRoute.class)
                  .addAsLibraries(libs)
                  .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    System.out.println(war.toString(true));
    return war;
  }

  @Inject
  CamelContext context;

  @Inject
  @Uri(MyJettyRoute.jettyEndpointString)
  private ProducerTemplate producer;

  @Inject
  @Uri("mock:result")
  private MockEndpoint mock;

  private final static String testJson = Json.createObjectBuilder()
                                                .add("ac","ABC")
                                                .add("name","John Smith")
                                                .add("balance","123")
                                                .build().toString();

  @Test
  public void camel_should_return_ok() {
    producer.requestBodyAndHeader(testJson, "Content-Type", "application/json");
    // Debug
    System.out.println ("*** IN: " + testJson);
    System.out.println ("*** OUT: " + mock.getReceivedExchanges().get(0).getIn().getBody());

  }

  @Test
  public void rest_should_return_ok() throws Exception {
    URL url = new URL("http://127.0.0.1:18080/proxy");
    System.out.println(testJson);

    given().
      header("Content-Type", "application/json").
      body(testJson).
    when().
      post(url).
    then().
      statusCode(HttpStatus.SC_OK).
      contentType(ContentType.JSON).
      body("[0].ac", equalTo("ABC")).
      body("[0].name", equalTo("John Smith")).
      body("$", not(hasKey("balance")));
  }
}
