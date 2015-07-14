/**
 * 
 */
package org.ats.services.functional;

import java.io.File;

import org.ats.services.DataDrivenModule;
import org.ats.services.FunctionalServiceModule;
import org.ats.services.OrganizationContext;
import org.ats.services.OrganizationServiceModule;
import org.ats.services.data.DatabaseModule;
import org.ats.services.data.MongoDBService;
import org.ats.services.event.EventModule;
import org.ats.services.event.EventService;
import org.ats.services.functional.Suite.SuiteBuilder;
import org.ats.services.organization.base.AuthenticationService;
import org.ats.services.organization.entity.Space;
import org.ats.services.organization.entity.Tenant;
import org.ats.services.organization.entity.User;
import org.ats.services.organization.entity.fatory.ReferenceFactory;
import org.ats.services.organization.event.AbstractEventTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * May 5, 2015
 */
public class KeywordProjectServiceTestCase extends AbstractEventTestCase {

  private KeywordProjectService funcService;

  private KeywordProjectFactory funcFactory;

  private SuiteService suiteService;
  
  private ReferenceFactory<SuiteReference> suiteRefFactory;
  
  private CaseFactory caseFactory;
  
  private CaseService caseService;
  
  private ReferenceFactory<CaseReference> caseRefFactory;

  private AuthenticationService<User> authService;
  
  private OrganizationContext context;

  private Tenant tenant;

  private Space space;

  private User user;
  
  private Suite suite;

  @BeforeClass
  public void init() throws Exception {
    this.injector = Guice.createInjector(
        new DatabaseModule(), 
        new EventModule(),
        new OrganizationServiceModule(),
        new DataDrivenModule(),
        new FunctionalServiceModule());
    
    this.funcService = injector.getInstance(KeywordProjectService.class);
    this.funcFactory = injector.getInstance(KeywordProjectFactory.class);
    
    this.suiteService = injector.getInstance(SuiteService.class);
    this.suiteRefFactory = injector.getInstance(Key.get(new TypeLiteral<ReferenceFactory<SuiteReference>>(){}));
    
    this.caseService = injector.getInstance(CaseService.class);
    this.caseFactory = injector.getInstance(CaseFactory.class);
    this.caseRefFactory = injector.getInstance(Key.get(new TypeLiteral<ReferenceFactory<CaseReference>>(){}));

    this.authService = injector.getInstance(Key.get(new TypeLiteral<AuthenticationService<User>>(){}));
    this.context = this.injector.getInstance(OrganizationContext.class);

    this.mongoService = injector.getInstance(MongoDBService.class);
    this.mongoService.dropDatabase();

    //start event service
    this.eventService = injector.getInstance(EventService.class);
    this.eventService.setInjector(injector);
    this.eventService.start();

    initService();
  }

  @AfterClass
  public void shutdown() throws Exception {
    this.eventService.stop();
    this.mongoService.dropDatabase();
  } 

  @BeforeMethod
  public void setup() throws Exception {
    this.tenant = tenantFactory.create("Fsoft");
    this.tenantService.create(this.tenant);

    this.space = spaceFactory.create("FSU1.BU11");
    this.space.setTenant(tenantRefFactory.create(this.tenant.getId()));
    this.spaceService.create(this.space);

    this.user = userFactory.create("haint@cloud-ats.net", "Hai", "Nguyen");
    this.user.setTenant(tenantRefFactory.create(this.tenant.getId()));
    this.user.joinSpace(spaceRefFactory.create(this.space.getId()));
    this.user.setPassword("12345");
    this.userService.create(this.user);
    
    ObjectMapper m = new ObjectMapper();
    JsonNode rootNode = m.readTree(new File("src/test/resources/full_example.json"));
    
    SuiteBuilder builder = new SuiteBuilder();
    builder.packageName("org.ats.generated")
      .suiteName("FullExample")
      .driverVar(SuiteBuilder.DEFAULT_DRIVER_VAR)
      .initDriver(SuiteBuilder.DEFAULT_INIT_DRIVER)
      .timeoutSeconds(SuiteBuilder.DEFAULT_TIMEOUT_SECONDS)
      .raw((DBObject)JSON.parse(rootNode.toString()));
    
    JsonNode stepsNode = rootNode.get("steps");
    Case caze = caseFactory.create("test", null);
    for (JsonNode json : stepsNode) {
      caze.addAction(json);
    }
    caseService.create(caze);
    builder.addCases(caseRefFactory.create(caze.getId()));

    this.suite = builder.build();
    suiteService.create(suite);
  }

