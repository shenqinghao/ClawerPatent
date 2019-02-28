# ClawerPatent
allFees.jsp 实现单个的专利同步功能，获取登录页面和输入验证码登录分为两部分，使用俩个ajax实现验证是否已经登录。
authorFees.jsp  实现批量同步功能，单个同步的功能都有，并实现ajax加载后台处理进度。
Clawer.java  htmlunit和jsoup实现爬虫功能。
KjPatentAction   跟前台交互的action
PatentFeeTimerTask 批量同步的代码，在action里都实现了，作用不大
authorFees_cookie 利用cookie实现在5分钟内不允许再次点击，单用户如果禁用或清除了cookie就不行了。
KjAuditLogDAO  在日志的数据库中查找含有批量同步和今天日期的数据，用来判断今天是否批量同步过。
