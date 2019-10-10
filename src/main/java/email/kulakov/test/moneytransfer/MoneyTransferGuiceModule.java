package email.kulakov.test.moneytransfer;

import com.google.inject.AbstractModule;
import email.kulakov.test.moneytransfer.db.Datastore;

import javax.validation.constraints.NotNull;

public class MoneyTransferGuiceModule extends AbstractModule {

    private final @NotNull Class<? extends Datastore> datastoreClass;

    public MoneyTransferGuiceModule(Class<? extends Datastore> datastoreClass) {
        this.datastoreClass = datastoreClass;
    }


    @Override
    protected void configure() {
        bind(Datastore.class).to(datastoreClass);
    }
}
