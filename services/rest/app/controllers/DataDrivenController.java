/**
 * 
 */
package controllers;

import org.ats.services.OrganizationContext;
import org.ats.services.datadriven.DataDriven;
import org.ats.services.datadriven.DataDrivenFactory;
import org.ats.services.datadriven.DataDrivenReference;
import org.ats.services.datadriven.DataDrivenService;
import org.ats.services.keyword.Case;
import org.ats.services.keyword.CaseService;
import org.ats.services.organization.SpaceService;
import org.ats.services.organization.acl.Authenticated;
import org.ats.services.organization.entity.fatory.ReferenceFactory;
import org.ats.services.organization.entity.reference.SpaceReference;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import actions.CorsComposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 *
 * May 21, 2015
 */
@CorsComposition.Cors
@Authenticated
public class DataDrivenController extends Controller {
  
  @Inject
  DataDrivenService service;
  
  @Inject
  OrganizationContext context;
  
  @Inject
  SpaceService spaceService;
  
  @Inject
  ReferenceFactory<SpaceReference> spaceRefFactory;
  
  @Inject
  DataDrivenService dataDrivenService;
  
  @Inject
  DataDrivenFactory dataDrivenFactory;
  
  @Inject
  ReferenceFactory<DataDrivenReference> dataRefFactory;
  
  @Inject
  CaseService caseService;
  
  public Result create() {
    JsonNode json = request().body().asJson();
    String name = json.get("name").asText();
    String caseId = json.get("caseId").asText();
    JsonNode dataset = json.get("dataset");
    
    Case caze = caseService.get(caseId);
    DataDriven driven = dataDrivenFactory.create(name, dataset.toString());
    
    dataDrivenService.create(driven);
    String drivenId = driven.getId();
    caze.setDataDriven(dataRefFactory.create(drivenId));
    caseService.update(caze);
    return ok(Json.parse(driven.toString()));
  }
  
  public Result get(String id) {
    
    DataDriven driven = service.get(id);
    ObjectNode json = Json.newObject();
    
    json.put("data", Json.parse(driven.toString()));
    
    return ok(json);
  }
  
  public Result delete(String id) {
    
    service.delete(id);
    return ok();
  }
  
  public Result update() {
    
    JsonNode json = request().body().asJson();
    String id = json.get("id").asText();
    JsonNode dataset = json.get("dataset");
    DataDriven driven = dataDrivenService.get(id);
    String name = json.get("name").asText();
    
    if (dataset.toString().equals(driven.getDataSource()) && name.equals(driven.getName())) {
      return status(304);
    }
    driven.put("name", name);
    driven.setDataSource(dataset.toString());
    dataDrivenService.update(driven);
    
    return status(200, Json.parse(driven.toString()));
  }
}
