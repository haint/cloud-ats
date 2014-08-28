/**
 * 
 */
package controllers;

import interceptor.AuthenticationInterceptor;
import interceptor.WizardInterceptor;

import java.util.Collection;

import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.feature.Feature;
import org.ats.component.usersmgt.feature.FeatureDAO;
import org.ats.component.usersmgt.feature.Operation;
import org.ats.component.usersmgt.feature.OperationDAO;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;
import org.ats.component.usersmgt.role.Permission;
import org.ats.component.usersmgt.role.Role;
import org.ats.component.usersmgt.role.RoleDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;

import com.mongodb.BasicDBObject;

import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.*;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Aug 1, 2014
 */
@With(WizardInterceptor.class)
public class Application extends Controller {

  public static Result index() {
    return ok(index.render());
  }
  
  public static Result signup(boolean group) {
    return ok(signup.render(group));
  }
  
  public static Result doSignup() throws UserManagementException {
    DynamicForm form = Form.form().bindFromRequest();
    boolean group = Boolean.parseBoolean(form.get("group"));
    
    if (group) {
      String companyName = form.get("company");
      String companyHost = form.get("host");
      Group company = new Group(companyName);
      company.put("host", companyHost);
      company.put("level", 1);
      
      String adminEmail = form.get("email");
      String adminPassword = form.get("password");
      
      User admin = new User(adminEmail, adminEmail);
      admin.put("password", adminPassword);
      company.addUser(admin.getId());
      admin.joinGroup(company);
      admin.put("joined", true);
      
      Feature organization = FeatureDAO.INSTANCE.find(new BasicDBObject("name", "Organization")).iterator().next();
      
      for (Feature f : FeatureDAO.INSTANCE.find(new BasicDBObject())) {
        company.addFeature(f);  
      }
      
      Role administration = new Role("Administration", company.getId());
      administration.put("desc", "This is administration role for organization management");
      administration.put("system", true);
      company.addRole(administration);
      
      for (Operation operation : organization.getOperations()) {
        administration.addPermission(new Permission(organization.getId(), operation.getId()));
      }
      administration.addUser(admin);
      admin.addRole(administration);
      
      GroupDAO.INSTANCE.create(company);
      UserDAO.INSTANCE.create(admin);
      RoleDAO.INSTANCE.create(administration);
      
      session().clear();
      session().put("email", admin.getEmail());
      session().put("user_id", admin.getId());
      session().put("group_id", company.getId());
    } else {
      String email = form.get("email");
      String password = form.get("password");
      User user = new User(email, email);
      user.put("password", password);
      UserDAO.INSTANCE.create(user);
      
      session().put("email", user.getEmail());
      session().put("user_id", user.getId());
    }
    return redirect(controllers.routes.Application.dashboard());
  }
  
  public static Result signin() {
    return ok(signin.render());
  }
  
  public static Result doSignin() throws UserManagementException {
    DynamicForm form = Form.form().bindFromRequest();
    String email = form.get("email");
    String password = form.get("password");
    Collection<User> users = UserDAO.INSTANCE.find(new BasicDBObject("email", email));
    
    if (users.isEmpty() || users.size() > 1) {
      flash().put("signin-faild", "true");
      return redirect(controllers.routes.Application.signin());
    }
    
    User user = users.iterator().next();
    if (user.getString("password").equals(password) && user.getBoolean("active")) {
      session().put("email", user.getEmail());
      session().put("user_id", user.getId());
      return redirect(controllers.routes.Application.dashboard());
    }
    
    flash().put("signin-faild", "true");
    return redirect(controllers.routes.Application.signin());
  }
  
  public static Result signout() {
    session().clear();
    return redirect(controllers.routes.Application.index());
  }
  
  public static Result untrail(String path) {
    return movedPermanently("/" + path);
  }
  
  @With(AuthenticationInterceptor.class)
  public static Result dashboard() {
    return ok(views.html.dashboard.dashboard.render());
  }
  
  public static Result wizard() throws UserManagementException {
    return ok(views.html.wizard.render());
  }
  
