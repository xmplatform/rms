/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.contract.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.service.CrudService;
import com.thinkgem.jeesite.common.utils.DateUtils;
import com.thinkgem.jeesite.common.utils.IdGen;
import com.thinkgem.jeesite.modules.common.dao.AttachmentDao;
import com.thinkgem.jeesite.modules.common.entity.Attachment;
import com.thinkgem.jeesite.modules.contract.dao.AuditDao;
import com.thinkgem.jeesite.modules.contract.dao.AuditHisDao;
import com.thinkgem.jeesite.modules.contract.dao.LeaseContractDao;
import com.thinkgem.jeesite.modules.contract.dao.LeaseContractDtlDao;
import com.thinkgem.jeesite.modules.contract.entity.Audit;
import com.thinkgem.jeesite.modules.contract.entity.AuditHis;
import com.thinkgem.jeesite.modules.contract.entity.FileType;
import com.thinkgem.jeesite.modules.contract.entity.LeaseContract;
import com.thinkgem.jeesite.modules.contract.entity.LeaseContractDtl;
import com.thinkgem.jeesite.modules.funds.dao.PaymentTransDao;
import com.thinkgem.jeesite.modules.funds.entity.PaymentTrans;
import com.thinkgem.jeesite.modules.sys.utils.UserUtils;

/**
 * 承租合同Service
 * 
 * @author huangsc
 * @version 2015-06-06
 */
@Service
@Transactional(readOnly = true)
public class LeaseContractService extends CrudService<LeaseContractDao, LeaseContract> {
    @Autowired
    private AttachmentDao attachmentDao;
    @Autowired
    private LeaseContractDtlDao leaseContractDtlDao;
    @Autowired
    private AuditDao auditDao;
    @Autowired
    private AuditHisDao auditHisDao;
    @Autowired
    private LeaseContractDao leaseContractDao;
    @Autowired
    private PaymentTransDao paymentTransDao;

    private static final String LEASE_CONTRACT_ROLE = "lease_contract_role";// 承租合同审批

    public LeaseContract get(String id) {
	LeaseContract leaseContract = super.get(id);
	if (null != leaseContract) {
	    LeaseContractDtl deaseContractDtl = new LeaseContractDtl();
	    deaseContractDtl.setLeaseContractId(id);
	    deaseContractDtl.setDelFlag("0");
	    List<LeaseContractDtl> leaseContractDtlList = leaseContractDtlDao.findList(deaseContractDtl);
	    leaseContract.setLeaseContractDtlList(leaseContractDtlList);
	}
	return leaseContract;
    }

    public List<LeaseContract> findList(LeaseContract leaseContract) {
	return super.findList(leaseContract);
    }

    public Page<LeaseContract> findLeaseContractList(Page<LeaseContract> page, LeaseContract leaseContract) {
	leaseContract.setPage(page);
	page.setList(dao.findLeaseContractList(leaseContract));
	return page;
    }

    public Page<LeaseContract> findPage(Page<LeaseContract> page, LeaseContract leaseContract) {
	return super.findPage(page, leaseContract);
    }

