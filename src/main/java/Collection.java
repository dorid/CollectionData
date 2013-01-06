import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * 以 dict.cn 网站为例的爬虫
 * @author Winter Lau
 */
public class Collection {

    private final static HttpClient client = new DefaultHttpClient();

    public static void main(String[] args) throws Exception, HttpException, URISyntaxException {


        Collection collection = new Collection();

        String url = "http://218.200.160.103:8082/CMS4-COPM/balance/cpStatisticList.do?pageSize=1500000&pageNum=1&flag=1&cpId=&cpName=&precopyrightId=&serviceType=1&startTime=201212&endTime=201301";
        List<Map> total = collection.getTotal(url);
        System.out.println("==========================================================");
        collection.getDetail(total);
    }

    public HttpGet createTotalHttpGet(String url) throws URISyntaxException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        get.setHeader("Accept-Encoding", "gzip, deflate");
        get.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        get.setHeader("Connection", "keep-alive");
        get.setHeader("Cookie", "JSESSIONID=ffe109bb91f6fd8887558d00382d");
        get.setHeader("Host", "218.200.160.103:8082");
        get.setHeader("Referer", "http://218.200.160.103:8080/CMS4-IMPORT/");
        get.setHeader("User-Agent", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        return get;
    }

    public HttpGet createDetailHttpGet(String url) throws URISyntaxException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        get.setHeader("Accept-Encoding", "gzip, deflate");
        get.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        get.setHeader("Connection", "keep-alive");
        get.setHeader("Cookie", "JSESSIONID=ffe109bb91f6fd8887558d00382d");
        get.setHeader("Host", "218.200.160.103:8082");
        get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:16.0) Gecko/20100101 Firefox/16.0");

        return get;
    }

    /**
     * 抓取网页
     *
     * @param url
     * @throws java.io.IOException
     */
    public List<Map> getTotal(String url) throws Exception, URISyntaxException, HttpException {
        HttpGet get = createTotalHttpGet(url);

        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        String html = dump(entity);

        System.out.println(html);

        List<Map> list = HTMLParser.readTotalTable(html);

        return list;
    }

    public void getDetail(List<Map> rowList) throws Exception {

        Connection connection = JdbcUtils.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("insert into songs(sname, province, download, stype, songer) value(?, ?, ?, ?, ?)");
        for (int i = 0; i < rowList.size(); i++) {
            Map map = rowList.get(i);
            String songCode = (String) map.get(HTMLParser.INDEX_SONGCODE);
            int downloadTotal = Integer.parseInt(map.get(HTMLParser.INDEX_DOWNLOAD).toString());
            String time = (String) map.get(HTMLParser.INDEX_TIME);
            time = URLEncoder.encode(time, "UTF-8");
            time = URLEncoder.encode(time, "UTF-8");

            String type = (String) map.get(HTMLParser.INDEX_TYPE);
            String oldType = type;
            if (type.equals("歌曲下载包月及随身听")) {
                oldType = "全曲";
            }
            if (type.equals("振铃及歌曲下载")) {
                oldType = "振铃";
            }
            type = URLEncoder.encode(oldType, "UTF-8");
            type = URLEncoder.encode(type, "UTF-8");

            String name = (String) map.get(HTMLParser.INDEX_SONGNAME);
            String oldName = name;
            name = URLEncoder.encode(name, "UTF-8");
            name = URLEncoder.encode(name, "UTF-8");

            String url = "http://218.200.160.103:8082/CMS4-COPM/balance/cpviewStatisticByProvince.do?copyrightId=" + songCode + "&accountTime=" +  time +"&businessType=" + type  +"&contentName=" + name;

            String songer = (String) map.get(HTMLParser.INDEX_SONGER);

            HttpGet httpGet = createDetailHttpGet(url);
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String html = dump(entity);

            List<Map> list = HTMLParser.readDetailTable(html);

            int download = 0;
            for (int j = 0; j < list.size(); j++) {
                Map row = list.get(j);
                if (row.size() == 0) {
                    continue;
                }
                String province = (String) row.get("0");
                 download += Integer.parseInt(row.get("1").toString());

                preparedStatement.setString(1, oldName);
                preparedStatement.setString(2, province);
                preparedStatement.setInt(3, Integer.parseInt(row.get("1").toString()));
                preparedStatement.setString(4, oldType);
                preparedStatement.setString(5, songer);
                preparedStatement.execute();
            }

            if (download != downloadTotal) {
                System.out.println("==========" + name);
                System.exit(1);
            }
        }

        JdbcUtils.free(null, preparedStatement, connection);

    }

    /**
     * 打印页面
     * @param entity
     * @throws java.io.IOException
     */
    private static String dump(HttpEntity entity) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(entity.getContent(), "UTF-8"));

        //System.out.println(IOUtils.toString(br));
        return IOUtils.toString(br);
    }

}



/*
*
*
* SELECT sum(download) from songs

SELECT sum(download) from songs


insert into analyze_data(sname,province,download,songer,stype)
(select sname,province,sum(download) download,songer,stype from songs group by sname,province,songer,stype)


select sum(download) as download from analyze_data where sname='戴玉强' and songer='戴玉强'

select * from analyze_data order by CONVERT( sname USING GBK ) ASC
select t.* from (select count(1) count, a.* from (select * from analyze_data group by sname, songer) a group by sname) t where count>1



select * from analyze_data order by sname

select *, sum(songer) from analyze_data group by sname, songer


DROP TABLE IF EXISTS `analyze_data`;
CREATE TABLE analyze_data (
  sname varchar(255) DEFAULT NULL,
  province varchar(255) DEFAULT NULL,
  download decimal(32,0) DEFAULT NULL,
  UNIQUE KEY `sname` (`sname`,`province`)
)

select * from songs
insert into analyze_data(sname,province,download) (select sname,province,sum(download) download from songs group by sname,province)
select * from analyze_data order by sname

select distinct stype from  songs

select sum(download) from songs where sname='七月火把节' and province='四川';

insert province (province) select distinct province from songs
* */