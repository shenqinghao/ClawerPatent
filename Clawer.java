package inc.tech.util;

import inc.tech.patent.dao.KjPatentDao;
import inc.tech.patent.dao.KjPatentFeeDao;
import inc.tech.persistent.DAOException;
import inc.tech.persistent.entity.KjAuditLog;
import inc.tech.persistent.entity.KjPatent;
import inc.tech.persistent.entity.KjPatentFee;
import inc.tech.sys.chop.dao.KjAuditLogDAO;
import inc.tech.sys.common.dao.KjDictObjtypeDAO;
import inc.tech.sys.user.UserBean;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.xml.XmlPage;


public class Clawer {
//	static String USERNAME="CN00230723";
//	static String PASSWORD="27401774";
//	static String QUERYKEY_EXAMPLE="2012102816763";
	static String USERNAME="130534199408200016";
	static String PASSWORD="!Peng712";
	static String QUERYKEY_EXAMPLE="2012102816763";
	static List<HtmlElement> usernameList;
	static List<HtmlElement> passwordList;
	static List<HtmlElement> loginList;
	static List<HtmlElement> codeList;
	final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_11);	
	/**
	 * doc example:
	 * <record>
		<bizhong/>
			<chaxunrq>20151020</chaxunrq>
		<ddhbrid/>
		<ddhm/>
		<dianhuahm/>
		<dingdanzt/>
		<dizhi/>
		<feiyongbz/>
			<feiyongzlmc>发明专利第5年年费</feiyongzlmc>
		<flag/>
		<fuzhubz/>
		<fuzhuwbje/>
			<guidingfyxh>101418568780</guidingfyxh>
		<huilv/>
		<jffs/>
		<jfrlx/>
		<jfrxm/>
		<jiaofeidh/>
		<jiaofeije/>
		<jiaofeilx/>
		<jiaofeirlx/>
		<remark/>
		<rid/>
		<saveFlag/>
		<sheng/>
		<shengchengsj/>
		<shengmc/>
			<shenqingh>2012102816763</shenqingh>
		<shi/>
			<shijiaoje>1200.00</shijiaoje>
		<shimc/>
		<sjhqdd/>
		<sjhqfs/>
		<sjrxm/>
		<total/>
		<userid/>
		<username/>
		<waibije/>
		<wenjianbh/>
		<xiangmushu/>
			<yingjiaofydm>40872083405344</yingjiaofydm>
			<yingjiaoje>1200.00</yingjiaoje>
		<youzhengbm/>
			<yuqirq>20160908</yuqirq>
		</record>
	 * @param PatentId
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws InterruptedException
	 */
	public static Document cponlineGetXmlByPatentId(String PatentId) throws IOException, SAXException, InterruptedException{
			final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_11);
			Document doc = null;
			// htmlunit 对css和javascript的支持不好，所以请关闭之
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(true); //打开JavaScript
			webClient.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
			webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			webClient.waitForBackgroundJavaScriptStartingBefore(50);//wait js execution for 2 seconds
	        
			//open cookie manager for saving username and password 
			webClient.getCookieManager().setCookiesEnabled(true);
			CookieManager cm = new CookieManager();
	        webClient.setCookieManager(cm);
	        
	        // get cponline's login page 
//	        HtmlPage cponline = webClient
//					.getPage("http://app.cponline.gov.cn/login_frame/login.jsp");
	        HtmlPage cponline = webClient.getPage("http://interactive.cponline.cnipa.gov.cn/txn999998.do");
	        HtmlElement cponlineEle = cponline.getDocumentElement();
//	        System.out.println(cponline.asXml());
			
			//get username input 、 password and login button input element
	        List<HtmlElement> usernameList = cponlineEle.getElementsByAttribute("input", "id", "username1");
	        List<HtmlElement>  passwordList = cponlineEle.getElementsByAttribute("input", "id", "password");
			List<HtmlElement> loginList = cponlineEle.getElementsByAttribute("input", "id", "dldwfw");
			List<HtmlElement> codeList = cponlineEle.getElementsByAttribute("input", "id", "securityCode1"); //yanzhengma
			HtmlImage imgCode = (HtmlImage)cponlineEle.getElementsByAttribute("img", "id", "Verify1").get(0);
			
			System.out.println(usernameList.size());
			System.out.println(passwordList.size());
			System.out.println(loginList.size());
			System.out.println(codeList.size());

			String path ="D:\\workspacetech\\tech\\yzm.jpg";
			File file=new File(path);
			imgCode.saveAs(file);
			//String path ="D:\\workspacetech\\tech\\yzm.jpg";
			String valCode = "";