    @Transactional(readOnly = false)
    public void audit(AuditHis auditHis) {
	AuditHis saveAuditHis = new AuditHis();
	saveAuditHis.setId(IdGen.uuid());
	saveAuditHis.setObjectType("0");// 承租合同
	saveAuditHis.setObjectId(auditHis.getObjectId());
	saveAuditHis.setAuditMsg(auditHis.getAuditMsg());
	saveAuditHis.setAuditStatus(auditHis.getAuditStatus());// 1:通过 2:拒绝
	saveAuditHis.setCreateDate(new Date());
	saveAuditHis.setCreateBy(UserUtils.getUser());
	saveAuditHis.setUpdateDate(new Date());
	saveAuditHis.setUpdateBy(UserUtils.getUser());
	saveAuditHis.setAuditTime(new Date());
	saveAuditHis.setAuditUser(UserUtils.getUser().getId());
	saveAuditHis.setDelFlag("0");
	auditHisDao.insert(saveAuditHis);

	LeaseContract leaseContract = new LeaseContract();
	leaseContract = leaseContractDao.get(auditHis.getObjectId());
	leaseContract.setContractStatus(auditHis.getAuditStatus());
	leaseContract.setUpdateDate(new Date());
	leaseContract.setUpdateBy(UserUtils.getUser());
	leaseContractDao.update(leaseContract);

	// 审核通过，生成款项
	if ("1".equals(auditHis.getAuditStatus())) {
	    // 审核
	    Audit audit = new Audit();
	    audit.setObjectId(auditHis.getObjectId());
	    audit.setNextRole("");
	    audit.setUpdateDate(new Date());
	    audit.setUpdateBy(UserUtils.getUser());
	    auditDao.update(audit);

	    PaymentTrans delPaymentTrans = new PaymentTrans();
	    delPaymentTrans.setTransId(leaseContract.getId());
	    paymentTransDao.delete(delPaymentTrans);

	    // 1.押金款项
	    Double deposit = leaseContract.getDeposit();

	    PaymentTrans paymentTrans = new PaymentTrans();
	    paymentTrans.setId(IdGen.uuid());
	    paymentTrans.setTradeType("0");// 承租合同
	    paymentTrans.setPaymentType("4");// 房租押金
	    paymentTrans.setTransId(leaseContract.getId());
	    paymentTrans.setTradeDirection("0");// 出款
	    paymentTrans.setStartDate(leaseContract.getEffectiveDate());
	    paymentTrans.setExpiredDate(leaseContract.getExpiredDate());
	    paymentTrans.setTradeAmount(deposit);
	    paymentTrans.setTransAmount(0d);
	    paymentTrans.setLastAmount(deposit);
	    paymentTrans.setTransStatus("0");// 未支付
	    paymentTrans.setCreateDate(new Date());
	    paymentTrans.setCreateBy(UserUtils.getUser());
	    paymentTrans.setUpdateDate(new Date());
	    paymentTrans.setUpdateBy(UserUtils.getUser());
	    paymentTrans.setDelFlag("0");
	    if (0 != deposit)
		paymentTransDao.insert(paymentTrans);

	    // 2.房租款项
	    LeaseContractDtl leaseContractDtl = new LeaseContractDtl();
	    leaseContractDtl.setLeaseContractId(leaseContract.getId());
	    leaseContractDtl.setDelFlag("0");
	    List<LeaseContractDtl> list = leaseContractDtlDao.findList(leaseContractDtl);

	    int monthSpace = leaseContract.getMonthSpace();// 打款月份间隔
	    List<PaymentTrans> listPaymentTrans = new ArrayList<PaymentTrans>();

	    for (LeaseContractDtl tmpLeaseContractDtl : list) {
		// 计算开始日期与结束日期之间的月数
		double month = DateUtils.getMonthSpace(tmpLeaseContractDtl.getStartDate(), tmpLeaseContractDtl.getEndDate());
		month = (month == 0 ? month++ : month);
		for (int i = 1; i <= month; i++) {
		    paymentTrans = new PaymentTrans();
		    paymentTrans.setId(IdGen.uuid());
		    paymentTrans.setTradeType("0");// 承租合同
		    paymentTrans.setPaymentType("6");// 房租
		    paymentTrans.setTransId(leaseContract.getId());
		    paymentTrans.setTradeDirection("0");// 出款
		    Date startDate = i == 1 ? tmpLeaseContractDtl.getStartDate() : DateUtils.dateAddMonth2(tmpLeaseContractDtl.getStartDate(), i - 1);
		    paymentTrans.setStartDate(startDate);
		    Date endDate = i == month ? tmpLeaseContractDtl.getEndDate() : DateUtils.dateAddMonth2(tmpLeaseContractDtl.getStartDate(), i);
		    paymentTrans.setExpiredDate(endDate);
		    paymentTrans.setTradeAmount(tmpLeaseContractDtl.getDeposit());
		    paymentTrans.setTransAmount(0d);
		    paymentTrans.setLastAmount(tmpLeaseContractDtl.getDeposit());
		    paymentTrans.setTransStatus("0");// 未支付
		    paymentTrans.setCreateDate(new Date());
		    paymentTrans.setCreateBy(UserUtils.getUser());
		    paymentTrans.setUpdateDate(new Date());
		    paymentTrans.setUpdateBy(UserUtils.getUser());
		    paymentTrans.setDelFlag("0");
		    if (0 != tmpLeaseContractDtl.getDeposit()) {
			// paymentTransDao.insert(paymentTrans);
			listPaymentTrans.add(paymentTrans);
		    }
		}
	    }

	    int split = listPaymentTrans.size() / monthSpace;
	    for (int i = 0; i < split; i++) {
		int end = (i + 1) * monthSpace;
		List<PaymentTrans> tmpList = listPaymentTrans.subList(i * monthSpace, i != (split - 1) ? end : listPaymentTrans.size());
		PaymentTrans tmpPaymentTrans = tmpList.get(0);
		double tradeAmount = 0d;
		for (PaymentTrans p : tmpList) {
		    tradeAmount += p.getTradeAmount();
		}
		tmpPaymentTrans.setTradeAmount(tradeAmount);
		tmpPaymentTrans.setTransAmount(0d);
		tmpPaymentTrans.setLastAmount(tmpPaymentTrans.getTradeAmount());
		if (i != 0) {
		    tmpPaymentTrans.setStartDate(DateUtils.dateAddDay(tmpPaymentTrans.getStartDate(), 1));
		}
		Date endDate = DateUtils.dateAddMonth2(tmpPaymentTrans.getStartDate(), monthSpace);
		tmpPaymentTrans.setExpiredDate(endDate);
		paymentTransDao.insert(tmpPaymentTrans);
	    }
	    if (0 != listPaymentTrans.size() % monthSpace) {
		List<PaymentTrans> tmpList = listPaymentTrans.subList(listPaymentTrans.size() - listPaymentTrans.size() % monthSpace - 1, listPaymentTrans.size() - 1);
		PaymentTrans tmpPaymentTrans = tmpList.get(0);
		tmpPaymentTrans.setId(IdGen.uuid());
		double tradeAmount = 0d;
		for (PaymentTrans p : tmpList) {
		    tradeAmount += p.getTradeAmount();
		}
		tmpPaymentTrans.setTradeAmount(tradeAmount);
		tmpPaymentTrans.setTransAmount(0d);
		tmpPaymentTrans.setLastAmount(tmpPaymentTrans.getTradeAmount());
		tmpPaymentTrans.setStartDate(DateUtils.dateAddDay(tmpPaymentTrans.getStartDate(), 1));
		Date endDate = DateUtils.dateAddMonth2(tmpPaymentTrans.getStartDate(), monthSpace);
		tmpPaymentTrans.setExpiredDate(endDate);
		paymentTransDao.insert(tmpPaymentTrans);
	    }
	}
    }

