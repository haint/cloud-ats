/**
 * 
 */
package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.vm.VMModel;

import org.ats.cloudstack.CloudStackClient;
import org.ats.component.usersmgt.DataFactory;
import org.ats.component.usersmgt.UserManagementException;
import org.ats.component.usersmgt.group.Group;
import org.ats.component.usersmgt.group.GroupDAO;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * Sep 10, 2014
 */
public class VMHelper {

  /** .*/
  private static String databaseName = "cloud-ats-vm";
  
  /** .*/
  private static String systemColumn = "cloud-system";
  
  /** .*/
  private static String userColumn = "cloud-user";
  
  public static DB getDatabase() {
    return DataFactory.getDatabase(databaseName);
  }
  
  public static long systemCount() {
    DB vmDB = DataFactory.getDatabase(databaseName);
    return vmDB.getCollection(systemColumn).count();
  }
  
  public static boolean createSystemVM(VMModel... vms) {
    DB db = getDatabase();
    DBCollection col = db.getCollection(systemColumn);
    WriteResult result = col.insert(vms, WriteConcern.ACKNOWLEDGED);
    boolean exist = false;
    for (DBObject index : col.getIndexInfo()) {
      if ("System VM Index".equals(index.get("name"))) exist = true;
    }
    if (!exist) {
      col.ensureIndex(new BasicDBObject("name", "text"), "System VM Index");
      System.out.println("create System VM Index");
    }
    return result.getError() == null;
  }
  
  public static boolean createUserVm(VMModel... vms) {
    DB db = getDatabase();
    DBCollection col = db.getCollection(userColumn);
    WriteResult result = col.insert(vms, WriteConcern.ACKNOWLEDGED);
    return result.getError() == null;
  }
  
  public static List<VMModel> getVMsByGroupID(String groupId) {
    DB vmDB = getDatabase();
    try {
      Group group = GroupDAO.INSTANCE.findOne(groupId);
      boolean system = group.getBoolean("system");
      DBCursor cursor = vmDB.getCollection(system ? systemColumn : userColumn).find(new BasicDBObject("group_id", groupId));
      List<VMModel> vms = new ArrayList<VMModel>();
      while (cursor.hasNext()) {
        vms.add(new VMModel().from(cursor.next()));
      }
      return vms;
    } catch (UserManagementException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void setSystemProperties(Map<String, String> properties) {
    DB db = getDatabase();
    DBCollection col = db.getCollection(systemColumn);
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      col.insert(BasicDBObjectBuilder.start("_id", entry.getKey()).append("value", entry.getValue()).append("system_property", true).get());
    }
  }
  
  public static Map<String, String> getSystemProperties() {
    DB db = getDatabase();
    DBCollection col = db.getCollection(systemColumn);
    DBCursor cursor = col.find(new BasicDBObject("system_property", true));
    Map<String, String> map = new HashMap<String, String>();
    while (cursor.hasNext()) {
      DBObject current = cursor.next();
      map.put((String)current.get("_id"), (String)current.get("value"));
    }
    
    return map;
  }
  
  public static String getSystemProperty(String name) {
    DB db = getDatabase();
    DBCollection col = db.getCollection(systemColumn);
    DBObject obj = col.findOne(new BasicDBObject("_id", name));
    return (String) obj.get("value");
  }
  
  public static CloudStackClient getCloudStackClient() {
    Map<String, String> properties = getSystemProperties();
    String cloudstackApiUrl = properties.get("cloudstack-api-url");
    String cloudstackApiKey = properties.get("cloudstack-api-key");
    String cloudstackApiSecret = properties.get("cloudstack-api-secret");
    return new CloudStackClient(cloudstackApiUrl, cloudstackApiKey, cloudstackApiSecret);
  }
}
