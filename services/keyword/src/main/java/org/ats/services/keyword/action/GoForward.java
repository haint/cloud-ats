/**
 * 
 */
package org.ats.services.keyword.action;

import java.io.IOException;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Apr 8, 2015
 */
@SuppressWarnings("serial")
public class GoForward extends AbstractAction {

  public String transform() throws IOException {
    return "wd.navigate().forward();\n";
  }

  public String getAction() {
    return "goForward";
  }

}
