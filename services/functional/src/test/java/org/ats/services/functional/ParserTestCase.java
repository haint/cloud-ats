/**
 * 
 */
package org.ats.services.functional;

import java.io.File;
import java.io.IOException;

import org.ats.services.FunctionalModule;
import org.ats.services.functional.action.IAction;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Apr 16, 2015
 */
public class ParserTestCase {

  private ActionFactory actionFactory;
  
  @BeforeMethod
  public void init() {
    Injector injector = Guice.createInjector(new FunctionalModule());
    this.actionFactory = injector.getInstance(ActionFactory.class);
  }
  
  @Test
  public void test() throws JsonProcessingException, IOException {
    ObjectMapper m = new ObjectMapper();
    JsonNode rootNode = m.readTree(new File("src/test/resources/full_example.json"));
    JsonNode stepsNode = rootNode.get("steps");
    for (JsonNode json : stepsNode) {
      IAction action = actionFactory.createAction(json);
      if (json.get("type").asText().startsWith("wait")) {
        Assert.assertNull(action);
      } else {
        Assert.assertNotNull(action);
      }
    }
  }
}
