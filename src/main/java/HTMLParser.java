import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.HtmlPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTMLParser {


    public static final String INDEX_TIME = "0";
    public static final String INDEX_SPCPCODE = "1";
    public static final String INDEX_SPCPNAME = "2";
    public static final String INDEX_TYPE = "3";
    public static final String INDEX_SONGCODE = "4";
    public static final String INDEX_SONGNAME = "5";
    public static final String INDEX_SONGER = "6";
    public static final String INDEX_DOWNLOAD = "7";



    /**
     * 按页面方式处理.解析标准的html页面
     *
     *
     * @param content 网页的内容
     * @throws Exception
     */
    public static List readTotalTable(String content) throws Exception {
        Parser myParser;
        myParser = Parser.createParser(content, "utf8");
        HtmlPage visitor = new HtmlPage(myParser);
        myParser.visitAllNodesWith(visitor);
        TableTag[] tables = visitor.getTables();

        List<Map> rowList = new ArrayList<Map>();
        for (int i = 0; i < tables.length; i++) {
            TableTag table = tables[2];

            for (int j = 0; j < table.getRowCount(); j++) {
                TableRow row = table.getRow(j);
                TableColumn[] columns = row.getColumns();
                Map column = new HashMap();
                for (int k = 0; k < columns.length; k++) {
                    String s = columns[k].toPlainTextString();
                    //System.out.println(columns[k].toPlainTextString().trim());

                    column.put(k + "", columns[k].toPlainTextString().trim());
                }

                if(columns.length != 0){
                    rowList.add(column);
                }
            }

            break;
        }

        return rowList;

    }


    public static List readDetailTable(String content) throws Exception {
        Parser myParser;
        myParser = Parser.createParser(content, "utf8");
        HtmlPage visitor = new HtmlPage(myParser);
        myParser.visitAllNodesWith(visitor);
        TableTag[] tables = visitor.getTables();

        List<Map> rowList = new ArrayList<Map>();
        for (int i = 0; i < tables.length; i++) {
            TableTag table = tables[i];

            for (int j = 0; j < table.getRowCount(); j++) {
                TableRow row = table.getRow(j);
                TableColumn[] columns = row.getColumns();
                if (columns.length == 0) {
                    continue;
                }
                Map column = new HashMap();
                for (int k = 0; k < columns.length; k++) {
                    String s = columns[k].toPlainTextString().trim();
                    //System.out.println(columns[k].toPlainTextString().trim());
                    if (s.equals("")) {
                        continue;
                    }

                    column.put(k + "", s);
                }

                if(columns.length != 0 && column != null){
                    rowList.add(column);
                }
            }
        }

        return rowList;

    }

    /**
     * 分别读纯文本和链接.
     *
     * @param result 网页的内容
     * @throws Exception
     */
    public static void readTextAndLinkAndTitle(String result) throws Exception {
        Parser parser;
        NodeList nodelist;
        parser = Parser.createParser(result, "utf8");
        NodeFilter textFilter = new NodeClassFilter(TextNode.class);
        NodeFilter linkFilter = new NodeClassFilter(LinkTag.class);
        NodeFilter titleFilter = new NodeClassFilter(TitleTag.class);
        OrFilter lastFilter = new OrFilter();
        lastFilter.setPredicates(new NodeFilter[]{textFilter, linkFilter, titleFilter});
        nodelist = parser.parse(lastFilter);
        Node[] nodes = nodelist.toNodeArray();
        String line = "";

        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            if (node instanceof TextNode) {
                TextNode textnode = (TextNode) node;
                line = textnode.getText();
            } else if (node instanceof LinkTag) {
                LinkTag link = (LinkTag) node;
                line = link.getLink();
            } else if (node instanceof TitleTag) {
                TitleTag titlenode = (TitleTag) node;
                line = titlenode.getTitle();
            }

            if (isTrimEmpty(line))
                continue;
            System.out.println(line);
        }
    }

    /**
     * 去掉左右空格后字符串是否为空
     */
    public static boolean isTrimEmpty(String astr) {
        if ((null == astr) || (astr.length() == 0)) {
            return true;
        }
        if (isBlank(astr.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 字符串是否为空:null或者长度为0.
     */
    public static boolean isBlank(String astr) {
        if ((null == astr) || (astr.length() == 0)) {
            return true;
        } else {
            return false;
        }
    }
}