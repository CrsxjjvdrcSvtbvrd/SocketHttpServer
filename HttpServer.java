import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpServer {
    private int port = 8080;
    private static final Map<String, String> MIME_TYPE = new HashMap<String, String>();
    private List<Router> routers = new ArrayList<>();
    private ServerSocket serverSocket;
    private RouterHandler defaultHandler;

    public HttpServer(){
        this(8080);
    }
    public HttpServer(int port){
        this.port = port;
        defaultHandler = new RouterHandler() {
            @Override
            public void handle(RouterContext context) throws IOException {
                context.response.r404();
                context.response.end();
            }
        };
    }
    public void setDeafultHandler(RouterHandler handler){
        defaultHandler = handler;
    }
    public void setLogHandler(HttpServer.LogHandler handler){
        Log.handler = handler;
    }
    public void start() throws IOException{
        if(serverSocket==null){
            serverSocket = new ServerSocket(port);
            Log.info("HttpServer start on port: "+port);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!isClosed()){
                    try{
                        Socket socket = serverSocket.accept();
                        //Log.info(socket.getInetAddress().getHostAddress()+" connected");
                        service(socket);
                    }catch (IOException e){
                        Log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public boolean isClosed(){
        if(serverSocket==null){
            return true;
        }
        return serverSocket.isClosed();
    }
    public void stop() throws IOException{
        if(serverSocket!=null){
            serverSocket.close();
        }
    }
    public HttpServer addRouter(Router router){
        routers.add(router);
        return this;
    }
    public HttpServer addRouter(String path,RouterHandler handler){
        routers.add(new Router(path,handler));
        return this;
    }

    private void service(Socket socket){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Request request = new Request(socket);
                    Response response = new Response(socket);
                    RouterContext context = new RouterContext();
                    context.request = request;
                    context.response = response;
                    context.socket = socket;
                    Log.info(request.method+" "+request.uri);
                    Router router = getRouter(request.uri);
                    context.router = router;
                    if(router==null){
                        Log.info("no route match");
                        defaultHandler.handle(context);
                    }else{
                        Log.info("router["+router.path+"]matched");
                        router.handle(context);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private Router getRouter(String url){
        String path = url;
        if(url.indexOf("?")>=0){
            path = url.substring(0, url.indexOf("?"));
        }
        for(Router router : routers){
            if(router.check(path)){
                return router;
            }
        }
        return null;
    }

    public static String getContentType(String uri){
        if(uri.lastIndexOf(".")>=0){
            String extension = "";
            if(uri.lastIndexOf("?")>=0){
                extension = uri.substring(uri.lastIndexOf("."),uri.lastIndexOf("?"));
            }else{
                extension = uri.substring(uri.lastIndexOf("."));
            }
            if(MIME_TYPE.containsKey(extension)){
                return MIME_TYPE.get(extension);
            }
        }
        return "application/octet-stream";
    }

    public static class Request{
        public String requestText = "";
        public String method = "GET";
        public String uri = "/";
        public String version = "HTTP/1.1";
        public Map<String, String> headers = new HashMap<String, String>();
        public Map<String, String> params = new HashMap<String, String>();
        public Map<String, String> cookies = new HashMap<String, String>();
        public String body = "";
        public Request(Socket socket){
            InputStream in;
            try{
                in = socket.getInputStream();
                int size = in.available();
                byte[] buffer = new byte[size];
                in.read(buffer);
                requestText = new String(buffer);
                if(requestText.length()>0){
                    String firstLine = requestText.substring(0,requestText.indexOf("\r\n"));
                    method = firstLine.split(" ")[0];
                    uri = firstLine.split(" ")[1];
                    version = firstLine.split(" ")[2];
                    String header = requestText.substring(requestText.indexOf("\r\n")+2);
                    String[] headers = header.split("\r\n");
                    for(String head : headers){
                        String[] keyValue = head.split(": ");
                        this.headers.put(keyValue[0], keyValue[1]);
                    }
                    body = requestText.substring(requestText.indexOf("\r\n\r\n")+4);
                    if(uri.indexOf("?")>=0){
                        String params = uri.substring(uri.indexOf("?")+1);
                        String[] paramsArray = params.split("&");
                        for(String param : paramsArray){
                            if(param.indexOf("=")>0){
                                String[] keyValue = param.split("=");
                                this.params.put(keyValue[0], keyValue[1]);
                            }else{
                                this.params.put(param, "");
                            }
                        }
                    }
                    if(headers.length>0){
                        String cookies = headers[0].substring(headers[0].indexOf("Cookie: ")+"Cookie: ".length());
                        String[] cookiesArray = cookies.split("; ");
                        for(String cookie : cookiesArray){
                            if(cookie.indexOf("=")>0){
                                String[] keyValue = cookie.split("=");
                                this.cookies.put(keyValue[0], keyValue[1]);
                            }else{
                                this.cookies.put(cookie, "");
                            }
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        public String getRequestUrl(){
            if(uri.indexOf("?")>=0){
                return uri.substring(0,uri.indexOf("?")-1);
            }
            return uri;
        }
    }
    public static class Response{
        public StringBuilder responseText = new StringBuilder();
        public StringBuilder bodyText = new StringBuilder();
        public ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        public OutputStream out;
        public Response(Socket socket){
            try{
                out = socket.getOutputStream();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        public Response setStatus(int status,String statusCode){
            responseText.append("HTTP/1.1 ");
            responseText.append(status);
            responseText.append(" ");
            responseText.append(statusCode);
            responseText.append("\r\n");
            return this;
        }
        public Response setContentType(String contentType){
            responseText.append("Content-Type: ");
            responseText.append(contentType);
            responseText.append("\r\n");
            return this;
        }
        public Response contentTextHtml(){
            responseText.append("Content-Type: text/html;charset=utf-8\r\n");
            return this;
        }
        public Response contentPlainText(){
            responseText.append("Content-Type: text/plain;charset=utf-8\r\n");
            return this;
        }
        public Response contentJson(){
            responseText.append("Content-Type: application/json;charset=utf-8\r\n");
            return this;
        }
        public Response contentOctetStream(){
            responseText.append("Content-Type: application/octet-stream;charset=utf-8\r\n");
            return this;
        }
        public Response contentTextCss(){
            responseText.append("Content-Type: text/css;charset=utf-8\r\n");
            return this;
        }
        public Response contentTextJs(){
            responseText.append("Content-Type: text/javascript;charset=utf-8\r\n");
            return this;
        }
        public Response addHeader(String key, String value){
            responseText.append(key+": "+value+"\r\n");
            return this;
        }
        public Response addBody(String body){
            bodyText.append(body);
            return this;
        }
        public Response appendBody(String body){
            return addBody(body+"\r\n");
        }
        public Response addBody(byte[] bytes){
            try{
                bodyStream.write(bytes);
            }catch(IOException e){
                Log.error(e.getMessage());
                e.printStackTrace();
            }
            return this;
        }
        public Response addBody(byte[] bytes, int i, int len) {
            bodyStream.write(bytes, i, len);
            return this;
        }
        public Response setContent(String content){
            bodyText = new StringBuilder(content);
            return this;
        }
        public Response setContent(byte[] bytes){
            try{
                bodyStream.close();
                bodyStream = new ByteArrayOutputStream();
                bodyStream.write(bytes);
            }catch(IOException e){
                Log.error(e.getMessage());
                e.printStackTrace();
            }
            return this;
        }
        public Response r404(){
            setStatus(404, "Not Found");
            setContentType("text/html");
            addBody("<h1>404 Not Found</h1>");
            return this;
        }
        public Response r200(){
            setStatus(200, "OK");
            //setContentType("text/html");
            return this;
        }
        public Response r301(String url){
            setStatus(301, "Moved Permanently");
            addHeader("Location", url);
            return this;
        }
        public Response r403(){
            setStatus(403, "Forbidden");
            setContentType("text/html");
            addBody("<h1>403 Forbidden</h1>");
            return this;
        }
        public Response r500(){
            setStatus(500, "Internal Server Error");
            setContentType("text/html");
            addBody("<h1>500 Internal Server Error</h1>");
            return this;
        }
        public Response r500(String s){
            setStatus(500, "Internal Server Error");
            setContentType("text/html");
            addBody("<h1>500 Internal Server Error</h1>");
            addBody("<p>"+s+"</p>");
            return this;
        }
        public void end() throws IOException{
            String body = bodyText.toString();
            addHeader("Content-Length: ",""+body.length());
            responseText.append("\r\n");
            responseText.append(body);;
            out.write(responseText.toString().getBytes("UTF-8"));
            out.close();
        }
        public void endBinary() throws IOException{
            byte[] bytes = bodyStream.toByteArray();
            addHeader("Content-Length: ",""+bytes.length);
            responseText.append("\r\n");
            out.write(responseText.toString().getBytes("UTF-8"));
            out.write(bodyStream.toByteArray());
            out.close();
        }
        public void end(String body) throws IOException{
            addBody(body);
            end();
        }
        public void endBinary(byte[] body) throws IOException{
            addBody(body);
            endBinary();
            /*addHeader("Content-Length", ""+body.length);
            responseText.append("\r\n");
            out.write(responseText.toString().getBytes("UTF-8"));
            out.write(body);
            out.close();*/
        }
    }

    public static HttpServer.RouterHandler staticFileHandler(File root,String path){
        return new HttpServer.RouterHandler() {
            @Override
            public void handle(HttpServer.RouterContext context) throws IOException {
                String url = context.request.getRequestUrl();
                Log.info("Get: "+url);
                if(path.length()>0&&url.indexOf(path)<0){
                    context.response.r404();
                    context.response.end();
                    return;
                }
                url = url.substring(path.length());
                File file = new File(root, url);
                if(file.isDirectory()){
                    File index = new File(file, "index.html");
                    if(!file.exists()){
                        context.response.r404();
                        context.response.end();
                        Log.info("Get: "+url+" No index.html");
                        return;
                    }
                    file = index;
                }
                if(!file.exists()){
                    context.response.r404();
                    context.response.end();
                    Log.info("Get: "+url+" 404");
                    return;
                }
                Log.info("Get file: "+file.getAbsolutePath());
                String contentType = "application/octet-stream";
                String fileName = file.getName();
                if(fileName.lastIndexOf(".")>=0){
                    String ext = fileName.substring(fileName.lastIndexOf("."));
                    contentType = HttpServer.getContentType(ext);
                }
                if(contentType.indexOf("text")>=0){
                    contentType+=";charset=utf-8";
                }
                Log.info("Get file: "+file.getAbsolutePath()+", contentType: "+contentType);
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] bytes = new byte[1024];
                    int len = 0;
                    while((len=inputStream.read(bytes))>0){
                        context.response.addBody(bytes, 0, len);
                    }
                    inputStream.close();
                    context.response.r200();
                    context.response.setContentType(contentType);
                    context.response.endBinary();
                    Log.info("Get: "+url+" OK");
                } catch (IOException e) {
                    context.response.r500(e.getMessage());
                    Log.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        };
    }

    public static class Router{
        public String path="";
        public RouterHandler handler;
        public Router(String path, RouterHandler handler){
            this.path = path;
            this.handler = handler;
        }
        public void handle(RouterContext context) throws IOException{
            handler.handle(context);
        }
        public Map<String,String> getPathParams(String url){
            Map<String,String> params = new HashMap<String,String>();
            String thiz[] = path.split("/");
            String that[] = url.split("/");
            if(thiz.length!=that.length){
                return params;
            }
            for(int i=0;i<thiz.length;i++){
                if(thiz[i].startsWith(":")){
                    params.put(thiz[i].substring(1), that[i]);
                    continue;
                }
                if(!thiz[i].equals(that[i])){
                    return params;
                }
            }
            return params;
        }
        public boolean check(String url){
            if(path.equals(url)){
                return true;
            }
            String thiz[] = path.split("/");
            String that[] = url.split("/");
            if(thiz.length!=that.length){
                return false;
            }
            for(int i=0;i<thiz.length;i++){
                if(thiz[i].startsWith(":")){
                    if(i+1==thiz.length){
                        return true;
                    }
                    continue;
                }
                if(!thiz[i].equals(that[i])){
                    return false;
                }
            }
            return false;
        }
    }

    public interface RouterHandler{
        public void handle(HttpServer.RouterContext context) throws IOException;
    }

    public static class RouterContext{
        public Request request;
        public Response response;
        public Router router;
        public Socket socket;
    }

    private static class Log{
        static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        static LogHandler handler = new LogHandler(){
            @Override
            public void info(String msg) {
                System.out.println(time()+"[INFO] "+msg);
            }

            @Override
            public void error(String msg) {
                System.out.println(time()+"[ERROR] "+msg);
            }
        };
        public static String time(){
            return formatter.format(new Date());
        }
        public static void info(String msg){
            handler.info(msg);
            //System.out.println(time()+"[INFO] "+msg);
        }
        public static void error(String msg){
            handler.error(msg);
            //System.out.println(time()+"[ERROR] "+msg);
        }
    }
    public interface LogHandler{
        public void info(String msg);
        public void error(String msg);
    }

    //auto init mime type
    static{
        INIT_MINIMAL_MIME_TYPE();
    }

    //minimal mime type
    public static void INIT_MINIMAL_MIME_TYPE() {
        MIME_TYPE.put(".htm", "text/html");
        MIME_TYPE.put(".html", "text/html");
        MIME_TYPE.put(".js", "text/javascript");
        MIME_TYPE.put(".json", "application/json");
        MIME_TYPE.put(".css", "text/css");
        MIME_TYPE.put(".png", "image/png");
        MIME_TYPE.put(".jpg", "image/jpeg");
        MIME_TYPE.put(".jpeg", "image/jpeg");
        MIME_TYPE.put(".gif", "image/gif");
        MIME_TYPE.put(".txt", "text/plain");
        MIME_TYPE.put(".xml", "application/xml");
    }
}
