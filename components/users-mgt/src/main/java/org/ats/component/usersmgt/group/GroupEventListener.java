/**
 * 
 */
package org.ats.component.usersmgt.group;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import org.ats.component.usersmgt.Event;
import org.ats.component.usersmgt.EventExecutedException;
import org.ats.component.usersmgt.EventListener;
import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.role.Role;
import org.ats.component.usersmgt.role.RoleDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;

import com.mongodb.BasicDBObject;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Jul 30, 2014
 */
public class GroupEventListener implements EventListener {

  public void execute(Event event) throws EventExecutedException {
    try {
      if ("delete-group".equals(event.getType())) {
        this.processDeleteGroupInUser(event);
        this.processDeleteGroupInRole(event);
        this.processDeleteGroupChildren(event);
        this.processDeleteGroupInParent(event);
      }
    } catch (UserManagementException e) {
      e.printStackTrace();
    }
  }
  
  private void processDeleteGroupInUser(Event event) throws UserManagementException {
    Group group = new Group().from(event.getSource());
    Pattern p = Pattern.compile(group.getId());
    Collection<User> users = UserDAO.getInstance(event.getDbName()).find(new BasicDBObject("group_ids", p));
    for (User user : users) {
      user.leaveGroup(group);
      UserDAO.getInstance(event.getDbName()).update(user);
    }
  }
  
  private void processDeleteGroupInRole(Event event) throws UserManagementException {
    Group group = new Group().from(event.getSource());
    Collection<Role> roles = RoleDAO.getInstance(event.getDbName()).find(new BasicDBObject("group_id", group.getId()));
    for (Role role : roles) {
      RoleDAO.getInstance(event.getDbName()).delete(role);
    }
  }
  
  private void processDeleteGroupChildren(Event event) throws UserManagementException {
    Group group = new Group().from(event.getSource());
    Set<Group> children = group.getGroupChildren();
    for (Group child : children) {
      GroupDAO.getInstance(event.getDbName()).delete(child);
    }
  }
  
  private void processDeleteGroupInParent(Event event) throws UserManagementException {
    Group group = new Group().from(event.getSource());
    Collection<Group> parents = GroupDAO.getInstance(event.getDbName()).find(new BasicDBObject("group_children_ids", Pattern.compile(group.getId())));
    for (Group parent : parents) {
      parent.removeGroupChild(group);
      GroupDAO.getInstance(event.getDbName()).update(parent);
    }
  }
}
