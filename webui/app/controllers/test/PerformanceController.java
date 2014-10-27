/**
 * 
 */
package controllers.test;

import helpertest.JMeterScriptHelper;
import helpertest.JenkinsJobHelper;
import helpertest.TestHelper;
import helpervm.VMHelper;
import interceptor.AuthenticationInterceptor;
import interceptor.Authorization;
import interceptor.WizardInterceptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import models.test.JenkinsJobModel;
import models.test.JenkinsJobStatus;
import models.test.TestProjectModel;
import models.test.TestProjectModel.TestProjectType;
import models.vm.VMModel;

import org.ats.common.StringUtil;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;
import org.ats.component.usersmgt.user.User;
import org.ats.component.usersmgt.user.UserDAO;
import org.ats.gitlab.GitlabAPI;
import org.ats.jmeter.JMeterFactory;
import org.ats.jmeter.JMeterParser;
import org.ats.jmeter.models.JMeterScript;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabProject;

import play.api.templates.Html;
import play.data.DynamicForm;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;
import scala.collection.mutable.StringBuilder;
import views.html.test.index;
import views.html.test.snapshot;

import com.mongodb.BasicDBObject;

import controllers.Application;
import controllers.vm.VMCreator;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Oct 20, 2014
 */

@With({WizardInterceptor.class, AuthenticationInterceptor.class})
@Authorization(feature = "Performance", operation = "")
public class PerformanceController extends TestController {

  public static Result index() {
    return ok(index.render("performance"));
  }
  
  public static Html getSnapshotHtml(TestProjectModel project) {
    StringBuilder sb = new StringBuilder();
    List<JMeterScript> scripts = JMeterScriptHelper.getJMeterScript(project.getId());
    for(JMeterScript script : scripts) {
      sb.append(snapshot.render(script));
    }
    return new Html(sb);
  }
  
  public static Result runProject(String projectId) throws Exception {
    TestProjectModel project = TestHelper.getProjectById(TestProjectType.performance, projectId);
    JMeterScript snapshot = JMeterScriptHelper.getLastestCommit(project.getId());    
    runSnapshot(project, snapshot);
    return redirect(routes.PerformanceController.index());
  }
  
  public static Result runSnapshot(String snapshotId) throws Exception {
    JMeterScript snapshot = JMeterScriptHelper.getJMeterScriptById(snapshotId);
    TestProjectModel project = TestHelper.getProjectById(TestProjectType.performance, snapshot.getString("project_id"));
    runSnapshot(project, snapshot);
    return redirect(routes.PerformanceController.index());
  }
  
  public static void runSnapshot(final TestProjectModel project, final JMeterScript snapshot) throws Exception {

    final VMModel jenkins = VMHelper.getVMByID(project.getString("jenkins_id"));
    
    final Group company = getCompany();
    
    List<VMModel> list = VMHelper.getReadyVMs(company.getId());
    
    //remove last build
    JenkinsJobHelper.deleteBuildOfSnapshot(snapshot.getString("_id"));
    
    if (list.isEmpty()) {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            VMModel vm = VMCreator.createNormalNonGuiVM(company);
            JenkinsJobModel job = new JenkinsJobModel(JenkinsJobHelper.getCurrentBuildIndex(project.getId()) + 1, project.getId(), snapshot.getString("_id"), vm.getId(), jenkins.getId(), TestProjectType.performance);
            JenkinsJobHelper.createJenkinsJob(job);
            
            project.put("status", job.getStatus().toString());
            TestHelper.updateProject(project);
            
            snapshot.put("status", job.getStatus().toString());
            JMeterScriptHelper.updateJMeterScript(snapshot);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      thread.start();
    } else {
      VMModel vm = list.get(0);
      JenkinsJobModel job = new JenkinsJobModel(JenkinsJobHelper.getCurrentBuildIndex(project.getId()) + 1, project.getId(), snapshot.getString("_id"), vm.getId(), jenkins.getId(), TestProjectType.performance);
      JenkinsJobHelper.createJenkinsJob(job);

      project.put("status", job.getStatus().toString());
      TestHelper.updateProject(project);
      
      snapshot.put("status", job.getStatus().toString());
      JMeterScriptHelper.updateJMeterScript(snapshot);
    }
  }
  
