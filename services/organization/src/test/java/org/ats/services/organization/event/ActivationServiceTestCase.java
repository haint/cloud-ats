/**
 * 
 */
package org.ats.services.organization.event;

import java.util.logging.Logger;

import org.ats.services.organization.ActivationService;
import org.ats.services.organization.RoleService;
import org.ats.services.organization.SpaceService;
import org.ats.services.organization.TenantService;
import org.ats.services.organization.UserService;
import org.ats.services.organization.entity.Tenant;
import org.ats.services.organization.entity.reference.TenantReference;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import akka.actor.UntypedActor;

import com.google.inject.Inject;


/**
 * @author TrinhTV3
 *
 * Email: TrinhTV3@fsoft.com.vn
 */
public class ActivationServiceTestCase extends EventTestCase {

  @Override @BeforeMethod
  public void init() throws Exception {
    super.init();
    
  }
  
  @Test
  public void testActivationUser() {
    Assert.assertEquals(activationService.countInActiveUser(), 0);
    Assert.assertEquals(userService.count(), 1);
    
    activationService.inActiveUser("haint@cloud-ats.net");
    
    Assert.assertEquals(userService.count(), 0);
    Assert.assertEquals(activationService.countInActiveUser(), 1);
    
    activationService.activeUser("haint@cloud-ats.net");
    
    Assert.assertEquals(activationService.countInActiveUser(), 0);
    Assert.assertEquals(userService.count(), 1);
  }
  
  @Test
  public void testActivationTenant() {
    
    Tenant tenant = tenantService.get("Fsoft");
    
    Assert.assertEquals(activationService.countInActiveTenant(), 0);
    Assert.assertEquals(activationService.countRoleIntoInActiveTenant(), 0);
    Assert.assertEquals(activationService.countSpaceIntoInActiveTenant(), 0);
    Assert.assertEquals(activationService.countInActiveUser(), 0);
    Assert.assertEquals(tenantService.count(), 3);
    Assert.assertEquals(spaceService.findSpaceInTenant(tenantRefFactory.create(tenant.getId())).count(), 2);
    Assert.assertEquals(roleService.count(), 2);
    Assert.assertEquals(userService.count(), 1);
    eventService.setListener(ActivationTenantListener.class);
    
    activationService.inActiveTenant("Fsoft");
    
  }
  
  static class ActivationTenantListener extends UntypedActor {

    @Inject
    private ActivationService activationService;
    
    @Inject private TenantService tenantService;
    
    @Inject private UserService userService;
    
    @Inject private RoleService roleService;
    
    @Inject private SpaceService spaceService;
    
    @Inject private Logger logger;
    
    @Override
    public void onReceive(Object message) throws Exception {
      
      if (message instanceof TenantReference) {
        TenantReference ref = (TenantReference) message;
        
        logger.info("processed inactive tenant : "+ ref.toJSon());
        Assert.assertEquals(activationService.countInActiveTenant(), 1);
        
        Assert.assertEquals(activationService.countSpaceIntoInActiveTenant(), 2);
        
        Assert.assertEquals(activationService.countRoleIntoInActiveTenant(), 2);
        
        Assert.assertEquals(activationService.countInActiveUser(), 1);
        
        Assert.assertEquals(tenantService.count(), 2);
        
        Assert.assertEquals(userService.count(), 0);
        
        Assert.assertEquals(roleService.count(), 0);
        
        Assert.assertEquals(spaceService.count(), 0);
      }
    }
    
  }
  
}
