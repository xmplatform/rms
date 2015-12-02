package com.thinkgem.jeesite.modules.app.web;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.druid.util.StringUtils;
import com.thinkgem.jeesite.modules.app.entity.AppToken;
import com.thinkgem.jeesite.modules.app.entity.AppUser;
import com.thinkgem.jeesite.modules.app.entity.ResponseData;
import com.thinkgem.jeesite.modules.app.entity.TAppCheckCode;
import com.thinkgem.jeesite.modules.app.service.AppTokenService;
import com.thinkgem.jeesite.modules.app.service.AppUserService;
import com.thinkgem.jeesite.modules.app.service.TAppCheckCodeService;
import com.thinkgem.jeesite.modules.app.util.JsonUtil;
import com.thinkgem.jeesite.modules.app.util.RandomStrUtil;
import com.thinkgem.jeesite.modules.common.service.SmsService;

@Controller
@RequestMapping("system")
public class SystemController {
	Logger log = LoggerFactory.getLogger(SystemController.class);
			
	@Autowired
	private TAppCheckCodeService tAppCheckCodeService;
	
	@Autowired
	private AppUserService appUserService;
	
	@Autowired
	private AppTokenService appTokenService;
	@Autowired
	private SmsService smsService;
	
	@RequestMapping(value="checkToken")
	@ResponseBody
	public String checkToken(HttpServletRequest request, HttpServletResponse response, Model model) {
		String res;
		
		String token = (String) request.getHeader("token");
		log.debug("in checkToken. token:" + token );
		//验证token是否存在
		AppToken appToken = new AppToken();
		appToken.setToken(token);
		appToken.setExprie(new Date());
		appToken = appTokenService.findByTokenAndExpire(appToken);
		if(appToken!=null)
			res = appToken.getPhone();
		else
			res = null;
		log.debug("in chekcToken. return phone:" + res);
		return res;		
	}
	
	
	@RequestMapping(value="register")
	@ResponseBody
	public ResponseData register(HttpServletRequest request, HttpServletResponse response, Model model) {
		
		ResponseData data = new ResponseData(); 
		String mobile = (String) request.getParameter("mobile");
		String code = (String) request.getParameter("code");
		String password =  (String) request.getParameter("password");
		
		TAppCheckCode tAppCheckCode = new TAppCheckCode();
		tAppCheckCode.setPhone(mobile);
		tAppCheckCode.setCode(code);
		tAppCheckCode.setExprie(new Date());
		boolean isValidCheckCode = tAppCheckCodeService.verifyCheckCode(tAppCheckCode);
		if(isValidCheckCode){
			//exist?
			AppUser appUser = new AppUser();
			appUser.setPhone(mobile);
			appUser = appUserService.getByPhone(appUser);
			if(appUser!=null){
				
				data.setCode("406");
				data.setMsg("用户不存在");
				data.setData("");
				
			}else{			
				//create app user
				appUser = new AppUser();
				appUser.setPhone(mobile);
				appUser.setPassword(password);
				appUserService.save(appUser);
				//generate user token
				AppToken appToken = new AppToken();
				appToken.setPhone(appUser.getPhone());
				appToken.setToken(RandomStrUtil.generateCode(false, 32));
				appToken.setExprie(caculateExpireTime(2592000));
				appTokenService.save(appToken);
				
				
				data.setCode("200");
				data.setMsg("注册成功");
				Map object = new HashMap();
				object.put("user_id", appUser.getPhone());
				object.put("token", appToken.getToken());
				object.put("expire", appToken.getExprie().getTime());
				data.setData(object);
			}
		}else{
			data.setCode("402");
			data.setMsg("无效验证码");
			data.setData("");
		}
		return data;
	}
	
	
	
	@RequestMapping(value="check_code")
	@ResponseBody
	public ResponseData check_code(HttpServletRequest request, HttpServletResponse response, Model model) {
		try{
			log.debug("parasMap:" + request.getParameterMap());
			String mobile = (String) request.getParameter("mobile");
			if(StringUtils.isEmpty(mobile)){
				ResponseData data = new ResponseData();
				data.setCode("101");
				data.setMsg("必填参数不能为空 ");	
				return data;
			}
		
			TAppCheckCode tAppCheckCode = new TAppCheckCode();
			tAppCheckCode.setPhone(mobile);
			tAppCheckCode.setCode(RandomStrUtil.generateCode(true, 6));
			Date expiredate = caculateExpireTime(60);
			tAppCheckCode.setExprie(expiredate);
			tAppCheckCodeService.merge(tAppCheckCode);
			smsService.sendSms(mobile, "验证码"+tAppCheckCode.getCode()+",您正在使用唐巢APP,验证码很重要，请勿谢泄露.");
			ResponseData data = new ResponseData(); 
			data.setCode("200");
			data.setMsg("成功获取验证码");
			data.setData(tAppCheckCode.getCode());
			return data;
		}catch(Exception e){
			log.error("in check_code:"+ e.getMessage());
			ResponseData data = new ResponseData(); 
			data.setCode("411");
			data.setMsg("获取验证码异常");
			return data;
		}
	}
	
