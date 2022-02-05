
import java.util.ArrayList;

public class HtmlBuilder {
    public HtmlNode head, body;

    public HtmlBuilder() {
        head = new HtmlNode("head");
        body = new HtmlNode("body");
    }

    @Override
    public String toString() {
        return "<!DOCTYPE html><html>" + head.toString() + body.toString() + "</html>";
    }

    public String formaString(){
        return "<!DOCTYPE html>\r\n<html>\r\n" + head.formatString() + body.formatString() + "</html>";
    }

    public HtmlBuilder utf8() {
        meta0("charset", "utf-8");
        return this;
    }

    public HtmlBuilder textscript0(String js) {
        body(textscript(js));
        return this;
    }

    public HtmlBuilder script0(String url) {
        head(script(url));
        return this;
    }

    public HtmlBuilder textcss0(String css) {
        head(textcss(css));
        return this;
    }

    public HtmlBuilder linkcss0(String href) {
        head(linkcss(href));
        return this;
    }

    public HtmlBuilder title0(String t) {
        head.appendChild(title(t));
        return this;
    }

    public HtmlBuilder meta0(String k, String v) {
        head(meta(k, v));
        return this;
    }

    public static HtmlNode textscript(String js) {
        return node("script").text(js);
    }

    public static HtmlNode script(String url) {
        return node("script").src(url);
    }

    public static HtmlNode textcss(String css) {
        return node("style").text(css).type("text/css");
    }

    public static HtmlNode linkcss(String href) {
        return node("link").end(false).href(href).stylesheet();
    }

    public static HtmlNode title(String t) {
        return node("title").text(t);
    }

    public static HtmlNode meta(String k, String v) {
        return node("meta").end(false).addAttribute(k, v);
    }

    public static HtmlNode div(HtmlNode... nodes) {
        return node("div", nodes);
    }

    public static HtmlNode text(String t) {
        return node("text").text(t);
    }

    public static HtmlNode node(String t, HtmlNode... nodes) {
        return new HtmlNode(t).appendChild(nodes);
    }

    public static HtmlNode node(String t, String clazz, HtmlNode... nodes) {
        return new HtmlNode(t).clazz(clazz).appendChild(nodes);
    }

    public static HtmlNode node(String t, String clazz, String id, HtmlNode... nodes) {
        return new HtmlNode(t).clazz(clazz).id(id).appendChild(nodes);
    }

    public static HtmlNode h1(HtmlNode... nodes) {
        return node("h1", nodes);
    }

    public static HtmlNode h2(HtmlNode... nodes) {
        return node("h2", nodes);
    }

    public static HtmlNode h3(HtmlNode... nodes) {
        return node("h3", nodes);
    }

    public static HtmlNode h4(HtmlNode... nodes) {
        return node("h4", nodes);
    }

    public static HtmlNode h5(HtmlNode... nodes) {
        return node("h5", nodes);
    }

    public static HtmlNode h6(HtmlNode... nodes) {
        return node("h6", nodes);
    }

    public static HtmlNode h1(String text, HtmlNode... nodes) {
        return node("h1", nodes).text(text);
    }

    public static HtmlNode h2(String text, HtmlNode... nodes) {
        return node("h2", nodes).text(text);
    }

    public static HtmlNode h3(String text, HtmlNode... nodes) {
        return node("h3", nodes).text(text);
    }

    public static HtmlNode h4(String text, HtmlNode... nodes) {
        return node("h4", nodes).text(text);
    }

    public static HtmlNode h5(String text, HtmlNode... nodes) {
        return node("h5", nodes).text(text);
    }

    public static HtmlNode h6(String text, HtmlNode... nodes) {
        return node("h6", nodes).text(text);
    }

    public static HtmlNode p(HtmlNode... nodes) {
        return node("p", nodes);
    }

    public static HtmlNode p(String text, HtmlNode... nodes) {
        return node("p", nodes).text(text);
    }

    public static HtmlNode a(HtmlNode... nodes) {
        return node("a", nodes).href("javascript:void(0);");
    }

    public static HtmlNode a(String text, HtmlNode... nodes) {
        return node("a", nodes).text(text).href("javascript:void(0);");
    }

    public static HtmlNode a(String text, String href, HtmlNode... nodes) {
        return node("a", nodes).text(text).href(href);
    }

    public static HtmlNode img(HtmlNode... nodes) {
        return node("img", nodes);
    }

