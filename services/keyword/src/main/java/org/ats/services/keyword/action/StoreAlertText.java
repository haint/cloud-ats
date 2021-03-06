/**
 * 
 */
package org.ats.services.keyword.action;

import java.io.IOException;

import org.ats.services.keyword.VariableFactory;
import org.ats.services.keyword.VariableFactory.DataType;

/**
 * @author NamBV2
 *
 * Apr 17, 2015
 */
@SuppressWarnings("serial")
public class StoreAlertText extends AbstractAction{

  /** .*/
  private String variable;
  
  /** .*/
  private VariableFactory factory;
  
  public StoreAlertText(String variable, VariableFactory factory) {
    this.variable = variable;
    this.factory = factory;
  }
  
  public String transform() throws IOException {
    StringBuilder sb = new StringBuilder(factory.getVariable(DataType.STRING, variable));
    sb.append(" = wd.switchTo().alert().getText();\n");
    return sb.toString();
  }

  @Override
  public String getAction() {
    return "storeAlertText";
  }

}
