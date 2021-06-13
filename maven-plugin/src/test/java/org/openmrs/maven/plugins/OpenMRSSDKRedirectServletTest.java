package org.openmrs.maven.plugins;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.protocol.ResponseServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.utility.OpenMRSSDKRedirectServlet;

@RunWith(MockitoJUnitRunner.Strict.class)
public class OpenMRSSDKRedirectServletTest{

    private OpenMRSSDKRedirectServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setup(){
        this.servlet = new OpenMRSSDKRedirectServlet();
    }

    @Test
    public void servletShouldRedirectSimpleRequest() throws IOException, ServletException{
        when(request.getServerPort()).thenReturn(80);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "http://localhost/openmrs");
    }

    @Test
    public void servletShouldRedirectRequest() throws IOException, ServletException{
        when(request.getServerPort()).thenReturn(8080);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "http://localhost:8080/openmrs");
    }

    @Test
    public void servletShouldRedirectAnotherRequest() throws IOException, ServletException{
        when(request.getServerPort()).thenReturn(443);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "http://localhost:443/openmrs");
    }

    @Test
    public void servletShouldRemovePathInfo() throws IOException, ServletException{
        when(request.getServerPort()).thenReturn(80);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getPathInfo()).thenReturn("/login");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "http://localhost/openmrs/login");
    }

    @Test
    public void redirectMaintainsHttps() throws IOException, ServletException{
        when(request.getServerPort()).thenReturn(8443);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("localhost");

        servlet.service(request, response);;

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "https://localhost:8443/openmrs");
    }

}
