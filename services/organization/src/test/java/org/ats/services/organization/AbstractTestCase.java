/**
 * 
 */
package org.ats.services.organization;

import org.ats.services.OrganizationServiceModule;
import org.ats.services.data.DatabaseModule;
import org.ats.services.data.MongoDBService;
import org.ats.services.event.EventModule;
import org.ats.services.event.EventService;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 10, 2015
 */
public abstract class AbstractTestCase {
  
  /** .*/
  protected Injector injector;
  
  /** .*/
  protected MongoDBService mongoService;
  
  /** .*/
  protected EventService eventService;
  
  public void init(boolean separate) throws Exception {
    System.setProperty(EventModule.EVENT_CONF, "src/test/resources/event.conf");
    
    String host = "localhost";
    
    int port = 27017;
    
    String dbName = separate ? "test-db-" + System.currentTimeMillis() : "test-db";
    
    Injector injector = Guice.createInjector(new DatabaseModule(host, port, dbName), new EventModule(), new OrganizationServiceModule());
    
    this.mongoService = injector.getInstance(MongoDBService.class);
    
    this.injector = injector;
    
    //start event service
    eventService = injector.getInstance(EventService.class);
    eventService.setInjector(injector);
    eventService.start();
  }
  
  public void tearDown() throws Exception {
    System.out.println("Cleanup database");
    this.mongoService.dropDatabase();
  }
}
