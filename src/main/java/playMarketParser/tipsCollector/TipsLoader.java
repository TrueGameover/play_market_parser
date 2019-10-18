package playMarketParser.tipsCollector;

import org.jsoup.nodes.Document;
import playMarketParser.DocReader;

import java.util.Arrays;

class TipsLoader extends Thread {

    private Query query;
    private OnTipLoadCompleteListener onTipLoadCompleteListener;

    TipsLoader(Query query, OnTipLoadCompleteListener onTipLoadCompleteListener) {
        this.query = query;
        this.onTipLoadCompleteListener = onTipLoadCompleteListener;
    }

    @Override
    public void run() {
        super.run();
        collectTips();
        onTipLoadCompleteListener.onTipsLoadComplete(this);
    }

    private void collectTips() {
        //��������� �� ������� url
        String url = "https://market.android.com/suggest/SuggRequest?json=1&c=3&query=" + query.getText() + "&hl=ru&gl=RU";
        //��������� js ��������
        Document doc = DocReader.readDocByURL(url);
        if (doc == null) return;
        //�������� ������� ��������� � ���� ������
        String content = doc.text();
        if (content.equals("[]")) {
            System.out.println(query.getText());
            return;
        }
        //������ ������
        content = content.replace("\"", "");
        content = content.replace("{", "");
        content = content.replace("[s:", "");
        content = content.replace(",t:q}]", "");
        //��������� �� ������ ������ ������ � ������, � ����� � ������
        String[] tips = content.split(",t:q},s:");
        for (String tip : tips) System.out.printf("%-35s%-50s%n", query.getText(), tip);
        query.addTips(Arrays.asList(tips));
    }

    interface OnTipLoadCompleteListener {
        void onTipsLoadComplete(TipsLoader tipsLoader);
    }


    Query getQuery() {
        return query;
    }

}
