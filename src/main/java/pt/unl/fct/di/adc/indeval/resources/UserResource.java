package pt.unl.fct.di.adc.indeval.resources;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.indeval.output.ErrorType;
import pt.unl.fct.di.adc.indeval.output.ResponseFormat;
import pt.unl.fct.di.adc.indeval.util.InputRequest;
import pt.unl.fct.di.adc.indeval.util.AuthInputRequest;
import pt.unl.fct.di.adc.indeval.util.RegisterData;
import pt.unl.fct.di.adc.indeval.util.Role;
import pt.unl.fct.di.adc.indeval.util.UserData;
import pt.unl.fct.di.adc.indeval.util.AuthToken;
import pt.unl.fct.di.adc.indeval.util.TokenManager;
import pt.unl.fct.di.adc.indeval.util.DeleteAccountData;
import pt.unl.fct.di.adc.indeval.util.ModifyAccountAttributesData;
import pt.unl.fct.di.adc.indeval.util.ShowUserRoleData;
import pt.unl.fct.di.adc.indeval.util.ChangeUserRoleData;
import pt.unl.fct.di.adc.indeval.util.ChangePwdData;

@Path("/")
public class UserResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public UserResource() { }
    
    @POST
    @Path("/createaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(InputRequest<RegisterData> request) {

        RegisterData data = request.input;

        if (!data.validRegistration()) {

            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.INVALID_INPUT))
                    .build();
        }

        Role role;

        try {

            role = Role.fromString(data.role);

        } catch (Exception e) {

            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.INVALID_INPUT))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user != null) {
            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.USER_ALREADY_EXISTS))
                    .build();
        }

        UserData userData = new UserData(data.username, data.password, data.phone, data.address, role);

        user = Entity.newBuilder(userKey)
                .set("user_name", userData.username)
                .set("user_pwd", DigestUtils.sha512Hex(userData.password))
                .set("user_phone", userData.phone)
                .set("user_address", userData.address)
                .set("user_role", role.toString())
                .build();

        datastore.put(user);

        return Response.ok()
                .entity(ResponseFormat.success(
                    Map.of(
                        "username", userData.username,
                        "role", role.toString()
                    )
                ))
                .build();
    }

    @POST
    @Path("/showusers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showUsers(AuthInputRequest<Void> request) {

        AuthToken at = request.token;

        ErrorType tokenValidation = TokenManager.validToken(at, Role.BOFFICER.level);

        if (tokenValidation != null) {
            return Response.ok()
                    .entity(ResponseFormat.error(tokenValidation))
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                                .setKind("User")
                                .build();
        
        QueryResults<Entity> res = datastore.run(query);
        List<Map<String, Object>> userList = new ArrayList<>();
        
        while(res.hasNext()) {

            Entity user = res.next();
            Map<String, Object> u = new HashMap<>();
            u.put("username", user.getString("user_name"));
            u.put("role", user.getString("user_role"));
            userList.add(u);
        }

        Map<String, Object> data = Map.of("users", userList);

        return Response.ok()
                .entity(ResponseFormat.success(data))
                .build();
    }

    @POST
    @Path("/deleteaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccount(AuthInputRequest<DeleteAccountData> request) {

        DeleteAccountData data = request.input;
        AuthToken at = request.token;

        ErrorType tokenValidation = TokenManager.validToken(at, Role.ADMIN.level);
        if (tokenValidation != null) {
            return Response.ok()
                    .entity(ResponseFormat.error(tokenValidation))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.USER_NOT_FOUND))
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                                .setKind("UserLog")
                                .setFilter(PropertyFilter.eq("log_user", data.username))
                                .build();
        QueryResults<Entity> tokens = datastore.run(query);
        while (tokens.hasNext()) {
            datastore.delete(tokens.next().getKey());
        }

        datastore.delete(userKey);


        return Response.ok()
                .entity(ResponseFormat.success(Map.of(
                    "message", "Account deleted successfully"
                )))
                .build();
    }

    @POST
    @Path("/modaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyAccountAttributes(AuthInputRequest<ModifyAccountAttributesData> request) {

        ModifyAccountAttributesData data = request.input;
        AuthToken at = request.token;

        ErrorType tokenValidation = TokenManager.validToken(at, Role.USER.level);
        if (tokenValidation != null) {
            return Response.ok()
                    .entity(ResponseFormat.error(tokenValidation))
                    .build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.USER_NOT_FOUND))
                    .build();
        }

        Role requesterRole = at.role;
        Role targetRole = Role.fromString(user.getString("user_role"));

        boolean authorized = false;

        if (requesterRole == Role.ADMIN) {
            authorized = true;
        }
            
        else if (requesterRole == Role.BOFFICER) {
            if (at.username.equals(data.username) || targetRole == Role.USER) {
                authorized = true;
            }
        }

        else if (requesterRole == Role.USER) {
            if (at.username.equals(data.username)) {
                authorized = true;
            }
        }

        if (!authorized) {
            return Response.ok()
                .entity(ResponseFormat.error(ErrorType.UNAUTHORIZED))
                .build();
        }

        if (!data.attributes.validModification()) {
            return Response.ok()
                    .entity(ResponseFormat.error(ErrorType.INVALID_INPUT))
                    .build();
        }

        Entity.Builder userBuilder = Entity.newBuilder(user);

        if (data.attributes.phone != null && !data.attributes.phone.isBlank()) {
            userBuilder.set("user_phone", data.attributes.phone);
        }

        if (data.attributes.address != null && !data.attributes.address.isBlank()) {
            userBuilder.set("user_address", data.attributes.address);
        }

        datastore.put(userBuilder.build());

        return Response.ok()
                .entity(ResponseFormat.success(Map.of(
                    "message", "Updated successfully"
                )))
                .build();
    }

