/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.funds.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.thinkgem.jeesite.common.config.Global;
import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.web.BaseController;
import com.thinkgem.jeesite.common.utils.StringUtils;
import com.thinkgem.jeesite.modules.funds.entity.Invoice;
import com.thinkgem.jeesite.modules.funds.service.InvoiceService;

/**
 * 发票信息Controller
 * @author huangsc
 * @version 2015-06-11
 */
@Controller
@RequestMapping(value = "${adminPath}/funds/invoice")
public class InvoiceController extends BaseController {

	@Autowired
	private InvoiceService invoiceService;
	
	@ModelAttribute
	public Invoice get(@RequestParam(required=false) String id) {
		Invoice entity = null;
		if (StringUtils.isNotBlank(id)){
			entity = invoiceService.get(id);
		}
		if (entity == null){
			entity = new Invoice();
		}
		return entity;
	}
	
	@RequiresPermissions("funds:invoice:view")
	@RequestMapping(value = {"list", ""})
	public String list(Invoice invoice, HttpServletRequest request, HttpServletResponse response, Model model) {
		Page<Invoice> page = invoiceService.findPage(new Page<Invoice>(request, response), invoice); 
		model.addAttribute("page", page);
		return "modules/funds/invoiceList";
	}

	@RequiresPermissions("funds:invoice:view")
	@RequestMapping(value = "form")
	public String form(Invoice invoice, Model model) {
		model.addAttribute("invoice", invoice);
		return "modules/funds/invoiceForm";
	}

	@RequiresPermissions("funds:invoice:edit")
	@RequestMapping(value = "save")
	public String save(Invoice invoice, Model model, RedirectAttributes redirectAttributes) {
		if (!beanValidator(model, invoice)){
			return form(invoice, model);
		}
		invoiceService.save(invoice);
		addMessage(redirectAttributes, "保存发票信息成功");
		return "redirect:"+Global.getAdminPath()+"/funds/tradingAccounts/?repage";
	}
	
	@RequiresPermissions("funds:invoice:edit")
	@RequestMapping(value = "delete")
	public String delete(Invoice invoice, RedirectAttributes redirectAttributes) {
		invoiceService.delete(invoice);
		addMessage(redirectAttributes, "删除发票信息成功");
		return "redirect:"+Global.getAdminPath()+"/funds/invoice/?repage";
	}

}