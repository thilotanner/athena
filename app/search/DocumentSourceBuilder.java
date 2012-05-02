package search;

import models.Document;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

public class DocumentSourceBuilder
{
    public static XContentBuilder source(Document document) throws IOException
    {
        return XContentFactory.jsonBuilder()
            .startObject()
                .field("date", document.date)
                .field("title", document.title)
                .field("text", document.text)
            .endObject();
    }
}
