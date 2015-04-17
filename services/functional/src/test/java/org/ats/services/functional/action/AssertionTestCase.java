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
public class AssertionTestCase {

  @Test
  public void testAssertBodyText() throws IOException {
    
    Value value = new Value("not body text", true);
    
    AssertBodyText action = new AssertBodyText(value, true);
    Assert.assertEquals(action.transform(), "assertNotEquals(wd.findElement(By.tagName(\"html\")).getText(), not body text);\n");
  
    action = new AssertBodyText(value, false);
    
    Assert.assertEquals(action.transform(), "assertEquals(wd.findElement(By.tagName(\"html\")).getText(), not body text);\n");
  }
  
  @Test 
  public void testAssertCurrentUrl() throws IOException {
    
    Value value = new Value("http://google.com", true);
    
    AssertCurrentUrl action = new AssertCurrentUrl(value, true);
    
    Assert.assertEquals(action.transform(), "assertNotEquals(wd.getCurrentUrl(), http://google.com);\n");
    
    action = new AssertCurrentUrl(value, false);
    Assert.assertEquals(action.transform(), "assertEquals(wd.getCurrentUrl(), http://google.com);\n");
  }
  
  @Test
  public void testAssertElementPresent() throws IOException {
    
    IDLocator locator = new IDLocator(new Value("i am id", false));
    
    AssertElementPresent action = new AssertElementPresent(locator, true);
    
    Assert.assertEquals(action.transform(), "assertFalse((wd.findElements(By.id(\"i am id\")).size() != 0));\n");
    
    action = new AssertElementPresent(locator, false);
    Assert.assertEquals(action.transform(), "assertTrue((wd.findElements(By.id(\"i am id\")).size() != 0));\n");
  }
  
  @Test
  public void testAssertPageSource() throws IOException {
    
    Value value = new Value("page_source", true);
    
    AssertPageSource action = new AssertPageSource(value, true);
    
    Assert.assertEquals(action.transform(), "assertNotEquals(wd.getPageSource(), page_source);\n");
    
    action = new AssertPageSource(value, false);
    
    Assert.assertEquals(action.transform(), "assertEquals(wd.getPageSource(), page_source);\n");
  }
  
  @Test
  public void testAssertText() throws IOException {
    
    IDLocator locator = new IDLocator(new Value("i_am_an_id", false));
    
    AssertText action = new AssertText(locator, new Value("text", true), true);
    
    Assert.assertEquals(action.transform(), "assertNotEquals(wd.findElement(By.id(\"i_am_an_id\")).getText(), text);\n");
    
    action = new AssertText(locator, new Value("text", true), false);
    
    Assert.assertEquals(action.transform(), "assertEquals(wd.findElement(By.id(\"i_am_an_id\")).getText(), text);\n");
  }
  
  @Test
  public void testAssertTextPresent() throws IOException {
    Value value = new Value("text_present", true);
    
    AssertTextPresent action = new AssertTextPresent(value, true);
    
    Assert.assertEquals(action.transform(), "assertFalse(wd.findElement(By.tagName(\"html\")).getText().contains(text_present));\n");
    
    action = new AssertTextPresent(value, false);
    Assert.assertEquals(action.transform(), "assertTrue(wd.findElement(By.tagName(\"html\")).getText().contains(text_present));\n");
  }
  
  @Test
  public void testAssertCookieByName() throws IOException {
    Value name = new Value("test_cookie", false);
    Value value = new Value("cookie", true);
    
    AssertCookieByName assertCookieByName = new AssertCookieByName(name, value, false);
    Assert.assertEquals(assertCookieByName.transform(), 
        "assertEquals(wd.manage().getCookieNamed(\"test_cookie\").getValue(), cookie);\n");
    
    assertCookieByName = new AssertCookieByName(name, value, true);
    Assert.assertEquals(assertCookieByName.transform(), 
        "assertNotEquals(wd.manage().getCookieNamed(\"test_cookie\").getValue(), cookie);\n");
  }
  
  @Test
  public void testAssertCookiePresent() throws IOException {
    Value name = new Value("test_cookie",false);
    
    AssertCookiePresent assertCookiePresent = new AssertCookiePresent(name, true);
    Assert.assertEquals(assertCookiePresent.transform(), 
        "assertFalse((wd.manage().getCookieNamed(\"test_cookie\") != null));\n");
    
    assertCookiePresent = new AssertCookiePresent(name, false);
    Assert.assertEquals(assertCookiePresent.transform(), 
        "assertTrue((wd.manage().getCookieNamed(\"test_cookie\") != null));\n");
  }
  
