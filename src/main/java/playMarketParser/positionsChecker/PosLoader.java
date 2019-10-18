package playMarketParser.positionsChecker;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import playMarketParser.DocReader;

import java.util.*;

public class PosLoader extends Thread {

    private final static int CHECKED_POS_COUNT = 50;
    private final String appURL;
    private final String format = "%-30s%-2s%n";

    private OnPosLoadCompleteListener onPosLoadCompleteListener;
    private Query query;

    PosLoader(Query query, String appId, OnPosLoadCompleteListener onPosLoadCompleteListener) {
        this.onPosLoadCompleteListener = onPosLoadCompleteListener;
        this.query = query;
        appURL = "/store/apps/details?id=" + appId;
    }

    @Override
    public void run() {
        super.run();
        query.addPseudoPos(getPos());
        onPosLoadCompleteListener.onPosLoadComplete(this);
    }

    private int getPos() {
        //CSS ����� div-� �� ������� �� �������� ����������
        String appLinkClass = "b8cIId ReQCgd Q9MA7b";
        //��������� url �������� ������
        String url = "https://play.google.com/store/search?q=" + query.getText() + "&c=apps";
        Document doc = DocReader.readDocByURL(url);
        Elements appsLinksDivs;
        if (doc != null)
            //�������� ������ div-�� �� �������� �� ����������
            appsLinksDivs = doc.getElementsByClass(appLinkClass);
        else {
            System.out.printf(format, query.getText(), "�� ������� ��������� �������� ����������� ������");
            return 0;
        }
        //�������� ������ ������ �� ����������
        List<String> appsURLs = new ArrayList<>();
        for (int i = 0; i < Math.min(appsLinksDivs.size(), CHECKED_POS_COUNT); i++) {
            String curURL = appsLinksDivs.get(i).child(0).attr("href");
            if (appURL.equals(curURL)) {
                System.out.printf(format, query.getText(), i + 1);
                return i + 1;
            }
        }
        System.out.printf(format, query.getText(), "���������� ����������� � ���-" + CHECKED_POS_COUNT);
        return 0;
    }

    interface OnPosLoadCompleteListener {
        void onPosLoadComplete(PosLoader posLoader);
    }

}