	@RequestMapping(value="login/pwd")
	@ResponseBody
	public ResponseData loginWithPwd(HttpServletRequest request, HttpServletResponse response, Model model) {
		
		ResponseData data = new ResponseData(); 
		String phone = (String) request.getParameter("mobile");
		String password = (String) request.getParameter("password");
		AppUser appUser = new AppUser();
		appUser.setPhone(phone);
		appUser = appUserService.getByPhone(appUser);
		if(appUser==null){
			data.setCode("403");
			data.setMsg("用户不存在");
		}else if(!password.equals(appUser.getPassword())){
			data.setCode("404");
			data.setMsg("用户名/密码有误");			
		}else{
			//generate new user token
			AppToken appToken = new AppToken();
			appToken.setPhone(appUser.getPhone());
			appToken.setToken(RandomStrUtil.generateCode(false, 32));
			appToken.setExprie(caculateExpireTime(2592000));
			appTokenService.merge(appToken);
			Map object = new HashMap();
			object.put("user_id", appUser.getPhone());
			object.put("token", appToken.getToken());
			object.put("expire", appToken.getExprie().getTime());
			data.setData(object);
			data.setCode("200");
			data.setMsg("登陆成功");
		}
		return data;
	}
	
	@RequestMapping(value="login/code")
	@ResponseBody
	public ResponseData loginWithCode(HttpServletRequest request, HttpServletResponse response, Model model) {
		ResponseData data = new ResponseData(); 
		
		String mobile = (String) request.getParameter("mobile");
		String code = (String)request.getParameter("code");;
		
		TAppCheckCode tAppCheckCode = new TAppCheckCode();
		tAppCheckCode.setPhone(mobile);
		tAppCheckCode.setCode(code);
		tAppCheckCode.setExprie(new Date());
		boolean isValidCheckCode = tAppCheckCodeService.verifyCheckCode(tAppCheckCode);
		
		if(isValidCheckCode){
			
			//generate user token
			AppToken appToken = new AppToken();
			appToken.setPhone(mobile);
			appToken.setToken(RandomStrUtil.generateCode(false, 32));
			appToken.setExprie(caculateExpireTime(2592000));
			appTokenService.merge(appToken);
			
			data.setCode("200");
			data.setMsg("登陆成功");
			Map object = new HashMap();
			object.put("user_id", mobile);
			object.put("token", appToken.getToken());
			object.put("expire", appToken.getExprie().getTime());
			data.setData(object);
		}else{
			data.setCode("402");
			data.setMsg("无效验证码");
			data.setData("");
		}
		return data;
	}
	
	@RequestMapping(value="self/pwd")
	@ResponseBody
	public ResponseData changePwd(HttpServletRequest request, HttpServletResponse response, Model model) {
		ResponseData data = new ResponseData(); 
		log.debug(request.getParameterMap().toString());
		String mobile = (String) request.getParameter("mobile");
		String oldPassword = (String)request.getParameter("old_password");
		String newPassword = (String)request.getParameter("new_password");	
		if(oldPassword == null || newPassword== null){
			data.setCode("101");
			data.setMsg("必填参数不能为空 ");	
			return data;
		}
		
		AppUser appUser = new AppUser();
		appUser.setPhone(mobile);
		appUser.setPassword(oldPassword);
		appUser = appUserService.getByPhone(appUser);
		if(!oldPassword.equals(appUser.getPassword())){
			data.setCode("102");
			data.setMsg("用户原密码有误");			
		}else{
			appUser.setPassword(newPassword);
			appUserService.save(appUser);
			data.setCode("100");
			data.setMsg("修改密码成功");
		}
		return data;
	}
 
	@RequestMapping(value="pwd/reset")
	@ResponseBody
	public ResponseData resetPwd(HttpServletRequest request, HttpServletResponse response, Model model) {
		ResponseData data = new ResponseData(); 
		log.debug(request.getParameterMap().toString());
		String mobile = (String) request.getParameter("mobile");
		String code = (String)request.getParameter("code");
		String password = (String)request.getParameter("password");	
		if(mobile == null || code== null || password == null){
			data.setCode("101");
			data.setMsg("必填参数不能为空 ");	
			return data;
		}
		
		TAppCheckCode tAppCheckCode = new TAppCheckCode();
		tAppCheckCode.setPhone(mobile);
		tAppCheckCode.setCode(code);
		tAppCheckCode.setExprie(new Date());
		boolean isValidCheckCode = tAppCheckCodeService.verifyCheckCode(tAppCheckCode);
		
		if(isValidCheckCode){
			//cheange pwd
			AppUser appUser = new AppUser();
			appUser.setPhone(mobile);
			appUser = appUserService.getByPhone(appUser);
			appUser.setPassword(password);
			appUser.setUpdateDate(new Date());
			appUserService.save(appUser);
			//generate user token
			AppToken appToken = new AppToken();
			appToken.setPhone(mobile);
			appToken.setToken(RandomStrUtil.generateCode(false, 32));
			appToken.setExprie(caculateExpireTime(2592000));
			appTokenService.merge(appToken);
			
			data.setCode("200");
			data.setMsg("重置密码成功");
			Map object = new HashMap();
			object.put("user_id", appUser.getPhone());
			object.put("token", appToken.getToken());
			object.put("expire", appToken.getExprie().getTime());
			data.setData(object);
		}else{
			data.setCode("402");
			data.setMsg("无效验证码");
			data.setData("");
		}
		return data;
	}
	
	
	/**
	 * 计算过期时间，单位秒
	 * @param duration
	 * @return
	 */
	private Date caculateExpireTime(int duration){
			GregorianCalendar cal = new GregorianCalendar();  
	       cal.setTime(new Date());  
	       cal.add(13, duration);
	       return cal.getTime(); 
	}

}
