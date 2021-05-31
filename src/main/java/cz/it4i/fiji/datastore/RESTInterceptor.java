/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import lombok.extern.log4j.Log4j2;

@Log4j2
@TimeoutingRequest
@Interceptor
public class RESTInterceptor {

	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {
		log.info("intercept: {}", ctx.getMethod());
		return ctx.proceed();
	}
}
