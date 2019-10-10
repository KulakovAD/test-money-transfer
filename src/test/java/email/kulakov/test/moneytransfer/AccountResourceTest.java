package email.kulakov.test.moneytransfer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import email.kulakov.test.moneytransfer.db.BlockingDatastore;
import email.kulakov.test.moneytransfer.db.ConcurrentDatastore;
import email.kulakov.test.moneytransfer.db.Datastore;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

@RunWith(value = Parameterized.class)
public class AccountResourceTest {

    private static String SERVICE_URL = "http://localhost:8080/v1/accounts";

    private final Class<? extends Datastore> datastoreClass;

    private Server server;

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{ConcurrentDatastore.class, BlockingDatastore.class};
    }

    public AccountResourceTest(Class<? extends Datastore> datastoreClass) {
        this.datastoreClass = datastoreClass;
    }

    @Before
    public void prepare() throws Exception {
        final Injector injector = Guice.createInjector(new MoneyTransferGuiceModule(datastoreClass));
        server = MoneyTransferApplication.createServer(injector, 8080);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void createAccountTest() {
        //when
        final long id1 = createAccount();
        //then
        Assert.assertEquals(1, id1);

        //when
        final long id2 = createAccount();
        //then
        Assert.assertEquals(2, id2);
    }

    @Test
    public void notExistingBalanceTest() {
        //when
        final Response response = balanceRequest(42L);
        //then
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void changeMoneySucessTest() {
        //given
        final long id1 = createAccount();
        final BigDecimal delta = new BigDecimal("12.34");

        //when
        assertSuccessChangeMoney(id1, delta);
        //then
        Assert.assertEquals(delta, balance(id1));

        //when
        assertSuccessChangeMoney(id1, delta);
        //then
        Assert.assertEquals(delta.multiply(new BigDecimal(2)), balance(id1));

        //when
        assertSuccessChangeMoney(id1, delta.negate());
        //then
        Assert.assertEquals(delta, balance(id1));
    }

    @Test
    public void changeMoneyToNegativeTest() {
        //given
        final long id1 = createAccount();
        final BigDecimal initial = new BigDecimal("10.00");
        assertSuccessChangeMoney(id1, initial);

        //when
        final BigDecimal delta = new BigDecimal("-100.00");
        final Response response = changeMoneyRequest(id1, delta);

        //then
        Assert.assertEquals(500, response.getStatus());
        Assert.assertEquals(initial, balance(id1));
    }

    @Test
    public void changeMoneyNotExistingTest() {
        //when
        final BigDecimal delta = new BigDecimal("10.00");
        final Response response = changeMoneyRequest(42L, delta);

        //then
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void transferMoneySuccessTest() {
        //given
        final long id1 = createAccount();
        final long id2 = createAccount();
        final BigDecimal initial = new BigDecimal("100.00");
        final BigDecimal toTransfer = new BigDecimal("10.00");
        assertSuccessChangeMoney(id1, initial);
        assertSuccessChangeMoney(id2, initial);

        //when
        assertSuccessTransferMoney(id1, id2, toTransfer);
        //then
        Assert.assertEquals(initial.subtract(toTransfer), balance(id1));
        Assert.assertEquals(initial.add(toTransfer), balance(id2));

        //when
        assertSuccessTransferMoney(id2, id1, toTransfer);
        //then
        Assert.assertEquals(initial, balance(id1));
        Assert.assertEquals(initial, balance(id2));
    }

    @Test
    public void transferMoneyToNegativeTest() {
        //given
        final long id1 = createAccount();
        final long id2 = createAccount();
        final BigDecimal initial = new BigDecimal("10.00");
        final BigDecimal toTransfer = new BigDecimal("100.00");
        assertSuccessChangeMoney(id1, initial);
        assertSuccessChangeMoney(id2, initial);

        //when
        final Response response = transferMoneyRequest(id1, id2, toTransfer);
        //then
        Assert.assertEquals(500, response.getStatus());
        Assert.assertEquals(initial, balance(id1));
        Assert.assertEquals(initial, balance(id2));
    }

    @Test
    public void transferNegativeAmountTest() {
        //given
        final long id1 = createAccount();
        final long id2 = createAccount();
        final BigDecimal initial = new BigDecimal("100.00");
        final BigDecimal toTransfer = new BigDecimal("-10.00");
        assertSuccessChangeMoney(id1, initial);
        assertSuccessChangeMoney(id2, initial);

        //when
        final Response response = transferMoneyRequest(id1, id2, toTransfer);
        //then
        Assert.assertEquals(500, response.getStatus());
        Assert.assertEquals(initial, balance(id1));
        Assert.assertEquals(initial, balance(id2));
    }

    @Test
    public void transferMoneyToNotExistingTest() {
        //given
        final long id1 = createAccount();
        final BigDecimal initial = new BigDecimal("100.00");
        final BigDecimal toTransfer = new BigDecimal("10.00");
        assertSuccessChangeMoney(id1, initial);

        //when
        final Response response = transferMoneyRequest(id1, id1 + 1, toTransfer);
        //then
        Assert.assertEquals(404, response.getStatus());
        Assert.assertEquals(initial, balance(id1));
    }

    @Test
    public void transferMoneyFromNotExistingTest() {
        //given
        final long id2 = createAccount();
        final BigDecimal initial = new BigDecimal("100.00");
        final BigDecimal toTransfer = new BigDecimal("10.00");
        assertSuccessChangeMoney(id2, initial);

        //when
        final Response response = transferMoneyRequest(id2 - 1, id2, toTransfer);
        //then
        Assert.assertEquals(404, response.getStatus());
        Assert.assertEquals(initial, balance(id2));
    }

    @Test
    public void transferMoneyToSelfTest() {
        //given
        final long id1 = createAccount();
        final BigDecimal initial = new BigDecimal("100.00");
        final BigDecimal toTransfer = new BigDecimal("10.00");
        assertSuccessChangeMoney(id1, initial);

        //when
        assertSuccessTransferMoney(id1, id1, toTransfer);
        //then
        Assert.assertEquals(initial, balance(id1));
    }

    private long createAccount() {
        final Response response = ClientBuilder.newClient()
                .target(SERVICE_URL + "/create").request()
                .post(Entity.form(new Form()));
        Assert.assertEquals(200, response.getStatus());
        return response.readEntity(Long.class);
    }

    private void assertSuccessChangeMoney(long account, BigDecimal delta) {
        final Response response = changeMoneyRequest(account, delta);
        Assert.assertEquals(200, response.getStatus());
    }

    private BigDecimal balance(long account) {
        final Response response = balanceRequest(account);
        Assert.assertEquals(200, response.getStatus());
        return new BigDecimal(response.readEntity(String.class));
    }

    private void assertSuccessTransferMoney(long from, long to, BigDecimal amount) {
        final Response response = transferMoneyRequest(from, to, amount);
        Assert.assertEquals(200, response.getStatus());
    }

    private Response balanceRequest(long account) {
        return ClientBuilder.newClient()
                .target(SERVICE_URL + "/" + account + "/balance").request()
                .get();
    }

    private Response changeMoneyRequest(long account, BigDecimal delta) {
        return ClientBuilder.newClient()
                .target(SERVICE_URL + "/" + account + "/changeMoney").request()
                .post(Entity.form(new Form("delta", delta.toString())));
    }

    private Response transferMoneyRequest(long from, long to, BigDecimal amount) {
        final Form form = new Form()
                .param("to", String.valueOf(to))
                .param("amount", amount.toString());
        return ClientBuilder.newClient()
                .target(SERVICE_URL + "/" + from + "/transferMoney").request()
                .post(Entity.form(form));
    }
}

