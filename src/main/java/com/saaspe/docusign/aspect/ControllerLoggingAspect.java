package com.saaspe.docusign.aspect;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@EnableAspectJAutoProxy
@RestControllerAdvice
@Configuration
public class ControllerLoggingAspect {

	private final Logger logger = LoggerFactory.getLogger(ControllerLoggingAspect.class);

	@Around("@within(com.saaspe.docusign.aspect.ControllerLogging) || @annotation(com.saaspe.docusign.aspect.ControllerLogging)")
	public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		String methodName = joinPoint.getSignature().getName();
		String className = joinPoint.getTarget().getClass().getSimpleName();
		Object[] args = joinPoint.getArgs();
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = (requestAttributes instanceof ServletRequestAttributes)
				? ((ServletRequestAttributes) requestAttributes).getRequest()
				: null;
		HttpServletResponse response = (requestAttributes instanceof ServletRequestAttributes)
				? ((ServletRequestAttributes) requestAttributes).getResponse()
				: null;
		String traceId = (response != null) ? response.getHeader("X-Trace-Id") : null;
		// Set MDC trace id to the value in response header
		if (traceId != null) {
			org.slf4j.MDC.put("traceId", traceId);
		}
		logger.info("[START] {}::{} [TRACE ID: {}]", className, methodName, traceId);
		if (request != null) {
			logger.info("Request URL: {} {}, From: {}", request.getMethod(), request.getRequestURL(),
					request.getHeader("X-From"));
			logger.info("Request Parameters: {}", request.getParameterMap().entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, entry -> Arrays.toString(entry.getValue()))));
		}
		logger.info("Method Arguments: {}", args);
		Object result;
		try {
			result = joinPoint.proceed();
			logger.info("Method Returned: {}", result);
		} catch (Throwable t) {
			String errorMessage = String.format("Exception in method %s: %s: %s", methodName, t.getMessage(), traceId);
			logger.error(errorMessage, t);
			return errorMessage;
		} finally {
			// Remove MDC trace id
			org.slf4j.MDC.remove("traceId");
			logger.info("[END] {}::{} [TIME ELAPSED: {}ms] [TRACE ID: {}]", className, methodName,
					System.currentTimeMillis() - startTime, traceId);
		}
		return result;
	}

}
