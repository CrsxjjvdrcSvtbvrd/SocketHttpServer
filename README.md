# SocketHttpServer

A simple http server with java socket

### Usage

```java
// init mime type
HttpServer.INIT_FULL_MIME_TYPE();
int port = 8080;
HttpServer server = new HttpServer(port);
```

start server
```java
try {
    server.start();
} catch (IOException e) {
    e.printStackTrace();
}
```

stop server
```java
try {
    server.stop();
} catch (IOException e) {
    e.printStackTrace();
}
```

add router
```java
server.addRouter("/",new HttpServer.RouterHandler() {
    @Override
    public void handle(HttpServer.RouterContext context) throws IOException {
        context.response.r200().contentTextHtml();
        context.response.addBody("<h4>Hello World</h4>");
        context.response.end();
    }
});
```

use default router when router not matched

* in default router handler, the context.router may be null 
```java
server.setDeafultHandler(new HttpServer.RouterHandler() {
    @Override
    public void handle(HttpServer.RouterContext context) throws IOException {
        context.response.r200().contentTextHtml();
        context.response.addBody("<h4>emm</h4>");
        context.response.addBody("<p>"+context.request.uri+"</p>");
        context.response.end();
    }
});
```

set static file router

* /css/a.css -> webroot/css/a.css
* staticFileHandler's path must be same as router
```java
server.addRouter("/css", 
    HttpServer.staticFileHandler(new File("webroot/css/"), "/css"));
```

host static file

**not recommend, why not use nginx?**
```java
server.setDeafultHandler(HttpServer.staticFileHandler(new File("path/to"),""));
```

### something else
you can delete function **INIT_MINIMAL_MIME_TYPE()** or **INIT_FULL_MIME_TYPE()** and then uncomment **static{}** in line 511, then you don't need to init mime type in your code