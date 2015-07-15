/**
 * 
 */
package org.ats.services.keyword.action;

import java.io.IOException;

import org.ats.common.MapBuilder;
import org.ats.services.keyword.locator.AbstractLocator;
import org.rythmengine.RythmEngine;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Apr 9, 2015
 */
@SuppressWarnings("serial")
public class SetElementNotSelected extends AbstractAction {

  private AbstractLocator locator;
  
  public SetElementNotSelected(AbstractLocator locator) {
    this.locator = locator;
  }
  
  public String transform() throws IOException {
    String template =
        "if (wd.findElement(@locator).isSelected()) {\n" +
        "      wd.findElement(@locator).click();\n" +
        "    }\n";
    RythmEngine engine = new RythmEngine(new MapBuilder<String, Boolean>("codegen.compact", false).build());
    return engine.render(template, locator.transform());
  }

  public String getAction() {
    return "setElementNotSelected";
  }

}
