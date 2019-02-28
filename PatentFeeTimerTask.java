package inc.tech.patent.util;

import inc.tech.patent.action.*;
import inc.tech.patent.dao.KjPatentDao;
import inc.tech.patent.dao.KjPatentFeeDao;
import inc.tech.patent.dao.KjProjectStaffDao;
import inc.tech.persistent.DAOException;
import inc.tech.persistent.entity.KjAuditLog;
import inc.tech.persistent.entity.KjPatent;
import inc.tech.persistent.entity.KjPatentFee;
import inc.tech.persistent.entity.KjProjectstaff;
import inc.tech.persistent.entity.KjTableTemplate;
import inc.tech.persistent.entity.KjUser;
import inc.tech.sys.chop.dao.KjAuditLogDAO;
import inc.tech.sys.init.TechSystem;
import inc.tech.sys.mail.dao.KjMailDAO;
import inc.tech.sys.mail.form.KjMailForm;
import inc.tech.sys.table.dao.KjTableTemplateDao;
import inc.tech.user.dao.KjUserDAO;
import inc.tech.util.Clawer;
import inc.tech.util.DateUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.WebClient;
 
public class PatentFeeTimerTask extends TimerTask {
	Clawer patentClawer = new Clawer();
	WebClient webClient = null;
	
