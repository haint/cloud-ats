/**
 * 
 */
package org.ats.services.keyword.action;

import java.io.IOException;

/**
 * @author TrinhTV3
 *
 * Email: TrinhTV3@fsoft.com.vn
 */
@SuppressWarnings("serial")
public class VerifyAlertPresent extends AbstractAction {
  
  private boolean negated;

  public VerifyAlertPresent(boolean negated) {
    this.negated = negated;
  }
  
  @Override
  public String transform() throws IOException {
    
    StringBuilder sb = new StringBuilder("if (").append(negated ? "" : "!").append("isAlertPresent(wd)) {\n");
    sb.append("      System.out.println(\"").append(negated ? "!" : "").append("verifyAlertPresent failed\");\n    }\n");
    return sb.toString();
  }

  @Override
  public String getAction() {
    return "verifyAlertPresent";
  }

}
