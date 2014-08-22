/**
 * 
 */
package controllers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.feature.Feature;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;
import org.ats.component.usersmgt.role.Permission;
import org.ats.component.usersmgt.role.Role;
import org.ats.component.usersmgt.role.RoleDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;

import com.mongodb.BasicDBObject;

import play.mvc.Controller;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Aug 22, 2014
 */
public class FeatureAction extends Controller {

  public static boolean hasPermissionOnFeature(Feature feature) throws UserManagementException {
    User currentUser = UserDAO.INSTANCE.findOne(session("user_id"));
    Group currentGroup = GroupDAO.INSTANCE.findOne(session("group_id"));
    
    if (currentGroup == null) return false;
    
    if (!currentGroup.getFeatures().contains(feature)) return false;
    
    if (feature.getBoolean("disable")) return false;
    
    //check for Organization feature
    if (feature.getBoolean("system") && feature.getName().equals("Organization")) {
      LinkedList<Group> parents = currentGroup.buildParentTree();
      for (Group parent : parents) {
        BasicDBObject query = new BasicDBObject("name", "Administration");
        query.append("system", true);
        query.append("group_id", parent.getId());
        query.append("user_ids", Pattern.compile(currentUser.getId()));
        if (!RoleDAO.INSTANCE.find(query).isEmpty()) return true;
      }
    }
    
    //check current user has right permission to perform feature on current group
    for (Role role : currentGroup.getRoles()) {
      if (role.getUsers().contains(currentUser)) {
        for (Permission per : role.getPermissions()) {
          if (per.getFeature().equals(feature)) return true;
        }
      }
    }
    
    return false;
  }
}