  @Test
  public void testAssertElementAttribute() throws IOException {
    Value attributeName = new Value("href", false);
    Value value = new Value("link_href", true);
    LinkTextLocator locator = new LinkTextLocator(new Value("i am a link", false));
    
    AssertElementAttribute assertElementAttribute = new AssertElementAttribute(attributeName, value, locator, true);
    Assert.assertEquals(assertElementAttribute.transform(), 
        "assertNotEquals(wd.findElement(By.linkText(\"i am a link\")).getAttribute(\"href\"), link_href);\n");
    
    assertElementAttribute = new AssertElementAttribute(attributeName, value, locator, false);
    Assert.assertEquals(assertElementAttribute.transform(), 
        "assertEquals(wd.findElement(By.linkText(\"i am a link\")).getAttribute(\"href\"), link_href);\n");
  }
  
  @Test
  public void testAssertElementSelected() throws IOException {
    IDLocator locator = new IDLocator(new Value("unchecked_checkbox", false));
    
    AssertElementSelected assertElementSelected = new AssertElementSelected(locator,true);
    Assert.assertEquals(assertElementSelected.transform(),
        "assertFalse((wd.findElement(By.id(\"unchecked_checkbox\")).isSelected()));\n");
    
    assertElementSelected = new AssertElementSelected(locator,false);
    Assert.assertEquals(assertElementSelected.transform(),
        "assertTrue((wd.findElement(By.id(\"unchecked_checkbox\")).isSelected()));\n");
  }
  
  @Test
  public void testAssertElementValue() throws IOException {
    IDLocator locator = new IDLocator(new Value("comments", false));
    Value value = new Value("not w00t", false);
    AssertElementValue assertElementValue = new AssertElementValue(value, locator, true);
    Assert.assertEquals(assertElementValue.transform(), 
        "assertNotEquals(wd.findElement(By.id(\"comments\")).getAttribute(\"value\"), \"not w00t\");\n");
    
    assertElementValue = new AssertElementValue(value, locator, false);
    Assert.assertEquals(assertElementValue.transform(), 
        "assertEquals(wd.findElement(By.id(\"comments\")).getAttribute(\"value\"), \"not w00t\");\n");
  }
  
  @Test
  public void testAssertTitle() throws IOException {
    AssertTitle assertTitle = new AssertTitle(new Value("this is title", false), true);
    Assert.assertEquals(assertTitle.transform(), 
        "assertNotEquals(wd.getTitle(), \"this is title\");\n");
    
    assertTitle = new AssertTitle(new Value("this is title", false), false);
    Assert.assertEquals(assertTitle.transform(), 
        "assertEquals(wd.getTitle(), \"this is title\");\n");
  }
  
  @Test
  public void testAssertElementStyle() throws IOException {
    IDLocator locator = new IDLocator(new Value("i am a id", false));
    Value value = new Value("value is bar", false);
    Value propertyName = new Value("bar", false);
    
    AssertElementStyle assertElementStyle = new AssertElementStyle(propertyName, value, locator, false);
    Assert.assertEquals(assertElementStyle.transform(), 
        "assertEquals(wd.findElement(By.id(\"i am a id\")).getCssValue(\"bar\"), \"value is bar\");\n");
  }
  
  @Test
  public void testAssertAlertPresent() throws IOException {
    AssertAlertPresent alertPresent = new AssertAlertPresent(true);
    Assert.assertEquals(alertPresent.transform(), "assertFalse(isAlertPresent(wd));\n");

    alertPresent = new AssertAlertPresent(false);
    Assert.assertEquals(alertPresent.transform(), "assertTrue(isAlertPresent(wd));\n");
  }
  
  @Test
  public void testAssertAlertText() throws IOException {
    Value text = new Value("this is alert text", false);
    
    AssertAlertText alertText = new AssertAlertText(text, true);
    Assert.assertEquals(alertText.transform(), 
        "assertNotEquals(wd.switchTo().alert().getText(), \"this is alert text\");\n");
    
    alertText = new AssertAlertText(text, false);
    Assert.assertEquals(alertText.transform(), 
        "assertEquals(wd.switchTo().alert().getText(), \"this is alert text\");\n");
  }
  
  @Test
  public void testAssertEval() throws IOException {
    Value script = new Value("test", false);
    Value value = new Value("value", false);
    
    AssertEval action = new AssertEval(script, value, false);
    Assert.assertEquals(action.transform(), "assertEquals(wd.executeScript(\"test\"), \"value\");\n");
  }
}
