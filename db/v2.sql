alter table t_house add intent_mode varchar(64) comment '������������';

alter table t_house add is_feature varchar(64) comment '�Ƿ�ѡ��Դ';
alter table t_room add is_feature varchar(64) comment '�Ƿ�ѡ��Դ';

update t_house set is_feature='0' where is_feature is null;
update t_room set is_feature='0' where is_feature is null;

alter table t_house add rental float comment '�������';
alter table t_room add rental float comment '�������';

alter table t_house add short_desc varchar(255) comment '����';
alter table t_room add short_desc varchar(255) comment '����';

alter table t_house add short_location varchar(255) comment '��ַ����';
alter table t_room add short_location varchar(255) comment '��ַ����';

alter table t_house add pay_way varchar(2) comment '���ʽ';
alter table t_room add pay_way varchar(2) comment '���ʽ';

insert into sys_dict (id, value, label, type, description, sort, parent_id, create_by, create_date, update_by, update_date, remarks, del_flag) values('356','0','����Ѻһ','rent_fee_type','���ʽ','1','0','1',now(),'1',now(),NULL,'0');
insert into sys_dict (id, value, label, type, description, sort, parent_id, create_by, create_date, update_by, update_date, remarks, del_flag) values('357','1','����Ѻ��','rent_fee_type','���ʽ','2','0','1',now(),'1',now(),NULL,'0');

insert into sys_menu (id, parent_id, parent_ids, name, sort, href, target, icon, is_show, permission, create_by, create_date, update_by, update_date, remarks, del_flag) values('314','113','0,1,113,','ԤԼ����','50106','/contract/book',NULL,NULL,'1',NULL,'1',now(),'1',now(),NULL,'0');
insert into sys_menu (id, parent_id, parent_ids, name, sort, href, target, icon, is_show, permission, create_by, create_date, update_by, update_date, remarks, del_flag) values('315','314','0,1,96,314','�鿴','30',NULL,NULL,NULL,'0','contract:contractBook:view','1',now(),'1',now(),NULL,'0');
insert into sys_menu (id, parent_id, parent_ids, name, sort, href, target, icon, is_show, permission, create_by, create_date, update_by, update_date, remarks, del_flag) values('316','314','0,1,96,314','�޸�','60',NULL,NULL,NULL,'0','contract:contractBook:edit','1',now(),'1',now(),NULL,'0');

insert into sys_dict (id, value, label, type, description, sort, parent_id, create_by, create_date, update_by, update_date, remarks, del_flag) values('358','0','�ܼ�ȷ����','book_status','ԤԼ״̬','1','0','1',now(),'1',now(),NULL,'0');
insert into sys_dict (id, value, label, type, description, sort, parent_id, create_by, create_date, update_by, update_date, remarks, del_flag) values('359','1','ԤԼ�ɹ�','book_status','ԤԼ״̬','2','0','1',now(),'1',now(),NULL,'0');

create table t_contract_book
(
	ID                   varchar(64) NOT NULL,
	user_id              varchar(64),
	house_id             varchar(64),
	room_id              varchar(64),
	user_name            varchar(64),
	user_phone           varchar(64),
	user_gender          varchar(64),
	book_date            DATETIME,
	book_status          varchar(64),
	sales_id             varchar(64),
	CREATE_BY            VARCHAR(64) COMMENT '������',
  CREATE_DATE          DATETIME 	COMMENT '����ʱ��',
  UPDATE_BY            VARCHAR(64) COMMENT '������',
  UPDATE_DATE          TIMESTAMP COMMENT '����ʱ��',
  REMARKS              VARCHAR(255) COMMENT '��ע��Ϣ',
  DEL_FLAG             CHAR(1) DEFAULT '0' NOT NULL COMMENT 'ɾ�����',
	primary key (ID)
) comment = 'ԤԼ������Ϣ';