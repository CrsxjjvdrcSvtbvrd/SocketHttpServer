
public class Test {
    public static void main(String[] args) {
        HtmlBuilder builder = new HtmlBuilder();
        builder.utf8();
        builder.head(
            HtmlBuilder.title("title"),
            HtmlBuilder.meta("content", "width=device-width, initial-scale=1.0"),
            HtmlBuilder.meta("name", "viewport"),
            HtmlBuilder.textcss("body { background-color: #fff; }"),
            HtmlBuilder.script("/js/my.js"),
            HtmlBuilder.linkcss("css/my.css")
        ).body(
            HtmlBuilder.h1("h1"),
            HtmlBuilder.div(
                HtmlBuilder.p("p"),
                HtmlBuilder.hr(),
                HtmlBuilder.ul(
                    HtmlBuilder.li(
                        HtmlBuilder.a("list1", "/list1")
                    ),
                    HtmlBuilder.li("list 2"),
                    HtmlBuilder.li("list 3")
                )
            ).clazz("container"),
            HtmlBuilder.textscript("console.log('hello world')")
        );
        System.out.println(builder.formaString());
    }
}
/* the output html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>title</title>
  <meta content="width=device-width, initial-scale=1.0">
  <meta name="viewport">
  <style type="text/css">body { background-color: #fff; }</style>
  <script src="/js/my.js"></script>
  <link href="css/my.css" rel="stylesheet">
</head>
<body>
  <h1>h1</h1>
  <div class="container">
    <p>p</p>
    <hr></hr>
    <ul>
      <li>
        <a href="/list1">list1</a>
      </li>
      <li>list 2</li>
      <li>list 3</li>
    </ul>
  </div>
  <script>console.log('hello world')</script>
</body>
</html>
*/
