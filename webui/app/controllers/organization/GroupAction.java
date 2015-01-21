/**
 * 
 */
package controllers.organization;

import interceptor.AuthenticationInterceptor;
import interceptor.Authorization;
import interceptor.WithoutSystem;
import interceptor.WizardInterceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.ats.component.usersmgt.EventExecutor;
import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.feature.Feature;
import org.ats.component.usersmgt.feature.FeatureDAO;
import org.ats.component.usersmgt.feature.Operation;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;
import org.ats.component.usersmgt.role.Permission;
import org.ats.component.usersmgt.role.Role;
import org.ats.component.usersmgt.role.RoleDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;

import play.api.templates.Html;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.organization.index;
import views.html.organization.group.adduser;
import views.html.organization.group.editgroup;
import views.html.organization.group.invite;
import views.html.organization.group.newgroup;

import com.mongodb.BasicDBObject;

import controllers.Application;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Aug 12, 2014
 */
@With({WizardInterceptor.class, AuthenticationInterceptor.class})
@Authorization(feature = "Organization", operation = "Administration")

public class GroupAction extends Controller {
  
  @WithoutSystem
  public static Result newGroup() throws UserManagementException {
    Group current = Organization.setCurrentGroup(null);
    if (current == null) return forbidden(views.html.forbidden.render());
    
    session().put("group_id", current.getId());
    
    Html body = newgroup.render(current.getFeatures());
    return ok(index.render("group" , body, current.getId()));
  }
  
  @WithoutSystem
  public static Result newGroupBody() throws UserManagementException {
    Group current = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    return ok(newgroup.render(current.getFeatures()));
  }
  
  @WithoutSystem
  public static Result doCreate() throws UserManagementException {
    String name = request().getQueryString("name");
    Group group = new Group(Application.dbName, name);
    group.put("desc", request().getQueryString("desc"));
    String[] features = request().queryString().get("feature");
    for (String f : features) {
      group.addFeature(FeatureDAO.getInstance(Application.dbName).findOne(f));
    }
    
    
    Feature organization = FeatureDAO.getInstance(Application.dbName).find(new BasicDBObject("name", "Organization")).iterator().next();
    group.addFeature(organization);
    
    Role administration = new Role(Application.dbName, "Administration", group.getId());
    administration.put("desc", "This is administration role for organization management");
    administration.put("system", true);
    group.addRole(administration);
    
    for (Operation operation : organization.getOperations()) {
      administration.addPermission(new Permission(Application.dbName, organization.getId(), operation.getId()));
    }
    
    Group current = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    int level = current.getInt("level");
    group.put("level", level + 1);
    current.addGroupChild(group);
    
    GroupDAO.getInstance(Application.dbName).create(group);
    GroupDAO.getInstance(Application.dbName).update(current);
    RoleDAO.getInstance(Application.dbName).create(administration);
    
    session().put("group_id", group.getId());
    
    return redirect(routes.Organization.body());
  }
  
  @WithoutSystem
  public static Result invite() throws UserManagementException {
    Group currentGroup = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
    
    if (currentGroup.getInt("level") == 1) {
      BasicDBObject query = new BasicDBObject("group_id", currentGroup.getId());
      query.put("name", "Administration");
      query.put("user_ids", Pattern.compile(currentUser.getId()));
      
      Role adRole = RoleDAO.getInstance(Application.dbName).find(query).iterator().next();
      
      StringBuilder sb = new StringBuilder();
      for (Group g : GroupDAO.getInstance(Application.dbName).buildParentTree(currentGroup)) {
        sb.append("/").append(g.get("name"));
      }
      sb.append("/").append(currentGroup.getString("name"));
      String groupPath = sb.toString();
      
      sb.setLength(0);
      sb.append("http://").append(request().host());
      sb.append(routes.Invitation.index(currentUser.getId(), adRole.getId(), currentGroup.getId()));
      
      Html body = invite.render(groupPath, sb.toString());
      return ok(index.render("group" , body, currentGroup.getId()));
    } else if (currentGroup.getInt("level") > 1) {
      
      Html body = adduser.render(getAvailableUser(currentGroup), GroupDAO.getInstance(Application.dbName).buildParentTree(currentGroup).getLast(), currentGroup);
      return ok(index.render("group" , body, currentGroup.getId()));
    } else {
      return forbidden(views.html.forbidden.render());
    }
  }
  
  public static Result addUser() throws UserManagementException {
    Group currentGroup = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    if (request().getQueryString("user") != null) {
      String[] users = request().queryString().get("user");
      for (String u : users) {
        User user = UserDAO.getInstance(Application.dbName).findOne(u);
        user.joinGroup(currentGroup);
        currentGroup.addUser(user);

        UserDAO.getInstance(Application.dbName).update(user);
        GroupDAO.getInstance(Application.dbName).update(currentGroup);
      }
    }
    return redirect(routes.Organization.index() + "?nav=user&group=" + currentGroup.getId());
  }
  
