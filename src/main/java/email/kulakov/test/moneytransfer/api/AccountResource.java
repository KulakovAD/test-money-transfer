package email.kulakov.test.moneytransfer.api;

import email.kulakov.test.moneytransfer.db.Datastore;
import email.kulakov.test.moneytransfer.db.OperationResult;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

@Path("/v1/accounts")
public class AccountResource {

    @Inject
    private Datastore datastore;

    @POST
    @Path("/create")
    public Long create() {
        return datastore.createAccount();
    }

    @GET
    @Path("/{id}/balance")
    public Response balance(@PathParam("id") Long accountId) {
        if (accountId == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final BigDecimal balance = datastore.balance(accountId);
        if (balance == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(balance.toString()).build();
        }
    }

    @POST
    @Path("/{id}/changeMoney")
    public Response changeMoney(@PathParam("id") Long accountId,
                                @FormParam("delta") String delta) {
        if (accountId == null || delta == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final OperationResult result = datastore.changeMoney(accountId, new BigDecimal(delta));
        return toResponse(result);
    }

    @POST
    @Path("/{id}/transferMoney")
    public Response transferMoney(@PathParam("id") Long fromAccount,
                                  @FormParam("to") Long toAccount,
                                  @FormParam("amount") String amount) {
        if (fromAccount == null || toAccount == null || amount == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final OperationResult result = datastore.transferMoney(fromAccount, toAccount, new BigDecimal(amount));
        return toResponse(result);
    }

    private Response toResponse(OperationResult result) {
        if(result.isSuccess()) {
            return Response.ok("Ok").build();
        }
        if(result == OperationResult.NoAccount) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
