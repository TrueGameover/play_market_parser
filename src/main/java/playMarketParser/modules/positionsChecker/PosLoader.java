package playMarketParser.modules.positionsChecker;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import playMarketParser.entities.Connection;
import playMarketParser.entities.Query;

import java.io.IOException;


public class PosLoader extends Thread {

    private final String appURL;
    private Query query;
    private OnPosLoadCompleteListener onPosLoadCompleteListener;
    private String language;
    private String country;

    PosLoader(Query query, String appId, String language, String country, OnPosLoadCompleteListener onPosLoadCompleteListener) {
        this.onPosLoadCompleteListener = onPosLoadCompleteListener;
        this.query = query;
        this.country = country;
        this.language = language;
        appURL = "/store/apps/details?id=" + appId;
    }

    @Override
    public void run() {
        super.run();
        try {
            query.addPseudoPos(getPos());
            onPosLoadCompleteListener.onPosLoadingComplete(query, true);
        } catch (IOException e) {
            onPosLoadCompleteListener.onPosLoadingComplete(query, false);
        }
    }

    private int getPos() throws IOException {
        //CSS ����� div-� �� ������� �� �������� ����������
        String appLinkClass = "b8cIId ReQCgd Q9MA7b";
        //��������� url �������� ������
        String url = "https://play.google.com/store/search?q=" + query.getText() + "&c=apps" +
                (language != null ? "&hl=" + language : "") +
                (country != null ? "&gl=" + country : "");
        Document doc = Connection.getDocument(url);
        if (doc == null) throw new IOException("�� ������� ��������� �������� ����������� ������");
        //�������� ������ div-�� �� �������� �� ����������
        Elements appsLinksDivs = doc.getElementsByClass(appLinkClass);
        String format = "%-30s%-2s%n";
        //�������� ������ ������ �� ����������
        for (int i = 0; i < appsLinksDivs.size(); i++) {
            String curURL = appsLinksDivs.get(i).child(0).attr("href");
            if (appURL.equals(curURL)) {
                System.out.printf(format, query.getText(), i + 1);
                return i + 1;
            }
        }
        System.out.printf(format, query.getText(), "���������� ����������� � ���-" + appsLinksDivs.size());
        return 0;
    }

    interface OnPosLoadCompleteListener {
        void onPosLoadingComplete(Query query, boolean isSuccess);
    }

}
