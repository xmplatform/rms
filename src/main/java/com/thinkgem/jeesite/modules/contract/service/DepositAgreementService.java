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

import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.service.CrudService;
import com.thinkgem.jeesite.common.utils.IdGen;
import com.thinkgem.jeesite.modules.common.dao.AttachmentDao;
import com.thinkgem.jeesite.modules.common.entity.Attachment;
import com.thinkgem.jeesite.modules.contract.dao.AuditDao;
import com.thinkgem.jeesite.modules.contract.dao.AuditHisDao;
import com.thinkgem.jeesite.modules.contract.dao.ContractTenantDao;
import com.thinkgem.jeesite.modules.contract.dao.DepositAgreementDao;
import com.thinkgem.jeesite.modules.contract.entity.Audit;
import com.thinkgem.jeesite.modules.contract.entity.AuditHis;
import com.thinkgem.jeesite.modules.contract.entity.ContractTenant;
import com.thinkgem.jeesite.modules.contract.entity.DepositAgreement;
import com.thinkgem.jeesite.modules.contract.entity.FileType;
import com.thinkgem.jeesite.modules.funds.dao.PaymentTradeDao;
import com.thinkgem.jeesite.modules.funds.dao.PaymentTransDao;
import com.thinkgem.jeesite.modules.funds.dao.ReceiptDao;
import com.thinkgem.jeesite.modules.funds.dao.TradingAccountsDao;
import com.thinkgem.jeesite.modules.funds.entity.PaymentTrade;
import com.thinkgem.jeesite.modules.funds.entity.PaymentTrans;
import com.thinkgem.jeesite.modules.funds.entity.Receipt;
import com.thinkgem.jeesite.modules.funds.entity.TradingAccounts;
import com.thinkgem.jeesite.modules.inventory.dao.HouseDao;
import com.thinkgem.jeesite.modules.inventory.dao.RoomDao;
import com.thinkgem.jeesite.modules.inventory.entity.House;
import com.thinkgem.jeesite.modules.inventory.entity.Room;
import com.thinkgem.jeesite.modules.person.dao.TenantDao;
import com.thinkgem.jeesite.modules.person.entity.Tenant;
import com.thinkgem.jeesite.modules.sys.utils.UserUtils;

/**
 * 定金协议Service
 * 
 * @author huangsc
 * @version 2015-06-09
 */
@Service
@Transactional(readOnly = true)
public class DepositAgreementService extends CrudService<DepositAgreementDao, DepositAgreement> {
    @Autowired
    private PaymentTransDao paymentTransDao;
    @Autowired
    private DepositAgreementDao depositAgreementDao;
    @Autowired
    private AuditDao auditDao;
    @Autowired
    private AuditHisDao auditHisDao;
    @Autowired
    private ContractTenantDao contractTenantDao;
    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private HouseDao houseDao;
    @Autowired
    private RoomDao roomDao;
    @Autowired
    private TradingAccountsDao tradingAccountsDao;
    @Autowired
    private AttachmentDao attachmentDao;
    @Autowired
    private ReceiptDao receiptDao;
    @Autowired
    private PaymentTradeDao paymentTradeDao;

    private static final String DEPOSIT_AGREEMENT_ROLE = "deposit_agreement_role";// 定金协议审批

    public DepositAgreement get(String id) {
	return super.get(id);
    }

    public List<DepositAgreement> findList(DepositAgreement depositAgreement) {
	return super.findList(depositAgreement);
    }

    public Page<DepositAgreement> findPage(Page<DepositAgreement> page, DepositAgreement depositAgreement) {
	return super.findPage(page, depositAgreement);
    }

    public List<Tenant> findTenant(DepositAgreement depositAgreement) {
	List<Tenant> tenantList = new ArrayList<Tenant>();
	ContractTenant contractTenant = new ContractTenant();
	contractTenant.setDepositAgreementId(depositAgreement.getId());
	List<ContractTenant> list = contractTenantDao.findList(contractTenant);
	for (ContractTenant tmpContractTenant : list) {
	    Tenant tenant = tenantDao.get(tmpContractTenant.getTenantId());
	    tenantList.add(tenant);
	}
	return tenantList;
    }

