package weibo4j.examples.search;

import weibo4j.Search;
import weibo4j.Weibo;
import weibo4j.model.WeiboException;

public class searchSuggestionsUsers {

	public static void main(String[] args) {
		Weibo weibo = new Weibo();
		weibo.setToken(args[0]);
		Search search = new Search();
		try {
			search.searchSuggestionsUsers(args[1]);
		} catch (WeiboException e) {
			e.printStackTrace();
		}
	}

}
