package search;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BulkIndexJob extends Job
{
    private static final int BATCH_SIZE = 100;

    @Override
    public void doJob() throws Exception
    {
        Session hibernateSession = (Session) JPA.em().getDelegate();

        hibernateSession.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException
            {
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT * FROM Document");

                Client client = Play.plugin(ElasticSearchPlugin.class).client();
                BulkRequestBuilder bulkRequest = client.prepareBulk();

                int count = 0;
                while(rs.next()) {

                    try {
                        bulkRequest.add(client.prepareIndex("documents", "document", rs.getString("id"))
                            .setSource(XContentFactory.jsonBuilder()
                                .startObject()
                                    .field("date", rs.getDate("date"))
                                    .field("title", rs.getString("title"))
                                    .field("text", rs.getString("text"))
                                .endObject()));

                        count++;
                    } catch (IOException e) {
                        Logger.warn(e, "Error while create source");
                    }

                    if(count == BATCH_SIZE) {
                        bulkRequest.execute();

                        bulkRequest = client.prepareBulk();

                        count = 0;

                        Logger.info(rs.getRow() + " document indexed");
                    }
                }

                bulkRequest.execute();

                rs.close();
            }
        });

        hibernateSession.close();
    }
}
