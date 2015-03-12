/**
 * 
 */
package org.ats.services.organization.entities;

import java.util.ArrayList;
import java.util.List;

import org.ats.services.data.common.Reference;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Mar 9, 2015
 */
@SuppressWarnings("serial")
public class Feature extends BasicDBObject {
  
  public static final FeatureRef ANY = new FeatureRef("*");
  
  public Feature(String name, String... actions) {
    this.put("_id", name);
    for (String action : actions) {
      addAction(new Action(action));
    }
  }
  
  public void addAction(Action action) {
    Object obj = this.get("actions");
    BasicDBList actions = obj == null ? new BasicDBList() : (BasicDBList) obj;
    actions.add(action);
    this.put("actions", actions);
  }
  
  public void removeAction(String action) {
    this.removeAction(new Action(action));
  }
  
  public void removeAction(Action action) {
    Object obj = this.get("actions");
    BasicDBList actions = obj == null ? new BasicDBList() : (BasicDBList) obj;
    actions.remove(action);
    this.put("actions", actions);
  }
  
  public boolean hasAction(Action action) {
    if (action == Action.ANY) return true;
    Object obj = this.get("actions");
    return obj == null ? false : ((BasicDBList) obj).contains(action);
  }
  
  public List<Action> getActions() {
    Object obj = this.get("actions");
    BasicDBList actions = obj == null ? new BasicDBList() : (BasicDBList) obj;
    List<Action> list = new ArrayList<Action>();
    for (int i = 0; i < actions.size(); i++) {
      list.add((Action) actions.get(i));
    }
    return list;
  }
  
  public static class Action extends BasicDBObject {
    
    public static final Action ANY = new Action("*");
    
    public Action(String name) {
      this.put("_id", name);
    }
    
    public String getId() {
      return this.getString("_id");
    }
    
    public String getName() {
      return getId();
    }
    
    public void setDescription(String desc) {
      this.put("desc", desc);
    }
    
    public String getDescription() {
      return this.getString("desc");
    }
  }

  public static class FeatureRef extends Reference<Feature> {
    
    public FeatureRef(String id) {
      super(id);
    }

    @Override
    public Feature get() {
      return null;
    }
  }
}
