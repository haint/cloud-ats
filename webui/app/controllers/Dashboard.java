/**
 * 
 */
package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;

import com.mongodb.BasicDBObject;

import interceptor.AuthenticationInterceptor;
import interceptor.WizardInterceptor;
import play.api.templates.Html;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import scala.collection.mutable.StringBuilder;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Aug 5, 2014
 */

@With({WizardInterceptor.class, AuthenticationInterceptor.class})
public class Dashboard extends Controller {

  public static Result body() {
    return ok(views.html.dashboard.body.render());
  }
  
  public static Html groupMenuList() throws UserManagementException {
    StringBuilder sb = new StringBuilder();
    User currentUser = UserDAO.INSTANCE.findOne(session("user_id"));
    
    List<Group> groups = currentUser.getBoolean("system") ? new ArrayList<Group>(GroupDAO.INSTANCE.find(new BasicDBObject())) :  currentUser.getGroups();
    Collections.sort(groups, new Comparator<Group>() {
      @Override
      public int compare(Group o1, Group o2) {
        return o1.getInt("level") - o2.getInt("level");
      }
    });
    for (Group group : groups) {
      sb.append("<li><a href='");
      sb.append(controllers.routes.Dashboard.changeGroup(group.getId())).append("'>");
      LinkedList<Group> parents = group.buildParentTree();
      for (Group p : parents) {
        sb.append(" / ").append(p.getString("name"));
      }
      sb.append(" / ").append(group.getString("name"));
      sb.append("</a></li>");
    }
    return new Html(sb);
  }
  
  public static Result changeGroup(String g) throws UserManagementException {
    User currentUser = UserDAO.INSTANCE.findOne(session("user_id"));
    Group group = GroupDAO.INSTANCE.findOne(g);
    if (group.getUsers().contains(currentUser) || currentUser.getBoolean("system")) {
      String referer = request().getHeader("referer");
      session("group_id", group.getId());
      //return redirect(referer);
    }
    return redirect(controllers.routes.Application.dashboard());
  }
}
