package controllers;

import models.Document;
import org.elasticsearch.action.mlt.MoreLikeThisRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import play.Play;
import play.mvc.Controller;
import search.ElasticSearchPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Documents extends Controller
{
    private static final int PAGE_SIZE = 10;

    public static void index()
    {
        render();
    }

    public static void search(String search, int page)
    {
        if(page < 1) {
            page = 1;
        }

        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        if(search == null || search.isEmpty()) {
            qb.must(QueryBuilders.matchAllQuery());
        } else {
            for(String searchPart : search.split("\\s+")) {
                qb.must(QueryBuilders.queryString(String.format("%s*", searchPart)).defaultField("_all"));
            }
        }

        Client client = Play.plugin(ElasticSearchPlugin.class).client();

        SearchResponse response = client.prepareSearch("documents")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb)
                .setFrom((page - 1) * PAGE_SIZE).setSize(PAGE_SIZE)
                .execute()
                .actionGet();

        renderArgs.put("search", search);

        renderSearchResponse(response);
    }

    public static void moreLikeThis(Long id, int page)
    {
        if(page < 1) {
            page = 1;
        }

        Client client = Play.plugin(ElasticSearchPlugin.class).client();
        MoreLikeThisRequest request = Requests.moreLikeThisRequest("documents");
        request.searchFrom((page - 1) * PAGE_SIZE);
        request.searchSize(PAGE_SIZE);
        request.type("document");
        request.id(id.toString());
        SearchResponse response = client.moreLikeThis(request)
                .actionGet();

        renderArgs.put("thisDocument", Document.findById(id));

        renderSearchResponse(response);
    }

    private static void renderSearchResponse(SearchResponse response)
    {
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

        List<Document> documents = new ArrayList<Document>(response.getHits().getHits().length);
        for(SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> hitMap = hit.sourceAsMap();

            Document document = new Document();
            document.id = Long.valueOf(hit.id());

            document.date = dateTimeFormatter.parseDateTime((String) hitMap.get("date")).toDate();
            document.title = (String) hitMap.get("title");
            document.text = (String) hitMap.get("text");

            document.scorePercentage = (int) ((1.0 - (1.0 / (1.0 + hit.getScore()))) * 100);

            documents.add(document);
        }

        renderArgs.put("count", response.getHits().totalHits());
        renderArgs.put("pageSize", PAGE_SIZE);

        render("@search", documents);
    }
}