//			try {
//				 valCode = new OCR_tesseract().recognizeText(new File(path), "jpg");
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			HtmlPage loginPage;
//			Scanner scanner=new Scanner(System.in);
			HtmlInput codeinput =(HtmlInput) codeList.get(0);
//			String codes=scanner.nextLine();
			codeinput.setValueAttribute(valCode);
			if(usernameList.size()==1 && passwordList.size()==1 && loginList.size() ==1){// succeed get the three elements
				HtmlInput usernameInput =(HtmlInput) usernameList.get(0);
				HtmlInput passwordInput =(HtmlInput) passwordList.get(0);
				usernameInput.setValueAttribute(USERNAME);
				passwordInput.setValueAttribute(PASSWORD);
				System.out.println("-----------");
				
				loginPage=(HtmlPage)loginList.get(0).click();
				Thread.sleep(20); //waiting for click button clicking  and  execute the JavaScript
//				System.out.print(loginPage.asXml()+"+++++++++++++++++++++++++++++++++++++++++++++");
				
				
				//HtmlPage mainPage = webClient
				//		.getPage("http://app.cponline.gov.cn/txn04d000017.do?path=&cmd=网上缴费");//1
				
				//directly explore search HTML Page 
				
//				HtmlPage searchPage = webClient
//						.getPage("http://app.cponline.gov.cn/app/02_zxfw/zxzf/query-ds_zxzf.jsp");//1
				HtmlPage searchPage = webClient.getPage("http://app.cponline.cnipa.gov.cn/main.jsp/txn04d000017.do?path=&cmd=网上缴费");
				HtmlElement searchPageEle = searchPage.getDocumentElement();
//				System.out.print(searchPage.asXml());
				
				HtmlPage queryPage = webClient.getPage("http://app.cponline.cnipa.gov.cn/app/02_zxfw/zxzf/query-ds_zxzf.jsp");
				HtmlElement queryPageEle = queryPage.getDocumentElement();
				System.out.print(queryPage.asXml());
				//get search text input and click button
//				List<HtmlElement> searchContentList = searchPageEle.getElementsByAttribute("input", "id", "select-key:shenqingh");
//				List<HtmlElement> buttonList = searchPageEle.getElementsByAttribute("input", "class", "menu");
				List<HtmlElement> searchContentList = queryPageEle.getElementsByAttribute("input", "id", "select-key:shenqingh");
				List<HtmlElement> buttonList = queryPageEle.getElementsByAttribute("input", "class", "menu");
				System.out.print(searchContentList.size());
				System.out.print(buttonList.size());
				
				if(searchContentList.size() ==1 && buttonList.size()==1){ // succeed get the two elements
					HtmlInput searchContent = (HtmlInput)searchContentList.get(0);
					HtmlInput button = (HtmlInput)buttonList.get(0);
					searchContent.setValueAttribute(PatentId);
					HtmlPage dataPage = (HtmlPage)button.click();
					Thread.sleep(40);
					// don't know the usage of 'stamp' element
					//HtmlElement stampEle = dataPage.getDocumentElement().getElementsByTagName("stamp").get(0);
					XmlPage datadetailXmlPage = null;
					
					// execution AJAX and synchronization
					//get DATA XML
					datadetailXmlPage = webClient
							.getPage("http://app.cponline.cnipa.gov.cn/txn04d000001.ajax?select-key:shenqingh="+PatentId+"&charset-encoding=UTF-8");//1
					

					doc = Jsoup.parse(datadetailXmlPage.getDocumentElement().asText());
					System.out.print(doc.toString());
					
				}
			}
			return doc;
	}
	public static Document quickGetXmlByPatentId(String PatentId) throws IOException, SAXException, InterruptedException{
		final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_11);
		Document doc = null;
		// htmlunit 对css和javascript的支持不好，所以请关闭之
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true); //打开JavaScript
		webClient.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScriptStartingBefore(50);//wait js execution for 2 seconds
        
		//open cookie manager for saving username and password 
		webClient.getCookieManager().setCookiesEnabled(true);
		CookieManager cm = new CookieManager();
        webClient.setCookieManager(cm);
        
        // get cponline's login page 
        HtmlPage cponline = webClient
				.getPage("http://app.cponline.gov.cn/login_frame/login.jsp");
        HtmlElement cponlineEle = cponline.getDocumentElement();
		
		//get username input 、 password and login button input element
		List<HtmlElement> usernameList = cponlineEle.getElementsByAttribute("input", "id", "username");
		List<HtmlElement> passwordList = cponlineEle.getElementsByAttribute("input", "id", "password");
		List<HtmlElement> loginList = cponlineEle.getElementsByAttribute("div", "class", "btnlogin");
		HtmlPage loginPage;
		if(usernameList.size()==1 && passwordList.size()==1 && loginList.size() ==1){// succeed get the three elements
			HtmlInput usernameInput =(HtmlInput) usernameList.get(0);
			HtmlInput passwordInput =(HtmlInput) passwordList.get(0);
			usernameInput.setValueAttribute(USERNAME);
			passwordInput.setValueAttribute(PASSWORD);
			
			loginPage=(HtmlPage)loginList.get(0).click();
			Thread.sleep(20); //waiting for click button clicking  and  execute the JavaScript
			
			//HtmlPage mainPage = webClient
			//		.getPage("http://app.cponline.gov.cn/txn04d000017.do?path=&cmd=网上缴费");//1
			
			//directly explore search HTML Page 
			
			XmlPage datadetailXmlPage = null;
			
			// execution AJAX and synchronization
			//get DATA XML
			datadetailXmlPage = webClient
					.getPage("http://app.cponline.gov.cn/txn04d000001.ajax?select-key:shenqingh="+PatentId+"&charset-encoding=UTF-8");//1
			doc = Jsoup.parse(datadetailXmlPage.asXml());
				
			
		}
		return doc;
}
	
	public WebClient preLoginCponline()throws IOException, SAXException, InterruptedException{
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true); //打开JavaScript
		webClient.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScriptStartingBefore(50);//wait js execution for 2 seconds
		
        
		//open cookie manager for saving username and password 
		webClient.getCookieManager().setCookiesEnabled(true);
		CookieManager cm = new CookieManager();
        webClient.setCookieManager(cm);
        
        
        // get cponline's login page 
        //shenqignhao  网址修改
        HtmlPage cponline = webClient.getPage("http://interactive.cponline.cnipa.gov.cn/txn999998.do");
        HtmlElement cponlineEle = cponline.getDocumentElement();
		
		//get username input 、 password and login button input element

		// shenqinghao 获取元素
		 usernameList = cponlineEle.getElementsByAttribute("input", "id", "username1"); 
	     passwordList = cponlineEle.getElementsByAttribute("input", "id", "password");
	 	 loginList = cponlineEle.getElementsByAttribute("input", "id", "dldwfw");
		 codeList = cponlineEle.getElementsByAttribute("input", "id", "securityCode1"); //yanzhengma
		HtmlImage imgCode = (HtmlImage)cponlineEle.getElementsByAttribute("img", "id", "Verify1").get(0);
		
