package org.ats.services.organization.event;

import java.util.ArrayList;
import java.util.List;

import org.ats.common.MapBuilder;
import org.ats.common.PageList;
import org.ats.services.event.Event;
import org.ats.services.organization.UserService;
import org.ats.services.organization.entity.Tenant;
import org.ats.services.organization.entity.User;
import org.ats.services.organization.entity.fatory.ReferenceFactory;
import org.ats.services.organization.entity.reference.TenantReference;

import akka.actor.UntypedActor;

import com.google.inject.Inject;

public class DeleteTenantActor extends UntypedActor{

  @Inject
  private UserService userService;
  
  @Inject
  private ReferenceFactory<TenantReference> tenantRefFactory;
  
  @Override
  public void onReceive(Object message) throws Exception {

    if (message instanceof Event) {
      
      Event event = (Event) message;
      if ("delete-tenant".equals(event.getName())) {
        
        Tenant tenant = (Tenant) event.getSource();
        TenantReference ref = tenantRefFactory.create(tenant.getId());
        
        process(ref);
      } else if ("delete-tenant-ref".equals(event.getName())) {
        
        TenantReference ref = (TenantReference) event.getSource();
        process(ref);
        
      } else unhandled(message);
      
    }
  }

  private void process(TenantReference ref) {
    
    PageList<User> listUser = userService.findUserInTenant(ref);
    
    listUser.setSortable(new MapBuilder<String, Boolean>("created_date", true).build());
    
    List<User> holder = new ArrayList<User>();
    while(listUser.hasNext()) {
      
      for (User user : listUser.next()) {
        holder.add(user);
        
      }
    }
    for (User user : holder) {
      
      userService.delete(user);
    }
    
    if (!"deadLetters".equals(getSender().path().name())) {
      getSender().tell(ref, getSelf());
    }
    
  }
}