    /**
     * 定金转违约，各分别生成一笔应出定金，一笔定金违约金，都是已经到账的。 如果有退费再生成退费
     */
    @Transactional(readOnly = false)
    public void breakContract(DepositAgreement depositAgreement) {
	Double refundAmount = depositAgreement.getRefundAmount();
	depositAgreement = depositAgreementDao.get(depositAgreement.getId());
	if (refundAmount != null && refundAmount > 0) {
	    depositAgreement.setRefundAmount(refundAmount);
	}

	/* 1.生成款项--定金转违约退费 */
	// 定金转违约,'26'='定金转违约退费',出款,未到账登记
	if (null != depositAgreement.getRefundAmount() && depositAgreement.getRefundAmount() > 0) {
	    generateAndSavePaymentTrans("2", "26", depositAgreement.getId(), "0", depositAgreement.getRefundAmount(), depositAgreement.getRefundAmount(), 0D, "0", depositAgreement.getStartDate(), depositAgreement.getExpiredDate());
	}

	// 系统生成完全到账的应出定金款项 2=定金转违约；27=应出定金 应出为0，
	generateAndSavePaymentTrans("2", "27", depositAgreement.getId(), "0", depositAgreement.getDepositAmount(), 0D, depositAgreement.getDepositAmount(), "2", depositAgreement.getStartDate(), depositAgreement.getExpiredDate());

	// 系统生成完全到账的应收定金违约金 2=定金转违约；1=定金违约金；应收为1，
	generateAndSavePaymentTrans("2", "1", depositAgreement.getId(), "1", depositAgreement.getDepositAmount(), 0D, depositAgreement.getDepositAmount(), "2", depositAgreement.getStartDate(), depositAgreement.getExpiredDate());

	if (null != depositAgreement.getRefundAmount() && depositAgreement.getRefundAmount() > 0) {
	    // 更新定金协议为“定金转违约到账待登记”
	    depositAgreement.setAgreementBusiStatus("3");// '3'='定金转违约到账待登记'
	    depositAgreement.setUpdateDate(new Date());
	    depositAgreement.setUpdateBy(UserUtils.getUser());
	    depositAgreementDao.update(depositAgreement);
	} else {
	    // 更新定金协议为“定金转违约到账待登记”
	    depositAgreement.setAgreementBusiStatus("1");// '1'= 已转违约
	    depositAgreement.setUpdateDate(new Date());
	    depositAgreement.setUpdateBy(UserUtils.getUser());
	    depositAgreementDao.update(depositAgreement);
	}

	/* 3.更新房屋/房间状态 */
	if ("0".equals(depositAgreement.getRentMode())) {// 整租
	    House house = houseDao.get(depositAgreement.getHouse().getId());
	    house.setHouseStatus("1");// 待出租可预订
	    house.setCreateBy(UserUtils.getUser());
	    house.setUpdateDate(new Date());
	    houseDao.update(house);
	    // 同时把房间的状态都更新为“待出租可预订”
	    Room parameterRoom = new Room();
	    parameterRoom.setHouse(house);
	    List<Room> rooms = roomDao.findList(parameterRoom);
	    if (CollectionUtils.isNotEmpty(rooms)) {
		for (Room r : rooms) {
		    r.setRoomStatus("1");// “待出租可预订”
		    r.setUpdateBy(UserUtils.getUser());
		    r.setUpdateDate(new Date());
		    roomDao.update(r);
		}
	    }
	} else {// 单间
	    Room room = roomDao.get(depositAgreement.getRoom().getId());
	    room.setRoomStatus("1");// 待出租可预订
	    room.setCreateBy(UserUtils.getUser());
	    room.setUpdateDate(new Date());
	    roomDao.update(room);
	}
    }