    @Transactional(readOnly = false)
    public void save(LeaseContract leaseContract) {
	leaseContract.setContractStatus("0");// 待审核

	String id = super.saveAndReturnId(leaseContract);
	if (StringUtils.isEmpty(leaseContract.getId())) {

	    // 审核
	    Audit audit = new Audit();
	    audit.setId(IdGen.uuid());
	    audit.setObjectId(id);
	    audit.setObjectType("0");// 承租合同
	    audit.setNextRole(LEASE_CONTRACT_ROLE);
	    audit.setCreateDate(new Date());
	    audit.setCreateBy(UserUtils.getUser());
	    audit.setUpdateDate(new Date());
	    audit.setUpdateBy(UserUtils.getUser());
	    audit.setDelFlag("0");
	    auditDao.insert(audit);

	    // 保存附件
	    if (!StringUtils.isBlank(leaseContract.getLandlordId())) {
		Attachment attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(id);
		attachment.setAttachmentType(FileType.OWNER_ID.getValue());
		attachment.setAttachmentPath(leaseContract.getLandlordId());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    if (!StringUtils.isBlank(leaseContract.getProfile())) {
		Attachment attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(id);
		attachment.setAttachmentType(FileType.CERTIFICATE_FILE.getValue());
		attachment.setAttachmentPath(leaseContract.getProfile());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    if (!StringUtils.isBlank(leaseContract.getCertificate())) {
		Attachment attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(id);
		attachment.setAttachmentType(FileType.HOUSE_CERTIFICATE.getValue());
		attachment.setAttachmentPath(leaseContract.getCertificate());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    if (!StringUtils.isBlank(leaseContract.getRelocation())) {
		Attachment attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(id);
		attachment.setAttachmentType(FileType.HOUSE_AGREEMENT_CERTIFICATE.getValue());
		attachment.setAttachmentPath(leaseContract.getRelocation());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    // 承租合同明细
	    List<LeaseContractDtl> leaseContractDtlList = leaseContract.getLeaseContractDtlList();
	    if (null != leaseContractDtlList && leaseContractDtlList.size() > 0) {
		for (LeaseContractDtl leaseContractDtl : leaseContractDtlList) {
		    leaseContractDtl.setId(IdGen.uuid());
		    leaseContractDtl.setLeaseContractId(id);
		    leaseContractDtl.setCreateDate(new Date());
		    leaseContractDtl.setCreateBy(UserUtils.getUser());
		    leaseContractDtl.setUpdateDate(new Date());
		    leaseContractDtl.setUpdateBy(UserUtils.getUser());
		    leaseContractDtl.setDelFlag("0");
		    leaseContractDtlDao.insert(leaseContractDtl);
		}
	    }
	} else {
	    // 审核
	    Audit audit = new Audit();
	    audit.setObjectId(leaseContract.getId());
	    audit.setNextRole(LEASE_CONTRACT_ROLE);
	    audit.setUpdateDate(new Date());
	    audit.setUpdateBy(UserUtils.getUser());
	    auditDao.update(audit);

	    // 保存附件
	    Attachment attachment = new Attachment();
	    attachment.setLeaseContractId(leaseContract.getId());
	    attachmentDao.delete(attachment);

	    if (!StringUtils.isBlank(leaseContract.getLandlordId())) {
		attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(leaseContract.getId());
		attachment.setAttachmentType(FileType.OWNER_ID.getValue());
		attachment.setAttachmentPath(leaseContract.getLandlordId());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    if (!StringUtils.isBlank(leaseContract.getProfile())) {
		attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(leaseContract.getId());
		attachment.setAttachmentType(FileType.CERTIFICATE_FILE.getValue());
		attachment.setAttachmentPath(leaseContract.getProfile());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    if (!StringUtils.isBlank(leaseContract.getCertificate())) {
		attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(leaseContract.getId());
		attachment.setAttachmentType(FileType.HOUSE_CERTIFICATE.getValue());
		attachment.setAttachmentPath(leaseContract.getCertificate());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    if (!StringUtils.isBlank(leaseContract.getRelocation())) {
		attachment = new Attachment();
		attachment.setId(IdGen.uuid());
		attachment.setLeaseContractId(id);
		attachment.setAttachmentType(FileType.HOUSE_AGREEMENT_CERTIFICATE.getValue());
		attachment.setAttachmentPath(leaseContract.getRelocation());
		attachment.setCreateDate(new Date());
		attachment.setCreateBy(UserUtils.getUser());
		attachment.setUpdateDate(new Date());
		attachment.setUpdateBy(UserUtils.getUser());
		attachment.setDelFlag("0");
		attachmentDao.insert(attachment);
	    }

	    // 承租合同明细，由于删除手法的特殊性，此处需做修改.
	    List<LeaseContractDtl> leaseContractDtlList = leaseContract.getLeaseContractDtlList();// 总提交数据集
	    List<LeaseContractDtl> addLeaseContractDtlList = Lists.newArrayList();// 新增的list
	    List<LeaseContractDtl> delLeaseContractDtlList = Lists.newArrayList();// 删除的list
	    if (CollectionUtils.isNotEmpty(leaseContractDtlList)) {
		for (LeaseContractDtl lcd : leaseContractDtlList) {
		    if (StringUtils.isEmpty(lcd.getId()) && "0".equals(lcd.getDelFlag())) {
			addLeaseContractDtlList.add(lcd);
		    } else if (StringUtils.isNotEmpty(lcd.getId()) && "1".equals(lcd.getDelFlag())) {
			delLeaseContractDtlList.add(lcd);
		    } else {// 更新
			leaseContractDtlDao.update(lcd);
		    }
		}
	    }
	    if (CollectionUtils.isNotEmpty(delLeaseContractDtlList)) {
		for (LeaseContractDtl l : delLeaseContractDtlList) {
		    leaseContractDtlDao.delete(l);
		}
	    }
	    if (CollectionUtils.isNotEmpty(addLeaseContractDtlList)) {
		for (LeaseContractDtl l : addLeaseContractDtlList) {
		    l.setId(IdGen.uuid());
		    l.setLeaseContractId(leaseContract.getId());
		    l.setCreateDate(new Date());
		    l.setCreateBy(UserUtils.getUser());
		    l.setUpdateDate(new Date());
		    l.setUpdateBy(UserUtils.getUser());
		    l.setDelFlag("0");
		    leaseContractDtlDao.insert(l);
		}
	    }
	}
    }

    @Transactional(readOnly = false)
    public void delete(LeaseContract leaseContract) {
	super.delete(leaseContract);
    }

    @Transactional(readOnly = true)
    public List<LeaseContract> findAllValidLeaseContracts() {
	return leaseContractDao.findAllList(new LeaseContract());
    }

    @Transactional(readOnly = true)
    public Integer getTotalValidLeaseContractCounts() {
	return leaseContractDao.getTotalValidLeaseContractCounts(new LeaseContract());
    }

}