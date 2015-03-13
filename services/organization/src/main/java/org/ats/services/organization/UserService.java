/**
 * 
 */
package org.ats.services.organization;

import java.util.Date;
import java.util.logging.Logger;

import org.ats.common.PageList;
import org.ats.services.data.MongoDBService;
import org.ats.services.organization.entities.Space.SpaceRef;
import org.ats.services.organization.entities.Tenant.TenantRef;
import org.ats.services.organization.entities.User;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 10, 2015
 */
public class UserService extends AbstractMongoCRUD<User> {

  /** .*/
  private final String COL_NAME = "org-user";
  
  @Inject
  UserService(MongoDBService mongo, Logger logger) {
    this.col = mongo.getDatabase().getCollection(COL_NAME);
    this.logger = logger;
    this.createTextIndex("email", "first_name", "last_name");
    
    //create index for spaces, tenant, role and created_date
    this.col.createIndex(new BasicDBObject("created_date", 1));
    this.col.createIndex(new BasicDBObject("tenant._id", 1));
    this.col.createIndex(new BasicDBObject("spaces._id", 1));
    this.col.createIndex(new BasicDBObject("roles._id", 1));
  }
  
  public PageList<User> findUsersInSpace(SpaceRef space) {
    return findIn("spaces",  space);
  }
  
  public PageList<User> findUserInTenant(TenantRef tenant) {
    BasicDBObject query = new BasicDBObject("tenant", tenant.toJSon());
    return query(query);
  }
  
  public User transform(DBObject source) {
    String email = (String) source.get("email");
    String firstName = (String) source.get("first_name");
    String lastName = (String) source.get("last_name");
    Date created_date = (Date) source.get("created_date");
    User user = new User(email, firstName, lastName);
    user.put("created_date", created_date);
    user.put("tenant", source.get("tenant"));
    user.put("spaces", source.get("spaces"));
    user.put("roles", source.get("roles"));
    return user;
  }
}
