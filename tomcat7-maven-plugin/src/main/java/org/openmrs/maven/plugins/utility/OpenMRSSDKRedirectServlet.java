package org.openmrs.maven.plugins.utility;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.catalina.servlets.DefaultServlet;

/**
 * This Servlet will do a proper redirect from http://localhost/port to http://localhost/port/openmrs
 * and is deployed in the root context in RunTomcat.java 
 * <p>The root context uses the tempDirectory we’re already using and
 * then just routes everything through this servlet.</p>
 * 
 */
public class OpenMRSSDKRedirectServlet extends DefaultServlet {

	private ServletConfig servletConfig;

	@Override
	public void init(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}

	@Override
	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	@Override
	public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException,
			ServletException {
		if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;

			int serverPort = request.getServerPort();
			String port;
			if ("http".equalsIgnoreCase(request.getScheme())) {
				port = serverPort == 80 ? "" :  ":" + serverPort;
			} else if ("https".equalsIgnoreCase(request.getScheme())) {
				port = serverPort == 443 ? "" : ":" + serverPort;
			} else {
				port = ":" + serverPort;
			}

			String queryString = request.getQueryString();
			if (queryString != null) {
				queryString = "?" + queryString;
			} else {
				queryString = "";
			}

			String pathInfo = request.getPathInfo();
			if (pathInfo == null) {
				pathInfo = "";
			}

			String redirectUrl = request.getScheme() + "://" + request.getServerName() +
					port + "/openmrs" + pathInfo + queryString;

			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", redirectUrl);
			return;
		}

		super.service(servletRequest, servletResponse);
	}

	@Override
	public String getServletInfo() {
		return "OpenMRS SDK Redirection Servlet";
	}

	@Override
	public void destroy() {

	}
}