@POST
@Path("/showuserrole")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response showUserRole(AuthInputRequest<ShowUserRoleData> request) {

    ShowUserRoleData data = request.input;
    AuthToken at = request.token;

    ErrorType tokenValidation = TokenManager.validToken(at, Role.BOFFICER.level);
    if (tokenValidation != null) {
        return Response.ok()
                .entity(ResponseFormat.error(tokenValidation))
                .build();
    }

    Key userKey = datastore.newKeyFactory()
            .setKind("User")
            .newKey(data.username);

    Entity user = datastore.get(userKey);

    if (user == null) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.USER_NOT_FOUND))
                .build();
    }

    String role = user.getString("user_role");

    return Response.ok()
            .entity(ResponseFormat.success(
                    Map.of(
                            "username", data.username,
                            "role", role
                    )
            ))
            .build();
    }

@POST
@Path("/changeuserrole")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response changeUserRole(AuthInputRequest<ChangeUserRoleData> request) {

    ChangeUserRoleData data = request.input;
    AuthToken at = request.token;

    ErrorType tokenValidation = TokenManager.validToken(at, Role.ADMIN.level);
    if (tokenValidation != null) {
        return Response.ok()
                .entity(ResponseFormat.error(tokenValidation))
                .build();
    }

    Role newRole;
    try {
        newRole = Role.fromString(data.newRole);
    } catch (Exception e) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.INVALID_INPUT))
                .build();
    }

    Key userKey = datastore.newKeyFactory()
            .setKind("User")
            .newKey(data.username);

    Entity user = datastore.get(userKey);

    if (user == null) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.USER_NOT_FOUND))
                .build();
    }

    Entity updatedUser = Entity.newBuilder(user)
            .set("user_role", newRole.toString())
            .build();

    datastore.put(updatedUser);

    return Response.ok()
            .entity(ResponseFormat.success(
                    Map.of("message", "Role updated successfully")
            ))
            .build();
    }

@POST
@Path("/changeuserpwd")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response changeUserPassword(AuthInputRequest<ChangePwdData> request) {

    ChangePwdData data = request.input;
    AuthToken at = request.token;

    ErrorType tokenValidation = TokenManager.validToken(at, Role.USER.level);
    if (tokenValidation != null) {
        return Response.ok()
                .entity(ResponseFormat.error(tokenValidation))
                .build();
    }

    if (!data.username.equals(at.username)) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.UNAUTHORIZED))
                .build();
    }

    Key userKey = datastore.newKeyFactory()
            .setKind("User")
            .newKey(data.username);

    Entity user = datastore.get(userKey);

    if (user == null) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.USER_NOT_FOUND))
                .build();
    }

    String storedPassword = user.getString("user_pwd");

    if (!DigestUtils.sha512Hex(data.oldPwd).equals(storedPassword)) {
        return Response.ok()
                .entity(ResponseFormat.error(ErrorType.INVALID_CREDENTIALS))
                .build();
    }

    Entity updatedUser = Entity.newBuilder(user)
            .set("user_pwd", DigestUtils.sha512Hex(data.newPwd))
            .build();

    datastore.put(updatedUser);

    return Response.ok()
            .entity(ResponseFormat.success(
                    Map.of("message", "Password changed successfully")
            ))
            .build();
    }


}
