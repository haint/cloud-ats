/**
 * 
 */
package org.ats.services.organization;

import java.util.logging.Logger;

import org.ats.services.data.MongoDBService;
import org.ats.services.organization.entity.Feature;
import org.ats.services.organization.entity.fatory.FeatureFactory;

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
public class FeatureService extends AbstractMongoCRUD<Feature> {
  
  /** .*/
  private final String COL_NAME = "org-feature";
  
  /** .*/
  private FeatureFactory factory;
  
  @Inject
  FeatureService(MongoDBService mongo, Logger logger, FeatureFactory factory) {
    this.col = mongo.getDatabase().getCollection(COL_NAME);
    this.logger = logger;
    this.factory = factory;
    
    this.createTextIndex("_id");
    this.col.createIndex(new BasicDBObject("created_date", 1));
    this.col.createIndex(new BasicDBObject("actions._id", 1));
  }

  public Feature transform(DBObject source) {
    Feature feature = factory.create((String) source.get("_id"));
    feature.put("created_date", source.get("created_date"));
    feature.put("actions", source.get("actions"));
    return feature;
  }

}