    @Transactional(readOnly = false)
    public void audit(AuditHis auditHis) {
	AuditHis saveAuditHis = new AuditHis();
	saveAuditHis.setId(IdGen.uuid());
	saveAuditHis.setObjectType("1");// 定金协议
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

	if ("1".equals(auditHis.getAuditStatus())) {
	    // 审核
	    Audit audit = new Audit();
	    audit.setObjectId(auditHis.getObjectId());
	    audit.setNextRole("");
	    audit.setUpdateDate(new Date());
	    audit.setUpdateBy(UserUtils.getUser());
	    auditDao.update(audit);
	} else {// 审核拒绝时，需要把房屋和房间的状态回滚到原先状态
	    /* 更新房屋/房间状态 */
	    DepositAgreement depositAgreement = this.depositAgreementDao.get(auditHis.getObjectId());
	    if ("0".equals(depositAgreement.getRentMode())) {// 整租
		House house = houseDao.get(depositAgreement.getHouse().getId());
		house.setHouseStatus("1");// 待出租可预订
		house.setUpdateBy(UserUtils.getUser());
		house.setUpdateDate(new Date());
		houseDao.update(house);
		// 同时把房间的状态都更新为“待出租可预订”
		Room parameterRoom = new Room();
		parameterRoom.setHouse(house);
		List<Room> rooms = roomDao.findList(parameterRoom);
		if (CollectionUtils.isNotEmpty(rooms)) {
		    for (Room r : rooms) {
			r.setRoomStatus("1");// 待出租可预订
			r.setUpdateBy(UserUtils.getUser());
			r.setUpdateDate(new Date());
			roomDao.update(r);
		    }
		}
	    } else {// 单间
		Room room = roomDao.get(depositAgreement.getRoom().getId());
		room.setRoomStatus("1");// 待出租可预订
		room.setUpdateBy(UserUtils.getUser());
		room.setUpdateDate(new Date());
		roomDao.update(room);
		// 更新房屋状态
		House h = houseDao.get(room.getHouse().getId());
		if ("2".equals(h.getHouseStatus())) {// 如果房屋状态是“已预定”
		    h.setHouseStatus("1");// 待出租可预订
		    h.setUpdateBy(UserUtils.getUser());
		    h.setUpdateDate(new Date());
		    houseDao.update(h);
		}
	    }

	    // 删除生成的款项
	    PaymentTrans delPaymentTrans = new PaymentTrans();
	    delPaymentTrans.setTransId(auditHis.getObjectId());
	    paymentTransDao.delete(delPaymentTrans);

	    /* 删除账务交易 */
	    TradingAccounts tradingAccounts = new TradingAccounts();
	    tradingAccounts.setTradeId(auditHis.getObjectId());
	    tradingAccounts.setTradeStatus("0");// 待审核
	    List<TradingAccounts> list = tradingAccountsDao.findList(tradingAccounts);
	    if (null != list && list.size() > 0) {
		for (TradingAccounts tmpTradingAccounts : list) {
		    // 删除账务和款项关联关系记录
		    PaymentTrade pt = new PaymentTrade();
		    pt.setTradeId(tmpTradingAccounts.getId());
		    paymentTradeDao.delete(pt);

		    /* 删除收据 */
		    Receipt receipt = new Receipt();
		    TradingAccounts delTradingAccounts = new TradingAccounts();
		    delTradingAccounts.setId(tmpTradingAccounts.getId());
		    receipt.setTradingAccounts(delTradingAccounts);
		    receipt.setUpdateBy(UserUtils.getUser());
		    receipt.setUpdateDate(new Date());
		    this.receiptDao.delete(receipt);
		}

		// 删除账务交易
		tradingAccounts.setUpdateBy(UserUtils.getUser());
		tradingAccounts.setUpdateDate(new Date());
		tradingAccounts.setDelFlag("1");
		tradingAccountsDao.delete(tradingAccounts);
	    }
	}

	DepositAgreement depositAgreement = depositAgreementDao.get(auditHis.getObjectId());
	depositAgreement.setAgreementStatus("1".equals(auditHis.getAuditStatus()) ? "3" : auditHis.getAuditStatus());// 2:内容审核拒绝
														     // 3:内容审核通过到账收据待审核
	depositAgreement.setUpdateDate(new Date());
	depositAgreement.setUpdateBy(UserUtils.getUser());
	depositAgreementDao.update(depositAgreement);
    }