//		System.out.println(usernameList.size());
//		System.out.println(passwordList.size());
//		System.out.println(loginList.size());
//		System.out.println(codeList.size());
		//shenqinghao  获取验证码
		
		String uploadPath = SysConfig.UPLOAD_PATH;
		String imagePath = uploadPath.substring(0,uploadPath.indexOf("techUpload"))+"tech/images/";
		File file=new File(imagePath +"yzm.jpg");
		imgCode.saveAs(file);
//		if(file == null){
//			System.out.print("jpg is null");
//		}
//		System.out.print(file.getCanonicalPath());
		return webClient;
		
	}
	public  WebClient newLoginCponline(String valCode)throws IOException, SAXException, InterruptedException{
		HtmlInput codeinput =(HtmlInput) codeList.get(0);
		codeinput.setValueAttribute(valCode);
		HtmlPage loginPage;
		if(usernameList.size()==1 && passwordList.size()==1 && loginList.size() ==1){// succeed get the three elements
			HtmlInput usernameInput =(HtmlInput) usernameList.get(0);
			HtmlInput passwordInput =(HtmlInput) passwordList.get(0);
			usernameInput.setValueAttribute(USERNAME);
			passwordInput.setValueAttribute(PASSWORD);
			
			loginPage=(HtmlPage)loginList.get(0).click();
			Thread.sleep(20);
			
			return webClient;
		}else{
			return null;
		}
	}
	public  WebClient loginCponline()throws IOException, SAXException, InterruptedException{
		  
		
		
		final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_11);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true); //打开JavaScript
		webClient.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScriptStartingBefore(50);//wait js execution for 2 seconds
        
		//open cookie manager for saving username and password 
		webClient.getCookieManager().setCookiesEnabled(true);
		CookieManager cm = new CookieManager();
        webClient.setCookieManager(cm);
        
        // get cponline's login page 
        //shenqignhao  网址修改
        HtmlPage cponline = webClient.getPage("http://interactive.cponline.cnipa.gov.cn/txn999998.do");
        HtmlElement cponlineEle = cponline.getDocumentElement();
		
		//get username input 、 password and login button input element

		// shenqinghao 获取元素
		List<HtmlElement> usernameList = cponlineEle.getElementsByAttribute("input", "id", "username1"); 
	    List<HtmlElement> passwordList = cponlineEle.getElementsByAttribute("input", "id", "password");
	 	List<HtmlElement> loginList = cponlineEle.getElementsByAttribute("input", "id", "dldwfw");
		List<HtmlElement> codeList = cponlineEle.getElementsByAttribute("input", "id", "securityCode1"); //yanzhengma
		HtmlImage imgCode = (HtmlImage)cponlineEle.getElementsByAttribute("img", "id", "Verify1").get(0);
		