	public void run(){
		
		List<Long> updatedPatentId = new ArrayList<Long>();//for email
		long startTime=System.currentTimeMillis();   //获取开始时间  
		
//		if (TechSystem.getInstance().getTechConfig().getPatenFeeGetOn())             先注释掉了  
		if (true)
		{
		System.out.println(" PatentFeeTimerTask  专利网费用获取 启动.... "  );
		
		
			//TODO 1 取所有 科技系统中过程状态为31以后的数据。 并使用过期时间排序、分组。
		KjPatentDao patentDao = KjPatentDao.getInstance();//专利
//		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.state in (select s.code from KjPatentState s where s.id >= 31 )"
//				+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.patentNo in ('2015107021978','2011103261546','2011103318309','2010105221873')"
			+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		List<KjPatent> kjpl = patentDao.findByHQL(sqlWhere);
		//Clawer patentClawer = new Clawer();
		KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();
		
		if(kjpl!=null)
		{
			System.out.println("非失效专利 - " + kjpl.size());
			//登陆专利网 
			//WebClient webClient = null;
			try{
				long startLoginTime=System.currentTimeMillis();   //获取开始时间
			
				//webClient = patentClawer.loginCponline();
				if(webClient!=null){
					long endLoginTime=System.currentTimeMillis(); //获取结束时间  
//					System.out.println("连接时间： "+(endLoginTime-startLoginTime)+"ms");
					
//					for(int i =0;i<1000;i++)
					for(int i =0;i<kjpl.size() ;i++)
//					for(int i =2200;i<kjpl.size() && i<2250 ;i++)
					{
						//TODO 2 对每组专利，判断历史日志，今天是否轮到更新它
						if(i%50 == 0) System.out.println(i);
						//获取该专利的年费，并计算剩余时间
						boolean isUpdate = false;
						
						Map resultMap = KjPatentFeeDao.getInstance().getRemainDayAndExpireDayByPatentId(kjpl.get(i).getPatentId());
						Long remainDays = 10000L; // 当前专利的剩余天数,以最小剩余天数为准
						Date expireDay = new Date(); // 专利离当前最小剩余天数对应的截止日期(用于找到该截止日期需要提醒的所有费用记录)
						remainDays = (Long) resultMap.get("remainDays");
						expireDay = (Date) resultMap.get("expireDay");
						
						if(remainDays<=120)
						{
							long daysFromLastLog = daysFromLastUpdateLogDate(kjpl.get(i));
							if(daysFromLastLog!=-1){// 表示有日志且计算成功
//								System.out.println("表示有日志且计算成功" + daysFromLastLog);
								List<KjPatentFee> kpfs=patentFeedao.findByPIdAndConfig(Long.valueOf(kjpl.get(i).getPatentId()),11L);
								if (kpfs != null && kpfs.size() > 0) {// 有专利年费记录才有剩余时间？
									if (remainDays < 7l) {// 剩余时间 小于 7天 1天更新一次 （包过剩余时间 小于 0？？）
										isUpdate = true;
									} else if (remainDays >= 7l && remainDays < 15l) {// 大于7天 小于15天的 2天更新一次
										if (daysFromLastLog >= 7) isUpdate = true;
									} else if (remainDays >= 15l && remainDays < 30l) {// 大于15天  小于30天的 7天更新一次
										if (daysFromLastLog >= 7l) isUpdate = true;
									} else if (remainDays >= 30l && remainDays < 60l) {// 大于30天 小于60天的 15天更新一次
										if (daysFromLastLog >= 15l) isUpdate = true;
									} else {// 大于60天 30天更新一次
										if (daysFromLastLog >= 30l) isUpdate = true;
									}
								}else{
									// TODO 没有专利年费 ？
									isUpdate=true;
								}
							}else{//表示该专利没有日志 或者 日志 没有“同步专利网费用”的字样，即认为第一次执行，全部更新
								isUpdate=true;
//								System.out.println("表示该专利没有日志 或者 日志 没有 同步专利网费用 的字样");
							}
//							System.out.println(kjpl.get(i) .getPatentNo() + " : isUpdate-" + isUpdate);
						}
						
						//TODO 3 给可以更新的专利 进行更新 getFeeSynchronization的双胞胎方法 ，并记录日志
						if(isUpdate){
							
//							System.out.println("给可以更新的专利 进行更新 getFeeSynchronization的双胞胎方法 ，并记录日志");
							
							//if(!kjpl.get(i).getState().equals("55") && !kjpl.get(i).getState().equals("56")){
//							System.out.println(kjpl.get(i).getPatentNo()+" state:"+kjpl.get(i).getState());
							String patentNo = kjpl.get(i).getPatentNo().toUpperCase().replaceAll("ZL", "").replaceAll(" ", "");//专利号
							if(patentNo.contains("."))patentNo=patentNo.replace(".", "");// 专利号中 含有 "." 字符 特殊处理。
							Document doc =  patentClawer.getDocOfCponline(webClient, patentNo);
							if(doc!=null)
							{
								boolean isSucceed = patentClawer.synPatentAndFee(doc,kjpl.get(i),null);
								if(isSucceed){//如果更新成功
									updatedPatentId.add(kjpl.get(i).getPatentId());
									System.out.println("patentNo:"+patentNo+" syn succeed");
								}else{
									System.out.println("patentNo:"+patentNo+" syn error");
								}
							}else{
								System.out.println("patentNo:"+patentNo+" 获取专利网数据失败，网址（http://www.cponline.gov.cn/）");
							}
							//}
						}
					}// end for
				}// end if(webClient!=null){
				else{
					System.out.println("专利网（http://www.cponline.gov.cn/）连接失败 ----------- connect failure：  ");
				}
				
			}catch(InterruptedException e ){//SAXException, InterruptedException
				System.out.println(e.getMessage());
			}catch(SAXException e)
			{
				System.out.println(e.getMessage());
			}catch(IOException e)
			{
				System.out.println(e.getMessage());
			}catch(ParseException e)
			{
				System.out.println(e.getMessage());
			}catch(DAOException e)
			{
				System.out.println(e.getMessage());
			}
			
		}
		else
		{
			System.out.println("无数据");
		}
		//KjPatent kjp = patentDao.findByPk(Long.valueOf(patentId));
			//为了方便测试批量执行的效率，现在可以先省掉1 2，直接执行3，更新前20条或N条，判断批量效率
			
		} else {
			System.out.println("专利年份额获取及同步程序 已禁用 ----------- PatentFeeTimerTask not open： ");
		}
//		for(int i=0;i<updatedPatentId.size();i++)
//		{
//			System.out.println(updatedPatentId.get(i));
//		}
		long endTime=System.currentTimeMillis(); //获取结束时间  
		System.out.println("同步所用所有时间： -----------  PatentFeeTimerTask cost time： "+(endTime-startTime)+"ms");
		String sendType = "";
//		sendType = TechSystem.getInstance().getTechConfig().getPatenFeeMailSendType();
		//TODO sendType 中含有字符1，给发明专利发邮件，含有2，给实用新型发，含有3，给外观设计发。
//		List<Long> sendMailPatentList = getListForSendMail(updatedPatentId,sendType); //根据patentIdList和sendType，判断是否符合发送邮件条件，返回列表
//		System.out.println("符合要求的记录数： ----------- toSend mail ： "+sendMailPatentList.size());
		
		System.out.println(" ----  senMail function will open in coming days ...");
		
		//给patentId里面的所有人员发送邮件
//		for(int i=0;i<sendMailPatentList.size();i++)
//		{
//			try {
//				sendMailById(sendMailPatentList.get(i));
//			} catch (ParseException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
		//测试发送邮件功能
//		Long id = 2283L;
//		try {
//			sendMailById(id);
//		} catch (ParseException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
		
		System.out.println("结束时系统时间 ----------- end PatentFeeTimerTask： "+new Date(Calendar.getInstance().getTimeInMillis()));
	}
	public static long daysFromLastUpdateLogDate(KjPatent kjp) throws ParseException{
		KjAuditLogDAO kjAuditdao = KjAuditLogDAO.getInstance();
		List<KjAuditLog> kjAuditLogList = kjAuditdao.getAuditLogList(15l, kjp.getPatentId());
		if(kjAuditLogList!=null && kjAuditLogList.size()>0)
		{
			//对获取的日志排序
			Collections.sort(kjAuditLogList, new Comparator<KjAuditLog>() {//日志从近到晚排序，离现在越近排越前面
	            public int compare(KjAuditLog arg0, KjAuditLog arg1) {
	                return arg1.getOperTime().compareTo(arg0.getOperTime());
	            }
	        });
			//获取上次日志时间
			String lastUpdateLogTime ="";
			for(int i=0;i<kjAuditLogList.size();i++){//同步专利网费用数据
				if(kjAuditLogList.get(i).getOperMsg()!=null 
						&& kjAuditLogList.get(i).getOperMsg().contains("同步专利网费用")){
					lastUpdateLogTime= kjAuditLogList.get(i).getOperTime();
					break;
				}
			}
			if(!"".equals(lastUpdateLogTime)){//说明之前有同步的记录
				//计算两次相差的时间
				SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd");
			    Date todayDate=new java.util.Date();
			    Date lastDate=df.parse(lastUpdateLogTime);
			    long lastTime=lastDate.getTime();
			    long todayTime=todayDate.getTime();
//			    System.out.println((todayTime-lastTime)/(1000*60*60*24));
			    long intervalDays = (todayTime-lastTime)/(1000*60*60*24);
			    return intervalDays;
			}else{//说明之前没有同步的记录   ！！！！
				return -1;
			}
			
		    
		}else{
			return -1;
		}
		
	}
	
