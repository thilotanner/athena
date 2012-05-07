package clustering;

import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.synthetic.ByUrlClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;

import java.util.ArrayList;
import java.util.List;

public class Carrot2Cluster
{
    List<models.Document> inputDocuments;

    List<Document> documents;

    public Carrot2Cluster(List<models.Document> inputDocuments)
    {
        this.inputDocuments = inputDocuments;

        documents = new ArrayList<Document>(inputDocuments.size());
        for(models.Document inputDocument : inputDocuments) {
            documents.add(new Document(inputDocument.title, inputDocument.text));
        }
    }

    public void cluster()
    {
        /* A controller to manage the processing pipeline. */
        final Controller controller = ControllerFactory.createSimple();

        /*
        * Perform clustering by topic using the Lingo algorithm. Lingo can
        * take advantage of the original query, so we provide it along with the documents.
        */
        final ProcessingResult byTopicClusters = controller.process(documents, "data mining",
                LingoClusteringAlgorithm.class);
        final List<Cluster> clustersByTopic = byTopicClusters.getClusters();

        /* Perform clustering by domain. In this case query is not useful, hence it is null. */
        final ProcessingResult byDomainClusters = controller.process(documents, null,
                ByUrlClusteringAlgorithm.class);
        final List<Cluster> clustersByDomain = byDomainClusters.getClusters();
        // [[[end:clustering-document-list]]]

        ConsoleFormatter.displayClusters(clustersByTopic);
        ConsoleFormatter.displayClusters(clustersByDomain);
    }
}
