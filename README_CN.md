# SocketHttpServer

使用java socket的http服务器

### Usage

```java
// init mime type
HttpServer.INIT_FULL_MIME_TYPE();
int port = 8080;
HttpServer server = new HttpServer(port);
```

启动

```java
try {
    server.start();
} catch (IOException e) {
    e.printStackTrace();
}
```

关闭

```java
try {
    server.stop();
} catch (IOException e) {
    e.printStackTrace();
}
```

添加路由

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

当路由不匹配是使用默认路由

* 使用默认路由时，RouterContext.router为空

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

设置静态文件路由

* /css/a.css -> webroot/css/a.css
* staticFileHandler的参数'path'必须和路由的path一样

```java
server.addRouter("/css", 
    HttpServer.staticFileHandler(new File("webroot/css/"), "/css"));
```

静态文件

**为什么不用强无敌的nginx，，，**

```java
server.setDeafultHandler(HttpServer.staticFileHandler(new File("path/to"),""));
```

### 其他

可以删除函数 **INIT_MINIMAL_MIME_TYPE()** 或 **INIT_FULL_MIME_TYPE()** 来节省空间， 取消511行左右的 **static{}** 的注释这样就不用在代码里面初始化mime type了