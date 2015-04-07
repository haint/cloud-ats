/**
 * 
 */
package org.ats.services.organization;

import java.util.List;
import java.util.logging.Logger;

import org.ats.common.PageList;
import org.ats.services.OrganizationContext;
import org.ats.services.data.MongoDBService;
import org.ats.services.event.Event;
import org.ats.services.event.EventFactory;
import org.ats.services.organization.base.AbstractMongoCRUD;
import org.ats.services.organization.entity.Space;
import org.ats.services.organization.entity.User;
import org.ats.services.organization.entity.fatory.ReferenceFactory;
import org.ats.services.organization.entity.fatory.SpaceFactory;
import org.ats.services.organization.entity.reference.SpaceReference;
import org.ats.services.organization.entity.reference.TenantReference;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 13, 2015
 */
@Singleton
public class SpaceService extends AbstractMongoCRUD<Space> {
  
  /** .*/
  private final String COL_NAME = "org-space";
  
  /** .*/
  @Inject
  private SpaceFactory factory;
  
  /** .*/
  @Inject
  private OrganizationContext context;
  
  @Inject
  private EventFactory eventFactory;
  
  @Inject
  private ReferenceFactory<SpaceReference> refFactory;
  
  @Inject
  SpaceService(MongoDBService mongo, Logger logger) {
    this.col = mongo.getDatabase().getCollection(COL_NAME);
    this.logger = logger;
    
    this.createTextIndex("name");
    //
    this.col.createIndex(new BasicDBObject("created_date", 1));
    this.col.createIndex(new BasicDBObject("tenant._id", 1));
    this.col.createIndex(new BasicDBObject("roles._id", 1));
  }
  
  @Override
  public void delete(Space obj) {
    super.delete(obj);
    Event event = eventFactory.create(obj, "delete-space");
    event.broadcast();
  }
  
  @Override
  public void delete(String id) {
    super.delete(id);
    SpaceReference ref = refFactory.create(id);
    Event event = eventFactory.create(ref, "delete-space-ref");
    event.broadcast();
  }

  public Space transform(DBObject source) {
    Space space = factory.create((String) source.get("name"));
    space.put("_id", source.get("_id"));
    space.put("created_date", source.get("created_date"));
    space.put("desc", source.get("desc"));
    space.put("tenant", source.get("tenant"));
    space.put("roles", source.get("roles"));
    return space;
  }
  
  public OrganizationContext goTo(SpaceReference ref) {
    if (ref == null) throw new NullPointerException("The space could not be null");

    User user = this.context.getUser();
    if (user == null) {
      logger.info("The user has not logged yet");
      return null;
    }
    
    Space space = ref.get();
    if (space == null) {
      logger.info("The space " + ref.getId() + " does not exist");
      return null;
    }
    
    if (user.getSpaces().contains(ref)) {
      context.setSpace(space);
      logger.info("The user " + user.getEmail() + " has jumped to space " + space.getName());
      return context;
    }
    
    logger.info("The user " + user.getEmail() + " does not belong to space " + space.getName());
    return null;
  }
  
  public PageList<Space> findSpaceInTenant(TenantReference tenant) {
    BasicDBObject query = new BasicDBObject("tenant", tenant.toJSon());
    return query(query);
  }

  public void restoreSpace(List<DBObject> list) {
    
    this.col.insert(list);
  }
}