    @Transactional(readOnly = false)
    public void save(DepositAgreement depositAgreement) {
	String id = super.saveAndReturnId(depositAgreement);

	if ("1".equals(depositAgreement.getValidatorFlag())) {// 正常保存，而非暂存,暂存为0
	    // 生成款项
	    PaymentTrans delPaymentTrans = new PaymentTrans();
	    delPaymentTrans.setTransId(id);
	    paymentTransDao.delete(delPaymentTrans);

	    TradingAccounts delTradingAccounts = new TradingAccounts();
	    delTradingAccounts.setTradeId(id);
	    List<TradingAccounts> list = tradingAccountsDao.findList(delTradingAccounts);

	    for (TradingAccounts dTradingAccounts : list) {
		// 删除已经上传的收据附件
		Attachment attachment3 = new Attachment();
		attachment3.setTradingAccountsId(dTradingAccounts.getId());
		attachmentDao.delete(attachment3);

		// 同时删除已经录入的收据记录
		Receipt r = new Receipt();
		r.setTradingAccounts(dTradingAccounts);
		receiptDao.delete(r);

		// 删除账务记录
		tradingAccountsDao.delete(dTradingAccounts);

		// 删除账务记录款项关联信息
		PaymentTrade delPaymentTrade = new PaymentTrade();
		delPaymentTrade.setTradeId(dTradingAccounts.getId());
		paymentTradeDao.delete(delPaymentTrade);
	    }

	    if (null != depositAgreement.getStartDate() && null != depositAgreement.getExpiredDate() && null != depositAgreement.getDepositAmount()) {
		// 定金协议， 应收定金,收款,未到账登记
		generateAndSavePaymentTrans("1", "0", id, "1", depositAgreement.getDepositAmount(), depositAgreement.getDepositAmount(), 0D, "0", depositAgreement.getStartDate(), depositAgreement.getExpiredDate());
	    }

	    /* 更新房屋/房间状态 */
	    if ("0".equals(depositAgreement.getRentMode())) {// 整租
		House house = houseDao.get(depositAgreement.getHouse().getId());
		if (house != null) {
		    house.setHouseStatus("2");// 已预定
		    house.setCreateBy(UserUtils.getUser());
		    house.setUpdateDate(new Date());
		    houseDao.update(house);
		    // 同时把所属的房间状态都更新为“已预定”
		    Room parameterRoom = new Room();
		    parameterRoom.setHouse(house);
		    List<Room> rooms = roomDao.findList(parameterRoom);
		    if (CollectionUtils.isNotEmpty(rooms)) {
			for (Room r : rooms) {
			    r.setRoomStatus("2");// 已预定
			    r.setUpdateBy(UserUtils.getUser());
			    r.setUpdateDate(new Date());
			    roomDao.update(r);
			}
		    }
		}
	    } else {// 单间
		if (null != depositAgreement.getRoom() && !StringUtils.isBlank(depositAgreement.getRoom().getId())) {
		    Room room = roomDao.get(depositAgreement.getRoom().getId());
		    if (room != null) {
			room.setRoomStatus("2");// 已预定
			room.setCreateBy(UserUtils.getUser());
			room.setUpdateDate(new Date());
			roomDao.update(room);
			// 同时更新该房间所属房屋的状态
			House h = houseDao.get(room.getHouse().getId());
			if ("1".equals(h.getHouseStatus())) {// 待出租可预订
			    Room queryRoom = new Room();
			    queryRoom.setHouse(h);
			    List<Room> roomsOfHouse = roomDao.findList(queryRoom);
			    if (CollectionUtils.isNotEmpty(roomsOfHouse)) {
				int depositCount = 0;// 预定或出租的数量
				for (Room depositedRoom : roomsOfHouse) {
				    if ("2".equals(depositedRoom.getRoomStatus()) || "3".equals(depositedRoom.getRoomStatus())) {// 房间已预定
					depositCount = depositCount + 1;
				    }
				}
				if (depositCount == roomsOfHouse.size()) {// 房屋内房间全部出租或预定，房屋状态更新为“已预定”
				    h.setHouseStatus("2");// 已预定
				    h.setCreateBy(UserUtils.getUser());
				    h.setUpdateDate(new Date());
				    houseDao.update(h);
				}
			    }
			}
		    }
		}
	    }

	    // 审核
	    Audit audit = new Audit();
	    audit.setId(IdGen.uuid());
	    audit.setObjectId(id);
	    auditDao.delete(audit);
	    audit.setObjectType("1");// 预约定金
	    audit.setNextRole(DEPOSIT_AGREEMENT_ROLE);
	    audit.setCreateDate(new Date());
	    audit.setCreateBy(UserUtils.getUser());
	    audit.setUpdateDate(new Date());
	    audit.setUpdateBy(UserUtils.getUser());
	    audit.setDelFlag("0");
	    auditDao.insert(audit);
	}

	if (null != depositAgreement.getTenantList() && depositAgreement.getTenantList().size() > 0) {
	    /* 合同租客关联信息 */
	    ContractTenant delTenant = new ContractTenant();
	    delTenant.setDepositAgreementId(id);
	    contractTenantDao.delete(delTenant);
	    List<Tenant> list = depositAgreement.getTenantList();
	    if (null != list && list.size() > 0) {
		for (Tenant tenant : list) {
		    ContractTenant contractTenant = new ContractTenant();
		    contractTenant.setId(IdGen.uuid());
		    contractTenant.setTenantId(tenant.getId());
		    contractTenant.setDepositAgreementId(id);
		    contractTenant.setCreateDate(new Date());
		    contractTenant.setCreateBy(UserUtils.getUser());
		    contractTenant.setUpdateDate(new Date());
		    contractTenant.setUpdateBy(UserUtils.getUser());
		    contractTenant.setDelFlag("0");
		    contractTenantDao.insert(contractTenant);
		}
	    }
	}

	if (!depositAgreement.getIsNewRecord()) {// 新增清空定金协议所有的附件信息
	    Attachment attachment = new Attachment();
	    attachment.setDepositAgreemId(depositAgreement.getId());
	    attachmentDao.delete(attachment);
	}

	if (!StringUtils.isBlank(depositAgreement.getDepositAgreementFile())) {
	    generateAndSaveAttachment(depositAgreement.getId(), depositAgreement.getDepositAgreementFile(), FileType.DEPOSITAGREEMENT_FILE.getValue());
	}

	if (!StringUtils.isBlank(depositAgreement.getDepositCustomerIDFile())) {
	    generateAndSaveAttachment(depositAgreement.getId(), depositAgreement.getDepositCustomerIDFile(), FileType.TENANT_ID.getValue());
	}

	if (!StringUtils.isBlank(depositAgreement.getDepositOtherFile())) {
	    generateAndSaveAttachment(depositAgreement.getId(), depositAgreement.getDepositOtherFile(), FileType.DEPOSITRECEIPT_FILE_OTHER.getValue());
	}
    }

