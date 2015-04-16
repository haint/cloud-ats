/**
 * 
 */
package org.ats.services.functional.action;

import java.io.IOException;

import org.ats.services.functional.Value;
import org.ats.services.functional.locator.IDLocator;
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
}
