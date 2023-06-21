package org.openmrs.maven.plugins.utility;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Translates the Maven proxy settings into the environment variables used by Node
 *
 * Note that this has limited support for the no_proxy environment variable, though support for this feature is variable
 * across the Node ecosystem
 */
public class NodeProxyHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(NodeProxyHelper.class);
	
	public static ProxyContext setupProxyContext(MavenSession mavenSession) {
		ProxyContext context = new ProxyContext();
		context.setupProxyContext(mavenSession);
		return context;
	}
	
	public static class ProxyContext {
		
		private ProxyConfig proxyConfig;
		
		public void setupProxyContext(MavenSession mavenSession) {
			if (mavenSession != null &&
					mavenSession.getSettings() != null &&
					mavenSession.getSettings().getProxies() != null &&
					!mavenSession.getSettings().getProxies().isEmpty()) {
				
				proxyConfig = new NodeProxyHelper.ProxyConfig();
				mavenSession.getSettings().getProxies().stream().filter(Proxy::isActive)
						.filter(proxy -> proxy.getProtocol() != null && proxy.getProtocol().equalsIgnoreCase("http"))
						.forEach(proxy -> {
							proxyConfig.setHttpProxy(NodeProxyHelper.ProxyConfig.getProxyUrl(proxy));
							if(StringUtils.isNotBlank(proxy.getNonProxyHosts())) {
								// parse Maven non-proxy host format into no_proxy format
								String[] hosts = proxy.getNonProxyHosts().split("\\|");
								StringBuilder sb = new StringBuilder();
								Arrays.stream(hosts).forEach(host -> {
									String chompedHost = host;
								// Incompatibility: JVM non-proxy hosts support wildcards, but no_proxy only accepts
								// domain suffixes. This means we can convert, e.g. *.openmrs.org but not, e.g.,
								// openmrs.*.openconceptlab.org. In practice, the first form is likely more common.
								if (host.startsWith("*")) {
									chompedHost = chompedHost.substring(1);
								}

								if (chompedHost.contains("*")) {
									LOG.info(
											"Skipping incompatible host string [{}]. Host strings should be in the format *.openmrs.org only.",
											host);
									return;
								}

								sb.append(chompedHost).append(",");
								});
								if (sb.length() > 0) {
								// remove last ,
								proxyConfig.setNoProxy(sb.substring(0, sb.length() - 1));
							}
							}
						});
			}
		}
		
		public void applyProxyContext(List<MojoExecutor.Element> configuration) {
			// disable the frontend plugin's default proxy behaviour
			configuration.add(element("npmInheritsProxyConfigFromMaven", "false"));
			
			if (proxyConfig == null) {
				return;
			}
			
			List<MojoExecutor.Element> environmentVariables = new ArrayList<>(3);
			
			if (proxyConfig.getHttpProxy() != null) {
				environmentVariables.add(element("http_proxy", proxyConfig.getHttpProxy()));
				environmentVariables.add(element("https_proxy", proxyConfig.getHttpProxy()));
			}
			
			if (proxyConfig.getNoProxy() != null) {
				environmentVariables.add(element("no_proxy", proxyConfig.getNoProxy()));
			}
			
			configuration.add(element("environmentVariables", environmentVariables.toArray(new MojoExecutor.Element[0])));
		}
	}
	
	public static class ProxyConfig {
		
		private String httpProxy = null;
		
		private String noProxy = null;
		
		public static String getProxyUrl(Proxy proxy) {
			try {
				return new URI("http", null, proxy.getHost(), proxy.getPort(), null, null, null).toString();
			}
			catch (URISyntaxException e) {
				return null;
			}
		}
		
		public String getHttpProxy() {
			return httpProxy;
		}
		
		public void setHttpProxy(String httpProxy) {
			this.httpProxy = httpProxy;
		}
		
		public String getNoProxy() {
			return noProxy;
		}
		
		public void setNoProxy(String nonProxyHosts) {
			this.noProxy = nonProxyHosts;
		}
	}
	
}
