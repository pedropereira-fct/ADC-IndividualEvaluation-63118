package pt.unl.fct.di.adc.indeval.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.QueryResults;

import pt.unl.fct.di.adc.indeval.output.ErrorType;

public class TokenManager {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public TokenManager() { }

    public static ErrorType validToken(AuthToken token, int permissionLevel) {

        if (!completeToken(token)) {
            return ErrorType.INVALID_TOKEN;
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                                .setKind("UserLog")
                                .setFilter(PropertyFilter.eq("log_id", token.tokenId))
                                .build();

        QueryResults<Entity> res = datastore.run(query);

        if (!res.hasNext()) {
            return ErrorType.INVALID_TOKEN;
        }

        Entity tokenEntity = res.next();

        if (System.currentTimeMillis() > tokenEntity.getLong("expires_at")) {
            datastore.delete(tokenEntity.getKey());
            return ErrorType.TOKEN_EXPIRED;
        }

        int roleLevel = Role.fromString(tokenEntity.getString("log_role")).level;

        if (roleLevel < permissionLevel) {
            return ErrorType.UNAUTHORIZED;
        }

        return null;
    }

    private static boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    private static boolean completeToken (AuthToken token) {
        return nonEmptyOrBlankField(token.tokenId) &&
                nonEmptyOrBlankField(token.username) &&
                token.role != null &&
                token.issuedAt != null &&
                token.expiresAt != null;
    }

}
