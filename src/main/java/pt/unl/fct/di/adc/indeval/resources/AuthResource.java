package pt.unl.fct.di.adc.indeval.resources;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.indeval.output.ResponseFormat;
import pt.unl.fct.di.adc.indeval.output.ErrorType;
import pt.unl.fct.di.adc.indeval.util.InputRequest;
import pt.unl.fct.di.adc.indeval.util.AuthInputRequest;
import pt.unl.fct.di.adc.indeval.util.LoginData;
import pt.unl.fct.di.adc.indeval.util.AuthToken;
import pt.unl.fct.di.adc.indeval.util.TokenManager;
import pt.unl.fct.di.adc.indeval.util.Role;
import pt.unl.fct.di.adc.indeval.util.LogoutData;


@Path("/")
public class AuthResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public AuthResource() { }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(InputRequest<LoginData> request) {

        LoginData data = request.input;

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);


        if (user == null) {

            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.USER_NOT_FOUND))
                    .build();
        }

        if (!correctPassword(data, user)) {

            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.INVALID_CREDENTIALS))
                    .build();
        }

        String role = user.getString("user_role");
        AuthToken at = new AuthToken(data.username, Role.fromString(role));

        KeyFactory logKeyFactory = datastore.newKeyFactory()
                            .addAncestor(PathElement.of("User", data.username))
                            .setKind("UserLog");

        Key logKey = datastore.allocateId(logKeyFactory.newKey());
        Entity userLog = Entity.newBuilder(logKey)
                            .set("log_id", at.tokenID)
                            .set("log_user", at.username)
                            .set("log_role", role)
                            .set("issued_at", at.issuedAt)
                            .set("expired_at", at.expiresAt)
                            .build();

        datastore.put(userLog);

        return Response.ok()
                .entity(ResponseFormat.success(
                    Map.of(
                        "token", Map.of(
                            "tokenId", at.tokenID,
                            "username", at.username,
                            "role", role,
                            "issuedAt", at.issuedAt,
                            "expiresAt", at.expiresAt
                        )
                    )
                ))
                .build();

    }

    private static boolean correctPassword(LoginData data, Entity user) {
        return (DigestUtils.sha512Hex(data.password)).equals(user.getString("user_pwd"));
    }

    @POST
    @Path("/showauthsessions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showAuthSessions(AuthInputRequest<Void> request) {

        AuthToken at = request.token;
        ErrorType tokenValidation = TokenManager.validToken(at, Role.ADMIN.level);

        if (tokenValidation != null) {
            return Response.ok()
                    .entity(ResponseFormat.error(tokenValidation))
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("UserLog")
            .build();

    QueryResults<Entity> results = datastore.run(query);

    List<Map<String, Object>> sessions = new ArrayList<>();

    long currentTime = System.currentTimeMillis();

    while (results.hasNext()) {

        Entity session = results.next();

        long expiresAt = session.getLong("expire_at");

        if (currentTime > expiresAt) {
            datastore.delete(session.getKey());
            continue;
        }

        sessions.add(Map.of(
                "tokenId", session.getString("log_id"),
                "username", session.getString("log_user"),
                "role", session.getString("log_role"),
                "expiresAt", expiresAt
        ));
    }

    return Response.ok()
            .entity(ResponseFormat.success(
                    Map.of("sessions", sessions)
            ))
            .build();
    }

@POST
@Path("/logout")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response logout(AuthInputRequest<LogoutData> request) {

    LogoutData data = request.input;
    AuthToken at = request.token;

    ErrorType tokenValidation = TokenManager.validToken(at, Role.USER.level);
    if (tokenValidation != null) {
        return Response.ok()
                .entity(ResponseFormat.error(tokenValidation))
                .build();
    }

    Role requesterRole = at.role;

    if (requesterRole != Role.ADMIN && !data.username.equals(at.username)) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.UNAUTHORIZED))
                .build();
    }

    Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("UserLog")
            .setFilter(PropertyFilter.eq("log_id", at.tokenID))
            .build();

    QueryResults<Entity> results = datastore.run(query);

    if (!results.hasNext()) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.INVALID_TOKEN))
                .build();
    }

    Entity tokenEntity = results.next();

    datastore.delete(tokenEntity.getKey());

    return Response.ok()
            .entity(ResponseFormat.success(
                    Map.of("message", "Logout successful")
            ))
            .build();
}

}