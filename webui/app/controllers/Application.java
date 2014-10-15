/**
 * 
 */
package controllers;

import interceptor.AuthenticationInterceptor;
import interceptor.WizardInterceptor;

import java.util.Collection;

import models.vm.VMModel;

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

import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.index;
import views.html.signin;
import views.html.signup;

import com.mongodb.BasicDBObject;

import controllers.vm.VMCreator;

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
  
  public static Promise<Result> doSignup() throws Exception {
    DynamicForm form = Form.form().bindFromRequest();
    boolean group = Boolean.parseBoolean(form.get("group"));
    
    if (group) {
      String companyName = form.get("company");
      String companyHost = form.get("host");
      final Group company = new Group(companyName);
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
      company.addFeature(organization);  
      
      Role administration = new Role("Administration", company.getId());
      administration.put("desc", "This is administration role for organization management");
      administration.put("system", true);
      company.addRole(administration);
      
      for (Operation operation : organization.getOperations()) {
        administration.addPermission(new Permission(organization.getId(), operation.getId()));
      }
      administration.addUser(admin);
      admin.addRole(administration);
      
      Feature vmFeature = FeatureDAO.INSTANCE.find(new BasicDBObject("name", "Virtual Machine")).iterator().next();
      company.addFeature(vmFeature);
      
      Operation sysMgt = OperationDAO.INSANCE.find(new BasicDBObject("name", "Manage System VM")).iterator().next();
      Operation normalMgt = OperationDAO.INSANCE.find(new BasicDBObject("name", "Manage Test VM")).iterator().next();
      
      Role vmRole = new Role("VM Management", company.getId());
      
      vmRole.addPermission(new Permission(vmFeature.getId(), sysMgt.getId()));
      vmRole.addPermission(new Permission(vmFeature.getId(), normalMgt.getId()));

      vmRole.addUser(admin);
      admin.addRole(vmRole);
      company.addRole(vmRole);
      
      GroupDAO.INSTANCE.create(company);
      UserDAO.INSTANCE.create(admin);
      RoleDAO.INSTANCE.create(administration, vmRole);
      
      //Create system vm
      Promise<VMModel> result = Promise.promise(new Function0<VMModel>() {
        @Override
        public VMModel apply() throws Throwable {
          return VMCreator.createCompanySystemVM(company);
        }
      });
      
      session().clear();
      session().put("email", admin.getEmail());
      session().put("user_id", admin.getId());
      session().put("group_id", company.getId());
      
      return result.map(new Function<VMModel, Result>() {
        @Override
        public Result apply(VMModel a) throws Throwable {
          return redirect(controllers.routes.Application.dashboard());
        }
      });
    } else {
      String email = form.get("email");
      String password = form.get("password");
      User user = new User(email, email);
      user.put("password", password);
      UserDAO.INSTANCE.create(user);
      
      session().put("email", user.getEmail());
      session().put("user_id", user.getId());
      
      return Promise.<Result>pure(redirect(controllers.routes.Application.dashboard()));
    }
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
    
    //Initialize Organization feature
    FeatureInitializer.createOrganizationFeature(email, password);
    
    
    User root = UserDAO.INSTANCE.find(new BasicDBObject("system", true)).iterator().next();
    Group system = GroupDAO.INSTANCE.find(new BasicDBObject("system", true)).iterator().next();
    
    //Initialize VM Management feature
    FeatureInitializer.createVMFeature(root, system);
    
    //login
    session().clear();
    session().put("email", root.getEmail());
    session().put("user_id", root.getId());
    session().put("group_id", system.getId());
    
    return redirect(controllers.vm.routes.VMController.index());
  }
}
