package nl.tno.esdl.esdldrive;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.DrivePermission;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.edr.PublicUserInfo;

public class AccessControllerTest {

	@Test
	public void test() {
		UserInfo userInfo = new PublicUserInfo();
		userInfo.getReadOnlyFolders().add("/Projects/Shared Models");
		userInfo.getReadOnlyFolders().add("/Projects/development/read-only");
		List<String> write_access = Arrays.asList("/Projects/development","/Projects/Energie en Ruimte","/Projects/ESSIM-KPI test","/Projects/Go-E","/Organizations/TNO/MCS","/Projects/Mondaine","/Projects/Pocityf","/Public","/edr/Public","/Projects/SeedERP - Energy modelling","/Projects/Shared Models","/Test group","/Projects/WarmingUp");
		userInfo.getReadWriteFolders().addAll(write_access);
		System.out.println(userInfo);
		
		DrivePermission permission = AccessController.getPermission("/Projects/Shared Models/test.esdl", userInfo);
		System.out.println(permission);
		Assert.assertFalse(permission.isWritable());
		Assert.assertTrue(permission.isAccessible());
		
		Assert.assertFalse(AccessController.canAccess("/tmp", userInfo));
		Assert.assertTrue(AccessController.canAccess("/edr", userInfo));
		Assert.assertFalse(AccessController.canAccess("/Users/edwin", userInfo));
		Assert.assertFalse(AccessController.canAccess("/Users/publicUser", userInfo));
		
		DrivePermission p1 = AccessController.getPermission("/Projects/development", userInfo);
		System.out.println(p1);
		p1 = AccessController.getPermission("/Projects/development/read-only/test.esdl", userInfo);
		System.out.println(p1);
		p1 = AccessController.getPermission("/Projects/development/writable/test.esdl", userInfo);
		System.out.println(p1);
		userInfo.getReadWriteFolders().add("/Projects/development/read-only/test.esdl");
		p1 = AccessController.getPermission("/Projects/development/read-only/test.esdl", userInfo);
		System.out.println(userInfo);
		System.out.println(p1);
		
		p1 = AccessController.getPermission("/Public/test", userInfo);
		System.out.println(p1);
		
	}

}
