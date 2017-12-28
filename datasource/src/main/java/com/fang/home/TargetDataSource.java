package com.fang.home;

import java.lang.annotation.*;

/**
 * 在方法上使用，用于指定使用哪个数据源
 *@作者 wzx
 *@日期 2017-1-12
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {
	String name();
}