	/*
	 * yzw 2015-11-15 基于专利id列表发送邮件
	 */
	public void sendMailById(Long id)throws ParseException, IOException
	{
		
		KjUser kjUser = null;
		KjUserDAO kjUserDao = KjUserDAO.getInstance();
		KjPatent kjPatent =new KjPatent();
		KjMailForm kjMailForm = new KjMailForm(); //初始化邮件Form
		StringBuffer contentStrBuf = new StringBuffer(""); //邮件内容缓冲
		String mailType = "patentFee";    //邮件类型
		String firstObjName = "";  //设置邮件主题
		
		kjMailForm.setTitle("关于专利：");
		kjMailForm.setMailType(mailType);
		
		//获取邮件内容
		kjPatent = KjPatentDao.getInstance().findByPk(id);
	
		kjUser = kjPatent.getKjFirstInventorId();
	    firstObjName = "《"+kjPatent.getName()+"》" + " 需尽快缴纳年费";
	    
	    KjTableTemplate kjTableTemplate = KjTableTemplateDao.getInstance().searchTable("专利费用", 15l);
	    String thisContent = kjTableTemplate.getContent();
	    thisContent = thisContent.replaceAll("param_patentName", kjPatent.getName()==null?"":kjPatent.getName());
	    thisContent = thisContent.replaceAll("param_patentNo", kjPatent.getPatentNo()==null?"":kjPatent.getPatentNo());
	    thisContent = thisContent.replaceAll("param_patentName", kjPatent.getKjFirstInventorId().getName());
	    String remindStr = KjPatentFeeDao.getInstance().getRemindStringByPatentId(kjPatent.getPatentId());
	    thisContent = thisContent.replaceAll("param_feeNameAndAmount", remindStr.substring(remindStr.indexOf("其中")));
	    List<KjProjectstaff> newProjectStaffList = KjProjectStaffDao.getInstance().searchProjectStaff(kjPatent.getPatentId(), 2L, "0");
	    List<KjProjectstaff> oldProjectStaffList = KjProjectStaffDao.getInstance().searchProjectStaff(kjPatent.getPatentId(), 0L, "0");
	    if(newProjectStaffList==null || newProjectStaffList.size()==0) newProjectStaffList = oldProjectStaffList;
	    	String allUsername = "";
	    if(newProjectStaffList!=null)
	    {
	    	for (int j = 0; j < newProjectStaffList.size(); j++) 
	    	{
	    		if(j==0) allUsername = newProjectStaffList.get(j).getKjUser().getName();
	    		else allUsername = allUsername + "," + newProjectStaffList.get(j).getKjUser().getName();
			}
	    }
	    thisContent = thisContent.replaceAll("param_allInventors", allUsername==null?"":allUsername);
	    String acceptDate = DateUtil.convertDateToString(kjPatent.getAcceptDate());
	    thisContent = thisContent.replaceAll("param_applyDate", acceptDate==null?"":acceptDate);
	    			
	    contentStrBuf.append(thisContent);
	    			
		 kjMailForm.setTitle(kjMailForm.getTitle() + firstObjName + " 的提醒");
		 kjMailForm.setContent(contentStrBuf.toString());
		 
		 String content = contentStrBuf.toString();
		 String title = kjMailForm.getTitle();
		 String sendMailStatus = "";
		 String sendSmsStatus = "未提交发送请求";
		 
		 //获取所有人员
		 List<String> toUserId = new ArrayList<String>();
		 /*/(只获取第一发明人id，只对第一发明人发送邮件)
		 if(newProjectStaffList!=null && newProjectStaffList.size()>0)
		 {
			 if(newProjectStaffList.get(0).getStaffId()!=null)
				 toUserId.add(newProjectStaffList.get(0).getStaffId()); 
		 }*/
		 //获取老师发明人（获取第一发明人）
		 for(int i=0;i<newProjectStaffList.size();i++)
		 {
			String sendMailUserIdTemp = newProjectStaffList.get(i).getStaffId();
			if(sendMailUserIdTemp !=null && sendMailUserIdTemp.length() == 6)  //staffId位数是6位，表示老师
			{
				toUserId.add(sendMailUserIdTemp);
				break;
			}
		 }
		 /*KjMailAction kjMailAction = new KjMailAction();
		 List<String> thisUserIds =  kjMailAction.getUserId(id.toString(), mailType);
		 //KjPatent thisPatent = KjPatentDao.getInstance().findByPk(id);
		 if(thisUserIds!=null && thisUserIds.size()>0){
			for (int j = 0; j < thisUserIds.size(); j++){
				String thisUserId = thisUserIds.get(j);
				//KjUser thisUser = KjUserDAO.getInstance().findByPk(thisUserId);
				if(thisUserId.length()>0 && !toUserId.contains(thisUserId)){
					toUserId.add(thisUserId);
				}
			}
		 }*/
		 //start - 发送邮件
		 String toAddressList = "";
		 String sendMailStatusItem = "未提交发送请求";
		 //send mails
		 if(toUserId.size()>0){                         
				for (int j = 0; j < toUserId.size(); j++){
					Boolean isSend = false;
					String thisUserId = toUserId.get(j);
					kjUser = kjUserDao.findByPk(thisUserId);
					//System.out.println("emailAdd"+kjUser.getEmail());
						if (kjUser != null && kjUser.getEmail() != null && kjUser.getEmail().length() > 0){
							try{
								Properties p = new Properties(); // Properties p =
																	// System.getProperties();
								p.put("mail.smtp.auth", "true");
								//System.out.println(TechSystem.getInstance().getTechConfig().getFromServer());
								p.put("mail.transport.protocol", "smtp");
								p.put("mail.smtp.host", TechSystem.getInstance().getTechConfig().getFromServer());
								p.put("mail.smtp.port", "25");
								// 建立会话
								Session session2 = Session.getInstance(p);
								Message msg = new MimeMessage(session2); // 建立信息
								
								// zz 2012-03-26
								String fromMail = MimeUtility.encodeText( "kj.tju.edu.cn", "gb2312", "B");
		
								msg.setFrom(new InternetAddress( TechSystem.getInstance().getTechConfig().getFromAddress(), fromMail));
		
								msg.addRecipient(Message.RecipientType.TO, new InternetAddress(kjUser.getEmail()));
							
								// msg.setSentDate(); // 发送日期
								if (title != null && title.length() > 0)
									msg.setSubject(title); // 主题
								msg.setContent(content, "text/html;charset = gbk");  //zz 215-04-30
								
								if(toAddressList.equals("")){
									toAddressList = kjUser.getEmail();
								}
								else{
									toAddressList = toAddressList + "," + kjUser.getEmail();
								}
								
								// 邮件服务器进行验证,本地调试的时候要屏蔽
								Transport tran = session2.getTransport("smtp");
								tran.connect(TechSystem.getInstance().getTechConfig() .getFromServer(),
											TechSystem.getInstance() .getTechConfig().getFromAddress(), 
											TechSystem .getInstance().getTechConfig() .getFromPassword() );
								tran.sendMessage(msg, msg.getAllRecipients()); // 发送
								
								isSend = true;
							} 
							catch (AddressException e) {
								//sendMap.put(kjUser.getEmail(), "收件地址出错");
								sendMailStatusItem = "收件地址出错";
							}
							catch (MessagingException e) {
								//sendMap.put(kjUser.getEmail(), "邮件发送出错");
								sendMailStatusItem = "邮件发送出错";
							} // 发件人
						}
						else{
							//sendMap.put(thisUserId, "收件人或地址信息不完整");
							sendMailStatusItem = "收件人或地址信息不完整";
						}
						if(isSend){
							sendMailStatusItem = "邮件发送成功";
						}
						else{
							sendMailStatusItem = "邮件发送失败";
						}
					if(j == 0){
						sendMailStatus = sendMailStatusItem;
					}else{
						sendMailStatus = sendMailStatus + "," +sendMailStatusItem;
			        }
		        }
		}
		//发送记录存入数据库
		kjMailForm.setTableIds(id.toString());
		kjMailForm.setMailType(mailType);
		kjMailForm.setFromAddress(TechSystem.getInstance() .getTechConfig().getFromAddress());
		kjMailForm.setCreateDate(null);
			
		kjMailForm.setToAddress(toAddressList);
		kjMailForm.setContent(content);
		kjMailForm.setTitle(title);
			
		kjMailForm.setSendMailStatus(sendMailStatus);
		kjMailForm.setSendSmsStatus(sendSmsStatus);
		
		KjUser sendUser = KjUserDAO.getInstance().findByPk("k19122");
	    KjMailDAO.getInstance().saveKjMail(sendUser,kjMailForm);
	    
	    /*System.out.println("邮件标题："+title);
	    System.out.println("邮件内容："+content);
	    System.out.println("发送状态："+sendMailStatus);
	    System.out.println("邮件类型："+mailType);
	    System.out.println("发送对象："+toAddressList);
	    System.out.println("发送者："+kjMailForm.getFromAddress());
	    */
	}
	
