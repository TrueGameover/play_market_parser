package playMarketParser.tipsCollector;

import org.jsoup.nodes.Document;
import playMarketParser.DocReader;
import playMarketParser.Prefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class TipsLoader extends Thread {

    private Query query;
    private OnTipLoadCompleteListener onTipLoadCompleteListener;
    private List<Tip> tips = new ArrayList<>();

    TipsLoader(Query query, OnTipLoadCompleteListener onTipLoadCompleteListener) {
        this.query = query;
        this.onTipLoadCompleteListener = onTipLoadCompleteListener;
    }

    @Override
    public void run() {
        super.run();
        try {
            collectTips();
            onTipLoadCompleteListener.onTipsLoadingComplete(this, true);
        } catch (IOException e) {
            onTipLoadCompleteListener.onTipsLoadingComplete(this, false);
        }
    }

    private void collectTips() throws IOException {
        String queryText = query.getText();
        //��������� �� ������� url
        String url = "https://market.android.com/suggest/SuggRequest?json=1&c=3&query=" + queryText + "&hl=" + Prefs.getString("tips_lang");

        //��������� js ��������
        Document doc = DocReader.readDocByURL(url);
        //�������� ������� ��������� � ���� ������
        String content = doc.text();
        if (content.equals("[]")) {
            System.out.println(queryText);
            return;
        }
        //������ ������
        content = content.replace("\"", "");
        content = content.replace("{", "");
        content = content.replace("[s:", "");
        content = content.replace(",t:q}]", "");
        //��������� �� ������ ������ ������ � ������, � ����� � ������
        String[] tipsArray = content.split(",t:q},s:");
        for (String tip : tipsArray) {
            if (isUncorrected(queryText, tip)) tips.add(new Tip(query.getRootQueryText(), tip, query.getDepth()));
            System.out.printf("%-35s%-50s%n", queryText, tip);
        }
    }

    interface OnTipLoadCompleteListener {
        void onTipsLoadingComplete(TipsLoader tipsLoader, boolean isSuccess);
    }

    Query getQuery() {
        return query;
    }

    List<Tip> getTips() {
        return tips;
    }

    private static boolean isUncorrected(String query, String tip) {
        return (tip.length() > query.length() && tip.substring(0, query.length()).equals(query));
    }
}