    @Transactional(readOnly = false)
    public void delete(DepositAgreement depositAgreement) {
	super.delete(depositAgreement);
    }

    @Transactional(readOnly = true)
    public List<DepositAgreement> findAllValidAgreements() {
	return depositAgreementDao.findAllList(new DepositAgreement());
    }

    @Transactional(readOnly = true)
    public Integer getTotalValidDACounts() {
	return depositAgreementDao.getTotalValidDACounts(new DepositAgreement());
    }

    private void generateAndSaveAttachment(String id, String filePath, String fileType) {
	Attachment attachment = new Attachment();
	attachment.setId(IdGen.uuid());
	attachment.setDepositAgreemId(id);
	attachment.setAttachmentType(fileType);
	attachment.setAttachmentPath(filePath);
	attachment.setCreateDate(new Date());
	attachment.setCreateBy(UserUtils.getUser());
	attachment.setUpdateDate(new Date());
	attachment.setUpdateBy(UserUtils.getUser());
	attachment.setDelFlag("0");
	attachmentDao.insert(attachment);
    }

    private void generateAndSavePaymentTrans(String tradeType, String paymentType, String transId, String tradeDirection, Double tradeAmount, Double lastAmount, Double transAmount, String transStatus, Date startDate, Date expiredDate) {
	if (tradeAmount != null && tradeAmount > 0D) {
	    PaymentTrans paymentTrans = new PaymentTrans();
	    paymentTrans.setId(IdGen.uuid());
	    paymentTrans.setTradeType(tradeType);
	    paymentTrans.setPaymentType(paymentType);
	    paymentTrans.setTransId(transId);
	    paymentTrans.setTradeDirection(tradeDirection);
	    paymentTrans.setStartDate(startDate);
	    paymentTrans.setExpiredDate(expiredDate);
	    paymentTrans.setTradeAmount(tradeAmount);
	    paymentTrans.setLastAmount(lastAmount);
	    paymentTrans.setTransAmount(transAmount);
	    paymentTrans.setTransStatus(transStatus);
	    paymentTrans.setCreateDate(new Date());
	    paymentTrans.setCreateBy(UserUtils.getUser());
	    paymentTrans.setUpdateDate(new Date());
	    paymentTrans.setUpdateBy(UserUtils.getUser());
	    paymentTrans.setDelFlag("0");
	    paymentTransDao.insert(paymentTrans);
	}
    }
}