	/*
	 * yzw 2015-11-15 根据系统更新的专利id列表，判断符合发邮件条件的专利
	 */
	public List<Long> getListForSendMail(List<Long> idList, String sendType)
	{
		//测试list
		/*idList.add(553L);
		idList.add(555L);
		idList.add(587L);
		idList.add(663L);
		idList.add(87L);
		idList.add(88L);
		idList.add(93L);*/
		List<Long> patentList = new ArrayList<Long>();
		//System.out.println("判断列表");
		
		for(int i=0;i<idList.size();i++)
		{
			Long patentId = idList.get(i);
			KjPatent kjPatent = KjPatentDao.getInstance().findByPk(patentId);
			String type = kjPatent.getType();
			Long giveUp = kjPatent.getIsGiveup();
			if(sendType.contains(type))
			{ 
				 if(giveUp != null && giveUp == 1)
					 continue;
			     Long remainDays = 10000L; // 当前专利的剩余天数,以最小剩余天数为准
				 Date expireDay = new Date(); // 专利离当前最小剩余天数对应的截止日期(用于找到该截止日期需要提醒的所有费用记录)
				 Map resultMap = KjPatentFeeDao.getInstance().getRemainDayAndExpireDayByPatentId(patentId);
				 remainDays = (Long)resultMap.get("remainDays");
				 expireDay = (Date)resultMap.get("expireDay");
				 
				 Long between_days = 10000L;
				 if(KjMailDAO.getInstance().getMailBetweenDays(type, patentId, expireDay)!= null)
					 between_days =  KjMailDAO.getInstance().getMailBetweenDays(type, patentId, expireDay); //获取最近一次发邮件的日期和专利到期日之间的天数
				 
				 //System.out.println("截止日期"+expireDay);
				 //System.out.println("剩余天数"+remainDays);
				 //System.out.println("上次发邮件距到期日的天数"+between_days);
				 if(remainDays<=15)
				 {
					 if(between_days>15)
						 patentList.add(patentId);
				 }
				 else if(remainDays>15 && remainDays<=29)
				 {
					 if(between_days>29)
						 patentList.add(patentId);
				 }
				 else if(remainDays>29 && remainDays<=60)
				 {
					 if(between_days>60)
						 patentList.add(patentId);
				 }
			}
		}
		return patentList;
	}
	
	public static void main(String[] args)
	{
		PatentFeeTimerTask task = new PatentFeeTimerTask();
		task.run();
	}
	
}
