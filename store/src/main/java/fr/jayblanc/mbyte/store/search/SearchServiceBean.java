package fr.jayblanc.mbyte.store.search;

import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import fr.jayblanc.mbyte.store.index.IndexStoreException;
import fr.jayblanc.mbyte.store.index.IndexStoreResult;
import fr.jayblanc.mbyte.store.index.IndexStoreService;
import fr.jayblanc.mbyte.store.index.IndexableContent;
import fr.jayblanc.mbyte.store.metrics.GenerateMetric;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SearchServiceBean implements SearchService {

    private static final Logger LOGGER = Logger.getLogger(SearchServiceBean.class.getName());

    @Inject IndexStoreService index;
    @Inject AuthenticationService auth;

    @Override
    @GenerateMetric(key = "search", type = GenerateMetric.Type.INCREMENT)
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SearchResult> search(String query) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Searching results for query: " + query);
        try {
            String scope = (auth.getConnectedProfile().isOwner())? IndexableContent.Scope.PRIVATE.name(): IndexableContent.Scope.PUBLIC.name();
            List<IndexStoreResult> results = index.search(scope, query);
            return results.stream().map(res -> {
                SearchResult result = SearchResult.fromIndexStoreResult(res);
                return result;
            }).collect(Collectors.toList());
        } catch (IndexStoreException e ) {
            throw new SearchServiceException("Error while searching query", e);
        }
    }

}
