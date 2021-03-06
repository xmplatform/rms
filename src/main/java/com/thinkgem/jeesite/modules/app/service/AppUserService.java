/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.app.service;

import java.util.List;

import com.thinkgem.jeesite.modules.sys.dao.UserDao;
import com.thinkgem.jeesite.modules.sys.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.service.CrudService;
import com.thinkgem.jeesite.modules.app.dao.AppUserDao;
import com.thinkgem.jeesite.modules.app.entity.AppUser;

/**
 * APP用户Service
 * @author mabindong
 * @version 2015-11-24
 */
@Service
@Transactional(readOnly = true)
public class AppUserService extends CrudService<AppUserDao, AppUser> {
	

	@Autowired
	private AppUserDao appUserDao;

    @Autowired
    private UserDao userDao;

	public AppUser get(String id) {
		return super.get(id);
	}
	
	public List<AppUser> findList(AppUser appUser) {
		return super.findList(appUser);
	}
	
	public Page<AppUser> findPage(Page<AppUser> page, AppUser appUser) {
		return super.findPage(page, appUser);
	}
	
	@Transactional(readOnly = false)
	public void save(AppUser appUser) {
		super.save(appUser);
	}
	
	@Transactional(readOnly = false)
	public void delete(AppUser appUser) {
		super.delete(appUser);
	}
	
	public AppUser getByPhone(AppUser appUser) {
		return appUserDao.getByPhone(appUser);
	}

    public User getServiceUserByPhone(AppUser appUser){
        String sysUserId = appUserDao.getServiceUserByPhone(appUser);
        if(sysUserId == null){
            return null;
        }
        User  user = userDao.get(sysUserId);
        return user;
    }

    public User getServiceUserByContractId(String contract_id) {
        String sysUserId = appUserDao.getServiceUserByContractId(contract_id);
        if(sysUserId == null){
            return null;
        }
        User  user = userDao.get(sysUserId);
        return user;
    }
}