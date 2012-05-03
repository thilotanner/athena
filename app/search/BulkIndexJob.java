package search;

import models.Document;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;
public class BulkIndexJob extends Job
{
    private static final int BATCH_SIZE = 1000;

    @Override
    public void doJob() throws Exception
    {
        long numberOfDocuments = Document.count();

        Session hibernateSession = (Session) JPA.em().getDelegate();

        StatelessSession session = hibernateSession.getSessionFactory().openStatelessSession();

        Client client = Play.plugin(ElasticSearchPlugin.class).client();

        for(int i = 0; i < numberOfDocuments; i += BATCH_SIZE) {
            BulkRequestBuilder bulkRequest = client.prepareBulk();

            ScrollableResults results = session.createCriteria(Document.class)
                    .setFirstResult(i)
                    .setMaxResults(BATCH_SIZE)
                    .setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);

            while(results.next()) {
                Document document = (Document) results.get(0);

                bulkRequest.add(client.prepareIndex("documents", "document", document.id.toString())
                        .setSource(DocumentSourceBuilder.source(document)));
            }

            bulkRequest.execute();

            results.close();

            Logger.info(String.format("%d documents indexed", i + BATCH_SIZE));
        }

        session.close();

        hibernateSession.close();
    }
}
