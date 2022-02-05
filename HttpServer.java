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
        public Response contextPlainText(){
            responseText.append("Content-Type: text/plain;charset=utf-8\r\n");
            return this;
        }
        public Response contextJson(){
            responseText.append("Content-Type: application/json;charset=utf-8\r\n");
            return this;
        }
        public Response contextOctetStream(){
            responseText.append("Content-Type: application/octet-stream;charset=utf-8\r\n");
            return this;
        }
        public Response contextTextCss(){
            responseText.append("Content-Type: text/css;charset=utf-8\r\n");
            return this;
        }
        public Response contextTextJs(){
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
    /*
    //auto init mime type
    static{
        INIT_FULL_MIME_TYPE();
    }
    */
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
    public static void INIT_FULL_MIME_TYPE() {
        MIME_TYPE.put(".svg", "image/svg+xml");
        MIME_TYPE.put(".xml", "application/xml");
        MIME_TYPE.put(".323", "text/h323");
        MIME_TYPE.put(".3gp", "video/3gpp");
        MIME_TYPE.put(".aab", "application/x-authoware-bin");
        MIME_TYPE.put(".aam", "application/x-authoware-map");
        MIME_TYPE.put(".aas", "application/x-authoware-seg");
        MIME_TYPE.put(".acx", "application/internet-property-stream");
        MIME_TYPE.put(".ai", "application/postscript");
        MIME_TYPE.put(".aif", "audio/x-aiff");
        MIME_TYPE.put(".aifc", "audio/x-aiff");
        MIME_TYPE.put(".aiff", "audio/x-aiff");
        MIME_TYPE.put(".als", "audio/X-Alpha5");
        MIME_TYPE.put(".amc", "application/x-mpeg");
        MIME_TYPE.put(".ani", "application/octet-stream");
        MIME_TYPE.put(".apk", "application/vnd.android.package-archive");
        MIME_TYPE.put(".asc", "text/plain");
        MIME_TYPE.put(".asd", "application/astound");
        MIME_TYPE.put(".asf", "video/x-ms-asf");
        MIME_TYPE.put(".asn", "application/astound");
        MIME_TYPE.put(".asp", "application/x-asap");
        MIME_TYPE.put(".asr", "video/x-ms-asf");
        MIME_TYPE.put(".asx", "video/x-ms-asf");
        MIME_TYPE.put(".au", "audio/basic");
        MIME_TYPE.put(".avb", "application/octet-stream");
        MIME_TYPE.put(".avi", "video/x-msvideo");
        MIME_TYPE.put(".awb", "audio/amr-wb");
        MIME_TYPE.put(".axs", "application/olescript");
        MIME_TYPE.put(".bas", "text/plain");
        MIME_TYPE.put(".bcpio", "application/x-bcpio");
        MIME_TYPE.put(".bin", "application/octet-stream");
        MIME_TYPE.put(".bld", "application/bld");
        MIME_TYPE.put(".bld2", "application/bld2");
        MIME_TYPE.put(".bmp", "image/bmp");
        MIME_TYPE.put(".bpk", "application/octet-stream");
        MIME_TYPE.put(".bz2", "application/x-bzip2");
        MIME_TYPE.put(".c", "text/plain");
        MIME_TYPE.put(".cal", "image/x-cals");
        MIME_TYPE.put(".cat", "application/vnd.ms-pkiseccat");
        MIME_TYPE.put(".ccn", "application/x-cnc");
        MIME_TYPE.put(".cco", "application/x-cocoa");
        MIME_TYPE.put(".cdf", "application/x-cdf");
        MIME_TYPE.put(".cer", "application/x-x509-ca-cert");
        MIME_TYPE.put(".cgi", "magnus-internal/cgi");
        MIME_TYPE.put(".chat", "application/x-chat");
        MIME_TYPE.put(".class", "application/octet-stream");
        MIME_TYPE.put(".clp", "application/x-msclip");
        MIME_TYPE.put(".cmx", "image/x-cmx");
        MIME_TYPE.put(".co", "application/x-cult3d-object");
        MIME_TYPE.put(".cod", "image/cis-cod");
        MIME_TYPE.put(".conf", "text/plain");
        MIME_TYPE.put(".cpio", "application/x-cpio");
        MIME_TYPE.put(".cpp", "text/plain");
        MIME_TYPE.put(".cpt", "application/mac-compactpro");
        MIME_TYPE.put(".crd", "application/x-mscardfile");
        MIME_TYPE.put(".crl", "application/pkix-crl");
        MIME_TYPE.put(".crt", "application/x-x509-ca-cert");
        MIME_TYPE.put(".csh", "application/x-csh");
        MIME_TYPE.put(".csm", "chemical/x-csml");
        MIME_TYPE.put(".csml", "chemical/x-csml");
        MIME_TYPE.put(".css", "text/css");
        MIME_TYPE.put(".cur", "application/octet-stream");
        MIME_TYPE.put(".dcm", "x-lml/x-evm");
        MIME_TYPE.put(".dcr", "application/x-director");
        MIME_TYPE.put(".dcx", "image/x-dcx");
        MIME_TYPE.put(".der", "application/x-x509-ca-cert");
        MIME_TYPE.put(".dhtml", "text/html");
        MIME_TYPE.put(".dir", "application/x-director");
        MIME_TYPE.put(".dll", "application/x-msdownload");
        MIME_TYPE.put(".dmg", "application/octet-stream");
        MIME_TYPE.put(".dms", "application/octet-stream");
        MIME_TYPE.put(".doc", "application/msword");
        MIME_TYPE.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE.put(".dot", "application/msword");
        MIME_TYPE.put(".dvi", "application/x-dvi");
        MIME_TYPE.put(".dwf", "drawing/x-dwf");
        MIME_TYPE.put(".dwg", "application/x-autocad");
        MIME_TYPE.put(".dxf", "application/x-autocad");
        MIME_TYPE.put(".dxr", "application/x-director");
        MIME_TYPE.put(".ebk", "application/x-expandedbook");
        MIME_TYPE.put(".emb", "chemical/x-embl-dl-nucleotide");
        MIME_TYPE.put(".embl", "chemical/x-embl-dl-nucleotide");
        MIME_TYPE.put(".eps", "application/postscript");
        MIME_TYPE.put(".epub", "application/epub+zip");
        MIME_TYPE.put(".eri", "image/x-eri");
        MIME_TYPE.put(".es", "audio/echospeech");
        MIME_TYPE.put(".esl", "audio/echospeech");
        MIME_TYPE.put(".etc", "application/x-earthtime");
        MIME_TYPE.put(".etx", "text/x-setext");
        MIME_TYPE.put(".evm", "x-lml/x-evm");
        MIME_TYPE.put(".evy", "application/envoy");
        MIME_TYPE.put(".exe", "application/octet-stream");
        MIME_TYPE.put(".fh4", "image/x-freehand");
        MIME_TYPE.put(".fh5", "image/x-freehand");
        MIME_TYPE.put(".fhc", "image/x-freehand");
        MIME_TYPE.put(".fif", "application/fractals");
        MIME_TYPE.put(".flr", "x-world/x-vrml");
        MIME_TYPE.put(".flv", "flv-application/octet-stream");
        MIME_TYPE.put(".fm", "application/x-maker");
        MIME_TYPE.put(".fpx", "image/x-fpx");
        MIME_TYPE.put(".fvi", "video/isivideo");
        MIME_TYPE.put(".gau", "chemical/x-gaussian-input");
        MIME_TYPE.put(".gca", "application/x-gca-compressed");
        MIME_TYPE.put(".gdb", "x-lml/x-gdb");
        MIME_TYPE.put(".gif", "image/gif");
        MIME_TYPE.put(".gps", "application/x-gps");
        MIME_TYPE.put(".gtar", "application/x-gtar");
        MIME_TYPE.put(".gz", "application/x-gzip");
        MIME_TYPE.put(".h", "text/plain");
        MIME_TYPE.put(".hdf", "application/x-hdf");
        MIME_TYPE.put(".hdm", "text/x-hdml");
        MIME_TYPE.put(".hdml", "text/x-hdml");
        MIME_TYPE.put(".hlp", "application/winhlp");
        MIME_TYPE.put(".hqx", "application/mac-binhex40");
        MIME_TYPE.put(".hta", "application/hta");
        MIME_TYPE.put(".htc", "text/x-component");
        MIME_TYPE.put(".htm", "text/html");
        MIME_TYPE.put(".html", "text/html");
        MIME_TYPE.put(".hts", "text/html");
        MIME_TYPE.put(".htt", "text/webviewhtml");
        MIME_TYPE.put(".ice", "x-conference/x-cooltalk");
        MIME_TYPE.put(".ico", "image/x-icon");
        MIME_TYPE.put(".ief", "image/ief");
        MIME_TYPE.put(".ifm", "image/gif");
        MIME_TYPE.put(".ifs", "image/ifs");
        MIME_TYPE.put(".iii", "application/x-iphone");
        MIME_TYPE.put(".imy", "audio/melody");
        MIME_TYPE.put(".ins", "application/x-internet-signup");
        MIME_TYPE.put(".ips", "application/x-ipscript");
        MIME_TYPE.put(".ipx", "application/x-ipix");
        MIME_TYPE.put(".isp", "application/x-internet-signup");
        MIME_TYPE.put(".it", "audio/x-mod");
        MIME_TYPE.put(".itz", "audio/x-mod");
        MIME_TYPE.put(".ivr", "i-world/i-vrml");
        MIME_TYPE.put(".j2k", "image/j2k");
        MIME_TYPE.put(".jad", "text/vnd.sun.j2me.app-descriptor");
        MIME_TYPE.put(".jam", "application/x-jam");
        MIME_TYPE.put(".jar", "application/java-archive");
        MIME_TYPE.put(".java", "text/plain");
        MIME_TYPE.put(".jfif", "image/pipeg");
        MIME_TYPE.put(".jnlp", "application/x-java-jnlp-file");
        MIME_TYPE.put(".jpe", "image/jpeg");
        MIME_TYPE.put(".jpeg", "image/jpeg");
        MIME_TYPE.put(".jpg", "image/jpeg");
        MIME_TYPE.put(".jpz", "image/jpeg");
        MIME_TYPE.put(".js", "text/javascript");
        MIME_TYPE.put(".jwc", "application/jwc");
        MIME_TYPE.put(".kjx", "application/x-kjx");
        MIME_TYPE.put(".lak", "x-lml/x-lak");
        MIME_TYPE.put(".latex", "application/x-latex");
        MIME_TYPE.put(".lcc", "application/fastman");
        MIME_TYPE.put(".lcl", "application/x-digitalloca");
        MIME_TYPE.put(".lcr", "application/x-digitalloca");
        MIME_TYPE.put(".lgh", "application/lgh");
        MIME_TYPE.put(".lha", "application/octet-stream");
        MIME_TYPE.put(".lml", "x-lml/x-lml");
        MIME_TYPE.put(".lmlpack", "x-lml/x-lmlpack");
        MIME_TYPE.put(".log", "text/plain");
        MIME_TYPE.put(".lsf", "video/x-la-asf");
        MIME_TYPE.put(".lsx", "video/x-la-asf");
        MIME_TYPE.put(".lzh", "application/octet-stream");
        MIME_TYPE.put(".m13", "application/x-msmediaview");
        MIME_TYPE.put(".m14", "application/x-msmediaview");
        MIME_TYPE.put(".m15", "audio/x-mod");
        MIME_TYPE.put(".m3u", "audio/x-mpegurl");
        MIME_TYPE.put(".m3url", "audio/x-mpegurl");
        MIME_TYPE.put(".m4a", "audio/mp4a-latm");
        MIME_TYPE.put(".m4b", "audio/mp4a-latm");
        MIME_TYPE.put(".m4p", "audio/mp4a-latm");
        MIME_TYPE.put(".m4u", "video/vnd.mpegurl");
        MIME_TYPE.put(".m4v", "video/x-m4v");
        MIME_TYPE.put(".ma1", "audio/ma1");
        MIME_TYPE.put(".ma2", "audio/ma2");
        MIME_TYPE.put(".ma3", "audio/ma3");
        MIME_TYPE.put(".ma5", "audio/ma5");
        MIME_TYPE.put(".man", "application/x-troff-man");
        MIME_TYPE.put(".map", "magnus-internal/imagemap");
        MIME_TYPE.put(".mbd", "application/mbedlet");
        MIME_TYPE.put(".mct", "application/x-mascot");
        MIME_TYPE.put(".mdb", "application/x-msaccess");
        MIME_TYPE.put(".mdz", "audio/x-mod");
        MIME_TYPE.put(".me", "application/x-troff-me");
        MIME_TYPE.put(".mel", "text/x-vmel");
        MIME_TYPE.put(".mht", "message/rfc822");
        MIME_TYPE.put(".mhtml", "message/rfc822");
        MIME_TYPE.put(".mi", "application/x-mif");
        MIME_TYPE.put(".mid", "audio/mid");
        MIME_TYPE.put(".midi", "audio/midi");
        MIME_TYPE.put(".mif", "application/x-mif");
        MIME_TYPE.put(".mil", "image/x-cals");
        MIME_TYPE.put(".mio", "audio/x-mio");
        MIME_TYPE.put(".mmf", "application/x-skt-lbs");
        MIME_TYPE.put(".mng", "video/x-mng");
        MIME_TYPE.put(".mny", "application/x-msmoney");
        MIME_TYPE.put(".moc", "application/x-mocha");
        MIME_TYPE.put(".mocha", "application/x-mocha");
        MIME_TYPE.put(".mod", "audio/x-mod");
        MIME_TYPE.put(".mof", "application/x-yumekara");
        MIME_TYPE.put(".mol", "chemical/x-mdl-molfile");
        MIME_TYPE.put(".mop", "chemical/x-mopac-input");
        MIME_TYPE.put(".mov", "video/quicktime");
        MIME_TYPE.put(".movie", "video/x-sgi-movie");
        MIME_TYPE.put(".mp2", "video/mpeg");
        MIME_TYPE.put(".mp3", "audio/mpeg");
        MIME_TYPE.put(".mp4", "video/mp4");
        MIME_TYPE.put(".mpa", "video/mpeg");
        MIME_TYPE.put(".mpc", "application/vnd.mpohun.certificate");
        MIME_TYPE.put(".mpe", "video/mpeg");
        MIME_TYPE.put(".mpeg", "video/mpeg");
        MIME_TYPE.put(".mpg", "video/mpeg");
        MIME_TYPE.put(".mpg4", "video/mp4");
        MIME_TYPE.put(".mpga", "audio/mpeg");
        MIME_TYPE.put(".mpn", "application/vnd.mophun.application");
        MIME_TYPE.put(".mpp", "application/vnd.ms-project");
        MIME_TYPE.put(".mps", "application/x-mapserver");
        MIME_TYPE.put(".mpv2", "video/mpeg");
        MIME_TYPE.put(".mrl", "text/x-mrml");
        MIME_TYPE.put(".mrm", "application/x-mrm");
        MIME_TYPE.put(".ms", "application/x-troff-ms");
        MIME_TYPE.put(".msg", "application/vnd.ms-outlook");
        MIME_TYPE.put(".mts", "application/metastream");
        MIME_TYPE.put(".mtx", "application/metastream");
        MIME_TYPE.put(".mtz", "application/metastream");
        MIME_TYPE.put(".mvb", "application/x-msmediaview");
        MIME_TYPE.put(".mzv", "application/metastream");
        MIME_TYPE.put(".nar", "application/zip");
        MIME_TYPE.put(".nbmp", "image/nbmp");
        MIME_TYPE.put(".nc", "application/x-netcdf");
        MIME_TYPE.put(".ndb", "x-lml/x-ndb");
        MIME_TYPE.put(".ndwn", "application/ndwn");
        MIME_TYPE.put(".nif", "application/x-nif");
        MIME_TYPE.put(".nmz", "application/x-scream");
        MIME_TYPE.put(".nokia-op-logo", "image/vnd.nok-oplogo-color");
        MIME_TYPE.put(".npx", "application/x-netfpx");
        MIME_TYPE.put(".nsnd", "audio/nsnd");
        MIME_TYPE.put(".nva", "application/x-neva1");
        MIME_TYPE.put(".nws", "message/rfc822");
        MIME_TYPE.put(".oda", "application/oda");
        MIME_TYPE.put(".ogg", "audio/ogg");
        MIME_TYPE.put(".oom", "application/x-AtlasMate-Plugin");
        MIME_TYPE.put(".p10", "application/pkcs10");
        MIME_TYPE.put(".p12", "application/x-pkcs12");
        MIME_TYPE.put(".p7b", "application/x-pkcs7-certificates");
        MIME_TYPE.put(".p7c", "application/x-pkcs7-mime");
        MIME_TYPE.put(".p7m", "application/x-pkcs7-mime");
        MIME_TYPE.put(".p7r", "application/x-pkcs7-certreqresp");
        MIME_TYPE.put(".p7s", "application/x-pkcs7-signature");
        MIME_TYPE.put(".pac", "audio/x-pac");
        MIME_TYPE.put(".pae", "audio/x-epac");
        MIME_TYPE.put(".pan", "application/x-pan");
        MIME_TYPE.put(".pbm", "image/x-portable-bitmap");
        MIME_TYPE.put(".pcx", "image/x-pcx");
        MIME_TYPE.put(".pda", "image/x-pda");
        MIME_TYPE.put(".pdb", "chemical/x-pdb");
        MIME_TYPE.put(".pdf", "application/pdf");
        MIME_TYPE.put(".pfr", "application/font-tdpfr");
        MIME_TYPE.put(".pfx", "application/x-pkcs12");
        MIME_TYPE.put(".pgm", "image/x-portable-graymap");
        MIME_TYPE.put(".pict", "image/x-pict");
        MIME_TYPE.put(".pko", "application/ynd.ms-pkipko");
        MIME_TYPE.put(".pm", "application/x-perl");
        MIME_TYPE.put(".pma", "application/x-perfmon");
        MIME_TYPE.put(".pmc", "application/x-perfmon");
        MIME_TYPE.put(".pmd", "application/x-pmd");
        MIME_TYPE.put(".pml", "application/x-perfmon");
        MIME_TYPE.put(".pmr", "application/x-perfmon");
        MIME_TYPE.put(".pmw", "application/x-perfmon");
        MIME_TYPE.put(".png", "image/png");
        MIME_TYPE.put(".pnm", "image/x-portable-anymap");
        MIME_TYPE.put(".pnz", "image/png");
        MIME_TYPE.put(".pot", "application/vnd.ms-powerpoint");
        MIME_TYPE.put(".ppm", "image/x-portable-pixmap");
        MIME_TYPE.put(".pps", "application/vnd.ms-powerpoint");
        MIME_TYPE.put(".ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPE.put(".pqf", "application/x-cprplayer");
        MIME_TYPE.put(".pqi", "application/cprplayer");
        MIME_TYPE.put(".prc", "application/x-prc");
        MIME_TYPE.put(".prf", "application/pics-rules");
        MIME_TYPE.put(".prop", "text/plain");
        MIME_TYPE.put(".proxy", "application/x-ns-proxy-autoconfig");
        MIME_TYPE.put(".ps", "application/postscript");
        MIME_TYPE.put(".ptlk", "application/listenup");
        MIME_TYPE.put(".pub", "application/x-mspublisher");
        MIME_TYPE.put(".pvx", "video/x-pv-pvx");
        MIME_TYPE.put(".qcp", "audio/vnd.qcelp");
        MIME_TYPE.put(".qt", "video/quicktime");
        MIME_TYPE.put(".qti", "image/x-quicktime");
        MIME_TYPE.put(".qtif", "image/x-quicktime");
        MIME_TYPE.put(".r3t", "text/vnd.rn-realtext3d");
        MIME_TYPE.put(".ra", "audio/x-pn-realaudio");
        MIME_TYPE.put(".ram", "audio/x-pn-realaudio");
        MIME_TYPE.put(".rar", "application/octet-stream");
        MIME_TYPE.put(".ras", "image/x-cmu-raster");
        MIME_TYPE.put(".rc", "text/plain");
        MIME_TYPE.put(".rdf", "application/rdf+xml");
        MIME_TYPE.put(".rf", "image/vnd.rn-realflash");
        MIME_TYPE.put(".rgb", "image/x-rgb");
        MIME_TYPE.put(".rlf", "application/x-richlink");
        MIME_TYPE.put(".rm", "audio/x-pn-realaudio");
        MIME_TYPE.put(".rmf", "audio/x-rmf");
        MIME_TYPE.put(".rmi", "audio/mid");
        MIME_TYPE.put(".rmm", "audio/x-pn-realaudio");
        MIME_TYPE.put(".rmvb", "audio/x-pn-realaudio");
        MIME_TYPE.put(".rnx", "application/vnd.rn-realplayer");
        MIME_TYPE.put(".roff", "application/x-troff");
        MIME_TYPE.put(".rp", "image/vnd.rn-realpix");
        MIME_TYPE.put(".rpm", "audio/x-pn-realaudio-plugin");
        MIME_TYPE.put(".rt", "text/vnd.rn-realtext");
        MIME_TYPE.put(".rte", "x-lml/x-gps");
        MIME_TYPE.put(".rtf", "application/rtf");
        MIME_TYPE.put(".rtg", "application/metastream");
        MIME_TYPE.put(".rtx", "text/richtext");
        MIME_TYPE.put(".rv", "video/vnd.rn-realvideo");
        MIME_TYPE.put(".rwc", "application/x-rogerwilco");
        MIME_TYPE.put(".s3m", "audio/x-mod");
        MIME_TYPE.put(".s3z", "audio/x-mod");
        MIME_TYPE.put(".sca", "application/x-supercard");
        MIME_TYPE.put(".scd", "application/x-msschedule");
        MIME_TYPE.put(".sct", "text/scriptlet");
        MIME_TYPE.put(".sdf", "application/e-score");
        MIME_TYPE.put(".sea", "application/x-stuffit");
        MIME_TYPE.put(".setpay", "application/set-payment-initiation");
        MIME_TYPE.put(".setreg", "application/set-registration-initiation");
        MIME_TYPE.put(".sgm", "text/x-sgml");
        MIME_TYPE.put(".sgml", "text/x-sgml");
        MIME_TYPE.put(".sh", "application/x-sh");
        MIME_TYPE.put(".shar", "application/x-shar");
        MIME_TYPE.put(".shtml", "magnus-internal/parsed-html");
        MIME_TYPE.put(".shw", "application/presentations");
        MIME_TYPE.put(".si6", "image/si6");
        MIME_TYPE.put(".si7", "image/vnd.stiwap.sis");
        MIME_TYPE.put(".si9", "image/vnd.lgtwap.sis");
        MIME_TYPE.put(".sis", "application/vnd.symbian.install");
        MIME_TYPE.put(".sit", "application/x-stuffit");
        MIME_TYPE.put(".skd", "application/x-Koan");
        MIME_TYPE.put(".skm", "application/x-Koan");
        MIME_TYPE.put(".skp", "application/x-Koan");
        MIME_TYPE.put(".skt", "application/x-Koan");
        MIME_TYPE.put(".slc", "application/x-salsa");
        MIME_TYPE.put(".smd", "audio/x-smd");
        MIME_TYPE.put(".smi", "application/smil");
        MIME_TYPE.put(".smil", "application/smil");
        MIME_TYPE.put(".smp", "application/studiom");
        MIME_TYPE.put(".smz", "audio/x-smd");
        MIME_TYPE.put(".snd", "audio/basic");
        MIME_TYPE.put(".spc", "application/x-pkcs7-certificates");
        MIME_TYPE.put(".spl", "application/futuresplash");
        MIME_TYPE.put(".spr", "application/x-sprite");
        MIME_TYPE.put(".sprite", "application/x-sprite");
        MIME_TYPE.put(".sdp", "application/sdp");
        MIME_TYPE.put(".spt", "application/x-spt");
        MIME_TYPE.put(".src", "application/x-wais-source");
        MIME_TYPE.put(".sst", "application/vnd.ms-pkicertstore");
        MIME_TYPE.put(".stk", "application/hyperstudio");
        MIME_TYPE.put(".stl", "application/vnd.ms-pkistl");
        MIME_TYPE.put(".stm", "text/html");
        MIME_TYPE.put(".sv4cpio", "application/x-sv4cpio");
        MIME_TYPE.put(".sv4crc", "application/x-sv4crc");
        MIME_TYPE.put(".svf", "image/vnd");
        MIME_TYPE.put(".svh", "image/svh");
        MIME_TYPE.put(".svr", "x-world/x-svr");
        MIME_TYPE.put(".swf", "application/x-shockwave-flash");
        MIME_TYPE.put(".swfl", "application/x-shockwave-flash");
        MIME_TYPE.put(".t", "application/x-troff");
        MIME_TYPE.put(".tad", "application/octet-stream");
        MIME_TYPE.put(".talk", "text/x-speech");
        MIME_TYPE.put(".tar", "application/x-tar");
        MIME_TYPE.put(".taz", "application/x-tar");
        MIME_TYPE.put(".tbp", "application/x-timbuktu");
        MIME_TYPE.put(".tbt", "application/x-timbuktu");
        MIME_TYPE.put(".tcl", "application/x-tcl");
        MIME_TYPE.put(".tex", "application/x-tex");
        MIME_TYPE.put(".texi", "application/x-texinfo");
        MIME_TYPE.put(".texinfo", "application/x-texinfo");
        MIME_TYPE.put(".tgz", "application/x-compressed");
        MIME_TYPE.put(".thm", "application/vnd.eri.thm");
        MIME_TYPE.put(".tif", "image/tiff");
        MIME_TYPE.put(".tiff", "image/tiff");
        MIME_TYPE.put(".tki", "application/x-tkined");
        MIME_TYPE.put(".tkined", "application/x-tkined");
        MIME_TYPE.put(".toc", "application/toc");
        MIME_TYPE.put(".toy", "image/toy");
        MIME_TYPE.put(".tr", "application/x-troff");
        MIME_TYPE.put(".trk", "x-lml/x-gps");
        MIME_TYPE.put(".trm", "application/x-msterminal");
        MIME_TYPE.put(".tsi", "audio/tsplayer");
        MIME_TYPE.put(".tsp", "application/dsptype");
        MIME_TYPE.put(".tsv", "text/tab-separated-values");
        MIME_TYPE.put(".ttf", "application/octet-stream");
        MIME_TYPE.put(".ttz", "application/t-time");
        MIME_TYPE.put(".txt", "text/plain");
        MIME_TYPE.put(".uls", "text/iuls");
        MIME_TYPE.put(".ult", "audio/x-mod");
        MIME_TYPE.put(".ustar", "application/x-ustar");
        MIME_TYPE.put(".uu", "application/x-uuencode");
        MIME_TYPE.put(".uue", "application/x-uuencode");
        MIME_TYPE.put(".vcd", "application/x-cdlink");
        MIME_TYPE.put(".vcf", "text/x-vcard");
        MIME_TYPE.put(".vdo", "video/vdo");
        MIME_TYPE.put(".vib", "audio/vib");
        MIME_TYPE.put(".viv", "video/vivo");
        MIME_TYPE.put(".vivo", "video/vivo");
        MIME_TYPE.put(".vmd", "application/vocaltec-media-desc");
        MIME_TYPE.put(".vmf", "application/vocaltec-media-file");
        MIME_TYPE.put(".vmi", "application/x-dreamcast-vms-info");
        MIME_TYPE.put(".vms", "application/x-dreamcast-vms");
        MIME_TYPE.put(".vox", "audio/voxware");
        MIME_TYPE.put(".vqe", "audio/x-twinvq-plugin");
        MIME_TYPE.put(".vqf", "audio/x-twinvq");
        MIME_TYPE.put(".vql", "audio/x-twinvq");
        MIME_TYPE.put(".vre", "x-world/x-vream");
        MIME_TYPE.put(".vrml", "x-world/x-vrml");
        MIME_TYPE.put(".vrt", "x-world/x-vrt");
        MIME_TYPE.put(".vrw", "x-world/x-vream");
        MIME_TYPE.put(".vts", "workbook/formulaone");
        MIME_TYPE.put(".wav", "audio/x-wav");
        MIME_TYPE.put(".wax", "audio/x-ms-wax");
        MIME_TYPE.put(".wbmp", "image/vnd.wap.wbmp");
        MIME_TYPE.put(".wcm", "application/vnd.ms-works");
        MIME_TYPE.put(".wdb", "application/vnd.ms-works");
        MIME_TYPE.put(".web", "application/vnd.xara");
        MIME_TYPE.put(".wi", "image/wavelet");
        MIME_TYPE.put(".wis", "application/x-InstallShield");
        MIME_TYPE.put(".wks", "application/vnd.ms-works");
        MIME_TYPE.put(".wm", "video/x-ms-wm");
        MIME_TYPE.put(".wma", "audio/x-ms-wma");
        MIME_TYPE.put(".wmd", "application/x-ms-wmd");
        MIME_TYPE.put(".wmf", "application/x-msmetafile");
        MIME_TYPE.put(".wml", "text/vnd.wap.wml");
        MIME_TYPE.put(".wmlc", "application/vnd.wap.wmlc");
        MIME_TYPE.put(".wmls", "text/vnd.wap.wmlscript");
        MIME_TYPE.put(".wmlsc", "application/vnd.wap.wmlscriptc");
        MIME_TYPE.put(".wmlscript", "text/vnd.wap.wmlscript");
        MIME_TYPE.put(".wmv", "audio/x-ms-wmv");
        MIME_TYPE.put(".wmx", "video/x-ms-wmx");
        MIME_TYPE.put(".wmz", "application/x-ms-wmz");
        MIME_TYPE.put(".woff", "application/x-font-woff");
        MIME_TYPE.put(".wpng", "image/x-up-wpng");
        MIME_TYPE.put(".wps", "application/vnd.ms-works");
        MIME_TYPE.put(".wpt", "x-lml/x-gps");
        MIME_TYPE.put(".wri", "application/x-mswrite");
        MIME_TYPE.put(".wrl", "x-world/x-vrml");
        MIME_TYPE.put(".wrz", "x-world/x-vrml");
        MIME_TYPE.put(".ws", "text/vnd.wap.wmlscript");
        MIME_TYPE.put(".wsc", "application/vnd.wap.wmlscriptc");
        MIME_TYPE.put(".wv", "video/wavelet");
        MIME_TYPE.put(".wvx", "video/x-ms-wvx");
        MIME_TYPE.put(".wxl", "application/x-wxl");
        MIME_TYPE.put(".x-gzip", "application/x-gzip");
        MIME_TYPE.put(".xaf", "x-world/x-vrml");
        MIME_TYPE.put(".xar", "application/vnd.xara");
        MIME_TYPE.put(".xbm", "image/x-xbitmap");
        MIME_TYPE.put(".xdm", "application/x-xdma");
        MIME_TYPE.put(".xdma", "application/x-xdma");
        MIME_TYPE.put(".xdw", "application/vnd.fujixerox.docuworks");
        MIME_TYPE.put(".xht", "application/xhtml+xml");
        MIME_TYPE.put(".xhtm", "application/xhtml+xml");
        MIME_TYPE.put(".xhtml", "application/xhtml+xml");
        MIME_TYPE.put(".xla", "application/vnd.ms-excel");
        MIME_TYPE.put(".xlc", "application/vnd.ms-excel");
        MIME_TYPE.put(".xll", "application/x-excel");
        MIME_TYPE.put(".xlm", "application/vnd.ms-excel");
        MIME_TYPE.put(".xls", "application/vnd.ms-excel");
        MIME_TYPE.put(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPE.put(".xlt", "application/vnd.ms-excel");
        MIME_TYPE.put(".xlw", "application/vnd.ms-excel");
        MIME_TYPE.put(".xm", "audio/x-mod");
        MIME_TYPE.put(".xmz", "audio/x-mod");
        MIME_TYPE.put(".xof", "x-world/x-vrml");
        MIME_TYPE.put(".xpi", "application/x-xpinstall");
        MIME_TYPE.put(".xpm", "image/x-xpixmap");
        MIME_TYPE.put(".xsit", "text/xml");
        MIME_TYPE.put(".xsl", "text/xml");
        MIME_TYPE.put(".xul", "text/xul");
        MIME_TYPE.put(".xwd", "image/x-xwindowdump");
        MIME_TYPE.put(".xyz", "chemical/x-pdb");
        MIME_TYPE.put(".yz1", "application/x-yz1");
        MIME_TYPE.put(".z", "application/x-compress");
        MIME_TYPE.put(".zac", "application/x-zaurus-zac");
        MIME_TYPE.put(".zip", "application/zip");
        MIME_TYPE.put(".json", "application/json");
    }
}
