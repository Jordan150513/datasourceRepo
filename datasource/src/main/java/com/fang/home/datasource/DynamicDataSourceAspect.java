package com.fang.home.datasource;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 切换数据源Advice
 *@作者 wzx
 *@日期 2017-1-12
 */
@Aspect
@Order(-1)// 保证该AOP在@Transactional之前执行
@Component
public class DynamicDataSourceAspect {

	private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceAspect.class);
	@Autowired
    HttpServletRequest request;

	@Before("@annotation(ds)")
	public void changeDataSource(JoinPoint point, TargetDataSource ds) throws Throwable {
		String dsId = ds.name();
//		if (request.getParameter("istest") != null && request.getParameter("istest").equals("1")){
//			if (DynamicDataSourceContextHolder.containsDataSource("box_"+ds.name())) {
//				dsId = "box_"+ds.name();
//			}
//		}
		if (!DynamicDataSourceContextHolder.containsDataSource(dsId)) {
			logger.error("数据源[{}]不存在，使用默认数据源 > {}", ds.name(), point.getSignature());
		}else {
            logger.debug("Use DataSource : {} > {}", ds.name(), point.getSignature());
            DynamicDataSourceContextHolder.setDataSourceType(dsId);
        }
		
	}
	@After("@annotation(ds)")
    public void restoreDataSource(JoinPoint point, TargetDataSource ds) {
        logger.debug("Revert DataSource : {} > {}", ds.name(), point.getSignature());
        DynamicDataSourceContextHolder.clearDataSourceType();
    }
}