//		System.out.println(usernameList.size());
//		System.out.println(passwordList.size());
//		System.out.println(loginList.size());
//		System.out.println(codeList.size());
		//shenqinghao  获取验证码
		
//		String path ="D://image//yzm.jpg";
		File file=new File("D:/tomcat6.0.48/webapps/tech/images/yzm.jpg");
		imgCode.saveAs(file);
		if(file == null){
			System.out.print("jpg is null");
		}
		System.out.print(file.getCanonicalPath());
		/*
		String path ="D:\\tomcat6.0.48\\bin\\yzm.jpg";
		String valCode = "";
		try {
			 valCode = new OCR_tesseract().recognizeText(new File(path), "jpg");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		HtmlInput codeinput =(HtmlInput) codeList.get(0);
		Scanner scanner=new Scanner(System.in);
		String valCode=scanner.nextLine();
		codeinput.setValueAttribute(valCode);
		
		HtmlPage loginPage;
		if(usernameList.size()==1 && passwordList.size()==1 && loginList.size() ==1){// succeed get the three elements
			HtmlInput usernameInput =(HtmlInput) usernameList.get(0);
			HtmlInput passwordInput =(HtmlInput) passwordList.get(0);
			usernameInput.setValueAttribute(USERNAME);
			passwordInput.setValueAttribute(PASSWORD);
			
			loginPage=(HtmlPage)loginList.get(0).click();
			Thread.sleep(20);
			
			return webClient;
			
//			if(loginPage.asText().contains("中国专利电子审批系统"))//登陆成功
//			{
//				return webClient;
//			}else{//登陆失败
//				return null;
//			}
			
		}else{//登陆失败
			return null;
		}
		
		
	}
	public  Document getDocOfCponline(WebClient webClient,String patentNo)throws IOException{
		
		Document doc = null;
		XmlPage datadetailXmlPage = null;
		if(patentNo!=null && !"".equals(patentNo)){
			//
			datadetailXmlPage = webClient
			.getPage("http://app.cponline.cnipa.gov.cn/txn04d000001.ajax?select-key:shenqingh="+patentNo+"&charset-encoding=UTF-8");//1
			
			doc = Jsoup.parse(datadetailXmlPage.asXml());
			String errorCode = "";
			String successNumber = "";
			if(!doc.select("error-code").isEmpty() && doc.select("error-code").get(0).text().equals("000000")){
				errorCode = "000000";
			}
//			System.out.println(doc.select("success-number").toString());
			if(!doc.select("success-number").isEmpty() && doc.select("success-number").get(0).text().equals("1")){
				successNumber = "1";
			}
			if(errorCode.equals("000000")&&successNumber.equals("1"))
			{
				return doc;
			}else{
				return null;
			}
			//System.out.println(doc.text());
			
		}else{
			return null;
		}
		
	}
	public static ClawerCponlinePatentModel getOneRecord(Element e){
		ClawerCponlinePatentModel oneRecord = new ClawerCponlinePatentModel();
		oneRecord.setChaxunrq(e.select("chaxunrq").get(0).text());
		oneRecord.setFeiyongzlmc(e.select("feiyongzlmc").get(0).text());
		oneRecord.setGuidingfyxh(e.select("guidingfyxh").get(0).text());
		oneRecord.setShenqingh(e.select("shenqingh").get(0).text());
		oneRecord.setShijiaoje(e.select("shijiaoje").get(0).text());
		oneRecord.setYingjiaofydm(e.select("yingjiaofydm").get(0).text());
		oneRecord.setYingjiaoje(e.select("yingjiaoje").get(0).text());
		oneRecord.setYuqirq(e.select("yuqirq").get(0).text());
		return oneRecord;
	}
	public  boolean synPatentAndFee(Document patentDoc,KjPatent kjp,UserBean userbean, int synButtonState)throws DAOException, IOException,SAXException ,InterruptedException , ParseException{
		//KjPatentDao patentDao = KjPatentDao.getInstance();//专利
		if(kjp==null)return false;
		String patentId = kjp.getPatentId().toString();  
//		System.out.println("专利年费同步测试：patentId-->"+patentId);
		
		String patentNo = kjp.getPatentNo().replaceAll("ZL", "").replaceAll(" ", "");
		if(patentNo.contains("."))patentNo=patentNo.replace(".", "");
//		System.out.println("专利年费同步测试：patentNo-->"+patentNo);
		
		KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfs=patentFeedao.findByPIdAndConfig(Long.valueOf(patentId),11L);
		
		
		
		
		long tic,tid;
		tic = System.currentTimeMillis(); 
		List<ClawerCponlinePatentModel > yearFeeList = new ArrayList<ClawerCponlinePatentModel >();//年费
		List<ClawerCponlinePatentModel > lateFeeList = new ArrayList<ClawerCponlinePatentModel >();//滞纳金
		List<ClawerCponlinePatentModel > huifuFeeList = new ArrayList<ClawerCponlinePatentModel >();//恢复金
		//解析获取的xml 文档  parse xml doc
		
		for (Element e : patentDoc.select("record")) {
	        //System.out.println(e.text());
			if(e.getElementsByTag("feiyongzlmc")!=null && e.getElementsByTag("feiyongzlmc").size()>0){//feiyongzlmc
				
				if(e.getElementsByTag("feiyongzlmc").get(0).text().indexOf("滞纳")>-1){
					ClawerCponlinePatentModel oneLateFee = getOneRecord(e);
					

			        lateFeeList.add(oneLateFee);
				}else if(e.getElementsByTag("feiyongzlmc").get(0).text().indexOf("恢复")>-1){//恢复金 ，可能需要更改
					ClawerCponlinePatentModel oneLateFee = getOneRecord(e);

					huifuFeeList.add(oneLateFee);
				}else if(e.getElementsByTag("feiyongzlmc").get(0).text().indexOf("年费")>-1)//实用新型专利第3年年费
				{
					ClawerCponlinePatentModel oneYearFee = getOneRecord(e);
					
					Pattern pattern = Pattern.compile("[0-9]{1,2}");  
			        Matcher matcher = pattern.matcher(oneYearFee.getFeiyongzlmc());
					if(matcher.find()){
						String year = matcher.group(0);
						oneYearFee.setYear(year);
					}
					
					yearFeeList.add(oneYearFee);
				}
					
			}
	    }
		
		
		
		//sort
		Collections.sort(yearFeeList, new Comparator<ClawerCponlinePatentModel>() {
            public int compare(ClawerCponlinePatentModel arg0, ClawerCponlinePatentModel arg1) {
                return Integer.valueOf(arg0.getYear()).compareTo(Integer.valueOf(arg1.getYear()));
            }
        });
		
		
		Collections.sort(kpfs, new Comparator<KjPatentFee>() {
            public int compare(KjPatentFee arg0, KjPatentFee arg1) {
                return arg0.getYear().compareTo(arg1.getYear());
            }
        });
		tid = System.currentTimeMillis(); 
//		System.out.println("解析及排序时间： "+(tid-tic)+"ms");
		
		long startTime=System.currentTimeMillis();   //获取开始时间  
		
		boolean synresult = exePatentAndFee( yearFeeList, lateFeeList,huifuFeeList,kpfs,patentDoc, kjp,userbean ,synButtonState);
		long endTime=System.currentTimeMillis(); //获取结束时间  
//		System.out.println("同步时间： "+(endTime-startTime)+"ms");
		if(synresult){
			return true;
		}else{
			return false;
		}
		
	}
	public boolean exePatentAndFee(List<ClawerCponlinePatentModel > yearFeeList, 
									List<ClawerCponlinePatentModel > lateFeeList,
									List<ClawerCponlinePatentModel > huifuFeeList,
									List<KjPatentFee> kpfs,
									Document patentDoc, 
									KjPatent kjp,
									UserBean userbean,
									int synButtonState)throws DAOException, IOException,SAXException ,InterruptedException , ParseException{
		
		KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();//年费
		KjPatentDao patentDao = KjPatentDao.getInstance();//专利
		
		String patentId = kjp.getPatentId().toString();  
		//System.out.println("专利年费同步测试：patentId-->"+patentId);
		
		//String patentNo = kjp.getPatentNo();
		//if(patentNo.contains("."))patentNo=patentNo.replace(".", "");
		//System.out.println("专利年费同步测试：patentNo-->"+patentNo);
		
		
		// 写日志
		KjAuditLog auditlog = new KjAuditLog();
		auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));
		auditlog.setObjId( Long.valueOf(patentId));
		//日志类型
		if(synButtonState == 1){
			auditlog.setOper("批量同步数据");
		}else{
			auditlog.setOper("单个同步数据");
		}
		
		String synType = "定时自动同步:";
		String operMsg = "";
		if(userbean!=null){
			auditlog.setOperUser(userbean.getUid());
			synType = userbean.getName()+"同步:";
		}
		auditlog.setOperTime(DateUtil.getDate(new Date()));   //时间设置 
		
		
		
		// Synchronize rules 
		// 1. 早于专利网上第一条数据的 全部标为 已缴费
		if(yearFeeList.size()>0)
		{
			
			// 更新数据库中的数据  update record in the database
			int firstYear = Integer.valueOf(yearFeeList.get(0).getYear());
			for(int i=0;i<kpfs.size();i++)
			{
				if(kpfs.get(i).getYear()<firstYear ){ //小于网站上第一年的 且 未缴费的
					if(kpfs.get(i).getIsPaid()==0L){
						kpfs.get(i).setIsPaid(1L);
						patentFeedao.merge(kpfs.get(i));
//						auditlog.setOperTime(DateUtil.getDate(new Date()));
						operMsg = synType+kpfs.get(i).getYear()+"年费 状态由 未缴费-->已缴费" ;
//						KjAuditLogDAO.getInstance().save(auditlog);
					}
				}else if(kpfs.get(i).getYear()>=firstYear ) //大于或等于网站上第一年的
				{
					Boolean isYearFeeUpdate = false;
					//找到对应年份的记录 在list中的索引
					int tempIndex =kpfs.get(i).getYear().intValue()-firstYear;
					//ClawerCponlinePatentModel releatedYearFee = yearFeeList.get(tempIndex);
					
					
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date date = sdf.parse(yearFeeList.get(tempIndex).getYuqirq());
					SimpleDateFormat kpsdf = new SimpleDateFormat("yyyyMMdd");
					
					Date kpDate = null;
					if(kpfs.get(i).getExpiryDay()!=null)
						kpDate = kpsdf.parse(kpfs.get(i).getExpiryDay().toString());
					if(kpfs.get(i).getIsPaid()==1L){
						kpfs.get(i).setIsPaid(0L);
						isYearFeeUpdate=true;
						
					}
					if(kpDate==null || (kpDate!=null && kpDate.compareTo(date)!=0)){
						
						kpfs.get(i).setExpiryDay(date);
						isYearFeeUpdate=true;
						
					}
					
					if(kpfs.get(i).getAmount()==null || 
							(kpfs.get(i).getAmount()!=null 
									&& Math.abs(kpfs.get(i).getAmount()-Double.valueOf(yearFeeList.get(tempIndex).getYingjiaoje()))>0.01d)
							)
					{
						kpfs.get(i).setAmount(Double.valueOf(yearFeeList.get(tempIndex).getYingjiaoje()));
						isYearFeeUpdate=true;
					}
					if(isYearFeeUpdate){
						
						patentFeedao.merge(kpfs.get(i));
//						auditlog.setOperTime(DateUtil.getDate(new Date()));
						operMsg = synType + kpfs.get(i).getYear()+"年费 状态由 已缴费-->未缴费" ;
//						KjAuditLogDAO.getInstance().save(auditlog);
					}
				}	
				//System.out.println(kpfs.get(i).getPatentId().getPatentId()+" "+kpfs.get(i).getYear());
			}
			// 添加数据到数据库中  add data to the database
			for(int i=0;i<yearFeeList.size();i++)
			{
				
				if(patentFeedao.findAnnelFee(11L, Long.valueOf(patentId), Long.valueOf(yearFeeList.get(i).getYear()))==null){
					
					double count = Double.valueOf(yearFeeList.get(i).getYingjiaoje());
					Long configId = 11L;
					
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						Date date = sdf.parse(yearFeeList.get(i).getYuqirq());
						
					patentFeedao.saveYearFee(Long.valueOf(patentId), count, 11L, Long.valueOf(yearFeeList.get(i).getYear()),date,0L);
					
//					auditlog.setOperTime(DateUtil.getDate(new Date()));
					operMsg = synType+" 新增"+yearFeeList.get(i).getYear()+"年费记录 状态为 未缴费";
				}
			}
		}
		auditlog.setOperMsg(operMsg);
		KjAuditLogDAO.getInstance().save(auditlog);
		//System.out.println(kjp.getState());
		
		String patent_state = "";
		boolean isOutOfDate = false; //判断是否过期
		//3.查询出专利的所有年费（滞纳金、恢复请求费）后，如果没有任何信息，如果以前曾经有缴费或未缴费的记录（科技系统中有专利费用信息），则应为“过期”（即系统中的无效55）。
		if(yearFeeList.size()==0 && lateFeeList.size()==0 && huifuFeeList.size()== 0 
				&& kpfs!=null && kpfs.size()>0){
			// 4.如果科技系统中第19年年费已交，专利网上没有年费数据显示“无费用信息”的，则状态更改为“有效期届满56”。（1.3的特例）
			
			if(kpfs.get(kpfs.size()-1)!=null &&  kpfs.get(kpfs.size()-1).getYear()!=null &&  
				(	(kpfs.get(kpfs.size()-1).getYear()>=20 && kjp.getType().equalsIgnoreCase("1") ) 
					|| (kpfs.get(kpfs.size()-1).getYear()>=10) && !kjp.getType().equalsIgnoreCase("1"))
					&& kpfs.get(kpfs.size()-1).getIsPaid()==1L ){
				if(!kjp.getState().equals("56")){
					kjp.setState("56");
					patent_state = "有效期届满";
					isOutOfDate=true;
				}
			}
			else if( !kjp.getState().equals("55"))
			{
				//kjp.get(0).setStateName("过期");
				kjp.setState("55");
				patent_state = "过期";
				isOutOfDate=true;
			}
		}
		
		if(!isOutOfDate)//非过期 并且 不是有效期满的
		{
			//2.1如果系统中专利为维持中，专利网上有滞纳金的，同步数据后，专利状态更改为“滞纳91” --- 逻辑有问题
			if(lateFeeList.size()>0 && kjp!=null){
				//如果网上 有滞纳金，则先同步数据。后更改状态
				List<KjPatentFee> kpLateFee=patentFeedao.findByPIdAndConfig(Long.valueOf(patentId),12L);
				if(kpLateFee!=null && kpLateFee.size()>0  ){//系统中有滞纳金，则更改
					boolean isLateFeeUpdate =false;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date date = sdf.parse(lateFeeList.get(0).getYuqirq());
					SimpleDateFormat kpsdf = new SimpleDateFormat("yyyyMMdd");
					Date kpDate = kpsdf.parse(kpLateFee.get(0).getExpiryDay().toString());
					
					if(kpLateFee.get(0).getIsPaid()==1L){//滞纳金状态不对
						kpLateFee.get(0).setIsPaid(0L);
						isLateFeeUpdate=true;
					}
					if(kpDate.compareTo(date)!=0){
						kpLateFee.get(0).setExpiryDay(date);//设置日期
						isLateFeeUpdate=true;
					}
					if(kpLateFee.get(0).getAmount()==null || 
							(kpLateFee.get(0).getAmount()!=null && 
							Math.abs(kpLateFee.get(0).getAmount()- Double.valueOf(lateFeeList.get(0).getYingjiaoje()))>0.01d
									)
							)
					{
						kpLateFee.get(0).setAmount(Double.valueOf(lateFeeList.get(0).getYingjiaoje()));//设置应交金
						isLateFeeUpdate=true;
					}
					
					if(isLateFeeUpdate){
						patentFeedao.merge(kpLateFee.get(0));
						auditlog.setOperTime(DateUtil.getDate(new Date()));
						auditlog.setOperMsg(synType+" 同步专利网费用数据，更新滞纳金 已缴费 --> 未缴费" );
						KjAuditLogDAO.getInstance().save(auditlog);
					}
					
				}else if(kpLateFee==null || kpLateFee.size()==0){//没有则新增
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date date = sdf.parse(lateFeeList.get(0).getYuqirq());
					patentFeedao.saveYearFee(
							Long.valueOf(patentId), 
							Double.valueOf(lateFeeList.get(0).getYingjiaoje()), 
							12L, 
							0l, 
							date,
							0L);
					
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(synType+" 同步专利网费用数据，新增滞纳金记录，状态：未缴费" );
					KjAuditLogDAO.getInstance().save(auditlog);
				}
				
				if(kjp.getState().equals("33")){
					kjp.setState("91");
					patent_state = "滞纳";
				}
				
			}
			//2.2相反，如果系统中状态为滞纳，专利网上没有滞纳金，则同步数据后，状态更改为“维持中33”
			else if( lateFeeList.size()== 0 && kjp!=null ){
				List<KjPatentFee> kpLateFee=patentFeedao.findByPIdAndConfig(Long.valueOf(patentId),12L);
				
				if(kpLateFee!=null && kpLateFee.size()>0 && kpLateFee.get(0).getIsPaid()==0L){
					
					kpLateFee.get(0).setIsPaid(1L);
					patentFeedao.merge(kpLateFee.get(0));
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(synType+" 同步专利网费用数据，更新滞纳金 未缴费 --> 已缴费" );
					KjAuditLogDAO.getInstance().save(auditlog);
				}
				
				if(kjp.getState().equals("91")){
					kjp.setState("33");
					patent_state = "维持中";
					//System.out.println("test");
					
				}
				
			}
			//恢复金部分 
			List<KjPatentFee> kpHuifuFee=patentFeedao.findByPIdAndConfig(Long.valueOf(patentId),14L);//获取系统中的恢复金
			if(huifuFeeList.size()>0 && kjp!=null   ){//如果网站上有恢复金，说明有恢复金 先同步数据，//
				
				//同步数据操作
				if(kpHuifuFee!=null && kpHuifuFee.size()>0 ){// 如果数据库中存在恢复金的记录，且状态是已缴费，则更改状态为未缴费
					boolean isHuifuFeeUpdate =false;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date date = sdf.parse(huifuFeeList.get(0).getYuqirq());
					SimpleDateFormat kpsdf = new SimpleDateFormat("yyyyMMdd");
					Date kpDate = kpsdf.parse(kpHuifuFee.get(0).getExpiryDay().toString());
					
					
					if(kpHuifuFee.get(0).getIsPaid()==1L){
						kpHuifuFee.get(0).setIsPaid(0L);
						isHuifuFeeUpdate=true;
					}
					if(kpDate.compareTo(date)!=0){
						kpHuifuFee.get(0).setExpiryDay(date);//设置日期
						isHuifuFeeUpdate=true;
					}
					if((kpHuifuFee.get(0).getAmount()==null) 
							|| (kpHuifuFee.get(0).getAmount() !=null 
									&& Math.abs(kpHuifuFee.get(0).getAmount()-Double.valueOf(huifuFeeList.get(0).getYingjiaoje()))>0.01d)
							)
					{
						kpHuifuFee.get(0).setAmount(Double.valueOf(huifuFeeList.get(0).getYingjiaoje()));//设置应交金
						isHuifuFeeUpdate=true;
					}
					
					if(isHuifuFeeUpdate){
						patentFeedao.merge(kpHuifuFee.get(0));
						auditlog.setOperTime(DateUtil.getDate(new Date()));
						auditlog.setOperMsg(synType+" 同步专利网费用数据，更新恢复金 已缴费 --> 未缴费" );
						KjAuditLogDAO.getInstance().save(auditlog);
					}
					
				}else if(kpHuifuFee==null || kpHuifuFee.size()==0){ // 数据库中没有恢复金的记录，则增加一条恢复金记录，且状态是未缴费 
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date date = sdf.parse(lateFeeList.get(0).getYuqirq());
					patentFeedao.saveYearFee(Long.valueOf(patentId), 
							Double.valueOf(lateFeeList.get(0).getYingjiaoje()), 
							14L, 
							0l, 
							date,
							0L);
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(synType+" 同步专利网费用数据，添加恢复金 状态： 未缴费" );
					KjAuditLogDAO.getInstance().save(auditlog);
				}
				//更改状态
				if(!kjp.getState().equals("92")){
//					kjp.setState("92");
//					patent_state = "恢复";
				}
				
			}
			//相反，如果系统中状态为有恢复金，专利网上没有没有恢复金，则将系统中的恢复金 设置为已缴费
			else if( huifuFeeList.size()== 0 && kpHuifuFee!=null && kpHuifuFee.size()>0  && kjp!=null   ){
				
				if(kpHuifuFee.get(0).getIsPaid()==0L){//如果恢复金未缴纳，则设置为已缴纳
					kpHuifuFee.get(0).setIsPaid(1L);
					patentFeedao.merge(kpHuifuFee.get(0));
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(synType+" 同步专利网费用数据，更新恢复金 未缴费 --> 已缴费" );
					KjAuditLogDAO.getInstance().save(auditlog);
				}
				//恢复金状态更改...暂时留白
			}
		}
		
		
		
		if(!patent_state.equals(""))
		{
			
			patentDao.merge(kjp);
			
			//System.out.println(kjp.getState()+"....test");
			auditlog.setOperTime(DateUtil.getDate(new Date()));
			auditlog.setOperMsg(synType+" 同步专利网费用数据：该专利改为 "+patent_state);
			KjAuditLogDAO.getInstance().save(auditlog);
		}
		
		
		
		return true;
	}

			
	
	
	public static void main(String[] args) throws IOException, SAXException, InterruptedException {
			Document doc = Clawer.cponlineGetXmlByPatentId(QUERYKEY_EXAMPLE);
			for (Element e : doc.select("record")) {
		        System.out.println(e.text());
		    }
			System.out.println("done!");
	}

}