    public static HtmlNode img(String src, HtmlNode... nodes) {
        return node("img", nodes).src(src);
    }

    public static HtmlNode table(HtmlNode... nodes) {
        return node("table", nodes);
    }

    public static HtmlNode input(HtmlNode... nodes) {
        return node("input", nodes);
    }

    public static HtmlNode input(String type, HtmlNode... nodes) {
        return node("input", nodes).type(type);
    }

    public static HtmlNode input(String type, String hint, HtmlNode... nodes) {
        return node("input", nodes).type(type).addAttribute("placeholder", hint);
    }

    public static HtmlNode li(HtmlNode... nodes) {
        return node("li", nodes);
    }

    public static HtmlNode li(String text, HtmlNode... nodes) {
        return node("li", nodes).text(text);
    }

    public static HtmlNode ul(HtmlNode... nodes) {
        return node("ul", nodes);
    }

    public static HtmlNode td(HtmlNode... nodes) {
        return node("td", nodes);
    }

    public static HtmlNode td(String text, HtmlNode... nodes) {
        return node("td", nodes).text(text);
    }

    public static HtmlNode th(HtmlNode... nodes) {
        return node("th", nodes);
    }

    public static HtmlNode th(String text, HtmlNode... nodes) {
        return node("th", nodes).text(text);
    }

    public static HtmlNode b(HtmlNode... nodes) {
        return node("b", nodes);
    }

    public static HtmlNode i(HtmlNode... nodes) {
        return node("i", nodes);
    }

    public static HtmlNode i(String i, HtmlNode... nodes) {
        return node("i", nodes).text(i);
    }

    public static HtmlNode u(HtmlNode... nodes) {
        return node("u", nodes);
    }

    public static HtmlNode s(HtmlNode... nodes) {
        return node("s", nodes);
    }

    public static HtmlNode br(HtmlNode... nodes) {
        return node("br", nodes);
    }

    public static HtmlNode hr(HtmlNode... nodes) {
        return node("hr", nodes);
    }

    public static HtmlNode span(HtmlNode... nodes) {
        return node("span", nodes);
    }

    public static HtmlNode span(String text, HtmlNode... nodes) {
        return node("span", nodes).text(text);
    }

    public static HtmlNode ol(HtmlNode... nodes) {
        return node("ol", nodes);
    }

    public static HtmlNode dl(HtmlNode... nodes) {
        return node("dl", nodes);
    }

    public static HtmlNode dt(HtmlNode... nodes) {
        return node("dt", nodes);
    }

    public static HtmlNode thead(HtmlNode... nodes) {
        return node("thead", nodes);
    }

    public static HtmlNode tbody(HtmlNode... nodes) {
        return node("tbody", nodes);
    }

    public static HtmlNode tr(HtmlNode... nodes) {
        return node("tr", nodes);
    }

    public static HtmlNode button(HtmlNode... nodes) {
        return node("button", nodes);
    }

    public static HtmlNode button(String text, HtmlNode... nodes) {
        return node("button", nodes).text(text);
    }

    public HtmlBuilder head(HtmlNode... nodes) {
        head.appendChild(nodes);
        return this;
    }

    public HtmlBuilder body(HtmlNode... nodes) {
        body.appendChild(nodes);
        return this;
    }

    public void release() {
        body.release();
        head.release();
    }

    public static class HtmlNode {
        public String node;
        public ArrayList<HtmlNode> children = new ArrayList<>();
        public KVMap attributes = new KVMap();
        public String innerText = "";
        // </> or <>
        public boolean endWith = true;

        public HtmlNode(String tag) {
            node = tag;
        }

        public HtmlNode id(String id) {
            return addAttribute("id", id);
        }

        public HtmlNode src(String src) {
            return addAttribute("src", src);
        }

        public HtmlNode clazz(String clazz) {
            return addAttribute("class", clazz);
        }

        public HtmlNode type(String type) {
            return addAttribute("type", type);
        }

        public HtmlNode href(String href) {
            return addAttribute("href", href);
        }

        public HtmlNode style(String style) {
            return addAttribute("style", style);
        }

        public HtmlNode stylesheet() {
            return addAttribute("rel", "stylesheet");
        }

        public HtmlNode value(String v) {
            return addAttribute("value", v);
        }

        public HtmlNode disable() {
            return addAttribute("disabled", "");
        }