  public static Result editGroup(String g) throws UserManagementException {
    if ("forgroup".equals(g) && request().getQueryString("group") != null) {
      g = request().getQueryString("group");
    }
    
    Group group_ = GroupDAO.getInstance(Application.dbName).findOne(g);
    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
    Group currentGroup = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    
    //Prevent edit system group or group level 1
    if (group_.getBoolean("system")) return forbidden(views.html.forbidden.render());

    //Prevent edit group level 1 if current user is not system
    if (group_.getInt("level") == 1 && ! currentUser.getBoolean("system") &&  !currentGroup.getBoolean("system")) return forbidden(views.html.forbidden.render());
    
    //Prevent edit group which has no right permission
    if (!Organization.isSystem(currentUser) 
        && !currentGroup.getBoolean("system") 
        && !Organization.isRightAdministration(group_)) {
      return forbidden(views.html.forbidden.render());
    }
    
    LinkedList<Group> parents = GroupDAO.getInstance(Application.dbName).buildParentTree(group_);
    
    Html body = editgroup.render(group_, parents.isEmpty() ? new ArrayList<Feature>(FeatureDAO.getInstance(Application.dbName).find(new BasicDBObject())) : parents.getLast().getFeatures());
    return ok(index.render("group", body, group_.getId()));
  }
  
  public static Result doEditGroup(String g) throws UserManagementException {
    Group group_ = GroupDAO.getInstance(Application.dbName).findOne(g);
    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
    Group currentGroup = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    
    //Prevent edit system group
    if (group_.getBoolean("system")) return forbidden(views.html.forbidden.render());

    //Prevent edit group level 1 if current user is not system
    if (group_.getInt("level") == 1 && !Organization.isSystem(currentUser) &&  !currentGroup.getBoolean("system")) return forbidden(views.html.forbidden.render());
    
    //Prevent edit group which has no right permission
    if (!Organization.isSystem(currentUser) 
        && !currentGroup.getBoolean("system")
        && !Organization.isRightAdministration(group_)) {
      
      return forbidden(views.html.forbidden.render());
    }
    
    //
    Set<String> currentFeature = group_.getString("feature_ids") == null ? new HashSet<String>() : group_.stringIDtoSet(group_.getString("feature_ids"));
    Set<String> actualFeature = new HashSet<String>();
    
    if (request().getQueryString("feature") != null) Collections.addAll(actualFeature, request().queryString().get("feature"));
    
    //Add new feature
    for (String f : actualFeature) {
      if (!currentFeature.contains(f)) {
        Feature feature = FeatureDAO.getInstance(Application.dbName).findOne(f);
        group_.addFeature(feature);
      }
    }
    
    //Remove no longer feature
    for (String f : currentFeature) {
      if (!actualFeature.contains(f)) {
        Feature feature = FeatureDAO.getInstance(Application.dbName).findOne(f);
        if (feature.getBoolean("system")) continue;
        group_.removeFeature(feature);
        
        //remove in children
        for (Group child : group_.getAllChildren()) {
          child.removeFeature(feature);
          GroupDAO.getInstance(Application.dbName).update(child);
        }
      }
    }
    
    if (request().getQueryString("name") != null) group_.put("name", request().getQueryString("name"));
    group_.put("desc", request().getQueryString("desc"));
    GroupDAO.getInstance(Application.dbName).update(group_);
    
    return redirect(routes.Organization.index() + "?nav=group&group=" + currentGroup.getId());
  }
  
  /**
   * TODO: Prevent delete group which no has right permission by pass query
   * @param g
   * @return
   * @throws UserManagementException
   */
  public static Result deleteGroup(String g) throws UserManagementException {
    if ("forgroup".equals(g) && request().getQueryString("group") != null) {
      g = request().getQueryString("group");
    }
    
    Group group_   = GroupDAO.getInstance(Application.dbName).findOne(g);
    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
    Group currentGroup = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));
    
    //Prevent delete system group or group level 1
    if (group_.getBoolean("system")) return forbidden(views.html.forbidden.render());
    
    //Prevent delete group which has no right permission
    if (!Organization.isSystem(currentUser) 
        && !currentGroup.getBoolean("system")
        && !Organization.isRightAdministration(group_)) {
      
      return forbidden(views.html.forbidden.render());
    }
    
    BasicDBObject query = new BasicDBObject("joined", true);
    query.put("group_ids", Pattern.compile(group_.getId()));
    Collection<User> users_ = UserDAO.getInstance(Application.dbName).find(query);

    GroupDAO.getInstance(Application.dbName).delete(group_);
    
    while(EventExecutor.getInstance(Application.dbName).isInProgress()) {
    }
    
    if (group_.getInt("level") == 1) {
      for (User u : users_) {
        u = UserDAO.getInstance(Application.dbName).findOne(u.getId());
        u.put("joined", false);
        UserDAO.getInstance(Application.dbName).update(u);
      }
    }
    
    return redirect(routes.Organization.index() + "?nav=group");
  }
  
  private static List<User> getAvailableUser(Group groupInvitation) throws UserManagementException {
    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
    LinkedList<Group> parents = GroupDAO.getInstance(Application.dbName).buildParentTree(groupInvitation);
    Group parent = parents.getLast();
    
    for (Group g : parents) {
      BasicDBObject query = new BasicDBObject("name", "Administration");
      query.put("system", true);
      query.put("group_id", g.getId());
      query.put("user_ids", Pattern.compile(currentUser.getId()));
      if (!RoleDAO.getInstance(Application.dbName).find(query).isEmpty()) {
        List<User> users = parent.getUsers();
        users.removeAll(groupInvitation.getUsers());
        return users;
      }
    }
    
    return Collections.emptyList();
  }
}