  public static Result deleteProject(String projectId) throws IOException {
    TestProjectModel project = TestHelper.getProjectById(TestProjectType.performance, projectId);
    
    TestHelper.removeProject(project);
    JenkinsJobHelper.deleteBuildOfProject(project.getId());
    JMeterScriptHelper.deleteScriptOfProject(project.getId());
    
    
    VMModel jenkins = VMHelper.getVMByID(project.getString("jenkins_id"));
    String gitlabToken = VMHelper.getSystemProperty("gitlab-api-token");
    GitlabAPI gitlabAPI = new GitlabAPI("http://" + jenkins.getPublicIP(), gitlabToken);
    gitlabAPI.deleteProject(project.getGitlabProjectId());
    
    return redirect(routes.PerformanceController.index());
  }

  public static Result createProjectByUpload(boolean run) {
    try {
      MultipartFormData body = request().body().asMultipartFormData();
      DynamicForm form = DynamicForm.form().bindFromRequest();

      String testName = form.get("name");

      FilePart uploaded = body.getFile("uploaded");
      if (uploaded != null) {
        FileInputStream fis = new FileInputStream(uploaded.getFile());
        String content = StringUtil.readStream(fis);

        JMeterFactory factory = new JMeterFactory();
        JMeterParser parser = factory.createJMeterParser(content);
        JMeterScript script = parser.parse();

        User currentUser = UserDAO.getInstance(Application.dbName).findOne(session("user_id"));
        Group currentGroup = GroupDAO.getInstance(Application.dbName).findOne(session("group_id"));

        Group company = null;
        if (currentGroup.getInt("level") > 1) {
          LinkedList<Group> parents = GroupDAO.getInstance(Application.dbName).buildParentTree(currentGroup);
          company = parents.get(0);
        } else if (currentGroup.getInt("level") == 1) {
          company = currentGroup;
        }

        VMModel jenkins = VMHelper.getVMs(new BasicDBObject("group_id", company.getId()).append("jenkins", true)).iterator().next();

        String gitlabToken = VMHelper.getSystemProperty("gitlab-api-token");

        GitlabAPI gitlabAPI = new GitlabAPI("http://" + jenkins.getPublicIP(), gitlabToken);

        GitlabProject gitProject = factory.createProject(gitlabAPI, company.getString("name"), testName);

        gitlabAPI.createFile(gitProject, "src/test/jmeter/script.jmx", "master", script.toString(), "Snapshot 1");

        GitlabCommit commit = gitlabAPI.getCommits(gitProject, "master").get(0);
        
        String gitSshUrl = gitProject.getSshUrl().replace("git.sme.org", jenkins.getPublicIP());

        String gitHttpUrl = gitProject.getHttpUrl().replace("git.sme.org", jenkins.getPublicIP());
        
        TestProjectModel project = new TestProjectModel(
            TestHelper.getCurrentProjectIndex(TestProjectType.performance) + 1,
            gitProject.getId(),
            testName, 
            currentGroup.getId(), 
            currentUser.getId(), 
            TestProjectType.performance, 
            gitSshUrl,
            gitHttpUrl,
            uploaded.getFilename(), content.getBytes("UTF-8"));

//        project.put("jmeter", script);
        project.put("status", run ? JenkinsJobStatus.Initializing.toString() : JenkinsJobStatus.Ready.toString());
        project.put("jenkins_id", jenkins.getId());
        
        //Additional for JMeterScript
        script.put("_id", commit.getId());
        script.put("project_id", project.getId());
        script.put("commit", commit.getTitle());
        script.put("index", 1);
        script.put("status", run ? JenkinsJobStatus.Initializing.toString() : JenkinsJobStatus.Ready.toString());
        
        JMeterScriptHelper.createScript(script);
        
        TestHelper.createProject(TestProjectType.performance, project);
        
        if (run) runSnapshot(project, script);
      }
      return redirect(controllers.test.routes.PerformanceController.index());
    } catch (Exception e) {
      e.printStackTrace();
      return forbidden(views.html.forbidden.render());
    }
  }
}