  public static Result doWizard() throws UserManagementException {
    session().clear();
    
    DynamicForm form = Form.form().bindFromRequest();
    String email = form.get("email");
    String password = form.get("password");
    
    Feature organization = new Feature("Organization");
    organization.put("desc", "This is organization management feature");
    organization.put("system", true);
    
    Operation ad = new Operation("Administration");
    organization.addOperation(ad);
    
    Group system = new Group("System Admin");
    system.put("desc", "This is group of system");
    system.put("system", true);
    system.put("level", 0);
    system.addFeature(organization);
    
    User root = new User(email, email);
    root.put("system", true);
    root.put("password", password);
    root.joinGroup(system);
    root.put("joined", true);
    
    system.addUser(root);
    
    Role administration = new Role("Administration", system.getId());
    administration.put("desc", "This is administration role for organization management");
    administration.put("system", true);
    administration.addPermission(new Permission(organization.getId(), ad.getId()));
    administration.addUser(root);
    root.addRole(administration);
    system.addRole(administration);

    //persist
    FeatureDAO.INSTANCE.create(organization);
    OperationDAO.INSANCE.create(ad);
    UserDAO.INSTANCE.create(root);
    GroupDAO.INSTANCE.create(system);
    RoleDAO.INSTANCE.create(administration);
    
    initMockData(root, system);
    
    return redirect(controllers.routes.Application.index());
  }
  
  private static void initMockData(User rootUser, Group rootGroup) throws UserManagementException {
    Feature foo = new Feature("Foo Feature");
    foo.put("desc", "This is dummy feature");
    
    Operation of1 = new Operation("Foo Action 1");
    of1.put("desc", "This is dummy operation");
    Operation of2 = new Operation("Foo Action 2");
    of2.put("desc", "This is dummy operation");
    Operation of3 = new Operation("Foo Action 3");
    of3.put("desc", "This is dummy operation");
    
    foo.addOperation(of1);
    foo.addOperation(of2);
    foo.addOperation(of3);
    
    Feature bar = new Feature("Bar Feature");
    bar.put("desc", "This is dummy feature");
    
    Operation ob1 = new Operation("Bar Action 1");
    ob1.put("desc", "This is dummy operation");
    Operation ob2 = new Operation("Bar Action 2");
    ob2.put("desc", "This is dummy operation");
    Operation ob3 = new Operation("Bar Action 3");
    ob3.put("desc", "This is dummy operation");
    
    bar.addOperation(ob1);
    bar.addOperation(ob2);
    bar.addOperation(ob3);
    
    Feature juu  = new Feature("Juu Feature");
    juu.put("desc", "This is dummy feature");
    
    Operation oj1 = new Operation("Juu Action 1");
    oj1.put("desc", "This is dummy operation");
    Operation oj2 = new Operation("Juu Action 2");
    oj2.put("desc", "This is dummy operation");
    Operation oj3 = new Operation("Juu Action 3");
    oj3.put("desc", "This is dummy operation");
    
    juu.addOperation(oj1);
    juu.addOperation(oj2);
    juu.addOperation(oj3);
    
    FeatureDAO.INSTANCE.create(foo, bar, juu);
    OperationDAO.INSANCE.create(of1, of2, of3, ob1, ob2, ob3, oj1, oj2, oj3);
    
    rootGroup.addFeature(foo);
    rootGroup.addFeature(bar);
    rootGroup.addFeature(juu);
    
    Role fooRole = new Role("Foo Role", rootGroup.getId());
    fooRole.put("desc", "This is dummy role");
    fooRole.addPermission(new Permission(foo.getId(), of1.getId()));
    fooRole.addPermission(new Permission(foo.getId(), of2.getId()));
    fooRole.addPermission(new Permission(foo.getId(), of3.getId()));
    
    Role barRole = new Role("Bar Role", rootGroup.getId());
    barRole.put("desc", "This is dummy role");
    barRole.addPermission(new Permission(bar.getId(), ob1.getId()));
    barRole.addPermission(new Permission(bar.getId(), ob2.getId()));
    barRole.addPermission(new Permission(bar.getId(), ob3.getId()));
    
    Role juuRole = new Role("Juu Role", rootGroup.getId());
    juuRole.put("desc", "This is dummy role");
    juuRole.addPermission(new Permission(juu.getId(), oj1.getId()));
    juuRole.addPermission(new Permission(juu.getId(), oj2.getId()));
    juuRole.addPermission(new Permission(juu.getId(), oj3.getId()));
    
    Role mixRole = new Role("Mix Role", rootGroup.getId());
    mixRole.put("desc", "This is dummy role");
    mixRole.addPermission(new Permission(foo.getId(), of1.getId()));
    mixRole.addPermission(new Permission(bar.getId(), ob2.getId()));
    mixRole.addPermission(new Permission(juu.getId(), oj3.getId()));
    
    
    rootGroup.addRole(fooRole);
    rootGroup.addRole(barRole);
    rootGroup.addRole(juuRole);
    rootGroup.addRole(mixRole);

    rootUser.addRole(fooRole);
    fooRole.addUser(rootUser);
    
    rootUser.addRole(barRole);
    barRole.addUser(rootUser);
    
    RoleDAO.INSTANCE.create(fooRole, barRole, juuRole, mixRole);
    GroupDAO.INSTANCE.update(rootGroup);
    UserDAO.INSTANCE.update(rootUser);
  }
}
