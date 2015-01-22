/**
 * 
 */
package controllers.vm;

import static akka.pattern.Patterns.ask;
import helpervm.OfferingHelper;
import helpervm.VMHelper;
import interceptor.AuthenticationInterceptor;
import interceptor.Authorization;
import interceptor.VMWizardIterceptor;
import interceptor.WithSystem;
import interceptor.WizardInterceptor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;

import models.vm.DefaultOfferingModel;
import models.vm.OfferingModel;
import models.vm.VMModel;

import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.http.impl.client.CloseableHttpClient;
import org.ats.cloudstack.CloudStackAPI;
import org.ats.cloudstack.CloudStackClient;
import org.ats.cloudstack.ServiceOfferingAPI;
import org.ats.cloudstack.VirtualMachineAPI;
import org.ats.cloudstack.model.ServiceOffering;
import org.ats.cloudstack.model.VirtualMachine;
import org.ats.common.html.HtmlParser;
import org.ats.common.html.XPathUtil;
import org.ats.common.http.HttpClientUtil;
import org.ats.common.ssh.SSHClient;
import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.feature.Feature;
import org.ats.component.usersmgt.feature.Operation;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;
import org.ats.component.usersmgt.role.Permission;
import org.ats.component.usersmgt.role.Role;
import org.ats.component.usersmgt.role.RoleDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;
import org.ats.jenkins.JenkinsMaster;
import org.ats.jenkins.JenkinsSlave;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import play.Logger;
import play.api.templates.Html;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import play.mvc.With;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import utils.Util;
import views.html.vm.alert;
import views.html.vm.amazontemplate;
import views.html.vm.azuretemplate;
import views.html.vm.cloudstacktemplate;
import views.html.vm.index;
import views.html.vm.offering;
import views.html.vm.offeringbody;
import views.html.vm.propertiesbody;
import views.html.vm.terminal;
import views.html.vm.vmbody;
import views.html.vm.vmproperties;
import views.html.vm.vmstatus;
import azure.AzureClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineRoleSize;
import com.mongodb.BasicDBObject;

import controllers.Application;
import controllers.organization.Organization;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Sep 4, 2014
 */
@With({WizardInterceptor.class, AuthenticationInterceptor.class})
@Authorization(feature = "Virtual Machine", operation = "")
public class VMController extends Controller {

