package controllers;


import models.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.bootstrap.ElasticSearch;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import play.Play;
import play.mvc.Controller;
import search.BulkIndexJob;
import search.ElasticSearchPlugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Application extends Controller
{

    public static void index()
    {
        render();
    }

    public static void search(String search)
    {
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        if(search == null || search.isEmpty()) {
            qb.must(QueryBuilders.matchAllQuery());
        } else {
            for(String searchPart : search.split("\\s+")) {
                qb.must(QueryBuilders.queryString(String.format("*%s*", searchPart)).defaultField("_all"));
            }
        }

        Client client = Play.plugin(ElasticSearchPlugin.class).client();

        SearchResponse response = client.prepareSearch("documents")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb)
                .setFrom(0).setSize(20)
                .execute()
                .actionGet();

        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

        List<Document> documents = new ArrayList<Document>(response.getHits().getHits().length);
        for(SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> hitMap = hit.sourceAsMap();

            Document document = new Document();
            document.id = Long.valueOf(hit.id());

            document.date = dateTimeFormatter.parseDateTime((String) hitMap.get("date")).toDate();
            document.title = (String) hitMap.get("title");
            //document.text = (String) hitMap.get("text");

            documents.add(document);
        }

        render(documents);
    }

    public static void elasticsearch()
    {
        render();
    }

    public static void indexDocuments()
    {
        BulkIndexJob bulkIndexJob = new BulkIndexJob();
        bulkIndexJob.now();
        index();
    }
}