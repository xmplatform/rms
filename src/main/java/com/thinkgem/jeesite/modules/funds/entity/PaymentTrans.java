/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.funds.entity;

import org.hibernate.validator.constraints.Length;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotNull;

import com.thinkgem.jeesite.common.persistence.DataEntity;

/**
 * 款项交易Entity
 * 
 * @author huangsc
 * @version 2015-06-11
 */
public class PaymentTrans extends DataEntity<PaymentTrans> {

    private static final long serialVersionUID = 1L;
    private String tradeType; // 交易类型
    private String paymentType; // 款项类型
    private String transId; // 交易对象
    private String transName; // 交易对象名称
    private String transObjectNo;// 交易对象编号
    private String tradeDirection; // 交易款项方向
    private Date startDate; // 交易款项开始时间，用于查询条件
    private Date expiredDate; // 交易款项到期时间，用于查询条件
    private Double tradeAmount; // 应该交易金额
    private Double transAmount; // 实际交易金额
    private Double lastAmount; // 剩余交易金额
    private Double transferDepositAmount; // 定金转合同的实转金额
    private String transStatus; // 交易款项状态
    private String remittanceDate;

    public PaymentTrans() {
	super();
    }

    public PaymentTrans(String id) {
	super(id);
    }

    @Length(min = 1, max = 64, message = "交易类型长度必须介于 1 和 64 之间")
    public String getTradeType() {
	return tradeType;
    }

    public void setTradeType(String tradeType) {
	this.tradeType = tradeType;
    }

    @Length(min = 1, max = 64, message = "款项类型长度必须介于 1 和 64 之间")
    public String getPaymentType() {
	return paymentType;
    }

    public void setPaymentType(String paymentType) {
	this.paymentType = paymentType;
    }

    @Length(min = 1, max = 64, message = "交易对象长度必须介于 1 和 64 之间")
    public String getTransId() {
	return transId;
    }

    public void setTransId(String transId) {
	this.transId = transId;
    }

    @Length(min = 1, max = 64, message = "交易款项方向长度必须介于 1 和 64 之间")
    public String getTradeDirection() {
	return tradeDirection;
    }

    public void setTradeDirection(String tradeDirection) {
	this.tradeDirection = tradeDirection;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "交易款项开始时间不能为空")
    public Date getStartDate() {
	return startDate;
    }

    public void setStartDate(Date startDate) {
	this.startDate = startDate;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "交易款项到期时间不能为空")
    public Date getExpiredDate() {
	return expiredDate;
    }

    public void setExpiredDate(Date expiredDate) {
	this.expiredDate = expiredDate;
    }

    @NotNull(message = "应该交易金额不能为空")
    public Double getTradeAmount() {
	return tradeAmount;
    }

    public void setTradeAmount(Double tradeAmount) {
	this.tradeAmount = tradeAmount;
    }

    @NotNull(message = "实际交易金额不能为空")
    public Double getTransAmount() {
	return transAmount;
    }

    public void setTransAmount(Double transAmount) {
	this.transAmount = transAmount;
    }

    public Double getLastAmount() {
	return lastAmount;
    }

    public void setLastAmount(Double lastAmount) {
	this.lastAmount = lastAmount;
    }

    public Double getTransferDepositAmount() {
	return transferDepositAmount;
    }

    public void setTransferDepositAmount(Double transferDepositAmount) {
	this.transferDepositAmount = transferDepositAmount;
    }

    @Length(min = 1, max = 64, message = "交易款项状态长度必须介于 1 和 64 之间")
    public String getTransStatus() {
	return transStatus;
    }

    public void setTransStatus(String transStatus) {
	this.transStatus = transStatus;
    }

    public String getTransName() {
	return transName;
    }

    public void setTransName(String transName) {
	this.transName = transName;
    }

    public String getTransObjectNo() {
	return transObjectNo;
    }

    public void setTransObjectNo(String transObjectNo) {
	this.transObjectNo = transObjectNo;
    }

    public String getRemittanceDate() {
	return remittanceDate;
    }

    public void setRemittanceDate(String remittanceDate) {
	this.remittanceDate = remittanceDate;
    }
}