  @With(VMWizardIterceptor.class)
  public static Result index() throws Exception {
    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));

    boolean system = checkCurrentSystem();
    Group group = null;
    if (system) {
      group = GroupDAO.getInstance(Application.dbName).find(new BasicDBObject("system", true)).iterator().next();
    } else {
      BasicDBObject query = new BasicDBObject("level", 1);
      query.append("user_ids", Pattern.compile(currentUser.getId()));
      group = GroupDAO.getInstance(Application.dbName).find(query).iterator().next();
    }
    
    if (hasPermission(group, "Manage System VM")) {
      return redirect(routes.VMController.systemVmView(group.getId()));
    } else {
      return redirect(routes.VMController.normalVMView(group.getId()));
    }
  }
  
  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage System VM")
  public static Result systemVmView(String groupId) throws Exception {
    Group group = GroupDAO.getInstance(Application.dbName).findOne(groupId);
    Html html = vmbody.render(checkCurrentSystem(), group, VMHelper.getVMsByGroupID(group.getId(), new BasicDBObject("system", true)), false);
    return ok(index.render(html));
  }
  
  @With(VMWizardIterceptor.class)
  @WithSystem
  public static Result changeGroup(String groupId) throws Exception {
    Group group = GroupDAO.getInstance(Application.dbName).findOne(groupId);
    Html html = vmbody.render(checkCurrentSystem(), group, VMHelper.getVMsByGroupID(group.getId(), new BasicDBObject("system", true)), false);
    return ok(index.render(html));
  }

  @With(VMWizardIterceptor.class)
  public static WebSocket<JsonNode> vmStatus(final String groupId, final String sessionId) {
    return new WebSocket<JsonNode>() {
      @Override
      public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        try {
          VMStatusActor.addChannel(new VMChannel(sessionId, groupId, out));
        } catch (Exception e) {
          Logger.debug("Can not create akka for vm status actor", e);
        }
      }
    };
  }
  
  @With(VMWizardIterceptor.class)
  public static WebSocket<JsonNode> vmLog(final String groupId, final String sessionId) {
    return new WebSocket<JsonNode>() {
      @Override
      public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        try {
          Await.result(ask(VMLogActor.actor, new VMChannel(sessionId, groupId, out), 1000), Duration.create(1, TimeUnit.SECONDS));
        } catch (Exception e) {
          Logger.debug("Can not create akka for vm log actor", e);
        }
      }
    };
  }

  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage System VM")
  public static Result offeringView(String groupId) throws Exception {
    Group group = GroupDAO.getInstance(Application.dbName).findOne(groupId);
    boolean system = checkCurrentSystem();
    List<OfferingModel> list = group.getBoolean("system") ? OfferingHelper.getOfferings() : OfferingHelper.getEnableOfferings();
    Html html = offeringbody.render(system, group, list);
    return ok(index.render(html));
  }

  @WithSystem
  public static Result getOfferings() throws Exception {
    String cloudstackApiUrl = request().getQueryString("cloudstack-api-url");
    String cloudstackApiKey = request().getQueryString("cloudstack-api-key");
    String cloudstackApiSecret = request().getQueryString("cloudstack-api-secret");
    CloudStackClient client = new CloudStackClient(cloudstackApiUrl, cloudstackApiKey, cloudstackApiSecret);

    List<ServiceOffering> list = ServiceOfferingAPI.listServiceOfferings(client, null, null);
    return ok(offering.render(list));
  }

  @WithSystem
  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage System VM")
  public static Result propertiesView() throws Exception {
    Group group = GroupDAO.getInstance(Application.dbName).find(new BasicDBObject("system", true)).iterator().next();
    return ok(index.render(propertiesbody.render(checkCurrentSystem(), group, VMHelper.getSystemProperties())));
  }
  
  @WithSystem
  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage System VM")
  public static Result saveProperties() throws Exception {
    DynamicForm form = Form.form().bindFromRequest();
    String cloudstackApiUrl = form.get("cloudstack-api-url");
    String cloudstackApiKey = form.get("cloudstack-api-key");
    String cloudstackApiSecret = form.get("cloudstack-api-secret");
    String cloudstackUsername = form.get("cloudstack-username");
    String cloudstackPassword = form.get("cloudstack-password");
    
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("cloudstack-api-url", cloudstackApiUrl);
    properties.put("cloudstack-api-key", cloudstackApiKey);
    properties.put("cloudstack-api-secret", cloudstackApiSecret);
    properties.put("cloudstack-username", cloudstackUsername);
    properties.put("cloudstack-password", cloudstackPassword);

    VMHelper.updateSystemProperties(properties);
    return redirect(routes.VMController.propertiesView());
  }

  @WithSystem
  public static Result doWizard() throws UserManagementException, IOException {
    DynamicForm form = Form.form().bindFromRequest();
    //get queryString for azure api
    
    String subcriptionIdAzure = form.get("subscription-id");
    String keystoreType = form.get("keystore-type");
    String keystorePwd = form.get("keystore-password");    
    String defaultUser = form.get("default-user");
    String defaultPassword = form.get("default-password");
    
    String keystorePath = "";
    File fileinput = null;
    play.mvc.Http.MultipartFormData _body = request().body().asMultipartFormData();        
    play.mvc.Http.MultipartFormData.FilePart keystorefile = _body.getFile("keystore-file");

    if (keystorefile != null) {
      String fileName = keystorefile.getFilename();
      fileinput = keystorefile.getFile();
      String rootPath = play.Play.application().path().getAbsolutePath();  
      String sepPath = System.getProperty("file.separator");
      keystorePath = sepPath + "conf" + sepPath;
      
      //uload
      Util.uploadFile(fileinput, fileName, rootPath + keystorePath);
      keystorePath = keystorePath + fileName;
    }
    
    String gitlabToken = form.get("gitlab-token");
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("subscription-id", subcriptionIdAzure);
    properties.put("keystore-type", keystoreType);
    properties.put("keystore-password", keystorePwd);
    properties.put("keystore-path", keystorePath);    
    properties.put("default-user", defaultUser);
    properties.put("default-password", defaultPassword);
    properties.put("gitlab-api-token", gitlabToken);
    
    //insert properties into database
    VMHelper.setSystemProperties(properties);
    
    //insert offer
    createOffering(form);
    return redirect(controllers.vm.routes.VMController.index());
  }

  private static void createOffering(DynamicForm form) throws IOException {
    String[] list = AzureClient.getAvailabilityOfferingNames();
    for (String offering : list) {
      OfferingModel model = null;
      
      switch (offering) {
      case VirtualMachineRoleSize.EXTRASMALL:
        model = new OfferingModel(offering, offering, 1, -1, 768);
        model.put("disabled", form.get(offering) != null ? false : true);
        break;
      case VirtualMachineRoleSize.SMALL:
        model = new OfferingModel(offering, offering, 1, -1, 1792);
        model.put("disabled", form.get(offering) != null ? false : true);
        break;
      case VirtualMachineRoleSize.MEDIUM:
        model = new OfferingModel(offering, offering, 2, -1, 3584);
        model.put("disabled", form.get(offering) != null ? false : true);
        break;
      default:
        break;
      }
      
      OfferingHelper.createOffering(model);
    }
  }
  
  @With(VMWizardIterceptor.class)
  public static Result viewConsoleURL(String vmId) {
    String cloudstackApiUrl = VMHelper.getSystemProperty("cloudstack-api-url");
    String cloudstackUsername = VMHelper.getSystemProperty("cloudstack-username");
    String cloudstackPassword = VMHelper.getSystemProperty("cloudstack-password");

    try {
      CloseableHttpClient client = CloudStackAPI.login(VMHelper.getCloudStackClient(), cloudstackUsername, cloudstackPassword);
      String cloudstackConsoleUrl = cloudstackApiUrl.substring(0, cloudstackApiUrl.lastIndexOf('/') + 1) + "console?cmd=access&vm=" + vmId;
      String response = HttpClientUtil.fetch(client, cloudstackConsoleUrl);

      HtmlParser parser = new HtmlParser();
      Document doc = parser.parseWellForm(response);
      NodeList nodeList = (NodeList)XPathUtil.read(doc, "/html/frameset/frame", XPathConstants.NODESET);
      Node node = nodeList.item(0);
      response().setContentType("html");
      String src = node.getAttributes().getNamedItem("src").getNodeValue();
      return ok(terminal.render(src));
    } catch (Exception e) {
      Logger.debug("Has error when view vm remote console", e);
      return ok();
    }
  }

  /**
   * Start, Stop or Restore VM
   * @param action
   * @param vmId
   * @return
   * @throws Exception
   */
  @With(VMWizardIterceptor.class)
  public static Result vmAction(String action, final String vmId) throws Exception {

    if (! hasRightPermission(vmId)) return forbidden(views.html.forbidden.render());
    
    final AzureClient azureClient = VMHelper.getAzureClient();

    if ("start".equals(action)) {
      Future<OperationStatusResponse> response = azureClient.startVirtualMachineByName(vmId);
      response.get();
      Logger.debug("Start vm " + vmId + " successfully");
    } else if ("stop".equals(action)) {
      Future<OperationStatusResponse> response = azureClient.stopVirtualMachineByName(vmId);      
      response.get();
      Logger.debug("Stop vm " + vmId + " successfully");
    } else if ("restore".equals(action)) {
      
//      VirtualMachineAPI.restoreVM(client, vmId, null);
    } else if ("destroy".equals(action)) {
      
      final VMModel vm = VMHelper.getVMByID(vmId);
      
      if (!hasPermission(vm.getGroup(), "Manage Test VM"))
        return forbidden(views.html.forbidden.render());
      
      VMHelper.removeVM(vm);
      
      //remote node of chef server
      VMModel jenkins = VMHelper.getVMsByGroupID(vm.getGroup().getId(), new BasicDBObject("jenkins", true)).get(0);
      VMHelper.getKnife(jenkins).deleteNode(vm.getName());
      
      //remove node of jenkins server
      try {
        new JenkinsSlave( new JenkinsMaster(jenkins.getPublicIP(), "http", 8080), vm.getPublicIP()).release();
      } catch (IOException e) {
        Logger.debug("Could not release jenkins node ", e);
      }
      
      
      //remove node from guacamole
      
      boolean isGui = vm.getBoolean("gui");
      Session session = SSHClient.getSession(jenkins.getPublicIP(), 22, jenkins.getUsername(), jenkins.getPassword());
      ChannelExec channel = (ChannelExec) session.openChannel("exec");
      String command = "";
      if (isGui) {
        try {
          channel = (ChannelExec) session.openChannel("exec");
          command = "sudo -S -p '' /etc/guacamole/manage_con.sh "
              + vm.getPublicIP() + " 5900 '"
              + VMHelper.getSystemProperty("default-password") + "' vnc 1";
          channel.setCommand(command);
          OutputStream out = channel.getOutputStream();
          channel.connect();

          out.write((jenkins.getPassword() + "\n").getBytes());
          out.flush();
          channel.disconnect();
        } catch (Exception e) {
          Logger.error("Exception when run:" + e.getMessage());
        }

        Logger.info("Command run add connection guacamole: " + command);

      } else {

        try {
          channel = (ChannelExec) session.openChannel("exec");
          command = "sudo -S -p '' /etc/guacamole/manage_con.sh "
              + vm.getPublicIP() + " 22 '"
              + VMHelper.getSystemProperty("default-password") + "' ssh 1";
          channel.setCommand(command);
          OutputStream out = channel.getOutputStream();
          channel.connect();

          out.write((jenkins.getPassword() + "\n").getBytes());
          out.flush();
          channel.disconnect();
        } catch (Exception e) {
          Logger.error("Exception when run:" + e.getMessage());
        }

        Logger.info("Command run add connection guacamole: " + command);

      }
      
      //delete virtual machine from azure
      Future<OperationStatusResponse> response = azureClient.deleteVirtualMachineByName(vmId);
      response.get();
      Logger.info("Destroy vm " + vmId + "successfully");
      
    }

    return status(200);
  }

  @WithSystem
  @With(VMWizardIterceptor.class)
  public static Result updateVMProperties() throws Exception {
    DynamicForm form = Form.form().bindFromRequest();

    CloudStackClient client = VMHelper.getCloudStackClient();
    VMModel vmModel = VMHelper.getVMByID(form.get("vmId"));

    String ip = form.get("ip");
    String username = form.get("username");
    String password = form.get("password");

    ObjectNode json = Json.newObject();

    if (vmModel.getPublicIP().equals(ip)) {
      vmModel.put("username", username);
      vmModel.put("password", password);
      VMHelper.updateVM(vmModel);

      json.put("vmStatus", vmstatus.render(vmModel, true).toString());
      json.put("vmProperties", vmproperties.render(vmModel, true, null, true).toString());
      return ok(json);
    }

    List<VirtualMachine> vms = VirtualMachineAPI.listVirtualMachines(client, null, null, null, vmModel.getTemplateId(), VMDetails.nics);
    for (VirtualMachine vm : vms) {
      if (ip.equals(vm.nic[0].ipAddress)) {
        VMModel existedVMModel = VMHelper.getVMByID(vm.id);
        if (existedVMModel != null) {
          json.put("vmStatus", vmstatus.render(vmModel, true).toString());
          json.put("vmProperties", vmproperties.render(vmModel, true, alert.render("The ip " + ip + " is in use."), true).toString());
          return ok(json);
        } else {
          VMHelper.removeVM(vmModel);
          vmModel.put("_id", vm.id);
          vmModel.put("public_ip", ip);
          vmModel.put("username", username);
          vmModel.put("password", password);
          VMHelper.createVM(vmModel);

          json.put("vmStatus", vmstatus.render(vmModel, true).toString());
          json.put("vmProperties", vmproperties.render(vmModel, true, null, true).toString());
          return ok(json);
        }
      }
    }

    json.put("vmStatus", vmstatus.render(vmModel, true).toString());
    json.put("vmProperties", vmproperties.render(vmModel, true, alert.render("The ip " + ip + " is not available."), true).toString());
    return ok(json);
  }

  private static boolean hasRightPermission(String vmId) throws Exception {

    User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
    VMModel vmModel = VMHelper.getVMByID(vmId);
    Group group = vmModel.getGroup();

    if (!checkCurrentSystem()) {
      if (!group.getUsers().contains(currentUser)) return false;

      if (vmModel.getBoolean("system")) {
        return hasPermission(group, "Manage System VM");
      } else {
        return hasPermission(group, "Manage Test VM");
      }
    }

    return true;
  }

  public static boolean hasPermission(Group group, String operation) throws Exception {
    if (!checkCurrentSystem()) {

      User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));

      //check user ACL
      if (group.getInt("level") != 1 || !group.getUsers().contains(currentUser)) {
        return false;
      }

      Collection<Role> roles = RoleDAO.getInstance(Application.dbName).find(new BasicDBObject("group_id", group.getId()).append("user_ids", Pattern.compile(currentUser.getId())));

      for (Role role : roles) {
        for (Permission per : role.getPermissions()) {
          Feature f = per.getFeature();
          if (f.getName().equals("Virtual Machine")) {
            Operation o = per.getOpertion();
            if (o.getName().equals(operation)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    return true;
  }

  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage Test VM")
  public static Result normalVMView(String groupId) throws Exception {
    Group group = GroupDAO.getInstance(Application.dbName).findOne(groupId);

    if (! hasPermission(group, "Manage Test VM")) return forbidden(views.html.forbidden.render());

    List<VMModel> list = VMHelper.getVMsByGroupID(groupId, new BasicDBObject("system", false));
    Html html = vmbody.render(checkCurrentSystem(), group, list, true);
    return ok(index.render(html));
  }

  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage Test VM")
  public static Result createrNormalVM(String groupId, final boolean gui) throws Exception {
    final Group company = GroupDAO.getInstance(Application.dbName).findOne(groupId);
    
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          VMModel jenkins = VMHelper.getVMsByGroupID(company.getId(), new BasicDBObject("jenkins", true)).get(0);
          JenkinsMaster jenkinsMaster = new JenkinsMaster(jenkins.getPublicIP(), "http", 8080);
          if (jenkinsMaster.isReady()) {
            VMModel vm =  gui  ? VMCreator.createNormalGuiVM(company): VMCreator.createNormalNonGuiVM(company);
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    thread.start();
    
    return ok();
  }
  
  @With(VMWizardIterceptor.class)
  @Authorization(feature = "Virtual Machine", operation = "Manage System VM")
  public static Result saveOffering(String groupId) throws Exception {
    DynamicForm form = Form.form().bindFromRequest();
    if (checkCurrentSystem()) {
      List<OfferingModel> offerings = OfferingHelper.getOfferings();
      List<OfferingModel> disabledOfferings = new ArrayList<OfferingModel>();
      boolean blank = true;
      for (OfferingModel offering : offerings) {
        if (form.get("offering-" + offering.getId()) != null) {
          blank = false;
          offering.put("disabled", false);
          //OfferingHelper.updateOffering(offering);
        } else {
          offering.put("disabled",true);
          disabledOfferings.add(offering);
        }
      }
      if (!blank) {
        for (OfferingModel offering : offerings) {
          OfferingHelper.updateOffering(offering);
        }
        //check if has only one offering
        List<OfferingModel> enabledOfferings = OfferingHelper.getEnableOfferings();
        if (enabledOfferings.size() == 1) {
          List<DefaultOfferingModel> groups = OfferingHelper.getInvalidDefaultOffering();
          for (DefaultOfferingModel groupOffering : groups) {
            groupOffering.put("offering_id", enabledOfferings.get(0).getId());
            OfferingHelper.updateDefaultOfferingOfGroup(groupOffering);
          }
        }
      }
    } else {
      String offeringId = form.get("offering");
      OfferingHelper.removeDefaultOfferingOfGroup(groupId);
      OfferingHelper.addDefaultOfferingForGroup(groupId, offeringId);
    }
    return ok();
  }

  public static boolean checkCurrentSystem() throws UserManagementException {
    return Organization.isSystem(UserDAO.getInstance(Application.dbName).findOne(session("user_id")));
  }
  
  //get template render form input apikey
  
  @WithSystem
  public static Result apivmView() throws Exception {
    String service = request().getQueryString("service");  
    Html html =null;
    if ("apiCloudStack".equalsIgnoreCase(service)) {
      html=cloudstacktemplate.render();
    } else if ("apiAzure".equalsIgnoreCase(service)) {
      html=azuretemplate.render();
    } else if ("apiAmazon".equalsIgnoreCase(service)) {
      html=amazontemplate.render();
    } else {
      html=Html.apply( "<h1>Default template</h1>");
    } 
    return ok(html);
  }
  
 
  
  
}