  @AfterMethod
  public void tearDown() {
    this.authService.logOut();
    this.mongoService.dropDatabase();
  }

  @Test
  public void testCRUD() throws Exception {
    KeywordProject project = null;

    try {
      project = funcFactory.create("Jira Automation");
      Assert.fail();
    } catch (IllegalStateException e) {

    }

    this.authService.logIn("haint@cloud-ats.net", "12345");
    this.spaceService.goTo(spaceRefFactory.create(this.space.getId()));
    
    Assert.assertNotNull(this.context.getUser());
    Assert.assertNotNull(this.context.getTenant());

    try {
      project = funcFactory.create("Jira Automation");
    } catch (IllegalStateException e) {
      e.printStackTrace();
      Assert.fail();
    }
    
    Assert.assertNotNull(project);
    Assert.assertEquals(project.getCreator().getId(), "haint@cloud-ats.net");
    Assert.assertEquals(project.getSpace().getId(), this.space.getId());
    
    funcService.create(project);
    Assert.assertEquals(funcService.count(), 1);
    Assert.assertEquals(funcService.get(project.getId()), project);
    
  }
  
  @Test
  public void testSuite() {
    
    this.authService.logIn("haint@cloud-ats.net", "12345");
    this.spaceService.goTo(spaceRefFactory.create(this.space.getId()));
    
    KeywordProject project = funcFactory.create("Jira Automation");
    funcService.create(project);
    
    SuiteReference suiteRef = suiteRefFactory.create(suite.getId());
    
    project.addSuite(suiteRef);
    funcService.update(project);
    
    project = funcService.get(project.getId());
    Assert.assertEquals(project.getSuites().size(), 1);
    Assert.assertEquals(project.getSuites().get(0).get(), suite);
    
    try {
      project.addSuite(suiteRef);
      Assert.fail();
    } catch (IllegalArgumentException e) {
    }
    
    project.removeSuite(suiteRef);
    funcService.update(project);
    
    try {
      project.addSuite(suiteRef);
      funcService.update(project);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      Assert.fail();
    }
    
    suiteService.delete(suite);
    project = funcService.get(project.getId());
    Assert.assertEquals(project.getSuites().size(), 0);
    
    try {
      project.addSuite(suiteRef);
      Assert.fail();
    } catch (IllegalArgumentException e) {
    }
  }
  
  @Test
  public void testCustomKeyword() throws Exception {
    this.authService.logIn("haint@cloud-ats.net", "12345");
    this.spaceService.goTo(spaceRefFactory.create(this.space.getId()));
    
    KeywordProject project = funcFactory.create("Jira Automation");
    
    ObjectMapper m = new ObjectMapper();
    JsonNode rootNode = m.readTree(new File("src/test/resources/full_example.json"));
    JsonNode stepsNode = rootNode.get("steps");
    
    CustomKeyword keyword = new CustomKeyword("custom_keyword");
    for (JsonNode json : stepsNode) {
      keyword.addAction(json);
    }
    project.addCustomKeyword(keyword);
    funcService.create(project);
    
    project = funcService.get(project.getId());
    Assert.assertEquals(project.getCustomKeywords().size(), 1);
    
    CustomKeyword persitedKeyword = project.getCustomKeywords().iterator().next();
    Assert.assertEquals(persitedKeyword.getName(), keyword.getName());
    Assert.assertEquals(persitedKeyword.getActions(), keyword.getActions());
    
    String actions = "[{\"type\":\"get\",\"url\":\"http://saucelabs.com/test/guinea-pig/\"}, {\"type\":\"clickElement\",\"locator\":{\"type\":\"link text\",\"value\":\"i am a link\"}}]";
    stepsNode = m.readTree(actions);
    keyword = new CustomKeyword("custom_keyword");
    for (JsonNode json : stepsNode) {
      keyword.addAction(json);
    }
    project.addCustomKeyword(keyword);
    funcService.update(project);
    
    project = funcService.get(project.getId());
    Assert.assertEquals(project.getCustomKeywords().size(), 1);
    
    persitedKeyword = project.getCustomKeywords().iterator().next();
    Assert.assertEquals(persitedKeyword.getName(), "custom_keyword");
    Assert.assertEquals(persitedKeyword.getActions().size(), 2);
    
    project.removeCustomKeyword("custom_keyword");
    funcService.update(project);
    project = funcService.get(project.getId());
    Assert.assertEquals(project.getCustomKeywords().size(), 0);
  }
}