        public HtmlNode appendChild(HtmlNode... nodes) {
            for (HtmlNode node : nodes)
                children.add(node);
            return this;
        }

        public HtmlNode addAttribute(String k, String v) {
            attributes.put(k, v);
            return this;
        }

        public HtmlNode removeAttribute(String k) {
            attributes.remove(k);
            return this;
        }

        public HtmlNode end(boolean end) {
            endWith = end;
            return this;
        }

        public HtmlNode text(String text) {
            innerText = text;
            return this;
        }

        public void release() {
            attributes.release();
            for (HtmlNode node : children)
                node.release();
            children = null;
        }

        @Override
        public String toString() {
            if (node.equalsIgnoreCase("text")) {
                return innerText;
            }
            if (children.size() == 0) {
                if (endWith)
                    return "<" + node + attributes.toHtmlCode() + ">" + innerText + "</" + node + ">";
                else
                    return "<" + node + attributes.toHtmlCode() + ">";
            }
            StringBuilder builder = new StringBuilder();
            builder.append("<").append(node).append(attributes.toHtmlCode()).append(">");
            builder.append(innerText);
            for (HtmlNode node : children) {
                builder.append(node.toString());
            }
            builder.append("</").append(node).append(">");
            return builder.toString();
        }
        public String formatString(){
            return formatString0(0);
            /*if (node.equalsIgnoreCase("text")) {
                return innerText+"\r\n";
            }
            if (children.size() == 0) {
                if (endWith)
                    return "<" + node + attributes.toHtmlCode() + ">" + innerText + "</" + node + ">\r\n";
                else
                    return "<" + node + attributes.toHtmlCode() + ">\r\n";
            }
            StringBuilder builder = new StringBuilder();
            builder.append("<").append(node).append(attributes.toHtmlCode()).append(">");
            builder.append(innerText);
            builder.append("\r\n");
            for (HtmlNode node : children) {
                builder.append(node.formatString());
            }
            builder.append("</").append(node).append(">\r\n");
            return builder.toString();*/
        }
        private String formatString0(int z){
            String tab = "";
            for (int i = 0; i < z; i++) {
                tab+="  ";
            }
            if (node.equalsIgnoreCase("text")) {
                return innerText+"\r\n";
            }
            if (children.size() == 0) {
                if (endWith)
                    return tab+"<" + node + attributes.toHtmlCode() + ">" + innerText + "</" + node + ">\r\n";
                else
                    return tab+"<" + node + attributes.toHtmlCode() + ">\r\n";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(tab).append("<").append(node).append(attributes.toHtmlCode()).append(">");
            builder.append(innerText);
            builder.append("\r\n");
            for (HtmlNode node : children) {
                builder.append(node.formatString0(z+1));
            }
            builder.append(tab).append("</").append(node).append(">\r\n");
            return builder.toString();
        }
    }

    public static class KVMap {
        public ArrayList<String> keys;
        public ArrayList<String> values;
        public int length = 0;

        public KVMap() {
            keys = new ArrayList<>();
            values = new ArrayList<>();
        }

        public KVMap(int l) {
            keys = new ArrayList<>(l);
            values = new ArrayList<>(l);
        }

        public int contains(String k) {
            int i = 0;
            for (String s : keys) {
                if (s.equals(k))
                    return i;
            }
            return -1;
        }

        public boolean put(String k, String v) {
            if (contains(k) == -1) {
                keys.add(k);
                values.add(v);
                length++;
                return true;
            }
            return false;
        }

        public void remove(String k) {
            int i = contains(k);
            if (i == -1)
                return;
            keys.remove(i);
            values.remove(i);
            length--;
        }

        public String value(String k) {
            int i = contains(k);
            if (i == -1)
                return null;
            return values.get(i);
        }

        public String toHtmlCode() {
            if (length == 0)
                return "";
            StringBuilder builder = new StringBuilder();
            builder.append(" ");
            for (int i = 0; i < length; i++) {
                if (keys.get(i).equalsIgnoreCase("disabled"))
                    builder.append(" disabled ");
                else
                    builder.append(keys.get(i)).append("=\"").append(values.get(i)).append("\" ");
            }
            String res = builder.toString();
            return res.substring(0, res.length() - 1);
        }

        public void release() {
            keys = null;
            values = null;
            length = 0;
        }
    }

}
