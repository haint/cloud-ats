/**
 * 
 */
package org.ats.services.functional.action;

import java.io.IOException;

import org.ats.services.functional.Value;
import org.ats.services.functional.locator.IDLocator;
import org.ats.services.functional.locator.LinkTextLocator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author TrinhTV3
 *
 * Email: TrinhTV3@fsoft.com.vn
 */
public class StoreTestCase {

  @Test
  public void testStoreBodyText() throws IOException {
    StoreBodyText storeBodyText = new StoreBodyText("body_text");
    Assert.assertEquals(storeBodyText.transform(), "String body_text = wd.findElement(By.tagName(\"html\")).getText();\n");
  }
  
  @Test
  public void testStorePageSource() throws IOException {
    StorePageSource storePageSource = new StorePageSource("page_source");
    Assert.assertEquals(storePageSource.transform(), "String page_source = wd.getPageSource();\n");
  }
  
  @Test
  public void testStoreText() throws IOException {
    IDLocator locator = new IDLocator(new Value("i_am_an_id", false));
    StoreText storeText = new StoreText("text", locator);
    Assert.assertEquals(storeText.transform(), "String text = wd.findElement(By.id(\"i_am_an_id\")).getText();\n");
  }
  
  @Test
  public void testStoreTextPresent() throws IOException {
    StoreTextPresent storeTextPresent = new StoreTextPresent("text_is_present", new Value("I am another div",false));
    Assert.assertEquals(storeTextPresent.transform(), "boolean text_is_present = wd.findElement(By.tagName(\"html\")).getText().contains(\"I am another div\");\n");
  }
  
  @Test
  public void testStoreElementPresent() throws IOException {
    IDLocator locator = new IDLocator(new Value("unchecked_checkbox", false));
    StoreElementPresent storeElementPresent = new StoreElementPresent(locator, "element_present");
    Assert.assertEquals(storeElementPresent.transform(), "boolean element_present = (wd.findElements(By.id(\"unchecked_checkbox\")).size() != 0);\n");
  }
  
  @Test
  public void testStoreElementAttribute() throws IOException {
    LinkTextLocator locator = new LinkTextLocator(new Value("i am a link", false));
    
    Value name = new Value("href", false);
    String var = "link_href";
    
    StoreElementAttribute action = new StoreElementAttribute(var, name, locator);
    Assert.assertEquals(action.transform(), "String link_href = wd.findElement(By.linkText(\"i am a link\")).getAttribute(\"href\");\n");
  }
    @Test
    public void testStore() throws IOException {
    String variable = "text_present";
    Value text = new Value("I am another div",false);
    Store store = new Store(text, variable);
    Assert.assertEquals(store.transform(), 
        "String text_present = \"\" + \"I am another div\";\n");
  }
  
  @Test
  public void testStoreCookiePresent() throws IOException {
    String variable = "cookie_is_present";
    Value name = new Value("test_cookie", false);
    StoreCookiePresent storeCookiePresent = new StoreCookiePresent(variable, name);
    Assert.assertEquals(storeCookiePresent.transform(), 
        "boolean cookie_is_present = (wd.manage().getCookieNamed(\"test_cookie\") != null);\n");
  }
  
  @Test
  public void testStoreCurrentUrl() throws IOException {
    String variable = "url";
    StoreCurrentUrl storeCurrentUrl = new StoreCurrentUrl(variable);
    Assert.assertEquals(storeCurrentUrl.transform(), "String url = wd.getCurrentUrl();\n");
  }
  
  @Test
  public void testStoreAlertPresent() throws IOException {
    
    StoreAlertPresent action = new StoreAlertPresent("var test");
    Assert.assertEquals(action.transform(), "boolean var test = isAlertPresent(wd);\n");
  }
  
  @Test
  public void testStoreEval() throws IOException {
    
    Value script = new Value("test", false);
    
    StoreEval action = new StoreEval(script, "value");
    Assert.assertEquals(action.transform(), "String value = wd.executeScript(\"test\");\n");
  }
  
  @Test
  public void testStoreAlertText() throws IOException {
    StoreAlertText storeAlertText = new StoreAlertText("alert_text");
    Assert.assertEquals(storeAlertText.transform(), "String alert_text = wd.switchTo().alert().getText();\n");
  }
  
  @Test
  public void testStoreElementStyle() throws IOException {
    Value propertyName = new Value("bar", false);
    IDLocator locator = new IDLocator(new Value("i am a id", false));
    StoreElementStyle storeElementStyle = new StoreElementStyle(propertyName, "element_style", locator);
    Assert.assertEquals(storeElementStyle.transform(), 
        "String element_style = wd.findElement(By.id(\"i am a id\")).getCssValue(\"bar\");\n");
  }
  
  @Test
  public void testStoreTitle() throws IOException {
    StoreTitle action = new StoreTitle("var");
    Assert.assertEquals(action.transform(), "String var = wd.getTitle();\n");
  }
  
  @Test
  public void testStoreElementValue() throws IOException {
    IDLocator locator = new IDLocator(new Value("i am id", false));
    StoreElementValue action = new StoreElementValue(locator, "var1");
    
    Assert.assertEquals(action.transform(), "String var1 = wd.findElement(By.id(\"i am id\")).getAttribute(\"value\");\n");
  }
  
  @Test
  public void testStoreElementSelected() throws IOException {
    
    IDLocator locator = new IDLocator(new Value("i am id", false));
    StoreElementSelected action = new StoreElementSelected("var2", locator);
    
    Assert.assertEquals(action.transform(), "boolean var2 = (wd.findElement(By.id(\"i am id\")).isSelected());\n");
  }
  
  @Test
  public void testStoreCookieByName() throws IOException {
    Value name = new Value("this is cookie name", false);
    StoreCookieByName storeCookieByName = new StoreCookieByName(name, "cookie_name");
    Assert.assertEquals(storeCookieByName.transform(), 
        "String cookie_name = wd.manage().getCookieNamed(\"this is cookie name\").getValue();\n");
  }
}
