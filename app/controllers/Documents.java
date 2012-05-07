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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Documents extends Controller
{
    private static final int DEFAULT_PAGE_SIZE = 10;

    public static void index()
    {
        render();
    }

    public static void search(String search, int page, int pageSize, String format) throws XMLStreamException
    {
        if(page < 1) {
            page = 1;
        }

        if(pageSize == 0) {
            pageSize = DEFAULT_PAGE_SIZE;
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
                .setFrom((page - 1) * pageSize).setSize(pageSize)
                .execute()
                .actionGet();

        renderArgs.put("search", search);

        List<Document> documents = extractDocuments(response);

        if("xml".equals(format)) {
            renderXML(documents, search);
        }

        renderArgs.put("count", response.getHits().totalHits());
        renderArgs.put("pageSize", pageSize);

        render("@search", documents);
    }

    public static void moreLikeThis(Long id, int page, int pageSize, String format) throws XMLStreamException
    {
        if(page < 1) {
            page = 1;
        }

        if(pageSize == 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }


        Client client = Play.plugin(ElasticSearchPlugin.class).client();
        MoreLikeThisRequest request = Requests.moreLikeThisRequest("documents");
        request.searchFrom((page - 1) * pageSize);
        request.searchSize(pageSize);
        request.type("document");
        request.id(id.toString());
        SearchResponse response = client.moreLikeThis(request)
                .actionGet();

        Document thisDocument = Document.findById(id);


        List<Document> documents = extractDocuments(response);

        if("xml".equals(format)) {
            renderXML(documents, thisDocument.title);
        }

        renderArgs.put("thisDocument", thisDocument);

        renderArgs.put("count", response.getHits().totalHits());
        renderArgs.put("pageSize", pageSize);

        render("@search", documents);
    }

    private static List<Document> extractDocuments(SearchResponse response)
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

        return documents;
    }

    private static void renderXML(List<Document> documents, String query) throws XMLStreamException
    {
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);

        writer.writeStartDocument();

        writer.writeStartElement("searchresult");

        writer.writeStartElement("query");
        writer.writeCharacters(query);
        writer.writeEndElement(); // query

        for(Document document : documents) {
            writer.writeStartElement("document");
            writer.writeAttribute("id", document.id.toString());

            writer.writeStartElement("title");
            writer.writeCharacters(document.title);
            writer.writeEndElement(); // title

            writer.writeStartElement("url");
            writer.writeEndElement(); // title

            writer.writeStartElement("snippet");
            writer.writeCharacters(document.text);
            writer.writeEndElement(); // query

            writer.writeEndElement(); // document
        }

        writer.writeEndElement(); // searchresult

        writer.writeEndDocument();

        writer.flush();
        writer.close();

        renderXml(stringWriter.toString());
    }
}
