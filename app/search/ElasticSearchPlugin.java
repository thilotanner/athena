package search;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.PlayPlugin;

public class ElasticSearchPlugin extends PlayPlugin
{
    private Node node;
    private Client client;

    @Override
    public void onApplicationStart()
    {
        // Start Node Builder
        Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("client.transport.sniff", true);
        settings.build();

        NodeBuilder nb = NodeBuilder.nodeBuilder().settings(settings).local(true).client(false).data(true);
        node = nb.node();
        client = node.client();
    }

    @Override
    public void onApplicationStop()
    {
        client.close();
        node.stop();
        node.close();
    }

    public Client client()
    {
        if(client == null) {
            throw new RuntimeException("ElasticSearch not started");
        }

        return client;
    }
}
