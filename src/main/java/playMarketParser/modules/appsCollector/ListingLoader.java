package playMarketParser.modules.appsCollector;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import playMarketParser.entities.Connection;
import playMarketParser.entities.FoundApp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListingLoader extends Thread {

    private String query;
    private String language;
    private String country;
    private AppsCollectingListener appsCollectingListener;
    private List<FoundApp> foundApps = new ArrayList<>();
    private boolean useSelenium;


    ListingLoader(String query, String language, String country, boolean useSelenium, AppsCollectingListener appsCollectingListener) {
        this.query = query;
        this.language = language;
        this.country = country;
        this.appsCollectingListener = appsCollectingListener;
        this.useSelenium = useSelenium;
    }

    @Override
    public void run() {
        super.run();
        try {
            String url = "https://play.google.com/store/search?c=apps&q=" + query +
                    (language != null ? "&hl=" + language : "") +
                    (country != null ? "&gl=" + country : "");

            if (this.useSelenium) {
                var options = new ChromeOptions();
                options.setHeadless(true);
                System.setProperty("webdriver.chrome.driver", "/opt/chromedriver/chromedriver");

                ChromeDriver chromeDriver = null;

                try {
                    chromeDriver = new ChromeDriver(options);
                    chromeDriver.get(url);

                    // scroll down
                    for (int i = 0; i < 5; i++) {
                        chromeDriver.executeScript("{\n" +
                                "    var i = 0;\n" +
                                "    var scrollInterval = setInterval(() => {\n" +
                                "    window.scrollTo(0, document.body.scrollHeight);\n" +
                                "    if(i++ > 3 ) {\n" +
                                "        clearInterval(scrollInterval);\n" +
                                "    }}, 2000);\n" +
                                "}");

                        sleep(4000);
                    }

                    Document doc = Jsoup.parse(chromeDriver.getPageSource());
                    var nodes = doc.select("c-wiz[data-node-index][jsmodel][jsshadow][data-p][autoupdate]");
                    var popularAttribute = this.findMostPopularAttributeValue("jsrenderer", nodes);

                    if (!popularAttribute.isBlank()) {
                        var iteration = -1;

                        nodes = nodes.select("[jsrenderer=" + popularAttribute + "]");

                        for (var node : nodes) {
                            var nodeIndex = node.attr("data-node-index");
                            var position = Integer.parseInt(nodeIndex.replace("1;", ""));
                            var developerLink = node.select("a[href*=/store/apps/dev]");

                            if (nodeIndex.equals("1;0")) {
                                iteration++;
                            }

                            var detailsHrefs = node.select("a[href*=/store/apps/details]");
                            var title = detailsHrefs.select("div[title]").attr("title");
                            var img = node.select("img[data-ils][srcset]").attr("src");
                            var developerTitle = developerLink.select("div").text();
                            var developerUrl = developerLink.attr("href");
                            var rate = node.select("div[aria-label*=5]").attr("aria-label");
                            var rateMatcher = Pattern.compile(": ([0-9,]{1,3}) ").matcher(rate);
                            var shortDescription = detailsHrefs.select(":not(:has(*))").text();
                            var appId = detailsHrefs.first().attr("href").replace("/store/apps/details?id=", "");

                            var foundApp = new FoundApp();
                            foundApp.setPosition(iteration * 50 + position);
                            foundApp.setName(title);
                            foundApp.setQuery(this.query);
                            foundApp.setIconUrl(img);
                            foundApp.setDevName(developerTitle);
                            foundApp.setDevUrl(developerUrl);
                            foundApp.setShortDescr(shortDescription);
                            foundApp.setId(appId);

                            if (rateMatcher.find() && rateMatcher.groupCount() > 0) {
                                foundApp.setAvgRate(Double.parseDouble(rateMatcher.group(1).replace(',', '.')));
                            }

                            foundApps.add(foundApp);
                        }
                    }

                } finally {
                    if (chromeDriver != null) {
                        chromeDriver.close();
                        chromeDriver.quit();
                    }
                }

            } else {
                Document doc = Connection.getDocument(url);
                parseJson(doc);
            }

            appsCollectingListener.onQueryProcessed(foundApps, query, true);
        } catch (IOException | InterruptedException e) {
            appsCollectingListener.onQueryProcessed(foundApps, query, false);
        }
    }

    private void parseJson(Document doc) {
        //Извлекаем JSON
        Pattern pattern = Pattern.compile("\\{key: 'ds:3'.*?data:(.*?), sideChannel", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(doc.data());
        if (!matcher.find()) {
            System.out.printf("%-40s%s%n", query, "Не удалось получить JSON");
            return;
        }
        String jsonData = matcher.group(1);
        JsonArray appsData;
        try {
            JsonArray fullData = (JsonArray) Jsoner.deserialize(jsonData);
            appsData =
                    ((JsonArray) ((JsonArray) ((JsonArray) ((JsonArray) fullData
                            .getCollection(0))
                            .getCollection(1))
                            .getCollection(0))
                            .getCollection(0))
                            .getCollection(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.printf("%-40s%s%n", query, "Не удалось спарсить JSON");
            return;
        }
        //Обходим все блоки с данными о приложении
        for (int i = 0; i < appsData.size(); i++) {
            FoundApp app = new FoundApp();
            app.setQuery(query);
            app.setPosition(i + 1);
            JsonArray appData = (JsonArray) appsData.get(i);
            //name
            try {
                app.setName(appData.getString(2));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить имя приложения");
            }
            //icon URL
            try {
                app.setIconUrl(((JsonArray) ((JsonArray) ((JsonArray) ((JsonArray) appData
                        .getCollection(1))
                        .getCollection(1))
                        .getCollection(2))
                        .getCollection(3))
                        .getString(2));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить URL иконки приложения");
            }
            //dev name
            try {
                app.setDevName(((JsonArray) ((JsonArray) ((JsonArray) appData
                        .getCollection(4))
                        .getCollection(0))
                        .getCollection(0))
                        .getString(0));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить имя разработчика");
            }
            //Dev URL
            try {
                app.setDevUrl("https://play.google.com" + ((JsonArray) ((JsonArray) ((JsonArray) ((JsonArray) ((JsonArray) appData
                        .getCollection(4))
                        .getCollection(0))
                        .getCollection(0))
                        .getCollection(1))
                        .getCollection(4))
                        .getString(2));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить URL разработчика");
            }
            //Avg Rate
            try {
                app.setAvgRate(Double.parseDouble((
                        (JsonArray) ((JsonArray) ((JsonArray) ((JsonArray) appData
                                .getCollection(6))
                                .getCollection(0))
                                .getCollection(2))
                                .getCollection(1))
                        .getString(0).replaceAll(",", "."))
                );
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить среднюю оценку");
            }
            //Short descr
            try {
                app.setShortDescr(((JsonArray) ((JsonArray) ((JsonArray) ((JsonArray) appData
                        .getCollection(4))
                        .getCollection(1))
                        .getCollection(1))
                        .getCollection(1))
                        .getString(1));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить краткое описание");
            }
            //App ID
            try {
                app.setId(((JsonArray) appData.getCollection(12)).getString(0));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("%-40s%s%n", query, "Не удалось определить ID приложения");
            }
            foundApps.add(app);
        }
    }

    private String findMostPopularAttributeValue(String attribute, Elements elements) {
        var attrs = elements.eachAttr(attribute);
        var map = new HashMap<String, Integer>();

        for (var attr : attrs) {
            map.put(attr, map.getOrDefault(attr, 0) + 1);
        }

        var currentValue = 0;
        var maxKey = "";

        for (var pair : map.entrySet()) {
            if (pair.getValue() > currentValue) {
                maxKey = pair.getKey();
                currentValue = pair.getValue();
            }
        }

        return maxKey;
    }

    interface AppsCollectingListener {
        void onQueryProcessed(List<FoundApp> foundApps, String query, boolean isSuccess);
    }
}
